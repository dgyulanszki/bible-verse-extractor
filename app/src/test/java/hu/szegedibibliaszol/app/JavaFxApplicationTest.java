package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.service.TranslationUsageService;
import hu.szegedibibliaszol.app.service.UiSessionService;
import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.testutil.FxTestSupport;
import hu.szegedibibliaszol.app.ui.MainViewFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaFxApplicationTest {

    @BeforeAll
    static void initializeJavaFx() {
        FxTestSupport.initialize();
    }

    @Test
    void startWithoutContextShowsLoadingSceneAndDefersSpringStartup() {
        AtomicInteger backgroundStarts = new AtomicInteger();
        JavaFxApplication javaFxApplication = new JavaFxApplication() {
            @Override
            void beginContextInitialization(Stage stage) {
                backgroundStarts.incrementAndGet();
            }
        };

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            javaFxApplication.start(stage);
            Label loadingLabel = (Label) ((StackPane) stage.getScene().getRoot()).getChildren().getFirst();

            assertEquals(MainViewFactory.APPLICATION_TITLE, stage.getTitle());
            assertTrue(stage.isShowing());
            assertTrue(stage.isMaximized());
            assertTrue(loadingLabel.getText().contains("betöltése"));
            assertEquals(1, backgroundStarts.get());
            javaFxApplication.ensureApplicationIcon(stage);
            assertEquals(1, stage.getIcons().size());
            stage.close();
            return null;
        });
    }

    @Test
    void resolveDefaultDatabasePathUsesRepositoryLocalAppDataDirectory() throws IOException {
        Path repositoryRoot = Files.createTempDirectory("app-repo-root");
        Files.createFile(repositoryRoot.resolve("pom.xml"));
        Files.createDirectories(repositoryRoot.resolve("app"));
        Files.createDirectories(repositoryRoot.resolve("scraper").resolve("nested"));

        Path resolvedPath = JavaFxApplication.resolveDefaultDatabasePath(repositoryRoot.resolve("scraper").resolve("nested"));

        assertEquals(repositoryRoot.resolve("app").resolve("data").resolve("bible-verses.db"), resolvedPath);
    }

    @Test
    void resolveDefaultDatabasePathFallsBackToUserHomeOutsideRepository() throws IOException {
        Path unrelatedDirectory = Files.createTempDirectory("app-non-repo");

        Path resolvedPath = JavaFxApplication.resolveDefaultDatabasePath(unrelatedDirectory);

        assertEquals(Path.of(System.getProperty("user.home"), "bible-verses.db"), resolvedPath);
    }

    @Test
    void resolveStartupDatabasePathPrefersConfiguredValue() throws IOException {
        Path configuredDatabasePath = Files.createTempFile("configured-app-db", ".db");
        Path repositoryRoot = Files.createTempDirectory("app-repo-root-configured");
        Files.createFile(repositoryRoot.resolve("pom.xml"));
        Files.createDirectories(repositoryRoot.resolve("app"));
        Files.createDirectories(repositoryRoot.resolve("scraper"));

        Path resolvedPath = JavaFxApplication.resolveStartupDatabasePath(
                configuredDatabasePath.toString(),
                repositoryRoot.resolve("scraper")
        );

        assertEquals(configuredDatabasePath, resolvedPath);
    }

    @Test
    void findRepositoryRootReturnsEmptyWhenRepositoryMarkersAreMissing() throws IOException {
        Path unrelatedDirectory = Files.createTempDirectory("app-no-markers");

        assertTrue(JavaFxApplication.findRepositoryRoot(unrelatedDirectory).isEmpty());
    }

    @Test
    void createApplicationContextBuildsSpringContext() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        ConfigurableApplicationContext applicationContext = javaFxApplication.createApplicationContext();

        assertNotNull(applicationContext);
        applicationContext.close();
    }

    @Test
    void createSceneUsesConfiguredDimensions() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        MainViewFactory mainViewFactory = mock(MainViewFactory.class);
        when(mainViewFactory.createRoot()).thenReturn(new BorderPane());

        Scene scene = javaFxApplication.createScene(mainViewFactory);

        assertEquals(1180, scene.getWidth());
        assertEquals(760, scene.getHeight());
    }

    @Test
    void startConfiguresStageFromSpringBean() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        MainViewFactory mainViewFactory = mock(MainViewFactory.class);
        when(mainViewFactory.createRoot()).thenReturn(new BorderPane());
        when(applicationContext.getBean(MainViewFactory.class)).thenReturn(mainViewFactory);

        JavaFxApplication javaFxApplication = new JavaFxApplication();
        javaFxApplication.setContext(applicationContext);

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            javaFxApplication.start(stage);
            assertEquals(MainViewFactory.APPLICATION_TITLE, stage.getTitle());
            assertNotNull(stage.getScene());
            assertTrue(stage.isMaximized());
            assertEquals(1, stage.getIcons().size());
            assertTrue(stage.getIcons().getFirst().getWidth() > 0);
            assertEquals(960, stage.getMinWidth());
            assertEquals(640, stage.getMinHeight());
            stage.close();
            return null;
        });
    }

    @Test
    void sceneReplacementKeepsTheWindowMaximized() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        MainViewFactory mainViewFactory = mock(MainViewFactory.class);
        when(mainViewFactory.createRoot()).thenAnswer(_ -> new BorderPane());
        when(applicationContext.getBean(MainViewFactory.class)).thenReturn(mainViewFactory);
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        javaFxApplication.setContext(applicationContext);

        Stage stage = FxTestSupport.runOnFxThread(() -> {
            Stage loadingStage = new Stage();
            javaFxApplication.prepareStage(loadingStage);
            javaFxApplication.showSceneExpanded(loadingStage, javaFxApplication.createLoadingScene());
            loadingStage.setMaximized(false);
            javaFxApplication.installMainScene(loadingStage);
            assertTrue(loadingStage.isMaximized());
            return loadingStage;
        });

        for (int pulse = 0; pulse < 4; pulse++) {
            FxTestSupport.waitForFxEvents();
        }
        FxTestSupport.runOnFxThread(() -> {
            assertTrue(stage.isMaximized());
            stage.close();
            return null;
        });
    }

    @Test
    void startupMaximizedStateSurvivesSceneReplacementThenAllowsRestore() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        Stage stage = FxTestSupport.runOnFxThread(() -> {
            Stage shownStage = new Stage();
            javaFxApplication.prepareStage(shownStage);
            shownStage.setScene(javaFxApplication.createLoadingScene());
            shownStage.show();
            javaFxApplication.keepStartupWindowExpanded(shownStage);
            shownStage.setMaximized(false);
            return shownStage;
        });

        FxTestSupport.waitForFxEvents();
        FxTestSupport.runOnFxThread(() -> {
            assertTrue(stage.isMaximized());
            return null;
        });

        for (int pulse = 0; pulse < 8; pulse++) {
            FxTestSupport.waitForFxEvents();
        }

        FxTestSupport.runOnFxThread(() -> {
            stage.setMaximized(false);
            return null;
        });
        FxTestSupport.waitForFxEvents();
        FxTestSupport.runOnFxThread(() -> {
            assertFalse(stage.isMaximized());
            stage.close();
            return null;
        });
    }

    @Test
    void deferredMaximizeIsSkippedAfterTheStageIsClosed() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            stage.setScene(javaFxApplication.createLoadingScene());
            stage.show();
            javaFxApplication.keepStartupWindowExpanded(stage);
            assertTrue(stage.isMaximized());
            stage.close();
            stage.setMaximized(false);
            return null;
        });
        FxTestSupport.waitForFxEvents();
    }

    @Test
    void loadApplicationIconReadsImageStream() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        byte[] pngBytes = iconPngBytes();

        Image image = javaFxApplication.loadApplicationIcon(new ByteArrayInputStream(pngBytes));

        assertEquals(1, (int) image.getWidth());
        assertEquals(1, (int) image.getHeight());
    }

    @Test
    void loadApplicationIconUsesPackagedResourceWhenPresent() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        Image image = javaFxApplication.loadApplicationIcon();

        assertNotNull(JavaFxApplication.class.getResource("/bible-verse-app-icon.png"));
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void loadApplicationIconCreatesFallbackWhenResourceIsMissing() {
        JavaFxApplication javaFxApplication = new JavaFxApplication() {
            @Override
            InputStream getApplicationIconStream() {
                return null;
            }
        };

        FxTestSupport.runOnFxThread(() -> {
            Image image = javaFxApplication.loadApplicationIcon();

            assertEquals(256, (int) image.getWidth());
            assertEquals(256, (int) image.getHeight());
            return null;
        });
    }

    @Test
    void loadApplicationIconWrapsIoFailures() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        InputStream failingStream = new ByteArrayInputStream(iconPngBytes()) {
            @Override
            public void close() throws IOException {
                throw new IOException("forced close failure");
            }
        };

        UncheckedIOException exception = assertThrows(UncheckedIOException.class, () -> javaFxApplication.loadApplicationIcon(failingStream));

        assertEquals("Failed to load application icon.", exception.getMessage());
    }

    @Test
    void installMainSceneFailsWhenContextIsMissing() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> javaFxApplication.installMainScene(stage));
            assertEquals("Application context is not initialized.", exception.getMessage());
            stage.close();
            return null;
        });
    }

    @Test
    void backgroundStartupInstallsMainSceneAfterWarmingCaches() throws Exception {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        MainViewFactory mainViewFactory = mock(MainViewFactory.class);
        VerseBrowserService verseBrowserService = mock(VerseBrowserService.class);
        UiSessionService uiSessionService = mock(UiSessionService.class);
        TranslationUsageService translationUsageService = mock(TranslationUsageService.class);
        when(mainViewFactory.createRoot()).thenAnswer(_ -> new BorderPane());
        when(applicationContext.getBean(MainViewFactory.class)).thenReturn(mainViewFactory);
        when(applicationContext.getBean(VerseBrowserService.class)).thenReturn(verseBrowserService);
        when(applicationContext.getBean(UiSessionService.class)).thenReturn(uiSessionService);
        when(applicationContext.getBean(TranslationUsageService.class)).thenReturn(translationUsageService);
        when(verseBrowserService.getTranslations()).thenReturn(List.of("Károli"));
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        when(translationUsageService.preferredTranslation()).thenReturn(Optional.of("Károli"));
        CountDownLatch startupFinished = new CountDownLatch(1);
        JavaFxApplication javaFxApplication = new JavaFxApplication() {
            @Override
            ConfigurableApplicationContext createApplicationContext() {
                return applicationContext;
            }

            @Override
            void completeStartup(Stage stage, ConfigurableApplicationContext startedContext) {
                super.completeStartup(stage, startedContext);
                startupFinished.countDown();
            }
        };

        Stage stage = FxTestSupport.runOnFxThread(() -> {
            Stage loadingStage = new Stage();
            javaFxApplication.beginContextInitialization(loadingStage);
            return loadingStage;
        });

        assertTrue(startupFinished.await(5, TimeUnit.SECONDS));
        verify(verseBrowserService).getTranslations();
        verify(uiSessionService).loadSession();
        verify(translationUsageService).preferredTranslation();
        FxTestSupport.runOnFxThread(() -> {
            javaFxApplication.installMainScene(stage);
            stage.close();
            return null;
        });
    }

    @Test
    void backgroundStartupShowsFailureAndIgnoresLateContextAfterStop() throws Exception {
        CountDownLatch failureShown = new CountDownLatch(1);
        JavaFxApplication failingApplication = new JavaFxApplication() {
            @Override
            ConfigurableApplicationContext createApplicationContext() {
                throw new IllegalStateException("db down");
            }

            @Override
            void showStartupFailure(Stage stage, Throwable failure) {
                super.showStartupFailure(stage, failure);
                failureShown.countDown();
            }
        };

        Stage stage = FxTestSupport.runOnFxThread(() -> {
            Stage loadingStage = new Stage();
            failingApplication.beginContextInitialization(loadingStage);
            return loadingStage;
        });
        assertTrue(failureShown.await(5, TimeUnit.SECONDS));
        FxTestSupport.runOnFxThread(() -> {
            Label failureLabel = (Label) ((StackPane) stage.getScene().getRoot()).getChildren().getFirst();
            assertTrue(failureLabel.getText().contains("db down"));
            stage.close();
            return null;
        });

        ConfigurableApplicationContext lateContext = mock(ConfigurableApplicationContext.class);
        JavaFxApplication stoppedApplication = new JavaFxApplication();
        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            stoppedApplication.stop();
            platform.verify(Platform::exit);
        }
        FxTestSupport.runOnFxThread(() -> {
            stoppedApplication.completeStartup(new Stage(), lateContext);
            return null;
        });
        verify(lateContext).close();

        FxTestSupport.runOnFxThread(() -> {
            Stage blankFailureStage = new Stage();
            stoppedApplication.showStartupFailure(blankFailureStage, new IllegalStateException(" "));
            stoppedApplication.showStartupFailure(blankFailureStage, null);
            Label failureLabel = (Label) ((StackPane) blankFailureStage.getScene().getRoot()).getChildren().getFirst();
            assertEquals("Az alkalmazás indítása nem sikerült.", failureLabel.getText());
            blankFailureStage.close();
            return null;
        });
    }

    @Test
    void stopSavesCurrentSessionClosesContextAndExitsPlatform() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        MainViewFactory mainViewFactory = mock(MainViewFactory.class);
        when(applicationContext.getBean(MainViewFactory.class)).thenReturn(mainViewFactory);
        JavaFxApplication javaFxApplication = new JavaFxApplication();
        javaFxApplication.setContext(applicationContext);

        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            javaFxApplication.stop();

            verify(mainViewFactory).saveCurrentSession();
            verify(applicationContext).close();
            platform.verify(Platform::exit);
        }
    }

    @Test
    void closeContextIgnoresMissingContext() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        assertDoesNotThrow(javaFxApplication::closeContext);
    }

    @Test
    void stopExitsPlatformWhenContextIsMissing() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            javaFxApplication.stop();

            platform.verify(Platform::exit);
        }
    }

    private byte[] iconPngBytes() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");
    }
}
