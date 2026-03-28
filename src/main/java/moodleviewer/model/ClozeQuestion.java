package moodleviewer.model;

public class ClozeQuestion extends Question {
    
    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getMoodleHeader());
        
        sb.append("<div style=\"margin-top: 15px; padding: 15px; background-color: #e2e3e5; border: 1px solid #d6d8db; border-radius: 4px; font-size: 14px; color: #383d41;\">")
          .append("ℹ️ <strong>Pregunta Anidada (Cloze):</strong> Los campos de respuesta (desplegables, cajas de texto o botones) se renderizan de forma incrustada directamente dentro del enunciado superior según el código de Moodle.")
          .append("</div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
}