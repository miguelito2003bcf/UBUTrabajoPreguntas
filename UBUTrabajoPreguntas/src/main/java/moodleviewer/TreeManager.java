package moodleviewer;

import javafx.beans.binding.Bindings;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import moodleviewer.model.Category;
import moodleviewer.model.Question;

import java.util.ArrayList;
import java.util.List;

public class TreeManager {

    private static TreeItem<Category> draggedCategoryNode = null;

    public static void configure(Main main) {
        TreeView<Category> tree = main.getCategoryTreeView();
        TableView<Question> table = main.getQuestionTableView();

        tree.setCellFactory(tv -> {
            TreeCell<Category> cell = new TreeCell<Category>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName() + " (" + item.getQuestions().size() + ")");
                    }
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty() && cell.getItem() != null) {
                    TreeItem<Category> draggedNode = cell.getTreeItem();
                    
                    if (draggedNode != null && draggedNode.getParent() != null) { 
                        Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent content = new ClipboardContent();
                        content.putString("MOVER_CATEGORIA");
                        db.setContent(content);

                        draggedCategoryNode = draggedNode;

                        SnapshotParameters params = new SnapshotParameters();
                        params.setFill(Color.TRANSPARENT);
                        db.setDragView(cell.snapshot(params, null));

                        event.consume();
                    }
                }
            });
            
            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && !cell.isEmpty()) {
                    String dragType = db.getString();
                    TreeItem<Category> targetNode = cell.getTreeItem();

                    if ("MOVER_PREGUNTAS".equals(dragType)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    } else if ("MOVER_CATEGORIA".equals(dragType)) {
                        if (isValidDropTarget(draggedCategoryNode, targetNode)) {
                            event.acceptTransferModes(TransferMode.MOVE);
                        }
                    }
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                
                if (db.hasString() && !cell.isEmpty()) {
                    String dragType = db.getString();
                    TreeItem<Category> targetNode = cell.getTreeItem();
                    
                    if (targetNode != null && targetNode.getValue() != null) {
                        Category targetCategory = targetNode.getValue();

                        if ("MOVER_PREGUNTAS".equals(dragType)) {
                            TreeItem<Category> selectedNode = tree.getSelectionModel().getSelectedItem();
                            if (selectedNode != null && selectedNode.getValue() != targetCategory) {
                                List<Question> questionsToMove = new ArrayList<>(table.getSelectionModel().getSelectedItems());
                                selectedNode.getValue().getQuestions().removeAll(questionsToMove);
                                targetCategory.getQuestions().addAll(questionsToMove);
                                
                                main.refreshQuestionTable();
                                tree.refresh(); 
                                success = true;
                            }
                        } 
                        else if ("MOVER_CATEGORIA".equals(dragType)) {
                            if (draggedCategoryNode != null && isValidDropTarget(draggedCategoryNode, targetNode)) {
                                TreeItem<Category> sourceParentNode = draggedCategoryNode.getParent();
                                
                                if (sourceParentNode != null) {
                                    Category sourceCategory = draggedCategoryNode.getValue();
                                    Category sourceParentCategory = sourceParentNode.getValue();

                                    sourceParentCategory.getSubcategories().remove(sourceCategory);
                                    targetCategory.getSubcategories().add(sourceCategory);

                                    sourceParentNode.getChildren().remove(draggedCategoryNode);
                                    targetNode.getChildren().add(draggedCategoryNode);
                                    targetNode.setExpanded(true);

                                    targetNode.getChildren().sort((n1, n2) -> 
                                        n1.getValue().getName().compareToIgnoreCase(n2.getValue().getName())
                                    );

                                    tree.getSelectionModel().select(draggedCategoryNode);
                                    main.refreshQuestionTable();
                                    tree.refresh();
                                    
                                    success = true;
                                    draggedCategoryNode = null; 
                                }
                            }
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            ContextMenu categoryMenu = new ContextMenu();
            
            MenuItem addCatItem = new MenuItem("➕ Añadir Subcategoría");
            addCatItem.setOnAction(event -> {
                Category c = cell.getItem();
                if (c != null) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Añadir Subcategoría");
                    dialog.setHeaderText("Añadir dentro de: " + c.getName());
                    dialog.setContentText("Nombre:");
                    dialog.showAndWait().ifPresent(newName -> {
                        if (!newName.trim().isEmpty()) {
                            c.getSubcategories().add(new Category(newName.trim()));
                            main.applyCategoryFilter(main.getSearchCategoryField().getText());
                        }
                    });
                }
            });

            MenuItem editCatItem = new MenuItem("✏️ Editar Nombre");
            editCatItem.setOnAction(event -> {
                Category c = cell.getItem();
                if (c != null) {
                    TextInputDialog dialog = new TextInputDialog(c.getName());
                    dialog.setTitle("Editar Categoría");
                    dialog.setHeaderText("Nuevo nombre:");
                    dialog.showAndWait().ifPresent(newName -> {
                        if (!newName.trim().isEmpty()) {
                            c.setName(newName.trim());
                            tree.refresh(); 
                        }
                    });
                }
            });

            MenuItem deleteCatItem = new MenuItem("📂 Eliminar (Mover contenido al padre)");
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = cell.getTreeItem();
                if (treeItemToDelete != null) {
                    TreeItem<Category> parentItem = treeItemToDelete.getParent();
                    
                    if (parentItem != null) { 
                        Category catToDelete = treeItemToDelete.getValue();
                        Category parentCat = parentItem.getValue();
                        
                        parentCat.getQuestions().addAll(catToDelete.getQuestions());
                        parentCat.getSubcategories().addAll(catToDelete.getSubcategories());
                        parentCat.getSubcategories().remove(catToDelete); 
                        
                        main.applyCategoryFilter(main.getSearchCategoryField().getText());
                    } else {
                        new Alert(Alert.AlertType.WARNING, "No se puede eliminar la categoría principal.").show();
                    }
                }
            });
            
            categoryMenu.getItems().addAll(addCatItem, editCatItem, deleteCatItem);
            cell.contextMenuProperty().bind(
                Bindings.when(cell.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(categoryMenu)
            );
            
            return cell;
        });
    }

    private static boolean isValidDropTarget(TreeItem<Category> source, TreeItem<Category> target) {
        if (source == null || target == null || source == target) return false;
        if (source.getParent() == target) return false;
        
        TreeItem<Category> temp = target;
        while (temp != null) {
            if (temp == source) return false; 
            temp = temp.getParent();
        }
        return true; 
    }
}