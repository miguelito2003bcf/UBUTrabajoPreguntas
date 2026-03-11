package moodleviewer;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.parser.XMLParser;
import moodleviewer.parser.XMLExporter;

import java.io.File;

public class Main extends Application {

    private TreeView<Category> categoryTreeView;
    
    private TableView<Question> questionTableView; 
    private WebView detailsWebView;
    
    private Category currentRootCategory; 
    private Button saveButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Visor de Preguntas Moodle XML");

        categoryTreeView = new TreeView<>();
        detailsWebView = new WebView();
        
        questionTableView = new TableView<>();
        
        TableColumn<Question, String> nameColumn = new TableColumn<>("Nombre de la pregunta");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        
        TableColumn<Question, String> typeColumn = new TableColumn<>("Tipo");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        
        nameColumn.prefWidthProperty().bind(questionTableView.widthProperty().multiply(0.75));
        typeColumn.prefWidthProperty().bind(questionTableView.widthProperty().multiply(0.24));
        
        questionTableView.getColumns().add(nameColumn);
        questionTableView.getColumns().add(typeColumn);

        VBox leftPane = new VBox(5, new Label("Categorías:"), categoryTreeView);
        VBox rightTopPane = new VBox(5, new Label("Preguntas:"), questionTableView); 
        VBox rightBottomPane = new VBox(5, new Label("Detalles de la pregunta:"), detailsWebView);

        leftPane.setPadding(new Insets(5));
        rightTopPane.setPadding(new Insets(5));
        rightBottomPane.setPadding(new Insets(5));

        VBox.setVgrow(categoryTreeView, Priority.ALWAYS);
        VBox.setVgrow(questionTableView, Priority.ALWAYS); 
        VBox.setVgrow(detailsWebView, Priority.ALWAYS);

        SplitPane rightSplitPane = new SplitPane();
        rightSplitPane.setOrientation(Orientation.VERTICAL);
        rightSplitPane.getItems().addAll(rightTopPane, rightBottomPane);
        rightSplitPane.setDividerPositions(0.5f);

        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(leftPane, rightSplitPane);
        mainSplitPane.setDividerPositions(0.3f);

        Button openButton = new Button("Cargar XML de Moodle");
        openButton.setOnAction(e -> openFile(primaryStage));

        saveButton = new Button("Guardar Cambios XML");
        saveButton.setOnAction(e -> saveFile(primaryStage));
        saveButton.setDisable(true); 

        ToolBar toolBar = new ToolBar(openButton, saveButton);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(mainSplitPane);

        setupSelectionListeners();

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupSelectionListeners() {
        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                questionTableView.setItems(FXCollections.observableArrayList(newVal.getValue().getQuestions()));
                detailsWebView.getEngine().loadContent("");
            }
        });

        questionTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                detailsWebView.getEngine().loadContent(newVal.getDetails());
            }
        });
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                currentRootCategory = XMLParser.parseMoodleXML(file);
                TreeItem<Category> rootItem = createTreeItem(currentRootCategory);
                rootItem.setExpanded(true);
                
                categoryTreeView.setRoot(rootItem);
                categoryTreeView.setShowRoot(false); 
                
                questionTableView.getItems().clear(); 
                detailsWebView.getEngine().loadContent(""); 
                
                saveButton.setDisable(false);
                
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al leer el archivo XML: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void saveFile(Stage stage) {
        if (currentRootCategory == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar XML de Moodle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                XMLExporter.exportMoodleXML(currentRootCategory, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Archivo guardado con éxito.");
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar el archivo XML: " + ex.getMessage());
                alert.showAndWait();
                ex.printStackTrace();
            }
        }
    }

    private TreeItem<Category> createTreeItem(Category category) {
        TreeItem<Category> item = new TreeItem<>(category);
        for (Category sub : category.getSubcategories()) {
            item.getChildren().add(createTreeItem(sub));
        }
        item.setExpanded(true); 
        return item;
    }

    public static void main(String[] args) {
        launch(args);
    }
}