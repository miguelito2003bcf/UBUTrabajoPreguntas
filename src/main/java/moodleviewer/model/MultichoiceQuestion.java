package moodleviewer.model;
import java.util.ArrayList;
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
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"margin-bottom: 15px; font-size: 14px; font-weight: bold; color: #333;\">Seleccione una").append(isSingleAnswer() ? ":" : " o más de una:").append("</div>");
        
        List<String> correctAnswers = new ArrayList<>();
        
        sb.append("<div style=\"margin-left: 5px;\">");
        
        // Creamos el desplegable real
        sb.append("<select style=\"padding: 10px; border: 1px solid #ced4da; border-radius: 4px; width: 100%; max-width: 600px; background-color: #ffffff; font-size: 15px; color: #495057; cursor: pointer;\">");
        sb.append("<option value=\"\" disabled selected>Haz clic para ver las opciones y puntuaciones...</option>");
        
        for (Answer a : answers) {
            double fractionVal = 0.0;
            try {
                if (a.getFraction() != null && !a.getFraction().isEmpty()) {
                    fractionVal = Double.parseDouble(a.getFraction());
                }
            } catch (NumberFormatException e) {
                // Ignorar error de parseo y mantener a 0
            }
            
            // Si es positiva, es correcta (total o parcialmente)
            if (fractionVal > 0) {
                correctAnswers.add(processPluginFiles(a.getText()));
            }
            
            // Formatear la fracción a porcentaje limpio (ej. 33.333 -> 33.3%, 100.0 -> 100%)
            String formattedFraction;
            if (fractionVal == Math.floor(fractionVal)) {
                formattedFraction = String.format("%.0f%%", fractionVal);
            } else {
                formattedFraction = String.format("%.1f%%", fractionVal).replace(",", ".");
            }
            
            // Limpiamos las etiquetas HTML para que se vea bien dentro del <option>
            String plainText = a.getText() != null ? a.getText().replaceAll("<[^>]+>", "").trim() : "";
            if (plainText.isEmpty()) {
                plainText = "[Imagen o contenido multimedia]";
            }
            
            // Añadimos solo el porcentaje limpio al final de la opción
            sb.append("<option>").append(plainText).append(" (").append(formattedFraction).append(")</option>");
        }
        sb.append("</select>");
        sb.append("</div>");
        
        // Mantenemos la visualización completa de las respuestas correctas abajo
        if (!correctAnswers.isEmpty()) {
            sb.append("<div style=\"margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;\">")
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