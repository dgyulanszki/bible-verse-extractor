package hu.szegedibibliaszol.scraper.service;

public class DynamicEfoScraper extends AbstractDynamicSiteScraper {

    private static final String TRANSLATION_ID = "efo";
    private static final String START_URL = "https://www.bible.com/bible/198/GEN.1.EFO";
    private static final String TRANSLATION_BUTTON_TEXT = "EFO";
    private static final String TRANSLATION_NAME = "Egyszerű fordítás (EFO)";

    @Override
    public String id() {
        return TRANSLATION_ID;
    }

    @Override
    public String translation() {
        return TRANSLATION_NAME;
    }

    @Override
    protected String startUrl() {
        return START_URL;
    }

    @Override
    protected String translationButtonText() {
        return TRANSLATION_BUTTON_TEXT;
    }
}

