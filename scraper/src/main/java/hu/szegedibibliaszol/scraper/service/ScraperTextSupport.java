package hu.szegedibibliaszol.scraper.service;

final class ScraperTextSupport {

    ScraperTextSupport() {
    }

    static String normalizeText(String text) {
        return text.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }
}

