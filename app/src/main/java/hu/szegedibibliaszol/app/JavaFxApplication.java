package hu.szegedibibliaszol.app;

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
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
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

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = createApplicationContext();
    }

    @Override
    public void start(Stage stage) {
        MainViewFactory mainViewFactory = requireContext().getBean(MainViewFactory.class);
        Scene scene = createScene(mainViewFactory);
        configureStage(stage, scene);
    }

    @Override
    public void stop() {
        if (context != null) {
            requireContext().getBean(MainViewFactory.class).saveCurrentSession();
        }
        closeContext();
        Platform.exit();
    }

    ConfigurableApplicationContext createApplicationContext() {
        Path defaultDatabasePath = resolveDefaultDatabasePath(Path.of(System.getProperty("user.dir")));
        return new SpringApplicationBuilder(BibleVerseAppApplication.class)
                .headless(false)
                .properties(Map.of("app.database.path", defaultDatabasePath.toString()))
                .run();
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

    void configureStage(Stage stage, Scene scene) {
        stage.setTitle(MainViewFactory.APPLICATION_TITLE);
        stage.getIcons().setAll(loadApplicationIcon());
        stage.setScene(scene);
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);
        stage.show();
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
