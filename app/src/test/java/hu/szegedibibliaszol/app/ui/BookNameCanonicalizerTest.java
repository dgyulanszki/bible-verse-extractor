package hu.szegedibibliaszol.app.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookNameCanonicalizerTest {

    @Test
    void canonicalBookNameUsesCanonicalNamesForCurrentTranslationBookVariants() {
        Map<String, String> expectedCanonicalNamesByInput = new LinkedHashMap<>();
        expectedCanonicalNamesByInput.put("1. Mózes", "1Mózes");
        expectedCanonicalNamesByInput.put("Mózes I. könyve", "1Mózes");
        expectedCanonicalNamesByInput.put("Mózes első könyve", "1Mózes");
        expectedCanonicalNamesByInput.put("A bírák könyve", "Bírák");
        expectedCanonicalNamesByInput.put("Énekek éneke", "Énekek éneke");
        expectedCanonicalNamesByInput.put("Ézsaiás próféta könyve", "Ézsaiás");
        expectedCanonicalNamesByInput.put("Jeremiás Siralmai", "Jeremiás siralmai");
        expectedCanonicalNamesByInput.put("Jeremiás siralmai", "Jeremiás siralmai");
        expectedCanonicalNamesByInput.put("Ámós", "Ámós");
        expectedCanonicalNamesByInput.put("Ámos könyve", "Ámós");
        expectedCanonicalNamesByInput.put("Ámósz próféta könyve", "Ámós");
        expectedCanonicalNamesByInput.put("Sofóniás", "Sofóniás");
        expectedCanonicalNamesByInput.put("Sofóniás könyve", "Sofóniás");
        expectedCanonicalNamesByInput.put("Zofóniás próféta könyve", "Sofóniás");
        expectedCanonicalNamesByInput.put("Aggeus", "Aggeus");
        expectedCanonicalNamesByInput.put("Aggeus könyve", "Aggeus");
        expectedCanonicalNamesByInput.put("Haggeus próféta könyve", "Aggeus");
        expectedCanonicalNamesByInput.put("Apostolok Cselekedetei", "Apostolok Cselekedetei");
        expectedCanonicalNamesByInput.put("Az apostolok cselekedetei", "Apostolok Cselekedetei");
        expectedCanonicalNamesByInput.put("Róma levél", "Róma");
        expectedCanonicalNamesByInput.put("Pál levele a rómaiakhoz", "Róma");
        expectedCanonicalNamesByInput.put("1Korintus", "1Korintus");
        expectedCanonicalNamesByInput.put("1. Korintus", "1Korintus");
        expectedCanonicalNamesByInput.put("I. Korintus levél", "1Korintus");
        expectedCanonicalNamesByInput.put("Pál első levele a korinthusiakhoz", "1Korintus");
        expectedCanonicalNamesByInput.put("2Korintus", "2Korintus");
        expectedCanonicalNamesByInput.put("2. Korintus", "2Korintus");
        expectedCanonicalNamesByInput.put("II. Korintus levél", "2Korintus");
        expectedCanonicalNamesByInput.put("Pál második levele a korinthusiakhoz", "2Korintus");
        expectedCanonicalNamesByInput.put("Efézusi levél", "Efezus");
        expectedCanonicalNamesByInput.put("Pál levele az efezusiakhoz", "Efezus");
        expectedCanonicalNamesByInput.put("Kolosse levél", "Kolossé");
        expectedCanonicalNamesByInput.put("Pál levele a kolosséiakhoz", "Kolossé");
        expectedCanonicalNamesByInput.put("1Thessalonika", "1Thessalonika");
        expectedCanonicalNamesByInput.put("1. Thessalonika", "1Thessalonika");
        expectedCanonicalNamesByInput.put("I. Thessalonika levél", "1Thessalonika");
        expectedCanonicalNamesByInput.put("Pál első levele a thesszalonikaiakhoz", "1Thessalonika");
        expectedCanonicalNamesByInput.put("2Thessalonika", "2Thessalonika");
        expectedCanonicalNamesByInput.put("2. Thessalonika", "2Thessalonika");
        expectedCanonicalNamesByInput.put("II. Thessalonika levél", "2Thessalonika");
        expectedCanonicalNamesByInput.put("Pál második levele a thesszalonikaiakhoz", "2Thessalonika");
        expectedCanonicalNamesByInput.put("I. Timóteus levél", "1Timóteus");
        expectedCanonicalNamesByInput.put("Pál első levele Timóteushoz", "1Timóteus");
        expectedCanonicalNamesByInput.put("II. Timóteus levél", "2Timóteus");
        expectedCanonicalNamesByInput.put("Pál második levele Timóteushoz", "2Timóteus");
        expectedCanonicalNamesByInput.put("Titus levél", "Titusz");
        expectedCanonicalNamesByInput.put("Pál levele Tituszhoz", "Titusz");
        expectedCanonicalNamesByInput.put("Zsidó levél", "Zsidók");
        expectedCanonicalNamesByInput.put("A zsidókhoz írt levél", "Zsidók");
        expectedCanonicalNamesByInput.put("I. Péter levél", "1Péter");
        expectedCanonicalNamesByInput.put("Péter első levele", "1Péter");
        expectedCanonicalNamesByInput.put("II. Péter levél", "2Péter");
        expectedCanonicalNamesByInput.put("Péter második levele", "2Péter");
        expectedCanonicalNamesByInput.put("I. János levél", "1János");
        expectedCanonicalNamesByInput.put("János első levele", "1János");
        expectedCanonicalNamesByInput.put("II. János levél", "2János");
        expectedCanonicalNamesByInput.put("János második levele", "2János");
        expectedCanonicalNamesByInput.put("III. János levél", "3János");
        expectedCanonicalNamesByInput.put("János harmadik levele", "3János");
        expectedCanonicalNamesByInput.put("Júdás levél", "Júdás");
        expectedCanonicalNamesByInput.put("Júdás levele", "Júdás");
        expectedCanonicalNamesByInput.put("Jelenések könyve", "Jelenések");
        expectedCanonicalNamesByInput.put("A jelenések könyve", "Jelenések");

        expectedCanonicalNamesByInput.forEach((inputBookName, expectedCanonicalName) -> assertEquals(
                expectedCanonicalName,
                BookNameCanonicalizer.canonicalBookName(inputBookName),
                () -> "Expected canonical book name for " + inputBookName
        ));
    }

    @Test
    void canonicalBookNameReturnsOriginalNameWhenNoMappingExists() {
        assertEquals("Ismeretlen könyv", BookNameCanonicalizer.canonicalBookName("Ismeretlen könyv"));
        assertEquals("Cselekedetek", BookNameCanonicalizer.canonicalBookName("Cselekedetek"));
    }

    @Test
    void canonicalBookNameReturnsNullWhenInputIsNull() {
        assertNull(BookNameCanonicalizer.canonicalBookName(null));
    }

    @Test
    void canonicalBookNamesByKeyReturnsReusableStaticMap() {
        Map<String, String> canonicalBookNamesByKey = BookNameCanonicalizer.canonicalBookNamesByKey();
        assertSame(canonicalBookNamesByKey, BookNameCanonicalizer.canonicalBookNamesByKey());
        assertEquals("1Mózes", canonicalBookNamesByKey.get(BookNameCanonicalizer.normalizeBookNameKey("1. Mózes")));
    }

    @Test
    void normalizeBookNameKeyRejectsNullInput() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BookNameCanonicalizer.normalizeBookNameKey(null)
        );

        assertEquals("book must not be null", exception.getMessage());
    }

    @Test
    void parseCanonicalBookNamesByKeyFailsFastWhenAResourceLineIsInvalid() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.parseCanonicalBookNamesByKey(List.of(
                        "canonical\tRevideált Károli\tKároli\tRevideált Új Fordítás (RÚF)\tEgyszerű fordítás (EFO)",
                        "1Mózes"
                ), "/test.tsv")
        );

        assertEquals("Invalid canonical book-name entry at line 2 in resource /test.tsv: 1Mózes", exception.getMessage());
    }

    @Test
    void parseCanonicalBookNamesByKeyIgnoresCommentsAndNormalizesKeys() {
        Map<String, String> parsed = BookNameCanonicalizer.parseCanonicalBookNamesByKey(List.of(
                "# comment",
                "canonical\tRevideált Károli\tKároli\tRevideált Új Fordítás (RÚF)\tEgyszerű fordítás (EFO)",
                "",
                "# another comment",
                "1Mózes\t1. Mózes\tMózes I. könyve\tMózes első könyve\tMózes első könyve"
        ), "/test.tsv");

        assertEquals(3, parsed.size());
        assertEquals("1Mózes", parsed.get(BookNameCanonicalizer.normalizeBookNameKey("1Mózes")));
        assertEquals("1Mózes", parsed.get(BookNameCanonicalizer.normalizeBookNameKey("1. Mózes")));
        assertEquals("1Mózes", parsed.get(BookNameCanonicalizer.normalizeBookNameKey("Mózes I. könyve")));
        assertEquals("1Mózes", parsed.get(BookNameCanonicalizer.normalizeBookNameKey("Mózes első könyve")));
    }

    @Test
    void parseCanonicalBookNamesByKeyFailsFastWhenACanonicalColumnIsBlank() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.parseCanonicalBookNamesByKey(List.of(
                        "canonical\tRevideált Károli\tKároli\tRevideált Új Fordítás (RÚF)\tEgyszerű fordítás (EFO)",
                        "1Mózes\t1. Mózes\t\tMózes első könyve\tMózes első könyve"
                ), "/test.tsv")
        );

        assertEquals(
                "Invalid canonical book-name entry at line 2 in resource /test.tsv: 1Mózes\t1. Mózes\t\tMózes első könyve\tMózes első könyve",
                exception.getMessage()
        );
    }

    @Test
    void parseCanonicalBookNamesByKeyFailsFastWhenHeaderIsMissing() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.parseCanonicalBookNamesByKey(List.of("# comment only"), "/test.tsv")
        );

        assertEquals("Missing canonical book-name header in resource: /test.tsv", exception.getMessage());
    }

    @Test
    void parseCanonicalBookNamesByKeyFailsFastWhenHeaderIsInvalid() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.parseCanonicalBookNamesByKey(List.of("canonical\tRÚF"), "/test.tsv")
        );

        assertEquals("Invalid canonical book-name header in resource /test.tsv: canonical\tRÚF", exception.getMessage());
    }

    @Test
    void loadCanonicalBookNamesByKeyFailsFastWhenResourceIsMissing() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.loadCanonicalBookNamesByKey(null, "/missing.tsv")
        );

        assertEquals("Missing canonical book-name resource: /missing.tsv", exception.getMessage());
    }

    @Test
    void loadCanonicalBookNamesByKeyWrapsIoFailures() {
        try (InputStream brokenInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        }) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> BookNameCanonicalizer.loadCanonicalBookNamesByKey(brokenInputStream, "/broken.tsv")
            );

            assertEquals("Could not load canonical book-name resource: /broken.tsv", exception.getMessage());
            IOException cause = assertInstanceOf(IOException.class, exception.getCause());
            assertEquals("boom", cause.getMessage());
        } catch (IOException ex) {
            throw new AssertionError("Unexpected close failure", ex);
        }
    }

    @Test
    void loadCanonicalBookNamesByKeyFailsFastWhenRowCountIsUnexpected() {
        String resource = String.join("\n",
                "canonical\tRevideált Károli\tKároli\tRevideált Új Fordítás (RÚF)\tEgyszerű fordítás (EFO)",
                "1Mózes\t1. Mózes\tMózes I. könyve\tMózes első könyve\tMózes első könyve"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BookNameCanonicalizer.loadCanonicalBookNamesByKey(
                        new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)),
                        "/test.tsv"
                )
        );

        assertEquals("Unexpected canonical book-name row count in resource /test.tsv: 1", exception.getMessage());
    }
}
