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

import java.util.Optional;

public class TreeManager {

    private static TreeItem<Category> draggedCategoryNode = null;

    public static void configure(Main main) {
        TreeView<Category> tree = main.getCategoryTreeView();

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

            // 1. INICIAR EL ARRASTRE DE CATEGORÍA
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty() && cell.getItem() != null) {
                    TreeItem<Category> draggedNode = cell.getTreeItem();
                    
                    if (draggedNode.getParent() != null) {
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

            // 2. PERMITIR SOLTAR (SOLO ACEPTA OTRAS CATEGORÍAS)
            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && !cell.isEmpty()) {
                    String dragType = db.getString();
                    TreeItem<Category> targetNode = cell.getTreeItem();

                    if ("MOVER_CATEGORIA".equals(dragType)) {
                        if (isValidDropTarget(draggedCategoryNode, targetNode)) {
                            event.acceptTransferModes(TransferMode.MOVE);
                        }
                    }
                }
                event.consume();
            });

            // 3. SOLTAR (PREGUNTAR: ¿SUBCATEGORÍA O COMBINAR?)
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                
                if (db.hasString() && !cell.isEmpty() && "MOVER_CATEGORIA".equals(db.getString())) {
                    TreeItem<Category> targetNode = cell.getTreeItem();

                    if (draggedCategoryNode != null && isValidDropTarget(draggedCategoryNode, targetNode)) {
                        // Guardamos las referencias antes de que termine el evento
                        TreeItem<Category> sourceNode = draggedCategoryNode;
                        TreeItem<Category> destNode = targetNode;

                        // Le decimos a JavaFX que el arrastre ha terminado para que no se congele el ratón
                        event.setDropCompleted(true);
                        event.consume();

                        // Lanzamos la pregunta un milisegundo después en el hilo de la interfaz
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Mover Categoría");
                            alert.setHeaderText("Has arrastrado '" + sourceNode.getValue().getName() + "' sobre '" + destNode.getValue().getName() + "'");
                            alert.setContentText("¿Qué deseas hacer con el contenido?");

                            ButtonType btnSub = new ButtonType("Convertir en Subcategoría");
                            ButtonType btnCombine = new ButtonType("Combinar (Fusionar)");
                            ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

                            alert.getButtonTypes().setAll(btnSub, btnCombine, btnCancel);

                            Optional<ButtonType> result = alert.showAndWait();
                            
                            if (result.isPresent() && result.get() != btnCancel) {
                                Category sourceCategory = sourceNode.getValue();
                                Category targetCategory = destNode.getValue();
                                Category sourceParentCategory = sourceNode.getParent().getValue();

                                if (result.get() == btnSub) {
                                    // OPCIÓN A: COMPORTAMIENTO NORMAL (Convertir en Subcategoría)
                                    sourceParentCategory.getSubcategories().remove(sourceCategory);
                                    targetCategory.getSubcategories().add(sourceCategory);

                                    sourceNode.getParent().getChildren().remove(sourceNode);
                                    destNode.getChildren().add(sourceNode);
                                    destNode.setExpanded(true);
                                    destNode.getChildren().sort((n1, n2) -> n1.getValue().getName().compareToIgnoreCase(n2.getValue().getName()));

                                    tree.getSelectionModel().select(sourceNode);
                                    
                                } else if (result.get() == btnCombine) {
                                    // OPCIÓN B: NUEVO COMPORTAMIENTO (Fusionar carpetas)
                                    // 1. Pasamos todas las preguntas
                                    targetCategory.getQuestions().addAll(sourceCategory.getQuestions());
                                    // 2. Pasamos todas las subcategorías
                                    targetCategory.getSubcategories().addAll(sourceCategory.getSubcategories());
                                    // 3. Eliminamos la carpeta origen vacía
                                    sourceParentCategory.getSubcategories().remove(sourceCategory);

                                    // Refrescamos el árbol entero para que se dibuje la fusión correctamente
                                    main.applyCategoryFilter(main.getSearchCategoryField().getText());
                                    tree.getSelectionModel().select(destNode);
                                }
                                
                                main.refreshQuestionTable();
                                tree.refresh();
                            }
                            draggedCategoryNode = null; 
                        });
                        return; // Salimos de la función aquí
                    }
                }
                event.setDropCompleted(false);
                event.consume();
            });

            // MENÚ CONTEXTUAL
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
                        c.setName(newName);
                        tree.refresh(); 
                    });
                }
            });

            MenuItem deleteCatItem = new MenuItem("📂 Eliminar");
            deleteCatItem.setOnAction(event -> {
                TreeItem<Category> treeItemToDelete = cell.getTreeItem();
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