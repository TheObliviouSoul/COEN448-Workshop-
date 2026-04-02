package com.concordia.discovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ProjectRegistry {
    private static final int EXPECTED_COLUMN_COUNT = 5;

    private final Map<String, Project> projectMap;
    private final List<Project> projects;

    private ProjectRegistry(Map<String, Project> projectMap) {
        LinkedHashMap<String, Project> orderedProjects = new LinkedHashMap<>(projectMap);
        this.projectMap = Collections.unmodifiableMap(orderedProjects);
        this.projects = List.copyOf(orderedProjects.values());
    }

    public static ProjectRegistry load(Path csvPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("CSV file is empty: " + csvPath);
            }

            Map<String, Project> projects = new LinkedHashMap<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length != EXPECTED_COLUMN_COUNT) {
                    throw new IllegalArgumentException(
                            "Malformed CSV row at line " + lineNumber + ": " + line);
                }

                Project project = new Project(
                        columns[0].trim(),
                        columns[1].trim(),
                        columns[2].trim(),
                        columns[3].trim(),
                        columns[4].trim());

                if (project.id().isBlank()) {
                    throw new IllegalArgumentException("Missing project ID at line " + lineNumber);
                }

                projects.put(project.id(), project);
            }

            return new ProjectRegistry(projects);
        }
    }

    public Optional<Project> findById(String id) {
        return Optional.ofNullable(projectMap.get(id));
    }

    public List<Project> findAll() {
        return projects;
    }
}
