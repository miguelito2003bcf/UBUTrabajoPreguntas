package moodleviewer.model;
import java.util.List;

public class MatchingQuestion extends Question {
    private List<String> pairs;

    public MatchingQuestion(String type, String name, String text, String grade, String penalty, List<String> pairs) {
        super(type, name, text, grade, penalty);
        this.pairs = pairs;
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9;'>--- Pares de Emparejamiento ---</h4><ul>");
        for (String p : pairs) {
            sb.append("<li>").append(p).append("</li>");
        }
        sb.append("</ul></div>");
        
        return sb.toString();
    }
}