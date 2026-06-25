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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Representa el resultado de una selección parcial de categorías para exportar.
 * Identifica las categorías marcadas por el usuario por referencia de objeto (no por nombre,
 * ya que dos categorías distintas del árbol pueden compartir nombre), y para cada una indica
 * si también deben incluirse sus subcategorías al exportar.
 *
 * AVISO DE ACOPLAMIENTO: como la identificación es por referencia de objeto (Category no
 * sobreescribe equals/hashCode, por lo que aquí se usa la identidad por defecto), esta
 * selección solo es válida mientras se siga trabajando sobre las MISMAS instancias de
 * Category con las que se construyó. Si en algún punto el árbol original se reconstruye o
 * se recargan las categorías desde disco (por ejemplo, tras una nueva importación), cualquier
 * CategorySelection creada antes queda obsoleta de forma silenciosa: isSelected() devolverá
 * false para todo porque las referencias ya no coinciden, sin lanzar ningún error. No reutilizar
 * una instancia de CategorySelection entre distintas cargas o reconstrucciones del árbol.
 */
public class CategorySelection {

    private final Set<Category> selectedCategories = new HashSet<>();
    private final Map<Category, Boolean> includeSubcategories = new HashMap<>();

    /**
     * Marca una categoría como seleccionada para exportar.
     *
     * @param category categoría a marcar.
     * @param withSubcategories true si también deben incluirse sus subcategorías.
     */
    public void select(Category category, boolean withSubcategories) {
        selectedCategories.add(category);
        includeSubcategories.put(category, withSubcategories);
    }

    /**
     * Desmarca una categoría previamente seleccionada.
     *
     * @param category categoría a desmarcar.
     */
    public void deselect(Category category) {
        selectedCategories.remove(category);
        includeSubcategories.remove(category);
    }

    /**
     * Indica si la categoría dada está marcada explícitamente por el usuario.
     *
     * @param category categoría a comprobar.
     * @return true si está marcada.
     */
    public boolean isSelected(Category category) {
        return selectedCategories.contains(category);
    }

    /**
     * Indica si, estando marcada, también deben incluirse sus subcategorías.
     * Si la categoría no está marcada, el resultado no tiene relevancia (devuelve false).
     *
     * @param category categoría a comprobar.
     * @return true si deben incluirse las subcategorías de esta categoría.
     */
    public boolean includesSubcategories(Category category) {
        return includeSubcategories.getOrDefault(category, false);
    }

    /**
     * Indica si no se ha marcado ninguna categoría.
     *
     * @return true si la selección está vacía.
     */
    public boolean isEmpty() {
        return selectedCategories.isEmpty();
    }
}