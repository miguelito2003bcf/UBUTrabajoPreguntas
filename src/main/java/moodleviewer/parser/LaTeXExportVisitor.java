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

public class LaTeXExportVisitor implements QuestionVisitor {
    
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
                String processedText = processTextForLatex(q, a.getText());
                
                if (isCorrect) {
                    writer.write("  \\CorrectChoice " + processedText + "\n");
                } else {
                    writer.write("  \\choice " + processedText + "\n");
                }
            }
            writer.write("\\end{" + envName + "}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(TrueFalseQuestion q) {
        try {
            writeQuestionTitle(q);
            boolean isTrueCorrect = "100".equals(q.getTrueAnswer().getFraction());
            
            writer.write("\\begin{unaRespuesta}\n");
            writer.write((isTrueCorrect ? "  \\CorrectChoice " : "  \\choice ") + "Verdadero\n");
            writer.write((!isTrueCorrect ? "  \\CorrectChoice " : "  \\choice ") + "Falso\n");
            writer.write("\\end{unaRespuesta}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(ShortAnswerQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
                List<String> correctAnswers = new ArrayList<>();
                for (Answer a : q.getAnswers()) {
                    if ("100".equals(a.getFraction())) {
                        correctAnswers.add(processTextForLatex(q, a.getText()));
                    }
                }
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta esperada:} \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + String.join(" / ", correctAnswers) + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm} % Espacio para respuesta corta\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(NumericalQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
                String tolText = (q.getTolerance() != null && !q.getTolerance().equals("0") && !q.getTolerance().isEmpty()) 
                        ? " (margen de error $\\pm$" + escapeLatex(q.getTolerance()) + ")" : "";
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta correcta:} \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + processTextForLatex(q, q.getAnswer().getText()) + tolText + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm} % Espacio para número\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(MatchingQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{emparejar}\n");
            char letter = 'A';
            for (MatchingPair p : q.getPairs()) {
                if (showAnswers) {
                    writer.write("  \\cgoCorrectChoice{" + letter + "} " + processTextForLatex(q, p.getQuestionText()) + " \\dotfill \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{" + processTextForLatex(q, p.getAnswerText()) + "}}}\n");
                } else {
                    writer.write("  \\cgoCorrectChoice{} " + processTextForLatex(q, p.getQuestionText()) + " \\dotfill \\rule{4cm}{0.4pt}\n");
                }
                letter++;
            }
            writer.write("\\end{emparejar}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(ClozeQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{1cm}\n\n");
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void visit(GenericQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{3cm} % Espacio amplio para redactar\n\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void writeQuestionTitle(Question q) throws IOException {
        String processed = processTextForLatex(q, q.getText());
        writer.write("\\question \\textbf{" + escapeLatex(q.getName()) + "}\\\\\n" + processed + "\n\n");
    }

    private String processTextForLatex(Question q, String html) {
        if (html == null || html.isEmpty()) return "";
        html = convertBase64ToPluginFile(q, html);
        saveImagesToDisk(q);
        html = formatClozeSyntax(html);
        html = resolvePluginFilePaths(html);
        String result = convertHtmlToLatexWithPandoc(html);
        return applyLatexFinalPatches(result);
    }

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
                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        fos.write(bytes);
                    }
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

    private void saveImagesToDisk(Question q) {
        if (q.getFiles() == null) return;
        for (MoodleFile mFile : q.getFiles()) {
            if (mFile.content != null && !mFile.content.isEmpty()) {
                try {
                    String decodedFilename = java.net.URLDecoder.decode(mFile.name, StandardCharsets.UTF_8.name());
                    File imageFile = new File(imagesDir, decodedFilename);
                    if (!imageFile.exists()) {
                        byte[] imageBytes = Base64.getDecoder().decode(mFile.content.replaceAll("\\s+", ""));
                        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                            fos.write(imageBytes);
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }

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
                    opt = opt.trim();
                    if (opt.isEmpty()) continue;
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    String cleanOpt = cleanClozeOption(opt, false);
                    if (showAnswers && isCorrect) replacement.append(" XXMCCORRECTXX ").append(cleanOpt).append(" ");
                    else replacement.append(" XXMCCHOICEXX ").append(cleanOpt).append(" ");
                }
                replacement.append(" XXMCENDXX ");
            }
            else if (type.contains("MULTIRESPONSE") || type.matches("MR|MRH|MRS|MRHS")) {
                replacement.append(" XXMRSTARTXX ");
                for (String opt : options) {
                    opt = opt.trim();
                    if (opt.isEmpty()) continue;
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    String cleanOpt = cleanClozeOption(opt, false);
                    if (showAnswers && isCorrect) replacement.append(" XXMRCORRECTXX ").append(cleanOpt).append(" ");
                    else replacement.append(" XXMRCHOICEXX ").append(cleanOpt).append(" ");
                }
                replacement.append(" XXMRENDXX ");
            }
            else {
                List<String> correctAnswersList = new ArrayList<>();
                for (String opt : options) {
                    opt = opt.trim();
                    boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                    if (isCorrect) correctAnswersList.add(cleanClozeOption(opt, type.contains("NUMERICAL") || type.equals("NM")));
                }
                boolean hasImgs = correctAnswersList.stream().anyMatch(ans -> ans.contains("<img"));
                if (showAnswers) {
                    String correctAnswer = correctAnswersList.isEmpty() ? "________" : String.join(hasImgs ? " " : " / ", correctAnswersList);
                    String safeAnswer = Matcher.quoteReplacement(correctAnswer);
                    replacement.append(hasImgs ? 
                        "<br><div style=\"text-align: center;\">XXSAANSIMGSTARTXX" + safeAnswer + "XXSAANSIMGENDXX</div><br>" :
                        " XXSAANSSTARTXX" + safeAnswer + "XXSAANSENDXX ");
                } else {
                    replacement.append(hasImgs ? 
                        "<br><div style=\"text-align: center;\">XXSABLANKIMGXX</div><br>" :
                        " XXSABLANKXX "); 
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
        ProcessBuilder pb = new ProcessBuilder("pandoc", "-f", "html+tex_math_dollars+tex_math_single_backslash", "-t", "latex");
        pb.redirectErrorStream(true);
        
        Process process;
        StringBuilder latex = new StringBuilder();
        
        try {
            process = pb.start();
            try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                pw.write(htmlText);
            }
            try (BufferedReader pr = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line; 
                while ((line = pr.readLine()) != null) {
                    latex.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error de lectura/escritura con Pandoc: " + e.getMessage());
            return htmlText;
        }
        
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.err.println("La espera del proceso fue interrumpida: " + e.getMessage());
            Thread.currentThread().interrupt(); 
            return htmlText;
        }
        
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

        result = result.replaceAll("(?m)^\\s*\\\\\\\\\\s*$", "");
        
        result = result.replaceAll("\\\\begin\\{longtable\\}\\[.*?\\]", "\\\\begin{tabular}")
                       .replace("\\begin{longtable}", "\\begin{tabular}")
                       .replace("\\end{longtable}", "\\end{tabular}")
                       .replaceAll("(?m)^\\s*\\\\endhead\\s*$", "")
                       .replaceAll("(?m)^\\s*\\\\endfirsthead\\s*$", "")
                       .replaceAll("(?m)^\\s*\\\\endfoot\\s*$", "")
                       .replaceAll("(?m)^\\s*\\\\endlastfoot\\s*$", "");

        StringBuilder finalSb = new StringBuilder();
        int currentIndex = 0;
        
        while (true) {
            int startIdx = result.indexOf("\\begin{tabular}", currentIndex);
            if (startIdx == -1) {
                finalSb.append(result.substring(currentIndex));
                break;
            }
            
            int depth = 1;
            int searchIdx = startIdx + "\\begin{tabular}".length();
            int endIdx = -1;
            
            while (depth > 0 && searchIdx < result.length()) {
                int nextBegin = result.indexOf("\\begin{tabular}", searchIdx);
                int nextEnd = result.indexOf("\\end{tabular}", searchIdx);
                
                if (nextEnd == -1) break; 
                
                if (nextBegin != -1 && nextBegin < nextEnd) {
                    depth++;
                    searchIdx = nextBegin + "\\begin{tabular}".length();
                } else {
                    depth--;
                    endIdx = nextEnd;
                    searchIdx = nextEnd + "\\end{tabular}".length();
                }
            }
            
            if (depth == 0 && endIdx != -1) {
                finalSb.append(result, currentIndex, startIdx); 
                
                String tableContent = result.substring(startIdx, endIdx + "\\end{tabular}".length());
                String wrappedTable = "\\begin{center}\n\\begin{adjustbox}{max width=0.95\\linewidth}\n" 
                                    + tableContent 
                                    + "\n\\end{adjustbox}\n\\end{center}";
                finalSb.append(wrappedTable);
                currentIndex = endIdx + "\\end{tabular}".length();
            } else {
                finalSb.append(result.substring(currentIndex));
                break;
            }
        }
        
        return finalSb.toString().trim();
    }

    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("%", "\\%").replace("_", "\\_").replace("#", "\\#").replace("&", "\\&").replace("$", "\\$");
    }
}