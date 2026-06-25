/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de Integridad de Estructuras de Datos de Preguntas")
public class QuestionDataTest {

    @Test
    @DisplayName("MultichoiceQuestion debe almacenar correctamente las opciones múltiples y sus fracciones")
    public void testMultichoiceDataStructure() {
        List<Answer> answers = Arrays.asList(
            new Answer("100", "Madrid", "¡Correcto!"),
            new Answer("0", "Barcelona", "Incorrecto")
        );
        
        MultichoiceQuestion mq = new MultichoiceQuestion("multichoice", "Capital", "¿Capital de España?", "1", "0.33", true, true, answers);
        
        // Verificamos atributos booleanos específicos
        assertTrue(mq.isSingleAnswer(), "Debería estar configurada para una sola respuesta");
        assertTrue(mq.isShuffleAnswers(), "Debería estar configurada para barajar respuestas");
        
        // Verificamos la integridad de la lista de respuestas
        assertEquals(2, mq.getAnswers().size(), "Debe contener 2 opciones de respuesta");
        assertEquals("100", mq.getAnswers().get(0).getFraction(), "La primera opción debe valer 100%");
        assertEquals("Madrid", mq.getAnswers().get(0).getText(), "El texto de la primera opción debe ser Madrid");
    }

    @Test
    @DisplayName("MatchingQuestion debe enlazar correctamente pares de pregunta-respuesta")
    public void testMatchingDataStructure() {
        List<MatchingPair> pairs = new ArrayList<>();
        pairs.add(new MatchingPair("Gato", "Felino"));
        pairs.add(new MatchingPair("Perro", "Cánido"));
        
        MatchingQuestion matchQ = new MatchingQuestion("matching", "Animales", "Empareja:", "1", "0", pairs);
        
        assertNotNull(matchQ.getPairs(), "La lista de emparejamientos no debe ser nula");
        assertEquals(2, matchQ.getPairs().size(), "Debe haber 2 pares de emparejamiento");
        
        MatchingPair firstPair = matchQ.getPairs().get(0);
        assertEquals("Gato", firstPair.getQuestionText(), "La parte izquierda del par es incorrecta");
        assertEquals("Felino", firstPair.getAnswerText(), "La parte derecha del par es incorrecta");
    }

    @Test
    @DisplayName("NumericalQuestion debe almacenar la tolerancia y el valor correcto")
    public void testNumericalDataStructure() {
        Answer numAns = new Answer("100", "3.14", "Aproximación de Pi");
        NumericalQuestion numQ = new NumericalQuestion("numerical", "Pi", "¿Valor de Pi?", "1", "0", numAns, "0.01");
        
        assertEquals("3.14", numQ.getAnswer().getText(), "El valor numérico base es incorrecto");
        assertEquals("0.01", numQ.getTolerance(), "La tolerancia de error debe ser almacenada");
    }
}