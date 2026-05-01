package moodleviewer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClozeQuestion extends Question {
	
    private static final Pattern CLOZE_TOKEN_PAT = Pattern.compile(
        "\\{([0-9]*):" +
        "(NUMERICAL|NM" +
        "|MULTICHOICE_VS|MCVS|MULTICHOICE_HS|MCHS" +
        "|MULTICHOICE_V|MCV|MULTICHOICE_H|MCH" +
        "|MULTICHOICE_S|MCS|MULTICHOICE|MC" +
        "|SHORTANSWER_C|SAC|MWC|SHORTANSWER|SA|MW" +
        "|MULTIRESPONSE_HS|MRHS|MULTIRESPONSE_H|MRH" +
        "|MULTIRESPONSE_S|MRS|MULTIRESPONSE|MR)" +
        ":(.+?)(?<!\\\\)\\}",
        Pattern.DOTALL
    );

    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    @Override
    public String getDetails() {
    	
        String baseText = processPluginFiles(text != null ? text : "");
        String header = super.getMoodleHeader();
        String textDivToRemove = "<div style=\"font-size: 15px; margin-bottom: 25px; color: #212529; line-height: 1.5;\">" + baseText + "</div>";
        header = header.replace(textDivToRemove, "");
        StringBuilder sb = new StringBuilder(header);
        String highlightedText = highlightClozeSyntax(baseText);
        String renderedText = renderCloze(baseText);
        
        sb.append("<div style=\"margin-bottom: 25px;\">")
          .append("<div style=\"font-size: 14px; font-weight: bold; color: #495057; margin-bottom: 8px;\">Sintaxis Cloze:</div>")
          .append("<div style=\"padding: 15px; background-color: #ffffff; border: 1px dashed #adb5bd; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;\">")
          .append(highlightedText)
          .append("</div></div>");
        
        sb.append("<div>")
          .append("<div style=\"font-size: 14px; font-weight: bold; color: #495057; margin-bottom: 8px;\">Vista previa del alumno:</div>")
          .append("<div style=\"padding: 15px; background-color: #fcfcfc; border: 1px solid #dee2e6; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;\">")
          .append(renderedText)
          .append("</div></div>");
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
    
    @Override
    public void accept(QuestionVisitor visitor) {
        visitor.visit(this);
    }

    private String highlightClozeSyntax(String html) {
        Matcher m = CLOZE_TOKEN_PAT.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(0);
            String replacement = "<span style=\"background-color: #e9ecef; color: #0056b3; font-family: 'Courier New', monospace; padding: 2px 6px; border-radius: 4px; font-weight: bold;\">" 
                               + escapeHtmlText(token) 
                               + "</span>";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String renderCloze(String html) {
        Matcher m = CLOZE_TOKEN_PAT.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String typeKey = m.group(2).toUpperCase();
            String rawAlts = m.group(3);

            List<ClozeAlt> alts = parseClozeAlternatives(rawAlts);
            String widget = buildClozeWidget(typeKey, alts);
            m.appendReplacement(sb, Matcher.quoteReplacement(widget));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String buildClozeWidget(String typeKey, List<ClozeAlt> alts) {
        StringBuilder widget = new StringBuilder();
        
        if (typeKey.matches("SHORTANSWER|SA|MW|SHORTANSWER_C|SAC|MWC|NUMERICAL|NM")) {
            String correct = alts.stream().filter(a -> a.fraction == 1.0).map(a -> a.text.replaceAll("<[^>]+>", "")).findFirst()
                .orElseGet(() -> alts.stream().max(Comparator.comparingDouble(a -> a.fraction)).map(a -> a.text.replaceAll("<[^>]+>", "")).orElse(""));
            
            int size = Math.max(10, Math.min(correct.length() + 6, 40));
            widget.append("<input type='text' disabled size='").append(size).append("' placeholder='").append(escapeHtmlAttr(correct))
                  .append("' style='border:1px solid #ced4da; border-radius: 4px; padding:4px 8px; font-style:italic; color:#495057; background-color:#e9ecef;' />");
        }
        else if (typeKey.matches("MULTICHOICE|MC|MULTICHOICE_S|MCS")) {
            widget.append("<select disabled style='border:1px solid #ced4da; border-radius:4px; padding:4px; background-color:#e9ecef;'>")
                  .append("<option value=''>Selecciona...</option>");
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction == 1.0;
                widget.append("<option ").append(correct ? "selected" : "").append(" style='").append(correct ? "color:#15803d;font-weight:bold;" : "color:#333;")
                      .append("'>").append(correct ? "✓ " : "").append(a.text).append("</option>");
            }
            widget.append("</select>");
        }
        else if (typeKey.matches("MULTICHOICE_V|MCV|MULTICHOICE_VS|MCVS|MULTIRESPONSE|MR|MULTIRESPONSE_S|MRS")) {
            String inputType = typeKey.contains("MULTICHOICE") ? "radio" : "checkbox";
            widget.append("<span style='display:inline-block; vertical-align:top; border:1px dashed #ccc; padding:5px; border-radius:4px;'>");
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction > 0;
                widget.append("<label style='display:block; margin-bottom:2px;'><input type='").append(inputType).append("' disabled ")
                      .append(correct ? "checked" : "").append("> <small style='color:#c00;'>(").append(formatFraction(a.fraction)).append(")</small> ")
                      .append(a.text).append("</label>");
            }
            widget.append("</span>");
        }
        else if (typeKey.matches("MULTICHOICE_H|MCH|MULTICHOICE_HS|MCHS|MULTIRESPONSE_H|MRH|MULTIRESPONSE_HS|MRHS")) {
            String inputType = typeKey.contains("MULTICHOICE") ? "radio" : "checkbox";
            widget.append("<span style='display:inline-block; vertical-align:top; border:1px dashed #ccc; padding:2px 8px; border-radius:4px;'>");
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction > 0;
                widget.append("<label style='margin-right:15px;'><input type='").append(inputType).append("' disabled ")
                      .append(correct ? "checked" : "").append("> <small style='color:#c00;'>(").append(formatFraction(a.fraction)).append(")</small> ")
                      .append(a.text).append("</label>");
            }
            widget.append("</span>");
        }

        return widget.toString();
    }

    private static List<ClozeAlt> parseClozeAlternatives(String raw) {
        List<ClozeAlt> result = new ArrayList<>();
        String[] parts = raw.split("(?<!\\\\)~");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            double fraction = 0.0;
            String text = "";

            if (part.startsWith("=")) {
                fraction = 1.0;
                part = part.substring(1);
            } else {
                Matcher pm = Pattern.compile("^%(-?[0-9]+(?:[.,][0-9]*)?)%").matcher(part);
                if (pm.find()) {
                    fraction = Double.parseDouble(pm.group(1).replace(',', '.')) / 100.0;
                    part = part.substring(pm.end());
                }
            }

            int hashIdx = -1;
            for (int i = 0; i < part.length(); i++) {
                if (part.charAt(i) == '#' && (i == 0 || part.charAt(i - 1) != '\\')) {
                    hashIdx = i; break;
                }
            }
            
            if (hashIdx >= 0) {
                text = part.substring(0, hashIdx);
            } else {
                text = part;
            }
            
            text = text.replace("\\}", "}").replace("\\~", "~").replace("\\=", "=");
            result.add(new ClozeAlt(fraction, text));
        }
        return result;
    }

    private static String formatFraction(double f) {
        double pct = f * 100.0;
        if (pct == Math.floor(pct)) return (int) pct + "%";
        return String.format(Locale.US, "%.1f%%", pct);
    }

    private static String escapeHtmlAttr(String text) {
        return text.replace("&", "&amp;").replace("'", "&#39;").replace("\"", "&quot;");
    }

    private static String escapeHtmlText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static class ClozeAlt {
        final double fraction;
        final String text;

        ClozeAlt(double fraction, String text) {
            this.fraction = fraction;
            this.text = text;
        }
    }
}