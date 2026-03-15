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
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"margin-bottom: 15px; font-size: 14px; font-weight: bold; color: #333;\">Seleccione una:</div>");
        sb.append("<div style=\"margin-left: 5px;\">");
        sb.append("<div style=\"display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 15px; color: #212529;\"><input type=\"radio\" disabled style=\"margin-top: 5px; margin-right: 12px; transform: scale(1.2);\"><div>Verdadero</div></div>");
        sb.append("<div style=\"display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 15px; color: #212529;\"><input type=\"radio\" disabled style=\"margin-top: 5px; margin-right: 12px; transform: scale(1.2);\"><div>Falso</div></div>");
        sb.append("</div>");
        
        String correctAnswer = "100".equals(trueAnswer.getFraction()) ? "Verdadero" : "Falso";
        sb.append("<div style=\"margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;\">")
          .append("La respuesta correcta es '<strong>").append(correctAnswer).append("</strong>'.")
          .append("</div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}