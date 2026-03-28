package moodleviewer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import moodleviewer.model.Category;
import moodleviewer.model.Question;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Main extends Application {

    private TreeView<Category> categoryTreeView = new TreeView<>();
    private TableView<Question> questionTableView = new TableView<>();
    private WebView detailsWebView = new WebView();
    
    private Button openButton = new Button("Cargar XML de Moodle");
    private Button saveButton = new Button("Guardar Cambios XML");
    private Button addQuestionButton = new Button("➕ Añadir Pregunta");
    
    private Button addCategoryButton = new Button("➕ Categoría");

    private TextField searchCategoryField = new TextField();
    private TextField searchQuestionField = new TextField();
    private MenuButton typeFilterMenu = new MenuButton("Filtrar por Tipo");

    private Category currentRootCategory; 
    private Set<String> activeTypeFilters = new HashSet<>(); 

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Editor Avanzado de Preguntas Moodle XML");

        initBasicComponents(primaryStage);
        TableManager.configure(this);
        TreeManager.configure(this);
        Scene scene = LayoutManager.buildScene(this);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initBasicComponents(Stage stage) {
        searchCategoryField.setPromptText("🔍 Buscar categoría...");
        searchCategoryField.textProperty().addListener((obs, oldVal, newVal) -> applyCategoryFilter(newVal));

        searchQuestionField.setPromptText("🔍 Buscar por nombre...");
        searchQuestionField.textProperty().addListener((obs, oldVal, newVal) -> refreshQuestionTable());
        HBox.setHgrow(searchQuestionField, Priority.ALWAYS); 

        typeFilterMenu.setDisable(true); 
        
        addQuestionButton.setDisable(true);
        addQuestionButton.setOnAction(e -> showAddQuestionDialog());

        addCategoryButton.setDisable(true);
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());

        openButton.setOnAction(e -> handleLoadXML(stage));
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> FileManager.saveXML(stage, currentRootCategory));

        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            refreshQuestionTable();
            detailsWebView.getEngine().loadContent("");
        });

        questionTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) detailsWebView.getEngine().loadContent(newVal.getDetails());
        });
    }

    private void handleLoadXML(Stage stage) {
        FileManager.openXML(stage).ifPresent(root -> {
            currentRootCategory = root;
            searchCategoryField.setText("");
            searchQuestionField.setText("");
            populateTypeFilterMenu(currentRootCategory);
            
            categoryTreeView.setRoot(TreeBuilder.createTreeItem(currentRootCategory));
            categoryTreeView.setShowRoot(false); 
            
            questionTableView.getItems().clear(); 
            detailsWebView.getEngine().loadContent(""); 
            
            saveButton.setDisable(false);
            addQuestionButton.setDisable(false); 
            addCategoryButton.setDisable(false); 
        });
    }

    private void showAddCategoryDialog() {
        if (currentRootCategory == null) return;

        TreeItem<Category> selectedItem = categoryTreeView.getSelectionModel().getSelectedItem();
        Category parentCategory = (selectedItem != null) ? selectedItem.getValue() : currentRootCategory;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Añadir Nueva Categoría");
        dialog.setHeaderText("Crear una nueva categoría en el banco");

        ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Ej: Tema 2 - Álgebra");

        ToggleGroup group = new ToggleGroup();
        RadioButton rbRoot = new RadioButton("Como Categoría Principal (Raíz)");
        RadioButton rbSub = new RadioButton("Como subcategoría de: " + (selectedItem != null ? parentCategory.getName() : ""));
        rbRoot.setToggleGroup(group);
        rbSub.setToggleGroup(group);

        if (selectedItem != null) {
            rbSub.setSelected(true);
        } else {
            rbRoot.setSelected(true);
            rbSub.setDisable(true);
        }

        grid.add(new Label("Ubicación:"), 0, 0);
        grid.add(new VBox(5, rbRoot, rbSub), 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAceptar && !nameField.getText().trim().isEmpty()) {
                Category newCat = new Category(nameField.getText().trim());
                if (rbRoot.isSelected()) {
                    currentRootCategory.getSubcategories().add(newCat);
                } else {
                    parentCategory.getSubcategories().add(newCat);
                }
                applyCategoryFilter(searchCategoryField.getText());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showAddQuestionDialog() {
        TreeItem<Category> selectedCategoryItem = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedCategoryItem == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, selecciona una categoría.").showAndWait();
            return;
        }
        
        Category targetCategory = selectedCategoryItem.getValue();
        AddQuestionDialog dialog = new AddQuestionDialog(targetCategory);
        Optional<Question> result = dialog.showAndWait();

        result.ifPresent(newQuestion -> {
            targetCategory.addQuestion(newQuestion);
            if (!typeContainsFilter(newQuestion.getType())) populateTypeFilterMenu(currentRootCategory);
            refreshQuestionTable();
            categoryTreeView.refresh(); 
        });
    }

    public void applyCategoryFilter(String searchText) {
        if (currentRootCategory == null) return;
        if (searchText == null || searchText.trim().isEmpty()) {
            categoryTreeView.setRoot(TreeBuilder.createTreeItem(currentRootCategory));
            return;
        }
        TreeItem<Category> filteredRoot = TreeBuilder.createFilteredTreeItem(currentRootCategory, searchText.toLowerCase());
        categoryTreeView.setRoot(filteredRoot);
    }

    public void refreshQuestionTable() {
        TreeItem<Category> selectedNode = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedNode != null && selectedNode.getValue() != null) {
            Category selectedCat = selectedNode.getValue();
            ObservableList<Question> obsList = FXCollections.observableArrayList(selectedCat.getQuestions());
            
            FilteredList<Question> filteredData = new FilteredList<>(obsList, q -> {
                String searchTxt = searchQuestionField.getText();
                boolean matchesText = searchTxt == null || searchTxt.isEmpty() || q.getName().toLowerCase().contains(searchTxt.toLowerCase());
                boolean matchesType = activeTypeFilters.isEmpty() || activeTypeFilters.contains(q.getType());
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
        Set<String> uniqueTypes = new HashSet<>();
        collectTypesRecursively(rootCategory, uniqueTypes);
        
        for (String type : uniqueTypes) {
            CheckMenuItem menuItem = new CheckMenuItem(type);
            menuItem.setSelected(false); 
            menuItem.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) activeTypeFilters.add(type); else activeTypeFilters.remove(type);
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

    private boolean typeContainsFilter(String type) {
        for(MenuItem item : typeFilterMenu.getItems()) if (item.getText().equals(type)) return true;
        return false;
    }

    public TreeView<Category> getCategoryTreeView() { return categoryTreeView; }
    public TableView<Question> getQuestionTableView() { return questionTableView; }
    public WebView getDetailsWebView() { return detailsWebView; }
    public TextField getSearchCategoryField() { return searchCategoryField; }
    public TextField getSearchQuestionField() { return searchQuestionField; }
    public MenuButton getTypeFilterMenu() { return typeFilterMenu; }
    public Button getAddQuestionButton() { return addQuestionButton; }
    public Button getAddCategoryButton() { return addCategoryButton; } 
    public Button getOpenButton() { return openButton; }
    public Button getSaveButton() { return saveButton; }

    public static void main(String[] args) { launch(args); }
}