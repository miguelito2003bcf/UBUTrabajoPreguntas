package moodleviewer.model;
import java.util.List;

public class MultichoiceQuestion extends Question {
    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private List<String> answers;

    public MultichoiceQuestion(String type, String name, String text, String grade, String penalty, boolean single, boolean shuffle, List<String> answers) {
        super(type, name, text, grade, penalty);
        this.singleAnswer = single;
        this.shuffleAnswers = shuffle;
        this.answers = answers;
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(getBasicDetails());
        sb.append("\n\n--- Opciones (Multichoice) ---\n");
        sb.append("¿Respuesta única?: ").append(singleAnswer ? "Sí" : "No").append("\n");
        sb.append("¿Barajar respuestas?: ").append(shuffleAnswers ? "Sí" : "No").append("\n\nRespuestas:\n");
        for (String a : answers) {
            sb.append("- ").append(a.replaceAll("<[^>]*>", "")).append("\n");
        }
        return sb.toString();
    }
}