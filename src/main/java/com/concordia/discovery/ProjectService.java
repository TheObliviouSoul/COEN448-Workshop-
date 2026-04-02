package com.concordia.discovery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ProjectService {
    private final ProjectRegistry registry;

    public ProjectService(ProjectRegistry registry) {
        this.registry = registry;
    }

    public static ProjectService fromCsv(Path csvPath) throws IOException {
        return new ProjectService(ProjectRegistry.load(csvPath));
    }

    public Optional<Project> getProjectById(String id) {
        return registry.findById(id);
    }

    public List<Project> getAllProjects() {
        return registry.findAll();
    }
}
