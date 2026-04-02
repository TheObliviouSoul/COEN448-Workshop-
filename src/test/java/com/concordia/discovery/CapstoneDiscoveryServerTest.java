package com.concordia.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CapstoneDiscoveryServerTest {
    @Test
    void rendersRegistryPage() throws IOException, InterruptedException {
        ProjectService service = ProjectService.fromCsv(Path.of("data", "capstones.csv"));
        CapstoneDiscoveryServer server = new CapstoneDiscoveryServer(0, service);
        server.start();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.baseUrl() + "/registry")).GET().build();
            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("C-2026-01"));
            assertTrue(response.body().contains("Open in search view"));
            assertTrue(response.body().contains("Browse every capstone in the discovery index."));
        } finally {
            server.stop();
        }
    }
}
