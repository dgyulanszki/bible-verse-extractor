package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.service.TranslationUsageService;
import hu.szegedibibliaszol.app.service.UiSessionService;
import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.ui.MainViewFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private static final String DATABASE_FILE_NAME = "bible-verses.db";
    private static final Path USER_HOME_FALLBACK_DATABASE_PATH = Path.of(System.getProperty("user.home"), DATABASE_FILE_NAME);
    private static final double DEFAULT_WIDTH = 1180;
    private static final double DEFAULT_HEIGHT = 760;
    private static final double MINIMUM_WIDTH = 960;
    private static final double MINIMUM_HEIGHT = 640;
    private static final String APPLICATION_ICON_RESOURCE = "/bible-verse-app-icon.png";
    private static final int APPLICATION_ICON_SIZE = 256;
    private static final String MAXIMIZE_GUARD = "startupMaximizeGuard";
    private static final String MAXIMIZE_ENFORCEMENT = "startupMaximizeEnforcement";
    private static final int MAXIMIZE_ENFORCEMENT_PULSES = 3;

    private ConfigurableApplicationContext context;
    private volatile boolean stopping;

    @Override
    public void start(Stage stage) {
        prepareStage(stage);
        ensureApplicationIcon(stage);
        if (context != null) {
            installMainScene(stage);
            return;
        }
        // Show a window immediately. Spring and the SQLite caches continue on a background task.
        showSceneExpanded(stage, createLoadingScene());
        beginContextInitialization(stage);
    }

    @Override
    public void stop() {
        stopping = true;
        if (context != null) {
            requireContext().getBean(MainViewFactory.class).saveCurrentSession();
        }
        closeContext();
        Platform.exit();
    }

    void beginContextInitialization(Stage stage) {
        Task<ConfigurableApplicationContext> startupTask = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                ConfigurableApplicationContext applicationContext = createApplicationContext();
                warmStartupCaches(applicationContext);
                return applicationContext;
            }
        };
        startupTask.setOnSucceeded(_ -> completeStartup(stage, startupTask.getValue()));
        startupTask.setOnFailed(_ -> showStartupFailure(stage, startupTask.getException()));
        Thread startupThread = new Thread(startupTask, "bible-verse-startup");
        startupThread.setDaemon(true);
        startupThread.start();
    }

    void warmStartupCaches(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBean(VerseBrowserService.class).getTranslations();
        applicationContext.getBean(UiSessionService.class).loadSession();
        applicationContext.getBean(TranslationUsageService.class).preferredTranslation();
    }

    void completeStartup(Stage stage, ConfigurableApplicationContext applicationContext) {
        if (stopping) {
            applicationContext.close();
            return;
        }
        setContext(applicationContext);
        installMainScene(stage);
    }

    void prepareStage(Stage stage) {
        stage.setTitle(MainViewFactory.APPLICATION_TITLE);
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);
        // Owning the window size stops a later setScene() from pushing the scene's
        // 1180x760 bounds through. On Windows that bounds update restores a maximized window.
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setMaximized(true);
    }

    void ensureApplicationIcon(Stage stage) {
        if (stage.getIcons().isEmpty()) {
            stage.getIcons().setAll(loadApplicationIcon());
        }
    }

    void installMainScene(Stage stage) {
        MainViewFactory mainViewFactory = requireContext().getBean(MainViewFactory.class);
        showSceneExpanded(stage, createScene(mainViewFactory));
    }

    void showSceneExpanded(Stage stage, Scene scene) {
        ensureMaximizeGuard(stage);
        int enforcement = beginMaximizeEnforcement(stage);
        stage.setScene(scene);
        if (!stage.isShowing()) {
            stage.show();
        }
        // show() applies the window size before the native maximized state.
        // Re-apply across the following pulses so that size event cannot win.
        enforceMaximized(stage, enforcement, MAXIMIZE_ENFORCEMENT_PULSES);
    }

    void keepStartupWindowExpanded(Stage stage) {
        ensureMaximizeGuard(stage);
        enforceMaximized(stage, beginMaximizeEnforcement(stage), MAXIMIZE_ENFORCEMENT_PULSES);
    }

    void ensureMaximizeGuard(Stage stage) {
        if (stage.getProperties().containsKey(MAXIMIZE_GUARD)) {
            return;
        }
        stage.getProperties().put(MAXIMIZE_GUARD, Boolean.TRUE);
        stage.maximizedProperty().addListener((observable, wasMaximized, maximized) -> {
            if (Boolean.TRUE.equals(maximized)) {
                return;
            }
            if (!stage.isShowing()) {
                return;
            }
            if (enforcementGeneration(stage) <= 0) {
                return;
            }
            Platform.runLater(() -> {
                if (stage.isShowing() && enforcementGeneration(stage) > 0) {
                    stage.setMaximized(true);
                }
            });
        });
    }

    void enforceMaximized(Stage stage, int enforcement, int pulsesRemaining) {
        if (!stage.isShowing() || enforcementGeneration(stage) != enforcement) {
            return;
        }
        stage.setMaximized(true);
        if (pulsesRemaining <= 0) {
            Platform.runLater(() -> releaseMaximizeEnforcement(stage, enforcement));
            return;
        }
        Platform.runLater(() -> enforceMaximized(stage, enforcement, pulsesRemaining - 1));
    }

    private int beginMaximizeEnforcement(Stage stage) {
        int enforcement = enforcementGeneration(stage) + 1;
        stage.getProperties().put(MAXIMIZE_ENFORCEMENT, enforcement);
        return enforcement;
    }

    private void releaseMaximizeEnforcement(Stage stage, int enforcement) {
        if (enforcementGeneration(stage) == enforcement) {
            stage.getProperties().put(MAXIMIZE_ENFORCEMENT, 0);
        }
    }

    private int enforcementGeneration(Stage stage) {
        Object storedGeneration = stage.getProperties().get(MAXIMIZE_ENFORCEMENT);
        if (storedGeneration instanceof Integer generation) {
            return generation;
        }
        return 0;
    }

    Scene createLoadingScene() {
        Label loadingLabel = new Label("Biblia könyvtár betöltése…");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        StackPane loadingRoot = new StackPane(loadingLabel);
        loadingRoot.setStyle("-fx-background-color: #25476F;");
        return new Scene(loadingRoot, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    void showStartupFailure(Stage stage, Throwable failure) {
        String detail = failure == null || failure.getMessage() == null || failure.getMessage().isBlank()
                ? "Az alkalmazás indítása nem sikerült."
                : "Az alkalmazás indítása nem sikerült: " + failure.getMessage();
        Label failureLabel = new Label(detail);
        failureLabel.setWrapText(true);
        failureLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        StackPane failureRoot = new StackPane(failureLabel);
        failureRoot.setStyle("-fx-background-color: #25476F;");
        showSceneExpanded(stage, new Scene(failureRoot, DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

    ConfigurableApplicationContext createApplicationContext() {
        String configuredDatabasePath = System.getProperty("app.database.path");
        if (configuredDatabasePath == null || configuredDatabasePath.isBlank()) {
            configuredDatabasePath = System.getenv("BIBLE_VERSE_DB_PATH");
        }
        Path defaultDatabasePath = resolveStartupDatabasePath(configuredDatabasePath, Path.of(System.getProperty("user.dir")));
        return new SpringApplicationBuilder(BibleVerseAppApplication.class)
                .headless(false)
                .logStartupInfo(false)
                .properties(Map.of("app.database.path", defaultDatabasePath.toString()))
                .run();
    }

    static Path resolveStartupDatabasePath(String configuredDatabasePath, Path searchStart) {
        if (configuredDatabasePath != null && !configuredDatabasePath.isBlank()) {
            return Path.of(configuredDatabasePath);
        }
        return resolveDefaultDatabasePath(searchStart);
    }

    static Path resolveDefaultDatabasePath(Path searchStart) {
        return findRepositoryRoot(searchStart)
                .map(repositoryRoot -> repositoryRoot.resolve("app").resolve("data").resolve(DATABASE_FILE_NAME))
                .orElse(USER_HOME_FALLBACK_DATABASE_PATH);
    }

    static Optional<Path> findRepositoryRoot(Path searchStart) {
        for (Path current = searchStart.toAbsolutePath(); current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("app"))
                    && Files.isDirectory(current.resolve("scraper"))) {
                return Optional.of(current);
            }
        }

        return Optional.empty();
    }

    Scene createScene(MainViewFactory mainViewFactory) {
        return new Scene(mainViewFactory.createRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    Image loadApplicationIcon() {
        InputStream iconStream = getApplicationIconStream();
        return iconStream == null ? createFallbackApplicationIcon() : loadApplicationIcon(iconStream);
    }

    InputStream getApplicationIconStream() {
        return JavaFxApplication.class.getResourceAsStream(APPLICATION_ICON_RESOURCE);
    }

    Image loadApplicationIcon(InputStream iconStream) {
        try (InputStream closableIconStream = iconStream) {
            return new Image(closableIconStream);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load application icon.", ex);
        }
    }

    Image createFallbackApplicationIcon() {
        Canvas canvas = new Canvas(APPLICATION_ICON_SIZE, APPLICATION_ICON_SIZE);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        graphics.setFill(Color.web("#123A67"));
        graphics.fillRoundRect(0, 0, APPLICATION_ICON_SIZE, APPLICATION_ICON_SIZE, 56, 56);

        graphics.setFill(Color.web("#0E1B2E"));
        graphics.fillRoundRect(44, 30, 154, 184, 24, 24);
        graphics.setStroke(Color.web("#32445B"));
        graphics.setLineWidth(4);
        graphics.strokeRoundRect(44, 30, 154, 184, 24, 24);

        graphics.setStroke(Color.web("#C89B3C"));
        graphics.setLineWidth(6);
        graphics.strokeLine(58, 62, 58, 192);
        graphics.strokeLine(58, 90, 58, 118);

        graphics.setFill(Color.web("#D8A63F"));
        graphics.fillRect(106, 74, 16, 58);
        graphics.fillRect(86, 94, 56, 16);

        graphics.setFill(Color.web("#B7862A"));
        graphics.fillRect(62, 198, 126, 20);
        graphics.setFill(Color.web("#D8A63F"));
        graphics.fillPolygon(new double[]{100, 116, 108}, new double[]{198, 198, 228}, 3);

        graphics.setFill(Color.rgb(255, 255, 255, 0.18));
        graphics.fillOval(112, 98, 92, 92);
        graphics.setStroke(Color.web("#D6A548"));
        graphics.setLineWidth(8);
        graphics.strokeOval(112, 98, 92, 92);
        graphics.setStroke(Color.web("#6A402B"));
        graphics.setLineWidth(14);
        graphics.strokeLine(182, 170, 222, 220);
        graphics.setStroke(Color.web("#D6A548"));
        graphics.setLineWidth(10);
        graphics.strokeLine(182, 170, 194, 184);
        graphics.strokeLine(210, 204, 222, 220);

        WritableImage image = new WritableImage(APPLICATION_ICON_SIZE, APPLICATION_ICON_SIZE);
        canvas.snapshot(new SnapshotParameters(), image);
        return image;
    }

    void setContext(ConfigurableApplicationContext context) {
        this.context = context;
    }

    void closeContext() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    private ConfigurableApplicationContext requireContext() {
        if (context == null) {
            throw new IllegalStateException("Application context is not initialized.");
        }
        return context;
    }
}
