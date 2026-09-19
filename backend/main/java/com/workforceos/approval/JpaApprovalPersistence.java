package com.workforceos.approval;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_flows")
class ApprovalFlowEntity {

    @Id
    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "registrar")
    private String registrar;

    @Column(nullable = false)
    private boolean open;

    protected ApprovalFlowEntity() {
    }

    ApprovalFlowEntity(ApprovalEngine.ApprovalFlow flow) {
        this.requestId = flow.requestId();
        this.organizationId = flow.organizationId();
        this.requestType = flow.requestType();
        this.subjectId = flow.subjectId();
        this.registrar = flow.registrar();
        this.open = flow.open();
    }

    ApprovalEngine.ApprovalFlow toRecord() {
        return new ApprovalEngine.ApprovalFlow(organizationId, requestId, requestType, subjectId, registrar, open);
    }
}

interface ApprovalFlowRepository extends JpaRepository<ApprovalFlowEntity, UUID> {
}

@Entity
@Table(name = "approval_actions")
class ApprovalActionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @Column(name = "reason")
    private String reason;

    protected ApprovalActionEntity() {
    }

    ApprovalActionEntity(ApprovalAction action) {
        this.id = UUID.randomUUID();
        this.organizationId = action.organizationId();
        this.requestId = action.requestId();
        this.requestType = action.requestType();
        this.decision = action.decision();
        this.actorId = action.actorId();
        this.decidedAt = action.decidedAt();
        this.reason = action.reason();
    }

    ApprovalAction toRecord() {
        return new ApprovalAction(organizationId, requestId, requestType, decision, actorId, decidedAt, reason);
    }
}

interface ApprovalActionRepository extends JpaRepository<ApprovalActionEntity, UUID> {

    List<ApprovalActionEntity> findAllByRequestIdOrderByDecidedAtAsc(UUID requestId);

    List<ApprovalActionEntity> findAllByOrderByDecidedAtAsc();
}

@Component
@Profile("!test")
class JpaApprovalStore implements ApprovalStore {

    private final ApprovalFlowRepository flowRepository;
    private final ApprovalActionRepository actionRepository;

    JpaApprovalStore(ApprovalFlowRepository flowRepository, ApprovalActionRepository actionRepository) {
        this.flowRepository = flowRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public Optional<ApprovalEngine.ApprovalFlow> findFlow(UUID requestId) {
        return flowRepository.findById(requestId).map(ApprovalFlowEntity::toRecord);
    }

    @Override
    public void saveFlow(ApprovalEngine.ApprovalFlow flow) {
        flowRepository.save(new ApprovalFlowEntity(flow));
    }

    @Override
    public List<ApprovalAction> findHistory(UUID requestId) {
        return actionRepository.findAllByRequestIdOrderByDecidedAtAsc(requestId).stream()
                .map(ApprovalActionEntity::toRecord)
                .toList();
    }

    @Override
    public List<ApprovalAction> findAllHistory() {
        return actionRepository.findAllByOrderByDecidedAtAsc().stream()
                .map(ApprovalActionEntity::toRecord)
                .toList();
    }

    @Override
    public void saveAction(ApprovalAction action) {
        actionRepository.save(new ApprovalActionEntity(action));
    }
}