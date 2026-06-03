package hu.szegedibibliaszol.scraper.service;

import java.util.regex.Pattern;

public class StaticRevidealtKaroliScraper extends AbstractStaticSiteScraper {

    private static final String ID = "revidealt-karoli";
    private static final String BASE_URL = "https://www.online-biblia.ro";
    private static final String TRANSLATION = "Revideált Károli";
    private static final String ROOT_PATH = "/bible/4";
    private static final Pattern BOOK_LINK_PATTERN = Pattern.compile("^/bible/4/([A-Z0-9]+)$");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String translation() {
        return TRANSLATION;
    }

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String rootPath() {
        return ROOT_PATH;
    }

    @Override
    protected Pattern bookLinkPattern() {
        return BOOK_LINK_PATTERN;
    }

    @Override
    protected Pattern chapterLinkPattern(String bookCode) {
        return Pattern.compile("^/bible/4/" + Pattern.quote(bookCode) + "/(\\d+)$");
    }
}


