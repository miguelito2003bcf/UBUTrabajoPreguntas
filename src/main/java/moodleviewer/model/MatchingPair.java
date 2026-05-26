package moodleviewer.model;

/**
 * Clase que representa un par pregunta-respuesta dentro de una pregunta de emparejamiento.
 */
public class MatchingPair {
	
    private String questionText;
    private String answerText;

    /**
     * Construye un par de emparejamientos con su enunciado y su respuesta.
     * 
     * @param questionText texto del enunciado.
     * @param answerText texto de la respuesta.
     */
    public MatchingPair(String questionText, String answerText) {
        this.questionText = questionText;
        this.answerText = answerText;
    }

    public String getQuestionText() { return questionText; }
    public String getAnswerText() { return answerText; }
}