package moodleviewer.parser;

import moodleviewer.model.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase creada como visitante que serializa cada tipo de pregunta de Moodle al formato LaTeX.
 * Implementa el patrón Visitor definido en QuestionVisitor.
 */
public class LaTeXExportVisitor implements QuestionVisitor {
    
	private static final Logger LOGGER = Logger.getLogger(LaTeXExportVisitor.class.getName());
    private final BufferedWriter writer;
    private final File imagesDir;
    private final String imagesFolderName;
    private final boolean showAnswers; 

    /**
     * Construye un visitante de exportación LaTeX.
     *
     * @param writer escritor del fichero .tex.
     * @param imagesDir directorio físico donde se guardarán las imágenes.
     * @param imagesFolderName nombre relativo de la carpeta de imágenes.
     * @param showAnswers true para incluir respuestas correctas.
     */
    public LaTeXExportVisitor(BufferedWriter writer, File imagesDir, String imagesFolderName, boolean showAnswers) {
        this.writer = writer;
        this.imagesDir = imagesDir;
        this.imagesFolderName = imagesFolderName;
        this.showAnswers = showAnswers;
    }

    /**
     * Exporta una pregunta de opción múltiple a LaTeX.
     * Usa unaRespuesta o variasRespuestas según admita una o varias respuestas.
     * 
     * @param q pregunta de opción múltiple a exportar.
     */
    @Override
    public void visit(MultichoiceQuestion q) {
        try {
            writeQuestionTitle(q);
            String envName = q.isSingleAnswer() ? "unaRespuesta" : "variasRespuestas";
            writer.write("\\begin{" + envName + "}\n");
            
            for (Answer a : q.getAnswers()) {
                boolean isCorrect = a.getFraction() != null && Double.parseDouble(a.getFraction()) > 0;
                
                String processedText = processTextForLatex(q, a.getText()).replaceAll("\\\\\\\\\\s*$", "").trim();
                String scoreBadge = showAnswers ? formatLaTeXScore(a.getFraction()) : "";
                
                if (isCorrect) writer.write("  \\CorrectChoice " + processedText + scoreBadge + "\n");
                else writer.write("  \\choice " + processedText + scoreBadge + "\n");
            }
            writer.write("\\end{" + envName + "}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { 
        	LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); 
        }
    }

