/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import moodleviewer.util.HtmlConstants;
import java.util.List;

/**
 * Clase extendida de Question que representa una pregunta de emparejamiento de Moodle.
 */
public class MatchingQuestion extends Question {
	
    private List<MatchingPair> pairs;

    /**
     * Construye una pregunta de emparejamiento con todos sus atributos.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado general en HTML.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     * @param pairs lista de pares enunciado-respuesta.
     */
    public MatchingQuestion(String type, String name, String text, String grade, String penalty, List<MatchingPair> pairs) {
        super(type, name, text, grade, penalty);
        this.pairs = pairs;
    }

    public List<MatchingPair> getPairs() { return pairs; }

    /**
     * Genera una tabla HTML en la que cada fila muestra el enunciado de un par a la izaquierda y un desplegable con la 
     * respuesta correcta preseleccionada a la derecha.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<table style=\"").append(HtmlConstants.TABLE_LAYOUT).append("\">");
        
        for (MatchingPair p : pairs) {
            sb.append("<tr>")
              .append("<td style=\"").append(HtmlConstants.MATCHING_CELL_QUESTION).append("\">").append(processPluginFiles(p.getQuestionText())).append("</td>")
              .append("<td style=\"").append(HtmlConstants.MATCHING_CELL_ANSWER).append("\">")
              .append("<select disabled style=\"").append(HtmlConstants.MATCHING_SELECT).append("\">")
              .append("<option>Elegir...</option>")
              .append("<option selected>").append(processPluginFiles(p.getAnswerText())).append("</option>")
              .append("</select></td></tr>");
        }
        sb.append("</table>");
        
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