package com.concordia.discovery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectServiceTest {
    @Test
    void returnsEmptyWhenProjectIsMissing() throws IOException {
        ProjectService service = ProjectService.fromCsv(Path.of("data", "capstones.csv"));

        assertTrue(service.getProjectById("C-9999-99").isEmpty());
    }
}
