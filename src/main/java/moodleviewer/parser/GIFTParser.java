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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser del formato GIFT de Moodle.
 *
 * Correcciones incluidas:
 * <ul>
 *   <li>PUNTO  3: detección de preguntas Cloze en {@link #createQuestionFromAnswers}.</li>
 *   <li>PUNTO 10: {@link #unescapeGIFT} deshace {@code \\} PRIMERO para evitar
 *       conversión incorrecta de secuencias como {@code \\~}.</li>
 *   <li>PUNTO 25: fracciones negativas en opción múltiple se conservan en el objeto
 *       {@link Answer} en lugar de convertirse a {@code "0"}.</li>
 *   <li>PUNTO 29: nombres de imagen generados con hash MD5 del contenido Base64
 *       en lugar de {@code System.currentTimeMillis()}, deduplicando automáticamente
 *       imágenes repetidas entre preguntas.</li>
 * </ul>
 */
public class GIFTParser {

    private static final Pattern TITLE_PATTERN       = Pattern.compile("^::(.*?)::(.*)", Pattern.DOTALL);
    private static final Pattern DATA_IMAGE_PATTERN  = Pattern.compile("(?i)^data:image/([a-zA-Z]+);base64,(.*)");
    /** Patrón para detectar huecos Cloze incrustados en el enunciado ({n:TIPO:...}). */
    private static final Pattern CLOZE_PATTERN       = Pattern.compile("\\{\\d*:[A-Za-z_]+:.*?\\}", Pattern.DOTALL);
    /** Patrón para fracciones de penalización en opciones GIFT, p.ej. %-50%. */
    private static final Pattern FRACTION_PATTERN    = Pattern.compile("^%(-?[0-9]+(?:[.,][0-9]*)?)%");

    // -------------------------------------------------------------------------
    //  Records públicos
    // -------------------------------------------------------------------------

    public record GiftParseIssue(int startLine, String blockPreview, String reason) {}
    public record GiftImportResult(Category rootCategory, List<GiftParseIssue> issues) {}

    // -------------------------------------------------------------------------
    //  API pública
    // -------------------------------------------------------------------------

    public static Category parseGIFT(File file) throws Exception {
        return parseGIFTWithReport(file).rootCategory();
    }

    public static GiftImportResult parseGIFTWithReport(File file) throws Exception {
        Category rootCategory    = new Category("Banco de Preguntas");
        Category currentCategory = rootCategory;
        List<GiftParseIssue> issues = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            StringBuilder block = new StringBuilder();
            int lineNumber    = 0;
            int blockStartLine = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                if (trimmed.startsWith("$CATEGORY:")) {
                    String path = trimmed.substring(10).trim();
                    currentCategory = findOrCreateCategory(rootCategory, path);
                    continue;
                }
                if (trimmed.startsWith("//")) continue;

                if (trimmed.isEmpty()) {
                    if (block.length() > 0) {
                        processQuestionBlockSafely(
                                block.toString(), currentCategory, blockStartLine, issues);
                        block.setLength(0);
                    }
                    blockStartLine = lineNumber + 1;
                } else {
                    if (block.length() == 0) blockStartLine = lineNumber;
                    block.append(line).append("\n");
                }
            }
            if (block.length() > 0) {
                processQuestionBlockSafely(
                        block.toString(), currentCategory, blockStartLine, issues);
            }
        }
        return new GiftImportResult(rootCategory, issues);
    }

    // -------------------------------------------------------------------------
    //  Procesamiento de bloques
    // -------------------------------------------------------------------------

    private static void processQuestionBlockSafely(
            String block, Category category, int startLine, List<GiftParseIssue> issues) {
        try {
            processQuestionBlock(block, category);
        } catch (Exception ex) {
            String preview = block.trim().replaceAll("\\s+", " ");
            if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            issues.add(new GiftParseIssue(startLine, preview, reason));
        }
    }

    private static void processQuestionBlock(String block, Category category) {
        block = block.trim();
        String title          = "Pregunta importada";
        String textAndAnswers = block;

        Matcher titleMatcher = TITLE_PATTERN.matcher(block);
        if (titleMatcher.find()) {
            title         = titleMatcher.group(1).trim();
            textAndAnswers = titleMatcher.group(2).trim();
        }

        // PUNTO 10: unescapeGIFT ahora deshace \\ PRIMERO
        textAndAnswers = unescapeGIFT(textAndAnswers);

        int openBrace  = textAndAnswers.lastIndexOf("{");
        int closeBrace = textAndAnswers.lastIndexOf("}");

        if (openBrace == -1 || closeBrace == -1) {
            GenericQuestion descQ = new GenericQuestion(
                    "description", title, cleanFormatTag(textAndAnswers), "0", "0");
            sanitizeQuestionImages(descQ);
            category.addQuestion(descQ);
            return;
        }

        String questionText  = cleanFormatTag(textAndAnswers.substring(0, openBrace).trim());
        String answersBlock  = textAndAnswers.substring(openBrace + 1, closeBrace).trim();

        Question q = createQuestionFromAnswers(title, questionText, answersBlock);
        if (q != null) category.addQuestion(q);
    }

    // -------------------------------------------------------------------------
    //  Creación de preguntas
    // -------------------------------------------------------------------------

    private static Question createQuestionFromAnswers(String title, String text, String answers) {
        Question q = null;

        if (answers.isEmpty()) {
            // PUNTO 3: antes de crear un essay, comprobar si el texto contiene huecos Cloze
            if (CLOZE_PATTERN.matcher(text).find()) {
                q = new ClozeQuestion("cloze", title, text, "1", "0.33");
            } else {
                q = new GenericQuestion("essay", title, text, "1", "0.33");
            }

        } else if (answers.equals("T") || answers.equals("TRUE")
                || answers.equals("F") || answers.equals("FALSE")) {
            boolean isTrue = answers.startsWith("T");
            q = new TrueFalseQuestion("truefalse", title, text, "1", "1",
                    new Answer(isTrue  ? "100" : "0", "Verdadero", ""),
                    new Answer(!isTrue ? "100" : "0", "Falso",     ""));

        } else if (answers.startsWith("#")) {
            String numVal   = cleanFormatTag(answers.substring(1).trim());
            String tolerance = "0";
            if (numVal.contains(":")) {
                String[] parts = numVal.split(":");
                numVal    = parts[0];
                tolerance = parts[1];
            }
            q = new NumericalQuestion("numerical", title, text, "1", "0.33",
                    new Answer("100", numVal, ""), tolerance);

        } else if (answers.contains("->")) {
            List<MatchingPair> pairs = new ArrayList<>();
            for (String line : answers.split("=")) {
                if (!line.contains("->")) continue;
                String[] parts = line.split("->");
                pairs.add(new MatchingPair(
                        cleanFormatTag(parts[0].trim()),
                        cleanFormatTag(parts[1].trim())));
            }
            q = new MatchingQuestion("matching", title, text, "1", "0.33", pairs);

        } else {
            List<Answer> ansList    = new ArrayList<>();
            boolean isShortAnswer  = true;
            String[] options       = answers.split("(?=[=~])");

            for (String opt : options) {
                opt = opt.trim();
                if (opt.isEmpty()) continue;
                if (opt.startsWith("~")) isShortAnswer = false;

                // PUNTO 25: parsear fracción real (incluye negativos como %-50%)
                String fraction;
                String ansText;
                if (opt.startsWith("=")) {
                    fraction = "100";
                    ansText  = cleanFormatTag(opt.substring(1).trim());
                } else {
                    // opt empieza con '~', extraer posible fracción numérica
                    String rest = opt.substring(1).trim();
                    Matcher fm  = FRACTION_PATTERN.matcher(rest);
                    if (fm.find()) {
                        // Convertir p.ej. "-50" a "-50" (ya es cadena), o "100" a "100"
                        fraction = fm.group(1).replace(',', '.');
                        ansText  = cleanFormatTag(rest.substring(fm.end()).trim());
                    } else {
                        fraction = "0";
                        ansText  = cleanFormatTag(rest);
                    }
                }
                ansList.add(new Answer(fraction, ansText, ""));
            }

            q = isShortAnswer
                    ? new ShortAnswerQuestion("shortanswer", title, text, "1", "0.33", false, ansList)
                    : new MultichoiceQuestion("multichoice", title, text, "1", "0.33", true, true, ansList);
        }

        if (q != null) sanitizeQuestionImages(q);
        return q;
    }

    // -------------------------------------------------------------------------
    //  Saneado de imágenes
    // -------------------------------------------------------------------------

    /**
     * Busca imágenes Base64 incrustadas en el HTML y las extrae como {@link MoodleFile}.
     *
     * PUNTO 29: el nombre del archivo se genera a partir del hash MD5 del contenido
     * Base64, de modo que dos preguntas con la misma imagen producen el mismo nombre
     * de fichero y no se almacenan copias duplicadas.
     */
    private static String extractInlineImages(String html, Question q) {
        if (html == null || !html.contains("data:image")) return html;

        Document  doc    = Jsoup.parseBodyFragment(html);
        Elements  images = doc.select("img");

        for (Element img : images) {
            String src = img.attr("src");
            Matcher m  = DATA_IMAGE_PATTERN.matcher(src);
            if (!m.find()) continue;

            String ext          = m.group(1);
            String base64Content = m.group(2).replaceAll("\\s+", "");

            // PUNTO 29: nombre basado en MD5 del contenido para deduplicar
            String filename = md5Filename(base64Content, ext);

            // Evitar añadir el mismo fichero dos veces (misma imagen en distintas preguntas)
            boolean alreadyAdded = q.getFiles().stream()
                    .anyMatch(f -> f.getName().equals(filename));
            if (!alreadyAdded) {
                q.getFiles().add(new MoodleFile(
                        filename,
                        "@@PLUGINFILE@@/" + filename,
                        "base64",
                        base64Content));
            }
            img.attr("src", "@@PLUGINFILE@@/" + filename);
        }
        return doc.body().html();
    }

    /** Calcula el hash MD5 de los datos Base64 y lo usa como nombre de fichero. */
    private static String md5Filename(String base64Content, String ext) {
        try {
            MessageDigest md  = MessageDigest.getInstance("MD5");
            byte[] hash       = md.digest(base64Content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "img_" + HexFormat.of().formatHex(hash) + "." + ext;
        } catch (Exception e) {
            // Fallback con timestamp solo si MD5 no está disponible (nunca debería ocurrir)
            return "img_" + System.currentTimeMillis() + "." + ext;
        }
    }

    private static void sanitizeQuestionImages(Question q) {
        q.setText(extractInlineImages(q.getText(), q));

        if (q instanceof MultichoiceQuestion mc) {
            for (Answer a : mc.getAnswers())
                a.setText(extractInlineImages(a.getText(), q));
        } else if (q instanceof ShortAnswerQuestion sa) {
            for (Answer a : sa.getAnswers())
                a.setText(extractInlineImages(a.getText(), q));
        } else if (q instanceof NumericalQuestion nq) {
            nq.getAnswer().setText(extractInlineImages(nq.getAnswer().getText(), q));
        } else if (q instanceof MatchingQuestion mq) {
            for (MatchingPair p : mq.getPairs()) {
                p.setQuestionText(extractInlineImages(p.getQuestionText(), q));
                p.setAnswerText(extractInlineImages(p.getAnswerText(), q));
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Utilidades de texto
    // -------------------------------------------------------------------------

    private static String cleanFormatTag(String text) {
        if (text == null) return "";
        if (text.startsWith("[html]"))   return text.substring(6).trim();
        if (text.startsWith("[moodle]")) return text.substring(8).trim();
        if (text.startsWith("[plain]"))  return text.substring(7).trim();
        return text;
    }

    /**
     * Deshace los escapes GIFT.
     *
     * PUNTO 10: {@code \\\\} → {@code \\} se aplica PRIMERO para evitar que una
     * secuencia como {@code \\~} se convierta incorrectamente en {@code ~} en vez
     * de en {@code \~}.
     */
    private static String unescapeGIFT(String text) {
        if (text == null) return "";
        // 1. Barra doble → barra simple (PRIMERO, antes de los demás escapes)
        return text.replace("\\\\", "\\")
                   .replace("\\~", "~")
                   .replace("\\=", "=")
                   .replace("\\#", "#")
                   .replace("\\{", "{")
                   .replace("\\}", "}")
                   .replace("\\:", ":");
    }

    private static Category findOrCreateCategory(Category root, String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) return root;
        String[] parts  = fullPath.split("/");
        Category current = root;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (part.equalsIgnoreCase("top")
                    || part.matches("(?i)^\\$[a-z0-9_]+\\$$")) continue;
            Category next = null;
            for (Category sub : current.getSubcategories()) {
                if (sub.getName().equals(part)) { next = sub; break; }
            }
            if (next == null) {
                next = new Category(part);
                current.addSubcategory(next);
            }
            current = next;
        }
        return current;
    }
}