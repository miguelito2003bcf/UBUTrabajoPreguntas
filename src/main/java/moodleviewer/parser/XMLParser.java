/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer.parser;

import moodleviewer.model.*;
import org.w3c.dom.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser del formato XML de exportación de bancos de preguntas de Moodle.
 *
 * PUNTO 6: {@link #getNestedText} ahora busca el elemento padre por hijo directo
 * en lugar de usar {@code getElementsByTagName}, que devuelve descendientes de
 * cualquier nivel y podía capturar el texto de un bloque de retroalimentación
 * anidado en vez del enunciado principal.
 */
public class XMLParser {

    public static Category parseMoodleXML(File xmlFile) throws Exception {
        Category rootCategory    = new Category("Banco de Preguntas");
        Category currentCategory = rootCategory;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList questionList = document.getElementsByTagName("question");

        for (int i = 0; i < questionList.getLength(); i++) {
            Node node = questionList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element element = (Element) node;
            String type = element.getAttribute("type");

            if ("category".equals(type)) {
                String fullPath = getNestedText(element, "category", "text");
                if (fullPath != null) {
                    currentCategory = findOrCreateCategory(rootCategory, fullPath);
                }
            } else if (!"info".equals(type)) {
                String name    = getNestedText(element, "name", "text");
                String text    = getNestedText(element, "questiontext", "text");
                String grade   = getSimpleText(element, "defaultgrade");
                String penalty = getSimpleText(element, "penalty");

                Question question = QuestionFactory.createQuestion(
                        type, name, text, grade, penalty, element);

                question.setGeneralFeedback(getNestedText(element, "generalfeedback", "text"));
                question.setFiles(extractFiles(element));
                currentCategory.addQuestion(question);
            }
        }
        return rootCategory;
    }

    // -------------------------------------------------------------------------
    //  Extracción de ficheros adjuntos
    // -------------------------------------------------------------------------

    private static List<MoodleFile> extractFiles(Element questionElement) {
        List<MoodleFile> files = new ArrayList<>();
        NodeList fileNodes = questionElement.getElementsByTagName("file");
        for (int j = 0; j < fileNodes.getLength(); j++) {
            Element fe = (Element) fileNodes.item(j);
            files.add(new MoodleFile(
                    fe.getAttribute("name"),
                    fe.getAttribute("path"),
                    fe.getAttribute("encoding"),
                    fe.getTextContent()));
        }
        return files;
    }

    // -------------------------------------------------------------------------
    //  PUNTO 6: lectura de texto buscando solo hijos directos del elemento padre,
    //  no descendientes de cualquier nivel.
    // -------------------------------------------------------------------------

    /**
     * Extrae el texto contenido en {@code <childTag>} que es hijo directo del primer
     * {@code <parentTag>} que es hijo directo de {@code parent}.
     *
     * <p>A diferencia del uso anterior de {@code getElementsByTagName}, este método
     * itera únicamente los hijos inmediatos de cada nivel, evitando que el texto de
     * un bloque de retroalimentación anidado contamine el enunciado principal.</p>
     */
    public static String getNestedText(Element parent, String parentTag, String childTag) {
        // Buscar el primer hijo directo con nombre parentTag
        Element parentEl = firstDirectChild(parent, parentTag);
        if (parentEl == null) return null;

        // Buscar el primer hijo directo de ese elemento con nombre childTag
        Element childEl = firstDirectChild(parentEl, childTag);
        if (childEl == null) return null;

        String rawHtml = childEl.getTextContent().trim();
        return Jsoup.clean(rawHtml,
                Safelist.relaxed().addAttributes("img", "src", "alt", "width", "height"));
    }

    /**
     * Extrae el texto de un hijo directo de {@code parent} con nombre {@code tag}.
     * Para tags de contenido HTML ({@code text}, {@code feedback}) aplica saneado JSoup.
     */
    public static String getSimpleText(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!child.getNodeName().equals(tag)) continue;
            String rawText = child.getTextContent().trim();
            if ("text".equals(tag) || "feedback".equals(tag)) {
                return Jsoup.clean(rawText,
                        Safelist.relaxed().addAttributes("img", "src", "alt", "width", "height"));
            }
            return rawText;
        }
        return null;
    }

    /**
     * Devuelve el primer hijo directo de {@code parent} cuyo nombre de nodo sea {@code tagName},
     * o {@code null} si no existe ninguno.
     */
    private static Element firstDirectChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && child.getNodeName().equals(tagName)) {
                return (Element) child;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    //  Navegación de categorías
    // -------------------------------------------------------------------------

    private static Category findOrCreateCategory(Category root, String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) return root;
        String[] parts = fullPath.split("/");
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