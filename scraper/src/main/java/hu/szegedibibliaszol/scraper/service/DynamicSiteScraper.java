package hu.szegedibibliaszol.scraper.service;

import java.util.List;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSiteScraper {

    private static final String HEADLESS_ARGUMENT = "--headless=new";
    private static final String DISABLE_GPU_ARGUMENT = "--disable-gpu";
    private static final String WINDOW_SIZE_ARGUMENT = "--window-size=1600,900";

    private static final Logger log = LoggerFactory.getLogger(DynamicSiteScraper.class);

    public List<String> scrape() {
        ChromeOptions options = createChromeOptions();
        log.info("Dynamic scraping is not implemented yet. Prepared browser options: {}", options);
        return List.of();
    }

    ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(HEADLESS_ARGUMENT, DISABLE_GPU_ARGUMENT, WINDOW_SIZE_ARGUMENT);
        return options;
    }
}
