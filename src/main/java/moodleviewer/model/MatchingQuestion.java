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
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<table style=\"width: 100%; border-collapse: separate; border-spacing: 0 15px; font-size: 15px; color: #212529;\">");
        
        for (MatchingPair p : pairs) {
            sb.append("<tr>")
              .append("<td style=\"vertical-align: middle; width: 45%; text-align: right; padding-right: 20px;\">").append(processPluginFiles(p.getQuestionText())).append("</td>")
              .append("<td style=\"vertical-align: middle; width: 55%;\">")
              .append("<select disabled style=\"padding: 8px; border: 1px solid #ccc; border-radius: 4px; width: 100%; max-width: 250px; background-color: #f8f9fa;\">")
              .append("<option>Elegir...</option>")
              .append("<option selected>").append(processPluginFiles(p.getAnswerText())).append("</option>")
              .append("</select>")
              .append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}