/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de la sintaxis y renderizado Cloze (ClozeQuestion)")
public class ClozeQuestionTest {

    // Nos aseguramos de devolver el flag estático a su estado original después de cada test
    @AfterEach
    public void tearDown() {
        ClozeQuestion.MODO_PREVIA_ALUMNO = false;
    }

    @Test
    @DisplayName("Debe resaltar la sintaxis en modo Profesor (MODO_PREVIA_ALUMNO = false)")
    public void testClozeSyntaxHighlighting() {
        ClozeQuestion.MODO_PREVIA_ALUMNO = false;
        
        String clozeText = "La capital de España es {1:SA:=Madrid}.";
        ClozeQuestion q = new ClozeQuestion("cloze", "Geografía", clozeText, "1", "0");
        
        String detailsHtml = q.getDetails();
        
        // Verificamos que se mantienen los tokens pero envueltos en un span con el estilo de resalte
        assertTrue(detailsHtml.contains("Sintaxis Cloze:"), "Debe mostrar el título de 'Sintaxis Cloze'");
        assertTrue(detailsHtml.contains("<span style="), "Debe contener etiquetas <span> para resaltar el texto");
        assertTrue(detailsHtml.contains("{1:SA:=Madrid}"), "La sintaxis Cloze original debe estar visible");
    }

    @Test
    @DisplayName("Debe renderizar inputs interactivos en modo Alumno (MODO_PREVIA_ALUMNO = true)")
    public void testClozeStudentRenderingShortAnswer() {
        ClozeQuestion.MODO_PREVIA_ALUMNO = true;
        
        String clozeText = "La capital de Francia es {1:SHORTANSWER:=París~%0%Lyon}.";
        ClozeQuestion q = new ClozeQuestion("cloze", "Geografía", clozeText, "1", "0");
        
        String detailsHtml = q.getDetails();
        
        // Verificamos que la sintaxis desaparece y se convierte en un input
        assertTrue(detailsHtml.contains("Vista previa del alumno:"), "Debe mostrar el título de 'Vista previa del alumno'");
        assertFalse(detailsHtml.contains("{1:SHORTANSWER:="), "La sintaxis cruda NO debe estar visible en modo alumno");
        
        // Debe haber generado un input de texto con París como placeholder/valor correcto visual
        assertTrue(detailsHtml.contains("<input type='text' disabled"), "Debe generar un elemento <input> deshabilitado");
        assertTrue(detailsHtml.contains("placeholder='París'"), "El input debe mostrar la respuesta correcta 'París'");
    }

    @Test
    @DisplayName("Debe renderizar menús desplegables (select) para opciones múltiples")
    public void testClozeStudentRenderingMultiChoice() {
        ClozeQuestion.MODO_PREVIA_ALUMNO = true;
        
        String clozeText = "El color del cielo es {1:MULTICHOICE:=Azul~%0%Rojo~%0%Verde}.";
        ClozeQuestion q = new ClozeQuestion("cloze", "Colores", clozeText, "1", "0");
        
        String detailsHtml = q.getDetails();
        
        // Debe generar un desplegable
        assertTrue(detailsHtml.contains("<select"), "Debe generar un elemento <select> para MULTICHOICE");
        assertTrue(detailsHtml.contains("Azul"), "Debe contener la opción Azul");
        assertTrue(detailsHtml.contains("Rojo"), "Debe contener la opción Rojo");
        assertTrue(detailsHtml.contains("✓"), "Debe marcar visualmente la opción correcta con un tick (✓)");
    }
}