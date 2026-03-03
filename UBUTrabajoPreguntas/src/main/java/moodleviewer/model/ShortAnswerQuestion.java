package moodleviewer.model;

public class ShortAnswerQuestion extends Question {
    private String expectedAnswer;
    private boolean caseSensitive;

    public ShortAnswerQuestion(String type, String name, String text, String grade, String penalty, String expectedAnswer, boolean caseSensitive) {
        super(type, name, text, grade, penalty);
        this.expectedAnswer = expectedAnswer;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public String getDetails() {
        return getBasicDetails() + "\n\n--- Respuesta Corta ---\nRespuesta esperada: " + expectedAnswer + 
               "\n¿Distingue mayúsculas?: " + (caseSensitive ? "Sí" : "No");
    }
}