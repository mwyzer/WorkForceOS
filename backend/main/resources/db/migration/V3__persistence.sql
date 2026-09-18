CREATE TABLE shift_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    overnight BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    breaks TEXT NOT NULL
);

CREATE TABLE rosters (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE roster_assignments (
    id UUID PRIMARY KEY,
    roster_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    shift_template_id UUID NOT NULL,
    start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE INDEX idx_roster_assignments_roster_id ON roster_assignments (roster_id);
CREATE INDEX idx_roster_assignments_employee_id ON roster_assignments (employee_id);

CREATE TABLE attendance_sessions (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    roster_id UUID NOT NULL,
    shift_template_id UUID NOT NULL,
    clock_in_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    clock_out_at TIMESTAMP(6) WITH TIME ZONE,
    active BOOLEAN NOT NULL
);
CREATE INDEX idx_attendance_sessions_employee_id ON attendance_sessions (employee_id);
CREATE INDEX idx_attendance_sessions_active ON attendance_sessions (active);

CREATE TABLE attendance_corrections (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    attendance_id UUID NOT NULL,
    correction_type VARCHAR(40) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE approval_flows (
    request_id UUID PRIMARY KEY,
    request_type VARCHAR(50) NOT NULL,
    subject_id UUID NOT NULL,
    registrar VARCHAR(255),
    open BOOLEAN NOT NULL
);

CREATE TABLE approval_actions (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    actor_id VARCHAR(255),
    decided_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    reason VARCHAR(1000)
);
CREATE INDEX idx_approval_actions_request_id ON approval_actions (request_id);

CREATE TABLE handovers (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    items TEXT NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    resource_id UUID NOT NULL,
    details TEXT NOT NULL,
    timestamp TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE shift_swap_requests (
    id UUID PRIMARY KEY,
    requesting_employee_id UUID NOT NULL,
    target_employee_id UUID NOT NULL,
    offered_date DATE NOT NULL,
    requested_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    organization_id UUID,
    aggregate_type VARCHAR(100),
    aggregate_id UUID,
    actor_id VARCHAR(255),
    correlation_id VARCHAR(100),
    causation_id VARCHAR(100),
    payload TEXT,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    last_error VARCHAR(2000)
);
CREATE INDEX idx_outbox_events_status ON outbox_events (status);