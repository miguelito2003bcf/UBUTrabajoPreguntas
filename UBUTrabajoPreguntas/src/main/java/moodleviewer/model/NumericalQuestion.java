package moodleviewer.model;

public class NumericalQuestion extends Question {
    private String answer;
    private String tolerance;

    public NumericalQuestion(String type, String name, String text, String grade, String penalty, String answer, String tolerance) {
        super(type, name, text, grade, penalty);
        this.answer = answer;
        this.tolerance = tolerance;
    }

    @Override
    public String getDetails() {
        return getBasicDetails() + "\n\n--- Respuesta Numérica ---\nRespuesta esperada: " + answer + 
               "\nTolerancia permitida: " + (tolerance != null ? tolerance : "0");
    }
}