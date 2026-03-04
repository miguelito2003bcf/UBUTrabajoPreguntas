package moodleviewer.model;

public class TrueFalseQuestion extends Question {
    private String correctAnswer;

    public TrueFalseQuestion(String type, String name, String text, String grade, String penalty, String correctAnswer) {
        super(type, name, text, grade, penalty);
        this.correctAnswer = correctAnswer;
    }

    @Override
    public String getDetails() {
        return getBasicDetailsHtml() + 
               "<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>" +
               "<h4 style='color: #2980b9;'>--- Verdadero / Falso ---</h4>" +
               "<b>Respuesta correcta:</b> " + correctAnswer + 
               "</div>";
    }
}