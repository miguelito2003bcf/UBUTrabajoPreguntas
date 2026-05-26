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
import moodleviewer.model.ClozeQuestion;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Clase creada como punto de entrada y controlador principal de la aplicación.
 * Mantiene el estado global de la sesión y coordina los gestores especializados.
 */
public class Main extends Application {

    private TreeView<Category> categoryTreeView = new TreeView<>();
    private TableView<Question> questionTableView = new TableView<>();
    private WebView detailsWebView = new WebView();
    private Button openButton = new Button("Cargar XML de Moodle");
    private Button saveButton = new Button("Guardar Cambios XML");
    private Button addQuestionButton = new Button("➕ Añadir Pregunta");
    private Button exportLatexButton = new Button("Guardar Cambios LaTeX");
    private Button addCategoryButton = new Button("➕ Categoría");
    private CheckBox clozeToggle = new CheckBox("Ver como el alumno (Renderizado)");
    private TextField searchCategoryField = new TextField();
    private TextField searchQuestionField = new TextField();
    private MenuButton typeFilterMenu = new MenuButton("Filtrar por Tipo");
    private Category currentRootCategory; 
    private Set<String> activeTypeFilters = new HashSet<>(); 

    /**
     * Punto de entrada de JavaFX. Inicializa los componentes y muestra la ventana principal.
     *
     * @param primaryStage ventana principal proporcionada por JavaFX.
     */
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

    /**
     * Inicializa los componentes básicos y registra todos los listeners de interacción.
     * 
     * @param stage ventana principal.
     */
    private void initBasicComponents(Stage stage) {
        searchCategoryField.setPromptText("🔍 Buscar categoría...");
        searchCategoryField.textProperty().addListener((obs, oldVal, newVal) -> applyCategoryFilter(newVal));

        searchQuestionField.setPromptText("🔍 Buscar por nombre...");
        searchQuestionField.textProperty().addListener((obs, oldVal, newVal) -> refreshQuestionTable());
        HBox.setHgrow(searchQuestionField, Priority.ALWAYS); 

        typeFilterMenu.setDisable(true); 
        
        addQuestionButton.setDisable(true);
        addQuestionButton.setOnAction(e -> showAddQuestionDialog());
        
        exportLatexButton.setDisable(true);
        exportLatexButton.setOnAction(e -> FileManager.exportLaTeX(stage, currentRootCategory));

        addCategoryButton.setDisable(true);
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());

        openButton.setOnAction(e -> handleLoadXML(stage));
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> FileManager.saveXML(stage, currentRootCategory));

        clozeToggle.setVisible(false);
        clozeToggle.setStyle("-fx-text-fill: #1177d1; -fx-font-weight: bold; -fx-padding: 5 0 5 10;");
        clozeToggle.selectedProperty().addListener((obs, old, isSelected) -> {
            ClozeQuestion.MODO_PREVIA_ALUMNO = isSelected;
            Question current = questionTableView.getSelectionModel().getSelectedItem();
            if (current != null) {
                detailsWebView.getEngine().loadContent(current.getDetails());
            }
        });

        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            refreshQuestionTable();
            detailsWebView.getEngine().loadContent("");
            clozeToggle.setVisible(false);
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
    }

    /**
     * Carga un XML de Moodle y actualiza toda la interfaz con los datos leídos.
     * 
     * @param stage ventana padre para el diálogo de apertura de fichero.
     */
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
            exportLatexButton.setDisable(false);
        });
    }

    /**
     * Muestra el diálogo para crear una nueva categoría en el banco. Permite elegir entre crearla como raíz
     * o como subcategoría de la selección dada.
     */
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
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        ToggleGroup group = new ToggleGroup();
        RadioButton rbRoot = new RadioButton("Como Raíz");
        RadioButton rbSub = new RadioButton("Como subcategoría de: " + (selectedItem != null ? parentCategory.getName() : ""));
        rbRoot.setToggleGroup(group); rbSub.setToggleGroup(group);

        if (selectedItem != null) rbSub.setSelected(true); else { rbRoot.setSelected(true); rbSub.setDisable(true); }

        grid.add(new Label("Ubicación:"), 0, 0);
        grid.add(new VBox(5, rbRoot, rbSub), 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
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

    /**
     * Muestra el diálogo para añadir una pregunta para la categoría seleccionada. Tras añadir la pregunta, actualiza
     * el menú de tipos y refresca la tabla.
     */
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

    /**
     * Aplica un filtro de texto al árbol de categorías. Si el texto está vacío, muestra el árbol completo
     * sin filtrar.
     * 
     * @param searchText texto de búsqueda.
     */
    public void applyCategoryFilter(String searchText) {
        if (currentRootCategory == null) return;
        if (searchText == null || searchText.trim().isEmpty()) {
            categoryTreeView.setRoot(TreeBuilder.createTreeItem(currentRootCategory));
            return;
        }
        TreeItem<Category> filteredRoot = TreeBuilder.createFilteredTreeItem(currentRootCategory, searchText.toLowerCase());
        categoryTreeView.setRoot(filteredRoot);
    }

    /**
     * Actualiza la tabla de preguntas aplicando el filtro de texto y de tipo activos. Si no hay categoría seleccionada la tabla se vacía.
     */
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

    /**
     * Rellena el menú de filtrado por tipo con todos los tipos presentes en el banco.
     * Limpia los filtros activos al regenerar el menú para evitar estados inconsistentes.
     * 
     * @param rootCategory categoría raíz desde la que se recolectan los tipos.
     */
    private void populateTypeFilterMenu(Category rootCategory) {
        typeFilterMenu.getItems().clear();
        activeTypeFilters.clear(); 
        Set<String> uniqueTypes = new HashSet<>();
        collectTypesRecursively(rootCategory, uniqueTypes);
        for (String type : uniqueTypes) {
            CheckMenuItem menuItem = new CheckMenuItem(type);
            menuItem.selectedProperty().addListener((obs, was, is) -> {
                if (is) activeTypeFilters.add(type); else activeTypeFilters.remove(type);
                refreshQuestionTable(); 
            });
            typeFilterMenu.getItems().add(menuItem);
        }
        typeFilterMenu.setDisable(false); 
    }

    /**
     * Recorre recursivamente el árbol acumulando todos los tipos de pregunta distintos.
     * 
     * @param cat categoría a explorar.
     * @param typesSet conjunto acumulador de tipos.
     */
    private void collectTypesRecursively(Category cat, Set<String> typesSet) {
        for (Question q : cat.getQuestions()) typesSet.add(q.getType());
        for (Category sub : cat.getSubcategories()) collectTypesRecursively(sub, typesSet);
    }

    /**
     * Comprueba si el menú de filtrado ya contiene un elemento para el tipo indicado.
     *
     * @param type tipo de pregunta a buscar.
     * @return true si ya existe un MenuItem con ese texto.
     */
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
    public Button getExportLatexButton() { return exportLatexButton; }
    public CheckBox getClozeToggle() { return clozeToggle; }

    /**
     * Método principal que lanza la aplicación JavaFX.
     * 
     * @param args argumentos de línea de comandos. (no utilizados)
     */
    public static void main(String[] args) { launch(args); }
}