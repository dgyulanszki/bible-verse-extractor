package hu.szegedibibliaszol.app.ui;

import hu.szegedibibliaszol.app.service.UiSessionService;
import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import hu.szegedibibliaszol.app.ui.model.RangeSelectionSnapshot;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainViewFactory {

    public static final String APPLICATION_TITLE = "Bibliai igeeszköz";
    static final String INITIAL_STATUS_MESSAGE = "Válassz fordítást a közös bibliaadatbázisból való igerészletek összeállításához.";
    static final String GENERAL_HELP_HEADER_TEXT = "Általános súgó";
    static final String TUTORIAL_HEADER_TEXT = "Gyors útmutató";
    static final String TRANSLATION_HELP_HEADER_TEXT = "Fordítássor súgó";
    static final String RANGE_HELP_HEADER_TEXT = "Szakaszsor súgó";
    private static final String TRANSLATION_PLACEHOLDER = "Fordítás";
    private static final String BOOK_PLACEHOLDER = "Könyv";
    private static final String CHAPTER_PLACEHOLDER = "Fejezet";
    private static final String FROM_VERSE_PLACEHOLDER = "Kezdő vers";
    private static final String TO_VERSE_PLACEHOLDER = "Záró vers";
    private static final String EDITOR_UPDATE_PROPERTY = "editorUpdate";
    private static final String ACCENT_BLUE = "#355C8A";
    private static final String DEEP_BLUE = "#25476F";
    private static final String PALE_BLUE = "#EAF2FB";
    private static final String PANEL_WHITE = "#FFFFFF";
    private static final String RANGE_RESET_BUTTON_TEXT = "↺";
    private static final Map<String, String> CANONICAL_BOOK_NAMES_BY_KEY = createCanonicalBookNamesByKey();

    private final VerseBrowserService verseBrowserService;
    private final UiSessionService uiSessionService;

    private ComboBox<String> activeTranslationBox;
    private List<RangeSelectionControls> activeRangeSelections = List.of();
    private boolean suppressTranslationChangeHandling;

    @Autowired
    public MainViewFactory(VerseBrowserService verseBrowserService, UiSessionService uiSessionService) {
        this.verseBrowserService = verseBrowserService;
        this.uiSessionService = uiSessionService;
    }

    MainViewFactory(VerseBrowserService verseBrowserService) {
        this(verseBrowserService, new UiSessionService() {
            @Override
            public Optional<AppSessionSnapshot> loadSession() {
                return Optional.empty();
            }

            @Override
            public void saveSession(AppSessionSnapshot sessionSnapshot) {
            }
        });
    }

    public Parent createRoot() {
        ComboBox<String> translationBox = new ComboBox<>();
        Button translationHelpButton = createHelpButton("A fordítássor súgójának megnyitása");
        Button addRangeButton = new Button("+");
        Button saveButton = new Button("Mentés");
        Button copyButton = new Button("Másolás");
        Button resetButton = new Button("Alaphelyzet");
        Button tutorialButton = new Button("Útmutató");
        Button generalHelpButton = createHelpButton("Az általános súgó megnyitása");
        Label statusLabel = new Label(INITIAL_STATUS_MESSAGE);
        VBox rangeSelectionsBox = new VBox(10);
        List<RangeSelectionControls> rangeSelections = new ArrayList<>();
        ScrollPane mainScrollPane = new ScrollPane();

        this.activeTranslationBox = translationBox;
        this.activeRangeSelections = rangeSelections;

        configurePlaceholderDisplay(translationBox, TRANSLATION_PLACEHOLDER);
        setComboBoxItems(translationBox, verseBrowserService.getTranslations());
        configureSelectionTooltip(
                translationBox,
                "Fordítás kiválasztása a közös bibliaadatbázisból.",
                "Kiválasztott fordítás: "
        );
        addRangeButton.setTooltip(new Tooltip("Új szakasz hozzáadása (Numpad +)"));
        addRangeButton.setDisable(true);
        addRangeButton.setStyle(primaryButtonStyle());
        copyButton.setDisable(true);
        saveButton.setTooltip(new Tooltip("Az aktuális munkamenet mentése (Ctrl+S)"));
        copyButton.setTooltip(new Tooltip("Az összes kész szakasz másolása a vágólapra (Ctrl+C)"));
        resetButton.setTooltip(new Tooltip("Az összes kijelölés alaphelyzetbe állítása (Ctrl+R)"));
        saveButton.setStyle(secondaryButtonStyle());
        copyButton.setStyle(primaryButtonStyle());
        resetButton.setStyle(secondaryButtonStyle());
        tutorialButton.setStyle(primaryButtonStyle());
        tutorialButton.setTooltip(new Tooltip("Gyors kezdési útmutató megnyitása új felhasználóknak"));
        translationHelpButton.setStyle(helpButtonStyle());
        generalHelpButton.setStyle(helpButtonStyle());
        statusLabel.setStyle("-fx-text-fill: " + PANEL_WHITE + "; -fx-font-weight: bold;");
        bindLabelTooltip(statusLabel);

        Runnable refreshCopyState = () -> copyButton.setDisable(!isCopyReady(translationBox.getValue(), rangeSelections));
        rebuildRangeSelections(rangeSelectionsBox, rangeSelections, refreshCopyState);

        translationBox.valueProperty().addListener((_, oldValue, newValue) -> {
            if (suppressTranslationChangeHandling || Objects.equals(oldValue, newValue)) {
                return;
            }
            if (shouldConfirmTranslationChange(oldValue, newValue, rangeSelections)
                    && !showConfirmationDialog(
                    "Fordítás módosítása",
                    "Biztosan módosítod a fordítást?",
                    "A fordítás módosítása törli az összes szakasz könyv-, fejezet- és versválasztását."
            )) {
                restoreTranslationSelection(translationBox, oldValue);
                return;
            }
            applyTranslationSelection(newValue, rangeSelections, addRangeButton, statusLabel, refreshCopyState);
        });

        translationHelpButton.setOnAction(_ -> createTranslationHelpAlert().showAndWait());
        tutorialButton.setOnAction(_ -> createTutorialAlert().showAndWait());
        generalHelpButton.setOnAction(_ -> createGeneralHelpAlert().showAndWait());

        addRangeButton.setOnAction(_ -> {
            RangeSelectionControls rangeSelection = addRangeSelection(rangeSelectionsBox, rangeSelections, refreshCopyState);
            if (translationBox.getValue() != null) {
                rangeSelection.suppressSelectionChangeHandling = true;
                try {
                    setComboBoxItems(rangeSelection.bookBox, verseBrowserService.getBooks(translationBox.getValue()));
                } finally {
                    rangeSelection.suppressSelectionChangeHandling = false;
                }
            }
            refreshRangeSelectionPresentation(rangeSelections);
            refreshCopyState.run();
            statusLabel.setText("Új szakasz hozzáadva. Válassz könyvet.");
            mainScrollPane.setVvalue(1.0);
        });

        saveButton.setOnAction(_ -> {
            saveCurrentSession();
            statusLabel.setText("A munkamenet elmentve.");
        });

        copyButton.setOnAction(_ -> {
            List<String> formattedRanges = rangeSelections.stream()
                    .map(rangeSelection -> formatVerseRangeText(
                            verseBrowserService.findVerseRange(
                                    translationBox.getValue(),
                                    rangeSelection.bookBox.getValue(),
                                    rangeSelection.chapterBox.getValue(),
                                    rangeSelection.fromVerseBox.getValue(),
                                    rangeSelection.toVerseBox.getValue()
                            ),
                            rangeSelection.fromVerseBox.getValue(),
                            rangeSelection.toVerseBox.getValue()
                    ))
                    .toList();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(formatVerseRangesText(formattedRanges));
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            statusLabel.setText("A kijelölt szakaszok a vágólapra kerültek.");
        });

        resetButton.setOnAction(_ -> {
            suppressTranslationChangeHandling = true;
            try {
                translationBox.setValue(null);
            } finally {
                suppressTranslationChangeHandling = false;
            }
            setComboBoxItems(translationBox, verseBrowserService.getTranslations());
            rebuildRangeSelections(rangeSelectionsBox, rangeSelections, refreshCopyState);
            addRangeButton.setDisable(true);
            statusLabel.setText(INITIAL_STATUS_MESSAGE);
        });

        HBox generalHelpRow = new HBox(8, tutorialButton, generalHelpButton);
        generalHelpRow.setAlignment(Pos.CENTER_RIGHT);
        generalHelpRow.setPadding(new Insets(0, 12, 0, 12));

        HBox translationRow = new HBox(12,
                translationBox,
                translationHelpButton,
                saveButton,
                copyButton,
                resetButton
        );
        translationRow.setPadding(new Insets(0, 12, 4, 12));
        translationRow.setStyle(panelRowStyle());

        HBox addRangeRow = new HBox(addRangeButton);
        addRangeRow.setPadding(new Insets(0, 12, 4, 12));

        Label titleLabel = new Label(APPLICATION_TITLE);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        bindLabelTooltip(titleLabel);

        Label subtitleLabel = new Label("Válassz fordítást, állíts össze egy vagy több igerészletet, majd másold a szöveget a vágólapra.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-text-fill: " + PALE_BLUE + ";");
        bindLabelTooltip(subtitleLabel);

        VBox mainContent = new VBox(6,
                titleLabel,
                subtitleLabel,
                generalHelpRow,
                translationRow,
                rangeSelectionsBox,
                addRangeRow,
                statusLabel,
                createContentPanel()
        );
        mainContent.setPadding(new Insets(16));
        mainContent.setStyle("-fx-background-color: linear-gradient(to bottom, " + DEEP_BLUE + ", " + ACCENT_BLUE + ");");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + DEEP_BLUE + ";");
        mainScrollPane.setContent(mainContent);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setStyle("-fx-background: " + DEEP_BLUE + "; -fx-background-color: transparent;");
        root.setCenter(mainScrollPane);
        registerKeyboardShortcuts(root, addRangeButton, saveButton, copyButton, resetButton);

        HBox.setHgrow(translationBox, Priority.ALWAYS);

        restoreSavedSession(translationBox, addRangeButton, rangeSelectionsBox, rangeSelections, statusLabel, refreshCopyState);
        refreshCopyState.run();
        return root;
    }

    Alert createGeneralHelpAlert() {
        return createInformationAlert("Súgó", GENERAL_HELP_HEADER_TEXT, generalHelpContentText());
    }

    Alert createTutorialAlert() {
        return createInformationAlert("Útmutató", TUTORIAL_HEADER_TEXT, tutorialContentText());
    }

    Alert createTranslationHelpAlert() {
        return createInformationAlert("Súgó", TRANSLATION_HELP_HEADER_TEXT, translationHelpContentText());
    }

    Alert createRangeHelpAlert() {
        return createInformationAlert("Súgó", RANGE_HELP_HEADER_TEXT, rangeHelpContentText());
    }

    String generalHelpContentText() {
        return """
                1. Válassz fordítást.
                2. Add hozzá a szükséges szakaszsorokat a + gombbal vagy a numerikus billentyűzet + gombjával.
                3. Minden sorban válassz könyvet, fejezetet, majd kezdő és záró verset.
                4. A ↺ gomb csak az adott szakaszsort állítja vissza alaphelyzetbe.
                5. A kezdő és záró vers lehet azonos is.
                6. Az Útmutató gomb új felhasználóknak rövid, lépésenkénti kezdési segítséget ad.
                7. A Mentés gomb vagy a Ctrl+S billentyűparancs kézzel elmenti az aktuális munkamenetet.
                8. A Másolás gomb vagy a Ctrl+C billentyűparancs az összes kész szakaszt egyetlen blokkba másolja.
                9. Az Alaphelyzet gomb vagy a Ctrl+R billentyűparancs törli az aktuális kijelöléseket.
                10. Ha egy felirat rövidítve látszik, vidd fölé az egeret: a teljes szöveg buboréksúgóban megjelenik.
                11. Az alkalmazás bezárásakor a munkamenet automatikusan elmentődik.
                """;
    }

    String tutorialContentText() {
        return """
                Gyors kezdés:
                1. Válassz egy fordítást a felső legördülőből.
                2. Ha több szakasz kell, kattints a + gombra vagy nyomd meg a numerikus billentyűzet + gombját.
                3. Minden szakaszsorban válassz könyvet, fejezetet, kezdő verset és záró verset.
                4. Ha egy sort törölnél vagy újrakezdenél, használd a - vagy a ↺ gombot.
                5. Ha elkészültél, a Másolás gombbal vagy a Ctrl+C billentyűparanccsal másold a szöveget a vágólapra.
                6. A Mentés gombbal vagy a Ctrl+S billentyűparanccsal mentsd el a munkamenetet.
                7. Az Alaphelyzet gombbal vagy a Ctrl+R billentyűparanccsal mindent lenullázhatsz.
                8. Ha valamelyik felirat rövidítve látszik, vidd fölé az egeret a teljes buboréksúgóért.
                """;
    }

    String translationHelpContentText() {
        return """
                A fordítássorban választhatod ki a közös fordítást minden szakaszhoz.
                - A + gomb vagy a numerikus billentyűzet + gombja új szakaszsort ad hozzá.
                - A Mentés gomb vagy a Ctrl+S billentyűparancs kézzel elmenti az aktuális munkamenetet.
                - A Másolás gomb vagy a Ctrl+C billentyűparancs az összes kész szakaszt a vágólapra másolja.
                - Az Alaphelyzet gomb vagy a Ctrl+R billentyűparancs törli az aktuális kijelöléseket.
                - A fordítás módosítását a program megerősítteti, ha már van kitöltött alsó szintű választás.
                """;
    }

    String rangeHelpContentText() {
        return """
                Minden szakaszsorban ugyanahhoz a fordításhoz választhatod könyvet, fejezetet és vershatárokat.
                - A legördülők első eleme a kiürített, alapértelmezett állapot.
                - A ↺ gomb csak az adott sort állítja vissza alaphelyzetbe.
                - A kezdő és záró vers lehet ugyanaz is.
                - A könyv módosítását a program megerősítteti, ha a sorban már van fejezet- vagy versválasztás.
                - A - gomb eltávolítja az adott szakaszsort.
                - A sor szövegei fölé húzva az egeret a teljes felirat buboréksúgóban látható.
                """;
    }

    public void saveCurrentSession() {
        uiSessionService.saveSession(currentSessionSnapshot());
    }

    String formatVerseRangeText(List<VerseRow> verseRows, int fromVerse, int toVerse) {
        if (verseRows.isEmpty()) {
            throw new IllegalArgumentException("Legalább egy vers szükséges a szakasz formázásához.");
        }

        VerseRow firstVerse = verseRows.getFirst();
        String lineSeparator = System.lineSeparator();
        String verseBlocks = verseRows.stream()
                .map(verseRow -> verseRow.verse() + lineSeparator + verseRow.text())
                .reduce((left, right) -> left + lineSeparator + lineSeparator + right)
                .orElseThrow();
        return canonicalBookName(firstVerse.book())
                + " "
                + firstVerse.chapter()
                + ":"
                + (fromVerse == toVerse ? String.valueOf(fromVerse) : fromVerse + "-" + toVerse)
                + lineSeparator + lineSeparator
                + verseBlocks;
    }

    String formatVerseRangesText(List<String> formattedRanges) {
        if (formattedRanges.isEmpty()) {
            throw new IllegalArgumentException("Legalább egy formázott szakasz szükséges.");
        }
        String lineSeparator = System.lineSeparator();
        return String.join(lineSeparator + lineSeparator, formattedRanges);
    }

    boolean showConfirmationDialog(String title, String header, String content) {
        ButtonType yesButton = new ButtonType("Igen", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Nem", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(yesButton, noButton);
        return showConfirmationDialogAndWait(alert, noButton) == yesButton;
    }

    ButtonType showConfirmationDialogAndWait(Alert alert, ButtonType fallbackButton) {
        return alert.showAndWait().orElse(fallbackButton);
    }

    static boolean isRangeReady(Integer fromVerse, Integer toVerse) {
        return fromVerse != null && toVerse != null && fromVerse <= toVerse;
    }

    static String rangeSelectionStatus(int rangeIndex, Integer fromVerse, Integer toVerse) {
        if (fromVerse == null && toVerse == null) {
            return "A(z) " + rangeIndex + ". szakaszhoz válaszd ki a kezdő és záró verset.";
        }
        if (fromVerse == null) {
            return "A(z) " + rangeIndex + ". szakaszhoz válassz kezdő verset, amely nem nagyobb a záró versnél.";
        }
        if (toVerse == null) {
            return "A(z) " + rangeIndex + ". szakaszhoz válassz záró verset, amely nem kisebb a kezdő versnél.";
        }
        return "A(z) " + rangeIndex + ". szakasz kész. Adj hozzá újabbat, vagy kattints a Másolás gombra.";
    }

    private Button createHelpButton(String tooltipText) {
        Button helpButton = new Button("?");
        helpButton.setTooltip(new Tooltip(tooltipText));
        return helpButton;
    }

    private String primaryButtonStyle() {
        return "-fx-font-weight: bold; -fx-background-color: " + PANEL_WHITE + "; -fx-text-fill: " + DEEP_BLUE
                + "; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + PANEL_WHITE + ";";
    }

    private String secondaryButtonStyle() {
        return "-fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: " + PANEL_WHITE
                + "; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + PANEL_WHITE + ";";
    }

    private String helpButtonStyle() {
        return "-fx-font-weight: bold; -fx-background-color: " + DEEP_BLUE + "; -fx-text-fill: " + PANEL_WHITE
                + "; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: " + PANEL_WHITE + ";";
    }

    private String panelRowStyle() {
        return "-fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 10;"
                + " -fx-border-color: rgba(255, 255, 255, 0.32); -fx-border-radius: 10; -fx-padding: 10;";
    }

    private String comboBoxStyle() {
        return "-fx-background-color: " + PANEL_WHITE + "; -fx-text-fill: " + DEEP_BLUE
                + "; -fx-border-color: " + ACCENT_BLUE + "; -fx-border-radius: 8; -fx-background-radius: 8;";
    }

    private Alert createInformationAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }

    private void bindLabelTooltip(Label label) {
        Tooltip tooltip = new Tooltip(textOrEmpty(label.getText()));
        label.setTooltip(tooltip);
        label.textProperty().addListener((_, _, newText) -> tooltip.setText(textOrEmpty(newText)));
    }

    private <T> void configureSelectionTooltip(ComboBox<T> comboBox, String emptyTooltipText, String selectedValuePrefix) {
        Tooltip tooltip = new Tooltip(emptyTooltipText);
        comboBox.setTooltip(tooltip);
        if (comboBox.isEditable()) {
            comboBox.getEditor().setTooltip(tooltip);
        }
        comboBox.valueProperty().addListener((_, _, newValue) -> tooltip.setText(
                newValue == null ? emptyTooltipText : selectedValuePrefix + newValue
        ));
    }

    private String textOrEmpty(String text) {
        return text == null ? "" : text;
    }

    private void configurePlaceholderDisplay(ComboBox<?> comboBox, String placeholderText) {
        comboBox.setPromptText(placeholderText);
        comboBox.setStyle(comboBoxStyle());
        comboBox.setCellFactory(_ -> new PlaceholderListCell<>(placeholderText));
        PlaceholderListCell<Object> buttonCell = new PlaceholderListCell<>(placeholderText);
        comboBox.valueProperty().addListener((_, _, newValue) -> buttonCell.setText(
                newValue == null ? placeholderText : String.valueOf(newValue)
        ));
        @SuppressWarnings("unchecked")
        ComboBox<Object> typedComboBox = (ComboBox<Object>) comboBox;
        typedComboBox.setEditable(false);
        typedComboBox.valueProperty().addListener((_, _, newValue) -> {
            if (typedComboBox.isEditable()) {
                updateEditorText(typedComboBox, newValue);
            }
        });
        typedComboBox.setButtonCell(buttonCell);
    }

    private void configureNumericQuickSelection(ComboBox<Integer> comboBox, String placeholderText) {
        configurePlaceholderDisplay(comboBox, placeholderText);
        comboBox.setEditable(true);
        comboBox.setConverter(integerComboBoxConverter());
        comboBox.getEditor().setPromptText(placeholderText);
        comboBox.getEditor().setTextFormatter(new TextFormatter<>(change -> isNumericEditorChangeAllowed(comboBox, change)
                ? change
                : null));
        comboBox.valueProperty().addListener((_, _, newValue) -> updateEditorText(comboBox, newValue));
        comboBox.getEditor().textProperty().addListener((_, _, newText) -> {
            if (!isEditorUpdateInProgress(comboBox)) {
                applyNumericQuickSelection(comboBox, newText);
            }
        });
        comboBox.getEditor().focusedProperty().addListener((_, _, focused) -> {
            if (focused) {
                comboBox.getEditor().selectAll();
                return;
            }
            commitNumericEditorText(comboBox);
        });
        comboBox.getEditor().setOnAction(_ -> commitNumericEditorText(comboBox));
    }

    private VBox createContentPanel() {
        Label instructionsLabel = new Label(
                "Ebben a nézetben nem jelenik meg verslista. A fenti vezérlőkkel állítsd össze a kívánt szakaszokat, majd kattints a Másolás gombra."
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-text-fill: " + DEEP_BLUE + ";");
        bindLabelTooltip(instructionsLabel);

        VBox contentPanel = new VBox(instructionsLabel);
        contentPanel.setPadding(new Insets(12));
        contentPanel.setStyle("-fx-background-color: " + PANEL_WHITE + "; -fx-background-radius: 12; -fx-border-color: "
                + ACCENT_BLUE
                + "; -fx-border-width: 0 0 0 6; -fx-border-radius: 12;");
        return contentPanel;
    }

    private void registerKeyboardShortcuts(
            Parent root,
            Button addRangeButton,
            Button saveButton,
            Button copyButton,
            Button resetButton
    ) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.S) {
                    fireIfEnabled(saveButton, event);
                    return;
                }
                if (event.getCode() == KeyCode.C) {
                    fireIfEnabled(copyButton, event);
                    return;
                }
                if (event.getCode() == KeyCode.R) {
                    fireIfEnabled(resetButton, event);
                    return;
                }
            }
            if (event.getCode() == KeyCode.ADD && !addRangeButton.isDisable()) {
                addRangeButton.fire();
                event.consume();
            }
        });
    }

    private void fireIfEnabled(Button button, KeyEvent event) {
        if (!button.isDisable()) {
            button.fire();
            event.consume();
        }
    }

    private void restoreSavedSession(
            ComboBox<String> translationBox,
            Button addRangeButton,
            VBox rangeSelectionsBox,
            List<RangeSelectionControls> rangeSelections,
            Label statusLabel,
            Runnable refreshCopyState
    ) {
        Optional<AppSessionSnapshot> savedSession = uiSessionService.loadSession();
        if (savedSession.isEmpty() || savedSession.get().isEmpty()) {
            return;
        }

        AppSessionSnapshot sessionSnapshot = savedSession.get();
        int desiredRangeCount = Math.max(1, sessionSnapshot.ranges().size());
        rebuildRangeSelections(rangeSelectionsBox, rangeSelections, refreshCopyState, desiredRangeCount);
        suppressTranslationChangeHandling = true;
        try {
            translationBox.setValue(sessionSnapshot.translation());
        } finally {
            suppressTranslationChangeHandling = false;
        }
        applyTranslationSelection(sessionSnapshot.translation(), rangeSelections, addRangeButton, statusLabel, refreshCopyState);
        for (int index = 0; index < sessionSnapshot.ranges().size(); index++) {
            restoreRangeSelection(rangeSelections.get(index), sessionSnapshot.translation(), sessionSnapshot.ranges().get(index), refreshCopyState);
        }
        refreshCopyState.run();
        statusLabel.setText("Az előző munkamenet automatikusan betöltődött.");
    }

    private AppSessionSnapshot currentSessionSnapshot() {
        if (activeTranslationBox == null || activeTranslationBox.getValue() == null) {
            return new AppSessionSnapshot(null, List.of());
        }
        return new AppSessionSnapshot(
                activeTranslationBox.getValue(),
                activeRangeSelections.stream()
                        .map(rangeSelection -> new RangeSelectionSnapshot(
                                rangeSelection.bookBox.getValue(),
                                rangeSelection.chapterBox.getValue(),
                                rangeSelection.fromVerseBox.getValue(),
                                rangeSelection.toVerseBox.getValue()
                        ))
                        .toList()
        );
    }

    private void rebuildRangeSelections(
            VBox rangeSelectionsBox,
            List<RangeSelectionControls> rangeSelections,
            Runnable refreshCopyState
    ) {
        rebuildRangeSelections(rangeSelectionsBox, rangeSelections, refreshCopyState, 1);
    }

    private void rebuildRangeSelections(
            VBox rangeSelectionsBox,
            List<RangeSelectionControls> rangeSelections,
            Runnable refreshCopyState,
            int desiredRangeCount
    ) {
        rangeSelectionsBox.getChildren().clear();
        rangeSelections.clear();
        for (int index = 0; index < desiredRangeCount; index++) {
            addRangeSelection(rangeSelectionsBox, rangeSelections, refreshCopyState);
        }
        refreshRangeSelectionPresentation(rangeSelections);
        refreshCopyState.run();
    }

    private RangeSelectionControls addRangeSelection(
            VBox rangeSelectionsBox,
            List<RangeSelectionControls> rangeSelections,
            Runnable refreshCopyState
    ) {
        RangeSelectionControls rangeSelection = createRangeSelectionControls(refreshCopyState);
        rangeSelection.removeButton.setOnAction(_ -> {
            rangeSelections.remove(rangeSelection);
            rangeSelectionsBox.getChildren().remove(rangeSelection.container);
            refreshRangeSelectionPresentation(rangeSelections);
            refreshCopyState.run();
        });
        rangeSelections.add(rangeSelection);
        rangeSelectionsBox.getChildren().add(rangeSelection.container);
        return rangeSelection;
    }

    private RangeSelectionControls createRangeSelectionControls(Runnable refreshCopyState) {
        Label rangeLabel = new Label("Szakasz");
        ComboBox<String> bookBox = new ComboBox<>();
        ComboBox<Integer> chapterBox = new ComboBox<>();
        Label chapterSeparatorLabel = new Label(":");
        ComboBox<Integer> fromVerseBox = new ComboBox<>();
        Label verseRangeSeparatorLabel = new Label("-");
        ComboBox<Integer> toVerseBox = new ComboBox<>();
        Button resetButton = new Button(RANGE_RESET_BUTTON_TEXT);
        Button removeButton = new Button("-");
        Button helpButton = createHelpButton("A szakaszsor súgójának megnyitása");

        configurePlaceholderDisplay(bookBox, BOOK_PLACEHOLDER);
        configureNumericQuickSelection(chapterBox, CHAPTER_PLACEHOLDER);
        configureNumericQuickSelection(fromVerseBox, FROM_VERSE_PLACEHOLDER);
        configureNumericQuickSelection(toVerseBox, TO_VERSE_PLACEHOLDER);
        bindLabelTooltip(rangeLabel);
        chapterSeparatorLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        verseRangeSeparatorLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        configureSelectionTooltip(bookBox, "Könyv kiválasztása ehhez a szakaszhoz.", "Kiválasztott könyv: ");
        configureSelectionTooltip(chapterBox, "Fejezet kiválasztása ehhez a szakaszhoz.", "Kiválasztott fejezet: ");
        configureSelectionTooltip(fromVerseBox, "Kezdő vers kiválasztása ehhez a szakaszhoz.", "Kiválasztott kezdő vers: ");
        configureSelectionTooltip(toVerseBox, "Záró vers kiválasztása ehhez a szakaszhoz.", "Kiválasztott záró vers: ");
        rangeLabel.setMinWidth(96);
        rangeLabel.setPrefWidth(96);
        rangeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        resetButton.setStyle(secondaryButtonStyle());
        resetButton.setTooltip(new Tooltip("A szakaszsor alaphelyzetbe állítása"));
        removeButton.setStyle(secondaryButtonStyle());
        removeButton.setTooltip(new Tooltip("A szakaszsor eltávolítása"));
        helpButton.setStyle(helpButtonStyle());
        helpButton.setOnAction(_ -> createRangeHelpAlert().showAndWait());

        RangeSelectionControls rangeSelection = new RangeSelectionControls(
                new HBox(12, rangeLabel, bookBox, chapterBox, chapterSeparatorLabel, fromVerseBox, verseRangeSeparatorLabel, toVerseBox, resetButton, removeButton),
                rangeLabel,
                bookBox,
                chapterBox,
                fromVerseBox,
                toVerseBox,
                removeButton,
                helpButton
        );
        bindLabelTooltip(rangeSelection.statusLabel);
        rangeSelection.container.setAlignment(Pos.CENTER_LEFT);
        rangeSelection.container.setPadding(new Insets(4, 12, 4, 12));
        rangeSelection.container.setStyle(panelRowStyle());
        HBox.setHgrow(bookBox, Priority.ALWAYS);

        setComboBoxItems(bookBox, List.of());
        setComboBoxItems(chapterBox, List.of());
        setComboBoxItems(fromVerseBox, List.of());
        setComboBoxItems(toVerseBox, List.of());
        resetButton.setOnAction(_ -> resetRangeSelection(
                rangeSelection,
                activeTranslationBox == null ? null : activeTranslationBox.getValue(),
                refreshCopyState
        ));

        bookBox.valueProperty().addListener((_, oldValue, newValue) -> {
            if (rangeSelection.suppressSelectionChangeHandling || Objects.equals(oldValue, newValue)) {
                return;
            }
            if (shouldConfirmBookChange(oldValue, newValue, rangeSelection)
                    && !showConfirmationDialog(
                    "Könyv módosítása",
                    "Biztosan módosítod a könyvet a(z) " + rangeSelection.displayIndex + ". szakaszban?",
                    "A könyv módosítása törli az adott sor fejezet- és versválasztását."
            )) {
                restoreBookSelection(rangeSelection, oldValue);
                return;
            }
            applyBookSelection(rangeSelection, activeTranslationBox == null ? null : activeTranslationBox.getValue(), newValue, refreshCopyState);
        });

        chapterBox.valueProperty().addListener((_, _, newValue) -> {
            if (!rangeSelection.suppressSelectionChangeHandling) {
                applyChapterSelection(rangeSelection, activeTranslationBox == null ? null : activeTranslationBox.getValue(), newValue, refreshCopyState);
            }
        });

        fromVerseBox.valueProperty().addListener((_, _, _) -> {
            if (!rangeSelection.suppressSelectionChangeHandling) {
                updateVerseRangeBoxes(rangeSelection, refreshCopyState);
            }
        });

        toVerseBox.valueProperty().addListener((_, _, _) -> {
            if (!rangeSelection.suppressSelectionChangeHandling) {
                updateVerseRangeBoxes(rangeSelection, refreshCopyState);
            }
        });
        return rangeSelection;
    }

    private void resetRangeSelection(RangeSelectionControls rangeSelection, String translation, Runnable refreshCopyState) {
        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            setComboBoxItems(rangeSelection.bookBox, translation == null ? List.of() : verseBrowserService.getBooks(translation));
            clearComboBox(rangeSelection.chapterBox);
            clearComboBox(rangeSelection.fromVerseBox);
            clearComboBox(rangeSelection.toVerseBox);
            rangeSelection.availableVerses.set(List.of());
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }
        refreshCopyState.run();
        rangeSelection.statusLabel.setText(translation == null
                ? "A(z) " + rangeSelection.displayIndex + ". szakasz alaphelyzetbe állítva. Válassz fordítást."
                : "A(z) " + rangeSelection.displayIndex + ". szakasz alaphelyzetbe állítva. Válassz könyvet.");
    }

    private void applyTranslationSelection(
            String translation,
            List<RangeSelectionControls> rangeSelections,
            Button addRangeButton,
            Label statusLabel,
            Runnable refreshCopyState
    ) {
        for (RangeSelectionControls rangeSelection : rangeSelections) {
            rangeSelection.suppressSelectionChangeHandling = true;
            try {
                setComboBoxItems(rangeSelection.bookBox, translation == null ? List.of() : verseBrowserService.getBooks(translation));
                clearComboBox(rangeSelection.chapterBox);
                clearComboBox(rangeSelection.fromVerseBox);
                clearComboBox(rangeSelection.toVerseBox);
                rangeSelection.availableVerses.set(List.of());
            } finally {
                rangeSelection.suppressSelectionChangeHandling = false;
            }
        }
        addRangeButton.setDisable(translation == null);
        refreshCopyState.run();
        statusLabel.setText(translation == null
                ? INITIAL_STATUS_MESSAGE
                : "A fordítás kiválasztva. Válassz könyvet a szakaszokhoz.");
    }

    private void applyBookSelection(
            RangeSelectionControls rangeSelection,
            String translation,
            String book,
            Runnable refreshCopyState
    ) {
        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            setComboBoxItems(
                    rangeSelection.chapterBox,
                    rangeSelection.bookBox.getValue() == null
                            ? List.of()
                            : verseBrowserService.getChapters(translation, rangeSelection.bookBox.getValue())
            );
            clearComboBox(rangeSelection.fromVerseBox);
            clearComboBox(rangeSelection.toVerseBox);
            rangeSelection.availableVerses.set(List.of());
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }
        refreshCopyState.run();
        rangeSelection.statusLabel.setText(book == null
                ? "A(z) " + rangeSelection.displayIndex + ". szakaszhoz válassz könyvet."
                : "A(z) " + rangeSelection.displayIndex + ". szakaszban a könyv kiválasztva. Válassz fejezetet.");
    }

    private void applyChapterSelection(
            RangeSelectionControls rangeSelection,
            String translation,
            Integer chapter,
            Runnable refreshCopyState
    ) {
        if (chapter == null) {
            rangeSelection.availableVerses.set(List.of());
            clearVerseRangeBoxes(rangeSelection);
            refreshCopyState.run();
            rangeSelection.statusLabel.setText("A(z) " + rangeSelection.displayIndex + ". szakaszhoz válassz fejezetet.");
            return;
        }

        rangeSelection.availableVerses.set(List.copyOf(verseBrowserService.getVerses(
                translation,
                rangeSelection.bookBox.getValue(),
                chapter
        )));
        updateVerseRangeBoxes(rangeSelection, refreshCopyState);
    }

    private void restoreRangeSelection(
            RangeSelectionControls rangeSelection,
            String translation,
            RangeSelectionSnapshot rangeSelectionSnapshot,
            Runnable refreshCopyState
    ) {
        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            selectIfAvailable(rangeSelection.bookBox, rangeSelectionSnapshot.book());
            setComboBoxItems(
                    rangeSelection.chapterBox,
                    rangeSelectionSnapshot.book() == null
                            ? List.of()
                            : verseBrowserService.getChapters(translation, rangeSelectionSnapshot.book())
            );
            selectIfAvailable(rangeSelection.chapterBox, rangeSelectionSnapshot.chapter());
            rangeSelection.availableVerses.set(rangeSelectionSnapshot.chapter() == null
                    ? List.of()
                    : List.copyOf(verseBrowserService.getVerses(
                    translation,
                    rangeSelectionSnapshot.book(),
                    rangeSelectionSnapshot.chapter()
            )));
            updateComboBoxItems(rangeSelection.fromVerseBox, rangeSelection.availableVerses.get());
            updateComboBoxItems(rangeSelection.toVerseBox, rangeSelection.availableVerses.get());
            selectIfAvailable(rangeSelection.fromVerseBox, rangeSelectionSnapshot.fromVerse());
            selectIfAvailable(rangeSelection.toVerseBox, rangeSelectionSnapshot.toVerse());
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }
        updateVerseRangeBoxes(rangeSelection, refreshCopyState);
    }

    private void refreshRangeSelectionPresentation(List<RangeSelectionControls> rangeSelections) {
        boolean disableRemoveButtons = rangeSelections.size() == 1;
        for (int index = 0; index < rangeSelections.size(); index++) {
            RangeSelectionControls rangeSelection = rangeSelections.get(index);
            rangeSelection.displayIndex = index + 1;
            rangeSelection.rangeLabel.setText("Szakasz " + rangeSelection.displayIndex);
            rangeSelection.removeButton.setDisable(disableRemoveButtons);
            boolean showHelpButton = index == 0;
            rangeSelection.helpButton.setManaged(showHelpButton);
            rangeSelection.helpButton.setVisible(showHelpButton);
        }
    }

    private void updateVerseRangeBoxes(RangeSelectionControls rangeSelection, Runnable refreshCopyState) {
        List<Integer> availableVerses = rangeSelection.availableVerses.get();
        if (availableVerses.isEmpty()) {
            clearVerseRangeBoxes(rangeSelection);
            refreshCopyState.run();
            rangeSelection.statusLabel.setText("A(z) " + rangeSelection.displayIndex + ". szakasz kiválasztott fejezetében nincs vers.");
            return;
        }

        Integer selectedFromVerse = rangeSelection.fromVerseBox.getValue();
        Integer selectedToVerse = rangeSelection.toVerseBox.getValue();
        List<Integer> availableFromVerses = selectedToVerse == null
                ? availableVerses
                : availableVerses.stream().filter(verse -> verse <= selectedToVerse).toList();
        List<Integer> availableToVerses = selectedFromVerse == null
                ? availableVerses
                : availableVerses.stream().filter(verse -> verse >= selectedFromVerse).toList();

        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            updateComboBoxItems(rangeSelection.fromVerseBox, availableFromVerses);
            updateComboBoxItems(rangeSelection.toVerseBox, availableToVerses);
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }

        refreshCopyState.run();
        rangeSelection.statusLabel.setText(rangeSelectionStatus(
                rangeSelection.displayIndex,
                rangeSelection.fromVerseBox.getValue(),
                rangeSelection.toVerseBox.getValue()
        ));
    }

    private void clearVerseRangeBoxes(RangeSelectionControls rangeSelection) {
        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            clearComboBox(rangeSelection.fromVerseBox);
            clearComboBox(rangeSelection.toVerseBox);
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }
    }

    private boolean shouldConfirmTranslationChange(
            String oldValue,
            String newValue,
            List<RangeSelectionControls> rangeSelections
    ) {
        return !Objects.equals(oldValue, newValue)
                && rangeSelections.stream().anyMatch(rangeSelection -> rangeSelection.bookBox.getValue() != null
                || rangeSelection.chapterBox.getValue() != null
                || rangeSelection.fromVerseBox.getValue() != null
                || rangeSelection.toVerseBox.getValue() != null);
    }

    private boolean shouldConfirmBookChange(String oldValue, String newValue, RangeSelectionControls rangeSelection) {
        return !Objects.equals(oldValue, newValue)
                && (rangeSelection.chapterBox.getValue() != null
                || rangeSelection.fromVerseBox.getValue() != null
                || rangeSelection.toVerseBox.getValue() != null);
    }

    private boolean isCopyReady(String translation, List<RangeSelectionControls> rangeSelections) {
        return translation != null
                && !rangeSelections.isEmpty()
                && rangeSelections.stream().allMatch(rangeSelection -> rangeSelection.bookBox.getValue() != null
                && rangeSelection.chapterBox.getValue() != null
                && isRangeReady(rangeSelection.fromVerseBox.getValue(), rangeSelection.toVerseBox.getValue()));
    }

    private <T> void setComboBoxItems(ComboBox<T> comboBox, List<T> items) {
        replaceComboBoxItems(comboBox, items);
        comboBox.getSelectionModel().clearSelection();
        comboBox.setValue(null);
    }

    private void updateComboBoxItems(ComboBox<Integer> comboBox, List<Integer> items) {
        Integer currentValue = comboBox.getValue();
        replaceComboBoxItems(comboBox, items);
        if (currentValue == null) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            return;
        }
        if (!items.contains(currentValue)) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            return;
        }
        comboBox.setValue(currentValue);
    }

    private void restoreTranslationSelection(ComboBox<String> translationBox, String value) {
        suppressTranslationChangeHandling = true;
        try {
            translationBox.setValue(value);
        } finally {
            suppressTranslationChangeHandling = false;
        }
    }

    private void restoreBookSelection(RangeSelectionControls rangeSelection, String value) {
        rangeSelection.suppressSelectionChangeHandling = true;
        try {
            rangeSelection.bookBox.setValue(value);
        } finally {
            rangeSelection.suppressSelectionChangeHandling = false;
        }
    }

    private <T> void selectIfAvailable(ComboBox<T> comboBox, T value) {
        if (value != null && comboBox.getItems().contains(value)) {
            comboBox.setValue(value);
            return;
        }
        comboBox.getSelectionModel().clearSelection();
        comboBox.setValue(null);
    }

    private <T> void clearComboBox(ComboBox<T> comboBox) {
        comboBox.getSelectionModel().clearSelection();
        comboBox.setValue(null);
        replaceComboBoxItems(comboBox, List.of());
    }

    private <T> void replaceComboBoxItems(ComboBox<T> comboBox, List<T> items) {
        comboBox.setItems(FXCollections.observableArrayList(withDefaultOption(items)));
    }

    private <T> List<T> withDefaultOption(List<T> items) {
        List<T> valuesWithDefault = new ArrayList<>();
        valuesWithDefault.add(null);
        valuesWithDefault.addAll(items);
        return valuesWithDefault;
    }

    private String canonicalBookName(String book) {
        return CANONICAL_BOOK_NAMES_BY_KEY.getOrDefault(normalizeBookNameKey(book), book);
    }

    private static Map<String, String> createCanonicalBookNamesByKey() {
        Map<String, String> canonicalBookNamesByKey = new LinkedHashMap<>();
        addCanonicalBookName(canonicalBookNamesByKey, "1Mózes");
        addCanonicalBookName(canonicalBookNamesByKey, "2Mózes");
        addCanonicalBookName(canonicalBookNamesByKey, "3Mózes");
        addCanonicalBookName(canonicalBookNamesByKey, "4Mózes");
        addCanonicalBookName(canonicalBookNamesByKey, "5Mózes");
        addCanonicalBookName(canonicalBookNamesByKey, "Józsué");
        addCanonicalBookName(canonicalBookNamesByKey, "Bírák");
        addCanonicalBookName(canonicalBookNamesByKey, "Ruth");
        addCanonicalBookName(canonicalBookNamesByKey, "1Sámuel");
        addCanonicalBookName(canonicalBookNamesByKey, "2Sámuel");
        addCanonicalBookName(canonicalBookNamesByKey, "1Királyok");
        addCanonicalBookName(canonicalBookNamesByKey, "2Királyok");
        addCanonicalBookName(canonicalBookNamesByKey, "1Krónikák");
        addCanonicalBookName(canonicalBookNamesByKey, "2Krónikák");
        addCanonicalBookName(canonicalBookNamesByKey, "Ezsdrás");
        addCanonicalBookName(canonicalBookNamesByKey, "Nehémiás");
        addCanonicalBookName(canonicalBookNamesByKey, "Eszter");
        addCanonicalBookName(canonicalBookNamesByKey, "Jób");
        addCanonicalBookName(canonicalBookNamesByKey, "Zsoltárok");
        addCanonicalBookName(canonicalBookNamesByKey, "Példabeszédek");
        addCanonicalBookName(canonicalBookNamesByKey, "Prédikátor");
        addCanonicalBookName(canonicalBookNamesByKey, "Énekek éneke", "Énekek");
        addCanonicalBookName(canonicalBookNamesByKey, "Ézsaiás");
        addCanonicalBookName(canonicalBookNamesByKey, "Jeremiás");
        addCanonicalBookName(canonicalBookNamesByKey, "Jeremiás siralmai", "Jeremiássiralmai");
        addCanonicalBookName(canonicalBookNamesByKey, "Ezékiel");
        addCanonicalBookName(canonicalBookNamesByKey, "Dániel");
        addCanonicalBookName(canonicalBookNamesByKey, "Hóseás");
        addCanonicalBookName(canonicalBookNamesByKey, "Jóel");
        addCanonicalBookName(canonicalBookNamesByKey, "Ámós", "Ámósz");
        addCanonicalBookName(canonicalBookNamesByKey, "Abdiás");
        addCanonicalBookName(canonicalBookNamesByKey, "Jónás");
        addCanonicalBookName(canonicalBookNamesByKey, "Mikeás");
        addCanonicalBookName(canonicalBookNamesByKey, "Náhum");
        addCanonicalBookName(canonicalBookNamesByKey, "Habakuk");
        addCanonicalBookName(canonicalBookNamesByKey, "Sofóniás", "Zofóniás");
        addCanonicalBookName(canonicalBookNamesByKey, "Aggeus", "Haggeus");
        addCanonicalBookName(canonicalBookNamesByKey, "Zakariás");
        addCanonicalBookName(canonicalBookNamesByKey, "Malakiás");
        addCanonicalBookName(canonicalBookNamesByKey, "Máté");
        addCanonicalBookName(canonicalBookNamesByKey, "Márk");
        addCanonicalBookName(canonicalBookNamesByKey, "Lukács");
        addCanonicalBookName(canonicalBookNamesByKey, "János");
        addCanonicalBookName(canonicalBookNamesByKey, "Apostolok Cselekedetei", "Cselekedetek");
        addCanonicalBookName(canonicalBookNamesByKey, "Róma");
        addCanonicalBookName(canonicalBookNamesByKey, "1Korintus", "1Korinthus");
        addCanonicalBookName(canonicalBookNamesByKey, "2Korintus", "2Korinthus");
        addCanonicalBookName(canonicalBookNamesByKey, "Galata");
        addCanonicalBookName(canonicalBookNamesByKey, "Efezus");
        addCanonicalBookName(canonicalBookNamesByKey, "Filippi");
        addCanonicalBookName(canonicalBookNamesByKey, "Kolossé");
        addCanonicalBookName(canonicalBookNamesByKey, "1Thessalonika", "1Thesszalonika");
        addCanonicalBookName(canonicalBookNamesByKey, "2Thessalonika", "2Thesszalonika");
        addCanonicalBookName(canonicalBookNamesByKey, "1Timóteus");
        addCanonicalBookName(canonicalBookNamesByKey, "2Timóteus");
        addCanonicalBookName(canonicalBookNamesByKey, "Titusz");
        addCanonicalBookName(canonicalBookNamesByKey, "Filemon");
        addCanonicalBookName(canonicalBookNamesByKey, "Zsidók");
        addCanonicalBookName(canonicalBookNamesByKey, "Jakab");
        addCanonicalBookName(canonicalBookNamesByKey, "1Péter");
        addCanonicalBookName(canonicalBookNamesByKey, "2Péter");
        addCanonicalBookName(canonicalBookNamesByKey, "1János");
        addCanonicalBookName(canonicalBookNamesByKey, "2János");
        addCanonicalBookName(canonicalBookNamesByKey, "3János");
        addCanonicalBookName(canonicalBookNamesByKey, "Júdás");
        addCanonicalBookName(canonicalBookNamesByKey, "Jelenések");
        return canonicalBookNamesByKey;
    }

    private static void addCanonicalBookName(Map<String, String> canonicalBookNamesByKey, String canonicalName, String... aliases) {
        canonicalBookNamesByKey.put(normalizeBookNameKey(canonicalName), canonicalName);
        for (String alias : aliases) {
            canonicalBookNamesByKey.put(normalizeBookNameKey(alias), canonicalName);
        }
    }

    private static String normalizeBookNameKey(String book) {
        return book.toLowerCase(Locale.ROOT).replaceAll("[\\s.]", "");
    }

    private <T> void updateEditorText(ComboBox<T> comboBox, T value) {
        runWithEditorUpdate(comboBox, () -> comboBox.getEditor().setText(value == null ? "" : String.valueOf(value)));
    }

    private boolean isNumericEditorChangeAllowed(ComboBox<Integer> comboBox, TextFormatter.Change change) {
        String newText = change.getControlNewText();
        if (newText.isEmpty()) {
            return true;
        }
        if (!newText.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return hasAvailableValueWithPrefix(comboBox, newText);
    }

    private boolean hasAvailableValueWithPrefix(ComboBox<Integer> comboBox, String prefix) {
        return comboBox.getItems().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(value -> value.startsWith(prefix));
    }

    private void applyNumericQuickSelection(ComboBox<Integer> comboBox, String editorText) {
        if (editorText == null || editorText.isBlank()) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            return;
        }
        Integer typedValue = parseInteger(editorText);
        if (typedValue == null || !comboBox.getItems().contains(typedValue)) {
            clearNumericSelectionPreservingEditorText(comboBox, editorText);
            return;
        }
        comboBox.getSelectionModel().select(typedValue);
        comboBox.setValue(typedValue);
        if (comboBox.isShowing() || comboBox.isFocused() || comboBox.getEditor().isFocused()) {
            comboBox.show();
        }
    }

    private void commitNumericEditorText(ComboBox<Integer> comboBox) {
        String editorText = comboBox.getEditor().getText();
        if (editorText == null || editorText.isBlank()) {
            comboBox.setValue(null);
            return;
        }

        Integer typedValue = parseInteger(editorText);
        if (typedValue != null && comboBox.getItems().contains(typedValue)) {
            comboBox.setValue(typedValue);
            return;
        }

        updateEditorText(comboBox, comboBox.getValue());
    }

    private void clearNumericSelectionPreservingEditorText(ComboBox<Integer> comboBox, String editorText) {
        runWithEditorUpdate(comboBox, () -> {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            comboBox.getEditor().setText(editorText);
        });
    }

    private Integer parseInteger(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private StringConverter<Integer> integerComboBoxConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.valueOf(value);
            }

            @Override
            public Integer fromString(String text) {
                return parseInteger(text);
            }
        };
    }

    private boolean isEditorUpdateInProgress(ComboBox<?> comboBox) {
        return Boolean.TRUE.equals(comboBox.getProperties().get(EDITOR_UPDATE_PROPERTY));
    }

    private void runWithEditorUpdate(ComboBox<?> comboBox, Runnable action) {
        comboBox.getProperties().put(EDITOR_UPDATE_PROPERTY, true);
        try {
            action.run();
        } finally {
            comboBox.getProperties().put(EDITOR_UPDATE_PROPERTY, false);
        }
    }

    static final class PlaceholderListCell<T> extends ListCell<T> {

        private final String placeholderText;

        PlaceholderListCell(String placeholderText) {
            this.placeholderText = placeholderText;
            setText(placeholderText);
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? placeholderText : String.valueOf(item));
        }

        void applyItem(T item, boolean empty) {
            updateItem(item, empty);
        }
    }

    private static final class RangeSelectionControls {

        private final HBox container;
        private final Label rangeLabel;
        private final ComboBox<String> bookBox;
        private final ComboBox<Integer> chapterBox;
        private final ComboBox<Integer> fromVerseBox;
        private final ComboBox<Integer> toVerseBox;
        private final Button removeButton;
        private final Button helpButton;
        private final Label statusLabel;
        private final AtomicReference<List<Integer>> availableVerses = new AtomicReference<>(List.of());
        private boolean suppressSelectionChangeHandling;
        private int displayIndex;

        private RangeSelectionControls(
                HBox container,
                Label rangeLabel,
                ComboBox<String> bookBox,
                ComboBox<Integer> chapterBox,
                ComboBox<Integer> fromVerseBox,
                ComboBox<Integer> toVerseBox,
                Button removeButton,
                Button helpButton
        ) {
            this.container = container;
            this.rangeLabel = rangeLabel;
            this.bookBox = bookBox;
            this.chapterBox = chapterBox;
            this.fromVerseBox = fromVerseBox;
            this.toVerseBox = toVerseBox;
            this.removeButton = removeButton;
            this.helpButton = helpButton;
            this.statusLabel = new Label();
            this.container.getChildren().add(this.statusLabel);
            HBox.setHgrow(this.statusLabel, Priority.ALWAYS);
            this.container.getChildren().add(this.helpButton);
        }
    }
}
