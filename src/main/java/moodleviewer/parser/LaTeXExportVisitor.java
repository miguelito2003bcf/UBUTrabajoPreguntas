package moodleviewer.parser;

import moodleviewer.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            writeQuestionTitle(q, " \\\\\\\\");
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
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
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
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
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
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
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
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
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
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
            writer.write("\\vspace{2cm} % Espacio para rellenar huecos\n\n");
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void visit(GenericQuestion q) {
        try {
            writeQuestionTitle(q, " \\\\\\\\\\\\\\\\");
            writer.write("\\vspace{3cm} % Espacio amplio para redactar el ensayo\n\n");
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }

    private void writeQuestionTitle(Question q, String suffix) throws IOException {
        String processed = processTextForLatex(q, q.getText());
        processed = processed.trim();
        processed = processed.replaceAll("(\\\\\\\\\\s*)+$", "").trim();
        writer.write("\\textbf{Pregunta:} " + processed);
        if (!processed.endsWith("\\end{center}")) {
            writer.write(suffix);
        }
        writer.write("\n");
    }

    private String processTextForLatex(Question q, String html) {
        if (html == null) return "";
        saveImagesToDisk(q);
        html = formatClozeSyntax(html);
        html = formatHtmlTables(html);
        Pattern pattern = Pattern.compile("(?i)<img[^>]*src=\"@@PLUGINFILE@@/([^\"]+)\"[^>]*>");
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String filename = matcher.group(1);
            matcher.appendReplacement(sb, " [[[IMG:" + filename + "]]] ");
        }
        matcher.appendTail(sb);
        String cleanedText = cleanHtmlForLatex(sb.toString());
        Pattern restorePattern = Pattern.compile("\\[\\[\\[IMG:(.+?)\\]\\]\\]");
        Matcher restoreMatcher = restorePattern.matcher(cleanedText);
        StringBuffer finalSb = new StringBuffer();
        while (restoreMatcher.find()) {
            String filename = restoreMatcher.group(1).replace("\\_", "_").replace("\\%", "%");
            String latexImage = "\n\\begin{center}\\includegraphics[width=0.6\\textwidth]{\"" + imagesFolderName + "/" + filename + "\"}\\end{center}\n";
            restoreMatcher.appendReplacement(finalSb, Matcher.quoteReplacement(latexImage));
        }
        restoreMatcher.appendTail(finalSb);
        String result = finalSb.toString();
        result = result.replaceAll("(?m)^\\s*\\\\\\\\\\s*$", "");
        result = result.replaceAll("(?s)(\\\\\\\\\\s*)+$", "");
        
        return result.trim();
    }

    private String formatClozeSyntax(String html) {
        Pattern clozePattern = Pattern.compile("(?s)\\{\\d+:[A-Za-z0-9_]+:(.*?)\\}");
        Matcher matcher = clozePattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String optionsStr = matcher.group(1);
            String[] options = optionsStr.split("~");
            String correctAnswer = "________";
            for (String opt : options) {
                opt = opt.trim();
                if (opt.startsWith("=") || opt.startsWith("%100%")) {
                    String cleanOpt = opt.replaceFirst("^(=|%100%)", "");
                    int hashIndex = cleanOpt.indexOf("#"); 
                    if (hashIndex != -1) {
                        cleanOpt = cleanOpt.substring(0, hashIndex);
                    }
                    correctAnswer = cleanOpt.trim();
                    break;
                }
            }
            String replacement = " \\textbf{[Respuesta: " + correctAnswer + "]} ";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String formatHtmlTables(String html) {
        Pattern tablePattern = Pattern.compile("(?is)<table[^>]*>(.*?)</table>");
        Matcher tableMatcher = tablePattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (tableMatcher.find()) {
            String tableContent = tableMatcher.group(1);
            tableContent = tableContent.replaceAll("(?i)<tr[^>]*>", "");
            tableContent = tableContent.replaceAll("(?i)</tr>", Matcher.quoteReplacement(" \\\\ \\hline\n"));
            tableContent = tableContent.replaceAll("(?i)<t[dh][^>]*>", " ");
            tableContent = tableContent.replaceAll("(?i)</t[dh]>", " & "); 
            tableContent = tableContent.replaceAll("&\\s*\\\\\\\\", Matcher.quoteReplacement("\\\\"));
            String latexTable = "\n\\begin{center}\n" +
                                "\\resizebox{\\textwidth}{!}{\n" +
                                "\\begin{tabular}{|*{15}{c|}}\\hline\n" +
                                tableContent +
                                "\\end{tabular}\n" +
                                "}\n\\end{center}\n";
            tableMatcher.appendReplacement(sb, Matcher.quoteReplacement(latexTable));
        }
        tableMatcher.appendTail(sb);
        return sb.toString();
    }

    private void saveImagesToDisk(Question q) {
        if (q.getFiles() == null) return;
        for (MoodleFile mFile : q.getFiles()) {
            if (mFile.content != null && !mFile.content.isEmpty()) {
                try {
                    File imageFile = new File(imagesDir, mFile.name);
                    if (!imageFile.exists()) {
                        byte[] imageBytes = Base64.getDecoder().decode(mFile.content);
                        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                            fos.write(imageBytes);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error guardando imagen " + mFile.name + ": " + e.getMessage());
                }
            }
        }
    }

    private String cleanHtmlForLatex(String html) {
        if (html == null) return "";
        String cleaned = html.replaceAll("(?i)<li[^>]*>", "\\\\textbullet\\ ");
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>", " \\\\\\\\ \n");
        cleaned = cleaned.replaceAll("(?i)</p>", " \\\\\\\\ \n");
        cleaned = cleaned.replaceAll("<[^>]*>", "");
        cleaned = cleaned.replace("&nbsp;", " ")
                         .replace("&lt;", "<")
                         .replace("&gt;", ">")
                         .replace("&amp;", "\\&");
                         
        return protectMathAndEscape(cleaned).trim();
    }

    private String protectMathAndEscape(String text) {
        StringBuilder sb = new StringBuilder();
        String[] parts = text.split("\\$\\$", -1); 
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                sb.append(parts[i].replace("%", "\\%")
                                  .replace("_", "\\_")
                                  .replace("#", "\\#")
                                  .replace("$", "\\$"));
            } else {
                sb.append("$").append(parts[i]).append("$");
            }
        }
        return sb.toString();
    }

    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("%", "\\%")
                   .replace("_", "\\_")
                   .replace("#", "\\#")
                   .replace("&", "\\&")
                   .replace("$", "\\$");
    }
}