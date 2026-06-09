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
 * Clase que representa un fichero adjunto embebido en una pregunta de Moodle.
 */
public class MoodleFile {
	
    public String name, path, encoding, content;
    
    /**
     * Construye un MoodleFile con todos sus atributos.
     * 
     * @param name nombre del fichero.
     * @param path ruta virtual dentro de Moodle.
     * @param encoding tipo de codificación del contenido.
     * @param content contenido del fichero codificado en Base64.
     */
    public MoodleFile(String name, String path, String encoding, String content) {
        this.name = name; 
        this.path = path; 
        this.encoding = encoding; 
        this.content = content;
    }
}