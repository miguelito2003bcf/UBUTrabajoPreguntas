package moodleviewer.model;

/**
 * Interfaz del patrón de diseño Visitor para el procesamiento de los
 * distintos tipos de pregunta de Moodle. Cada subclase concreta de Question 
 * implementa el método accept invocando el método visit correspondiente a su tipo.
 */
public interface QuestionVisitor {
    void visit(MultichoiceQuestion question);
    void visit(TrueFalseQuestion question);
    void visit(ShortAnswerQuestion question);
    void visit(NumericalQuestion question);
    void visit(MatchingQuestion question);
    void visit(ClozeQuestion question);
    void visit(GenericQuestion question);
}