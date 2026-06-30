package hu.szegedibibliaszol.app.ui;

import hu.szegedibibliaszol.app.ApplicationVersion;
import hu.szegedibibliaszol.app.service.UiSessionService;
import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.testutil.FxTestSupport;
import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import hu.szegedibibliaszol.app.ui.model.RangeSelectionSnapshot;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainViewFactoryTest {

    @BeforeAll
    static void initializeJavaFx() {
        FxTestSupport.initialize();
    }

    @Test
    void createRootBuildsHungarianMultiRangeUiAndCopiesCanonicalBookNames() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül."),
                new VerseRow("Revideált Károli", "1. Mózes", 4, 5, "Kainra azonban nem tekintett."),
                new VerseRow("Revideált Károli", "Zsoltárok", 23, 1, "Az Úr az én pásztorom, nem szűkölködöm."),
                new VerseRow("Revideált Károli", "Zsoltárok", 23, 2, "Füves legelőkön nyugtat engem.")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Clipboard.getSystemClipboard().clear();
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox generalHelpRow = (HBox) mainContent.getChildren().get(2);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);
            HBox addRangeRow = (HBox) mainContent.getChildren().get(5);
            Label statusLabel = (Label) mainContent.getChildren().get(6);
            Label titleLabel = assertInstanceOf(Label.class, mainContent.getChildren().getFirst());
            Label subtitleLabel = assertInstanceOf(Label.class, mainContent.getChildren().get(1));
            ComboBox<String> translationBox = comboBox(translationRow, 0);
            Button tutorialButton = (Button) generalHelpRow.getChildren().getFirst();
            Button generalHelpButton = (Button) generalHelpRow.getChildren().get(1);
            Button addRangeButton = (Button) addRangeRow.getChildren().getFirst();
            Button saveButton = (Button) translationRow.getChildren().get(2);
            Button copyButton = (Button) translationRow.getChildren().get(3);
            Button resetButton = (Button) translationRow.getChildren().get(4);
            VBox contentPanel = (VBox) mainContent.getChildren().get(7);
            HBox footerRow = (HBox) mainContent.getChildren().get(8);
            Label instructionsLabel = assertInstanceOf(Label.class, contentPanel.getChildren().getFirst());
            Label versionLabel = assertInstanceOf(Label.class, footerRow.getChildren().getFirst());

            HBox firstRangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            Label firstRangeLabel = (Label) firstRangeRow.getChildren().getFirst();
            ComboBox<String> firstBookBox = comboBox(firstRangeRow, 0);
            ComboBox<Integer> firstChapterBox = comboBox(firstRangeRow, 1);
            Label firstChapterSeparatorLabel = (Label) firstRangeRow.getChildren().get(3);
            ComboBox<Integer> firstFromVerseBox = comboBox(firstRangeRow, 2);
            Label firstVerseRangeSeparatorLabel = (Label) firstRangeRow.getChildren().get(5);
            ComboBox<Integer> firstToVerseBox = comboBox(firstRangeRow, 3);
            Button firstResetButton = (Button) firstRangeRow.getChildren().get(7);
            Button firstRemoveButton = (Button) firstRangeRow.getChildren().get(8);
            Label firstRangeStatus = (Label) firstRangeRow.getChildren().get(9);
            Button firstRangeHelpButton = (Button) firstRangeRow.getChildren().get(10);

            assertEquals(MainViewFactory.INITIAL_STATUS_MESSAGE, statusLabel.getText());
            assertEquals("Fordítás", translationBox.getPromptText());
            assertEquals("Könyv", firstBookBox.getPromptText());
            assertEquals("Fejezet", firstChapterBox.getPromptText());
            assertEquals("Kezdő vers", firstFromVerseBox.getPromptText());
            assertEquals("Záró vers", firstToVerseBox.getPromptText());
            assertEquals(":", firstChapterSeparatorLabel.getText());
            assertEquals("-", firstVerseRangeSeparatorLabel.getText());
            assertEquals("Szakasz 1", firstRangeLabel.getText());
            assertEquals("Útmutató", tutorialButton.getText());
            assertEquals("?", generalHelpButton.getText());
            assertEquals("?", firstRangeHelpButton.getText());
            assertEquals("↺", firstResetButton.getText());
            assertEquals(MainViewFactory.APPLICATION_TITLE, titleLabel.getText());
            assertEquals(ApplicationVersion.current(), versionLabel.getText());
            assertEquals(2, translationBox.getItems().size());
            assertNull(translationBox.getItems().getFirst());
            assertEquals("Revideált Károli", translationBox.getItems().get(1));
            assertEquals(1, firstBookBox.getItems().size());
            assertNull(firstBookBox.getItems().getFirst());
            assertTrue(addRangeButton.isDisable());
            assertTrue(copyButton.isDisable());
            assertTrue(firstRemoveButton.isDisable());
            assertTrue(instructionsLabel.getText().contains("nem jelenik meg verslista"));
            assertTrue(addRangeButton.getTooltip().getText().contains("Numpad +"));
            assertEquals("Az aktuális munkamenet mentése (Ctrl+S)", saveButton.getTooltip().getText());
            assertEquals("Az összes kész szakasz másolása a vágólapra (Ctrl+C)", copyButton.getTooltip().getText());
            assertEquals("Az összes kijelölés alaphelyzetbe állítása (Ctrl+R)", resetButton.getTooltip().getText());
            assertTrue(tutorialButton.getTooltip().getText().contains("Gyors kezdési útmutató"));
            assertEquals(MainViewFactory.APPLICATION_TITLE, titleLabel.getTooltip().getText());
            assertEquals(ApplicationVersion.current(), versionLabel.getTooltip().getText());
            assertTrue(subtitleLabel.getTooltip().getText().contains("egy vagy több igerészletet"));
            assertEquals(MainViewFactory.INITIAL_STATUS_MESSAGE, statusLabel.getTooltip().getText());
            assertTrue(translationBox.getTooltip().getText().contains("Fordítás kiválasztása"));
            assertEquals(96, firstRangeLabel.getPrefWidth());
            assertTrue(titleLabel.getStyle().contains("white"));
            assertTrue(versionLabel.getStyle().contains("11px"));
            assertTrue(translationRow.getStyle().contains("rgba(255, 255, 255, 0.14)"));
            assertTrue(translationBox.getStyle().contains("#355C8A"));
            assertTrue(firstRangeRow.getStyle().contains("rgba(255, 255, 255, 0.14)"));
            assertTrue(contentPanel.getStyle().contains("#355C8A"));
            assertEquals(javafx.geometry.Pos.CENTER_RIGHT, footerRow.getAlignment());

            firstResetButton.fire();
            assertEquals("A(z) 1. szakasz alaphelyzetbe állítva. Válassz fordítást.", firstRangeStatus.getText());
            assertNull(firstBookBox.getValue());

            translationBox.setValue("Revideált Károli");
            assertEquals("A fordítás kiválasztva. Válassz könyvet a szakaszokhoz.", statusLabel.getText());
            assertEquals("A fordítás kiválasztva. Válassz könyvet a szakaszokhoz.", statusLabel.getTooltip().getText());
            assertEquals("Kiválasztott fordítás: Revideált Károli", translationBox.getTooltip().getText());
            assertFalse(addRangeButton.isDisable());
            assertEquals(3, firstBookBox.getItems().size());
            assertNull(firstBookBox.getItems().getFirst());
            assertEquals("1. Mózes", firstBookBox.getItems().get(1));
            assertEquals("Zsoltárok", firstBookBox.getItems().get(2));

            firstBookBox.setValue("1. Mózes");
            assertEquals("A(z) 1. szakaszban a könyv kiválasztva. Válassz fejezetet.", firstRangeStatus.getText());
            assertEquals("Kiválasztott könyv: 1. Mózes", firstBookBox.getTooltip().getText());
            assertEquals(2, firstChapterBox.getItems().size());
            assertNull(firstChapterBox.getItems().get(0));
            assertEquals(4, firstChapterBox.getItems().get(1));

            firstChapterBox.setValue(4);
            assertEquals("A(z) 1. szakaszhoz válaszd ki a kezdő és záró verset.", firstRangeStatus.getText());
            assertEquals("Kiválasztott fejezet: 4", firstChapterBox.getTooltip().getText());
            assertEquals(3, firstFromVerseBox.getItems().size());
            assertNull(firstFromVerseBox.getItems().get(0));
            assertEquals(4, firstFromVerseBox.getItems().get(1));
            assertEquals(5, firstFromVerseBox.getItems().get(2));
            assertEquals(3, firstToVerseBox.getItems().size());
            assertNull(firstToVerseBox.getItems().get(0));
            assertEquals(4, firstToVerseBox.getItems().get(1));
            assertEquals(5, firstToVerseBox.getItems().get(2));

            firstFromVerseBox.setValue(4);
            assertEquals("A(z) 1. szakaszhoz válassz záró verset, amely nem kisebb a kezdő versnél.", firstRangeStatus.getText());
            assertTrue(copyButton.isDisable());

            firstChapterBox.setValue(null);
            assertEquals("A(z) 1. szakaszhoz válassz fejezetet.", firstRangeStatus.getText());

            firstChapterBox.setValue(4);
            firstFromVerseBox.setValue(4);

            firstToVerseBox.setValue(4);
            assertEquals("A(z) 1. szakasz kész. Adj hozzá újabbat, vagy kattints a Másolás gombra.", firstRangeStatus.getText());
            assertFalse(copyButton.isDisable());

            root.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "+", "+", KeyCode.ADD, false, false, false, false));
            assertTrue(copyButton.isDisable());
            assertEquals(2, rangeSelectionsBox.getChildren().size());
            assertFalse(firstRemoveButton.isDisable());

            HBox secondRangeRow = (HBox) rangeSelectionsBox.getChildren().get(1);
            Label secondRangeLabel = (Label) secondRangeRow.getChildren().getFirst();
            ComboBox<String> secondBookBox = comboBox(secondRangeRow, 0);
            ComboBox<Integer> secondChapterBox = comboBox(secondRangeRow, 1);
            ComboBox<Integer> secondFromVerseBox = comboBox(secondRangeRow, 2);
            ComboBox<Integer> secondToVerseBox = comboBox(secondRangeRow, 3);
            Button secondResetButton = (Button) secondRangeRow.getChildren().get(7);
            Label secondRangeStatus = (Label) secondRangeRow.getChildren().get(9);

            assertEquals("Szakasz 2", secondRangeLabel.getText());
            assertEquals(3, secondBookBox.getItems().size());
            assertNull(secondBookBox.getItems().get(0));
            assertEquals("1. Mózes", secondBookBox.getItems().get(1));
            assertEquals("Zsoltárok", secondBookBox.getItems().get(2));
            assertFalse(secondRangeRow.getChildren().get(10).isVisible());

            secondBookBox.setValue("Zsoltárok");
            assertEquals("A(z) 2. szakaszban a könyv kiválasztva. Válassz fejezetet.", secondRangeStatus.getText());
            secondChapterBox.setValue(23);
            assertEquals("A(z) 2. szakaszhoz válaszd ki a kezdő és záró verset.", secondRangeStatus.getText());
            secondFromVerseBox.setValue(1);
            secondToVerseBox.setValue(2);
            secondToVerseBox.setValue(1);
            secondFromVerseBox.setValue(2);
            assertNull(secondFromVerseBox.getValue());
            assertNull(secondToVerseBox.getValue());
            assertEquals("A(z) 2. szakaszhoz válaszd ki a kezdő és záró verset.", secondRangeStatus.getText());
            secondToVerseBox.setValue(2);
            assertEquals("A(z) 2. szakaszhoz válassz kezdő verset, amely nem nagyobb a záró versnél.", secondRangeStatus.getText());
            secondFromVerseBox.setValue(2);
            assertEquals("A(z) 2. szakasz kész. Adj hozzá újabbat, vagy kattints a Másolás gombra.", secondRangeStatus.getText());
            secondFromVerseBox.setValue(1);
            assertEquals("A(z) 2. szakasz kész. Adj hozzá újabbat, vagy kattints a Másolás gombra.", secondRangeStatus.getText());
            assertEquals(2, secondToVerseBox.getValue());
            assertFalse(copyButton.isDisable());

            secondResetButton.fire();
            assertNull(secondBookBox.getValue());
            assertNull(secondChapterBox.getValue());
            assertNull(secondFromVerseBox.getValue());
            assertNull(secondToVerseBox.getValue());
            assertEquals("A(z) 2. szakasz alaphelyzetbe állítva. Válassz könyvet.", secondRangeStatus.getText());
            assertTrue(copyButton.isDisable());

            secondBookBox.setValue("Zsoltárok");
            secondChapterBox.setValue(23);
            secondFromVerseBox.setValue(1);
            secondToVerseBox.setValue(2);
            assertFalse(copyButton.isDisable());

            saveButton.fire();
            verify(uiSessionService).saveSession(new AppSessionSnapshot(
                    "Revideált Károli",
                    List.of(
                            new RangeSelectionSnapshot("1. Mózes", 4, 4, 4),
                            new RangeSelectionSnapshot("Zsoltárok", 23, 1, 2)
                    )
            ));
            assertEquals("A munkamenet elmentve.", statusLabel.getText());

            root.fireEvent(ctrlShortcutEvent(KeyCode.S));
            verify(uiSessionService, times(2)).saveSession(new AppSessionSnapshot(
                    "Revideált Károli",
                    List.of(
                            new RangeSelectionSnapshot("1. Mózes", 4, 4, 4),
                            new RangeSelectionSnapshot("Zsoltárok", 23, 1, 2)
                    )
            ));

            root.fireEvent(ctrlShortcutEvent(KeyCode.C));
            assertEquals("A kijelölt szakaszok a vágólapra kerültek.", statusLabel.getText());
            assertEquals(
                    "1Mózes 4:4" + System.lineSeparator() + System.lineSeparator()
                            + "4" + System.lineSeparator() + "Ábel is vitt elsőszülött juhai közül."
                            + System.lineSeparator() + System.lineSeparator()
                            + "Zsoltárok 23:1-2" + System.lineSeparator() + System.lineSeparator()
                            + "1" + System.lineSeparator() + "Az Úr az én pásztorom, nem szűkölködöm."
                            + System.lineSeparator() + System.lineSeparator()
                            + "2" + System.lineSeparator() + "Füves legelőkön nyugtat engem.",
                    Clipboard.getSystemClipboard().getString()
            );

            Button secondRemoveButton = (Button) secondRangeRow.getChildren().get(8);
            secondRemoveButton.fire();
            assertEquals(1, rangeSelectionsBox.getChildren().size());
            assertFalse(copyButton.isDisable());

            root.fireEvent(ctrlShortcutEvent(KeyCode.R));
            assertNull(translationBox.getValue());
            assertTrue(addRangeButton.isDisable());
            assertTrue(copyButton.isDisable());
            assertEquals(MainViewFactory.INITIAL_STATUS_MESSAGE, statusLabel.getText());
            assertEquals(1, rangeSelectionsBox.getChildren().size());

            saveButton.fire();
            return null;
        });
    }

    @Test
    void createRootLoadsSessionShowsHelpButtonsAndConfirmsTranslationAndBookChanges() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.of(new AppSessionSnapshot(
                "Revideált Károli",
                List.of(new RangeSelectionSnapshot("1. Mózes", 4, 4, 4))
        )));
        Alert generalHelpAlert = mock(Alert.class);
        Alert tutorialAlert = mock(Alert.class);
        Alert translationHelpAlert = mock(Alert.class);
        Alert rangeHelpAlert = mock(Alert.class);
        TestMainViewFactory mainViewFactory = new TestMainViewFactory(
                new VerseBrowserService(List.of(
                        new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül."),
                        new VerseRow("Revideált Károli", "1. Mózes", 4, 5, "Kainra azonban nem tekintett.")
                )),
                uiSessionService,
                generalHelpAlert,
                tutorialAlert,
                translationHelpAlert,
                rangeHelpAlert
        );
        mainViewFactory.enqueueConfirmation(false);
        mainViewFactory.enqueueConfirmation(true);
        mainViewFactory.enqueueConfirmation(false);
        mainViewFactory.enqueueConfirmation(true);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox generalHelpRow = (HBox) mainContent.getChildren().get(2);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);
            Label statusLabel = (Label) mainContent.getChildren().get(6);
            ComboBox<String> translationBox = comboBox(translationRow, 0);
            Button translationHelpButton = (Button) translationRow.getChildren().get(1);
            Button tutorialButton = (Button) generalHelpRow.getChildren().getFirst();
            Button generalHelpButton = (Button) generalHelpRow.getChildren().get(1);

            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);
            Button rangeHelpButton = (Button) rangeRow.getChildren().get(10);

            assertEquals("Revideált Károli", translationBox.getValue());
            assertEquals("1. Mózes", bookBox.getValue());
            assertEquals(4, chapterBox.getValue());
            assertEquals(4, fromVerseBox.getValue());
            assertEquals(4, toVerseBox.getValue());
            assertEquals("Az előző munkamenet automatikusan betöltődött.", statusLabel.getText());

            tutorialButton.fire();
            generalHelpButton.fire();
            translationHelpButton.fire();
            rangeHelpButton.fire();
            verify(tutorialAlert).showAndWait();
            verify(generalHelpAlert).showAndWait();
            verify(translationHelpAlert).showAndWait();
            verify(rangeHelpAlert).showAndWait();

            translationBox.setValue(null);
            assertEquals("Revideált Károli", translationBox.getValue());

            translationBox.setValue(null);
            assertNull(translationBox.getValue());
            assertNull(bookBox.getValue());
            assertNull(chapterBox.getValue());
            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());

            translationBox.setValue("Revideált Károli");
            bookBox.setValue("1. Mózes");
            chapterBox.setValue(4);
            fromVerseBox.setValue(4);
            toVerseBox.setValue(4);

            bookBox.setValue(null);
            assertEquals("1. Mózes", bookBox.getValue());

            bookBox.setValue(null);
            assertNull(bookBox.getValue());
            assertNull(chapterBox.getValue());
            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());
            return null;
        });
    }

    @Test
    void createRootShowsNoVerseMessageForEmptyChapter() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        VerseBrowserService verseBrowserService = mock(VerseBrowserService.class);
        when(verseBrowserService.getTranslations()).thenReturn(List.of("Revideált Károli"));
        when(verseBrowserService.getBooks("Revideált Károli")).thenReturn(List.of("Üres könyv"));
        when(verseBrowserService.getChapters("Revideált Károli", "Üres könyv")).thenReturn(List.of(1));
        when(verseBrowserService.getVerses("Revideált Károli", "Üres könyv", 1)).thenReturn(List.of());
        MainViewFactory mainViewFactory = new MainViewFactory(verseBrowserService, uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);
            Label rangeStatus = (Label) rangeRow.getChildren().get(9);

            translationBox.setValue("Revideált Károli");
            bookBox.setValue("Üres könyv");
            chapterBox.setValue(1);

            assertEquals("A(z) 1. szakasz kiválasztott fejezetében nincs vers.", rangeStatus.getText());
            assertEquals(1, fromVerseBox.getItems().size());
            assertNull(fromVerseBox.getItems().getFirst());
            assertEquals(1, toVerseBox.getItems().size());
            assertNull(toVerseBox.getItems().getFirst());
            return null;
        });
    }

    @Test
    void helpTextsAndFormatHelpersUseHungarianTexts() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of()));

        assertDoesNotThrow(mainViewFactory::saveCurrentSession);

        FxTestSupport.runOnFxThread(() -> {
            Alert generalHelpAlert = mainViewFactory.createGeneralHelpAlert();
            Alert tutorialHelpAlert = mainViewFactory.createTutorialAlert();
            Alert translationHelpAlert = mainViewFactory.createTranslationHelpAlert();
            Alert rangeHelpAlert = mainViewFactory.createRangeHelpAlert();

            assertEquals("Súgó", generalHelpAlert.getTitle());
            assertEquals(MainViewFactory.GENERAL_HELP_HEADER_TEXT, generalHelpAlert.getHeaderText());
            assertEquals(mainViewFactory.generalHelpContentText(), generalHelpAlert.getContentText());
            assertEquals("Útmutató", tutorialHelpAlert.getTitle());
            assertEquals(MainViewFactory.TUTORIAL_HEADER_TEXT, tutorialHelpAlert.getHeaderText());
            assertEquals(mainViewFactory.tutorialContentText(), tutorialHelpAlert.getContentText());
            assertEquals(MainViewFactory.TRANSLATION_HELP_HEADER_TEXT, translationHelpAlert.getHeaderText());
            assertEquals(MainViewFactory.RANGE_HELP_HEADER_TEXT, rangeHelpAlert.getHeaderText());

            generalHelpAlert.close();
            tutorialHelpAlert.close();
            translationHelpAlert.close();
            rangeHelpAlert.close();
            return null;
        });

        assertTrue(MainViewFactory.isRangeReady(4, 4));
        assertFalse(MainViewFactory.isRangeReady(nullInteger(), 4));
        assertFalse(MainViewFactory.isRangeReady(4, nullInteger()));
        assertTrue(mainViewFactory.generalHelpContentText().contains("numerikus billentyűzet + gombjával"));
        assertTrue(mainViewFactory.generalHelpContentText().contains("Ctrl+S"));
        assertTrue(mainViewFactory.generalHelpContentText().contains("Ctrl+C"));
        assertTrue(mainViewFactory.generalHelpContentText().contains("Ctrl+R"));
        assertTrue(mainViewFactory.generalHelpContentText().contains("Útmutató gomb"));
        assertTrue(mainViewFactory.generalHelpContentText().contains("buboréksúgóban"));
        assertTrue(mainViewFactory.tutorialContentText().contains("Gyors kezdés"));
        assertTrue(mainViewFactory.tutorialContentText().contains("Ctrl+S"));
        assertTrue(mainViewFactory.tutorialContentText().contains("Ctrl+C"));
        assertTrue(mainViewFactory.tutorialContentText().contains("Ctrl+R"));
        assertTrue(mainViewFactory.translationHelpContentText().contains("numerikus billentyűzet + gombja"));
        assertTrue(mainViewFactory.translationHelpContentText().contains("Ctrl+S"));
        assertTrue(mainViewFactory.translationHelpContentText().contains("Ctrl+C"));
        assertTrue(mainViewFactory.translationHelpContentText().contains("Ctrl+R"));
        assertTrue(mainViewFactory.rangeHelpContentText().contains("↺ gomb"));
        assertTrue(mainViewFactory.rangeHelpContentText().contains("buboréksúgóban"));
        assertEquals("A(z) 3. szakaszhoz válaszd ki a kezdő és záró verset.", MainViewFactory.rangeSelectionStatus(3, null, null));
        assertEquals("A(z) 3. szakaszhoz válassz kezdő verset, amely nem nagyobb a záró versnél.", MainViewFactory.rangeSelectionStatus(3, null, 5));
        assertEquals("A(z) 3. szakaszhoz válassz záró verset, amely nem kisebb a kezdő versnél.", MainViewFactory.rangeSelectionStatus(3, 5, null));
        assertEquals("A(z) 3. szakasz kész. Adj hozzá újabbat, vagy kattints a Másolás gombra.", MainViewFactory.rangeSelectionStatus(3, 5, 5));
        assertEquals(
                "Első" + System.lineSeparator() + System.lineSeparator() + "Második",
                mainViewFactory.formatVerseRangesText(List.of("Első", "Második"))
        );

        IllegalArgumentException emptyRangeException = assertThrows(
                IllegalArgumentException.class,
                () -> mainViewFactory.formatVerseRangeText(List.of(), 1, 1)
        );
        assertEquals("Legalább egy vers szükséges a szakasz formázásához.", emptyRangeException.getMessage());

        IllegalArgumentException emptyRangesException = assertThrows(
                IllegalArgumentException.class,
                () -> mainViewFactory.formatVerseRangesText(List.of())
        );
        assertEquals("Legalább egy formázott szakasz szükséges.", emptyRangesException.getMessage());
    }

    @Test
    void showConfirmationDialogHandlesIgenAndNemButtons() {
        TestMainViewFactory mainViewFactory = new TestMainViewFactory(
                new VerseBrowserService(List.of()),
                mock(UiSessionService.class),
                mock(Alert.class),
                mock(Alert.class),
                mock(Alert.class),
                mock(Alert.class)
        );
        mainViewFactory.enqueueConfirmation(false);
        mainViewFactory.enqueueConfirmation(true);

        FxTestSupport.runOnFxThread(() -> {
            assertFalse(mainViewFactory.showConfirmationDialog("Cím", "Fejléc", "Tartalom"));

            assertTrue(mainViewFactory.showConfirmationDialog("Cím", "Fejléc", "Tartalom"));
            return null;
        });
    }

    @Test
    void showConfirmationDialogBuildsHungarianButtonsAndUsesWaitResult() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of())) {
            @Override
            ButtonType showConfirmationDialogAndWait(Alert alert, ButtonType fallbackButton) {
                assertEquals("Cím", alert.getTitle());
                assertEquals("Fejléc", alert.getHeaderText());
                assertEquals("Tartalom", alert.getContentText());
                assertEquals("Igen", alert.getButtonTypes().get(0).getText());
                assertEquals("Nem", alert.getButtonTypes().get(1).getText());
                return fallbackButton;
            }
        };

        FxTestSupport.runOnFxThread(() -> {
            assertFalse(mainViewFactory.showConfirmationDialog("Cím", "Fejléc", "Tartalom"));
            return null;
        });
    }

    @Test
    void showConfirmationDialogAndWaitUsesAlertResultOrFallback() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of()));
        Alert confirmedAlert = mock(Alert.class);
        ButtonType yesButton = new ButtonType("Igen", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Nem", ButtonBar.ButtonData.NO);
        when(confirmedAlert.showAndWait()).thenReturn(Optional.of(yesButton));

        Alert cancelledAlert = mock(Alert.class);
        when(cancelledAlert.showAndWait()).thenReturn(Optional.empty());

        FxTestSupport.runOnFxThread(() -> {
            assertEquals(yesButton, mainViewFactory.showConfirmationDialogAndWait(confirmedAlert, noButton));
            assertEquals(noButton, mainViewFactory.showConfirmationDialogAndWait(cancelledAlert, noButton));
            return null;
        });
    }

    @Test
    void showConfirmationDialogAndWaitReturnsFallbackWhenAlertReturnsEmpty() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of()));
        Alert alert = mock(Alert.class);
        ButtonType fallbackButton = new ButtonType("Nem", ButtonBar.ButtonData.NO);
        when(alert.showAndWait()).thenReturn(Optional.empty());

        assertEquals(fallbackButton, mainViewFactory.showConfirmationDialogAndWait(alert, fallbackButton));
    }

    @Test
    void createRootClearsInvalidVerseSelectionsWhenChapterChanges() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül."),
                new VerseRow("Revideált Károli", "1. Mózes", 4, 5, "Kainra azonban nem tekintett."),
                new VerseRow("Revideált Károli", "1. Mózes", 5, 1, "Ez Ádám nemzetségének a könyve."),
                new VerseRow("Revideált Károli", "1. Mózes", 5, 2, "Férfivá és nővé teremtette őket.")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            translationBox.setValue("Revideált Károli");
            bookBox.setValue("1. Mózes");
            chapterBox.setValue(4);
            fromVerseBox.setValue(4);
            toVerseBox.setValue(5);

            chapterBox.setValue(5);

            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());
            return null;
        });
    }

    @Test
    void createRootRestoresSavedTranslationWithEmptyRangeSelections() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.of(new AppSessionSnapshot(
                "Revideált Károli",
                List.of(new RangeSelectionSnapshot(null, null, null, null))
        )));
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül.")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            assertEquals("Revideált Károli", translationBox.getValue());
            assertNull(bookBox.getValue());
            assertNull(chapterBox.getValue());
            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());
            return null;
        });
    }

    @Test
    void createRootIgnoresUnavailableSavedTranslationAndKeepsDefaultState() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.of(new AppSessionSnapshot(
                "Nem elérhető fordítás",
                List.of(new RangeSelectionSnapshot("1. Mózes", 4, 4, 4))
        )));
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül.")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);
            Label statusLabel = (Label) mainContent.getChildren().get(6);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            assertNull(translationBox.getValue());
            assertEquals(1, rangeSelectionsBox.getChildren().size());
            assertNull(bookBox.getValue());
            assertNull(chapterBox.getValue());
            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());
            assertEquals("A mentett fordítás már nem érhető el, ezért a munkamenet alaphelyzetből indult.", statusLabel.getText());
            return null;
        });
    }

    @Test
    void createRootClearsUnavailableSavedChapterAndVerseSelections() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.of(new AppSessionSnapshot(
                "Revideált Károli",
                List.of(new RangeSelectionSnapshot("1. Mózes", 99, 4, 8))
        )));
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül."),
                new VerseRow("Revideált Károli", "1. Mózes", 4, 5, "Kainra azonban nem tekintett.")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            assertEquals("Revideált Károli", translationBox.getValue());
            assertEquals("1. Mózes", bookBox.getValue());
            assertNull(chapterBox.getValue());
            assertNull(fromVerseBox.getValue());
            assertNull(toVerseBox.getValue());
            assertEquals(List.of(4), nonNullItems(chapterBox));
            assertEquals(List.of(), nonNullItems(fromVerseBox));
            assertEquals(List.of(), nonNullItems(toVerseBox));
            return null;
        });
    }

    @Test
    void placeholderListCellShowsPlaceholderAndSelectedItemText() {
        FxTestSupport.runOnFxThread(() -> {
            MainViewFactory.PlaceholderListCell<Integer> placeholderListCell = new MainViewFactory.PlaceholderListCell<>("Vers");

            placeholderListCell.updateItem(null, false);
            assertEquals("Vers", placeholderListCell.getText());

            placeholderListCell.updateItem(5, false);
            assertEquals("5", placeholderListCell.getText());

            placeholderListCell.updateItem(null, true);
            assertEquals("Vers", placeholderListCell.getText());
            return null;
        });
    }

    @Test
    void createRootUsesScrollableLayoutWithTextDropdownsAndNumericQuickSelection() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 1, "Kezdet"),
                new VerseRow("Revideált Károli", "1. Mózes", 5, 1, "Ötödik fejezet első verse"),
                new VerseRow("Revideált Károli", "1. Mózes", 5, 10, "Ötödik fejezet tizedik verse"),
                new VerseRow("Revideált Károli", "Zsoltárok", 23, 1, "Az Úr az én pásztorom")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            ScrollPane pageScrollPane = assertInstanceOf(ScrollPane.class, root.getCenter());
            VBox mainContent = assertInstanceOf(VBox.class, pageScrollPane.getContent());
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            assertFalse(translationBox.isEditable());
            assertFalse(bookBox.isEditable());
            assertTrue(chapterBox.isEditable());
            assertTrue(fromVerseBox.isEditable());
            assertTrue(toVerseBox.isEditable());

            translationBox.setValue("Revideált Károli");
            bookBox.setValue("1. Mózes");
            chapterBox.getEditor().setText("5");
            assertEquals(5, chapterBox.getValue());
            assertEquals(List.of(4, 5), nonNullItems(chapterBox));

            fromVerseBox.getEditor().setText("10");
            assertEquals(10, fromVerseBox.getValue());
            assertEquals(List.of(1, 10), nonNullItems(fromVerseBox));

            toVerseBox.getEditor().setText("2");
            assertNull(toVerseBox.getValue());
            assertEquals("", toVerseBox.getEditor().getText());

            toVerseBox.getEditor().setText("10");
            assertEquals(10, toVerseBox.getValue());
            assertEquals(List.of(10), nonNullItems(toVerseBox));
            return null;
        });
    }

    @Test
    void privateNumericQuickSelectionHelpersHandleValidInvalidAndBlankInput() throws Exception {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of()));

        FxTestSupport.runOnFxThread(() -> {
            ComboBox<Integer> comboBox = new ComboBox<>();
            invokePrivate(mainViewFactory, "configureNumericQuickSelection", new Class<?>[]{ComboBox.class, String.class}, comboBox, "Fejezet");
            invokePrivate(mainViewFactory, "replaceComboBoxItems", new Class<?>[]{ComboBox.class, List.class}, comboBox, List.of(4, 14, 24));
            assertEquals("14", comboBox.getConverter().toString(14));
            assertEquals(24, comboBox.getConverter().fromString("24"));
            assertNull(comboBox.getConverter().fromString("nem szám"));

            comboBox.getEditor().setText("4");
            assertEquals(4, comboBox.getValue());
            assertEquals(List.of(4, 14, 24), nonNullItems(comboBox));

            triggerEditorAction(comboBox);
            assertEquals(4, comboBox.getValue());

            comboBox.getEditor().setText("nem szám");
            assertEquals("4", comboBox.getEditor().getText());

            comboBox.getEditor().setText("2");
            assertEquals("2", comboBox.getEditor().getText());
            assertNull(comboBox.getValue());

            comboBox.getEditor().setText("99");
            triggerEditorAction(comboBox);
            assertNull(comboBox.getValue());
            assertEquals("", comboBox.getEditor().getText());

            comboBox.setValue(14);
            invokePrivate(mainViewFactory, "applyNumericQuickSelection", new Class<?>[]{ComboBox.class, String.class}, comboBox, "");
            assertNull(comboBox.getValue());

            comboBox.getEditor().setText("");
            triggerEditorAction(comboBox);
            assertNull(comboBox.getValue());
            assertNull(comboBox.getItems().getFirst());
            return null;
        });

        assertNull(invokePrivate(mainViewFactory, "parseInteger", new Class<?>[]{String.class}, (Object) null));
        assertNull(invokePrivate(mainViewFactory, "parseInteger", new Class<?>[]{String.class}, "abc"));
        assertEquals(14, invokePrivate(mainViewFactory, "parseInteger", new Class<?>[]{String.class}, " 14 "));
    }

    @Test
    void numericQuickSelectionCommitsTypedValueThroughRealFocusChangesAndCoversNullTranslationPaths() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of()));

        FxTestSupport.runOnFxThread(() -> {
            ComboBox<Integer> comboBox = new ComboBox<>();
            invokePrivate(mainViewFactory, "configureNumericQuickSelection", new Class<?>[]{ComboBox.class, String.class}, comboBox, "Fejezet");
            invokePrivate(mainViewFactory, "replaceComboBoxItems", new Class<?>[]{ComboBox.class, List.class}, comboBox, List.of(4, 14));

            Stage stage = new Stage();
            Button focusTarget = new Button("Más mező");
            try {
                stage.setScene(new Scene(new VBox(comboBox, focusTarget), 320, 120));
                stage.show();
                FxTestSupport.waitForFxEvents();
                comboBox.show();

                invokePrivate(mainViewFactory, "applyNumericQuickSelection", new Class<?>[]{ComboBox.class, String.class}, comboBox, "14");
                assertTrue(comboBox.isShowing());

                comboBox.getEditor().requestFocus();
                FxTestSupport.waitForFxEvents();

                comboBox.getEditor().requestFocus();
                comboBox.getEditor().setText("4");
                focusTarget.requestFocus();
                FxTestSupport.waitForFxEvents();
                assertEquals(4, comboBox.getValue());
            } finally {
                stage.close();
            }

            Object rangeSelection = invokePrivate(mainViewFactory, "createRangeSelectionControls", new Class<?>[]{Runnable.class}, (Runnable) () -> {
            });
            Class<?> rangeSelectionClass = rangeSelection.getClass();

            invokePrivate(
                    mainViewFactory,
                    "handleBookSelectionChange",
                    new Class<?>[]{rangeSelectionClass, String.class, String.class, Runnable.class},
                    rangeSelection,
                    null,
                    "1. Mózes",
                    (Runnable) () -> {
                    }
            );
            invokePrivate(
                    mainViewFactory,
                    "handleChapterSelectionChange",
                    new Class<?>[]{rangeSelectionClass, Integer.class, Runnable.class},
                    rangeSelection,
                    4,
                    (Runnable) () -> {
                    }
            );
            return null;
        });
    }

    @Test
    void createRootRejectsTypedImpossibleToVerseAndKeepsCopyDisabled() {
        UiSessionService uiSessionService = mock(UiSessionService.class);
        when(uiSessionService.loadSession()).thenReturn(Optional.empty());
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 1, "Első vers"),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "Második vers"),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 4, "Negyedik vers")
        )), uiSessionService);

        FxTestSupport.runOnFxThread(() -> {
            Parent parent = mainViewFactory.createRoot();
            BorderPane root = (BorderPane) parent;
            VBox mainContent = mainContent(root);
            HBox translationRow = (HBox) mainContent.getChildren().get(3);
            VBox rangeSelectionsBox = (VBox) mainContent.getChildren().get(4);

            ComboBox<String> translationBox = comboBox(translationRow, 0);
            Button copyButton = (Button) translationRow.getChildren().get(3);
            HBox rangeRow = (HBox) rangeSelectionsBox.getChildren().getFirst();
            ComboBox<String> bookBox = comboBox(rangeRow, 0);
            ComboBox<Integer> chapterBox = comboBox(rangeRow, 1);
            ComboBox<Integer> fromVerseBox = comboBox(rangeRow, 2);
            ComboBox<Integer> toVerseBox = comboBox(rangeRow, 3);

            translationBox.setValue("Revideált Károli");
            bookBox.setValue("1. Mózes");
            chapterBox.setValue(1);
            fromVerseBox.setValue(4);

            assertTrue(copyButton.isDisable());

            toVerseBox.getEditor().setText("2");

            assertEquals("", toVerseBox.getEditor().getText());
            assertNull(toVerseBox.getValue());
            assertTrue(copyButton.isDisable());
            return null;
        });
    }

    @Test
    void createRootCanBeShownInStage() {
        MainViewFactory mainViewFactory = new MainViewFactory(new VerseBrowserService(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 4, 4, "Ábel is vitt elsőszülött juhai közül.")
        )));

        FxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            try {
                assertDoesNotThrow(() -> {
                    stage.setScene(new Scene(mainViewFactory.createRoot(), 1180, 760));
                    stage.show();
                });
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> comboBox(HBox row, int index) {
        return (ComboBox<T>) row.getChildren().stream()
                .filter(ComboBox.class::isInstance)
                .skip(index)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing ComboBox at logical index " + index));
    }

    private VBox mainContent(BorderPane root) {
        ScrollPane pageScrollPane = assertInstanceOf(ScrollPane.class, root.getCenter());
        return assertInstanceOf(VBox.class, pageScrollPane.getContent());
    }

    private <T> List<T> nonNullItems(ComboBox<T> comboBox) {
        return comboBox.getItems().stream().filter(Objects::nonNull).toList();
    }

    private void triggerEditorAction(ComboBox<?> comboBox) {
        comboBox.getEditor().getOnAction().handle(new ActionEvent());
    }

    private KeyEvent ctrlShortcutEvent(KeyCode keyCode) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, true, false, false);
    }

    private Integer nullInteger() {
        return null;
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }


    private static final class TestMainViewFactory extends MainViewFactory {

        private final Alert generalHelpAlert;
        private final Alert tutorialHelpAlert;
        private final Alert translationHelpAlert;
        private final Alert rangeHelpAlert;
        private final ArrayDeque<Boolean> confirmationResponses = new ArrayDeque<>();

        private TestMainViewFactory(
                VerseBrowserService verseBrowserService,
                UiSessionService uiSessionService,
                Alert generalHelpAlert,
                Alert tutorialHelpAlert,
                Alert translationHelpAlert,
                Alert rangeHelpAlert
        ) {
            super(verseBrowserService, uiSessionService);
            this.generalHelpAlert = generalHelpAlert;
            this.tutorialHelpAlert = tutorialHelpAlert;
            this.translationHelpAlert = translationHelpAlert;
            this.rangeHelpAlert = rangeHelpAlert;
        }

        private void enqueueConfirmation(boolean response) {
            confirmationResponses.add(response);
        }

        @Override
        Alert createGeneralHelpAlert() {
            return generalHelpAlert;
        }

        @Override
        Alert createTutorialAlert() {
            return tutorialHelpAlert;
        }

        @Override
        Alert createTranslationHelpAlert() {
            return translationHelpAlert;
        }

        @Override
        Alert createRangeHelpAlert() {
            return rangeHelpAlert;
        }

        @Override
        ButtonType showConfirmationDialogAndWait(Alert alert, ButtonType fallbackButton) {
            if (!confirmationResponses.removeFirst()) {
                return fallbackButton;
            }
            return alert.getButtonTypes().stream()
                    .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.YES)
                    .findFirst()
                    .orElse(fallbackButton);
        }
    }
}
