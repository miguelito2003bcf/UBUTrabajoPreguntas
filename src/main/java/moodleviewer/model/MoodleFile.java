/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer.model;

/**
 * Representa un fichero adjunto embebido en una pregunta de Moodle.
 *
 * PUNTO 27: campos encapsulados como privados con getters públicos,
 * igual que el resto del modelo.
 */
public class MoodleFile {

    private final String name;
    private final String path;
    private final String encoding;
    private final String content;

    /**
     * @param name     nombre del fichero.
     * @param path     ruta virtual dentro de Moodle (p.ej. {@code @@PLUGINFILE@@/img.png}).
     * @param encoding tipo de codificación del contenido (normalmente {@code "base64"}).
     * @param content  contenido del fichero codificado en Base64.
     */
    public MoodleFile(String name, String path, String encoding, String content) {
        this.name     = name;
        this.path     = path;
        this.encoding = encoding;
        this.content  = content;
    }

    public String getName()     { return name; }
    public String getPath()     { return path; }
    public String getEncoding() { return encoding; }
    public String getContent()  { return content; }
}