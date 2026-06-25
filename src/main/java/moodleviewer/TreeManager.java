/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

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
import moodleviewer.util.I18n; 
import moodleviewer.util.DragAndDropConstants; 
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import moodleviewer.commands.Command;
import moodleviewer.commands.CommandManager;
import moodleviewer.commands.MoveCategoryCommand;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import java.util.Optional;

public class TreeManager {

    private static TreeItem<Category> draggedCategoryNode = null;

    public static void configure(TreeView<Category> tree) {
        tree.setCellFactory(tv -> new CategoryTreeCell());

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                EventBus.getInstance().publish(new AppEvents.CategorySelectedEvent(newVal.getValue()));
            }
        });
    }
    
    private static void expandAll(TreeItem<?> item, boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandAll(child, expand);
            }
        }
    }
    
    private static int countTotalQuestions(Category category) {
        if (category == null) return 0;
        
        int count = category.getQuestions().size();
        for (Category subcategory : category.getSubcategories()) {
            count += countTotalQuestions(subcategory);
        }
        return count;
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
    
    private static class CategoryTreeCell extends TreeCell<Category> {

        public CategoryTreeCell() {
            configurarEventos();
            configurarMenuContextual();
        }

        @Override
        protected void updateItem(Category item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                int directQuestions = item.getQuestions().size();
                int totalQuestions = countTotalQuestions(item);
                setText(item.getName() + " (" + totalQuestions + " | " + directQuestions + ")");
            }
        }

        private void configurarEventos() {
            this.setOnDragDetected(event -> {
                if (!this.isEmpty() && this.getTreeItem().getParent() != null) {
                    draggedCategoryNode = this.getTreeItem();
                    Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(DragAndDropConstants.MOVE_CATEGORY);
                    db.setContent(content);
                    
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    db.setDragView(this.snapshot(params, null));
                    event.consume();
                }
            });

            this.setOnDragOver(event -> {
                if (!this.isEmpty() && event.getDragboard().hasString()) {
                    String dragType = event.getDragboard().getString();
                    TreeItem<Category> targetItem = this.getTreeItem();

                    if (DragAndDropConstants.MOVE_QUESTIONS.equals(dragType)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    } else if (DragAndDropConstants.MOVE_CATEGORY.equals(dragType) && isValidDropTarget(draggedCategoryNode, targetItem)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            this.setOnDragDropped(event -> {
                boolean success = false;
                if (!this.isEmpty() && event.getDragboard().hasString()) {
                    String dragType = event.getDragboard().getString();
                    Category destCategory = this.getItem();

                    if (DragAndDropConstants.MOVE_QUESTIONS.equals(dragType)) {
                        EventBus.getInstance().publish(new AppEvents.MoveQuestionsEvent(destCategory));
                        success = true;
                        
                    } else if (DragAndDropConstants.MOVE_CATEGORY.equals(dragType) && draggedCategoryNode != null) {
                        Category sourceCategory = draggedCategoryNode.getValue();
                        TreeItem<Category> sourceParentItem = draggedCategoryNode.getParent();
                        Category sourceParentCategory = sourceParentItem.getValue();

                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle(I18n.get("tree.dlg.move.title"));
                            alert.setHeaderText(I18n.get("tree.dlg.move.header", sourceCategory.getName(), destCategory.getName()));
                            
                            ButtonType btnSub = new ButtonType(I18n.get("tree.dlg.move.btn.sub"));
                            ButtonType btnCombine = new ButtonType(I18n.get("tree.dlg.move.btn.combine"));
                            ButtonType btnCancel = new ButtonType(I18n.get("tree.dlg.move.btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
                            
                            alert.getButtonTypes().setAll(btnSub, btnCombine, btnCancel);
                            
                            Optional<ButtonType> result = alert.showAndWait();
                            
                            if (result.isPresent() && result.get() != btnCancel) {
                                if (result.get() == btnSub) {
                                    // PATRÓN COMMAND: Encapsulamos el movimiento de categoría
                                    Command moveCmd = new MoveCategoryCommand(sourceCategory, sourceParentCategory, destCategory);
                                    CommandManager.getInstance().executeCommand(moveCmd);
                                } else if (result.get() == btnCombine) {
                                    destCategory.getQuestions().addAll(sourceCategory.getQuestions());
                                    sourceParentCategory.getSubcategories().remove(sourceCategory);
                                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                                }
                            }
                        });
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                draggedCategoryNode = null;
            });

            this.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.isControlDown()) {
                    TreeItem<Category> clickedItem = this.getTreeItem();
                    if (clickedItem != null && !this.isEmpty()) {
                        boolean newState = !clickedItem.isExpanded();
                        expandAll(clickedItem, newState);
                        event.consume();
                    }
                }
            });
        }

        private void configurarMenuContextual() {
            ContextMenu categoryMenu = new ContextMenu();
            
            MenuItem addCatItem = new MenuItem(I18n.get("tree.ctx.addSub"));
            addCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 13, "#495057"));
            addCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle(I18n.get("tree.dlg.add.title"));
                dialog.setHeaderText(I18n.get("tree.dlg.add.header", this.getItem().getName()));
                dialog.setContentText(I18n.get("tree.dlg.add.content"));
                dialog.showAndWait().ifPresent(name -> {
                    this.getItem().addSubcategory(new Category(name));
                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                });
            });

            MenuItem editCatItem = new MenuItem(I18n.get("tree.ctx.rename"));
            editCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.EDIT, 13, "#495057"));
            editCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(this.getItem().getName());
                dialog.setTitle(I18n.get("tree.ctx.rename"));
                dialog.setHeaderText(I18n.get("tree.dlg.rename.header"));
                dialog.showAndWait().ifPresent(newName -> {
                    this.getItem().setName(newName);
                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                });
            });

            MenuItem deleteCatItem = new MenuItem(I18n.get("tree.ctx.delete"));
            deleteCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.TRASH_ALT, 13, "#c0392b"));
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = this.getTreeItem();
                TreeItem<Category> parentItem = treeItemToDelete.getParent();

                if (parentItem != null && parentItem.getParent() != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle(I18n.get("tree.dlg.del.title"));
                    confirm.setHeaderText(I18n.get("tree.dlg.del.header", treeItemToDelete.getValue().getName()));
                    confirm.setContentText(I18n.get("tree.dlg.del.content", parentItem.getValue().getName()));

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            Category catToDelete = treeItemToDelete.getValue();
                            Category parentCat = parentItem.getValue();
                            parentCat.getQuestions().addAll(catToDelete.getQuestions());
                            parentCat.getSubcategories().addAll(catToDelete.getSubcategories());
                            parentCat.getSubcategories().remove(catToDelete);
                            
                            EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                        }
                    });

                } else {
                    new Alert(Alert.AlertType.WARNING, I18n.get("tree.alert.delRoot")).show();
                }
            });
            
            categoryMenu.getItems().addAll(addCatItem, editCatItem, deleteCatItem);
            this.contextMenuProperty().bind(Bindings.when(this.emptyProperty()).then((ContextMenu) null).otherwise(categoryMenu));
        }
    }
}