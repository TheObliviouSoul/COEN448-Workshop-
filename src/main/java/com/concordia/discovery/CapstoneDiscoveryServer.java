package com.concordia.discovery;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CapstoneDiscoveryServer {
    private final HttpServer server;
    private final ProjectService projectService;

    public CapstoneDiscoveryServer(int port, ProjectService projectService) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.projectService = projectService;
        this.server.createContext("/", this::handleRootRedirect);
        this.server.createContext("/search", this::handleSearch);
        this.server.createContext("/registry", this::handleRegistry);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String baseUrl() {
        return "http://localhost:" + port();
    }

    private void handleRootRedirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/search");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        Map<String, String> queryParameters = parseQuery(exchange.getRequestURI().getRawQuery());
        String requestedId = queryParameters.getOrDefault("id", "").trim();
        Optional<Project> project = requestedId.isEmpty()
                ? Optional.empty()
                : projectService.getProjectById(requestedId);

        sendHtml(exchange, renderPage(requestedId, project, false));
    }

    private void handleRegistry(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        sendHtml(exchange, renderPage("", Optional.empty(), true));
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private String renderPage(String requestedId, Optional<Project> project, boolean showRegistry) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Capstone Discovery</title>
                  <style>
                    :root {
                      --ink: #17313d;
                      --muted: #5f7078;
                      --teal: #0f766e;
                      --teal-deep: #115e59;
                      --coral: #d96c47;
                      --sun: #f2c14e;
                      --paper: rgba(255, 250, 241, 0.82);
                      --panel-border: rgba(255, 255, 255, 0.58);
                      --shadow: rgba(23, 49, 61, 0.17);
                    }

                    * { box-sizing: border-box; }

                    body {
                      margin: 0;
                      min-height: 100vh;
                      font-family: "Trebuchet MS", "Gill Sans", sans-serif;
                      line-height: 1.5;
                      color: var(--ink);
                      background:
                        radial-gradient(circle at top left, rgba(242, 193, 78, 0.62) 0%%, transparent 32%%),
                        linear-gradient(135deg, #fff8ee 0%%, #f6efdf 42%%, #d7eef0 100%%);
                      overflow-x: hidden;
                      position: relative;
                    }

                    body::before,
                    body::after {
                      content: "";
                      position: fixed;
                      width: 24rem;
                      height: 24rem;
                      border-radius: 999px;
                      filter: blur(28px);
                      opacity: 0.48;
                      z-index: 0;
                      pointer-events: none;
                      animation: drift 15s ease-in-out infinite;
                    }

                    body::before {
                      top: -7rem;
                      right: -5rem;
                      background: rgba(15, 118, 110, 0.24);
                    }

                    body::after {
                      bottom: -8rem;
                      left: -4rem;
                      background: rgba(217, 108, 71, 0.22);
                      animation-delay: -7s;
                    }

                    .backdrop-grid {
                      position: fixed;
                      inset: 0;
                      z-index: 0;
                      pointer-events: none;
                      background-image:
                        linear-gradient(rgba(23, 49, 61, 0.06) 1px, transparent 1px),
                        linear-gradient(90deg, rgba(23, 49, 61, 0.06) 1px, transparent 1px);
                      background-size: 30px 30px;
                      mask-image: radial-gradient(circle at center, black 38%%, transparent 82%%);
                      opacity: 0.35;
                      animation: gridPulse 10s ease-in-out infinite;
                    }

                    .shell {
                      position: relative;
                      z-index: 1;
                      max-width: 70rem;
                      margin: 0 auto;
                      padding: 4rem 1.5rem 5rem;
                    }

                    .panel,
                    .result,
                    .status-card {
                      position: relative;
                      overflow: hidden;
                      border: 1px solid var(--panel-border);
                      border-radius: 28px;
                      background: var(--paper);
                      backdrop-filter: blur(14px);
                      box-shadow: 0 24px 60px var(--shadow);
                      animation: riseIn 0.85s cubic-bezier(0.2, 0.9, 0.2, 1) both;
                    }

                    .panel::before,
                    .result::before,
                    .status-card::before {
                      content: "";
                      position: absolute;
                      inset: 0 auto auto 0;
                      width: 100%%;
                      height: 6px;
                      background: linear-gradient(90deg, var(--teal) 0%%, var(--sun) 48%%, var(--coral) 100%%);
                    }

                    .hero {
                      padding: 2rem;
                      margin-bottom: 1.25rem;
                    }

                    .eyebrow {
                      margin: 0;
                      font-size: 0.78rem;
                      font-weight: 700;
                      letter-spacing: 0.18em;
                      text-transform: uppercase;
                      color: var(--teal);
                    }

                    h1 {
                      margin: 0.45rem 0 0.9rem;
                      max-width: 10ch;
                      font-family: Georgia, "Times New Roman", serif;
                      font-size: clamp(2.7rem, 5vw, 5rem);
                      line-height: 0.94;
                    }

                    .hero-copy {
                      max-width: 44rem;
                      margin: 0;
                      font-size: 1.08rem;
                      color: #294550;
                    }

                    .top-actions {
                      margin-top: 1rem;
                    }

                    .search-panel {
                      padding: 1.25rem;
                      animation-delay: 0.08s;
                    }

                    form {
                      display: grid;
                      grid-template-columns: minmax(0, 0.9fr) minmax(16rem, 1.25fr) auto;
                      gap: 1rem;
                      align-items: end;
                    }

                    .field-stack {
                      display: grid;
                      gap: 0.28rem;
                    }

                    label {
                      font-size: 0.76rem;
                      font-weight: 700;
                      letter-spacing: 0.12em;
                      text-transform: uppercase;
                      color: var(--ink);
                    }

                    .helper {
                      font-size: 0.92rem;
                      color: var(--muted);
                    }

                    input {
                      width: 100%%;
                      min-width: 0;
                      padding: 0.95rem 1rem;
                      border: 1px solid rgba(23, 49, 61, 0.14);
                      border-radius: 18px;
                      background: rgba(255, 255, 255, 0.74);
                      font: inherit;
                      color: var(--ink);
                      box-shadow: inset 0 1px 2px rgba(23, 49, 61, 0.06);
                      transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
                    }

                    input::placeholder {
                      color: #8ca0a5;
                    }

                    input:focus {
                      outline: none;
                      border-color: rgba(15, 118, 110, 0.7);
                      transform: translateY(-1px);
                      box-shadow:
                        0 0 0 4px rgba(15, 118, 110, 0.12),
                        inset 0 1px 2px rgba(23, 49, 61, 0.06);
                    }

                    button {
                      padding: 0.98rem 1.35rem;
                      border: none;
                      border-radius: 18px;
                      background: linear-gradient(135deg, var(--teal) 0%%, #149489 100%%);
                      color: white;
                      font: inherit;
                      font-weight: 700;
                      letter-spacing: 0.02em;
                      cursor: pointer;
                      box-shadow: 0 16px 28px rgba(15, 118, 110, 0.28);
                      transition: transform 180ms ease, box-shadow 180ms ease, filter 180ms ease;
                    }

                    button:hover {
                      transform: translateY(-2px) scale(1.01);
                      box-shadow: 0 20px 34px rgba(15, 118, 110, 0.32);
                      filter: saturate(1.05);
                    }

                    button:active {
                      transform: translateY(0);
                    }

                    .ghost-link,
                    .inline-link {
                      color: var(--teal-deep);
                      font-weight: 700;
                      text-decoration: none;
                    }

                    .ghost-link:hover,
                    .inline-link:hover {
                      color: var(--teal);
                    }

                    .status-card {
                      max-width: 38rem;
                      margin-top: 1.25rem;
                      padding: 1rem 1.2rem;
                      animation-delay: 0.16s;
                    }

                    .status-card:not(.missing) {
                      border-left: 6px solid var(--sun);
                    }

                    .status-card.missing {
                      border-left: 6px solid var(--coral);
                      background: rgba(255, 243, 237, 0.92);
                      color: #8f341b;
                    }

                    .result {
                      margin-top: 1.25rem;
                      padding: 1.5rem;
                      animation-delay: 0.16s;
                    }

                    .result::after {
                      content: "";
                      position: absolute;
                      inset: 0;
                      background: linear-gradient(110deg, transparent 0%%, rgba(255, 255, 255, 0.42) 48%%, transparent 100%%);
                      transform: translateX(-130%%);
                      animation: sweepAcross 1.5s ease 0.32s both;
                      pointer-events: none;
                    }

                    .result-header {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: 1rem;
                      flex-wrap: wrap;
                      margin-bottom: 0.9rem;
                    }

                    .chip,
                    .project-id {
                      margin: 0;
                    }

                    .chip {
                      display: inline-flex;
                      align-items: center;
                      padding: 0.35rem 0.75rem;
                      border-radius: 999px;
                      background: rgba(242, 193, 78, 0.28);
                      color: #8a6110;
                      font-size: 0.76rem;
                      font-weight: 700;
                      letter-spacing: 0.08em;
                      text-transform: uppercase;
                    }

                    .project-id {
                      font-family: "Courier New", monospace;
                      font-size: 0.95rem;
                      font-weight: 700;
                      letter-spacing: 0.12em;
                      color: var(--teal-deep);
                    }

                    #project-title {
                      margin: 0;
                      font-family: Georgia, "Times New Roman", serif;
                      font-size: clamp(1.8rem, 3vw, 2.7rem);
                      line-height: 1.02;
                    }

                    .metrics {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 1rem;
                      margin: 1.25rem 0 1rem;
                    }

                    .metric,
                    .abstract-card {
                      padding: 1rem 1.15rem;
                      border-radius: 20px;
                      border: 1px solid rgba(23, 49, 61, 0.08);
                      background: rgba(255, 255, 255, 0.68);
                      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
                      animation: cardFloat 0.7s ease both;
                    }

                    .metric:nth-child(2) {
                      animation-delay: 0.08s;
                    }

                    .abstract-card {
                      animation-delay: 0.16s;
                    }

                    .metric-label {
                      display: block;
                      margin-bottom: 0.45rem;
                      font-size: 0.74rem;
                      font-weight: 700;
                      letter-spacing: 0.11em;
                      text-transform: uppercase;
                      color: var(--muted);
                    }

                    .metric-value {
                      font-size: 1.1rem;
                      font-weight: 700;
                    }

                    #abstract-val {
                      margin: 0;
                      font-size: 1rem;
                      color: #233e49;
                    }

                    .result-actions {
                      margin-top: 1rem;
                    }

                    .registry-grid {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
                      gap: 1rem;
                      margin-top: 1.25rem;
                    }

                    .registry-card {
                      position: relative;
                      overflow: hidden;
                      border: 1px solid var(--panel-border);
                      border-radius: 24px;
                      background: rgba(255, 255, 255, 0.72);
                      padding: 1.25rem;
                      box-shadow: 0 18px 34px rgba(23, 49, 61, 0.12);
                      animation: riseIn 0.85s cubic-bezier(0.2, 0.9, 0.2, 1) both;
                    }

                    .registry-card::before {
                      content: "";
                      position: absolute;
                      inset: 0 auto auto 0;
                      width: 100%%;
                      height: 5px;
                      background: linear-gradient(90deg, var(--teal) 0%%, var(--sun) 48%%, var(--coral) 100%%);
                    }

                    .registry-card h3 {
                      margin: 0.55rem 0 0.7rem;
                      font-family: Georgia, "Times New Roman", serif;
                      font-size: 1.5rem;
                      line-height: 1.02;
                    }

                    .registry-meta {
                      margin: 0 0 0.7rem;
                      font-weight: 700;
                      color: var(--teal-deep);
                    }

                    .registry-abstract {
                      margin: 0 0 1rem;
                      color: #233e49;
                    }

                    @keyframes riseIn {
                      from {
                        opacity: 0;
                        transform: translateY(26px) scale(0.985);
                      }
                      to {
                        opacity: 1;
                        transform: translateY(0) scale(1);
                      }
                    }

                    @keyframes cardFloat {
                      from {
                        opacity: 0;
                        transform: translateY(18px);
                      }
                      to {
                        opacity: 1;
                        transform: translateY(0);
                      }
                    }

                    @keyframes drift {
                      0%%, 100%% {
                        transform: translate3d(0, 0, 0) scale(1);
                      }
                      50%% {
                        transform: translate3d(0, 22px, 0) scale(1.08);
                      }
                    }

                    @keyframes gridPulse {
                      0%%, 100%% {
                        opacity: 0.24;
                      }
                      50%% {
                        opacity: 0.38;
                      }
                    }

                    @keyframes sweepAcross {
                      from {
                        transform: translateX(-130%%);
                      }
                      to {
                        transform: translateX(130%%);
                      }
                    }

                    @media (max-width: 760px) {
                      .shell {
                        padding: 3rem 1rem 4rem;
                      }

                      .hero,
                      .search-panel,
                      .result,
                      .registry-card {
                        padding: 1.2rem;
                      }

                      form {
                        grid-template-columns: 1fr;
                      }

                      .metrics {
                        grid-template-columns: 1fr;
                      }

                      h1 {
                        max-width: none;
                      }

                      button {
                        width: 100%%;
                      }
                    }
                  </style>
                </head>
                <body>
                  <div class="backdrop-grid" aria-hidden="true"></div>
                  <main class="shell">
                    <section class="panel hero">
                      <p class="eyebrow">%s</p>
                      <h1>%s</h1>
                      <p class="hero-copy">
                        %s
                      </p>
                    </section>
                    <section class="panel search-panel">
                      <form action="/search" method="get">
                        <div class="field-stack">
                          <label for="search-input">Project ID</label>
                          <span class="helper">Try <strong>C-2026-01</strong> for the lecture-note scenario.</span>
                        </div>
                        <input id="search-input" name="id" value="%s" placeholder="e.g. C-2026-01" autocomplete="off" />
                        <button id="search-btn" type="submit">Inspect Record</button>
                      </form>
                      <div class="top-actions">
                        <a class="ghost-link" href="%s">%s</a>
                      </div>
                    </section>
                """.formatted(
                showRegistry ? "Registry Browser" : "Behavioral and Structural Verification",
                showRegistry ? "Browse every capstone in the discovery index." : "Trace a capstone from ID to evidence.",
                showRegistry
                        ? "Review every record in the flat-file source, then jump back to the focused search view when you need a single project."
                        : "Search the registry by project ID and watch the metadata surface from the flat-file source into the browser.",
                escapeHtml(requestedId),
                showRegistry ? "/search" : "/registry",
                showRegistry ? "Return to focused search" : "Browse all projects"));

        if (showRegistry) {
            html.append(buildRegistryCards(projectService.getAllProjects()));
        } else if (requestedId.isEmpty()) {
            html.append("""
                    <div id="status-message" class="status-card">
                      Enter a project ID to load location, schedule, and abstract details.
                    </div>
                    """);
        } else if (project.isPresent()) {
            Project value = project.get();
            html.append("""
                    <section class="result">
                      <div class="result-header">
                        <p class="chip">Registry Match</p>
                        <p class="project-id">%s</p>
                      </div>
                      <h2 id="project-title">%s</h2>
                      <div class="metrics">
                        <article class="metric">
                          <span class="metric-label">Location</span>
                          <span id="location-val" class="metric-value">%s</span>
                        </article>
                        <article class="metric">
                          <span class="metric-label">Scheduled Time</span>
                          <span id="time-val" class="metric-value">%s</span>
                        </article>
                      </div>
                      <article class="abstract-card">
                        <span class="metric-label">Research Abstract</span>
                        <p id="abstract-val">%s</p>
                      </article>
                      <div class="result-actions">
                        <a class="inline-link" href="/registry">Compare with the rest of the registry</a>
                      </div>
                    </section>
                    """.formatted(
                    escapeHtml(value.id()),
                    escapeHtml(value.title()),
                    escapeHtml(value.location()),
                    escapeHtml(value.scheduledTime()),
                    escapeHtml(value.abstractText())));
        } else {
            html.append("""
                    <div id="status-message" class="status-card missing">
                      Project "%s" was not found.
                    </div>
                    """.formatted(escapeHtml(requestedId)));
        }

        html.append("""
                  </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String buildRegistryCards(List<Project> projects) {
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"registry-grid\">");

        for (Project project : projects) {
            html.append("""
                    <article class="registry-card">
                      <p class="project-id">""");
            html.append(escapeHtml(project.id()));
            html.append("""
                    </p>
                      <h3>""");
            html.append(escapeHtml(project.title()));
            html.append("""
                    </h3>
                      <p class="registry-meta">""");
            html.append(escapeHtml(project.location()));
            html.append(" · ");
            html.append(escapeHtml(project.scheduledTime()));
            html.append("""
                    </p>
                      <p class="registry-abstract">""");
            html.append(escapeHtml(project.abstractText()));
            html.append("""
                    </p>
                      <a class="inline-link" href="/search?id=""");
            html.append(project.id());
            html.append("""
                    ">Open in search view</a>
                    </article>
                    """);
        }

        html.append("</section>");
        return html.toString();
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }

        for (String pair : rawQuery.split("&")) {
            String[] segments = pair.split("=", 2);
            String key = decode(segments[0]);
            String value = segments.length > 1 ? decode(segments[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
