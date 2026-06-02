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

/**
 * Clase creada para configurar el comportamiento del árbol de categorías.
 */
public class TreeManager {

    private static TreeItem<Category> draggedCategoryNode = null;

    /**
     * Aplica la configuración completa al árbol de categorías de la aplicación.
     * 
     * @param main instancia de main que proporciona el árbol, la tabla y los métodos de refresco.
     */
    public static void configure(Main main) {
        TreeView<Category> tree = main.getCategoryTreeView();
        tree.setCellFactory(tv -> new CategoryTreeCell(main));
    }
    
    /**
     * Expande o contrae recursivamente un TreeItem y todos sus descendientes.
     * 
     * @param item el nodo a expandir/contraer.
     * @param expand true para expandir o false para contraer.
     */
    private static void expandAll(TreeItem<?> item, boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandAll(child, expand);
            }
        }
    }
    
    /**
     * Cuenta de forma recursiva el número total de preguntas que hay en una categoría y en todas
     * sus subcategorías descendientes.
     * 
     * @param category categoría base desde la que empezar a contar.
     * @return número total de preguntas en la rama.
     */
    private static int countTotalQuestions(Category category) {
        if (category == null) return 0;
        
        int count = category.getQuestions().size();
        for (Category subcategory : category.getSubcategories()) {
            count += countTotalQuestions(subcategory);
        }
        return count;
    }

    /**
     * Comprueba si un destino es válido para el drop de una categoría arrastrada. Un destino es inválido
     * si coincide con el origen, es su padre actual o es un descendiente de él.
     * 
     * @param source nodo que se está arrastrando.
     * @param target nodo sobre el que se pretende soltar.
     * @return true si el drop es válido, o false en caso contrario.
     */
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
    
    /**
     * Clase interna que define cómo se visualiza y cómo se comporta cada celda del árbol.
     */
    private static class CategoryTreeCell extends TreeCell<Category> {
    	
        private Main main;
        private TreeView<Category> tree;
        private TableView<Question> table;

        /**
         * Construye una nueva celda para el árbol de categorías.
         * 
         * @param main instancia para acceder a los componentes globales.
         */
        public CategoryTreeCell(Main main) {
            this.main = main;
            this.tree = main.getCategoryTreeView();
            this.table = main.getQuestionTableView();
            
            configurarEventos();
            configurarMenuContextual();
        }

        /**
         * Actualiza el contenido visual de la celda cada vez que JavaFX la redibuja.
         * Muestra el nombre de la categoría junto con el conteo de preguntas.
         * 
         * @param item la categoría asociada a esta celda.
         * @param empty indica si la celda está vacía.
         */
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

        /**
         * Configura los manejadores de eventos interactivos para la celda. Esto incluye el inicio y
         * recepción de operaciones Drag & Drop para mover preguntas y categorías, junto con el atajo para
         * contraer o expandir la rama completa.
         */
        private void configurarEventos() {
            this.setOnDragDetected(event -> {
                if (!this.isEmpty() && this.getTreeItem().getParent() != null) {
                    draggedCategoryNode = this.getTreeItem();
                    Dragboard db = this.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("MOVER_CATEGORIA");
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

                    if ("MOVER_PREGUNTAS".equals(dragType)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    } else if ("MOVER_CATEGORIA".equals(dragType) && isValidDropTarget(draggedCategoryNode, targetItem)) {
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

        /**
         * Configura y asocia un menú contextual a la celda. Proporciona las opciones de añadir una subcategoría,
         * renombrar la categoría actual o eliminarla, gestionando la reubicación de sus preguntas si fuese necesario.
         */
        private void configurarMenuContextual() {
            ContextMenu categoryMenu = new ContextMenu();
            
            MenuItem addCatItem = new MenuItem("➕ Añadir Subcategoría");
            addCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Nueva Categoría");
                dialog.setHeaderText("Crear dentro de: " + this.getItem().getName());
                dialog.setContentText("Nombre:");
                dialog.showAndWait().ifPresent(name -> {
                    this.getItem().addSubcategory(new Category(name));
                    main.applyCategoryFilter(main.getSearchCategoryField().getText());
                });
            });

            MenuItem editCatItem = new MenuItem("Renombrar");
            editCatItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(this.getItem().getName());
                dialog.setTitle("Renombrar");
                dialog.setHeaderText("Nuevo nombre:");
                dialog.showAndWait().ifPresent(newName -> {
                    this.getItem().setName(newName);
                    tree.refresh();
                });
            });

            MenuItem deleteCatItem = new MenuItem("Eliminar Categoría");
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = this.getTreeItem();
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
            this.contextMenuProperty().bind(Bindings.when(this.emptyProperty()).then((ContextMenu) null).otherwise(categoryMenu));
        }
    }
}