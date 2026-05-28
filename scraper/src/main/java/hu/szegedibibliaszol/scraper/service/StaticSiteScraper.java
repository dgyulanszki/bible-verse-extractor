package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticSiteScraper {

    private static final Logger log = LoggerFactory.getLogger(StaticSiteScraper.class);

    public List<VerseRecord> scrape() {
        log.info("Static scraping is not implemented yet.");
        return List.of();
    }
}

