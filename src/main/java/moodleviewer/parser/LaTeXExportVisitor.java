/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer.parser;

import moodleviewer.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Visitante que serializa cada tipo de pregunta Moodle a LaTeX.
 *
 * Correcciones:
 * <ul>
 *   <li>PUNTO  1: Pandoc se invoca como {@code "pandoc"} (resuelto por PATH) en vez
 *       de ruta hardcodeada, con mensaje de error claro si no se encuentra.</li>
 *   <li>PUNTO  2: {@code saveImagesToDisk} registra los errores en el logger en vez
 *       de silenciarlos con un bloque {@code catch} vacío.</li>
 *   <li>PUNTO  9: {@link #escapeLatex} ahora escapa también {@code \}, {@code ^},
 *       {@code ~}, {@code {}, {@code }}, {@code <} y {@code >}.</li>
 *   <li>PUNTO 19: {@link #visit(ClozeQuestion)} reemplaza cada hueco Cloze por una
 *       línea en blanco {@code \rule} (modo examen) o la respuesta correcta (solucionario)
 *       en lugar de escribir solo {@code \vspace{1cm}}.</li>
 *   <li>PUNTO 29: nombres de imagen generados con hash MD5 para deduplicar.</li>
 * </ul>
 */
public class LaTeXExportVisitor implements QuestionVisitor {

    private static final Logger  LOGGER       = Logger.getLogger(LaTeXExportVisitor.class.getName());
    private static final Pattern CLOZE_TOKEN  = Pattern.compile(
            "\\{([0-9]*):(NUMERICAL|NM|MULTICHOICE[_A-Z]*|MC[A-Z]*|SHORTANSWER[_A-Z]*|SA[A-Z]*|MW[A-Z]*|MULTIRESPONSE[_A-Z]*|MR[A-Z]*):(.+?)(?<!\\\\)\\}",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final BufferedWriter writer;
    private final File           imagesDir;
    private final String         imagesFolderName;
    private final boolean        showAnswers;

    public LaTeXExportVisitor(BufferedWriter writer, File imagesDir,
                              String imagesFolderName, boolean showAnswers) {
        this.writer           = writer;
        this.imagesDir        = imagesDir;
        this.imagesFolderName = imagesFolderName;
        this.showAnswers      = showAnswers;
    }

    // =========================================================================
    //  Visitantes por tipo
    // =========================================================================

    @Override
    public void visit(MultichoiceQuestion q) {
        try {
            writeQuestionTitle(q);
            String env = q.isSingleAnswer() ? "unaRespuesta" : "variasRespuestas";
            writer.write("\\begin{" + env + "}\n");
            for (Answer a : q.getAnswers()) {
                boolean correct   = a.getFraction() != null && Double.parseDouble(a.getFraction()) > 0;
                String  text      = processTextForLatex(q, a.getText()).replaceAll("\\\\\\\\\\s*$", "").trim();
                String  badge     = showAnswers ? formatLaTeXScore(a.getFraction()) : "";
                writer.write(correct ? "  \\CorrectChoice " : "  \\choice ");
                writer.write(text + badge + "\n");
            }
            writer.write("\\end{" + env + "}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    @Override
    public void visit(TrueFalseQuestion q) {
        try {
            writeQuestionTitle(q);
            boolean tc = "100".equals(q.getTrueAnswer().getFraction());
            writer.write("\\begin{unaRespuesta}\n");
            writer.write((tc  ? "  \\CorrectChoice " : "  \\choice ") + "Verdadero"
                    + (showAnswers ? formatLaTeXScore(q.getTrueAnswer().getFraction())  : "") + "\n");
            writer.write((!tc ? "  \\CorrectChoice " : "  \\choice ") + "Falso"
                    + (showAnswers ? formatLaTeXScore(q.getFalseAnswer().getFraction()) : "") + "\n");
            writer.write("\\end{unaRespuesta}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    @Override
    public void visit(ShortAnswerQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
                List<String> correct = new ArrayList<>();
                for (Answer a : q.getAnswers())
                    correct.add(processTextForLatex(q, a.getText()) + formatLaTeXScore(a.getFraction()));
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta esperada:} "
                        + "\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{"
                        + String.join(" / ", correct) + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm}\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    @Override
    public void visit(NumericalQuestion q) {
        try {
            writeQuestionTitle(q);
            if (showAnswers) {
                String tol = (q.getTolerance() != null && !q.getTolerance().equals("0")
                        && !q.getTolerance().isEmpty())
                        ? " (margen de error $\\pm$" + escapeLatex(q.getTolerance()) + ")" : "";
                writer.write("\\vspace{0.3cm}\n\\noindent \\textbf{Respuesta correcta:} "
                        + "\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{"
                        + processTextForLatex(q, q.getAnswer().getText())
                        + tol + formatLaTeXScore(q.getAnswer().getFraction()) + "}}}\n\n");
            } else {
                writer.write("\\vspace{1.5cm}\n\n");
            }
            writer.write("\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    @Override
    public void visit(MatchingQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\begin{emparejar}\n");
            char   letter      = 'A';
            int    numPairs    = q.getPairs().size();
            String pairFrac    = numPairs > 0 ? String.valueOf(100.0 / numPairs) : "0";
            String scoreBadge  = showAnswers ? formatLaTeXScore(pairFrac) : "";
            for (MatchingPair p : q.getPairs()) {
                String qLatex = processTextForLatex(q, p.getQuestionText())
                        .replaceAll("\\\\\\\\\\s*$", "").replace("\n", " ").trim();
                String aLatex = processTextForLatex(q, p.getAnswerText())
                        .replaceAll("\\\\\\\\\\s*$", "").replace("\n", " ").trim();
                if (showAnswers) {
                    writer.write("  \\cgoCorrectChoice{" + letter + "} " + qLatex
                            + " \\dotfill \\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{"
                            + aLatex + scoreBadge + "}}}\n");
                } else {
                    writer.write("  \\cgoCorrectChoice{} " + qLatex
                            + " \\dotfill \\rule{4cm}{0.4pt}\n");
                }
                letter++;
            }
            writer.write("\\end{emparejar}\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    /**
     * PUNTO 19: las preguntas Cloze se exportan reemplazando cada hueco por un control
     * adecuado en lugar de dejar solo un espacio vertical.
     *
     * <ul>
     *   <li>Modo examen: cada hueco se sustituye por {@code \rule{3cm}{0.4pt}}.</li>
     *   <li>Modo solucionario: la respuesta correcta (primera opción con {@code =} o
     *       fracción 100%) se muestra resaltada con el mismo estilo que en los otros
     *       tipos de pregunta.</li>
     * </ul>
     */
    @Override
    public void visit(ClozeQuestion q) {
        try {
            String processed = processTextForLatex(q, q.getText());
            // Sustituir cada token Cloze por su representación LaTeX
            processed = replaceClozeTokens(processed);
            writer.write("\\question \\textbf{" + escapeLatex(q.getName()) + "}\\\\\n"
                    + processed + "\n\n\\vspace{0.5cm}\n");
        } catch (IOException e) { log(e); }
    }

    @Override
    public void visit(GenericQuestion q) {
        try {
            writeQuestionTitle(q);
            writer.write("\\vspace{3cm}\n\n");
        } catch (IOException e) { log(e); }
    }

    // =========================================================================
    //  PUNTO 19: sustitución de tokens Cloze
    // =========================================================================

    private String replaceClozeTokens(String text) {
        Matcher m  = CLOZE_TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String type    = m.group(2).toUpperCase();
            String rawAlts = m.group(3);
            String replacement;

            if (showAnswers) {
                // Extraer la primera alternativa correcta (= o fracción 100%)
                String correct = extractCorrectClozeAnswer(rawAlts);
                replacement = "\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{"
                        + escapeLatex(correct) + "}}}";
            } else {
                // Modo examen: línea en blanco proporcional al tipo
                boolean isMulti = type.startsWith("MULTICHOICE") || type.startsWith("MC")
                        || type.startsWith("MULTIRESPONSE") || type.startsWith("MR");
                replacement = isMulti ? "\\rule{4cm}{0.4pt}" : "\\rule{3cm}{0.4pt}";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Extrae el texto de la primera alternativa marcada como correcta en un token Cloze. */
    private static String extractCorrectClozeAnswer(String rawAlts) {
        String[] parts = rawAlts.split("(?<!\\\\)~");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("=")) return cleanClozeOption(part.substring(1).trim(), false);
            if (part.matches("^%100(\\.0+)?%.*"))
                return cleanClozeOption(part.replaceFirst("^%100(\\.0+)?%", "").trim(), false);
        }
        // Si no hay correcta explícita, devolver la primera opción
        if (parts.length > 0) {
            String first = parts[0].trim();
            if (first.startsWith("=")) return cleanClozeOption(first.substring(1).trim(), false);
            return cleanClozeOption(first, false);
        }
        return "________";
    }

    // =========================================================================
    //  Escritura de títulos y procesado HTML→LaTeX
    // =========================================================================

    private void writeQuestionTitle(Question q) throws IOException {
        String processed = processTextForLatex(q, q.getText());
        writer.write("\\question \\textbf{" + escapeLatex(q.getName()) + "}\\\\\n"
                + processed + "\n\n");
    }

    private String processTextForLatex(Question q, String html) {
        if (html == null || html.isEmpty()) return "";

        Document  doc    = Jsoup.parseBodyFragment(html);
        Elements  images = doc.select("img");
        int       counter = 1;

        for (Element img : images) {
            String src = img.attr("src");
            if (src.matches("(?i)^data:image/([a-zA-Z]+);base64,(.*)")) {
                Matcher m = Pattern.compile("(?i)^data:image/([a-zA-Z]+);base64,(.*)").matcher(src);
                if (m.find()) {
                    String ext  = m.group(1);
                    String data = m.group(2).replaceAll("\\s+", "");
                    // PUNTO 29: nombre con MD5 para deduplicar
                    String filename = md5Filename(data, ext);
                    try {
                        File imageFile = new File(imagesDir, filename);
                        if (!imageFile.exists()) {
                            byte[] bytes = Base64.getDecoder().decode(data);
                            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                                fos.write(bytes);
                            }
                        }
                        img.attr("src", imagesFolderName + "/" + filename);
                        counter++;
                    } catch (Exception e) {
                        // PUNTO 2: registrar en lugar de silenciar
                        LOGGER.log(Level.WARNING,
                                "No se pudo extraer imagen Base64 para LaTeX: " + filename, e);
                    }
                }
            } else if (src.contains("@@PLUGINFILE@@/")) {
                String filename = src.substring(src.lastIndexOf("/") + 1);
                try { filename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8); }
                catch (Exception ignored) {}
                img.attr("src", imagesFolderName + "/" + filename);
            }
        }
        html = doc.body().html();
        saveImagesToDisk(q);
        html = formatClozeSyntax(html);
        String result = convertHtmlToLatexWithPandoc(html);
        return applyLatexFinalPatches(result);
    }

    private void saveImagesToDisk(Question q) {
        if (q.getFiles() == null) return;
        for (MoodleFile mf : q.getFiles()) {
            if (mf.getContent() == null || mf.getContent().isEmpty()) continue;
            try {
                String decodedName = java.net.URLDecoder.decode(
                        mf.getName(), StandardCharsets.UTF_8);
                File imageFile = new File(imagesDir, decodedName);
                if (!imageFile.exists()) {
                    byte[] bytes = Base64.getDecoder().decode(
                            mf.getContent().replaceAll("\\s+", ""));
                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        fos.write(bytes);
                    }
                }
            } catch (Exception e) {
                // PUNTO 2: registrar el error en lugar de silenciarlo
                LOGGER.log(Level.WARNING,
                        "No se pudo guardar imagen adjunta en disco: " + mf.getName(), e);
            }
        }
    }

    // =========================================================================
    //  PUNTO 1: invocar Pandoc por nombre (PATH), no por ruta hardcodeada
    // =========================================================================

    private String convertHtmlToLatexWithPandoc(String htmlText) {
        // PUNTO 1: "pandoc" resuelto por el PATH del sistema; funciona en Windows,
        // macOS y Linux sin importar la ruta de instalación del usuario.
        ProcessBuilder pb = new ProcessBuilder(
                "pandoc",
                "-f", "html+tex_math_dollars+tex_math_single_backslash",
                "-t", "latex");
        pb.redirectErrorStream(true);
        StringBuilder latex = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedWriter pw = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                pw.write(htmlText);
            }
            try (BufferedReader pr = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = pr.readLine()) != null) latex.append(line).append("\n");
            }
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Conversión Pandoc interrumpida", e);
            return htmlText;
        } catch (IOException e) {
            // PUNTO 1: mensaje claro si Pandoc no está en el PATH
            LOGGER.log(Level.WARNING,
                    "No se pudo ejecutar 'pandoc'. Asegúrate de que está instalado "
                    + "y disponible en el PATH del sistema.", e);
            return htmlText;
        }
        return latex.toString().trim();
    }

    // =========================================================================
    //  PUNTO 9: escapeLatex completo
    // =========================================================================

    /**
     * Escapa los caracteres especiales de LaTeX en texto plano.
     *
     * PUNTO 9: además de {@code %}, {@code _}, {@code #}, {@code &} y {@code $},
     * ahora también se escapan {@code \}, {@code ^}, {@code ~}, {@code {}, {@code }},
     * {@code <} y {@code >}, evitando errores de compilación con nombres de pregunta
     * o enunciados que contengan estos caracteres.
     *
     * El orden es importante: {@code \} debe escaparse PRIMERO para no reescapar
     * las barras introducidas por los escapes siguientes.
     */
    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\textbackslash{}")   // primero la barra
                .replace("{",  "\\{")
                .replace("}",  "\\}")
                .replace("%",  "\\%")
                .replace("_",  "\\_")
                .replace("#",  "\\#")
                .replace("&",  "\\&")
                .replace("$",  "\\$")
                .replace("^",  "\\textasciicircum{}")
                .replace("~",  "\\textasciitilde{}")
                .replace("<",  "\\textless{}")
                .replace(">",  "\\textgreater{}");
    }

    // =========================================================================
    //  Formato de puntuación
    // =========================================================================

    private String formatLaTeXScore(String fractionStr) {
        if (fractionStr == null || fractionStr.isEmpty() || "0".equals(fractionStr)) return "";
        try {
            double val = Double.parseDouble(fractionStr);
            String num = (val == Math.floor(val))
                    ? String.format("%.0f", val)
                    : String.format(Locale.US, "%.1f", val);
            return val > 0
                    ? " \\textbf{\\textcolor{azul}{(+" + num + "\\%)}}"
                    : " \\textbf{\\textcolor{red}{("  + num + "\\%)}}";
        } catch (NumberFormatException e) {
            return fractionStr.contains("-")
                    ? " \\textbf{\\textcolor{red}{(" + escapeLatex(fractionStr) + "\\%)}}"
                    : " \\textbf{\\textcolor{azul}{(" + escapeLatex(fractionStr) + "\\%)}}";
        }
    }

    // =========================================================================
    //  Utilidades Cloze y HTML→LaTeX
    // =========================================================================

    private String formatClozeSyntax(String html) {
        Pattern clozePattern = Pattern.compile("(?s)\\{\\d+:([A-Za-z0-9_]+):(.*?)\\}");
        Matcher matcher      = clozePattern.matcher(html);
        StringBuffer sb      = new StringBuffer();
        while (matcher.find()) {
            String   type    = matcher.group(1).toUpperCase();
            String[] options = matcher.group(2).split("(?<!\\\\)~");
            StringBuilder rep = new StringBuilder();
            if (type.contains("MULTICHOICE") || type.matches("MC[A-Z]*")) {
                rep.append(" XXMCSTARTXX ");
                for (String opt : options) {
                    opt = opt.trim(); if (opt.isEmpty()) continue;
                    boolean correct = opt.startsWith("=")
                            || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*")
                                && !opt.matches("^%0+(\\.0+)?%.*"));
                    String clean = cleanClozeOption(opt, false);
                    rep.append(showAnswers && correct ? " XXMCCORRECTXX " : " XXMCCHOICEXX ")
                       .append(clean).append(" ");
                }
                rep.append(" XXMCENDXX ");
            } else if (type.contains("MULTIRESPONSE") || type.matches("MR[A-Z]*")) {
                rep.append(" XXMRSTARTXX ");
                for (String opt : options) {
                    opt = opt.trim(); if (opt.isEmpty()) continue;
                    boolean correct = opt.startsWith("=")
                            || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*")
                                && !opt.matches("^%0+(\\.0+)?%.*"));
                    String clean = cleanClozeOption(opt, false);
                    rep.append(showAnswers && correct ? " XXMRCORRECTXX " : " XXMRCHOICEXX ")
                       .append(clean).append(" ");
                }
                rep.append(" XXMRENDXX ");
            } else {
                List<String> correctList = new ArrayList<>();
                for (String opt : options) {
                    opt = opt.trim();
                    boolean correct = opt.startsWith("=")
                            || (opt.matches("^%[0-9]+(\\.[0-9]+)?%.*")
                                && !opt.matches("^%0+(\\.0+)?%.*"));
                    if (correct)
                        correctList.add(cleanClozeOption(opt, type.contains("NUMERICAL") || type.equals("NM")));
                }
                boolean hasImgs = correctList.stream().anyMatch(a -> a.contains("<img"));
                if (showAnswers) {
                    String ans = correctList.isEmpty() ? "________"
                            : String.join(hasImgs ? " " : " / ", correctList);
                    rep.append(hasImgs
                            ? "<br><div style=\"text-align:center;\">XXSAANSIMGSTARTXX"
                              + Matcher.quoteReplacement(ans) + "XXSAANSIMGENDXX</div><br>"
                            : " XXSAANSSTARTXX" + Matcher.quoteReplacement(ans) + "XXSAANSENDXX ");
                } else {
                    rep.append(hasImgs ? "<br><div style=\"text-align:center;\">XXSABLANKIMGXX</div><br>"
                                       : " XXSABLANKXX ");
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String cleanClozeOption(String opt, boolean isNumerical) {
        String clean = opt.replaceFirst("^[=%]-?[0-9.]*%", "").replaceFirst("^=", "").trim();
        clean = clean.split("(?<!\\\\)#")[0].trim();
        if (isNumerical) clean = clean.replaceAll("(?<!\\\\):", " &plusmn; ");
        return clean.replace("\\~","~").replace("\\#","#")
                    .replace("\\=","=").replace("\\:",":")
                    .replace("\\}","}");
    }

    private String applyLatexFinalPatches(String result) {
        result = result
            .replace("XXSABLANKXX",     "\\rule{3cm}{0.4pt}")
            .replace("XXSABLANKIMGXX",  "\\rule{6cm}{0.4pt}")
            .replace("XXSAANSSTARTXX",  "\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{")
            .replace("XXSAANSENDXX",    "}}}")
            .replace("XXSAANSIMGSTARTXX","\\begin{center}\\fcolorbox{azul}{blue!5}{\\textcolor{azul}{\\textbf{}}\\\\\n")
            .replace("XXSAANSIMGENDXX", "\\\\\n}\\end{center}")
            .replace("XXMCSTARTXX",     "\\begin{unaRespuestaEnLinea}")
            .replace("XXMCENDXX",       "\\end{unaRespuestaEnLinea}")
            .replace("XXMCCORRECTXX",   "\\CorrectChoice ")
            .replace("XXMCCHOICEXX",    "\\choice ")
            .replace("XXMRSTARTXX",     "\\begin{variasRespuestasEnLinea}")
            .replace("XXMRENDXX",       "\\end{variasRespuestasEnLinea}")
            .replace("XXMRCORRECTXX",   "\\CorrectChoice ")
            .replace("XXMRCHOICEXX",    "\\choice ");

        result = result.replaceAll("(\\\\includegraphics\\[[^\\]\\n]*\\]\\{[^}\\n]*\\}~)\\s*\\n", "$1\\\\newline");
        result = result.replaceAll("(?m)(^\\s*(?:\\\\CorrectChoice|\\\\choice).*?)\\\\\\\\\\s*$", "$1");
        result = result.replaceAll("(?m)^\\s*\\\\\\\\\\s*$", "");
        result = result.replaceAll("\\\\begin\\{longtable\\}\\[.*?\\]", "\\\\begin{tabular}")
                       .replace("\\begin{longtable}",  "\\begin{tabular}")
                       .replace("\\end{longtable}",    "\\end{tabular}")
                       .replaceAll("(?m)^\\s*\\\\endhead\\s*$",      "")
                       .replaceAll("(?m)^\\s*\\\\endfirsthead\\s*$", "")
                       .replaceAll("(?m)^\\s*\\\\endfoot\\s*$",      "")
                       .replaceAll("(?m)^\\s*\\\\endlastfoot\\s*$",  "");

        // Envolver tablas en resizebox
        StringBuilder finalSb  = new StringBuilder();
        int           curIndex = 0;
        while (true) {
            int startIdx = result.indexOf("\\begin{tabular}", curIndex);
            if (startIdx == -1) { finalSb.append(result.substring(curIndex)); break; }
            int depth = 1, searchIdx = startIdx + 15, endIdx = -1;
            while (depth > 0 && searchIdx < result.length()) {
                int nb = result.indexOf("\\begin{tabular}", searchIdx);
                int ne = result.indexOf("\\end{tabular}",   searchIdx);
                if (ne == -1) break;
                if (nb != -1 && nb < ne) { depth++; searchIdx = nb + 15; }
                else                     { depth--; endIdx = ne; searchIdx = ne + 13; }
            }
            if (depth == 0 && endIdx != -1) {
                finalSb.append(result, curIndex, startIdx);
                String table = result.substring(startIdx, endIdx + 13);
                table = table.replace("\\bottomrule\\noalign{}", "");
                table = table.replace("\\end{tabular}", "\\bottomrule\\noalign{}\n\\end{tabular}");
                finalSb.append("\\begin{center}\n\\resizebox{0.95\\linewidth}{!}{\n")
                       .append(table).append("\n}\n\\end{center}");
                curIndex = endIdx + 13;
            } else { finalSb.append(result.substring(curIndex)); break; }
        }
        return finalSb.toString().trim();
    }

    // =========================================================================
    //  Utilidades
    // =========================================================================

    /** PUNTO 29: nombre de fichero basado en MD5 del contenido Base64. */
    private static String md5Filename(String base64Content, String ext) {
        try {
            MessageDigest md   = MessageDigest.getInstance("MD5");
            byte[]        hash = md.digest(
                    base64Content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex  = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return "img_" + hex + "." + ext;
        } catch (Exception e) {
            return "img_" + System.currentTimeMillis() + "." + ext;
        }
    }

    private void log(IOException e) {
        LOGGER.log(Level.SEVERE, "Error al exportar pregunta a LaTeX", e);
    }
}