    /**
     * Exporta una pregunta de verdadero/falso a LaTeX.
     * Siempre genera exactamente dos opciones dentro del entorno unaRespuesta.
     * 
     * @param q pregunta de verdadero/falso a exportar.
     */
    @Override
    public void visit(TrueFalseQuestion q) {
        try {
            writeQuestionTitle(q);
            boolean isTrueCorrect = "100".equals(q.getTrueAnswer().getFraction());
            String trueBadge = showAnswers ? formatLaTeXScore(q.getTrueAnswer().getFraction()) : "";
            String falseBadge = showAnswers ? formatLaTeXScore(q.getFalseAnswer().getFraction()) : "";
            
            writer.write("\\begin{unaRespuesta}\n");
            writer.write((isTrueCorrect ? "  \\CorrectChoice " : "  \\choice ") + "Verdadero" + trueBadge + "\n");
            writer.write((!isTrueCorrect ? "  \\CorrectChoice " : "  \\choice ") + "Falso" + falseBadge + "\n");
            writer.write("\\end{unaRespuesta}\n\n\\vspace{0.5cm}\n");
            
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }

    /**
     * Exporta una pregunta de respuesta corta a LaTeX.
     * En modo solucionario muestra las respuestas correctas en un recuadro azul y 
     * en modo examen deja un espacio en blanco.
     * 
     * @param q pregunta de respuesta corta a exportar.
     */
    @Override
    public void visit(ShortAnswerQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
                List<String> correctAnswers = new ArrayList<>();
                for (Answer a : q.getAnswers()) {
                    
                	String scoreBadge = formatLaTeXScore(a.getFraction());
                    correctAnswers.add(processTextForLatex(q, a.getText()) + scoreBadge);
                }
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta esperada:} \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + String.join(" / ", correctAnswers) + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm}\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }

    /**
     * Exporta una pregunta numérica a LaTeX.
     * En modo solucionario muestra el valor correcto y la tolerancia y en modo examen deja espacio.
     * 
     * @param q pregunta numérica a exportar.
     */
    @Override
    public void visit(NumericalQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
            	
                String tolText = (q.getTolerance() != null && !q.getTolerance().equals("0") && !q.getTolerance().isEmpty()) 
                        ? " (margen de error $\\pm$" + escapeLatex(q.getTolerance()) + ")" : "";
                String scoreBadge = formatLaTeXScore(q.getAnswer().getFraction());
                
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta correcta:} \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + processTextForLatex(q, q.getAnswer().getText()) + tolText + scoreBadge + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm}\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }

    /**
     * Exporta una pregunta de emparejamiento a LaTeX.
     * En modo solucionario añade la respuesta y una letra identificadora y en modo examen una línea en blanco.
     * 
     * @param q pregunta de emparejamiento a exportar.
     */
    @Override
    public void visit(MatchingQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{emparejar}\n");
            char letter = 'A';
            int numPairs = q.getPairs().size();
            String pairFraction = numPairs > 0 ? String.valueOf(100.0 / numPairs) : "0";
            String scoreBadge = showAnswers ? formatLaTeXScore(pairFraction) : "";

            for (MatchingPair p : q.getPairs()) {
            	
                String qLatex = processTextForLatex(q, p.getQuestionText()).replaceAll("\\\\\\\\\\s*$", "").replace("\n", " ").trim();
                String aLatex = processTextForLatex(q, p.getAnswerText()).replaceAll("\\\\\\\\\\s*$", "").replace("\n", " ").trim();
                
                if (showAnswers) {
                    writer.write("  \\cgoCorrectChoice{" + letter + "} " + qLatex + " \\dotfill \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + aLatex + scoreBadge + "}}}\n");
                } else {
                    writer.write("  \\cgoCorrectChoice{} " + qLatex + " \\dotfill \\rule{4cm}{0.4pt}\n");
                }
                letter++;
            }
            writer.write("\\end{emparejar}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { 
        	LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); 
        }
    }

    /**
     * Exporta una pregunta Cloze a LaTeX.
     * 
     * @param q pregunta Cloze a exportar.
     */
    @Override
    public void visit(ClozeQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{1cm}\n\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }
    
    /**
     * Exporta una pregunta genérica o de ensayo a LaTeX.
     * 
     * @param q pregunta genérica a exportar.
     */
    @Override
    public void visit(GenericQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{3cm}\n\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }

    /**
     * Escribe la línea de título de una pregunta en el fichero LaTeX.
     * 
     * @param q pregunta cuyo título se va a escribir.
     * @throws IOException si ocurre algún error de escritura.
     */
    private void writeQuestionTitle(Question q) throws IOException {
        String processed = processTextForLatex(q, q.getText());
        writer.write("\\question \\textbf{" + escapeLatex(q.getName()) + "}\\\\\n" + processed + "\n\n");
    }

    /**
     * Aplica la cadena completa de transformaciones para convertir el HTML de un fragmento de pregunta a LaTeX limpio.
     * 
     * @param q pregunta propietaria del HTML.
     * @param html fragmento HTML a convertir.
     * @return cadena LaTeX lista para ser escrita en el fichero de salida.
     */
    private String processTextForLatex(Question q, String html) {
        if (html == null || html.isEmpty()) return "";
        
        html = convertBase64ToPluginFile(q, html); 
        saveImagesToDisk(q);                       
        html = formatClozeSyntax(html);           
        html = resolvePluginFilePaths(html);       
        
        String result = convertHtmlToLatexWithPandoc(html); 
        
        return applyLatexFinalPatches(result);   
    }

    /**
     * Detecta imágenes Base64 en el HTML, las guarda en disco y sustituye sus URIs por referencias @@PLUGINFILE@@.
     * 
     * @param q pregunta propietaria.
     * @param html HTML con posibles atributos src=...
     * @return HTML con las URIs Base64 sustituidas por referencias @@PLUGINFILE@@.
     */
    private String convertBase64ToPluginFile(Question q, String html) {
        Pattern base64Pattern = Pattern.compile("(?i)src=[\"']data:image/([a-zA-Z]+);base64,([a-zA-Z0-9+/=\\s]+)[\"']");
        Matcher matcher = base64Pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        int counter = 1;
        
        while (matcher.find()) {
            String extension = matcher.group(1);
            String data = matcher.group(2).replaceAll("\\s+", ""); 
            String filename = "imgbase_" + System.currentTimeMillis() + "_" + counter + "." + extension;
            
            try {
                File imageFile = new File(imagesDir, filename);
                if (!imageFile.exists()) {
                    byte[] bytes = Base64.getDecoder().decode(data);
                    try (FileOutputStream fos = new FileOutputStream(imageFile)) { fos.write(bytes); }
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement("src=\"@@PLUGINFILE@@/" + filename + "\""));
                counter++;
            } catch (Exception e) { 
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0))); 
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Guarda en disco los ficheros MoodleFile adjuntos a la pregunta. Si el fichero ya existe no se sobreescribe. 
     * Los errores individuales se ignoran.
     * 
     * @param q pregunta cuyos ficheros adjuntos se van a guardar en disco.
     */
    private void saveImagesToDisk(Question q) {
        if (q.getFiles() == null) return;
        for (MoodleFile mFile : q.getFiles()) {
            if (mFile.content != null && !mFile.content.isEmpty()) {
                try {
                    String decodedFilename = java.net.URLDecoder.decode(mFile.name, StandardCharsets.UTF_8.name());
                    File imageFile = new File(imagesDir, decodedFilename);
                    if (!imageFile.exists()) {
                        byte[] imageBytes = Base64.getDecoder().decode(mFile.content.replaceAll("\\s+", ""));
                        try (FileOutputStream fos = new FileOutputStream(imageFile)) { fos.write(imageBytes); }
                    }
                } catch (Exception e) {}
            }
        }
    }

    /**
     * Sustituye referencias @@PLUGINFILE@@/nombre por rutas locales relativas. 
     * 
     * @param html HTML con referencias @@PLUGINFILE@@.
     * @return HTML con las referencias convertidas a rutas locales.
     */
    private String resolvePluginFilePaths(String html) {
        Pattern pattern = Pattern.compile("(?i)src=[\"']@@PLUGINFILE@@/([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String filename = matcher.group(1);
            try { filename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.name()); } catch (Exception e) { }
            String localPath = imagesFolderName + "/" + filename;
            matcher.appendReplacement(sb, Matcher.quoteReplacement("src=\"" + localPath + "\""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Tokeniza la sintaxis Cloze embebida en el HTML sustituyendo cada token por marcadores intermedios que sobreviven intactos
     * al paso de conversión con pandoc. Los marcadores se sustituyen por macros LaTeX definitivas en applyFinalPatches.
     * 
     * @param html HTML que puede contener token Cloze.
     * @return HTML con los tokens Cloze sustituidos por marcadores intermedios.
     */
    private String formatClozeSyntax(String html) {
        Pattern clozePattern = Pattern.compile("(?s)\\{\\d+:([A-Za-z0-9_]+):(.*?)\\}");
        Matcher matcher = clozePattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1).toUpperCase(); 
            String[] options = matcher.group(2).split("(?<!\\\\)~");
            StringBuilder replacement = new StringBuilder();
            if (type.contains("MULTICHOICE") || type.matches("MC|MCV|MCH|MCS|MCVS|MCHS")) {
                replacement.append(" XXMCSTARTXX ");
                for (String opt : options) {
                    opt = opt.trim(); if (opt.isEmpty()) continue;
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    String cleanOpt = cleanClozeOption(opt, false);
                    if (showAnswers && isCorrect) replacement.append(" XXMCCORRECTXX ").append(cleanOpt).append(" ");
                    else replacement.append(" XXMCCHOICEXX ").append(cleanOpt).append(" ");
                }
                replacement.append(" XXMCENDXX ");
            } else if (type.contains("MULTIRESPONSE") || type.matches("MR|MRH|MRS|MRHS")) {
                replacement.append(" XXMRSTARTXX ");
                for (String opt : options) {
                    opt = opt.trim(); if (opt.isEmpty()) continue;
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    String cleanOpt = cleanClozeOption(opt, false);
                    if (showAnswers && isCorrect) replacement.append(" XXMRCORRECTXX ").append(cleanOpt).append(" ");
                    else replacement.append(" XXMRCHOICEXX ").append(cleanOpt).append(" ");
                }
                replacement.append(" XXMRENDXX ");
            } else {
                List<String> correctAnswersList = new ArrayList<>();
                for (String opt : options) {
                    opt = opt.trim();
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    if (isCorrect) correctAnswersList.add(cleanClozeOption(opt, type.contains("NUMERICAL") || type.equals("NM")));
                }
                boolean hasImgs = correctAnswersList.stream().anyMatch(ans -> ans.contains("<img"));
                if (showAnswers) {
                    String correctAnswer = correctAnswersList.isEmpty() ? "________" : String.join(hasImgs ? " " : " / ", correctAnswersList);
                    replacement.append(hasImgs ? "<br><div style=\"text-align: center;\">XXSAANSIMGSTARTXX" + Matcher.quoteReplacement(correctAnswer) + "XXSAANSIMGENDXX</div><br>" : " XXSAANSSTARTXX" + Matcher.quoteReplacement(correctAnswer) + "XXSAANSENDXX ");
                } else {
                    replacement.append(hasImgs ? "<br><div style=\"text-align: center;\">XXSABLANKIMGXX</div><br>" : " XXSABLANKXX "); 
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Limpia el prefijo de puntuación y la retroalimentación de una opción Cloze, devolviendo únicamente el texto de la alternativa.
     * 
     * @param opt texto crudo de la opción.
     * @param isNumerical true si el tipo es NUMERICAL.
     * @return texto limpio sin prefijos de puntuación ni retroalimentación.
     */
    private String cleanClozeOption(String opt, boolean isNumerical) {
        String cleanOpt = opt.replaceFirst("^[=%]-?[0-9.]*%", "").replaceFirst("^=", "").trim();
        cleanOpt = cleanOpt.split("(?<!\\\\)#")[0].trim();
        if (isNumerical) cleanOpt = cleanOpt.replaceAll("(?<!\\\\):", " &plusmn; "); 
        return cleanOpt.replace("\\~", "~").replace("\\#", "#").replace("\\=", "=").replace("\\:", ":").replace("\\}", "}");
    }

    /**
     * Convierte un fragmento HTML a LaTeX invocando pandoc como proceso externo. Si pandoc no está disponible, devuelve el HTML original como fallback.
     * 
     * @param htmlText fragmento HTML a convertir.
     * @return cadena LaTeX resultante, o el HTML original si pandoc no está disponible.
     */
    private String convertHtmlToLatexWithPandoc(String htmlText) {
    	
    	String os = System.getProperty("os.name").toLowerCase();
        String pandocPath = os.contains("win") ? "C:\\Program Files\\Pandoc\\pandoc.exe" : "/usr/bin/pandoc";
        
        ProcessBuilder pb = new ProcessBuilder(pandocPath, "-f", "html+tex_math_dollars+tex_math_single_backslash", "-t", "latex");
        pb.redirectErrorStream(true);
        StringBuilder latex = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) { 
                pw.write(htmlText); 
            }
            try (BufferedReader pr = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = pr.readLine()) != null) latex.append(line).append("\n");
            }
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return htmlText;
        } catch (Exception e) { return htmlText; } 
        return latex.toString().trim();
    }

    /**
     * Aplica transformaciones finales sobre el LaTeX generado por pandoc.
     * 
     * @param result cadena LaTeX producida por pandoc.
     * @return cadena LaTeX con todos los parches aplicados.
     */
    private String applyLatexFinalPatches(String result) {
        result = result.replace("XXSABLANKXX", "\\rule{3cm}{0.4pt}")
                       .replace("XXSABLANKIMGXX", "\\rule{6cm}{0.4pt}")
                       .replace("XXSAANSSTARTXX", "\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{")
                       .replace("XXSAANSENDXX", "}}}")
                       .replace("XXSAANSIMGSTARTXX", "\\begin{center}\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{}}\\\\\n")
                       .replace("XXSAANSIMGENDXX", "\\\\\n}\\end{center}")
                       .replace("XXMCSTARTXX", "\\begin{unaRespuestaEnLinea}")
                       .replace("XXMCENDXX", "\\end{unaRespuestaEnLinea}")
                       .replace("XXMCCORRECTXX", "\\CorrectChoice ")
                       .replace("XXMCCHOICEXX", "\\choice ")
                       .replace("XXMRSTARTXX", "\\begin{variasRespuestasEnLinea}")
                       .replace("XXMRENDXX", "\\end{variasRespuestasEnLinea}")
                       .replace("XXMRCORRECTXX", "\\CorrectChoice ")
                       .replace("XXMRCHOICEXX", "\\choice ");

        result = result.replaceAll("(\\\\includegraphics\\[.*?\\]\\{.*?\\}~)\\s*\\n", "$1\\\\newline");
        result = result.replaceAll("(?m)(^\\s*(?:\\\\CorrectChoice|\\\\choice).*?)\\\\\\\\\\s*$", "$1");
        result = result.replaceAll("(?m)^\\s*\\\\\\\\\\s*$", "");
        result = result.replaceAll("\\\\begin\\{longtable\\}\\[.*?\\]", "\\\\begin{tabular}")
                       .replace("\\begin{longtable}", "\\begin{tabular}")
                       .replace("\\end{longtable}", "\\end{tabular}")
                       .replaceAll("(?m)^\\s*\\\\endhead\\s*$", "").replaceAll("(?m)^\\s*\\\\endfirsthead\\s*$", "")
                       .replaceAll("(?m)^\\s*\\\\endfoot\\s*$", "").replaceAll("(?m)^\\s*\\\\endlastfoot\\s*$", "");

        StringBuilder finalSb = new StringBuilder();
        int currentIndex = 0;
        
        while (true) {
            int startIdx = result.indexOf("\\begin{tabular}", currentIndex);
            if (startIdx == -1) { finalSb.append(result.substring(currentIndex)); break; }
            int depth = 1, searchIdx = startIdx + 15, endIdx = -1;
            while (depth > 0 && searchIdx < result.length()) {
                int nextBegin = result.indexOf("\\begin{tabular}", searchIdx);
                int nextEnd = result.indexOf("\\end{tabular}", searchIdx);
                if (nextEnd == -1) break; 
                if (nextBegin != -1 && nextBegin < nextEnd) { depth++; searchIdx = nextBegin + 15; }
                else { depth--; endIdx = nextEnd; searchIdx = nextEnd + 13; }
            }
            if (depth == 0 && endIdx != -1) {
                finalSb.append(result, currentIndex, startIdx); 
                String tableContent = result.substring(startIdx, endIdx + 13);
                
                tableContent = tableContent.replace("\\bottomrule\\noalign{}", "");
                tableContent = tableContent.replace("\\end{tabular}", "\\bottomrule\\noalign{}\n\\end{tabular}");
                
                String wrappedTable = "\\begin{center}\n\\resizebox{0.95\\linewidth}{!}{\n" 
                                    + tableContent 
                                    + "\n}\n\\end{center}";
                finalSb.append(wrappedTable);
                currentIndex = endIdx + 13;
            } else { finalSb.append(result.substring(currentIndex)); break; }
        }
        return finalSb.toString().trim();
    }

    /**
     * Escapa los caracteres especiales de LaTeX en texto plano.
     * 
     * @param text cadena a escapar.
     * @return cadena con los caracteres especiales LaTeX escapados con backslash.
     */
    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("%", "\\%").replace("_", "\\_").replace("#", "\\#").replace("&", "\\&").replace("$", "\\$");
    }
    
    /**
     * Genera la etiqueta de puntuación formateada para LaTeX a partir de la fracción de Moodle.
     * Aplica formato de texto en negrita, color azul para puntuaciones positivas y rojo para negativas.
     * 
     * @param fractionStr cadena con el valor de la fracción.
     * @return cadena formateada con macros de LaTeX para estilo y color.
     */
    private String formatLaTeXScore(String fractionStr) {
        if (fractionStr == null || fractionStr.isEmpty() || "0".equals(fractionStr)) {
            return "";
        }
        try {
            double val = Double.parseDouble(fractionStr);
            String formattedNumber;
            
            if (val == Math.floor(val)) {
                formattedNumber = String.format("%.0f", val);
            } else {
                formattedNumber = String.format(java.util.Locale.US, "%.1f", val);
            }
            
            if (val > 0) {
                return " \\textbf{\\textcolor{azul}{(+" + formattedNumber + "\\%)}}";
            } else {
                return " \\textbf{\\textcolor{red}{(" + formattedNumber + "\\%)}}";
            }
        } catch (NumberFormatException e) {
            if (fractionStr.contains("-")) {
                return " \\textbf{\\textcolor{red}{(" + escapeLatex(fractionStr) + "\\%)}}";
            } else {
                return " \\textbf{\\textcolor{azul}{(" + escapeLatex(fractionStr) + "\\%)}}";
            }
        }
    }
}