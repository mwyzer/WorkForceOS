package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class TeamService {

    private final ConcurrentMap<UUID, Team> teams = new ConcurrentHashMap<>();
    private final DepartmentService departmentService;

    public TeamService(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    public List<Team> findAll() {
        return teams.values().stream().toList();
    }

    public Team findById(UUID id) {
        Team team = teams.get(id);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
        }
        return team;
    }

    public Team create(TeamRequest request) {
        validate(request);

        Team team = new Team(UUID.randomUUID(), request.departmentId(), request.name().trim(), true);
        teams.put(team.id(), team);
        return team;
    }

    public Team deactivate(UUID id) {
        Team team = findById(id);
        Team deactivated = new Team(team.id(), team.departmentId(), team.name(), false);
        teams.put(id, deactivated);
        return deactivated;
    }

    private void validate(TeamRequest request) {
        if (request == null || request.departmentId() == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department and team name are required");
        }
        departmentService.findById(request.departmentId());
    }
}
