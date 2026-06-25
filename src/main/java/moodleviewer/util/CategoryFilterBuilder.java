/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.util;

import moodleviewer.model.Category;

/**
 * Construye un árbol {@link Category} filtrado a partir de una {@link CategorySelection},
 * pensado para exportaciones parciales (XML o LaTeX) en las que el usuario solo quiere
 * incluir una parte del banco de preguntas.
 *
 * El árbol resultante está formado por copias superficiales de las categorías originales
 * (mismo nombre, mismas preguntas por referencia, pero listas de subcategorías nuevas),
 * de modo que no se modifica ni se comparte estructura mutable con el árbol original en memoria.
 */
public final class CategoryFilterBuilder {

    private CategoryFilterBuilder() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }

    /**
     * Construye el árbol filtrado a partir de la raíz original y la selección del usuario.
     *
     * @param root      categoría raíz del árbol completo original.
     * @param selection selección de categorías marcadas por el usuario.
     * @return una nueva categoría raíz que contiene únicamente las ramas seleccionadas,
     *         o {@code null} si la selección no produce ningún contenido exportable.
     */
    public static Category buildFilteredTree(Category root, CategorySelection selection) {
        return buildRecursive(root, selection, false);
    }

    /**
     * Recorre recursivamente el árbol original construyendo la copia filtrada.
     *
     * @param category      categoría original a evaluar.
     * @param selection     selección de categorías marcadas.
     * @param ancestorIncludesAll true si algún ancestro de esta categoría está marcado con
     *                      "incluir subcategorías", en cuyo caso esta rama se copia completa
     *                      sin necesidad de que cada nodo esté marcado individualmente.
     * @return una copia filtrada de la categoría, o {@code null} si no debe incluirse
     *         (ni ella ni ningún descendiente suyo forma parte de la selección).
     */
    private static Category buildRecursive(Category category, CategorySelection selection, boolean ancestorIncludesAll) {
        boolean selfSelected = selection.isSelected(category);
        // Esta rama se copia íntegra (preguntas + subcategorías) si un ancestro ya lo decidió,
        // o si esta misma categoría está marcada con "incluir subcategorías".
        boolean includeAllDescendants = ancestorIncludesAll || (selfSelected && selection.includesSubcategories(category));

        Category copy = new Category(category.getName());

        // Las preguntas directas se incluyen si la categoría está marcada (con o sin subcategorías)
        // o si la rama completa ya viene heredada de un ancestro marcado.
        if (selfSelected || ancestorIncludesAll) {
            copy.getQuestions().addAll(category.getQuestions());
        }

        boolean anyChildIncluded = false;
        for (Category sub : category.getSubcategories()) {
            Category filteredSub = buildRecursive(sub, selection, includeAllDescendants);
            if (filteredSub != null) {
                copy.addSubcategory(filteredSub);
                anyChildIncluded = true;
            }
        }

        boolean hasOwnContent = !copy.getQuestions().isEmpty();
        if (hasOwnContent || anyChildIncluded) {
            return copy;
        }
        return null;
    }
}