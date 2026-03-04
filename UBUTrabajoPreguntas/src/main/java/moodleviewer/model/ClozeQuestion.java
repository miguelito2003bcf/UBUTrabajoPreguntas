package moodleviewer.model;

public class ClozeQuestion extends Question {
    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        return getBasicDetailsHtml() + 
               "<div style='font-family: Arial, sans-serif; font-size: 13px; margin-top: 15px;'>" +
               "<h4 style='color: #2980b9;'>--- Pregunta Anidada (Cloze) ---</h4>" +
               "Las respuestas están incrustadas en el texto usando la sintaxis { ... }." +
               "</div>";
    }
}