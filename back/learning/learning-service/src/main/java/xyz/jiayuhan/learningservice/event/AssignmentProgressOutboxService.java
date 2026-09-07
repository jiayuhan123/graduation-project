package xyz.jiayuhan.learningservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.learningservice.entity.AssignmentSyncEvent;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.mapper.AssignmentSyncEventMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 与学习数据同事务记录作业进度，唯一键保证状态事件幂等。 */
@Service
@RequiredArgsConstructor
public class AssignmentProgressOutboxService {

    private final AssignmentSyncEventMapper eventMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(
            PracticeTask task,
            String progressStatus,
            BigDecimal score,
            Integer correctCount,
            Integer totalCount,
            OffsetDateTime completedAt) {
        AssignmentSyncEvent event = new AssignmentSyncEvent();
        event.setAssignmentId(Long.valueOf(task.getSourceId()));
        event.setStudentId(task.getStudentId());
        event.setPracticeTaskId(task.getId());
        event.setProgressStatus(progressStatus);
        event.setScore(score);
        event.setCorrectCount(correctCount);
        event.setTotalCount(totalCount);
        event.setStartedAt(task.getStartedAt());
        event.setSubmittedAt(completedAt);
        event.setGradedAt(completedAt);
        event.setDeliveryStatus("PENDING");
        event.setRetryCount(0);
        event.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC));
        eventMapper.insert(event);
    }
}
