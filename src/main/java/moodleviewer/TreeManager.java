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
import moodleviewer.commands.MergeIntoCategoryCommand;
import moodleviewer.commands.RenameCategoryCommand;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.Optional;

/**
 * Gestiona el comportamiento del árbol de categorías.
 *
 * PUNTO I: convertido de clase con métodos y estado estáticos a clase instanciable.
 * Esto elimina el acoplamiento global, permite testing unitario y hace posible tener
 * en el futuro más de una instancia del árbol sin conflictos de estado compartido.
 *
 * PUNTO E: renombrar categorías y fusionar al mover ahora pasan por CommandManager,
 * por lo que ambas operaciones son reversibles con Ctrl+Z / Ctrl+Y.
 */
public class TreeManager {

    /** Nodo que se está arrastrando; estado de instancia en lugar de estático. */
    private TreeItem<Category> draggedCategoryNode = null;

    /** Referencia al árbol gestionado por esta instancia. */
    private TreeView<Category> tree;

    /**
     * Vincula esta instancia al árbol concreto y registra todos los listeners necesarios.
     * Sustituye al antiguo método estático {@code configure(TreeView)}.
     *
     * @param tree árbol de categorías sobre el que actúa este gestor.
     */
    public void configure(TreeView<Category> tree) {
        this.tree = tree;
        tree.setCellFactory(tv -> new CategoryTreeCell());

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                EventBus.getInstance().publish(new AppEvents.CategorySelectedEvent(newVal.getValue()));
            }
        });
    }

    // -------------------------------------------------------------------------
    //  Utilidades de árbol
    // -------------------------------------------------------------------------

    private void expandAll(TreeItem<?> item, boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandAll(child, expand);
            }
        }
    }

    private int countTotalQuestions(Category category) {
        if (category == null) return 0;
        int count = category.getQuestions().size();
        for (Category sub : category.getSubcategories()) {
            count += countTotalQuestions(sub);
        }
        return count;
    }

    private boolean isValidDropTarget(TreeItem<Category> source, TreeItem<Category> target) {
        if (source == null || target == null || source == target) return false;
        if (source.getParent() == target) return false;
        TreeItem<Category> temp = target;
        while (temp != null) {
            if (temp == source) return false;
            temp = temp.getParent();
        }
        return true;
    }

    // -------------------------------------------------------------------------
    //  Celda personalizada (clase interna no estática → accede a TreeManager.this)
    // -------------------------------------------------------------------------

    private class CategoryTreeCell extends TreeCell<Category> {

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
                int totalQuestions  = countTotalQuestions(item);
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
                    } else if (DragAndDropConstants.MOVE_CATEGORY.equals(dragType)
                            && isValidDropTarget(draggedCategoryNode, targetItem)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            this.setOnDragDropped(event -> {
                boolean success = false;
                if (!this.isEmpty() && event.getDragboard().hasString()) {
                    String dragType   = event.getDragboard().getString();
                    Category destCategory = this.getItem();

                    if (DragAndDropConstants.MOVE_QUESTIONS.equals(dragType)) {
                        EventBus.getInstance().publish(new AppEvents.MoveQuestionsEvent(destCategory));
                        success = true;

                    } else if (DragAndDropConstants.MOVE_CATEGORY.equals(dragType)
                            && draggedCategoryNode != null) {

                        Category sourceCategory   = draggedCategoryNode.getValue();
                        Category sourceParentCat  = draggedCategoryNode.getParent().getValue();

                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle(I18n.get("tree.dlg.move.title"));
                            alert.setHeaderText(I18n.get("tree.dlg.move.header",
                                    sourceCategory.getName(), destCategory.getName()));

                            ButtonType btnSub     = new ButtonType(I18n.get("tree.dlg.move.btn.sub"));
                            ButtonType btnCombine = new ButtonType(I18n.get("tree.dlg.move.btn.combine"));
                            ButtonType btnCancel  = new ButtonType(I18n.get("tree.dlg.move.btn.cancel"),
                                    ButtonBar.ButtonData.CANCEL_CLOSE);
                            alert.getButtonTypes().setAll(btnSub, btnCombine, btnCancel);

                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() != btnCancel) {
                                if (result.get() == btnSub) {
                                    // Mover como subcategoría → ya era reversible
                                    Command moveCmd = new MoveCategoryCommand(
                                            sourceCategory, sourceParentCat, destCategory);
                                    CommandManager.getInstance().executeCommand(moveCmd);

                                } else if (result.get() == btnCombine) {
                                    // PUNTO E: fusión ahora también es reversible
                                    Command mergeCmd = new MergeIntoCategoryCommand(
                                            sourceCategory, sourceParentCat, destCategory);
                                    CommandManager.getInstance().executeCommand(mergeCmd);
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
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && event.isControlDown()) {
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

            // --- Añadir subcategoría ---
            MenuItem addCatItem = new MenuItem(I18n.get("tree.ctx.addSub"));
            addCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 13, "#495057"));
            addCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle(I18n.get("tree.dlg.add.title"));
                dialog.setHeaderText(I18n.get("tree.dlg.add.header", this.getItem().getName()));
                dialog.setContentText(I18n.get("tree.dlg.add.content"));
                dialog.showAndWait().ifPresent(name -> {
                    this.getItem().addSubcategory(new Category(name));
                    CommandManager.getInstance().markAsDirty();
                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                });
            });

            // --- Renombrar (PUNTO E: ahora usa RenameCategoryCommand → reversible) ---
            MenuItem editCatItem = new MenuItem(I18n.get("tree.ctx.rename"));
            editCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.EDIT, 13, "#495057"));
            editCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(this.getItem().getName());
                dialog.setTitle(I18n.get("tree.ctx.rename"));
                dialog.setHeaderText(I18n.get("tree.dlg.rename.header"));
                dialog.showAndWait().ifPresent(newName -> {
                    if (!newName.isBlank() && !newName.equals(this.getItem().getName())) {
                        Command renameCmd = new RenameCategoryCommand(this.getItem(), newName);
                        CommandManager.getInstance().executeCommand(renameCmd);
                    }
                });
            });

            // --- Eliminar categoría ---
            MenuItem deleteCatItem = new MenuItem(I18n.get("tree.ctx.delete"));
            deleteCatItem.setGraphic(IconFactory.of(FontAwesomeSolid.TRASH_ALT, 13, "#c0392b"));
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = this.getTreeItem();
                TreeItem<Category> parentItem       = treeItemToDelete.getParent();

                if (parentItem != null && parentItem.getParent() != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle(I18n.get("tree.dlg.del.title"));
                    confirm.setHeaderText(I18n.get("tree.dlg.del.header",
                            treeItemToDelete.getValue().getName()));
                    confirm.setContentText(I18n.get("tree.dlg.del.content",
                            parentItem.getValue().getName()));

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            Category catToDelete = treeItemToDelete.getValue();
                            Category parentCat   = parentItem.getValue();
                            parentCat.getQuestions().addAll(catToDelete.getQuestions());
                            parentCat.getSubcategories().addAll(catToDelete.getSubcategories());
                            parentCat.getSubcategories().remove(catToDelete);
                            CommandManager.getInstance().markAsDirty();
                            EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                        }
                    });
                } else {
                    new Alert(Alert.AlertType.WARNING, I18n.get("tree.alert.delRoot")).show();
                }
            });

            categoryMenu.getItems().addAll(addCatItem, editCatItem, deleteCatItem);
            this.contextMenuProperty().bind(
                    Bindings.when(this.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(categoryMenu));
        }
    }
}