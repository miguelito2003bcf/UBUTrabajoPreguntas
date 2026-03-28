package moodleviewer.model;

public class MatchingPair {
    private String questionText;
    private String answerText;

    public MatchingPair(String questionText, String answerText) {
        this.questionText = questionText;
        this.answerText = answerText;
    }

    public String getQuestionText() { return questionText; }
    public String getAnswerText() { return answerText; }
}