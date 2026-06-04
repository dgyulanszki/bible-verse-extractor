package hu.szegedibibliaszol.app.ui;

import hu.szegedibibliaszol.app.service.UiSessionService;
import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import hu.szegedibibliaszol.app.ui.model.RangeSelectionSnapshot;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainViewFactory {

    public static final String APPLICATION_TITLE = "Bibliai igeeszköz";
    static final String INITIAL_STATUS_MESSAGE = "Válassz fordítást a közös bibliaadatbázisból való igerészletek összeállításához.";
    static final String GENERAL_HELP_HEADER_TEXT = "Általános súgó";
    static final String TRANSLATION_HELP_HEADER_TEXT = "Fordítássor súgó";
    static final String RANGE_HELP_HEADER_TEXT = "Szakaszsor súgó";
    private static final String TRANSLATION_PLACEHOLDER = "Fordítás";
    private static final String BOOK_PLACEHOLDER = "Könyv";
    private static final String CHAPTER_PLACEHOLDER = "Fejezet";
    private static final String FROM_VERSE_PLACEHOLDER = "Kezdő vers";
    private static final String TO_VERSE_PLACEHOLDER = "Záró vers";
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
        Button generalHelpButton = createHelpButton("Az általános súgó megnyitása");
        Label statusLabel = new Label(INITIAL_STATUS_MESSAGE);
        VBox rangeSelectionsBox = new VBox(10);
        List<RangeSelectionControls> rangeSelections = new ArrayList<>();

        this.activeTranslationBox = translationBox;
        this.activeRangeSelections = rangeSelections;

        configurePlaceholderDisplay(translationBox, TRANSLATION_PLACEHOLDER);
        setComboBoxItems(translationBox, verseBrowserService.getTranslations());
        addRangeButton.setTooltip(new Tooltip("Új szakasz hozzáadása"));
        addRangeButton.setDisable(true);
        copyButton.setDisable(true);

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

        HBox translationRow = new HBox(12,
                translationBox,
                translationHelpButton,
                addRangeButton,
                saveButton,
                copyButton,
                resetButton,
                generalHelpButton
        );
        translationRow.setPadding(new Insets(12, 12, 4, 12));

        Label titleLabel = new Label(APPLICATION_TITLE);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Válassz fordítást, állíts össze egy vagy több igerészletet, majd másold a szöveget a vágólapra.");
        subtitleLabel.setWrapText(true);

        VBox header = new VBox(6, titleLabel, subtitleLabel, translationRow, rangeSelectionsBox, statusLabel);
        header.setPadding(new Insets(16));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(createContentPanel());
        BorderPane.setMargin(root.getCenter(), new Insets(0, 16, 16, 16));

        HBox.setHgrow(translationBox, Priority.ALWAYS);

        restoreSavedSession(translationBox, addRangeButton, rangeSelectionsBox, rangeSelections, statusLabel, refreshCopyState);
        refreshCopyState.run();
        return root;
    }

    Alert createGeneralHelpAlert() {
        return createInformationAlert("Súgó", GENERAL_HELP_HEADER_TEXT, generalHelpContentText());
    }

    Alert createTranslationHelpAlert() {
        return createInformationAlert("Súgó", TRANSLATION_HELP_HEADER_TEXT, translationHelpContentText());
    }

    Alert createRangeHelpAlert() {
        return createInformationAlert("Súgó", RANGE_HELP_HEADER_TEXT, rangeHelpContentText());
    }

    String generalHelpContentText() {
        return "1. Válassz fordítást.\n"
                + "2. Add hozzá a szükséges szakaszsorokat a + gombbal.\n"
                + "3. Minden sorban válassz könyvet, fejezetet, majd kezdő és záró verset.\n"
                + "4. A kezdő és záró vers lehet azonos is.\n"
                + "5. A Másolás gomb az összes kész szakaszt egyetlen blokkba másolja.\n"
                + "6. A Mentés gombbal vagy az alkalmazás bezárásakor a munkamenet automatikusan elmentődik.";
    }

    String translationHelpContentText() {
        return "A fordítássorban választhatod ki a közös fordítást minden szakaszhoz.\n"
                + "- A + gomb új szakaszsort ad hozzá.\n"
                + "- A Mentés gomb kézzel elmenti az aktuális munkamenetet.\n"
                + "- A Másolás gomb az összes kész szakaszt a vágólapra másolja.\n"
                + "- Az Alaphelyzet gomb törli az aktuális kijelöléseket.\n"
                + "- A fordítás módosítását a program megerősítteti, ha már van kitöltött alsó szintű választás.";
    }

    String rangeHelpContentText() {
        return "Minden szakaszsorban ugyanahhoz a fordításhoz választhatod könyvet, fejezetet és vershatárokat.\n"
                + "- A legördülők első eleme a kiürített, alapértelmezett állapot.\n"
                + "- A kezdő és záró vers lehet ugyanaz is.\n"
                + "- A könyv módosítását a program megerősítteti, ha a sorban már van fejezet- vagy versválasztás.\n"
                + "- A - gomb eltávolítja az adott szakaszsort.";
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

    private Alert createInformationAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }

    private void configurePlaceholderDisplay(ComboBox<?> comboBox, String placeholderText) {
        comboBox.setPromptText(placeholderText);
        comboBox.setCellFactory(_ -> new PlaceholderListCell<>(placeholderText));
        PlaceholderListCell<Object> buttonCell = new PlaceholderListCell<>(placeholderText);
        comboBox.valueProperty().addListener((_, _, newValue) -> buttonCell.setText(
                newValue == null ? placeholderText : String.valueOf(newValue)
        ));
        @SuppressWarnings("unchecked")
        ComboBox<Object> typedComboBox = (ComboBox<Object>) comboBox;
        typedComboBox.setButtonCell(buttonCell);
    }

    private VBox createContentPanel() {
        Label instructionsLabel = new Label(
                "Ebben a nézetben nem jelenik meg verslista. A fenti vezérlőkkel állítsd össze a kívánt szakaszokat, majd kattints a Másolás gombra."
        );
        instructionsLabel.setWrapText(true);

        VBox contentPanel = new VBox(instructionsLabel);
        contentPanel.setPadding(new Insets(16));
        return contentPanel;
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
        ComboBox<Integer> fromVerseBox = new ComboBox<>();
        ComboBox<Integer> toVerseBox = new ComboBox<>();
        Button removeButton = new Button("-");
        Button helpButton = createHelpButton("A szakaszsor súgójának megnyitása");

        configurePlaceholderDisplay(bookBox, BOOK_PLACEHOLDER);
        configurePlaceholderDisplay(chapterBox, CHAPTER_PLACEHOLDER);
        configurePlaceholderDisplay(fromVerseBox, FROM_VERSE_PLACEHOLDER);
        configurePlaceholderDisplay(toVerseBox, TO_VERSE_PLACEHOLDER);
        removeButton.setTooltip(new Tooltip("A szakaszsor eltávolítása"));
        helpButton.setOnAction(_ -> createRangeHelpAlert().showAndWait());

        RangeSelectionControls rangeSelection = new RangeSelectionControls(
                new HBox(12, rangeLabel, bookBox, chapterBox, fromVerseBox, toVerseBox, removeButton, helpButton),
                rangeLabel,
                bookBox,
                chapterBox,
                fromVerseBox,
                toVerseBox,
                removeButton,
                helpButton
        );
        rangeSelection.container.setPadding(new Insets(4, 12, 4, 12));
        HBox.setHgrow(bookBox, Priority.ALWAYS);

        setComboBoxItems(bookBox, List.of());
        setComboBoxItems(chapterBox, List.of());
        setComboBoxItems(fromVerseBox, List.of());
        setComboBoxItems(toVerseBox, List.of());

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
        comboBox.setItems(FXCollections.observableArrayList(withDefaultOption(items)));
        comboBox.getSelectionModel().clearSelection();
        comboBox.setValue(null);
    }

    private void updateComboBoxItems(ComboBox<Integer> comboBox, List<Integer> items) {
        Integer currentValue = comboBox.getValue();
        comboBox.setItems(FXCollections.observableArrayList(withDefaultOption(items)));
        if (currentValue == null) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            return;
        }
        if (!items.contains(currentValue)) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
        }
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
        comboBox.setItems(FXCollections.observableArrayList(withDefaultOption(List.of())));
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
        for (String bookName : List.of(
                "1Mózes",
                "2Mózes",
                "3Mózes",
                "4Mózes",
                "5Mózes",
                "Józsué",
                "Bírák",
                "Ruth",
                "1Sámuel",
                "2Sámuel",
                "1Királyok",
                "2Királyok",
                "1Krónikák",
                "2Krónikák",
                "Ezsdrás",
                "Nehémiás",
                "Eszter",
                "Jób",
                "Zsoltárok",
                "Példabeszédek",
                "Prédikátor",
                "Énekek",
                "Ézsaiás",
                "Jeremiás",
                "Jeremiássiralmai",
                "Ezékiel",
                "Dániel",
                "Hóseás",
                "Jóel",
                "Ámósz",
                "Abdiás",
                "Jónás",
                "Mikeás",
                "Náhum",
                "Habakuk",
                "Zofóniás",
                "Haggeus",
                "Zakariás",
                "Malakiás",
                "Máté",
                "Márk",
                "Lukács",
                "János",
                "Cselekedetek",
                "Róma",
                "1Korinthus",
                "2Korinthus",
                "Galata",
                "Efezus",
                "Filippi",
                "Kolossé",
                "1Thesszalonika",
                "2Thesszalonika",
                "1Timóteus",
                "2Timóteus",
                "Titusz",
                "Filemon",
                "Zsidók",
                "Jakab",
                "1Péter",
                "2Péter",
                "1János",
                "2János",
                "3János",
                "Júdás",
                "Jelenések"
        )) {
            canonicalBookNamesByKey.put(normalizeBookNameKey(bookName), bookName.replace("Jeremiássiralmai", "Jeremiás siralmai"));
        }
        canonicalBookNamesByKey.put(normalizeBookNameKey("Jeremiás siralmai"), "Jeremiás siralmai");
        return canonicalBookNamesByKey;
    }

    private static String normalizeBookNameKey(String book) {
        return book.toLowerCase().replaceAll("[\\s.]", "");
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
        }
    }
}
