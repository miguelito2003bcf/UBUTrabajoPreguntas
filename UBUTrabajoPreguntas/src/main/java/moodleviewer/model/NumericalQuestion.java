package moodleviewer.model;

public class NumericalQuestion extends Question {
    private Answer answer;
    private String tolerance;

    public NumericalQuestion(String type, String name, String text, String grade, String penalty, Answer answer, String tolerance) {
        super(type, name, text, grade, penalty);
        this.answer = answer;
        this.tolerance = tolerance;
    }

    public Answer getAnswer() { return answer; }
    public String getTolerance() { return tolerance; }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 20px; font-size: 15px; color: #212529;\">")
          .append("<strong style=\"margin-right: 15px;\">Respuesta:</strong>")
          .append("<input type=\"text\" disabled style=\"padding: 8px; border: 1px solid #ccc; border-radius: 4px; width: 150px; background-color: #f8f9fa;\">")
          .append("</div>");
          
        String tolText = (tolerance != null && !tolerance.equals("0") && !tolerance.isEmpty()) ? " (margen de error ±" + tolerance + ")" : "";
        
        sb.append("<div style=\"margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;\">")
          .append("La respuesta correcta es: <strong>").append(processPluginFiles(answer.getText())).append("</strong>").append(tolText)
          .append("</div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}