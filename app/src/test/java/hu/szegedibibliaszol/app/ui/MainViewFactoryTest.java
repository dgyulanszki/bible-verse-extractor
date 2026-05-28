package hu.szegedibibliaszol.app.ui;

import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.testutil.FxTestSupport;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainViewFactoryTest {

    @BeforeAll
    static void initializeJavaFx() {
        FxTestSupport.initialize();
    }

    @Test
    void createRootBuildsBaseUiAndUpdatesStatusMessages() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService());

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox header = (VBox) root.getTop();
            HBox filters = (HBox) header.getChildren().get(2);
            ComboBox<String> translationBox = (ComboBox<String>) filters.getChildren().get(0);
            ComboBox<String> bookBox = (ComboBox<String>) filters.getChildren().get(1);
            ComboBox<Integer> chapterBox = (ComboBox<Integer>) filters.getChildren().get(2);
            ComboBox<Integer> verseBox = (ComboBox<Integer>) filters.getChildren().get(3);
            Button resetButton = (Button) filters.getChildren().get(4);
            Label statusLabel = (Label) header.getChildren().get(3);
            TableView<VerseRow> tableView = (TableView<VerseRow>) root.getCenter();

            assertEquals(MainViewFactory.INITIAL_STATUS_MESSAGE, statusLabel.getText());
            assertEquals("Translation", translationBox.getPromptText());
            assertEquals("Book", bookBox.getPromptText());
            assertEquals("Chapter", chapterBox.getPromptText());
            assertEquals("Verse", verseBox.getPromptText());
            assertTrue(translationBox.getItems().isEmpty());
            assertEquals(MainViewFactory.EMPTY_RESULTS_MESSAGE, ((Label) tableView.getPlaceholder()).getText());

            translationBox.setValue("KJV");
            assertEquals("Translation selected. Now choose a book.", statusLabel.getText());

            bookBox.setValue("John");
            assertEquals("Book selected. Choose a chapter.", statusLabel.getText());

            chapterBox.setValue(3);
            assertEquals("Chapter selected. You can refine to a specific verse or inspect the full chapter below.", statusLabel.getText());
            assertTrue(tableView.getItems().isEmpty());

            verseBox.setValue(16);
            assertEquals("Displaying the selected verse.", statusLabel.getText());

            verseBox.setValue(null);
            assertEquals("Displaying all verses in the selected chapter.", statusLabel.getText());

            resetButton.fire();
            assertNull(translationBox.getValue());
            assertTrue(bookBox.getItems().isEmpty());
            assertTrue(chapterBox.getItems().isEmpty());
            assertTrue(verseBox.getItems().isEmpty());
            assertSame(root.getCenter(), tableView);
            assertEquals("Selections cleared. Choose a translation to begin again.", statusLabel.getText());
            return null;
        });
    }

    @Test
    void createHelpAlertUsesExpectedContent() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService());

        FxTestSupport.runOnFxThread(() -> {
            Alert alert = mainViewFactory.createHelpAlert();

            assertEquals("Help", alert.getTitle());
            assertEquals(MainViewFactory.HELP_HEADER_TEXT, alert.getHeaderText());
            assertEquals(mainViewFactory.helpContentText(), alert.getContentText());
            alert.close();
            return null;
        });
    }
}

