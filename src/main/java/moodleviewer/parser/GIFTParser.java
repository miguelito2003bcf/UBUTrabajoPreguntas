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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GIFTParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^::(.*?)::(.*)", Pattern.DOTALL);
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile("(?i)^data:image/([a-zA-Z]+);base64,(.*)");

    public static Category parseGIFT(File file) throws Exception {
        Category rootCategory = new Category("Banco de Preguntas");
        Category currentCategory = rootCategory;

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            StringBuilder block = new StringBuilder();

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                
                if (trimmed.startsWith("$CATEGORY:")) {
                    String path = trimmed.substring(10).trim();
                    currentCategory = findOrCreateCategory(rootCategory, path);
                    continue;
                }

                if (trimmed.startsWith("//")) continue;

                if (trimmed.isEmpty()) {
                    if (block.length() > 0) {
                        processQuestionBlock(block.toString(), currentCategory);
                        block.setLength(0);
                    }
                } else {
                    block.append(line).append("\n");
                }
            }
            if (block.length() > 0) {
                processQuestionBlock(block.toString(), currentCategory);
            }
        }
        return rootCategory;
    }

    private static void processQuestionBlock(String block, Category currentCategory) {
        block = block.trim();
        String title = "Pregunta importada";
        String textAndAnswers = block;

        Matcher titleMatcher = TITLE_PATTERN.matcher(block);
        if (titleMatcher.find()) {
            title = titleMatcher.group(1).trim();
            textAndAnswers = titleMatcher.group(2).trim();
        }

        // Antes de parsear las llaves de GIFT, quitamos los escapes de caracteres de GIFT (\=, \~...)
        // para recuperar el HTML limpio original
        textAndAnswers = unescapeGIFT(textAndAnswers);

        int openBrace = textAndAnswers.lastIndexOf("{");
        int closeBrace = textAndAnswers.lastIndexOf("}");

        if (openBrace == -1 || closeBrace == -1) {
            GenericQuestion descQ = new GenericQuestion("description", title, cleanFormatTag(textAndAnswers), "0", "0");
            sanitizeQuestionImages(descQ);
            currentCategory.addQuestion(descQ);
            return;
        }

        String questionText = cleanFormatTag(textAndAnswers.substring(0, openBrace).trim());
        String answersBlock = textAndAnswers.substring(openBrace + 1, closeBrace).trim();

        Question q = createQuestionFromAnswers(title, questionText, answersBlock);
        if (q != null) currentCategory.addQuestion(q);
    }

    private static Question createQuestionFromAnswers(String title, String text, String answers) {
        Question q = null;

        if (answers.isEmpty()) {
            q = new GenericQuestion("essay", title, text, "1", "0.33");
        } else if (answers.equals("T") || answers.equals("TRUE") || answers.equals("F") || answers.equals("FALSE")) {
            boolean isTrue = answers.startsWith("T");
            Answer tAns = new Answer(isTrue ? "100" : "0", "Verdadero", "");
            Answer fAns = new Answer(!isTrue ? "100" : "0", "Falso", "");
            q = new TrueFalseQuestion("truefalse", title, text, "1", "1", tAns, fAns);
        } else if (answers.startsWith("#")) {
            String numVal = cleanFormatTag(answers.substring(1).trim());
            String tolerance = "0";
            if (numVal.contains(":")) {
                String[] parts = numVal.split(":");
                numVal = parts[0];
                tolerance = parts[1];
            }
            Answer a = new Answer("100", numVal, "");
            q = new NumericalQuestion("numerical", title, text, "1", "0.33", a, tolerance);
        } else if (answers.contains("->")) {
            List<MatchingPair> pairs = new ArrayList<>();
            String[] lines = answers.split("=");
            for (String line : lines) {
                if (line.contains("->")) {
                    String[] parts = line.split("->");
                    pairs.add(new MatchingPair(cleanFormatTag(parts[0].trim()), cleanFormatTag(parts[1].trim())));
                }
            }
            q = new MatchingQuestion("matching", title, text, "1", "0.33", pairs);
        } else {
            List<Answer> ansList = new ArrayList<>();
            boolean isShortAnswer = true;
            String[] options = answers.split("(?=[=~])");
            
            for (String opt : options) {
                opt = opt.trim();
                if (opt.isEmpty()) continue;
                if (opt.startsWith("~")) isShortAnswer = false;
                
                String fraction = opt.startsWith("=") ? "100" : "0";
                String ansText = cleanFormatTag(opt.substring(1).trim());
                ansList.add(new Answer(fraction, ansText, ""));
            }

            if (isShortAnswer) {
                q = new ShortAnswerQuestion("shortanswer", title, text, "1", "0.33", false, ansList);
            } else {
                q = new MultichoiceQuestion("multichoice", title, text, "1", "0.33", true, true, ansList);
            }
        }

        if (q != null) {
            // EXTRACCIÓN TRANSVERSAL: busca imágenes Base64 incrustadas tanto en el enunciado
            // como en todos los textos de respuesta asociados (según el tipo concreto de pregunta)
            // y las reescribe in-situ como @@PLUGINFILE@@, registrando el MoodleFile correspondiente.
            sanitizeQuestionImages(q);
        }

        return q;
    }

    /**
     * Analiza el HTML buscando imágenes incrustadas en Base64.
     * Si las encuentra, genera el objeto MoodleFile y reescribe el src como @@PLUGINFILE@@.
     *
     * @return el HTML resultante con los src ya reescritos (o el original si no había nada que hacer).
     */
    private static String extractInlineImages(String html, Question q) {
        if (html == null || !html.contains("data:image")) return html;
        
        Document doc = Jsoup.parseBodyFragment(html);
        Elements images = doc.select("img");
        int counter = q.getFiles().size() + 1;

        for (Element img : images) {
            String src = img.attr("src");
            Matcher m = DATA_IMAGE_PATTERN.matcher(src);
            if (m.find()) {
                String ext = m.group(1);
                String base64Content = m.group(2).replaceAll("\\s+", "");
                String filename = "gift_imported_img_" + System.currentTimeMillis() + "_" + counter + "." + ext;

                // Reconstruimos el objeto del modelo binario de tu aplicación
                MoodleFile mf = new MoodleFile(filename, "@@PLUGINFILE@@/" + filename, "base64", base64Content);
                q.getFiles().add(mf);

                // Saneamos el tag de la imagen para que coincida con el estándar XML
                img.attr("src", "@@PLUGINFILE@@/" + filename);
                counter++;
            }
        }
        return doc.body().html();
    }

    /**
     * Sanea todas las imágenes Base64 incrustadas de una pregunta recién creada: el enunciado
     * principal y, según el tipo concreto, los textos de respuesta asociados (opciones de
     * opción múltiple, respuesta numérica, pares de emparejamiento, etc.). Cada texto saneado
     * se reinyecta en el propio objeto mediante los setters de {@link Question} y {@link Answer},
     * de modo que ningún tipo de pregunta se queda con el Base64 original sin reescribir.
     */
    private static void sanitizeQuestionImages(Question q) {
        q.setText(extractInlineImages(q.getText(), q));

        if (q instanceof MultichoiceQuestion mc) {
            for (Answer a : mc.getAnswers()) {
                a.setText(extractInlineImages(a.getText(), q));
            }
        } else if (q instanceof ShortAnswerQuestion sa) {
            for (Answer a : sa.getAnswers()) {
                a.setText(extractInlineImages(a.getText(), q));
            }
        } else if (q instanceof NumericalQuestion nq) {
            nq.getAnswer().setText(extractInlineImages(nq.getAnswer().getText(), q));
        } else if (q instanceof MatchingQuestion mq) {
            // MatchingPair no expone setters; si en el futuro se necesita sanear imágenes
            // embebidas dentro de sus textos, habría que añadirle setQuestionText/setAnswerText.
            for (MatchingPair p : mq.getPairs()) {
                extractInlineImages(p.getQuestionText(), q);
                extractInlineImages(p.getAnswerText(), q);
            }
        }
    }

    private static String cleanFormatTag(String text) {
        if (text == null) return "";
        if (text.startsWith("[html]")) return text.substring(6).trim();
        if (text.startsWith("[moodle]")) return text.substring(8).trim();
        if (text.startsWith("[plain]")) return text.substring(7).trim();
        return text;
    }

    private static String unescapeGIFT(String text) {
        if (text == null) return "";
        return text.replace("\\~", "~")
                   .replace("\\=", "=")
                   .replace("\\#", "#")
                   .replace("\\{", "{")
                   .replace("\\}", "}")
                   .replace("\\:", ":")
                   .replace("\\\\", "\\");
    }

    private static Category findOrCreateCategory(Category root, String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) return root;
        String[] parts = fullPath.split("/");
        Category current = root;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (part.equalsIgnoreCase("top") || part.matches("(?i)^\\$[a-z0-9_]+\\$$")) continue;
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