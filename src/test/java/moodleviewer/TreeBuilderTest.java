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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests del constructor del árbol (TreeBuilder)")
public class TreeBuilderTest {

    private Category rootCategory;

    @BeforeEach
    public void setUp() {
        // Preparamos una jerarquía de categorías de prueba antes de cada test
        rootCategory = new Category("Raíz");
        
        Category catZ = new Category("Zoología");
        Category catA = new Category("Anatomía"); // Añadida después, pero debe ordenarse primero
        
        Category subA1 = new Category("Huesos");
        Category subA2 = new Category("Músculos");
        
        catA.addSubcategory(subA1);
        catA.addSubcategory(subA2);
        
        rootCategory.addSubcategory(catZ);
        rootCategory.addSubcategory(catA);
    }

    @Test
    @DisplayName("Debe construir el árbol y ordenar las subcategorías alfabéticamente")
    public void testCreateTreeItemSorting() {
        TreeItem<Category> rootItem = TreeBuilder.createTreeItem(rootCategory);
        
        assertNotNull(rootItem, "El nodo raíz no debería ser nulo");
        assertEquals("Raíz", rootItem.getValue().getName());
        assertEquals(2, rootItem.getChildren().size(), "Debe tener 2 hijos directos");
        
        // Comprobamos que "Anatomía" se ha colocado antes que "Zoología" por orden alfabético
        assertEquals("Anatomía", rootItem.getChildren().get(0).getValue().getName(), "El orden alfabético ha fallado");
        assertEquals("Zoología", rootItem.getChildren().get(1).getValue().getName());
    }

    @Test
    @DisplayName("Debe filtrar el árbol correctamente ignorando mayúsculas/minúsculas")
    public void testCreateFilteredTreeItem() {
        // Buscamos "hues", debería devolver la raíz -> Anatomía -> Huesos (ignorando Zoología y Músculos)
        TreeItem<Category> filteredItem = TreeBuilder.createFilteredTreeItem(rootCategory, "hues");
        
        assertNotNull(filteredItem, "El resultado del filtro no debería ser nulo");
        
        // Verificamos la estructura resultante
        assertEquals(1, filteredItem.getChildren().size(), "Solo debería quedar la rama que contiene 'Huesos'");
        TreeItem<Category> anatomiaItem = filteredItem.getChildren().get(0);
        assertEquals("Anatomía", anatomiaItem.getValue().getName());
        
        assertEquals(1, anatomiaItem.getChildren().size(), "Solo debería quedar la subcategoría 'Huesos'");
        assertEquals("Huesos", anatomiaItem.getChildren().get(0).getValue().getName());
    }
    
    @Test
    @DisplayName("Debe devolver null si ninguna categoría coincide con el filtro")
    public void testCreateFilteredTreeItemNoMatch() {
        // Buscamos algo que no existe
        TreeItem<Category> filteredItem = TreeBuilder.createFilteredTreeItem(rootCategory, "matemáticas");
        assertNull(filteredItem, "Si no hay coincidencias, debe devolver null");
    }
}