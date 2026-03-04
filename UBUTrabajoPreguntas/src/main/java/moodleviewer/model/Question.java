package moodleviewer.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Question {
    protected String type;
    protected String name;
    protected String questionText;
    protected String defaultGrade;
    protected String penalty;
    
    // NUEVO: Lista para guardar las imágenes incrustadas
    protected List<MoodleFile> files = new ArrayList<>();

    public Question(String type, String name, String questionText, String defaultGrade, String penalty) {
        this.type = type;
        this.name = name;
        this.questionText = questionText;
        this.defaultGrade = defaultGrade;
        this.penalty = penalty;
    }

    // NUEVO: Setter para inyectar los archivos desde el Parser
    public void setFiles(List<MoodleFile> files) {
        this.files = files;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getQuestionText() { return questionText; }
    public String getDefaultGrade() { return defaultGrade; }
    public String getPenalty() { return penalty; }

    // NUEVO: Devuelve el texto base en formato HTML y procesa las imágenes
    protected String getBasicDetailsHtml() {
        String htmlText = questionText != null ? questionText : "N/A";

        // Magia: Reemplazamos la etiqueta de Moodle por la imagen real
        if (files != null) {
            for (MoodleFile file : files) {
                if ("base64".equals(file.encoding)) {
                    String mimeType = file.name.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    String dataUri = "data:" + mimeType + ";base64," + file.content;
                    htmlText = htmlText.replace("@@PLUGINFILE@@/" + file.name, dataUri);
                }
            }
        }

        // Construimos el contenedor HTML
        return "<div style='font-family: Arial, sans-serif; font-size: 14px;'>" +
               "<h3 style='color: #2c3e50;'>Tipo de pregunta: " + type + "</h3>" +
               "<b>Nombre:</b> " + name + "<br>" +
               "<b>Nota por defecto:</b> " + (defaultGrade != null ? defaultGrade : "N/A") + "<br>" +
               "<b>Penalización:</b> " + (penalty != null ? penalty : "N/A") + "<br><br>" +
               "<div style='background-color: #f9f9f9; padding: 10px; border: 1px solid #ddd;'>" + 
               "<b>Enunciado:</b><br>" + htmlText + 
               "</div>";
    }

    public abstract String getDetails();

    @Override
    public String toString() {
        return name != null && !name.isEmpty() ? name : "Pregunta (" + type + ")";
    }
}