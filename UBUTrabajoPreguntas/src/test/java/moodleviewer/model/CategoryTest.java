package moodleviewer.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryTest {

    @Test
    public void testCategoryCreation() {
    	
        String categoryName = "Ingeniería Informática";
        Category category = new Category(categoryName);

        assertEquals(categoryName, category.getName(), "El nombre de la categoría debería coincidir.");
        assertTrue(category.getQuestions().isEmpty(), "La lista de preguntas debería inicializarse vacía.");
        assertTrue(category.getSubcategories().isEmpty(), "La lista de subcategorías debería inicializarse vacía.");
    }

    @Test
    public void testAddSubcategory() {

        Category root = new Category("Programación");
        Category sub = new Category("Java y Programación Orientada a Objetos");
        root.addSubcategory(sub);

        assertEquals(1, root.getSubcategories().size(), "Debería haber exactamente 1 subcategoría.");
        assertEquals("Java y Programación Orientada a Objetos", root.getSubcategories().get(0).getName(), "El nombre de la subcategoría insertada no es correcto.");
    }

    @Test
    public void testAddQuestion() {

        Category category = new Category("Bases de Datos");
        Question mockQuestion = new TrueFalseQuestion(
            "truefalse", 
            "SQL Básico", 
            "¿La sentencia SELECT se utiliza para extraer datos de una base de datos?", 
            "1.00", 
            "0.00", 
            "Verdadero"
        );

        category.addQuestion(mockQuestion);

        assertEquals(1, category.getQuestions().size(), "Debería haber 1 pregunta en la lista.");
        assertEquals("SQL Básico", category.getQuestions().get(0).getName(), "La pregunta insertada no coincide.");
    }
}