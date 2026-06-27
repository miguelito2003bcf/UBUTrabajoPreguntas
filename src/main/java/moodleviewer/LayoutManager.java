/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import moodleviewer.commands.CommandManager;

/**
 * Gestiona la disposición visual de los componentes principales.
 *
 * Cambios:
 * - PUNTO C: botón "Importar GIFT desde portapapeles" añadido en analysisBar.
 * - PUNTO D: counterLabel integrada bajo la tabla de preguntas.
 * - PUNTO 6: analysisBar usa clase CSS "tool-bar" en vez de estilo inline gris.
 */
public class LayoutManager {

    public static Scene buildScene(Main main) {
        BorderPane root = new BorderPane();
        root.setTop(buildTopToolbar(main));
        root.setCenter(buildMainSplitPane(main));
        Scene scene = new Scene(root, 1200, 800);
        registerAccelerators(scene, main);
        return scene;
    }

    // =========================================================================
    //  TOOLBAR SUPERIOR
    // =========================================================================

    private static HBox buildTopToolbar(Main main) {
        HBox toolBar = new HBox(10);
        toolBar.getStyleClass().add("tool-bar");
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(8, 15, 8, 15));

        HBox leftGroup = new HBox(10);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        leftGroup.getChildren().addAll(
                main.getOpenBankButton(),
                new Separator(Orientation.VERTICAL),
                main.getUndoButton(),
                main.getRedoButton()
        );

        Region leftSpacer  = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer,  Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox centerGroup = new HBox(main.getFileNameLabel());
        centerGroup.setAlignment(Pos.CENTER);

        HBox rightGroup = new HBox(10);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        rightGroup.getChildren().addAll(
                main.getSaveButton(),
                main.getExportLatexButton(),
                main.getExportGiftButton(),
                new Separator(Orientation.VERTICAL),
                main.getLanguageBox()
        );

        toolBar.getChildren().addAll(leftGroup, leftSpacer, centerGroup, rightSpacer, rightGroup);
        return toolBar;
    }

    // =========================================================================
    //  SPLITPANE PRINCIPAL
    // =========================================================================

    private static SplitPane buildMainSplitPane(Main main) {
        SplitPane split = new SplitPane();
        split.getStyleClass().add("split-pane");
        split.getItems().addAll(buildCategoriesPanel(main), buildQuestionsPanel(main));
        split.setDividerPositions(0.25);
        return split;
    }

    /**
     * Panel izquierdo: buscador + añadir categoría | árbol | análisis + importar portapapeles.
     *
     * PUNTO 6: analysisBar usa clase CSS "tool-bar".
     * PUNTO C: botón importClipboard añadido en analysisBar.
     */
    private static VBox buildCategoriesPanel(Main main) {
        HBox categoriesBar = new HBox(10);
        categoriesBar.setPadding(new Insets(8, 10, 8, 10));
        categoriesBar.setAlignment(Pos.CENTER_LEFT);
        categoriesBar.getStyleClass().add("tool-bar");
        HBox.setHgrow(main.getSearchCategoryField(), Priority.ALWAYS);
        categoriesBar.getChildren().addAll(
                main.getSearchCategoryField(),
                main.getAddCategoryButton()
        );

        // PUNTO 6: misma clase "tool-bar" que categoriesBar → coherencia visual
        // PUNTO C: importClipboardButton integrado aquí junto a estadísticas y duplicados
        HBox analysisBar = new HBox(10);
        analysisBar.setPadding(new Insets(8, 10, 8, 10));
        analysisBar.setAlignment(Pos.CENTER_LEFT);
        analysisBar.getStyleClass().add("tool-bar");
        analysisBar.getChildren().addAll(
                main.getStatsButton(),
                main.getDuplicatesButton(),
                new Separator(Orientation.VERTICAL),
                main.getImportClipboardButton()
        );

        VBox panel = new VBox(categoriesBar, main.getCategoryTreeView(), analysisBar);
        VBox.setVgrow(main.getCategoryTreeView(), Priority.ALWAYS);
        return panel;
    }

    /**
     * Panel derecho: barra de búsqueda | SplitPane vertical (tabla + contador | detalle).
     *
     * PUNTO D: counterLabel aparece inmediatamente bajo la tabla, dentro de tableContainer.
     */
    private static VBox buildQuestionsPanel(Main main) {
        HBox questionsBar = new HBox(10);
        questionsBar.setPadding(new Insets(8, 10, 8, 10));
        questionsBar.setAlignment(Pos.CENTER_LEFT);
        questionsBar.getStyleClass().add("tool-bar");
        HBox.setHgrow(main.getSearchQuestionField(), Priority.ALWAYS);
        questionsBar.getChildren().addAll(
                main.getSearchCriteriaCombo(),
                main.getSearchQuestionField(),
                main.getTypeFilterMenu(),
                main.getAddQuestionButton()
        );

        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getStyleClass().add("split-pane");

        // PUNTO D: tabla + etiqueta de contador en el mismo contenedor
        VBox tableContainer = new VBox(main.getQuestionTableView(), main.getCounterLabel());
        VBox.setVgrow(main.getQuestionTableView(), Priority.ALWAYS);

        // Toggle Cloze flotando sobre el WebView
        javafx.scene.layout.StackPane webOverlay = new javafx.scene.layout.StackPane();
        webOverlay.getChildren().add(main.getDetailsWebView());
        javafx.scene.layout.StackPane.setAlignment(main.getClozeToggle(), Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(main.getClozeToggle(), new Insets(15));
        webOverlay.getChildren().add(main.getClozeToggle());

        VBox webContainer = new VBox(webOverlay);
        VBox.setVgrow(webOverlay, Priority.ALWAYS);
        VBox.setVgrow(main.getDetailsWebView(), Priority.ALWAYS);

        verticalSplit.getItems().addAll(tableContainer, webContainer);
        verticalSplit.setDividerPositions(0.5);

        VBox panel = new VBox(questionsBar, verticalSplit);
        VBox.setVgrow(verticalSplit, Priority.ALWAYS);
        return panel;
    }

    // =========================================================================
    //  ACELERADORES DE TECLADO
    // =========================================================================

    private static void registerAccelerators(Scene scene, Main main) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                () -> { if (!main.getSaveButton().isDisabled()) main.getSaveButton().fire(); });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                () -> { if (!main.getOpenBankButton().isDisabled()) main.getOpenBankButton().fire(); });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> { if (!main.getAddQuestionButton().isDisabled()) main.getAddQuestionButton().fire(); });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> { if (!main.getAddCategoryButton().isDisabled()) main.getAddCategoryButton().fire(); });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                () -> { main.getSearchQuestionField().requestFocus();
                        main.getSearchQuestionField().selectAll(); });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                () -> CommandManager.getInstance().undo());
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN),
                () -> CommandManager.getInstance().redo());
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> CommandManager.getInstance().redo());
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN),
                () -> { if (!main.getDuplicatesButton().isDisabled()) main.getDuplicatesButton().fire(); });
        // PUNTO C: Ctrl+V (cuando el foco no está en un campo de texto) importa desde portapapeles
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> { if (!main.getImportClipboardButton().isDisabled())
                            main.getImportClipboardButton().fire(); });
    }
}