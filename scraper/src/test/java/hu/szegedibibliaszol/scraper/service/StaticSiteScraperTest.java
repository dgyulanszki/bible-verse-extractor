package hu.szegedibibliaszol.scraper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticSiteScraperTest {

    @Test
    void scrapeReturnsEmptyListUntilImplementationExists() {
        StaticSiteScraper staticSiteScraper = new StaticSiteScraper();

        assertTrue(staticSiteScraper.scrape().isEmpty());
    }
}

