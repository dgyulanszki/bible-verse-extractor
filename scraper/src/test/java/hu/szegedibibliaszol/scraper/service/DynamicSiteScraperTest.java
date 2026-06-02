package hu.szegedibibliaszol.scraper.service;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSiteScraperTest {

    @Test
    void scrapeReturnsEmptyListUntilImplementationExists() {
        DynamicSiteScraper dynamicSiteScraper = new DynamicSiteScraper();

        assertTrue(dynamicSiteScraper.scrape().isEmpty());
    }

    @Test
    void createChromeOptionsUsesExpectedArguments() {
        DynamicSiteScraper dynamicSiteScraper = new DynamicSiteScraper();

        ChromeOptions options = dynamicSiteScraper.createChromeOptions();

        String optionsDescription = options.toString();

        assertTrue(optionsDescription.contains("--headless=new"));
        assertTrue(optionsDescription.contains("--disable-gpu"));
        assertTrue(optionsDescription.contains("--window-size=1600,900"));
    }
}
