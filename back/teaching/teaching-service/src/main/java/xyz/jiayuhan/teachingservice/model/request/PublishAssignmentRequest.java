package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 发布作业；分层作业必须覆盖全部当前在班学生。 */
public record PublishAssignmentRequest(
        @NotNull(message = "版本号不能为空") @Min(0) Integer version,
        @NotNull(message = "学生分层不能为空") List<@Valid StudentLayerAssignmentRequest> studentLayers
) {
}
