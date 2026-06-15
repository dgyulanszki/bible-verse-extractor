package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.testutil.FxTestSupport;
import hu.szegedibibliaszol.app.ui.MainViewFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void initUsesCreateApplicationContext() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        TestJavaFxApplication javaFxApplication = new TestJavaFxApplication(applicationContext);

        javaFxApplication.init();

        assertEquals(1, javaFxApplication.createContextCalls);
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
            assertEquals(1, stage.getIcons().size());
            assertTrue(stage.getIcons().getFirst().getWidth() > 0);
            assertEquals(960, stage.getMinWidth());
            assertEquals(640, stage.getMinHeight());
            stage.close();
            return null;
        });
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
    void startFailsWhenContextIsMissing() {
        JavaFxApplication javaFxApplication = new JavaFxApplication();

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            assertThrows(IllegalStateException.class, () -> javaFxApplication.start(stage));
            stage.close();
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

    private static final class TestJavaFxApplication extends JavaFxApplication {

        private final ConfigurableApplicationContext applicationContext;
        private int createContextCalls;

        private TestJavaFxApplication(ConfigurableApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        ConfigurableApplicationContext createApplicationContext() {
            createContextCalls++;
            return applicationContext;
        }
    }

    private byte[] iconPngBytes() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");
    }
}
