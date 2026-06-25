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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import moodleviewer.commands.CommandManager;

/**
 * Gestiona la disposición visual (Layout) de los componentes principales de la interfaz.
 *
 * Estructura general:
 * - Toolbar superior: apertura/deshacer-rehacer a la izquierda, nombre de archivo centrado,
 *   exportación + selector de idioma a la derecha.
 * - SplitPane horizontal principal: panel de categorías (buscador + árbol) a la izquierda,
 *   panel de preguntas a la derecha.
 * - El panel de preguntas contiene a su vez un SplitPane vertical: barra de búsqueda/filtro
 *   de preguntas + tabla arriba, vista de detalle (WebView) abajo.
 * - Barra de estado inferior: estadísticas y duplicados a la izquierda.
 */
public class LayoutManager {

    public static Scene buildScene(Main main) {
        BorderPane root = new BorderPane();

        root.setTop(buildTopToolbar(main));
        root.setCenter(buildMainSplitPane(main));
        root.setBottom(buildStatusBar(main));

        Scene scene = new Scene(root, 1200, 800);
        registerAccelerators(scene, main);
        return scene;
    }

    // =====================================================================
    //  TOOLBAR SUPERIOR: apertura/undo-redo (izq) | nombre archivo (centro) | exportar+idioma (dcha)
    // =====================================================================

    private static HBox buildTopToolbar(Main main) {
        HBox toolBar = new HBox(10);
        toolBar.getStyleClass().add("tool-bar");
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(8, 15, 8, 15));

        // --- IZQUIERDA: apertura de archivo + deshacer/rehacer ---
        HBox leftGroup = new HBox(10);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        leftGroup.getChildren().addAll(
            main.getOpenBankButton(),
            new Separator(Orientation.VERTICAL),
            main.getUndoButton(),
            main.getRedoButton()
        );

        // --- CENTRO: nombre del archivo actual, con crecimiento simétrico para quedar centrado ---
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox centerGroup = new HBox(main.getFileNameLabel());
        centerGroup.setAlignment(Pos.CENTER);

        // --- DERECHA: exportación + separador + banderas de idioma ---
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

    // =====================================================================
    //  SPLITPANE PRINCIPAL: categorías (izq) | preguntas (dcha, con su propio split vertical)
    // =====================================================================

    private static SplitPane buildMainSplitPane(Main main) {
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getStyleClass().add("split-pane");

        VBox categoriesPanel = buildCategoriesPanel(main);
        VBox questionsPanel = buildQuestionsPanel(main);

        horizontalSplit.getItems().addAll(categoriesPanel, questionsPanel);
        horizontalSplit.setDividerPositions(0.25);
        return horizontalSplit;
    }

    /**
     * Panel izquierdo: barra propia con buscador de categorías + botón de añadir categoría,
     * y el árbol de categorías ocupando el resto del espacio disponible.
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

        VBox panel = new VBox(categoriesBar, main.getCategoryTreeView());
        VBox.setVgrow(main.getCategoryTreeView(), Priority.ALWAYS);
        return panel;
    }

    /**
     * Panel derecho: barra propia con criterio de búsqueda + buscador + filtro de tipo + botón
     * de añadir pregunta, y debajo el SplitPane vertical (tabla de preguntas / vista de detalle).
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

        VBox tableContainer = new VBox(main.getQuestionTableView());
        VBox.setVgrow(main.getQuestionTableView(), Priority.ALWAYS);

        // El toggle de vista previa Cloze flota sobre la esquina superior derecha del WebView.
        // Un StackPane es el contenedor correcto para superponer controles en JavaFX (a diferencia
        // de mezclar layout administrado con layoutX/layoutY manual, que no es fiable dentro de
        // contenedores que recalculan posiciones, como VBox).
        StackPane webOverlay = new StackPane();
        webOverlay.getChildren().add(main.getDetailsWebView());
        StackPane.setAlignment(main.getClozeToggle(), Pos.TOP_RIGHT);
        StackPane.setMargin(main.getClozeToggle(), new Insets(15));
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

    // =====================================================================
    //  BARRA DE ESTADO INFERIOR: estadísticas + duplicados, a la izquierda
    // =====================================================================

    private static HBox buildStatusBar(Main main) {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        statusBar.getChildren().addAll(
            main.getStatsButton(),
            main.getDuplicatesButton()
        );
        return statusBar;
    }

    // =====================================================================
    //  ATAJOS DE TECLADO
    // =====================================================================

    private static void registerAccelerators(Scene scene, Main main) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getSaveButton().isDisabled()) main.getSaveButton().fire();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getOpenBankButton().isDisabled()) main.getOpenBankButton().fire();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getAddQuestionButton().isDisabled()) main.getAddQuestionButton().fire();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), () -> {
            if (!main.getAddCategoryButton().isDisabled()) main.getAddCategoryButton().fire();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
            main.getSearchQuestionField().requestFocus();
            main.getSearchQuestionField().selectAll();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), () -> {
            CommandManager.getInstance().undo();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), () -> {
            CommandManager.getInstance().redo();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), () -> {
            CommandManager.getInstance().redo();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getDuplicatesButton().isDisabled()) main.getDuplicatesButton().fire();
        });
    }
}