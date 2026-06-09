/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

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