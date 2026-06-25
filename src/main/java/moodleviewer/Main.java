/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.model.ClozeQuestion;
import moodleviewer.util.I18n;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import moodleviewer.commands.CommandManager;
import org.controlsfx.control.textfield.CustomTextField;

import java.util.HashSet;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Clase principal de la aplicación. Gestiona la interfaz, el estado del banco de preguntas
 * y la actualización dinámica de idioma.
 */
public class Main extends Application {

    private TreeView<Category> categoryTreeView = new TreeView<>();
    private TableView<Question> questionTableView = new TableView<>();
    private WebView detailsWebView = new WebView();
    
    private FileManager fileManager = new FileManager();
    
    // Botón único de apertura: acepta XML y GIFT, y decide sustituir/combinar según el estado actual.
    private Button openBankButton = new Button(I18n.get("main.btn.openBank"));

    private Button saveButton = new Button(I18n.get("main.btn.saveXml"));
    private Button addQuestionButton = new Button(I18n.get("main.btn.addQuestion"));
    private Button exportLatexButton = new Button(I18n.get("main.btn.saveLatex"));
    private Button exportGiftButton = new Button(I18n.get("main.btn.exportGift"));
    
    private Button addCategoryButton = new Button(I18n.get("main.btn.addCategory"));
    private Button statsButton = new Button(I18n.get("main.btn.stats"));
    private Button duplicatesButton = new Button(I18n.get("main.btn.duplicates"));
    
    private Button undoButton = new Button();
    private Button redoButton = new Button();
    
    private CheckBox clozeToggle = new CheckBox(I18n.get("main.toggle.cloze"));
    
    private CustomTextField searchCategoryField = new CustomTextField();
    private CustomTextField searchQuestionField = new CustomTextField();
    private ComboBox<String> searchCriteriaCombo = new ComboBox<>();
    private MenuButton typeFilterMenu = new MenuButton(I18n.get("main.filter.type"));
    
    private Category currentRootCategory; 
    private Label fileNameLabel = new Label(I18n.get("main.lbl.noFile"));
    private Set<String> activeTypeFilters = new HashSet<>(); 
    
    private HBox languageBox = new HBox(8);
    private File loadedFile = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(I18n.get("main.window.title"));

        initBasicComponents(primaryStage);
        
        // 1. GESTORES DESACOPLADOS: Solo se les pasa su componente visual respectivo
        TableManager.configure(questionTableView);
        TreeManager.configure(categoryTreeView);
        
        Scene scene = LayoutManager.buildScene(this);

        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Evita perder cambios sin guardar al cerrar la ventana por accidente: si el documento
        // tiene modificaciones pendientes, se pregunta antes de permitir que la aplicación cierre.
        primaryStage.setOnCloseRequest(event -> {
            if (!confirmDiscardUnsavedChanges(primaryStage)) {
                event.consume();
            }
        });

