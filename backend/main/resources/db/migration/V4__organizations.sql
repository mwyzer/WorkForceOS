CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

INSERT INTO organizations (id, name, timezone, active, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'UTC', TRUE, CURRENT_TIMESTAMP);

-- Departments already carry organization_id; teams and employees derive it from their
-- department. Adding the discriminator explicitly makes tenant scoping possible at the
-- persistence layer regardless of the graph traversal.
ALTER TABLE teams ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_teams_organization_id ON teams (organization_id);

ALTER TABLE employees ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_employees_organization_id ON employees (organization_id);

ALTER TABLE user_accounts ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_user_accounts_organization_id ON user_accounts (organization_id);

ALTER TABLE leave_requests ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_leave_requests_organization_id ON leave_requests (organization_id);

ALTER TABLE overtime_requests ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_overtime_requests_organization_id ON overtime_requests (organization_id);

ALTER TABLE attendance_sessions ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_attendance_sessions_organization_id ON attendance_sessions (organization_id);

ALTER TABLE attendance_corrections ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_attendance_corrections_organization_id ON attendance_corrections (organization_id);

ALTER TABLE approval_flows ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
ALTER TABLE approval_actions ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_approval_actions_organization_id ON approval_actions (organization_id);

ALTER TABLE handovers ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_handovers_organization_id ON handovers (organization_id);

ALTER TABLE notifications ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_notifications_organization_id ON notifications (organization_id);

ALTER TABLE audit_logs ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs (organization_id);

ALTER TABLE shift_swap_requests ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_shift_swap_requests_organization_id ON shift_swap_requests (organization_id);

ALTER TABLE roster_assignments ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_roster_assignments_organization_id ON roster_assignments (organization_id);

ALTER TABLE risk_assessments ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
ALTER TABLE risk_recommendations ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
ALTER TABLE risk_alerts ADD COLUMN organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001' NOT NULL REFERENCES organizations (id);
CREATE INDEX idx_risk_assessments_organization_id ON risk_assessments (organization_id);
CREATE INDEX idx_risk_alerts_organization_id ON risk_alerts (organization_id);

-- Outbox events carry an optional organization_id column from V3; promote it to a
-- foreign-keyed tenant discriminator and index it.
ALTER TABLE outbox_events ADD CONSTRAINT fk_outbox_events_organization_id FOREIGN KEY (organization_id) REFERENCES organizations (id);
CREATE INDEX idx_outbox_events_organization_id ON outbox_events (organization_id);