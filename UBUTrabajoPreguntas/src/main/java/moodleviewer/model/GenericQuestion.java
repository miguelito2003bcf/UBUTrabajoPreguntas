package moodleviewer.model;

public class GenericQuestion extends Question {

    public GenericQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        // Al ser genérica, solo devuelve la parte de HTML base de la clase padre
        return getBasicDetailsHtml();
    }
}