package moodleviewer.model;

public class Answer {
    private String fraction;
    private String text;
    private String feedback;

    public Answer(String fraction, String text, String feedback) {
        this.fraction = fraction;
        this.text = text;
        this.feedback = feedback;
    }

    public String getFraction() { return fraction; }
    public String getText() { return text; }
    public String getFeedback() { return feedback; }
}