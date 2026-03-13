package moodleviewer.model;
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
        StringBuilder sb = new StringBuilder(getBasicDetailsHtml());
        sb.append("<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>");
        sb.append("<h4 style='color: #2980b9; margin-bottom: 5px;'>--- Opciones (Multichoice) ---</h4>");
        sb.append("<p style='margin-top: 0; color: #555;'><b>¿Respuesta única?:</b> ").append(singleAnswer ? "Sí" : "No").append("</p>");
        
        sb.append("<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>");
        sb.append("<tr style='background-color: #f4f6f8;'><th style='padding: 8px; border: 1px solid #ddd; width: 10%;'>Valor</th><th style='padding: 8px; border: 1px solid #ddd; width: 45%;'>Respuesta</th><th style='padding: 8px; border: 1px solid #ddd; width: 45%;'>Retroalimentación</th></tr>");

        for (Answer a : answers) {
            String color = "#333";
            try {
                double val = Double.parseDouble(a.getFraction());
                if (val > 0) color = "#27ae60";
                else if (val < 0) color = "#e74c3c";
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