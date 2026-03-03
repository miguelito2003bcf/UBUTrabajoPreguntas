package moodleviewer.model;

public class ClozeQuestion extends Question {
    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        return getBasicDetails() + "\n\n--- Pregunta Anidada (Cloze) ---\n" +
               "Las respuestas están incrustadas en el texto usando la sintaxis { ... }.";
    }
}