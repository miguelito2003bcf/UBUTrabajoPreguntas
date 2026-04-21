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

    public LaTeXExportVisitor(BufferedWriter writer, File imagesDir, String imagesFolderName) {
        this.writer = writer;
        this.imagesDir = imagesDir;
        this.imagesFolderName = imagesFolderName;
    }

    @Override
    public void visit(MultichoiceQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{enumerate}[label=\\textbf{\\alph*.}]\n");
            List<String> correctAnswers = new ArrayList<>();
            for (Answer a : q.getAnswers()) {
                boolean isCorrect = a.getFraction() != null && Double.parseDouble(a.getFraction()) > 0;
                String processedText = processTextForLatex(q, a.getText());
                writer.write("  \\item " + processedText + "\n");
                if (isCorrect) {
                    correctAnswers.add(processedText);
                }
            }
            writer.write("\\end{enumerate}\n\n");
            if (!correctAnswers.isEmpty()) {
                writer.write("\\vspace{0.3cm}\n");
                writer.write("\\noindent \\textbf{La(s) respuesta(s) correcta(s):}\n");
                writer.write("\\begin{itemize}[label=\\textbullet]\n");
                for (String ca : correctAnswers) {
                    writer.write("  \\item " + ca + "\n");
                }
                writer.write("\\end{itemize}\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(TrueFalseQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{enumerate}[label=\\textbf{\\alph*.}]\n");
            writer.write("  \\item Verdadero\n");
            writer.write("  \\item Falso\n");
            writer.write("\\end{enumerate}\n\n");
            String correctAnswer = "100".equals(q.getTrueAnswer().getFraction()) ? "Verdadero" : "Falso";
            writer.write("\\vspace{0.3cm}\n");
            writer.write("\\noindent \\textbf{La respuesta correcta es:} " + correctAnswer + "\n\n");
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(ShortAnswerQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{1.5cm} % Espacio para respuesta corta\n\n");
            List<String> correctAnswers = new ArrayList<>();
            for (Answer a : q.getAnswers()) {
                if ("100".equals(a.getFraction())) {
                    correctAnswers.add(processTextForLatex(q, a.getText()));
                }
            }
            if (!correctAnswers.isEmpty()) {
                writer.write("\\noindent \\textbf{La(s) respuesta(s) correcta(s):}\n");
                writer.write("\\begin{itemize}[label=\\textbullet]\n");
                for (String ca : correctAnswers) {
                    writer.write("  \\item " + ca + "\n");
                }
                writer.write("\\end{itemize}\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(NumericalQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{1.5cm} % Espacio para número\n\n");
            String tolText = (q.getTolerance() != null && !q.getTolerance().equals("0") && !q.getTolerance().isEmpty()) 
                    ? " (margen de error $\\pm$" + escapeLatex(q.getTolerance()) + ")" : "";
            writer.write("\\noindent \\textbf{La respuesta correcta es:} " + processTextForLatex(q, q.getAnswer().getText()) + tolText + "\n\n");
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void visit(MatchingQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{itemize}[label=--]\n");
            for (MatchingPair p : q.getPairs()) {
                writer.write("  \\item " + processTextForLatex(q, p.getQuestionText()) + " \\\\\\\\ \\dotfill \\rule{4cm}{0.4pt}\n");
            }
            writer.write("\\end{itemize}\n\n");
            writer.write("\\vspace{0.3cm}\n");
            writer.write("\\noindent \\textbf{Las parejas correctas son:}\n");
            writer.write("\\begin{itemize}[label=\\textbullet]\n");
            for (MatchingPair p : q.getPairs()) {
                writer.write("  \\item " + processTextForLatex(q, p.getQuestionText()) + " $\\rightarrow$ " + processTextForLatex(q, p.getAnswerText()) + "\n");
            }
            writer.write("\\end{itemize}\n");
            writer.write("\\vspace{0.5cm}\n");
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
        writer.write("\\textbf{Pregunta:} " + processed + "\n\n");
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
            List<String> correctAnswersList = new ArrayList<>();

            for (String opt : options) {
                opt = opt.trim();
                boolean isCorrect = opt.startsWith("=") || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*") && !opt.matches("^%0+(\\.0+)?%.*"));
                if (isCorrect) {
                    String cleanOpt = opt.replaceFirst("^[=%][0-9.]*%", "").replaceFirst("^=", "").trim();
                    cleanOpt = cleanOpt.split("(?<!\\\\)#")[0].trim();
                    if (type.contains("NUMERICAL") || type.equals("NM")) {
                        cleanOpt = cleanOpt.replaceAll("(?<!\\\\):", " &plusmn; "); 
                    }
                    correctAnswersList.add(cleanOpt.replace("\\~", "~").replace("\\#", "#").replace("\\=", "=").replace("\\:", ":").replace("\\}", "}"));
                }
            }
            
            boolean hasImgs = correctAnswersList.stream().anyMatch(ans -> ans.contains("<img"));
            
            String correctAnswer = correctAnswersList.isEmpty() ? "________" : 
                                   String.join(hasImgs ? " " : " / ", correctAnswersList);
            
            String replacement = hasImgs ? 
                "<br><div style=\"text-align: center;\"><strong>[Respuesta:</strong><br>" + correctAnswer + "<br><strong>]</strong></div><br>" :
                " <strong>[Respuesta: " + correctAnswer + "]</strong> ";
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String convertHtmlToLatexWithPandoc(String htmlText) {
        try {
            ProcessBuilder pb = new ProcessBuilder("pandoc", "-f", "html+tex_math_dollars+tex_math_single_backslash", "-t", "latex");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            try (BufferedWriter pw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                pw.write(htmlText);
            }
            StringBuilder latex = new StringBuilder();
            try (BufferedReader pr = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = pr.readLine()) != null) latex.append(line).append("\n");
            }
            process.waitFor();
            return latex.toString().trim();
        } catch (Exception e) { return htmlText; }
    }

    private String applyLatexFinalPatches(String result) {
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