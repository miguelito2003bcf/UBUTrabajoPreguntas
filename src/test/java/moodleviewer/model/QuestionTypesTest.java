package moodleviewer.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

public class QuestionTypesTest {

    @Test
    public void testTrueFalseQuestion() {
        // --- CÓDIGO ACTUALIZADO ---
        Answer trueAns = new Answer("0", "Verdadero", "Incorrecto");
        Answer falseAns = new Answer("100", "Falso", "Correcto");
        TrueFalseQuestion tfq = new TrueFalseQuestion("truefalse", "Herencia múltiple", "¿Java soporta herencia múltiple de clases?", "1", "0", trueAns, falseAns);
        
        assertEquals("Herencia múltiple", tfq.getName());
        assertTrue(tfq.getDetails().contains("Falso"), "El HTML debería contener la respuesta correcta.");
        assertTrue(tfq.getDetails().contains("Verdadero / Falso"), "Debería indicar que es una pregunta V/F.");
    }

    @Test
    public void testShortAnswerQuestion() {
        // --- CÓDIGO ACTUALIZADO ---
        List<Answer> answers = Arrays.asList(new Answer("100", "Hypertext Transfer Protocol", ""));
        ShortAnswerQuestion saq = new ShortAnswerQuestion("shortanswer", "Siglas HTTP", "¿Qué significa HTTP?", "1", "0", false, answers);
        
        assertTrue(saq.getDetails().contains("Respuesta Corta"));
        assertTrue(saq.getDetails().contains("Hypertext Transfer Protocol"));
        assertTrue(saq.getDetails().contains("No"), "Debería indicar 'No' en distingue mayúsculas.");
    }

    @Test
    public void testNumericalQuestion() {
        // --- CÓDIGO ACTUALIZADO ---
        Answer ans = new Answer("100", "8", "");
        NumericalQuestion nq = new NumericalQuestion("numerical", "Bits en un byte", "¿Cuántos bits tiene un byte?", "1", "0", ans, "0");
        
        assertTrue(nq.getDetails().contains("Respuesta Numérica"));
        assertTrue(nq.getDetails().contains("8"));
        assertTrue(nq.getDetails().contains("0"), "Debería incluir la tolerancia permitida.");
    }

    @Test
    public void testMultichoiceQuestion() {
        // --- CÓDIGO ACTUALIZADO ---
        List<Answer> answers = Arrays.asList(
            new Answer("100", "Java", ""), 
            new Answer("0", "C++", ""), 
            new Answer("0", "Python", "")
        );
        MultichoiceQuestion mcq = new MultichoiceQuestion("multichoice", "Lenguaje de la JVM", "¿Qué lenguaje ejecuta la máquina virtual de Java?", "1", "0", true, false, answers);
        
        assertTrue(mcq.getDetails().contains("Opciones (Multichoice)"));
        assertTrue(mcq.getDetails().contains("Java"), "Debería mostrar las respuestas pasadas por lista.");
        assertTrue(mcq.getDetails().contains("¿Respuesta única?: Sí"));
    }

    @Test
    public void testMatchingQuestion() {
        // --- CÓDIGO ACTUALIZADO ---
        List<MatchingPair> pairs = Arrays.asList(
            new MatchingPair("RAM", "Memoria Volátil"), 
            new MatchingPair("Disco Duro", "Almacenamiento Secundario")
        );
        MatchingQuestion mq = new MatchingQuestion("matching", "Componentes PC", "Empareja los componentes con su tipo", "1", "0", pairs);
        
        assertTrue(mq.getDetails().contains("Pares de Emparejamiento"));
        assertTrue(mq.getDetails().contains("RAM"), "Debería incluir las parejas correctas.");
    }

    @Test
    public void testClozeQuestion() {
        ClozeQuestion cq = new ClozeQuestion("cloze", "Código anidado", "El bucle {1:SHORTANSWER:=for} sirve para iterar un número conocido de veces.", "1", "0");
        assertTrue(cq.getDetails().contains("Pregunta Anidada (Cloze)"));
        assertTrue(cq.getDetails().contains("Las respuestas están incrustadas"));
    }

    @Test
    public void testGenericQuestion() {
        GenericQuestion gq = new GenericQuestion("essay", "Ensayo de Redes", "Explica el modelo OSI y sus 7 capas.", "10", "0");
        assertTrue(gq.getDetails().contains("Ensayo de Redes"));
        assertTrue(gq.getDetails().contains("Explica el modelo OSI y sus 7 capas."));
        assertTrue(gq.getDetails().contains("essay"), "Debería mostrar el tipo base de la pregunta.");
        assertFalse(gq.getDetails().contains("---"), "La pregunta genérica no debería tener el separador h4 de clases específicas.");
    }
}