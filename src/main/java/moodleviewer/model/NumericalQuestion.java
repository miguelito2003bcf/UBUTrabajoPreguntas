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

/**
 * Clase extendida de Question que representa una pregunta numerica de Moodle.
 */
public class NumericalQuestion extends Question {
    
	private Answer answer;
    private String tolerance;

    /**
     * Construye una pregunta numérica con todos sus atributos.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado en HTML.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     * @param answer respuesta correcta con su fracción y retroalimentación
     * @param tolerance margen de tolerancia como cadena numérica.
     */
    public NumericalQuestion(String type, String name, String text, String grade, String penalty, Answer answer, String tolerance) {
        super(type, name, text, grade, penalty);
        this.answer = answer;
        this.tolerance = tolerance;
    }

    public Answer getAnswer() { return answer; }
    public String getTolerance() { return tolerance; }

    /**
     * Muestra un campo de texto deshabilitado que representa la zona de entrada del alumno, seguido
     * de un bloque que indica el valor correcto y el margen de error si este es distinto de cero.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.FLEX_ROW).append("\">")
          .append("<strong style=\"margin-right: 15px;\">Respuesta:</strong>")
          .append("<input type=\"text\" disabled style=\"").append(HtmlConstants.INPUT_BASE).append(" width: 150px;\">")
          .append("</div>");
          
        String tolText = (tolerance != null && !tolerance.equals("0") && !tolerance.isEmpty()) ? " (margen de error ±" + tolerance + ")" : "";
        
        sb.append("<div style=\"").append(HtmlConstants.FEEDBACK_WARNING).append("\">")
          .append("La respuesta correcta es: <strong>").append(processPluginFiles(answer.getText())).append("</strong>").append(tolText)
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