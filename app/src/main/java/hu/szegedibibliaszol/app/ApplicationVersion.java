package hu.szegedibibliaszol.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

public final class ApplicationVersion {

    static final String RESOURCE_PATH = "/app-version.properties";
    static final String VERSION_PROPERTY_NAME = "app.version";
    static final String UNKNOWN_VERSION = "ismeretlen";
    private static final String UNRESOLVED_MAVEN_PLACEHOLDER_PATTERN = "^(?:@.+@|\\$\\{.+})$";
    private static final String VERSION = loadVersion();

    private ApplicationVersion() {
    }

    public static String current() {
        return VERSION;
    }

    static String loadVersion() {
        return resolveVersion(
                ApplicationVersion.class.getResourceAsStream(RESOURCE_PATH),
                ApplicationVersion.class.getPackage().getImplementationVersion(),
                RESOURCE_PATH,
                Path.of(System.getProperty("user.dir"))
        );
    }

    static String resolveVersion(InputStream inputStream, String fallbackVersion, String resourcePath) {
        return resolveVersion(inputStream, fallbackVersion, resourcePath, null);
    }

    static String resolveVersion(InputStream inputStream, String fallbackVersion, String resourcePath, Path searchStart) {
        String sanitizedFallbackVersion = firstKnownVersion(
                sanitizeVersion(fallbackVersion),
                loadVersionFromRepositoryPom(searchStart)
        );
        if (inputStream == null) {
            return sanitizedFallbackVersion;
        }

        Properties properties = new Properties();
        try (InputStream closeableInputStream = inputStream;
             InputStreamReader reader = new InputStreamReader(closeableInputStream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load application version resource: " + resourcePath, ex);
        }

        String resourceVersion = sanitizeVersion(properties.getProperty(VERSION_PROPERTY_NAME));
        return firstKnownVersion(resourceVersion, sanitizedFallbackVersion);
    }

    static String loadVersionFromRepositoryPom(Path searchStart) {
        if (searchStart == null) {
            return UNKNOWN_VERSION;
        }

        return JavaFxApplication.findRepositoryRoot(searchStart)
                .map(repositoryRoot -> readProjectVersion(repositoryRoot.resolve("pom.xml")))
                .orElse(UNKNOWN_VERSION);
    }

    static String readProjectVersion(Path pomPath) {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);

            NodeList versionNodes = factory.newDocumentBuilder()
                    .parse(inputStream)
                    .getDocumentElement()
                    .getElementsByTagName("version");
            if (versionNodes.getLength() == 0) {
                return UNKNOWN_VERSION;
            }
            return sanitizeVersion(versionNodes.item(0).getTextContent());
        } catch (Exception _) {
            return UNKNOWN_VERSION;
        }
    }

    private static String firstKnownVersion(String primaryVersion, String fallbackVersion) {
        return UNKNOWN_VERSION.equals(primaryVersion) ? fallbackVersion : primaryVersion;
    }

    static String sanitizeVersion(String version) {
        if (version == null) {
            return UNKNOWN_VERSION;
        }

        String trimmedVersion = version.trim();
        if (trimmedVersion.isEmpty() || trimmedVersion.matches(UNRESOLVED_MAVEN_PLACEHOLDER_PATTERN)) {
            return UNKNOWN_VERSION;
        }
        return trimmedVersion;
    }
}
