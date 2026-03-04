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
        return getBasicDetailsHtml() + 
               "<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>" +
               "<h4 style='color: #2980b9;'>--- Respuesta Numérica ---</h4>" +
               "<b>Respuesta esperada:</b> " + answer + "<br>" +
               "<b>Tolerancia permitida:</b> " + (tolerance != null ? tolerance : "0") + 
               "</div>";
    }
}