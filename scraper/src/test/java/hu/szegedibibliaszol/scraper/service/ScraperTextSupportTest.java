package hu.szegedibibliaszol.scraper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScraperTextSupportTest {

    @Test
    void normalizeTextReplacesNonBreakingSpacesAndCollapsesWhitespace() {
        assertEquals("első második harmadik", ScraperTextSupport.normalizeText("  első\u00A0 második\n\t harmadik  "));
    }

    @Test
    void utilityClassCanBeConstructedWithinItsPackage() {
        assertNotNull(new ScraperTextSupport());
    }
}


