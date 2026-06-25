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

    /**
     * Permite sustituir el texto del enunciado tras la construcción. Pensado para los
     * importadores (ej. {@code GIFTParser}), que necesitan reescribir imágenes Base64
     * incrustadas como referencias {@code @@PLUGINFILE@@} una vez extraído el fichero
     * a {@link MoodleFile}.
     *
     * @param questionText nuevo texto del enunciado.
     */
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    /**
     * Permite sustituir el texto de la respuesta tras la construcción, por el mismo
     * motivo que {@link #setQuestionText(String)}.
     *
     * @param answerText nuevo texto de la respuesta.
     */
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}