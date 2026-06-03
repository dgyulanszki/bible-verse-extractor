package hu.szegedibibliaszol.app.ui;

import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class MainViewFactory {

    static final String INITIAL_STATUS_MESSAGE = "Choose a translation to browse the verses stored in the shared Bible database.";
    static final String EMPTY_RESULTS_MESSAGE = "No verses found in the database for the current selection.";
    static final String HELP_HEADER_TEXT = "How to use the verse browser";

    private final VerseBrowserService verseBrowserService;

    public MainViewFactory(VerseBrowserService verseBrowserService) {
        this.verseBrowserService = verseBrowserService;
    }

    public Parent createRoot() {
        ComboBox<String> translationBox = new ComboBox<>();
        ComboBox<String> bookBox = new ComboBox<>();
        ComboBox<Integer> chapterBox = new ComboBox<>();
        ComboBox<Integer> verseBox = new ComboBox<>();
        Label statusLabel = new Label(INITIAL_STATUS_MESSAGE);
        TableView<VerseRow> tableView = createTableView();

        configurePlaceholderDisplay(translationBox, "Translation");
        configurePlaceholderDisplay(bookBox, "Book");
        configurePlaceholderDisplay(chapterBox, "Chapter");
        configurePlaceholderDisplay(verseBox, "Verse");

        translationBox.setItems(FXCollections.observableArrayList(verseBrowserService.getTranslations()));

        translationBox.valueProperty().addListener((_, _, newValue) -> {
            bookBox.setItems(FXCollections.observableArrayList(verseBrowserService.getBooks(newValue)));
            bookBox.getSelectionModel().clearSelection();
            chapterBox.getItems().clear();
            verseBox.getItems().clear();
            tableView.getItems().clear();
            statusLabel.setText(newValue == null
                    ? INITIAL_STATUS_MESSAGE
                    : "Translation selected. Now choose a book.");
        });

        bookBox.valueProperty().addListener((_, _, newValue) -> {
            chapterBox.setItems(FXCollections.observableArrayList(
                    verseBrowserService.getChapters(translationBox.getValue(), newValue)
            ));
            chapterBox.getSelectionModel().clearSelection();
            verseBox.getItems().clear();
            tableView.getItems().clear();
            statusLabel.setText(newValue == null
                    ? "Choose a book."
                    : "Book selected. Choose a chapter.");
        });

        chapterBox.valueProperty().addListener((_, _, newValue) -> {
            verseBox.setItems(FXCollections.observableArrayList(
                    verseBrowserService.getVerses(translationBox.getValue(), bookBox.getValue(), newValue)
            ));
            tableView.setItems(FXCollections.observableArrayList(
                    verseBrowserService.findVerses(translationBox.getValue(), bookBox.getValue(), newValue, null)
            ));
            statusLabel.setText(newValue == null
                    ? "Choose a chapter."
                    : "Chapter selected. You can refine to a specific verse or inspect the full chapter below.");
        });

        verseBox.valueProperty().addListener((_, _, newValue) -> {
            tableView.setItems(FXCollections.observableArrayList(
                    verseBrowserService.findVerses(translationBox.getValue(), bookBox.getValue(), chapterBox.getValue(), newValue)
            ));
            statusLabel.setText(newValue == null
                    ? "Displaying all verses in the selected chapter."
                    : "Displaying the selected verse.");
        });

        Button helpButton = new Button("?");
        helpButton.setTooltip(new Tooltip("Open usage help"));
        helpButton.setOnAction(_ -> createHelpAlert().showAndWait());

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(_ -> {
            translationBox.getSelectionModel().clearSelection();
            translationBox.setValue(null);
            bookBox.getItems().clear();
            bookBox.setValue(null);
            chapterBox.getItems().clear();
            chapterBox.setValue(null);
            verseBox.getItems().clear();
            verseBox.setValue(null);
            tableView.getItems().clear();
            statusLabel.setText(INITIAL_STATUS_MESSAGE);
        });

        HBox filters = new HBox(12, translationBox, bookBox, chapterBox, verseBox, resetButton, helpButton);
        filters.setPadding(new Insets(12));

        Label titleLabel = new Label("Bible Verse Tool");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Browse verses from the shared Bible database with translation, book, chapter, and verse filters.");

        VBox header = new VBox(6, titleLabel, subtitleLabel, filters, statusLabel);
        header.setPadding(new Insets(16));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tableView);
        BorderPane.setMargin(tableView, new Insets(0, 16, 16, 16));

        HBox.setHgrow(translationBox, Priority.ALWAYS);
        HBox.setHgrow(bookBox, Priority.ALWAYS);
        HBox.setHgrow(chapterBox, Priority.NEVER);
        HBox.setHgrow(verseBox, Priority.NEVER);

        tableView.setItems(FXCollections.observableArrayList());
        return root;
    }

    private TableView<VerseRow> createTableView() {
        TableView<VerseRow> tableView = new TableView<>();

        TableColumn<VerseRow, String> translationColumn = new TableColumn<>("Translation");
        translationColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().translation()));

        TableColumn<VerseRow, String> referenceColumn = new TableColumn<>("Reference");
        referenceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().reference()));

        TableColumn<VerseRow, String> textColumn = new TableColumn<>("Verse Text");
        textColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().text()));

        translationColumn.setPrefWidth(120);
        referenceColumn.setPrefWidth(160);
        textColumn.setPrefWidth(780);

        tableView.getColumns().setAll(List.of(translationColumn, referenceColumn, textColumn));
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(new Label(EMPTY_RESULTS_MESSAGE));
        return tableView;
    }

    Alert createHelpAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText(HELP_HEADER_TEXT);
        alert.setContentText(helpContentText());
        return alert;
    }

    String helpContentText() {
        return "Run the scraper first to populate the shared Bible database, then use the selectors to browse the stored verses.";
    }

    private <T> void configurePlaceholderDisplay(ComboBox<T> comboBox, String placeholderText) {
        comboBox.setPromptText(placeholderText);

        ListCell<T> buttonCell = new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? placeholderText : String.valueOf(item));
            }
        };
        buttonCell.setText(placeholderText);
        comboBox.valueProperty().addListener((_, _, newValue) -> buttonCell.setText(
                newValue == null ? placeholderText : String.valueOf(newValue)
        ));
        comboBox.setButtonCell(buttonCell);
    }
}
