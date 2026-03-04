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
        return getBasicDetailsHtml() + 
               "<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>" +
               "<h4 style='color: #2980b9;'>--- Respuesta Corta ---</h4>" +
               "<b>Respuesta esperada:</b> " + expectedAnswer + "<br>" +
               "<b>¿Distingue mayúsculas?:</b> " + (caseSensitive ? "Sí" : "No") + 
               "</div>";
    }
}