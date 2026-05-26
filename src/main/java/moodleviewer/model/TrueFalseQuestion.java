package moodleviewer.model;

import moodleviewer.util.HtmlConstants;

/**
 * Clase extendida de Question que representa una pregunta de verdadero/falso de Moodle.
 */
public class TrueFalseQuestion extends Question {
	
    private Answer trueAnswer;
    private Answer falseAnswer;

    /**
     * Construye una pregunta de verdadero/falso con todos sus atributos.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado en HTML.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     * @param trueAnswer respuesta para la opción "verdadero".
     * @param falseAnswer respuesta para la opción "falso".
     */
    public TrueFalseQuestion(String type, String name, String text, String grade, String penalty, Answer trueAnswer, Answer falseAnswer) {
        super(type, name, text, grade, penalty);
        this.trueAnswer = trueAnswer;
        this.falseAnswer = falseAnswer;
    }
    
    public Answer getTrueAnswer() { return trueAnswer; }
    public Answer getFalseAnswer() { return falseAnswer; }

    /**
     * Muestra dos botones deshabilitados e indica cuál es la respuesta correcta en un bloque resaltado.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append("\">Seleccione una:</div>");
        sb.append("<div style=\"margin-left: 5px;\">");
        sb.append("<div style=\"").append(HtmlConstants.FLEX_ROW_START).append("\"><input type=\"radio\" disabled style=\"margin-top: 5px; margin-right: 12px; transform: scale(1.2);\"><div>Verdadero</div></div>");
        sb.append("<div style=\"").append(HtmlConstants.FLEX_ROW_START).append("\"><input type=\"radio\" disabled style=\"margin-top: 5px; margin-right: 12px; transform: scale(1.2);\"><div>Falso</div></div>");
        sb.append("</div>");
        
        String correctAnswer = "100".equals(trueAnswer.getFraction()) ? "Verdadero" : "Falso";
        sb.append("<div style=\"").append(HtmlConstants.FEEDBACK_WARNING).append("\">")
          .append("La respuesta correcta es '<strong>").append(correctAnswer).append("</strong>'.")
          .append("</div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
    
    /**
     * Método creado para cumplir con el patrón de diseño Visitor.
     * 
     * @param visitor visitante que procesará esta pregunta.
     */
    @Override
    public void accept(QuestionVisitor visitor) {
        visitor.visit(this);
    }
}