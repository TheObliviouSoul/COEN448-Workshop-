Feature: Capstone Discovery Workflow
  Scenario: Successful metadata retrieval
    Given the project "C-2026-01" exists in the registry
    When I enter "C-2026-01" into the search interface
    Then the browser should display location "EV Building 3.101"
    And the browser should display scheduled time "Tuesday 10:15 AM"
    And the abstract should contain "LKW criteria"

  Scenario: Missing project lookup
    When I enter "C-9999-99" into the search interface
    Then the page should say "Project \"C-9999-99\" was not found."

  Scenario: Empty search landing state
    When I open the search page
    Then the page should say "Enter a project ID to load location, schedule, and abstract details."

  Scenario: Registry browsing
    When I open the registry page
    Then the registry should list project "C-2026-02"
    And the registry should list project "Mutation Analysis Workbench"
