package moodleviewer.model;

public class GenericQuestion extends Question {
    
    public GenericQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"margin-bottom: 10px; font-size: 14px; font-weight: bold; color: #333;\">Respuesta:</div>");
        
        // Simulamos el editor de texto HTML donde el alumno escribiría su ensayo
        sb.append("<div style=\"border: 1px solid #ced4da; border-radius: 4px; height: 150px; background-color: #e9ecef; display: flex; align-items: center; justify-content: center; color: #6c757d; font-style: italic;\">")
          .append("[ Área de editor de texto para el ensayo del alumno ]")
          .append("</div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}