package moodleviewer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests del modelo de datos: Categorías")
public class CategoryTest {

    @Test
    @DisplayName("Debe crear una categoría vacía correctamente inicializada")
    public void testCategoryCreation() {
        String categoryName = "Ingeniería Informática";
        
        Category category = new Category(categoryName);

        assertEquals(categoryName, category.getName(), "El nombre de la categoría no coincide con el proporcionado.");
        assertNotNull(category.getQuestions(), "La lista de preguntas no debe ser nula.");
        assertTrue(category.getQuestions().isEmpty(), "La lista de preguntas debería inicializarse vacía.");
        assertNotNull(category.getSubcategories(), "La lista de subcategorías no debe ser nula.");
        assertTrue(category.getSubcategories().isEmpty(), "La lista de subcategorías debería inicializarse vacía.");
    }

    @Test
    @DisplayName("Debe permitir añadir subcategorías anidadas")
    public void testAddSubcategory() {
        Category root = new Category("Programación");
        Category sub = new Category("Java y Programación Orientada a Objetos");

        root.addSubcategory(sub);

        assertEquals(1, root.getSubcategories().size(), "Debería haber exactamente 1 subcategoría en la raíz.");
        assertEquals("Java y Programación Orientada a Objetos", root.getSubcategories().get(0).getName(), "El nombre de la subcategoría anidada no es correcto.");
    }

    @Test
    @DisplayName("Debe permitir añadir preguntas a una categoría")
    public void testAddQuestion() {
        Category category = new Category("Bases de Datos");
        Answer trueAns = new Answer("100", "Verdadero", "¡Correcto!");
        Answer falseAns = new Answer("0", "Falso", "Repasa la teoría.");
        
        Question mockQuestion = new TrueFalseQuestion(
            "truefalse", 
            "SQL Básico", 
            "¿La sentencia SELECT se utiliza para extraer datos de una base de datos?", 
            "1.00", 
            "0.00", 
            trueAns,
            falseAns
        );

        category.addQuestion(mockQuestion);

        assertEquals(1, category.getQuestions().size(), "La categoría debería contener exactamente 1 pregunta.");
        assertEquals("SQL Básico", category.getQuestions().get(0).getName(), "El nombre de la pregunta insertada no coincide.");
    }
}