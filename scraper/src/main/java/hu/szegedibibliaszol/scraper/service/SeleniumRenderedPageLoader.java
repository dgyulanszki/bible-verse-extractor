package hu.szegedibibliaszol.scraper.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SeleniumRenderedPageLoader implements DynamicPageLoader {

    private static final Logger log = LoggerFactory.getLogger(SeleniumRenderedPageLoader.class);
    private static final String HEADLESS_BROWSER_PROPERTY = "scraper.dynamicBrowserHeadless";

    private final List<BrowserDriverCandidate> browserCandidates;
    private final Duration pageLoadTimeout;
    private final Duration renderTimeout;
    private WebDriver webDriver;

    SeleniumRenderedPageLoader(
            List<BrowserDriverCandidate> browserCandidates,
            Duration pageLoadTimeout,
            Duration renderTimeout
    ) {
        this.browserCandidates = List.copyOf(browserCandidates);
        this.pageLoadTimeout = pageLoadTimeout;
        this.renderTimeout = renderTimeout;
    }

    static List<BrowserDriverCandidate> defaultBrowserCandidates() {
        return defaultBrowserCandidates(() -> new ChromeDriver(chromeOptions()));
    }

    static List<BrowserDriverCandidate> defaultBrowserCandidates(Supplier<WebDriver> chromeDriverFactory) {
        return List.of(
                new BrowserDriverCandidate("Google Chrome", chromeDriverFactory)
        );
    }

    @Override
    public Document load(String url) throws IOException {
        WebDriver driver = webDriver();
        try {
            driver.navigate().to(url);
            waitForRenderedChapter(driver, url);
            return Jsoup.parse(currentPageSource(driver), url);
        } catch (WebDriverException ex) {
            throw new IOException("Could not render dynamic page in browser: " + url, ex);
        }
    }

    @Override
    public void close() {
        if (webDriver == null) {
            return;
        }

        webDriver.quit();
        webDriver = null;
    }

    static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(chromeArguments().toArray(String[]::new));
        if (Boolean.parseBoolean(System.getProperty(HEADLESS_BROWSER_PROPERTY, "false"))) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    static List<String> chromeArguments() {
        return List.of("--window-size=1920,1080", "--disable-gpu", "--disable-dev-shm-usage");
    }

    static boolean containsRenderedChapterContent(String pageSource) {
        return pageSource.contains("data-testid=\"chapter-content\"") && pageSource.contains("data-usfm");
    }

    static boolean isClientChallengePage(String pageSource) {
        return pageSource.contains("<title>Client Challenge</title>")
                || pageSource.contains("JavaScript is disabled in your browser.");
    }

    private void waitForRenderedChapter(WebDriver driver, String url) throws IOException {
        try {
            new WebDriverWait(driver, renderTimeout)
                    .pollingEvery(Duration.ofMillis(100))
                    .until(ignored -> containsRenderedChapterContent(currentPageSource(driver)));
        } catch (TimeoutException ex) {
            String pageSource = currentPageSource(driver);
            if (isClientChallengePage(pageSource)) {
                throw new IOException(
                        "Bible.com returned a client challenge page instead of chapter content for " + url,
                        ex
                );
            }
            throw new IOException("Timed out while waiting for rendered chapter content: " + url, ex);
        }
    }

    private WebDriver webDriver() {
        if (webDriver != null) {
            return webDriver;
        }

        IllegalStateException lastFailure = null;
        for (BrowserDriverCandidate browserCandidate : browserCandidates) {
            try {
                WebDriver driver = browserCandidate.factory().get();
                driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
                log.info("Using {} browser for dynamic scraping.", browserCandidate.name());
                webDriver = driver;
                return driver;
            } catch (RuntimeException ex) {
                log.warn(
                        "Could not start {} browser for dynamic scraping: {}",
                        browserCandidate.name(),
                        ex.getMessage()
                );
                lastFailure = new IllegalStateException(
                        "Could not start " + browserCandidate.name() + " browser for dynamic scraping.",
                        ex
                );
            }
        }

        throw new IllegalStateException(
                "Could not start a supported browser for dynamic scraping. Install Google Chrome, or disable the dynamic scraper.",
                lastFailure
        );
    }

    private String currentPageSource(WebDriver driver) {
        String pageSource = driver.getPageSource();
        if (pageSource == null) {
            return "";
        }
        return pageSource;
    }

    record BrowserDriverCandidate(String name, Supplier<WebDriver> factory) {
    }
}



