package hu.szegedibibliaszol.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationVersionTest {

    @Test
    void currentUsesResolvedBuildVersion() {
        assertNotEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.current());
        assertEquals("1.2.1-SNAPSHOT", ApplicationVersion.current());
    }

    @Test
    void resolveVersionUsesVersionFromFilteredResource() {
        String resolvedVersion = ApplicationVersion.resolveVersion(
                new ByteArrayInputStream("app.version=9.8.7\n".getBytes(StandardCharsets.UTF_8)),
                "fallback-version",
                "/test-version.properties"
        );

        assertEquals("9.8.7", resolvedVersion);
    }

    @Test
    void resolveVersionFallsBackWhenResourceIsMissingOrUnresolved() throws IOException {
        assertEquals("1.2.3", ApplicationVersion.resolveVersion(null, "1.2.3", "/missing.properties"));
        assertEquals(
                "2.0.0",
                ApplicationVersion.resolveVersion(
                        new ByteArrayInputStream("app.version=@project.version@\n".getBytes(StandardCharsets.UTF_8)),
                        "2.0.0",
                        "/test-version.properties"
                )
        );
        assertEquals(
                "3.4.5",
                ApplicationVersion.resolveVersion(
                        new ByteArrayInputStream("app.version=${project.version}\n".getBytes(StandardCharsets.UTF_8)),
                        null,
                        "/test-version.properties",
                        repositoryRoot("3.4.5")
                )
        );
        assertEquals(
                ApplicationVersion.UNKNOWN_VERSION,
                ApplicationVersion.resolveVersion(
                        new ByteArrayInputStream("app.version=   \n".getBytes(StandardCharsets.UTF_8)),
                        null,
                        "/test-version.properties"
                )
        );
    }

    @Test
    void resolveVersionWrapsIoFailures() {
        try (InputStream brokenInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        }) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> ApplicationVersion.resolveVersion(brokenInputStream, "1.2.3", "/broken.properties")
            );

            assertEquals("Could not load application version resource: /broken.properties", exception.getMessage());
            IOException cause = assertInstanceOf(IOException.class, exception.getCause());
            assertEquals("boom", cause.getMessage());
        } catch (IOException ex) {
            throw new AssertionError("Unexpected close failure", ex);
        }
    }

    @Test
    void loadVersionFromRepositoryPomReturnsRootProjectVersion() throws IOException {
        assertEquals("4.5.6-SNAPSHOT", ApplicationVersion.loadVersionFromRepositoryPom(repositoryRoot("4.5.6-SNAPSHOT")));
    }

    @Test
    void loadVersionFromRepositoryPomReturnsUnknownWhenSearchStartIsNull() {
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.loadVersionFromRepositoryPom(null));
    }

    @Test
    void loadVersionFromRepositoryPomReturnsUnknownOutsideRepository() throws IOException {
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.loadVersionFromRepositoryPom(Files.createTempDirectory("outside-repo")));
    }

    @Test
    void readProjectVersionReturnsUnknownWhenPomCannotBeRead() throws IOException {
        Path missingPom = Files.createTempDirectory("missing-pom").resolve("pom.xml");

        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.readProjectVersion(missingPom));
    }

    @Test
    void readProjectVersionReturnsUnknownWhenVersionElementIsMissing() throws IOException {
        Path pomWithoutVersion = Files.createTempFile("pom-without-version", ".xml");
        Files.writeString(pomWithoutVersion, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>hu.szegedibibliaszol</groupId>
                    <artifactId>bible-verse-tool</artifactId>
                </project>
                """);

        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.readProjectVersion(pomWithoutVersion));
    }

    @Test
    void sanitizeVersionNormalizesMissingAndPlaceholderValues() {
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.sanitizeVersion(null));
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.sanitizeVersion("   "));
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.sanitizeVersion("@project.version@"));
        assertEquals(ApplicationVersion.UNKNOWN_VERSION, ApplicationVersion.sanitizeVersion("${project.version}"));
        assertEquals("1.2.1-SNAPSHOT", ApplicationVersion.sanitizeVersion(" 1.2.1-SNAPSHOT "));
    }

    private Path repositoryRoot(String version) throws IOException {
        Path repositoryRoot = Files.createTempDirectory("bible-verse-tool-repo");
        Files.createDirectories(repositoryRoot.resolve("app"));
        Files.createDirectories(repositoryRoot.resolve("scraper"));
        Files.writeString(repositoryRoot.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>hu.szegedibibliaszol</groupId>
                    <artifactId>bible-verse-tool</artifactId>
                    <version>%s</version>
                </project>
                """.formatted(version));
        return repositoryRoot.resolve("app");
    }
}
