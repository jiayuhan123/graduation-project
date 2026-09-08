package xyz.jiayuhan.contentservice.event;

/** 内容服务审计动作的稳定编码。 */
public final class ContentAuditActions {

    public static final String MODULE_CREATED = "content.module.created";
    public static final String MODULE_UPDATED = "content.module.updated";
    public static final String MODULE_STATUS_CHANGED = "content.module.status-changed";
    public static final String MODULE_DELETED = "content.module.deleted";
    public static final String KNOWLEDGE_POINT_CREATED = "content.knowledge-point.created";
    public static final String KNOWLEDGE_POINT_UPDATED = "content.knowledge-point.updated";
    public static final String KNOWLEDGE_POINT_STATUS_CHANGED = "content.knowledge-point.status-changed";
    public static final String KNOWLEDGE_POINT_DELETED = "content.knowledge-point.deleted";
    public static final String QUESTION_CREATED = "content.question.created";
    public static final String QUESTION_UPDATED = "content.question.updated";
    public static final String QUESTION_REVIEWED = "content.question.reviewed";
    public static final String QUESTION_DELETED = "content.question.deleted";
    public static final String QUESTION_FEEDBACK_CREATED = "content.question-feedback.created";
    public static final String QUESTION_FEEDBACK_HANDLED = "content.question-feedback.handled";
    public static final String ERROR_TYPE_CREATED = "content.error-type.created";
    public static final String ERROR_TYPE_UPDATED = "content.error-type.updated";
    public static final String ERROR_TYPE_STATUS_CHANGED = "content.error-type.status-changed";
    public static final String ERROR_TYPE_DELETED = "content.error-type.deleted";

    private ContentAuditActions() {
    }
}
