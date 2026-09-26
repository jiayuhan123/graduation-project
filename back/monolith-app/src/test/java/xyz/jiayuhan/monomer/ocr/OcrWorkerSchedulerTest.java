package xyz.jiayuhan.monomer.ocr;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.service.FileService;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionItemRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.service.LearningQueryService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrWorkerSchedulerTest {

    @Test
    void claimsReadsRecognizesAndAppliesOnePendingTask() {
        LearningQueryService queryService = mock(LearningQueryService.class);
        LearningCommandService commandService = mock(LearningCommandService.class);
        FileService fileService = mock(FileService.class);
        QwenOcrClient client = mock(QwenOcrClient.class);
        OcrProperties properties = new OcrProperties();
        properties.setBatchSize(2);

        UUID fileId = UUID.randomUUID();
        LearningView.PhotoWorkItem workItem = new LearningView.PhotoWorkItem(
                101L, 7L, fileId, 1, OffsetDateTime.now());
        byte[] image = {1, 2, 3};
        PhotoRecognitionItemRequest item = new PhotoRecognitionItemRequest(
                1, null, null, "6 × 7 =", "42", null, new BigDecimal("0.9600"));

        when(queryService.listPendingPhotoTasks(2)).thenReturn(List.of(workItem));
        when(fileService.downloadInternal(7L, fileId)).thenReturn(new FileDownload(
                new ByteArrayInputStream(image), "homework.png", "image/png", image.length));
        when(client.recognize(any(byte[].class), eq("image/png"))).thenReturn(List.of(item));

        OcrWorkerScheduler scheduler = new OcrWorkerScheduler(
                queryService, commandService, fileService, client, properties);
        scheduler.poll();

        verify(commandService).startPhotoRecognition(101L);
        ArgumentCaptor<PhotoRecognitionRequest> request =
                ArgumentCaptor.forClass(PhotoRecognitionRequest.class);
        verify(commandService).applyPhotoRecognition(eq(101L), request.capture());
        assertEquals(List.of(item), request.getValue().items());
    }
}
