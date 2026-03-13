package moodleviewer.model;

public class ClozeQuestion extends Question {
    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
        return getBasicDetailsHtml() + "<p style='font-family: Arial; font-size: 13px; color: #7f8c8d;'><i>(Pregunta anidada tipo Cloze)</i></p>";
    }
}