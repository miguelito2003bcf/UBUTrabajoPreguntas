package moodleviewer.model;
import java.util.List;

public class ShortAnswerQuestion extends Question {
    private boolean caseSensitive;
    private List<Answer> answers;

    public ShortAnswerQuestion(String type, String name, String text, String grade, String penalty, boolean caseSensitive, List<Answer> answers) {
        super(type, name, text, grade, penalty);
        this.caseSensitive = caseSensitive;
        this.answers = answers;
    }

    public boolean isCaseSensitive() { return caseSensitive; }
    public List<Answer> getAnswers() { return answers; }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9; margin-bottom: 5px;'>--- Respuesta Corta ---</h4>");
        sb.append("<p style='margin-top: 0; color: #555;'><b>¿Distingue mayúsculas?:</b> ").append(caseSensitive ? "Sí" : "No").append("</p>");
        
        sb.append("<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>");
        sb.append("<tr style='background-color: #f4f6f8;'><th style='padding: 8px; border: 1px solid #ddd; width: 10%;'>Valor</th><th style='padding: 8px; border: 1px solid #ddd; width: 45%;'>Respuesta</th><th style='padding: 8px; border: 1px solid #ddd; width: 45%;'>Retroalimentación</th></tr>");

        for (Answer a : answers) {
            String color = "#333";
            try {
                double val = Double.parseDouble(a.getFraction());
                if (val > 0) color = "#27ae60"; else if (val < 0) color = "#e74c3c";
            } catch(Exception e){}

            sb.append("<tr>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center; font-weight: bold; color: ").append(color).append(";'>").append(a.getFraction()).append("%</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(a.getText()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd; font-style: italic; color: #7f8c8d;'>").append(a.getFeedback().isEmpty() ? "-" : a.getFeedback()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }
}