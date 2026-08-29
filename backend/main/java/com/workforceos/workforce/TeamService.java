package com.workforceos.workforce;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentService departmentService;

    public TeamService(TeamRepository teamRepository, DepartmentService departmentService) {
        this.teamRepository = teamRepository;
        this.departmentService = departmentService;
    }

    @Cacheable("teams")
    public List<Team> findAll() {
        return teamRepository.findAll().stream().map(TeamEntity::toRecord).toList();
    }

    @Cacheable(value = "team", key = "#id")
    public Team findById(UUID id) {
        return teamRepository.findById(id)
                .map(TeamEntity::toRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    @CacheEvict(value = "teams", allEntries = true)
    public Team create(TeamRequest request) {
        validate(request);

        TeamEntity team = new TeamEntity(UUID.randomUUID(), request.departmentId(), request.name().trim(), true);
        teamRepository.save(team);
        return team.toRecord();
    }

    @Caching(evict = {
            @CacheEvict(value = "teams", allEntries = true),
            @CacheEvict(value = "team", key = "#id")
    })
    public Team deactivate(UUID id) {
        TeamEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        team.deactivate();
        teamRepository.save(team);
        return team.toRecord();
    }

    private void validate(TeamRequest request) {
        if (request == null || request.departmentId() == null || ValidationUtils.isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department and team name are required");
        }
        departmentService.findById(request.departmentId());
    }
}
