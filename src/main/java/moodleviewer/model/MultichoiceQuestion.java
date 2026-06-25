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
import org.jsoup.Jsoup;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase extendida de Question que representa una pregunta de opción múltiple de Moodle.
 */
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
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append("\">Opciones:</div>");
        sb.append("<div style=\"").append(HtmlConstants.FLEX_ROW).append("\">")
          .append("<select disabled style=\"").append(HtmlConstants.MULTICHOICE_SELECT).append("\">");
        
        List<String> correctAnswers = new ArrayList<>();
        
        for (Answer a : answers) {
            double fractionVal = 0;
            try {
                fractionVal = Double.parseDouble(a.getFraction());
                if (fractionVal > 0) {
                    correctAnswers.add(processPluginFiles(a.getText()) + " (" + fractionVal + "%)");
                }
            } catch (NumberFormatException ignored) {}
            
            String formattedFraction;
            if (fractionVal == Math.floor(fractionVal)) {
                formattedFraction = String.format("%.0f%%", fractionVal);
            } else {
                formattedFraction = String.format(java.util.Locale.US, "%.1f%%", fractionVal);
            }
            
            // EXTRACCIÓN SEGURA DE TEXTO PLANO CON JSOUP
            String plainText = a.getText() != null ? Jsoup.parse(a.getText()).text().trim() : "";
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
    
    @Override
    public void accept(QuestionVisitor visitor) {
        visitor.visit(this);
    }
}