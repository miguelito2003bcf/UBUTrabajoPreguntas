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
import java.util.ArrayList;
import java.util.List;

/**
 * Clase extendida de Question que representa una pregunta opcióm múltiple de Moodle.
 */
public class MultichoiceQuestion extends Question {
	
    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private List<Answer> answers;

    /**
     * Construye una pregunta de opción múltiple con todos sus atributos.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado en HTML.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     * @param single true si solo admite una respuesta correcta.
     * @param shuffle true si las opciones deben barajarse.
     * @param answers lista de opciones de respuesta.
     */
    public MultichoiceQuestion(String type, String name, String text, String grade, String penalty, boolean single, boolean shuffle, List<Answer> answers) {
        super(type, name, text, grade, penalty);
        this.singleAnswer = single;
        this.shuffleAnswers = shuffle;
        this.answers = answers;
    }

    public boolean isSingleAnswer() { return singleAnswer; }
    public boolean isShuffleAnswers() { return shuffleAnswers; }
    public List<Answer> getAnswers() { return answers; }

    /**
     * Genera un desplegable con todas las opciones y sus porcentajes de calificación. Las opciones con fracción
     * positiva se consideran correctas y se listan también en un bloque de respuestas correctas al pie del panel.
     */
    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append("\">Seleccione una").append(isSingleAnswer() ? ":" : " o más de una:").append("</div>");
        
        List<String> correctAnswers = new ArrayList<>();
        sb.append("<div style=\"margin-left: 5px;\">");
        sb.append("<select style=\"").append(HtmlConstants.INPUT_BASE).append(" width: 100%; max-width: 600px; font-size: 15px; color: #495057; cursor: pointer;\">");
        sb.append("<option value=\"\" disabled selected>Haz clic para ver las opciones y puntuaciones...</option>");
        
        for (Answer a : answers) {
            double fractionVal = 0.0;
            try {
                if (a.getFraction() != null && !a.getFraction().isEmpty()) {
                    fractionVal = Double.parseDouble(a.getFraction());
                }
            } catch (NumberFormatException e) {
            }
            
            if (fractionVal > 0) {
                correctAnswers.add(processPluginFiles(a.getText()));
            }
            
            String formattedFraction;
            if (fractionVal == Math.floor(fractionVal)) {
                formattedFraction = String.format("%.0f%%", fractionVal);
            } else {
                formattedFraction = String.format("%.1f%%", fractionVal).replace(",", ".");
            }
            
            String plainText = a.getText() != null ? a.getText().replaceAll("<[^>]+>", "").trim() : "";
            if (plainText.isEmpty()) {
                plainText = "[Imagen o contenido multimedia]";
            }
            
            sb.append("<option>").append(plainText).append(" (").append(formattedFraction).append(")</option>");
        }
        sb.append("</select></div>");
        
        if (!correctAnswers.isEmpty()) {
            sb.append("<div style=\"").append(HtmlConstants.FEEDBACK_WARNING).append("\">")
              .append("La(s) respuesta(s) correcta(s):<br><ul style=\"margin-bottom: 0;\">");
            for (String ca : correctAnswers) {
                sb.append("<li style=\"margin-top: 5px;\">").append(ca).append("</li>");
            }
            sb.append("</ul></div>");
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