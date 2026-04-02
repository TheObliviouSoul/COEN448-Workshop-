package com.concordia.discovery.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.concordia.discovery.CapstoneDiscoveryServer;
import com.concordia.discovery.ProjectService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.nio.file.Path;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class CapstoneSteps {
    private static final Path CSV_PATH = Path.of("data", "capstones.csv");

    private CapstoneDiscoveryServer server;
    private ProjectService projectService;
    private WebDriver driver;

    @Before
    public void setUp() throws IOException {
        projectService = ProjectService.fromCsv(CSV_PATH);
        server = new CapstoneDiscoveryServer(0, projectService);
        server.start();
        driver = new HtmlUnitDriver(true);
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Given("the project {string} exists in the registry")
    public void verifyDataExists(String id) {
        assertTrue(projectService.getProjectById(id).isPresent());
    }

    @When("I open the search page")
    public void openSearchPage() {
        driver.get(server.baseUrl() + "/search");
    }

    @When("I open the registry page")
    public void openRegistryPage() {
        driver.get(server.baseUrl() + "/registry");
    }

    @When("I enter {string} into the search interface")
    public void performSearch(String id) {
        openSearchPage();
        WebElement searchInput = driver.findElement(By.id("search-input"));
        searchInput.clear();
        searchInput.sendKeys(id);
        driver.findElement(By.id("search-btn")).click();
    }

    @Then("the browser should display location {string}")
    public void verifyLocation(String expectedLocation) {
        String actualLocation = driver.findElement(By.id("location-val")).getText();
        assertEquals(expectedLocation, actualLocation);
    }

    @Then("the browser should display scheduled time {string}")
    public void verifyScheduledTime(String expectedScheduledTime) {
        String actualScheduledTime = driver.findElement(By.id("time-val")).getText();
        assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Then("the abstract should contain {string}")
    public void verifyAbstract(String expectedFragment) {
        String abstractText = driver.findElement(By.id("abstract-val")).getText();
        assertTrue(abstractText.contains(expectedFragment));
    }

    @Then("the page should say {string}")
    public void verifyStatusMessage(String expectedMessage) {
        String actualMessage = driver.findElement(By.id("status-message")).getText();
        assertEquals(expectedMessage, actualMessage);
    }

    @Then("the registry should list project {string}")
    public void verifyRegistryContainsProject(String expectedText) {
        assertTrue(driver.getPageSource().contains(expectedText));
    }
}
