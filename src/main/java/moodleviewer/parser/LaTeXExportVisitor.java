/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.parser;

import moodleviewer.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
 * Clase visitante que serializa cada tipo de pregunta de Moodle al formato LaTeX.
 * Utiliza JSoup para una manipulación robusta del DOM HTML.
 */
public class LaTeXExportVisitor implements QuestionVisitor {
    
    private static final Logger LOGGER = Logger.getLogger(LaTeXExportVisitor.class.getName());
    private final BufferedWriter writer;
    private final File imagesDir;
    private final String imagesFolderName;
    private final boolean showAnswers; 

    public LaTeXExportVisitor(BufferedWriter writer, File imagesDir, String imagesFolderName, boolean showAnswers) {
        this.writer = writer;
        this.imagesDir = imagesDir;
        this.imagesFolderName = imagesFolderName;
        this.showAnswers = showAnswers;
    }

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

    @Override
    public void visit(ClozeQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{1cm}\n\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }
    
    @Override
    public void visit(GenericQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{3cm}\n\n");
        } catch (IOException e) { LOGGER.log(Level.SEVERE, "Error al exportar la pregunta a formato LaTeX", e); }
    }

    private void writeQuestionTitle(Question q) throws IOException {
        String processed = processTextForLatex(q, q.getText());
        writer.write("\\question \\textbf{" + escapeLatex(q.getName()) + "}\\\\\n" + processed + "\n\n");
    }

    /**
     * Aplica la cadena de transformaciones para convertir el HTML en LaTeX.
     * Utiliza JSoup para manipular de forma segura las etiquetas de imágenes antes de llamar a Pandoc.
     */
    private String processTextForLatex(Question q, String html) {
        if (html == null || html.isEmpty()) return "";
        
        // 1. Parseo seguro del HTML utilizando JSoup
        Document doc = Jsoup.parseBodyFragment(html);
        int base64Counter = 1;
        Elements images = doc.select("img");
        
        for (Element img : images) {
            String src = img.attr("src");
            
            // A) Procesamiento de Imágenes Base64
            if (src.matches("(?i)^data:image/([a-zA-Z]+);base64,(.*)")) {
                Matcher m = Pattern.compile("(?i)^data:image/([a-zA-Z]+);base64,(.*)").matcher(src);
                if (m.find()) {
                    String extension = m.group(1);
                    String data = m.group(2).replaceAll("\\s+", "");
                    String filename = "imgbase_" + System.currentTimeMillis() + "_" + base64Counter + "." + extension;
                    
                    try {
                        File imageFile = new File(imagesDir, filename);
                        if (!imageFile.exists()) {
                            byte[] bytes = Base64.getDecoder().decode(data);
                            try (FileOutputStream fos = new FileOutputStream(imageFile)) { fos.write(bytes); }
                        }
                        // Mutamos el DOM directamente
                        img.attr("src", imagesFolderName + "/" + filename);
                        base64Counter++;
                    } catch (Exception e) { 
                        LOGGER.log(Level.WARNING, "No se pudo extraer la imagen Base64", e); 
                    }
                }
            } 
            // B) Procesamiento de referencias nativas Moodle (@@PLUGINFILE@@)
            else if (src.contains("@@PLUGINFILE@@/")) {
                String filename = src.substring(src.lastIndexOf("/") + 1);
                try { 
                    filename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.name()); 
                } catch (Exception e) { }
                // Mutamos el DOM directamente
                img.attr("src", imagesFolderName + "/" + filename);
            }
        }
        
        // Extraemos el HTML modificado (solo el interior del fragmento)
        html = doc.body().html();
        
        // 2. Guardamos en disco las imágenes adjuntas a nivel de modelo
        saveImagesToDisk(q);                       
        
        // 3. Procesamos la sintaxis Cloze. (Aquí mantenemos RegEx porque Cloze NO es HTML, es un micro-lenguaje de texto).
        html = formatClozeSyntax(html);           
        
        // 4. Conversión con Pandoc
        String result = convertHtmlToLatexWithPandoc(html); 
        
        // 5. Parches de macros
        return applyLatexFinalPatches(result);   
    }

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
    
    private String cleanClozeOption(String opt, boolean isNumerical) {
        String cleanOpt = opt.replaceFirst("^[=%]-?[0-9.]*%", "").replaceFirst("^=", "").trim();
        cleanOpt = cleanOpt.split("(?<!\\\\)#")[0].trim();
        if (isNumerical) cleanOpt = cleanOpt.replaceAll("(?<!\\\\):", " &plusmn; "); 
        return cleanOpt.replace("\\~", "~").replace("\\#", "#").replace("\\=", "=").replace("\\:", ":").replace("\\}", "}");
    }

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

        result = result.replaceAll("(\\\\includegraphics\\[[^\\]\\n]*\\]\\{[^}\\n]*\\}~)\\s*\\n", "$1\\\\newline");
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

    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("%", "\\%").replace("_", "\\_").replace("#", "\\#").replace("&", "\\&").replace("$", "\\$");
    }
    
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