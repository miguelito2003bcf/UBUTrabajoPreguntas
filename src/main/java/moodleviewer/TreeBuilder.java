/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.scene.control.TreeItem;
import moodleviewer.model.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Clase creada como fábrica de nodos TreeItem para el árbol de categorías.
 * Las subcategorías se ordenan alfabéticamente.
 */
public class TreeBuilder {

	/**
	 * Construye recursivamente el árbol completo a partir de la jerarquía de la categoría.
	 * 
	 * @param category categoría raíz a partir de la cual construir el árbol
	 * @return nodo con todos sus descendientes.
	 */
    public static TreeItem<Category> createTreeItem(Category category) {
        return createTreeItem(category, TreeItem::new);
    }

    /**
     * Construye recursivamente el árbol completo a partir de la jerarquía de la categoría,
     * permitiendo personalizar cómo se crea cada nodo individual (por ejemplo, para usar una
     * subclase de TreeItem con datos adicionales, como hace ExportScopeDialog con su árbol
     * de selección por checkboxes). El recorrido, el orden alfabético de subcategorías y la
     * expansión de los nodos se mantienen centralizados aquí para no duplicar esa lógica en
     * cada lugar que necesite construir un árbol de categorías.
     *
     * @param category categoría raíz a partir de la cual construir el árbol.
     * @param nodeFactory función que crea el TreeItem concreto a partir de una categoría.
     * @return nodo con todos sus descendientes, creados mediante nodeFactory.
     */
    public static TreeItem<Category> createTreeItem(Category category, Function<Category, TreeItem<Category>> nodeFactory) {
        TreeItem<Category> item = nodeFactory.apply(category);

        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            item.getChildren().add(createTreeItem(sub, nodeFactory));
        }
        item.setExpanded(true);
        return item;
    }

    /**
     * Construye recursivamente un árbol filtrado que solo incluye las categorías cuyo nombre contiene
     * el texto introducido en el buscador.
     * 
     * @param category categoría a evaluar.
     * @param searchText texto de búsqueda.
     * @return nodo con los descendientes que coinciden, o null si ninguno coincide.
     */
    public static TreeItem<Category> createFilteredTreeItem(Category category, String searchText) {
        boolean matches = category.getName().toLowerCase().contains(searchText);
        TreeItem<Category> item = new TreeItem<>(category);
        boolean hasMatchingChild = false;

        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            TreeItem<Category> filteredSub = createFilteredTreeItem(sub, searchText);
            if (filteredSub != null) {
                item.getChildren().add(filteredSub);
                hasMatchingChild = true;
            }
        }

        if (matches || hasMatchingChild) {
            item.setExpanded(true); 
            return item;
        }
        return null;
    }
}