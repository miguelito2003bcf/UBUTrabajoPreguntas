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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase extendida de Question que representa una pregunta anidada (Cloze) de Moodle.
 */
public class ClozeQuestion extends Question {
	
    public static boolean MODO_PREVIA_ALUMNO = false;

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

    /**
     * Construye una pregunta Cloze. Las subpreguntas se extraen dinámicamente del enunciado en
     * el momento de la visualización.
     * 
     * @param type tipo de Moodle.
     * @param name nombre de la pregunta.
     * @param text enunciado completo con la sintaxis Cloze embebida.
     * @param grade calificación por defecto.
     * @param penalty fracción de penalización.
     */
    public ClozeQuestion(String type, String name, String text, String grade, String penalty) {
        super(type, name, text, grade, penalty);
    }

    /**
     * Elimina el bloque de texto generado por getMoodleHeader y lo reemplaza con su propia representación,
     * que varía según el modo de previsualización.
     */
    @Override
    public String getDetails() {
        String baseText = processPluginFiles(text != null ? text : "");
        String header = super.getMoodleHeader();
        
        String textDivToRemove = "<div style=\"" + HtmlConstants.TEXT_BASE + "\">" + baseText + "</div>";
        header = header.replace(textDivToRemove, "");
        
        StringBuilder sb = new StringBuilder(header);

        if (!MODO_PREVIA_ALUMNO) {
            String highlightedText = highlightClozeSyntax(baseText);
            sb.append("<div style=\"").append(HtmlConstants.CLOZE_SECTION_WRAPPER).append("\">")
              .append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append(" margin-bottom: 8px;\">Sintaxis Cloze:</div>")
              .append("<div style=\"").append(HtmlConstants.CLOZE_CODE_BLOCK).append("\">")
              .append(highlightedText)
              .append("</div></div>");
        } else {
            String renderedText = renderCloze(baseText);
            sb.append("<div>")
              .append("<div style=\"").append(HtmlConstants.LABEL_BOLD).append(" margin-bottom: 8px;\">Vista previa del alumno:</div>")
              .append("<div style=\"").append(HtmlConstants.CLOZE_RENDER_BLOCK).append("\">")
              .append(renderedText)
              .append("</div></div>");
        }
        
        sb.append(getMoodleFooter());
        return sb.toString();
    }
    
    /**
     * Método creado para cumplir con el patrón de diseño Visitor.
     * 
     * @param visitor visitante que procesará esta pregunta.
     */
    @Override
    public void accept(QuestionVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Envuelve cada token Cloze con un subrayado de color azul para facilitar la revisión
     * de la sintaxis por el profesor.
     * 
     * @param html enunciado HTML con tokens Cloze.
     * @return HTML con los tokens Cloze resaltados visualmente. 
     */
    private String highlightClozeSyntax(String html) {
        Matcher m = CLOZE_TOKEN_PAT.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(0);
            String replacement = "<span style=\"" + HtmlConstants.CLOZE_HIGHLIGHT + "\">" 
                               + escapeHtmlText(token) 
                               + "</span>";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Sustituye cada token Cloze por el widget HTML interactivo correspondiente simulando la interfaz
     * que vería el alumno en Moodle.
     * 
     * @param html enunciado HTML con tokens Cloze.
     * @return HTML con los tokens sustituidos.
     */
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

    /**
     * Construye el widget HTML apropiado para un subproblema Cloze según su tipo.
     * 
     * @param typeKey clave del tipo en mayúsculas.
     * @param alts lista de alternativas parseadas.
     * @return cadena HTML con el widget interactivo deshabilitado.
     */
    private String buildClozeWidget(String typeKey, List<ClozeAlt> alts) {
        StringBuilder widget = new StringBuilder();
        
        if (typeKey.matches("SHORTANSWER|SA|MW|SHORTANSWER_C|SAC|MWC|NUMERICAL|NM")) {
            String correct = alts.stream().filter(a -> a.fraction == 1.0).map(a -> stripHtmlTags(a.text)).findFirst()
                .orElseGet(() -> alts.stream().max(Comparator.comparingDouble(a -> a.fraction)).map(a -> stripHtmlTags(a.text)).orElse(""));
            
            int size = Math.max(10, Math.min(correct.length() + 6, 40));
            widget.append("<input type='text' disabled size='").append(size).append("' placeholder='").append(escapeHtmlAttr(correct))
                  .append("' style='").append(HtmlConstants.CLOZE_TEXT_INPUT).append("' />");
        }
        else if (typeKey.matches("MULTICHOICE|MC|MULTICHOICE_S|MCS")) {
            
        	widget.append("<select style=\"").append(HtmlConstants.CLOZE_SELECT).append("\">")
                  .append("<option value=\"\" disabled selected>Elegir...</option>");
            
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction == 1.0;
                
                String plainText = stripHtmlTags(a.text).trim();
                if (plainText.isEmpty()) {
                    plainText = "[Imagen o contenido multimedia]";
                }
                
                String formattedFraction = formatFraction(a.fraction);
                String optionStyle = correct ? HtmlConstants.CLOZE_OPTION_CORRECT : HtmlConstants.CLOZE_OPTION_DEFAULT;
                
                widget.append("<option ").append(correct ? "selected" : "").append(" style=\"").append(optionStyle).append("\">")
                      .append(correct ? "✓ " : "").append(plainText).append(" (").append(formattedFraction).append(")</option>");
            }
            widget.append("</select>");
            
        }
        else if (typeKey.matches("MULTICHOICE_V|MCV|MULTICHOICE_VS|MCVS|MULTIRESPONSE|MR|MULTIRESPONSE_S|MRS")) {
            String inputType = typeKey.contains("MULTICHOICE") ? "radio" : "checkbox";
            widget.append("<span style='").append(HtmlConstants.CLOZE_OPTIONS_WRAPPER_VERTICAL).append("'>");
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction > 0;
                widget.append("<label style='").append(HtmlConstants.CLOZE_OPTION_LABEL_VERTICAL).append("'><input type='").append(inputType).append("' disabled ")
                      .append(correct ? "checked" : "").append("> <small style='").append(HtmlConstants.CLOZE_FRACTION_LABEL).append("'>(").append(formatFraction(a.fraction)).append(")</small> ")
                      .append(a.text).append("</label>");
            }
            widget.append("</span>");
        }
        else if (typeKey.matches("MULTICHOICE_H|MCH|MULTICHOICE_HS|MCHS|MULTIRESPONSE_H|MRH|MULTIRESPONSE_HS|MRHS")) {
            String inputType = typeKey.contains("MULTICHOICE") ? "radio" : "checkbox";
            widget.append("<span style='").append(HtmlConstants.CLOZE_OPTIONS_WRAPPER_HORIZONTAL).append("'>");
            for (ClozeAlt a : alts) {
                boolean correct = a.fraction > 0;
                widget.append("<label style='").append(HtmlConstants.CLOZE_OPTION_LABEL_HORIZONTAL).append("'><input type='").append(inputType).append("' disabled ")
                      .append(correct ? "checked" : "").append("> <small style='").append(HtmlConstants.CLOZE_FRACTION_LABEL).append("'>(").append(formatFraction(a.fraction)).append(")</small> ")
                      .append(a.text).append("</label>");
            }
            widget.append("</span>");
        }

        return widget.toString();
    }

