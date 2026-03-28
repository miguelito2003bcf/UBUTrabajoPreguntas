package moodleviewer.model;
import java.util.ArrayList;
import java.util.List;

public class MultichoiceQuestion extends Question {
    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private List<Answer> answers;

    public MultichoiceQuestion(String type, String name, String text, String grade, String penalty, boolean single, boolean shuffle, List<Answer> answers) {
        super(type, name, text, grade, penalty);
        this.singleAnswer = single;
        this.shuffleAnswers = shuffle;
        this.answers = answers;
    }

    public boolean isSingleAnswer() { return singleAnswer; }
    public boolean isShuffleAnswers() { return shuffleAnswers; }
    public List<Answer> getAnswers() { return answers; }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"margin-bottom: 15px; font-size: 14px; font-weight: bold; color: #333;\">Seleccione una").append(isSingleAnswer() ? ":" : " o más de una:").append("</div>");
        
        char letter = 'a';
        List<String> correctAnswers = new ArrayList<>();
        
        sb.append("<div style=\"margin-left: 5px;\">");
        for (Answer a : answers) {
            try {
                if (Double.parseDouble(a.getFraction()) > 0) {
                    correctAnswers.add(processPluginFiles(a.getText()));
                }
            } catch (Exception e) {}
            
            String inputType = isSingleAnswer() ? "radio" : "checkbox";
            
            sb.append("<div style=\"display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 15px; color: #212529;\">")
              .append("<input type=\"").append(inputType).append("\" disabled style=\"margin-top: 5px; margin-right: 12px; transform: scale(1.2);\">")
              .append("<div><strong>").append(letter).append(".</strong> ").append(processPluginFiles(a.getText()))
              .append("</div></div>");
            letter++;
        }
        sb.append("</div>");
        
        if (!correctAnswers.isEmpty()) {
            sb.append("<div style=\"margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;\">")
              .append("La(s) respuesta(s) correcta(s):<br><ul>");
            for (String ca : correctAnswers) sb.append("<li>").append(ca).append("</li>");
            sb.append("</ul></div>");
        }
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}