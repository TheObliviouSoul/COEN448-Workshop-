package com.concordia.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectRegistryTest {
    @Test
    void loadsProjectsFromCsv() throws IOException {
        ProjectRegistry registry = ProjectRegistry.load(Path.of("data", "capstones.csv"));

        Project project = registry.findById("C-2026-01").orElseThrow();

        assertEquals("EV Building 3.101", project.location());
        assertEquals("Tuesday 10:15 AM", project.scheduledTime());
        assertTrue(project.abstractText().contains("LKW criteria"));
    }

    @Test
    void failsFastOnMalformedRows(@TempDir Path tempDir) throws IOException {
        Path malformedCsv = tempDir.resolve("malformed.csv");
        Files.writeString(
                malformedCsv,
                """
                id,title,location,scheduledTime,abstractText
                C-2026-99,Incomplete Row,EV Building 3.101,Thursday 11:00 AM
                """);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> ProjectRegistry.load(malformedCsv));

        assertTrue(error.getMessage().contains("line 2"));
    }

    @Test
    void preservesCsvOrderForBrowsing() throws IOException {
        ProjectRegistry registry = ProjectRegistry.load(Path.of("data", "capstones.csv"));

        List<String> ids = registry.findAll().stream().map(Project::id).toList();

        assertEquals(List.of("C-2026-01", "C-2026-02", "C-2026-03"), ids);
    }
}
