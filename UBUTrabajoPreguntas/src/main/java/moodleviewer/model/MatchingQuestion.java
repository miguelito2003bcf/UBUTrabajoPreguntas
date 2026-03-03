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
        StringBuilder sb = new StringBuilder(getBasicDetails());
        sb.append("\n\n--- Pares de Emparejamiento ---\n");
        for (String p : pairs) {
            sb.append("- ").append(p.replaceAll("<[^>]*>", "")).append("\n");
        }
        return sb.toString();
    }
}