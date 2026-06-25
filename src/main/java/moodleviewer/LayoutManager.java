/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer;

import javafx.geometry.Insets;
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
import javafx.scene.layout.VBox;
import moodleviewer.commands.CommandManager;

/**
 * Gestiona la disposición visual (Layout) de los componentes principales de la interfaz.
 */
public class LayoutManager {

    public static Scene buildScene(Main main) {
        BorderPane root = new BorderPane();

        // --- BARRA SUPERIOR (Herramientas y Archivo) ---
        HBox toolBar = new HBox(10);
        toolBar.getStyleClass().add("tool-bar");
        toolBar.setAlignment(Pos.CENTER_LEFT);
        
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // AQUÍ ESTÁ EL ORDEN DE LOS BOTONES SOLICITADO
        toolBar.getChildren().addAll(
            main.getOpenButton(),
            main.getOpenGiftButton(),        // <--- NUEVO: Abrir GIFT al lado de Abrir XML
            new Separator(javafx.geometry.Orientation.VERTICAL),
            main.getSaveButton(),
            main.getExportLatexButton(),
            main.getExportGiftButton(),      // <--- NUEVO: Exportar GIFT junto a XML y LaTeX
            new Separator(javafx.geometry.Orientation.VERTICAL),
            main.getAddCategoryButton(),
            main.getAddQuestionButton(),
            new Separator(javafx.geometry.Orientation.VERTICAL),
            main.getUndoButton(),
            main.getRedoButton(),
            new Separator(javafx.geometry.Orientation.VERTICAL),
            main.getStatsButton(),
            leftSpacer,
            main.getLanguageBox()
        );

        // --- BARRA DE BÚSQUEDA ---
        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(5, 15, 5, 15));
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getChildren().addAll(
            main.getSearchCategoryField(),
            new Separator(javafx.geometry.Orientation.VERTICAL),
            main.getSearchQuestionField(),
            main.getSearchCriteriaCombo(),
            main.getTypeFilterMenu(),
            main.getClozeToggle()
        );

        VBox topContainer = new VBox(toolBar, searchBar);
        root.setTop(topContainer);

        // --- ZONA CENTRAL (SplitPane) ---
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getStyleClass().add("split-pane");
        
        VBox leftPanel = new VBox(main.getCategoryTreeView());
        VBox.setVgrow(main.getCategoryTreeView(), Priority.ALWAYS);
        
        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        verticalSplit.getStyleClass().add("split-pane");
        
        VBox tableContainer = new VBox(main.getQuestionTableView());
        VBox.setVgrow(main.getQuestionTableView(), Priority.ALWAYS);
        
        VBox webContainer = new VBox(main.getDetailsWebView());
        VBox.setVgrow(main.getDetailsWebView(), Priority.ALWAYS);
        
        verticalSplit.getItems().addAll(tableContainer, webContainer);
        verticalSplit.setDividerPositions(0.5);

        horizontalSplit.getItems().addAll(leftPanel, verticalSplit);
        horizontalSplit.setDividerPositions(0.25);
        
        root.setCenter(horizontalSplit);

        // --- BARRA DE ESTADO INFERIOR ---
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        statusBar.getChildren().add(main.getFileNameLabel());
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);

        // --- ATAJOS DE TECLADO ---
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getSaveButton().isDisabled()) main.getSaveButton().fire();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!main.getOpenButton().isDisabled()) main.getOpenButton().fire();
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

        return scene;
    }
}