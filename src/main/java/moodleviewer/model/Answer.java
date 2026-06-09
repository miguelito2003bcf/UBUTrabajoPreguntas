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
 * Clase que representa una respuesta individual asociada a una pregunta de Moodle.
 */
public class Answer {
	
    private String fraction;
    private String text;
    private String feedback;

    /**
     * Construye una nueva respuesta con todos sus atributos.
     * 
     * @param fraction fracción de puntuación en forma de cadena.
     * @param text texto de la respuesta.
     * @param feedback retroalimentación asociada a esta respuesta.
     */
    public Answer(String fraction, String text, String feedback) {
        this.fraction = fraction;
        this.text = text;
        this.feedback = feedback;
    }

    public String getFraction() { return fraction; }
    public String getText() { return text; }
    public String getFeedback() { return feedback; }
}