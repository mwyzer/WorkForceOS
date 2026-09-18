CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY,
    risk_type VARCHAR(40) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    score INT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    team_id UUID,
    department_id UUID,
    window_start TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    impact_minutes BIGINT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    evidence TEXT NOT NULL,
    dedupe_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_risk_assessments_dedupe_key UNIQUE (dedupe_key)
);

CREATE TABLE risk_recommendations (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES risk_assessments (id),
    risk_type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    action_endpoint VARCHAR(255) NOT NULL
);

CREATE TABLE risk_alerts (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES risk_assessments (id),
    severity VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX idx_risk_assessments_created_at ON risk_assessments (created_at);
CREATE INDEX idx_risk_alerts_status ON risk_alerts (status);