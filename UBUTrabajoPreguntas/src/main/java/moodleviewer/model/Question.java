package moodleviewer.model;

import java.util.ArrayList;
import java.util.List;

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
    public void setName(String name) { this.name = name; }
    public String getText() { return text; }
    public String getDefaultGrade() { return defaultGrade; }
    public String getPenalty() { return penalty; }
    
    public String getGeneralFeedback() { return generalFeedback; }
    public void setGeneralFeedback(String generalFeedback) { this.generalFeedback = generalFeedback; }

    public List<MoodleFile> getFiles() { return files; }
    public void setFiles(List<MoodleFile> files) { this.files = files; }

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

    public abstract String getDetails();

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
               "<body style=\"padding: 20px; min-height: 100vh; box-sizing: border-box;\">" +
               "  <div style=\"background-color: #fff; border: 1px solid #dee2e6; border-radius: 4px; padding: 25px; box-shadow: 0 1px 3px rgba(0,0,0,.05);\">" +
               "    <div style=\"border-bottom: 1px solid #eee; padding-bottom: 15px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;\">" +
               "      <strong style=\"color: #1177d1; font-size: 1.2rem;\">" + name + "</strong>" +
               "      <span style=\"background-color: #6c757d; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;\">" + type.toUpperCase() + "</span>" +
               "    </div>" +
               "    <div style=\"font-size: 15px; margin-bottom: 25px; color: #212529; line-height: 1.5;\">" + safeText + "</div>";
    }

    protected String getMoodleFooter() {
        StringBuilder sb = new StringBuilder();
        if (generalFeedback != null && !generalFeedback.isEmpty()) {
            sb.append("<div style=\"margin-top: 25px; padding: 15px; background-color: #d1ecf1; border: 1px solid #bee5eb; border-radius: 4px; color: #0c5460; font-size: 14px;\">")
              .append("<strong>Retroalimentación general:</strong><br>")
              .append(processPluginFiles(generalFeedback))
              .append("</div>");
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }
}