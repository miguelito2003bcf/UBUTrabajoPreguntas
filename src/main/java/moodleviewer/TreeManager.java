package moodleviewer;

import javafx.application.Platform;
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
import java.util.Optional;

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
                if (!cell.isEmpty() && cell.getTreeItem().getParent() != null) {
                    draggedCategoryNode = cell.getTreeItem();
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("MOVER_CATEGORIA");
                    db.setContent(content);
                    
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    db.setDragView(cell.snapshot(params, null));
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (!cell.isEmpty() && event.getDragboard().hasString()) {
                    String dragType = event.getDragboard().getString();
                    TreeItem<Category> targetItem = cell.getTreeItem();

                    if ("MOVER_PREGUNTAS".equals(dragType)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    } else if ("MOVER_CATEGORIA".equals(dragType) && isValidDropTarget(draggedCategoryNode, targetItem)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                boolean success = false;
                if (!cell.isEmpty() && event.getDragboard().hasString()) {
                    String dragType = event.getDragboard().getString();
                    Category destCategory = cell.getItem();

                    if ("MOVER_PREGUNTAS".equals(dragType)) {
                        List<Question> draggedQuestions = new ArrayList<>(table.getSelectionModel().getSelectedItems());
                        TreeItem<Category> sourceCategoryItem = tree.getSelectionModel().getSelectedItem();
                        
                        if (sourceCategoryItem != null && sourceCategoryItem.getValue() != destCategory) {
                            sourceCategoryItem.getValue().getQuestions().removeAll(draggedQuestions);
                            destCategory.getQuestions().addAll(draggedQuestions);
                            
                            main.refreshQuestionTable();
                            tree.refresh();
                            success = true;
                        }
                    } else if ("MOVER_CATEGORIA".equals(dragType) && draggedCategoryNode != null) {
                        Category sourceCategory = draggedCategoryNode.getValue();
                        TreeItem<Category> sourceParentItem = draggedCategoryNode.getParent();
                        Category sourceParentCategory = sourceParentItem.getValue();

                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Mover Categoría");
                            alert.setHeaderText("Mover '" + sourceCategory.getName() + "' a '" + destCategory.getName() + "'");
                            
                            ButtonType btnSub = new ButtonType("Como subcategoría");
                            ButtonType btnCombine = new ButtonType("Fusionar preguntas");
                            ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                            
                            alert.getButtonTypes().setAll(btnSub, btnCombine, btnCancel);
                            
                            Optional<ButtonType> result = alert.showAndWait();
                            
                            if (result.isPresent() && result.get() != btnCancel) {
                                if (result.get() == btnSub) {
                                    sourceParentCategory.getSubcategories().remove(sourceCategory);
                                    destCategory.getSubcategories().add(sourceCategory);
                                } else if (result.get() == btnCombine) {
                                    destCategory.getQuestions().addAll(sourceCategory.getQuestions());
                                    sourceParentCategory.getSubcategories().remove(sourceCategory);
                                }
                                main.applyCategoryFilter(main.getSearchCategoryField().getText());
                            }
                        });
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                draggedCategoryNode = null;
            });

            ContextMenu categoryMenu = new ContextMenu();
            MenuItem addCatItem = new MenuItem("➕ Añadir Subcategoría");
            addCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Nueva Categoría");
                dialog.setHeaderText("Crear dentro de: " + cell.getItem().getName());
                dialog.setContentText("Nombre:");
                dialog.showAndWait().ifPresent(name -> {
                    cell.getItem().addSubcategory(new Category(name));
                    main.applyCategoryFilter(main.getSearchCategoryField().getText());
                });
            });

            MenuItem editCatItem = new MenuItem("✏️ Renombrar");
            editCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(cell.getItem().getName());
                dialog.setTitle("Renombrar");
                dialog.setHeaderText("Nuevo nombre:");
                dialog.showAndWait().ifPresent(newName -> {
                    cell.getItem().setName(newName);
                    tree.refresh();
                });
            });

            MenuItem deleteCatItem = new MenuItem("🗑️ Eliminar Categoría");
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = cell.getTreeItem();
                TreeItem<Category> parentItem = treeItemToDelete.getParent();

                if (parentItem != null && parentItem.getParent() != null) { 
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmar Eliminación");
                    confirm.setHeaderText("¿Estás seguro de que deseas eliminar la categoría '" + treeItemToDelete.getValue().getName() + "'?");
                    confirm.setContentText("Sus preguntas y subcategorías se moverán a la categoría padre ('" + parentItem.getValue().getName() + "').");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            Category catToDelete = treeItemToDelete.getValue();
                            Category parentCat = parentItem.getValue();
                            parentCat.getQuestions().addAll(catToDelete.getQuestions());
                            parentCat.getSubcategories().addAll(catToDelete.getSubcategories());
                            parentCat.getSubcategories().remove(catToDelete); 
                            
                            main.applyCategoryFilter(main.getSearchCategoryField().getText());
                        }
                    });

                } else {
                    new Alert(Alert.AlertType.WARNING, "No se puede eliminar la categoría principal.").show();
                }
            });
            
            categoryMenu.getItems().addAll(addCatItem, editCatItem, deleteCatItem);
            cell.contextMenuProperty().bind(Bindings.when(cell.emptyProperty()).then((ContextMenu) null).otherwise(categoryMenu));
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