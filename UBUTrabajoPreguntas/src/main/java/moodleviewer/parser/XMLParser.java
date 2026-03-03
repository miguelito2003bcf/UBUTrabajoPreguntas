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

                    Question q; // Declaramos el padre

                    // POLIMORFISMO: Instanciamos a la hija correspondiente
                    switch (type) {
                        case "matching":
                            List<String> pairs = new ArrayList<>();
                            NodeList subqs = questionElement.getElementsByTagName("subquestion");
                            for(int j=0; j<subqs.getLength(); j++){
                                Element sq = (Element)subqs.item(j);
                                String qT = getSimpleText(sq, "text");
                                String aT = getNestedText(sq, "answer", "text");
                                if(qT != null && aT != null) pairs.add(qT + "  ->  " + aT);
                            }
                            q = new MatchingQuestion(type, name, text, grade, penalty, pairs);
                            break;

                        case "multichoice":
                            boolean single = "true".equals(getSimpleText(questionElement, "single"));
                            boolean shuffleMulti = "true".equals(getSimpleText(questionElement, "shuffleanswers"));
                            List<String> answers = new ArrayList<>();
                            NodeList ansNodes = questionElement.getElementsByTagName("answer");
                            for(int j=0; j<ansNodes.getLength(); j++){
                                Element ansElem = (Element)ansNodes.item(j);
                                String fraction = ansElem.getAttribute("fraction");
                                String aText = getSimpleText(ansElem, "text");
                                if(aText != null) answers.add(aText + " (Valor: " + fraction + "%)");
                            }
                            q = new MultichoiceQuestion(type, name, text, grade, penalty, single, shuffleMulti, answers);
                            break;

                        case "truefalse":
                            String correctTf = "Desconocida";
                            NodeList tfNodes = questionElement.getElementsByTagName("answer");
                            for(int j=0; j<tfNodes.getLength(); j++){
                                Element ansElem = (Element)tfNodes.item(j);
                                if("100".equals(ansElem.getAttribute("fraction"))) correctTf = getSimpleText(ansElem, "text");
                            }
                            q = new TrueFalseQuestion(type, name, text, grade, penalty, correctTf);
                            break;

                        case "numerical":
                            String numAns = getNestedText(questionElement, "answer", "text");
                            String tolerance = getNestedText(questionElement, "answer", "tolerance");
                            q = new NumericalQuestion(type, name, text, grade, penalty, numAns, tolerance);
                            break;

                        case "shortanswer":
                            boolean caseSensitive = "1".equals(getSimpleText(questionElement, "usecase"));
                            String shortAns = "";
                            NodeList saNodes = questionElement.getElementsByTagName("answer");
                            for(int j=0; j<saNodes.getLength(); j++){
                                Element ansElem = (Element)saNodes.item(j);
                                if("100".equals(ansElem.getAttribute("fraction"))) shortAns = getSimpleText(ansElem, "text");
                            }
                            q = new ShortAnswerQuestion(type, name, text, grade, penalty, shortAns, caseSensitive);
                            break;

                        case "cloze":
                            q = new ClozeQuestion(type, name, text, grade, penalty);
                            break;

                        default:
                            q = new GenericQuestion(type, name, text, grade, penalty);
                            break;
                    }
                    currentCategory.addQuestion(q);
                }
            }
        }
        return rootCategory;
    }

    private static String getNestedText(Element parent, String outerTag, String innerTag) {
        NodeList outerList = parent.getElementsByTagName(outerTag);
        if (outerList.getLength() > 0) {
            Element outerElement = (Element) outerList.item(0);
            NodeList innerList = outerElement.getElementsByTagName(innerTag);
            if (innerList.getLength() > 0) return innerList.item(0).getTextContent().trim();
        }
        return null;
    }

    private static String getSimpleText(Element parent, String tag) {
        NodeList list = parent.getChildNodes();
        for (int i=0; i<list.getLength(); i++) {
            if (list.item(i).getNodeName().equals(tag)) return list.item(i).getTextContent().trim();
        }
        return null;
    }
}