    /**
     * Parsea la cadena de alternativas de un token Cloze y devuelve una lista de ClozeAlt. 
     * 
     * @param raw cadena con las alternativas tal como aparece en la sintaxis Cloze.
     * @return lista de alternativas parseadas.
     */
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

    /**
     * Formatea un valor de fracción decimal como porcentake legible. Si es entero no muestra
     * decimales.
     * 
     * @param f fracción en rango (0.0, 1.0).
     * @return cadena de porcentaje.
     */
    private static String formatFraction(double f) {
        double pct = f * 100.0;
        if (pct == Math.floor(pct)) return (int) pct + "%";
        return String.format(Locale.US, "%.1f%%", pct);
    }

    /**
     * Extrae el texto plano de un fragmento HTML, eliminando cualquier etiqueta de forma segura.
     * Sustituye al antiguo regex "&lt;[^&gt;]+&gt;" (frágil ante HTML mal formado, comentarios o
     * atributos con '&gt;'), delegando el parseo real en JSoup.
     * 
     * @param html fragmento HTML de origen, puede ser null.
     * @return texto plano sin etiquetas, o cadena vacía si html es null.
     */
    private static String stripHtmlTags(String html) {
        if (html == null) return "";
        return Jsoup.parse(html).text();
    }

    /**
     * Escapa caracteres especiales HTML para uso seguro en atributos.
     * 
     * @param text texto a escapar.
     * @return texto con &, ' y " escapados.
     */
    private static String escapeHtmlAttr(String text) {
        return text.replace("&", "&amp;").replace("'", "&#39;").replace("\"", "&quot;");
    }

    /**
     * Escapa caracteres especiales HTML para uso seguro en contenido de texto.
     * 
     * @param text texto a escapar.
     * @return texto con &, <, >, " y ' escapados.
     */
    private static String escapeHtmlText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Clase creada para trabajar con la fracción de puntuación y texto de respuesta de una pregunta Cloze. 
     * Al tener esta clase podemos obtener objetos ClozeAlt en donde tenemos acceso a los dos valores.
     */
    private static class ClozeAlt {
    	
        final double fraction;
        final String text;

        /**
         * Construye un objeto ClozeAlt.
         * 
         * @param fraction calificación de la respuesta.
         * @param text texto de la respuesta.
         */
        ClozeAlt(double fraction, String text) {
            this.fraction = fraction;
            this.text = text;
        }
    }
}