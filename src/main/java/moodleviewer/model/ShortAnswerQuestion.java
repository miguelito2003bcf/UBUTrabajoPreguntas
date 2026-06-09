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
 * Clase extendida de Question que representa una pregunta de respuesta corta de Moodle.
 */
public class ShortAnswerQuestion extends Question {
	
    private boolean caseSensitive;
    private List<Answer> answers;

    /**
     * Construye una pregunta de respuesta corta con todos sus atributos.
     * 
     * @param type tipo Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado en HTML.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     * @param caseSensitive true si la comparación distingue entre mayúsculas y minúsculas.
     * @param answers lista de respuestas aceptadas con sus fracciones.
     */
    public ShortAnswerQuestion(String type, String name, String text, String grade, String penalty, boolean caseSensitive, List<Answer> answers) {
        super(type, name, text, grade, penalty);
        this.caseSensitive = caseSensitive;
        this.answers = answers;
    }

    public boolean isCaseSensitive() { return caseSensitive; }
    public List<Answer> getAnswers() { return answers; }

    /**
     * Muestra un campo de texto deshabilitado que representa la zona de respuesta del alumno,
     * junto a un bloque resaltado con la respuesta correcta.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.FLEX_ROW).append("\">")
          .append("<strong style=\"margin-right: 15px;\">Respuesta:</strong>")
          .append("<input type=\"text\" disabled style=\"").append(HtmlConstants.INPUT_BASE).append(" width: 300px;\">")
          .append("</div>");
          
        String correctAnswer = "";
        for (Answer a : answers) {
            if ("100".equals(a.getFraction())) {
                correctAnswer = processPluginFiles(a.getText());
                break;
            }
        }
        
        if (!correctAnswer.isEmpty()) {
            sb.append("<div style=\"").append(HtmlConstants.FEEDBACK_WARNING).append("\">")
              .append("La respuesta correcta es: <strong>").append(correctAnswer).append("</strong>")
              .append("</div>");
        }
        
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