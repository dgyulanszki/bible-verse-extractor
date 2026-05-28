package hu.szegedibibliaszol.scraper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSiteScraperTest {

    @Test
    void scrapeReturnsEmptyListUntilImplementationExists() {
        DynamicSiteScraper dynamicSiteScraper = new DynamicSiteScraper();

        assertTrue(dynamicSiteScraper.scrape().isEmpty());
    }
}

