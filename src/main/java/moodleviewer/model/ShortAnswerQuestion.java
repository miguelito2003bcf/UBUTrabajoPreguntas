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
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 20px; font-size: 15px; color: #212529;\">")
          .append("<strong style=\"margin-right: 15px;\">Respuesta:</strong>")
          .append("<input type=\"text\" disabled style=\"padding: 8px; border: 1px solid #ccc; border-radius: 4px; width: 300px; background-color: #f8f9fa;\">")
          .append("</div>");
          
        String correctAnswer = "";
        for (Answer a : answers) {
            if ("100".equals(a.getFraction())) {
                correctAnswer = processPluginFiles(a.getText());
                break;
            }
        }
        
        if (!correctAnswer.isEmpty()) {
            sb.append("<div style=\"margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;\">")
              .append("La respuesta correcta es: <strong>").append(correctAnswer).append("</strong>")
              .append("</div>");
        }
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
    
    @Override
    public void accept(QuestionVisitor visitor) {
        visitor.visit(this);
    }
}