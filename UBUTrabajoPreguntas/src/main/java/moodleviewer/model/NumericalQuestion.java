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
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9; margin-bottom: 10px;'>--- Respuesta Numérica ---</h4>");
        
        sb.append("<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>");
        sb.append("<tr style='background-color: #f4f6f8;'><th style='padding: 8px; border: 1px solid #ddd;'>Respuesta Esperada</th><th style='padding: 8px; border: 1px solid #ddd;'>Tolerancia</th><th style='padding: 8px; border: 1px solid #ddd;'>Valor</th><th style='padding: 8px; border: 1px solid #ddd;'>Retroalimentación</th></tr>");

        sb.append("<tr>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; font-weight: bold; color: #27ae60;'>").append(answer.getText()).append("</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center;'>± ").append(tolerance != null ? tolerance : "0").append("</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center;'>").append(answer.getFraction()).append("%</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; font-style: italic; color: #7f8c8d;'>").append(answer.getFeedback().isEmpty() ? "-" : answer.getFeedback()).append("</td>");
        sb.append("</tr>");

        sb.append("</table></div>");
        return sb.toString();
    }
}