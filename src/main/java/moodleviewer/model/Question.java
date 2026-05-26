package moodleviewer.model;

import moodleviewer.util.HtmlConstants;
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

    /**
     * Construye una pregunta con los atributos comunes a todos los tipos.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado en HTML.
     * @param defaultGrade calificación por defecto.
     * @param penalty fracción de penalización.
     */
    public Question(String type, String name, String text, String defaultGrade, String penalty) {
        this.type = type;
        this.name = name;
        this.text = text;
        this.defaultGrade = defaultGrade;
        this.penalty = penalty;
    }
    
    public String getType() { return type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getText() { return text; }
    public String getDefaultGrade() { return defaultGrade; }
    public String getPenalty() { return penalty; }    
    public String getGeneralFeedback() { return generalFeedback; }
    public void setGeneralFeedback(String generalFeedback) { this.generalFeedback = generalFeedback; }
    public List<MoodleFile> getFiles() { return files; }
    public void setFiles(List<MoodleFile> files) { this.files = files; }

    /**
     * Resuelve las referencias @@PLUGINFILE@@ del HTML sustituyéndolas por URIs de datos Base64 inline, para que las
     * imágenes se muestren en el WebView sin necesidad de acceso al servidor de Moodle.
     * 
     * @param html cadena HTML con posibles referencias @@PLUGINFILE@@
     * @return HTML con las referencias sustituidas por URIs.
     */
    public String processPluginFiles(String html) {
        if (html == null || html.isEmpty()) return "";
        if (files == null || files.isEmpty()) return html;
        
        String processed = html;
        for (MoodleFile f : files) {
            if (f.content != null && !f.content.isEmpty()) {
                String mimeType = "image/png"; 
                if (f.name.toLowerCase().endsWith(".jpg") || f.name.toLowerCase().endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (f.name.toLowerCase().endsWith(".gif")) mimeType = "image/gif";
                else if (f.name.toLowerCase().endsWith(".svg")) mimeType = "image/svg+xml";
                
                String base64URI = "data:" + mimeType + ";base64," + f.content.replace("\n", "").replace("\r", "");
                processed = processed.replace("@@PLUGINFILE@@" + f.path + f.name, base64URI);
            }
        }
        return processed;
    }

    /**
     * Genera el documento HTML completo con los detalles de esta pregunta, para ser cargado en el WebView del panel de detalle.
     * 
     * @return cadena con el documento HTML completo.
     */
    public abstract String getDetails();

    /**
     * Genera la cabecera HTML común, con la configuración MathJax, estilos CSS, nombre y tipo de la pregunta y el enunciado resuelto.
     * 
     * @return cadena HTML desde el <html> hasta el inicio del área de respuestas.
     */
    protected String getMoodleHeader() {
        String safeText = processPluginFiles(text != null ? text : "");
        
        return "<html><head><meta charset=\"UTF-8\">" +
               "<script>" +
               "  MathJax = {" +
               "    tex: {" +
               "      inlineMath: [['$$','$$'], ['$','$'], ['\\\\(','\\\\)']]," +
               "      displayMath: [['\\\\[','\\\\]']]" + 
               "    }" +
               "  };" +
               "</script>" +
               "<script id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js\"></script>" +
               "<style>" +
               "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; color: #343a40; padding: 20px; margin: 0; background-color: #f8f9fa; }" +
               "</style></head>" +
               "<body style=\"" + HtmlConstants.HTML_BODY + "\">" +
               "  <div style=\"" + HtmlConstants.HTML_CARD + "\">" +
               "    <div style=\"" + HtmlConstants.HEADER_FLEX + "\">" +
               "      <strong style=\"" + HtmlConstants.TITLE + "\">" + name + "</strong>" +
               "      <span style=\"" + HtmlConstants.BADGE + "\">" + type.toUpperCase() + "</span>" +
               "    </div>" +
               "    <div style=\"" + HtmlConstants.TEXT_BASE + "\">" + safeText + "</div>";
    }

    /**
     * Genera el pie HTML común, con la retroalimentación general y el cierre de los elementos abiertos por la cabecera.
     * 
     * @return cadena HTML con la retroalimentación y el cierre del documento.
     */
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

    /**
     * Método creado para cumplir con el patrón de diseño Visitor.
     * 
     * @param visitor visitante que procesará esta pregunta.
     */
    public abstract void accept(QuestionVisitor visitor);
}