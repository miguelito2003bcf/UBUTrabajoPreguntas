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
import org.w3c.dom.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase creada como parseadora del formato XML de exportación de bancos de preguntas de Moodle.
 * Transforma un fichero XML de Moodle en una jerarquía limpia gracias a JSoup.
 */
public class XMLParser {

    public static Category parseMoodleXML(File xmlFile) throws Exception {
        Category rootCategory = new Category("Banco de Preguntas");
        Category currentCategory = rootCategory;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList questionList = document.getElementsByTagName("question");

        for (int i = 0; i < questionList.getLength(); i++) {
            Node node = questionList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String type = element.getAttribute("type");

                if ("category".equals(type)) {
                    String fullPath = getNestedText(element, "category", "text");
                    if (fullPath != null) {
                        currentCategory = findOrCreateCategory(rootCategory, fullPath);
                    }
                } else if (!"info".equals(type)) {
                    String name = getNestedText(element, "name", "text");
                    String text = getNestedText(element, "questiontext", "text");
                    String grade = getSimpleText(element, "defaultgrade");
                    String penalty = getSimpleText(element, "penalty");

                    Question question = QuestionFactory.createQuestion(type, name, text, grade, penalty, element);

                    String generalFeedback = getNestedText(element, "generalfeedback", "text");
                    question.setGeneralFeedback(generalFeedback);

                    List<MoodleFile> files = extractFiles(element);
                    question.setFiles(files);

                    currentCategory.addQuestion(question);
                }
            }
        }
        return rootCategory;
    }

    private static List<MoodleFile> extractFiles(Element questionElement) {
        List<MoodleFile> files = new ArrayList<>();
        NodeList fileNodes = questionElement.getElementsByTagName("file");
        for (int j = 0; j < fileNodes.getLength(); j++) {
            Element fileElem = (Element) fileNodes.item(j);
            String name = fileElem.getAttribute("name");
            String path = fileElem.getAttribute("path");
            String encoding = fileElem.getAttribute("encoding");
            String content = fileElem.getTextContent();
            files.add(new MoodleFile(name, path, encoding, content));
        }
        return files;
    }

    /**
     * Extrae el texto anidado y utiliza JSoup para limpiar etiquetas HTML basura.
     */
    public static String getNestedText(Element parent, String parentTag, String childTag) {
        NodeList list = parent.getElementsByTagName(parentTag);
        if (list.getLength() > 0) {
            Element pElement = (Element) list.item(0);
            NodeList childList = pElement.getElementsByTagName(childTag);
            if (childList.getLength() > 0) {
                String rawHtml = childList.item(0).getTextContent().trim();
                // Safelist.relaxed() permite negritas, cursivas, listas e imágenes seguras, limpiando el resto.
                return Jsoup.clean(rawHtml, Safelist.relaxed().addAttributes("img", "src", "alt", "width", "height"));
            }
        }
        return null;
    }

    /**
     * Extrae el texto directo. Si es contenido enriquecido ("text" o "feedback"), lo sanea con JSoup.
     */
    public static String getSimpleText(Element parent, String tag) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName().equals(tag)) {
                String rawText = list.item(i).getTextContent().trim();
                if (tag.equals("text") || tag.equals("feedback")) {
                    return Jsoup.clean(rawText, Safelist.relaxed().addAttributes("img", "src", "alt", "width", "height"));
                }
                return rawText;
            }
        }
        return null;
    }

    private static Category findOrCreateCategory(Category root, String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) return root;
        String[] parts = fullPath.split("/");
        Category current = root;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            // Ignoramos los segmentos técnicos que usa Moodle para anclar la ruta de categorías:
            // "top" y cualquier placeholder de contexto entre signos de dólar
            // ($course$, $module$, $system$, etc.), ya que no son categorías reales del usuario.
            if (part.equalsIgnoreCase("top") || part.matches("(?i)^\\$[a-z0-9_]+\\$$")) continue;
            Category next = null;
            for (Category sub : current.getSubcategories()) {
                if (sub.getName().equals(part)) {
                    next = sub;
                    break;
                }
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