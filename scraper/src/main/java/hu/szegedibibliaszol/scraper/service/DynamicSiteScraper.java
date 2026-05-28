package hu.szegedibibliaszol.scraper.service;

import java.util.List;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSiteScraper {

    private static final Logger log = LoggerFactory.getLogger(DynamicSiteScraper.class);

    public List<String> scrape() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1600,900");
        log.info("Dynamic scraping is not implemented yet. Prepared browser options: {}", options);
        return List.of();
    }
}

