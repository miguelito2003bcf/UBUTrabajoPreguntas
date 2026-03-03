package moodleviewer.model;

public class TrueFalseQuestion extends Question {
    private String correctAnswer;

    public TrueFalseQuestion(String type, String name, String text, String grade, String penalty, String correctAnswer) {
        super(type, name, text, grade, penalty);
        this.correctAnswer = correctAnswer;
    }

    @Override
    public String getDetails() {
        return getBasicDetails() + "\n\n--- Verdadero / Falso ---\nRespuesta correcta: " + correctAnswer;
    }
}