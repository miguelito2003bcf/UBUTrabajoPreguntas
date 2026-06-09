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
 * Clase extendida de Question que representa una pregunta genérica o de ensayo de Moodle.
 */
public class GenericQuestion extends Question {
    
	/**
	 * Construye una pregunta genérica con los atributos comunes.
	 * 
	 * @param type tipo de Moodle.
	 * @param name nombre de la pregunta.
	 * @param text enunciado en HTML.
	 * @param grade calificación por defecto.
	 * @param penalty fracción por penalización.
	 */
    public GenericQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    /**
     * Muestra un área de texto deshabilitada de altura fija que representa visualmente el espacio donde el alumno´
     * escribiría su respuesta de ensayo. No muestra ninguna respeusta correcta, ya que las preguntas de ensayo 
     * requieren corrección manual.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append("\">Respuesta:</div>");
        sb.append("<div style=\"").append(HtmlConstants.INPUT_BASE).append(" height: 150px; display: flex; align-items: center; justify-content: center; color: #6c757d; font-style: italic;\">")
          .append("[ Área de editor de texto para el ensayo del alumno ]")
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