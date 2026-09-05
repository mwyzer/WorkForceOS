package com.workforceos.event;

public final class EventTypes {

    public static final String ROSTER_PUBLISHED = "RosterPublished";
    public static final String ATTENDANCE_CLOCKED_IN = "AttendanceClockedIn";
    public static final String ATTENDANCE_CLOCKED_OUT = "AttendanceClockedOut";
    public static final String LEAVE_APPROVED = "LeaveApproved";
    public static final String LEAVE_REJECTED = "LeaveRejected";
    public static final String OVERTIME_APPROVED = "OvertimeApproved";
    public static final String OVERTIME_REJECTED = "OvertimeRejected";
    public static final String HANDOVER_SUBMITTED = "HandoverSubmitted";
    public static final String HANDOVER_ACKNOWLEDGED = "HandoverAcknowledged";

    private EventTypes() {
    }
}