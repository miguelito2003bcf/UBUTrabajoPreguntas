package moodleviewer.model;

public interface QuestionVisitor {
    void visit(MultichoiceQuestion question);
    void visit(TrueFalseQuestion question);
    void visit(ShortAnswerQuestion question);
    void visit(NumericalQuestion question);
    void visit(MatchingQuestion question);
    void visit(ClozeQuestion question);
    void visit(GenericQuestion question);
}