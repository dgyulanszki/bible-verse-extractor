package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.ui.MainViewFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private static final double DEFAULT_WIDTH = 1180;
    private static final double DEFAULT_HEIGHT = 760;
    private static final double MINIMUM_WIDTH = 960;
    private static final double MINIMUM_HEIGHT = 640;

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
        closeContext();
        Platform.exit();
    }

    ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(BibleVerseAppApplication.class)
                .headless(false)
                .run();
    }

    Scene createScene(MainViewFactory mainViewFactory) {
        return new Scene(mainViewFactory.createRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    void configureStage(Stage stage, Scene scene) {
        stage.setTitle("Bible Verse Tool");
        stage.setScene(scene);
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);
        stage.show();
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
