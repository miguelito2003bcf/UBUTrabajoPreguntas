/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import moodleviewer.model.Category;
import moodleviewer.util.CategoryFilterBuilder;
import moodleviewer.util.CategorySelection;
import moodleviewer.util.I18n;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.Optional;

/**
 * Diálogo que se muestra antes de exportar (a XML o a LaTeX), preguntando al usuario si desea
 * exportar el banco de preguntas completo o únicamente una selección de categorías concretas.
 *
 * Si elige selección parcial, se presenta un árbol con dos controles independientes por categoría:
 * un checkbox para marcarla como incluida, y, si tiene subcategorías, un segundo checkbox para
 * decidir si también se incluyen estas.
 */
public class ExportScopeDialog {

    /**
     * Solicita al usuario el alcance de la exportación.
     *
     * @param rootCategory categoría raíz completa del banco de preguntas actual.
     * @param preselected   categoría ya seleccionada en el árbol principal (puede ser null),
     *                      usada como punto de partida premarcado en el árbol de checkboxes.
     * @return un Optional con la categoría raíz a exportar (la original si se elige "todo",
     *         o una nueva raíz filtrada si se elige selección parcial), o vacío si el usuario cancela
     *         o si su selección no contiene ningún contenido exportable.
     */
    public static Optional<Category> askExportScope(Category rootCategory, Category preselected) {

        Dialog<ButtonType> scopeDialog = new Dialog<>();
        scopeDialog.setTitle(I18n.get("export.scope.title"));
        scopeDialog.setHeaderText(I18n.get("export.scope.header"));

        ButtonType btnAll = new ButtonType(I18n.get("export.scope.btnAll"), ButtonBar.ButtonData.OK_DONE);
        ButtonType btnPartial = new ButtonType(I18n.get("export.scope.btnPartial"), ButtonBar.ButtonData.OTHER);
        ButtonType btnCancel = new ButtonType(I18n.get("export.scope.btnCancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        scopeDialog.getDialogPane().getButtonTypes().setAll(btnAll, btnPartial, btnCancel);

        Label infoLabel = new Label(I18n.get("export.scope.content"));
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(420);
        scopeDialog.getDialogPane().setContent(infoLabel);

        Optional<ButtonType> firstChoice = scopeDialog.showAndWait();
        if (firstChoice.isEmpty() || firstChoice.get() == btnCancel) {
            return Optional.empty();
        }
        if (firstChoice.get() == btnAll) {
            return Optional.of(rootCategory);
        }

        // --- Selección parcial: mostramos el árbol de checkboxes ---
        return askPartialSelection(rootCategory, preselected);
    }

    /**
     * Construye y muestra el árbol de checkboxes para elegir las categorías a exportar.
     *
     * @param rootCategory categoría raíz completa.
     * @param preselected   categoría a premarcar como punto de partida (puede ser null).
     * @return el árbol filtrado resultante, o vacío si se cancela o queda sin contenido.
     */
    private static Optional<Category> askPartialSelection(Category rootCategory, Category preselected) {
        CategorySelection selection = new CategorySelection();
        if (preselected != null) {
            selection.select(preselected, false);
        }

        TreeView<Category> checkTree = new TreeView<>();
        // Reutilizamos la construcción recursiva centralizada en TreeBuilder (mismo recorrido,
        // mismo orden alfabético de subcategorías) en lugar de duplicar esa lógica aquí; solo
        // necesitamos un TreeItem normal, ya que el estado de selección vive en CategorySelection
        // y se pinta en SelectableCategoryCell, no en el propio TreeItem.
        checkTree.setRoot(TreeBuilder.createTreeItem(rootCategory));
        checkTree.setShowRoot(false);
        checkTree.setCellFactory(tv -> new SelectableCategoryCell(selection, checkTree));
        checkTree.setPrefSize(420, 350);

        Dialog<ButtonType> selectionDialog = new Dialog<>();
        selectionDialog.setTitle(I18n.get("export.scope.title"));
        selectionDialog.setHeaderText(I18n.get("export.scope.partial.header"));
        selectionDialog.setResizable(true);

        ButtonType btnExport = new ButtonType(I18n.get("export.scope.btnExportSelection"), ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType(I18n.get("export.scope.btnCancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        selectionDialog.getDialogPane().getButtonTypes().setAll(btnExport, btnCancel);

        VBox content = new VBox(8);
        Label hint = new Label(I18n.get("export.scope.partial.hint"));
        hint.setWrapText(true);
        content.getChildren().addAll(hint, checkTree);
        selectionDialog.getDialogPane().setContent(content);

        Optional<ButtonType> choice = selectionDialog.showAndWait();
        if (choice.isEmpty() || choice.get() == btnCancel) {
            return Optional.empty();
        }

        if (selection.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18n.get("export.scope.partial.emptyWarning")).showAndWait();
            return Optional.empty();
        }

        Category filteredRoot = CategoryFilterBuilder.buildFilteredTree(rootCategory, selection);
        if (filteredRoot == null) {
            new Alert(Alert.AlertType.WARNING, I18n.get("export.scope.partial.emptyWarning")).showAndWait();
            return Optional.empty();
        }
        return Optional.of(filteredRoot);
    }

    /**
     * Celda de árbol que muestra el nombre de la categoría junto a dos checkboxes independientes:
     * uno para incluir la propia categoría, y otro (solo si tiene subcategorías) para incluir
     * también todas sus subcategorías.
     *
     * Si algún ancestro de esta categoría tiene activado "incluir subcategorías", esta celda se
     * muestra marcada y deshabilitada (heredada), ya que su inclusión depende del ancestro y no
     * tiene sentido editarla de forma independiente mientras dure esa herencia.
     */
    private static class SelectableCategoryCell extends TreeCell<Category> {

        private final CategorySelection selection;
        private final TreeView<Category> ownerTree;
        private final CheckBox includeBox = new CheckBox();
        private final CheckBox includeSubBox = new CheckBox();
        private final Label nameLabel = new Label();
        private final HBox container = new HBox(10);
        private boolean updatingProgrammatically = false;

        SelectableCategoryCell(CategorySelection selection, TreeView<Category> ownerTree) {
            this.selection = selection;
            this.ownerTree = ownerTree;

            includeSubBox.setGraphic(IconFactory.of(FontAwesomeSolid.SITEMAP, 12, "#868e96"));
            includeSubBox.setText(I18n.get("export.scope.partial.includeSub"));
            includeSubBox.setStyle("-fx-font-size: 11px; -fx-text-fill: #868e96;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(includeBox, nameLabel, spacer, includeSubBox);

            // Estos listeners solo deben reaccionar a clics reales del usuario sobre el checkbox,
            // nunca a los cambios de estado que updateItem() aplica al reciclar la celda durante
            // el scroll (de lo contrario se sobrescribiría la selección de otras categorías).
            includeBox.selectedProperty().addListener((obs, oldVal, isSelected) -> {
                if (updatingProgrammatically) return;
                Category category = getItem();
                if (category == null) return;
                if (isSelected) {
                    selection.select(category, includeSubBox.isSelected());
                } else {
                    selection.deselect(category);
                }
                // Si esta categoría tiene subcategorías, marcarla o desmarcarla puede cambiar
                // lo que ven sus hijas (al activarla queda disponible "incluir subcategorías";
                // al desactivarla se pierde cualquier herencia que estuviera dando a sus hijas).
                if (!category.getSubcategories().isEmpty()) {
                    ownerTree.refresh();
                }
            });

            includeSubBox.selectedProperty().addListener((obs, oldVal, isSelected) -> {
                if (updatingProgrammatically) return;
                Category category = getItem();
                if (category == null || !includeBox.isSelected()) return;
                selection.select(category, isSelected);
                // El efecto en cascada (marcar/desmarcar visualmente toda la subrama) afecta a celdas
                // distintas de esta; forzamos un refresco completo del árbol para que se vea al instante.
                ownerTree.refresh();
            });
        }

        /**
         * Indica si algún ancestro de este nodo del árbol tiene activado "incluir subcategorías",
         * en cuyo caso la inclusión de este nodo viene heredada y no debe controlarse manualmente.
         */
        private boolean isInheritedFromAncestor() {
            TreeItem<Category> parent = getTreeItem() != null ? getTreeItem().getParent() : null;
            while (parent != null) {
                Category parentCategory = parent.getValue();
                if (parentCategory != null && selection.isSelected(parentCategory) && selection.includesSubcategories(parentCategory)) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }

        @Override
        protected void updateItem(Category category, boolean empty) {
            super.updateItem(category, empty);
            if (empty || category == null) {
                setGraphic(null);
                return;
            }

            updatingProgrammatically = true;

            nameLabel.setText(category.getName());

            boolean inherited = isInheritedFromAncestor();
            includeBox.setSelected(inherited || selection.isSelected(category));
            includeBox.setDisable(inherited);

            boolean hasSubcategories = !category.getSubcategories().isEmpty();
            includeSubBox.setVisible(hasSubcategories);
            includeSubBox.setManaged(hasSubcategories);
            // Si viene heredada, "incluir subcategorías" también se hereda (todo lo de abajo entra igual).
            includeSubBox.setSelected(inherited || selection.includesSubcategories(category));
            includeSubBox.setDisable(inherited || !includeBox.isSelected());

            updatingProgrammatically = false;

            setGraphic(container);
        }
    }
}