        primaryStage.show();
    }

    /**
     * Si el documento actual tiene cambios sin guardar (según {@code CommandManager.isDirty()}),
     * pregunta al usuario qué desea hacer antes de continuar con una operación que los
     * descartaría (cerrar la aplicación, sustituir el banco actual al abrir uno nuevo, etc.).
     *
     * @param stage ventana sobre la que anclar los diálogos (necesaria si el usuario elige guardar).
     * @return true si la operación que motivó la llamada puede continuar; false si debe cancelarse.
     */
    private boolean confirmDiscardUnsavedChanges(Stage stage) {
        if (!CommandManager.getInstance().isDirty()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("file.dlg.unsavedChanges.title"));
        alert.setHeaderText(I18n.get("file.dlg.unsavedChanges.header"));
        alert.setContentText(I18n.get("file.dlg.unsavedChanges.content"));

        ButtonType btnSave = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnSave"), ButtonBar.ButtonData.YES);
        ButtonType btnDiscard = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnDiscard"), ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnCancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSave, btnDiscard, btnCancel);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == btnCancel) {
            return false;
        }
        if (choice.get() == btnSave) {
            Category preselected = getSelectedTreeCategory();
            boolean saved = fileManager.saveMoodleXML(stage, currentRootCategory, preselected, loadedFile).isPresent();
            return saved || !CommandManager.getInstance().isDirty();
        }
        return true; // btnDiscard
    }

    private void initBasicComponents(Stage stage) {
        searchCategoryField.setPromptText(I18n.get("main.search.category"));
        searchCategoryField.textProperty().addListener((obs, oldVal, newVal) -> applyCategoryFilter(newVal));
        searchCategoryField.setLeft(IconFactory.of(FontAwesomeSolid.SEARCH, 13, "#868e96"));

        searchQuestionField.setPromptText(I18n.get("main.search.question"));
        searchQuestionField.textProperty().addListener((obs, oldVal, newVal) -> refreshQuestionTable());
        searchQuestionField.setLeft(IconFactory.of(FontAwesomeSolid.SEARCH, 13, "#868e96"));
        HBox.setHgrow(searchQuestionField, Priority.ALWAYS); 
        
        searchCriteriaCombo.setItems(FXCollections.observableArrayList(
            I18n.get("main.search.byName"), 
            I18n.get("main.search.byText")
        ));
        searchCriteriaCombo.getSelectionModel().selectFirst();
        searchCriteriaCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshQuestionTable());

        typeFilterMenu.setDisable(true); 
        fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");

        // --- BOTÓN ÚNICO DE APERTURA (XML + GIFT, sustituir o combinar) ---
        openBankButton.setId("btn-primary");
        openBankButton.setGraphic(IconFactory.of(FontAwesomeSolid.FOLDER_OPEN, 14, "white"));
        openBankButton.setGraphicTextGap(8);
        openBankButton.setOnAction(e -> handleOpenBank(stage));

        saveButton.setDisable(true);
        saveButton.setOnAction(e -> {
            Category preselected = getSelectedTreeCategory();
            Optional<File> savedFile = fileManager.saveMoodleXML(stage, currentRootCategory, preselected, loadedFile);
            savedFile.ifPresent(file -> {
                loadedFile = file;
                fileNameLabel.setText(I18n.get("main.lbl.currentFile", loadedFile.getName()));
            });
        });

        addQuestionButton.setDisable(true);
        addQuestionButton.setOnAction(e -> showAddQuestionDialog());
        addQuestionButton.setId("btn-success");
        addQuestionButton.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 14, "white"));
        addQuestionButton.setGraphicTextGap(8);
        
        exportLatexButton.setDisable(true);
        exportLatexButton.setOnAction(e -> fileManager.exportLaTeX(stage, currentRootCategory, getSelectedTreeCategory()));

        exportGiftButton.setDisable(true);
        exportGiftButton.setGraphic(IconFactory.of(FontAwesomeSolid.FILE_EXPORT, 14, "#495057"));
        exportGiftButton.setGraphicTextGap(8);
        exportGiftButton.setOnAction(e -> {
            if (currentRootCategory != null) {
                fileManager.exportGIFT(stage, currentRootCategory, getSelectedTreeCategory());
            }
        });

        addCategoryButton.setDisable(true);
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());
        addCategoryButton.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 14, "#495057"));
        addCategoryButton.setGraphicTextGap(8);

        statsButton.setDisable(true);
        statsButton.setGraphic(IconFactory.of(FontAwesomeSolid.CHART_BAR, 14, "#495057"));
        statsButton.setGraphicTextGap(8);
        statsButton.setOnAction(e -> {
            if (currentRootCategory != null) {
                DashboardDialog dashboard = new DashboardDialog(currentRootCategory);
                dashboard.showAndWait();
            }
        });

        duplicatesButton.setDisable(true);
        duplicatesButton.setGraphic(IconFactory.of(FontAwesomeSolid.CLONE, 14, "#495057"));
        duplicatesButton.setGraphicTextGap(8);
        duplicatesButton.setOnAction(e -> {
            if (currentRootCategory != null) {
                DuplicateQuestionsDialog.showDuplicates(currentRootCategory);
            }
        });

        // --- BOTONES DESHACER / REHACER ---
        undoButton.setGraphic(IconFactory.of(FontAwesomeSolid.UNDO, 16, "#495057"));
        undoButton.setTooltip(new Tooltip(I18n.get("main.btn.undo.tooltip")));
        undoButton.setStyle("-fx-padding: 0 10 0 10;");
        undoButton.setDisable(true);
        undoButton.setOnAction(e -> CommandManager.getInstance().undo());

        redoButton.setGraphic(IconFactory.of(FontAwesomeSolid.REDO, 16, "#495057"));
        redoButton.setTooltip(new Tooltip(I18n.get("main.btn.redo.tooltip")));
        redoButton.setStyle("-fx-padding: 0 10 0 10;");
        redoButton.setDisable(true);
        redoButton.setOnAction(e -> CommandManager.getInstance().redo());

        EventBus.getInstance().subscribe(AppEvents.UndoRedoStateChangedEvent.class, event -> {
            undoButton.setDisable(!event.canUndo());
            redoButton.setDisable(!event.canRedo());
        });

        clozeToggle.setVisible(false);
        clozeToggle.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-text-fill: #1177d1; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-border-color: #dee2e6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        clozeToggle.selectedProperty().addListener((obs, old, isSelected) -> {
            ClozeQuestion.MODO_PREVIA_ALUMNO = isSelected;
            Question current = questionTableView.getSelectionModel().getSelectedItem();
            if (current != null) {
                detailsWebView.getEngine().loadContent(current.getDetails());
            }
        });

        // 2. BUS DE EVENTOS
        EventBus.getInstance().subscribe(AppEvents.CategorySelectedEvent.class, event -> {
            refreshQuestionTable();
            detailsWebView.getEngine().loadContent("");
            clozeToggle.setVisible(false);
        });

        EventBus.getInstance().subscribe(AppEvents.CategoryUpdatedEvent.class, event -> {
            applyCategoryFilter(searchCategoryField.getText());
            refreshQuestionTable(); 
        });

        EventBus.getInstance().subscribe(AppEvents.QuestionDeletedEvent.class, event -> {
            refreshQuestionTable(); 
            categoryTreeView.refresh(); 
            populateTypeFilterMenu(currentRootCategory); 
        });

        questionTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clozeToggle.setVisible(newVal instanceof ClozeQuestion);
                detailsWebView.getEngine().loadContent(newVal.getDetails());
            } else {
                clozeToggle.setVisible(false);
                detailsWebView.getEngine().loadContent("");
            }
        });

        double flagWidth = 21;
        double flagHeight = 14;

        javafx.scene.Node nodeEs;
        try {
            ImageView imgEs = new ImageView(new Image(getClass().getResourceAsStream("/es.png")));
            imgEs.setFitWidth(flagWidth);
            imgEs.setFitHeight(flagHeight);
            imgEs.setPreserveRatio(false);
            imgEs.setStyle("-fx-cursor: hand;");
            nodeEs = imgEs;
        } catch (Exception ex) {
            Label lblEs = new Label("🇪🇸");
            lblEs.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");
            nodeEs = lblEs;
        }
        nodeEs.setOnMouseClicked(e -> { I18n.setLocale(Locale.of("es", "ES")); updateLanguage(stage); });

        javafx.scene.Node nodeEn;
        try {
            ImageView imgEn = new ImageView(new Image(getClass().getResourceAsStream("/uk.png")));
            imgEn.setFitWidth(flagWidth);
            imgEn.setFitHeight(flagHeight);
            imgEn.setPreserveRatio(false);
            imgEn.setStyle("-fx-cursor: hand;");
            nodeEn = imgEn;
        } catch (Exception ex) {
            Label lblEn = new Label("🇬🇧");
            lblEn.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");
            nodeEn = lblEn;
        }
        nodeEn.setOnMouseClicked(e -> { I18n.setLocale(Locale.of("en", "GB")); updateLanguage(stage); });

        languageBox.getChildren().addAll(nodeEs, nodeEn);
        languageBox.setAlignment(Pos.CENTER);
    }

    /**
     * Maneja la apertura unificada del banco: delega la elección de fichero, formato y, si ya
     * había un banco cargado, la decisión de sustituir o combinar en {@code FileManager}.
     * Tras una sustitución, limpia el historial de deshacer/rehacer (es un documento nuevo);
     * tras una combinación, lo conserva (es una modificación del documento actual).
     */
    private void handleOpenBank(Stage stage) {
        Optional<FileManager.OpenResult> result = fileManager.openOrMergeBank(stage, currentRootCategory);
        if (result.isEmpty()) {
            return;
        }

        FileManager.OpenResult openResult = result.get();

        // Si la operación va a SUSTITUIR el banco actual (no a combinarlo) y ese banco tenía
        // cambios sin guardar, se avisa justo antes de aplicar el resultado. Se pregunta aquí,
        // después de que el usuario ya haya elegido "sustituir" en FileManager, para no
        // encadenar dos confirmaciones distintas cuando lo que realmente quiere es combinar
        // (donde no se pierde nada y no hay nada que avisar).
        if (!openResult.wasMerge() && !confirmDiscardUnsavedChanges(stage)) {
            return;
        }

        currentRootCategory = openResult.rootCategory();
        loadedFile = openResult.file();
        fileNameLabel.setText(I18n.get("main.lbl.currentFile", loadedFile.getName()));

        populateTypeFilterMenu(currentRootCategory);
        categoryTreeView.setRoot(TreeBuilder.createTreeItem(currentRootCategory));
        categoryTreeView.setShowRoot(false);
        refreshQuestionTable();

        if (!openResult.wasMerge()) {
            // Sustitución: es un documento nuevo, no tiene sentido conservar el historial
            // de deshacer/rehacer ni el estado "modificado" de un banco que ya no existe.
            questionTableView.getItems().clear();
            detailsWebView.getEngine().loadContent("");
            CommandManager.getInstance().clear();
        }
        // Si fue una combinación, FileManager ya ha marcado el documento como modificado
        // (markAsDirty) y el historial de deshacer/rehacer se conserva intacto.

        saveButton.setDisable(false);
        addQuestionButton.setDisable(false);
        addCategoryButton.setDisable(false);
        exportLatexButton.setDisable(false);
        exportGiftButton.setDisable(false);
        statsButton.setDisable(false);
        duplicatesButton.setDisable(false);
    }

    private void updateLanguage(Stage stage) {
        stage.setTitle(I18n.get("main.window.title"));
        openBankButton.setText(I18n.get("main.btn.openBank"));
        saveButton.setText(I18n.get("main.btn.saveXml"));
        addQuestionButton.setText(I18n.get("main.btn.addQuestion"));
        exportLatexButton.setText(I18n.get("main.btn.saveLatex"));
        exportGiftButton.setText(I18n.get("main.btn.exportGift"));
        addCategoryButton.setText(I18n.get("main.btn.addCategory"));
        statsButton.setText(I18n.get("main.btn.stats")); 
        duplicatesButton.setText(I18n.get("main.btn.duplicates"));
        undoButton.getTooltip().setText(I18n.get("main.btn.undo.tooltip"));
        redoButton.getTooltip().setText(I18n.get("main.btn.redo.tooltip"));
        clozeToggle.setText(I18n.get("main.toggle.cloze"));
        searchCategoryField.setPromptText(I18n.get("main.search.category"));
        searchQuestionField.setPromptText(I18n.get("main.search.question"));
        
        int selectedIndex = searchCriteriaCombo.getSelectionModel().getSelectedIndex();
        searchCriteriaCombo.setItems(FXCollections.observableArrayList(
            I18n.get("main.search.byName"), 
            I18n.get("main.search.byText")
        ));
        searchCriteriaCombo.getSelectionModel().select(Math.max(0, selectedIndex));
        
        if (loadedFile == null) fileNameLabel.setText(I18n.get("main.lbl.noFile"));
        else fileNameLabel.setText(I18n.get("main.lbl.currentFile", loadedFile.getName()));
        
        updateFilterButtonStyle();
        
        if (!questionTableView.getColumns().isEmpty()) {
            questionTableView.getColumns().get(0).setText(I18n.get("table.col.name"));
            questionTableView.getColumns().get(1).setText(I18n.get("table.col.type"));
        }
        categoryTreeView.refresh();
        questionTableView.refresh();
    }

    private Category getSelectedTreeCategory() {
        TreeItem<Category> selectedItem = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return null;
        Category category = selectedItem.getValue();
        return (category == currentRootCategory) ? null : category;
    }

    private void showAddCategoryDialog() {
        if (currentRootCategory == null) return;
        TreeItem<Category> selectedItem = categoryTreeView.getSelectionModel().getSelectedItem();
        Category parentCategory = (selectedItem != null) ? selectedItem.getValue() : currentRootCategory;
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("main.dlg.cat.title"));
        dialog.setHeaderText(I18n.get("main.dlg.cat.header"));
        ButtonType btnAceptar = new ButtonType(I18n.get("main.dlg.btnAccept"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField nameField = new TextField();
        ToggleGroup group = new ToggleGroup();
        RadioButton rbRoot = new RadioButton(I18n.get("main.dlg.cat.root"));
        RadioButton rbSub = new RadioButton(I18n.get("main.dlg.cat.sub", (selectedItem != null ? parentCategory.getName() : "")));
        rbRoot.setToggleGroup(group); rbSub.setToggleGroup(group);
        if (selectedItem != null) rbSub.setSelected(true); else { rbRoot.setSelected(true); rbSub.setDisable(true); }
        grid.add(new Label(I18n.get("main.dlg.cat.location")), 0, 0);
        grid.add(new VBox(5, rbRoot, rbSub), 1, 0);
        grid.add(new Label(I18n.get("main.dlg.cat.name")), 0, 1);
        grid.add(nameField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(db -> {
            if (db == btnAceptar && !nameField.getText().trim().isEmpty()) {
                Category newCat = new Category(nameField.getText().trim());
                if (rbRoot.isSelected()) currentRootCategory.getSubcategories().add(newCat);
                else parentCategory.getSubcategories().add(newCat);
                applyCategoryFilter(searchCategoryField.getText());
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showAddQuestionDialog() {
        TreeItem<Category> selectedCategoryItem = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedCategoryItem == null) {
            new Alert(Alert.AlertType.WARNING, I18n.get("main.alert.selectCat")).showAndWait();
            return;
        }
        Category targetCategory = selectedCategoryItem.getValue();
        AddQuestionDialog dialog = new AddQuestionDialog(targetCategory);
        
        dialog.showAndWait();
        
        populateTypeFilterMenu(currentRootCategory);
        refreshQuestionTable();
        categoryTreeView.refresh(); 
    }

    public void applyCategoryFilter(String searchText) {
        if (currentRootCategory == null) return;

        TreeItem<Category> previouslySelected = categoryTreeView.getSelectionModel().getSelectedItem();
        Category categoryToReselect = (previouslySelected != null) ? previouslySelected.getValue() : null;

        TreeItem<Category> newRoot;
        if (searchText == null || searchText.trim().isEmpty()) {
            newRoot = TreeBuilder.createTreeItem(currentRootCategory);
        } else {
            newRoot = TreeBuilder.createFilteredTreeItem(currentRootCategory, searchText.toLowerCase());
        }
        categoryTreeView.setRoot(newRoot);

        if (categoryToReselect != null && newRoot != null) {
            TreeItem<Category> match = findTreeItemByCategory(newRoot, categoryToReselect);
            if (match != null) {
                categoryTreeView.getSelectionModel().select(match);
            }
        }
    }

    private TreeItem<Category> findTreeItemByCategory(TreeItem<Category> node, Category target) {
        if (node == null) return null;
        if (node.getValue() == target) return node;
        for (TreeItem<Category> child : node.getChildren()) {
            TreeItem<Category> found = findTreeItemByCategory(child, target);
            if (found != null) return found;
        }
        return null;
    }

    public void refreshQuestionTable() {
        TreeItem<Category> selectedNode = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedNode != null && selectedNode.getValue() != null) {
            Category selectedCat = selectedNode.getValue();
            ObservableList<Question> obsList = FXCollections.observableArrayList(selectedCat.getQuestions());
            
            FilteredList<Question> filteredData = new FilteredList<>(obsList, q -> {
                String searchTxt = searchQuestionField.getText();
                boolean matchesType = activeTypeFilters.isEmpty() || activeTypeFilters.contains(q.getType());

                if (searchTxt == null || searchTxt.trim().isEmpty()) {
                    return matchesType;
                }

                String lowerSearch = searchTxt.toLowerCase();
                boolean matchesText = false;
                
                int searchIndex = searchCriteriaCombo.getSelectionModel().getSelectedIndex();
                if (searchIndex == 0) {
                    matchesText = q.getName().toLowerCase().contains(lowerSearch);
                } else if (searchIndex == 1) {
                    matchesText = q.getText() != null && q.getText().toLowerCase().contains(lowerSearch);
                }
                
                return matchesText && matchesType;
            });
            
            SortedList<Question> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(questionTableView.comparatorProperty());
            questionTableView.setItems(sortedData);
        } else {
            questionTableView.setItems(FXCollections.observableArrayList());
        }
    }

    private void populateTypeFilterMenu(Category rootCategory) {
        typeFilterMenu.getItems().clear();
        activeTypeFilters.clear(); 
        updateFilterButtonStyle();
        Set<String> uniqueTypes = new HashSet<>();
        collectTypesRecursively(rootCategory, uniqueTypes);
        for (String type : uniqueTypes) {
            CheckMenuItem menuItem = new CheckMenuItem(type.toUpperCase());
            menuItem.selectedProperty().addListener((obs, was, is) -> {
                if (is) activeTypeFilters.add(type); else activeTypeFilters.remove(type);
                updateFilterButtonStyle();
                refreshQuestionTable(); 
            });
            typeFilterMenu.getItems().add(menuItem);
        }
        typeFilterMenu.setDisable(false); 
    }

    private void collectTypesRecursively(Category cat, Set<String> typesSet) {
        for (Question q : cat.getQuestions()) typesSet.add(q.getType());
        for (Category sub : cat.getSubcategories()) collectTypesRecursively(sub, typesSet);
    }
    
    private void updateFilterButtonStyle() {
        if (activeTypeFilters.isEmpty()) {
            typeFilterMenu.setStyle(""); 
            typeFilterMenu.setText(I18n.get("main.filter.type"));
        } else {
            typeFilterMenu.setStyle("-fx-background-color: #dc3545; -fx-border-color: #c82333; -fx-text-fill: white; -fx-font-weight: bold;");
            typeFilterMenu.setText(I18n.get("main.filter.typeActive", String.valueOf(activeTypeFilters.size())));
        }
    }
    
    // Getters
    public TreeView<Category> getCategoryTreeView() { return categoryTreeView; }
    public TableView<Question> getQuestionTableView() { return questionTableView; }
    public WebView getDetailsWebView() { return detailsWebView; }
    public TextField getSearchCategoryField() { return searchCategoryField; }
    public TextField getSearchQuestionField() { return searchQuestionField; }
    public ComboBox<String> getSearchCriteriaCombo() { return searchCriteriaCombo; }
    public MenuButton getTypeFilterMenu() { return typeFilterMenu; }
    public Button getAddQuestionButton() { return addQuestionButton; }
    public Button getAddCategoryButton() { return addCategoryButton; } 
    public Button getOpenBankButton() { return openBankButton; }
    public Button getSaveButton() { return saveButton; }
    public Button getExportLatexButton() { return exportLatexButton; }
    public Button getExportGiftButton() { return exportGiftButton; }
    public Button getStatsButton() { return statsButton; }
    public Button getDuplicatesButton() { return duplicatesButton; }
    public Button getUndoButton() { return undoButton; }
    public Button getRedoButton() { return redoButton; }
    public CheckBox getClozeToggle() { return clozeToggle; }
    public Label getFileNameLabel() { return fileNameLabel; }
    public HBox getLanguageBox() { return languageBox; }

    public static void main(String[] args) { launch(args); }
}