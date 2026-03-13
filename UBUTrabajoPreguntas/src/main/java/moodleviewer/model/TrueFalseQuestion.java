package moodleviewer.model;

public class TrueFalseQuestion extends Question {
    private Answer trueAnswer;
    private Answer falseAnswer;

    public TrueFalseQuestion(String type, String name, String text, String grade, String penalty, Answer trueAnswer, Answer falseAnswer) {
        super(type, name, text, grade, penalty);
        this.trueAnswer = trueAnswer;
        this.falseAnswer = falseAnswer;
    }

    public Answer getTrueAnswer() { return trueAnswer; }
    public Answer getFalseAnswer() { return falseAnswer; }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9; margin-bottom: 10px;'>--- Verdadero / Falso ---</h4>");
        
        sb.append("<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>");
        sb.append("<tr style='background-color: #f4f6f8;'><th style='padding: 8px; border: 1px solid #ddd; width: 10%;'>Valor</th><th style='padding: 8px; border: 1px solid #ddd; width: 20%;'>Opción</th><th style='padding: 8px; border: 1px solid #ddd; width: 70%;'>Retroalimentación</th></tr>");

        String trueColor = "100".equals(trueAnswer.getFraction()) ? "#27ae60" : "#e74c3c";
        sb.append("<tr>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center; font-weight: bold; color: ").append(trueColor).append(";'>").append(trueAnswer.getFraction()).append("%</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>Verdadero</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; font-style: italic; color: #7f8c8d;'>").append(trueAnswer.getFeedback().isEmpty() ? "-" : trueAnswer.getFeedback()).append("</td>");
        sb.append("</tr>");

        String falseColor = "100".equals(falseAnswer.getFraction()) ? "#27ae60" : "#e74c3c";
        sb.append("<tr>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center; font-weight: bold; color: ").append(falseColor).append(";'>").append(falseAnswer.getFraction()).append("%</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>Falso</td>");
        sb.append("<td style='padding: 8px; border: 1px solid #ddd; font-style: italic; color: #7f8c8d;'>").append(falseAnswer.getFeedback().isEmpty() ? "-" : falseAnswer.getFeedback()).append("</td>");
        sb.append("</tr>");

        sb.append("</table></div>");
        return sb.toString();
    }
}