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
 * Clase que representa un par pregunta-respuesta dentro de una pregunta de emparejamiento.
 */
public class MatchingPair {
	
    private String questionText;
    private String answerText;

    /**
     * Construye un par de emparejamientos con su enunciado y su respuesta.
     * 
     * @param questionText texto del enunciado.
     * @param answerText texto de la respuesta.
     */
    public MatchingPair(String questionText, String answerText) {
        this.questionText = questionText;
        this.answerText = answerText;
    }

    public String getQuestionText() { return questionText; }
    public String getAnswerText() { return answerText; }
}