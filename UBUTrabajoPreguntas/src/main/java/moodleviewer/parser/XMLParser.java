package moodleviewer.parser;

import moodleviewer.model.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XMLParser {

    public static Category parseMoodleXML(File xmlFile) throws Exception {
        Category rootCategory = new Category("Categorías de Moodle");
        Category currentCategory = rootCategory;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList questionNodes = document.getElementsByTagName("question");

        for (int i = 0; i < questionNodes.getLength(); i++) {
            Node node = questionNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element questionElement = (Element) node;
                String type = questionElement.getAttribute("type");

                if ("category".equals(type)) {
                    String fullPath = getNestedText(questionElement, "category", "text");
                    if (fullPath != null) {
                        fullPath = fullPath.replace("$module$/top/", "").replace("$course$/top/", "");
                        if (fullPath.equals("$module$/top") || fullPath.equals("$course$/top")) fullPath = "General";

                        String[] parts = fullPath.split("/");
                        Category navCategory = rootCategory;
                        for (String part : parts) {
                            part = part.trim();
                            if (part.isEmpty()) continue;
                            
                            Category found = null;
                            for (Category sub : navCategory.getSubcategories()) {
                                if (sub.getName().equals(part)) {
                                    found = sub;
                                    break;
                                }
                            }
                            if (found == null) {
                                found = new Category(part);
                                navCategory.addSubcategory(found);
                            }
                            navCategory = found;
                        }
                        currentCategory = navCategory;
                    }
                } else {
                    String name = getNestedText(questionElement, "name", "text");
                    String text = getNestedText(questionElement, "questiontext", "text");
                    String grade = getSimpleText(questionElement, "defaultgrade");
                    String penalty = getSimpleText(questionElement, "penalty");

                    List<MoodleFile> questionFiles = new ArrayList<>();
                    NodeList fileNodes = questionElement.getElementsByTagName("file");
                    for (int f = 0; f < fileNodes.getLength(); f++) {
                        Element fileElem = (Element) fileNodes.item(f);
                        String fName = fileElem.getAttribute("name");
                        String fPath = fileElem.getAttribute("path");
                        String fEncoding = fileElem.getAttribute("encoding");
                        String fContent = fileElem.getTextContent().trim();
                        questionFiles.add(new MoodleFile(fName, fPath, fEncoding, fContent));
                    }

                    Question q = QuestionFactory.createQuestion(type, name, text, grade, penalty, questionElement);
                    q.setFiles(questionFiles);
                    currentCategory.addQuestion(q);
                }
            }
        }
        return rootCategory;
    }

    public static String getNestedText(Element parent, String outerTag, String innerTag) {
        NodeList outerList = parent.getElementsByTagName(outerTag);
        if (outerList.getLength() > 0) {
            Element outerElement = (Element) outerList.item(0);
            NodeList innerList = outerElement.getElementsByTagName(innerTag);
            if (innerList.getLength() > 0) return innerList.item(0).getTextContent().trim();
        }
        return null;
    }

    public static String getSimpleText(Element parent, String tag) {
        NodeList list = parent.getChildNodes();
        for (int i=0; i<list.getLength(); i++) {
            if (list.item(i).getNodeName().equals(tag)) return list.item(i).getTextContent().trim();
        }
        return null;
    }
}