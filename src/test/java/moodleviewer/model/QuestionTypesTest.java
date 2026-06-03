package moodleviewer.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas unitarias para verificar la correcta identificación de los tipos de pregunta
 * en la cabecera HTML generada por el método getMoodleHeader().
 */
public class QuestionTypesTest {

    @Test
    public void testTrueFalseQuestion() {
        Answer trueAnswer = new Answer("100", "Verdadero", "");
        Answer falseAnswer = new Answer("0", "Falso", "");
        Question q = new TrueFalseQuestion("truefalse", "Pregunta VF", "Enunciado", "1", "0", trueAnswer, falseAnswer);
        
        String html = q.getDetails();
        assertTrue(html.contains("TRUEFALSE"), "El HTML debería identificar correctamente el tipo de pregunta.");
    }

    @Test
    public void testShortAnswerQuestion() {
        List<Answer> answers = new ArrayList<>();
        answers.add(new Answer("100", "Respuesta", ""));
        Question q = new ShortAnswerQuestion("shortanswer", "Pregunta SA", "Enunciado", "1", "0", false, answers);
        
        String html = q.getDetails();
        assertTrue(html.contains("SHORTANSWER"), "Debería indicar el tipo correcto de pregunta.");
    }

    @Test
    public void testNumericalQuestion() {
        Answer ans = new Answer("100", "42", "");
        Question q = new NumericalQuestion("numerical", "Pregunta Num", "Enunciado", "1", "0", ans, "0");
        
        String html = q.getDetails();
        assertTrue(html.contains("NUMERICAL"), "Debería indicar el tipo de pregunta numérica.");
    }

    @Test
    public void testMultichoiceQuestion() {
        List<Answer> answers = new ArrayList<>();
        answers.add(new Answer("100", "A", ""));
        Question q = new MultichoiceQuestion("multichoice", "Pregunta MC", "Enunciado", "1", "0", true, true, answers);
        
        String html = q.getDetails();
        assertTrue(html.contains("MULTICHOICE"), "Debería identificar el tipo Multichoice.");
    }

    @Test
    public void testMatchingQuestion() {
        List<MatchingPair> pairs = new ArrayList<>();
        pairs.add(new MatchingPair("P1", "R1"));
        Question q = new MatchingQuestion("matching", "Pregunta Match", "Enunciado", "1", "0", pairs);
        
        String html = q.getDetails();
        assertTrue(html.contains("MATCHING"), "Debería indicar el tipo de pregunta de emparejamiento.");
    }

    @Test
    public void testClozeQuestion() {
        Question q = new ClozeQuestion("cloze", "Pregunta Cloze", "Enunciado {1:SA:=Resp}", "1", "0");
        
        String html = q.getDetails();
        assertTrue(html.contains("CLOZE"), "Debería identificar correctamente el tipo Cloze.");
    }

    @Test
    public void testGenericQuestion() {
        Question q = new GenericQuestion("essay", "Pregunta Ensayo", "Enunciado", "1", "0");
        
        String html = q.getDetails();
        assertTrue(html.contains("ESSAY"), "Debería mantener y mostrar el tipo original no soportado (essay).");
    }
}