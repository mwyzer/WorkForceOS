package com.workforceos.scheduling;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShiftTemplateService {

    private final ConcurrentMap<UUID, ShiftTemplate> shifts = new ConcurrentHashMap<>();

    public List<ShiftTemplate> findAll() {
        return shifts.values().stream().toList();
    }

    public ShiftTemplate findById(UUID id) {
        ShiftTemplate shift = shifts.get(id);
        if (shift == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift template not found");
        }
        return shift;
    }

    public ShiftTemplate create(ShiftTemplateRequest request) {
        validate(request);
        ShiftTemplate shift = toShift(UUID.randomUUID(), request);
        shifts.put(shift.id(), shift);
        return shift;
    }

    public ShiftTemplate update(UUID id, ShiftTemplateRequest request) {
        findById(id);
        validate(request);
        ShiftTemplate shift = toShift(id, request);
        shifts.put(id, shift);
        return shift;
    }

    private ShiftTemplate toShift(UUID id, ShiftTemplateRequest request) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        return new ShiftTemplate(
                id,
                request.organizationId(),
                request.name().trim(),
                start,
                end,
                !end.isAfter(start),
                request.breaks() == null ? List.of() : List.copyOf(request.breaks()),
                true);
    }

    private void validate(ShiftTemplateRequest request) {
        if (request == null
                || request.organizationId() == null
                || request.name() == null
                || request.name().isBlank()
                || request.startTime() == null
                || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift name, organization, start, and end are required");
        }
        if (request.startTime().equals(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift start and end cannot be equal");
        }
        if (request.breaks() != null) {
            boolean invalidBreak = request.breaks().stream()
                    .anyMatch(breakPeriod -> breakPeriod == null
                            || breakPeriod.start() == null
                            || breakPeriod.end() == null
                            || breakPeriod.start().equals(breakPeriod.end()));
            if (invalidBreak) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Break periods require different start and end times");
            }
        }
    }
}
