package xyz.jiayuhan.monomer.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.service.FileService;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionFailureRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionItemRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.service.LearningQueryService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;

/** 在模块化单体内领取 OCR 任务，读取私有原图并把百炼结果交还学习域落库。 */
@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "enabled", havingValue = "true")
public class OcrWorkerScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrWorkerScheduler.class);

    private final LearningQueryService learningQueryService;
    private final LearningCommandService learningCommandService;
    private final FileService fileService;
    private final QwenOcrClient qwenOcrClient;
    private final OcrProperties properties;

    public OcrWorkerScheduler(
            LearningQueryService learningQueryService,
            LearningCommandService learningCommandService,
            FileService fileService,
            QwenOcrClient qwenOcrClient,
            OcrProperties properties) {
        this.learningQueryService = learningQueryService;
        this.learningCommandService = learningCommandService;
        this.fileService = fileService;
        this.qwenOcrClient = qwenOcrClient;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.ocr.fixed-delay-ms:5000}")
    public void poll() {
        List<LearningView.PhotoWorkItem> pending = learningQueryService.listPendingPhotoTasks(
                Math.max(1, Math.min(properties.getBatchSize(), 20)));
        for (LearningView.PhotoWorkItem workItem : pending) {
            process(workItem);
        }
    }

    private void process(LearningView.PhotoWorkItem workItem) {
        try {
            learningCommandService.startPhotoRecognition(workItem.markingTaskId());
        } catch (BusinessException exception) {
            // 多实例同时轮询时，只有成功把 UPLOADED 改为 PROCESSING 的实例继续处理。
            log.debug("OCR 任务已被其他工作器领取，taskId={}", workItem.markingTaskId());
            return;
        } catch (RuntimeException exception) {
            log.error("OCR 任务领取失败，taskId={}", workItem.markingTaskId(), exception);
            return;
        }

        try {
            FileDownload download = fileService.downloadInternal(
                    workItem.studentId(), workItem.originalFileId());
            byte[] image = readImage(download);
            String contentType = imageContentType(download);
            List<PhotoRecognitionItemRequest> items = qwenOcrClient.recognize(image, contentType);
            learningCommandService.applyPhotoRecognition(
                    workItem.markingTaskId(), new PhotoRecognitionRequest(items));
            log.info("Qwen-VL-OCR 任务完成，taskId={}, itemCount={}",
                    workItem.markingTaskId(), items.size());
        } catch (RuntimeException exception) {
            log.error("Qwen-VL-OCR 任务失败，taskId={}", workItem.markingTaskId(), exception);
            markFailed(workItem.markingTaskId(), exception);
        }
    }

    private byte[] readImage(FileDownload download) {
        long maxBytes = properties.getMaxImageBytes();
        if (maxBytes <= 0 || maxBytes >= Integer.MAX_VALUE) {
            throw new QwenOcrClient.OcrProcessingException("OCR 图片大小限制配置不正确");
        }
        if (download.sizeBytes() <= 0 || download.sizeBytes() > maxBytes) {
            throw new QwenOcrClient.OcrProcessingException("作业图片大小超过 OCR 限制");
        }
        try (InputStream inputStream = download.inputStream()) {
            byte[] bytes = inputStream.readNBytes((int) maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new QwenOcrClient.OcrProcessingException("作业图片大小超过 OCR 限制");
            }
            return bytes;
        } catch (IOException exception) {
            throw new QwenOcrClient.OcrProcessingException("读取作业图片失败", exception);
        }
    }

    private String imageContentType(FileDownload download) {
        if (StringUtils.hasText(download.contentType())
                && download.contentType().toLowerCase().startsWith("image/")) {
            return download.contentType().toLowerCase();
        }
        String guessed = URLConnection.guessContentTypeFromName(download.originalName());
        if (StringUtils.hasText(guessed) && guessed.startsWith("image/")) {
            return guessed;
        }
        throw new QwenOcrClient.OcrProcessingException("OCR 仅支持图片文件");
    }

    private void markFailed(Long markingTaskId, RuntimeException exception) {
        String detail = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage().trim() : "内部处理异常";
        String reason = "Qwen-VL-OCR 识别失败：" + detail;
        if (reason.length() > 1000) {
            reason = reason.substring(0, 1000);
        }
        try {
            learningCommandService.failPhotoRecognition(
                    markingTaskId, new PhotoRecognitionFailureRequest(reason));
        } catch (RuntimeException failureException) {
            log.error("OCR 任务失败状态回写失败，taskId={}", markingTaskId, failureException);
        }
    }
}
