package moodleviewer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import moodleviewer.model.Category;
import moodleviewer.model.Question;

import java.util.ArrayList;
import java.util.List;

public class TableManager {
    
    public static void configure(Main main) {
        TableView<Question> table = main.getQuestionTableView();
        TreeView<Category> tree = main.getCategoryTreeView();

        TableColumn<Question, String> nameColumn = new TableColumn<>("Nombre de la pregunta");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        
        TableColumn<Question, String> typeColumn = new TableColumn<>("Tipo");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().toUpperCase()));
        typeColumn.setStyle("-fx-alignment: CENTER;"); 
        
        nameColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.75));
        typeColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.24));
        
        table.getColumns().add(nameColumn);
        table.getColumns().add(typeColumn);
        
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setOnDragDetected(event -> {
            List<Question> selectedQuestions = table.getSelectionModel().getSelectedItems();
            if (!selectedQuestions.isEmpty()) {
                Dragboard db = table.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString("MOVER_PREGUNTAS"); 
                db.setContent(content);
                
                Label dragLabel = new Label("Moviendo " + selectedQuestions.size() + " pregunta(s)");
                dragLabel.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-padding: 8px 12px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5px;");
                
                new Scene(dragLabel); 
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT); 
                db.setDragView(dragLabel.snapshot(params, null), dragLabel.getWidth() / 2, dragLabel.getHeight() / 2);
                event.consume();
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Question> row = new TableRow<>();
            ContextMenu questionMenu = new ContextMenu();
            
            MenuItem editQuestionItem = new MenuItem("✏️ Editar Nombre");
            editQuestionItem.setOnAction(event -> {
                Question q = row.getItem();
                if (q != null) {
                    TextInputDialog dialog = new TextInputDialog(q.getName());
                    dialog.setTitle("Editar Pregunta");
                    dialog.setHeaderText("Modificar el nombre de la pregunta");
                    
                    dialog.showAndWait().ifPresent(newName -> {
                        q.setName(newName); 
                        main.refreshQuestionTable(); 
                    });
                }
            });

            MenuItem deleteQuestionItem = new MenuItem("🗑️ Eliminar Pregunta(s)");
            deleteQuestionItem.setOnAction(event -> {
                List<Question> questionsToDelete = new ArrayList<>(table.getSelectionModel().getSelectedItems());
                TreeItem<Category> selectedCategory = tree.getSelectionModel().getSelectedItem();
                
                if (!questionsToDelete.isEmpty() && selectedCategory != null) {
                    selectedCategory.getValue().getQuestions().removeAll(questionsToDelete);
                    main.refreshQuestionTable(); 
                    main.getDetailsWebView().getEngine().loadContent(""); 
                    tree.refresh(); 
                }
            });
            
            questionMenu.getItems().addAll(editQuestionItem, deleteQuestionItem);
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(questionMenu));
            return row;
        });
    }
}