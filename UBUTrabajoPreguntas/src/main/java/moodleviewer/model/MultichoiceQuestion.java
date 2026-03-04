package moodleviewer.model;
import java.util.List;

public class MultichoiceQuestion extends Question {
    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private List<String> answers;

    public MultichoiceQuestion(String type, String name, String text, String grade, String penalty, boolean single, boolean shuffle, List<String> answers) {
        super(type, name, text, grade, penalty);
        this.singleAnswer = single;
        this.shuffleAnswers = shuffle;
        this.answers = answers;
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9;'>--- Opciones (Multichoice) ---</h4>");
        sb.append("<b>¿Respuesta única?:</b> ").append(singleAnswer ? "Sí" : "No").append("<br>");
        sb.append("<b>¿Barajar respuestas?:</b> ").append(shuffleAnswers ? "Sí" : "No").append("<br><br>");
        sb.append("<b>Respuestas:</b><ul>");
        
        for (String a : answers) {
            sb.append("<li>").append(a).append("</li>");
        }
        sb.append("</ul></div>");
        
        return sb.toString();
    }
}