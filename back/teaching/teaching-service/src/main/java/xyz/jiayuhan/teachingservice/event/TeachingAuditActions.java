package xyz.jiayuhan.teachingservice.event;

/** 教学领域稳定审计动作编码。 */
public final class TeachingAuditActions {

    public static final String CLASS_CREATED = "teaching.class.create";
    public static final String CLASS_UPDATED = "teaching.class.update";
    public static final String CLASS_STATUS_CHANGED = "teaching.class.status.change";
    public static final String STUDENT_JOINED = "teaching.class.student.join";
    public static final String STUDENT_LEFT = "teaching.class.student.leave";
    public static final String ASSIGNMENT_CREATED = "teaching.assignment.create";
    public static final String ASSIGNMENT_UPDATED = "teaching.assignment.update";
    public static final String ASSIGNMENT_PUBLISHED = "teaching.assignment.publish";
    public static final String ASSIGNMENT_STATUS_CHANGED = "teaching.assignment.status.change";
    public static final String SUBMISSION_COMMENTED = "teaching.submission.comment";

    private TeachingAuditActions() {
    }
}
