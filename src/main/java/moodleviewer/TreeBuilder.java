package moodleviewer;

import javafx.scene.control.TreeItem;
import moodleviewer.model.Category;
import java.util.ArrayList;
import java.util.List;

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
        TreeItem<Category> item = new TreeItem<>(category);
        
        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            item.getChildren().add(createTreeItem(sub));
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