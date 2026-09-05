package com.workforceos.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ApprovalEngineTests {

    private final ApprovalEngine engine = new ApprovalEngine();

    @Test
    void approvesAndRecordsHistoryForAnyRequestType() {
        UUID leaveId = UUID.randomUUID();
        UUID overtimeId = UUID.randomUUID();

        engine.register(leaveId, "LEAVE", UUID.randomUUID(), "employee.alice");
        engine.register(overtimeId, "OVERTIME", UUID.randomUUID(), "employee.alice");

        engine.decide(leaveId, ApprovalDecision.APPROVED, "manager.bob", "Approved");
        engine.decide(overtimeId, ApprovalDecision.REJECTED, "manager.bob", "Not needed");

        assertEquals(1, engine.history(leaveId).size());
        assertEquals(2, engine.findAllHistory().size());
        assertEquals(ApprovalDecision.APPROVED, engine.history(leaveId).getFirst().decision());
        assertEquals("manager.bob", engine.history(leaveId).getFirst().actorId());
    }

    @Test
    void rejectsDecidingAnAlreadyDecidedRequest() {
        UUID id = UUID.randomUUID();
        engine.register(id, "LEAVE", UUID.randomUUID(), "employee.alice");
        engine.decide(id, ApprovalDecision.APPROVED, "manager.bob", null);

        assertThrows(ResponseStatusException.class,
                () -> engine.decide(id, ApprovalDecision.REJECTED, "manager.carol", null));
    }

    @Test
    void rejectsRequestsUnknownToTheEngine() {
        assertThrows(ResponseStatusException.class,
                () -> engine.decide(UUID.randomUUID(), ApprovalDecision.APPROVED, "manager.bob", null));
    }

    @Test
    void rejectsSelfApprovalByTheRegistrar() {
        UUID id = UUID.randomUUID();
        engine.register(id, "LEAVE", UUID.randomUUID(), "employee.alice");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> engine.decide(id, ApprovalDecision.APPROVED, "EMPLOYEE.ALICE", null));
        assertEquals(409, exception.getStatusCode().value());
    }
}