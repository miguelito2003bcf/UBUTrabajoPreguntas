package moodleviewer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

@DisplayName("Tests del modelo de datos: Tipos de Preguntas")
public class QuestionTypesTest {

    @Test
    @DisplayName("Pregunta Verdadero/Falso: Generación de detalles HTML")
    public void testTrueFalseQuestion() {
        Answer trueAns = new Answer("0", "Verdadero", "Incorrecto");
        Answer falseAns = new Answer("100", "Falso", "Correcto");
        
        TrueFalseQuestion tfq = new TrueFalseQuestion("truefalse", "Herencia múltiple", "¿Java soporta herencia múltiple de clases?", "1", "0", trueAns, falseAns);
        String detailsHtml = tfq.getDetails();
        
        assertEquals("Herencia múltiple", tfq.getName(), "El nombre de la pregunta debe coincidir.");
        assertTrue(detailsHtml.contains("Falso"), "El HTML generado debería renderizar la opción de respuesta.");
        assertTrue(detailsHtml.contains("Verdadero / Falso"), "El HTML debería identificar correctamente el tipo de pregunta.");
    }

    @Test
    @DisplayName("Pregunta de Respuesta Corta: Generación de detalles HTML")
    public void testShortAnswerQuestion() {
        List<Answer> answers = Arrays.asList(new Answer("100", "Hypertext Transfer Protocol", ""));
        
        ShortAnswerQuestion saq = new ShortAnswerQuestion("shortanswer", "Siglas HTTP", "¿Qué significa HTTP?", "1", "0", false, answers);
        String detailsHtml = saq.getDetails();
        
        assertTrue(detailsHtml.contains("Respuesta Corta"), "Debería indicar el tipo correcto de pregunta.");
        assertTrue(detailsHtml.contains("Hypertext Transfer Protocol"), "Debería mostrar la cadena de texto de la respuesta esperada.");
        assertTrue(detailsHtml.contains("No"), "Debería indicar claramente que no distingue mayúsculas (false).");
    }

    @Test
    @DisplayName("Pregunta Numérica: Generación de detalles HTML y tolerancia")
    public void testNumericalQuestion() {
        Answer ans = new Answer("100", "8", "");
        
        NumericalQuestion nq = new NumericalQuestion("numerical", "Bits en un byte", "¿Cuántos bits tiene un byte?", "1", "0", ans, "0");
        String detailsHtml = nq.getDetails();
        
        assertTrue(detailsHtml.contains("Respuesta Numérica"), "Debería indicar el tipo de pregunta numérica.");
        assertTrue(detailsHtml.contains("8"), "Debería contener el valor numérico esperado.");
        assertTrue(detailsHtml.contains("0"), "Debería incluir en el renderizado la tolerancia de error permitida.");
    }

    @Test
    @DisplayName("Pregunta Multirrespuesta: Renderizado de opciones múltiples")
    public void testMultichoiceQuestion() {
        List<Answer> answers = Arrays.asList(
            new Answer("100", "Java", ""), 
            new Answer("0", "C++", ""), 
            new Answer("0", "Python", "")
        );
        
        MultichoiceQuestion mcq = new MultichoiceQuestion("multichoice", "Lenguaje de la JVM", "¿Qué lenguaje ejecuta la máquina virtual de Java?", "1", "0", true, false, answers);
        String detailsHtml = mcq.getDetails();
        
        assertTrue(detailsHtml.contains("Opciones (Multichoice)"), "Debería identificar el tipo Multichoice.");
        assertTrue(detailsHtml.contains("Java"), "El texto de la respuesta correcta debe estar presente en el HTML.");
        assertTrue(detailsHtml.contains("¿Respuesta única?: Sí"), "Debería indicar la configuración de respuesta única basándose en el booleano.");
    }

    @Test
    @DisplayName("Pregunta de Emparejamiento: Mapeo de pares lógicos")
    public void testMatchingQuestion() {
        List<MatchingPair> pairs = Arrays.asList(
            new MatchingPair("RAM", "Memoria Volátil"), 
            new MatchingPair("Disco Duro", "Almacenamiento Secundario")
        );
        
        MatchingQuestion mq = new MatchingQuestion("matching", "Componentes PC", "Empareja los componentes con su tipo", "1", "0", pairs);
        String detailsHtml = mq.getDetails();
        
        assertTrue(detailsHtml.contains("Pares de Emparejamiento"), "Debería indicar el tipo de pregunta de emparejamiento.");
        assertTrue(detailsHtml.contains("RAM") && detailsHtml.contains("Memoria Volátil"), "Ambas partes del par lógico deben estar en el HTML resultante.");
    }

    @Test
    @DisplayName("Pregunta Anidada (Cloze): Renderizado de sintaxis especial")
    public void testClozeQuestion() {
        ClozeQuestion cq = new ClozeQuestion("cloze", "Código anidado", "El bucle {1:SHORTANSWER:=for} sirve para iterar un número conocido de veces.", "1", "0");
        String detailsHtml = cq.getDetails();
        
        assertTrue(detailsHtml.contains("Pregunta Anidada (Cloze)"), "Debería identificar correctamente el tipo Cloze.");
        assertTrue(detailsHtml.contains("Las respuestas están incrustadas"), "Debería incluir el mensaje informativo propio de las preguntas anidadas.");
    }

    @Test
    @DisplayName("Pregunta Genérica: Renderizado sin formatos específicos")
    public void testGenericQuestion() {
        GenericQuestion gq = new GenericQuestion("essay", "Ensayo de Redes", "Explica el modelo OSI y sus 7 capas.", "10", "0");
        String detailsHtml = gq.getDetails();
        
        assertTrue(detailsHtml.contains("Ensayo de Redes"), "Debería mostrar el título de la pregunta genérica.");
        assertTrue(detailsHtml.contains("Explica el modelo OSI y sus 7 capas."), "Debería mostrar el enunciado principal.");
        assertTrue(detailsHtml.contains("essay"), "Debería mantener y mostrar el tipo original no soportado (essay).");
        assertFalse(detailsHtml.contains("---"), "No debería aplicar las líneas separadoras (<h4> o similar) propias de las clases hijas especializadas.");
    }
}