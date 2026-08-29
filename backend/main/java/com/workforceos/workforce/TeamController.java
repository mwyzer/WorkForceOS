package com.workforceos.workforce;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams")
    public List<Team> findAll() {
        return teamService.findAll();
    }

    @GetMapping("/teams/{id}")
    public Team findById(@PathVariable UUID id) {
        return teamService.findById(id);
    }

    @PostMapping("/teams")
    public ResponseEntity<Team> create(@RequestBody TeamRequest request) {
        Team team = teamService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/teams/" + team.id())).body(team);
    }

    @DeleteMapping("/teams/{id}")
    public Team deactivate(@PathVariable UUID id) {
        return teamService.deactivate(id);
    }
}
