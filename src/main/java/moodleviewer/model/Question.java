/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import moodleviewer.util.HtmlConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta que representa una pregunta del banco de preguntas de Moodle.
 * Define los atributos comunes, la infraestructura HTML compartida y los métodos 
 * abstractos que cada subclase debe implementar.
 */
public abstract class Question {
	
    protected String type;
    protected String name;
    protected String text;
    protected String defaultGrade;
    protected String penalty;
    protected String generalFeedback;
    protected List<MoodleFile> files = new ArrayList<>();

    public Question(String type, String name, String text, String defaultGrade, String penalty) {
        this.type = type;
        this.name = name;
        this.text = text;
        this.defaultGrade = defaultGrade;
        this.penalty = penalty;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getText() { return text; }
    public String getDefaultGrade() { return defaultGrade; }
    public String getPenalty() { return penalty; }
    public String getGeneralFeedback() { return generalFeedback; }
    public List<MoodleFile> getFiles() { return files; }

    public void setName(String name) { this.name = name; }
    public void setGeneralFeedback(String generalFeedback) { this.generalFeedback = generalFeedback; }
    public void setFiles(List<MoodleFile> files) { this.files = files; }

    /**
     * Permite sustituir el enunciado de la pregunta tras su construcción.
     * Pensado principalmente para los importadores (ej. {@code GIFTParser}), que necesitan
     * sanear el HTML original (por ejemplo, reescribir imágenes Base64 incrustadas como
     * referencias {@code @@PLUGINFILE@@}) una vez ya se ha creado el objeto Question concreto,
     * sin tener que reconstruir la instancia completa perdiendo el resto de sus atributos.
     *
     * @param text nuevo enunciado en HTML.
     */
    public void setText(String text) { this.text = text; }

    public abstract String getDetails();
    public abstract void accept(QuestionVisitor visitor);

    /**
     * Utiliza JSoup para buscar imágenes nativas de Moodle (@@PLUGINFILE@@) y sustituirlas
     * dinámicamente inyectando su código Base64, evitando que fallen por atributos desordenados.
     */
    public String processPluginFiles(String html) {
        if (html == null || html.isEmpty()) return "";
        if (files == null || files.isEmpty()) return html;

        Document doc = Jsoup.parseBodyFragment(html);
        Elements images = doc.select("img");

        for (Element img : images) {
            String src = img.attr("src");
            if (src.contains("@@PLUGINFILE@@/")) {
                String filename = src.substring(src.lastIndexOf("/") + 1);
                try { filename = java.net.URLDecoder.decode(filename, "UTF-8"); } catch (Exception e) {}
                
                for (MoodleFile mf : files) {
                    if (mf.name.equals(filename) && mf.content != null) {
                        String mimeType = "image/" + (filename.toLowerCase().endsWith(".png") ? "png" : "jpeg");
                        // Sustituimos el src por la imagen en Base64
                        img.attr("src", "data:" + mimeType + ";base64," + mf.content.replaceAll("\\s+", ""));
                        break;
                    }
                }
            }
        }
        return doc.body().html();
    }

    protected String getMoodleHeader() {
        String safeText = processPluginFiles(text);
        return "<!DOCTYPE html><html>" +
               "<head><meta charset=\"UTF-8\"></head>" +
               "<body style=\"" + HtmlConstants.HTML_BODY + "\">" +
               "  <div style=\"" + HtmlConstants.HTML_CARD + "\">" +
               "    <div style=\"" + HtmlConstants.HEADER_FLEX + "\">" +
               "      <strong style=\"" + HtmlConstants.TITLE + "\">" + name + "</strong>" +
               "      <span style=\"" + HtmlConstants.BADGE + "\">" + type.toUpperCase() + "</span>" +
               "    </div>" +
               "    <div style=\"" + HtmlConstants.TEXT_BASE + "\">" + safeText + "</div>";
    }

    protected String getMoodleFooter() {
        StringBuilder sb = new StringBuilder();
        if (generalFeedback != null && !generalFeedback.isEmpty()) {
            sb.append("<div style=\"").append(HtmlConstants.FEEDBACK_GENERAL).append("\">")
              .append("<strong>Retroalimentación general:</strong><br>")
              .append(processPluginFiles(generalFeedback))
              .append("</div>");
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }
}