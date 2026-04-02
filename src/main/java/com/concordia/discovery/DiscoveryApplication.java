package com.concordia.discovery;

import java.io.IOException;
import java.nio.file.Path;

public final class DiscoveryApplication {
    private DiscoveryApplication() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path csvPath = args.length > 1 ? Path.of(args[1]) : Path.of("data", "capstones.csv");

        ProjectService projectService = ProjectService.fromCsv(csvPath);
        CapstoneDiscoveryServer server = new CapstoneDiscoveryServer(port, projectService);
        server.start();

        System.out.println("Capstone Discovery running at " + server.baseUrl() + "/search");
        System.out.println("Using data source " + csvPath.toAbsolutePath());
    }
}
