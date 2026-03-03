package moodleviewer.model;

public abstract class Question {
    protected String type;
    protected String name;
    protected String questionText;
    protected String defaultGrade;
    protected String penalty;

    public Question(String type, String name, String questionText, String defaultGrade, String penalty) {
        this.type = type;
        this.name = name;
        this.questionText = questionText;
        this.defaultGrade = defaultGrade;
        this.penalty = penalty;
    }

    public String getName() { return name; }

    // Parte común a todas las preguntas
    protected String getBasicDetails() {
        return "Tipo de pregunta: " + type + "\n" +
               "Nombre: " + name + "\n" +
               "Nota por defecto: " + (defaultGrade != null ? defaultGrade : "N/A") + "\n" +
               "Penalización: " + (penalty != null ? penalty : "N/A") + "\n\n" +
               "Enunciado:\n" + (questionText != null ? questionText.replaceAll("<[^>]*>", "") : "N/A");
    }

    // Cada hija debe definir cómo se muestra
    public abstract String getDetails();

    @Override
    public String toString() {
        return name != null && !name.isEmpty() ? name : "Pregunta (" + type + ")";
    }
}