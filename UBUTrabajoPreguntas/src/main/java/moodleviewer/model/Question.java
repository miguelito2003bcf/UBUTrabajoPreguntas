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
            if ("base64".equals(f.encoding) && f.content != null) {
                String mimeType = "image/png"; 
                String nameLower = f.name.toLowerCase();
                if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (nameLower.endsWith(".gif")) mimeType = "image/gif";
                else if (nameLower.endsWith(".svg")) mimeType = "image/svg+xml";

                String base64URI = "data:" + mimeType + ";base64," + f.content.replace("\n", "").replace("\r", "");
                
                processed = processed.replace("@@PLUGINFILE@@" + f.path + f.name, base64URI);
                processed = processed.replace("@@PLUGINFILE@@/" + f.name, base64URI);
            }
        }
        return processed;
    }

    protected String getBasicDetailsHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; padding: 10px;'>");
        sb.append("<h3 style='color: #2c3e50; margin-bottom: 5px;'>").append(name).append("</h3>");
        sb.append("<span style='background-color: #e0e0e0; padding: 3px 8px; border-radius: 12px; font-size: 12px;'>").append(type.toUpperCase()).append("</span>");
        sb.append("<hr style='border: 0; height: 1px; background: #ddd; margin: 15px 0;'>");
        
        String safeText = processPluginFiles(text);
        sb.append("<p style='font-size: 14px; line-height: 1.5;'>").append(safeText).append("</p>");
        
        if (generalFeedback != null && !generalFeedback.isEmpty()) {
            sb.append("<div style='margin-top: 15px; padding: 10px; background-color: #e8f4f8; border-left: 4px solid #3498db;'>");
            sb.append("<b>Feedback general:</b><br>").append(processPluginFiles(generalFeedback));
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    public abstract String getDetails();
}