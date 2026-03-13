package moodleviewer.model;
import java.util.List;

public class MatchingQuestion extends Question {
    private List<MatchingPair> pairs;

    public MatchingQuestion(String type, String name, String text, String grade, String penalty, List<MatchingPair> pairs) {
        super(type, name, text, grade, penalty);
        this.pairs = pairs;
    }

    public List<MatchingPair> getPairs() { return pairs; }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9; margin-bottom: 10px;'>--- Pares de Emparejamiento ---</h4>");
        
        sb.append("<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>");
        sb.append("<tr style='background-color: #f4f6f8;'><th style='padding: 8px; border: 1px solid #ddd; width: 50%;'>Pregunta / Estímulo</th><th style='padding: 8px; border: 1px solid #ddd; width: 50%;'>Respuesta Correcta</th></tr>");
        
        for (MatchingPair p : pairs) {
            sb.append("<tr>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(p.getQuestionText()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd; color: #27ae60; font-weight: bold;'>").append(p.getAnswerText()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }
}