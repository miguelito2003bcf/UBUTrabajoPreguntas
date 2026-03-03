//Trabajo realizado por Miguel Alonso Alonso.

package moodleviewer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.parser.XMLParser;

import java.io.File;

public class Main extends Application {

    private TreeView<Category> categoryTreeView;
    private ListView<Question> questionListView;
    private TextArea detailsTextArea;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Visor de Preguntas Moodle XML");

        // Elementos de la UI
        categoryTreeView = new TreeView<>();
        questionListView = new ListView<>();
        detailsTextArea = new TextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setWrapText(true);

        // Configurar los contenedores (VBox)
        VBox leftPane = new VBox(5, new Label("Categorías:"), categoryTreeView);
        VBox rightTopPane = new VBox(5, new Label("Preguntas:"), questionListView);
        VBox rightBottomPane = new VBox(5, new Label("Detalles de la pregunta:"), detailsTextArea);

        leftPane.setPadding(new Insets(5));
        rightTopPane.setPadding(new Insets(5));
        rightBottomPane.setPadding(new Insets(5));

        // Truco: Hacer que las listas crezcan para ocupar todo el espacio disponible
        VBox.setVgrow(categoryTreeView, Priority.ALWAYS);
        VBox.setVgrow(questionListView, Priority.ALWAYS);
        VBox.setVgrow(detailsTextArea, Priority.ALWAYS);

        // 1. SplitPane Derecho (Vertical: Preguntas arriba, Detalles abajo)
        SplitPane rightSplitPane = new SplitPane();
        rightSplitPane.setOrientation(Orientation.VERTICAL);
        rightSplitPane.getItems().addAll(rightTopPane, rightBottomPane);
        rightSplitPane.setDividerPositions(0.5f); // Mitad y mitad

        // 2. SplitPane Principal (Horizontal: Categorías izquierda, Resto a la derecha)
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(leftPane, rightSplitPane);
        mainSplitPane.setDividerPositions(0.3f); // 30% para la izquierda, 70% para la derecha

        // Barra superior con botón para abrir archivo
        Button openButton = new Button("Cargar XML de Moodle");
        openButton.setOnAction(e -> openFile(primaryStage));
        ToolBar toolBar = new ToolBar(openButton);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(mainSplitPane);

        setupSelectionListeners();

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupSelectionListeners() {
        // Al seleccionar un elemento del árbol (rama), mostramos sus preguntas
        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                questionListView.setItems(FXCollections.observableArrayList(newVal.getValue().getQuestions()));
                detailsTextArea.clear();
            }
        });

        // Cuando se selecciona una pregunta, mostrar sus detalles
        questionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                detailsTextArea.setText(newVal.getDetails());
            }
        });
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                Category rootCategory = XMLParser.parseMoodleXML(file);
                TreeItem<Category> rootItem = createTreeItem(rootCategory);
                rootItem.setExpanded(true);
                
                categoryTreeView.setRoot(rootItem);
                categoryTreeView.setShowRoot(false); 
                
                questionListView.getItems().clear();
                detailsTextArea.clear();
                
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al leer el archivo XML: " + ex.getMessage());
                alert.showAndWait();
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