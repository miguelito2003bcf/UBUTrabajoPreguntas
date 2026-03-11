package moodleviewer.parser;

import moodleviewer.model.Category;
import moodleviewer.model.MoodleFile;
import moodleviewer.model.Question;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XMLExporter {

    public static void exportMoodleXML(Category rootCategory, File outputFile) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("quiz");
        doc.appendChild(rootElement);

        processCategory(rootCategory, rootElement, doc, "$course$/top");

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outputFile);
        transformer.transform(source, result);
    }

    private static void processCategory(Category category, Element parentElement, Document doc, String currentPath) {
        String categoryPath = currentPath;
        
        if (!category.getName().equals("Categorías de Moodle") && !category.getName().equals("General")) {
            categoryPath += "/" + category.getName();
            
            Element catQuestion = doc.createElement("question");
            catQuestion.setAttribute("type", "category");
            
            Element catElement = doc.createElement("category");
            Element textElement = doc.createElement("text");
            textElement.setTextContent(categoryPath);
            
            catElement.appendChild(textElement);
            catQuestion.appendChild(catElement);
            parentElement.appendChild(catQuestion);
        }

        for (Question q : category.getQuestions()) {
            Element qElement = doc.createElement("question");
            qElement.setAttribute("type", q.getType());

            Element nameElem = doc.createElement("name");
            Element nameText = doc.createElement("text");
            nameText.setTextContent(q.getName());
            nameElem.appendChild(nameText);
            qElement.appendChild(nameElem);

            Element qTextElem = doc.createElement("questiontext");
            qTextElem.setAttribute("format", "html");
            Element qTextStr = doc.createElement("text");
            qTextStr.setTextContent(q.getQuestionText());
            qTextElem.appendChild(qTextStr);
            
            if (q.getFiles() != null) {
                for (MoodleFile mf : q.getFiles()) {
                    Element fileElem = doc.createElement("file");
                    fileElem.setAttribute("name", mf.name);
                    fileElem.setAttribute("path", mf.path);
                    fileElem.setAttribute("encoding", mf.encoding);
                    fileElem.setTextContent(mf.content);
                    qTextElem.appendChild(fileElem);
                }
            }
            qElement.appendChild(qTextElem);

            if (q.getDefaultGrade() != null) {
                Element gradeElem = doc.createElement("defaultgrade");
                gradeElem.setTextContent(q.getDefaultGrade());
                qElement.appendChild(gradeElem);
            }
            if (q.getPenalty() != null) {
                Element penaltyElem = doc.createElement("penalty");
                penaltyElem.setTextContent(q.getPenalty());
                qElement.appendChild(penaltyElem);
            }


            parentElement.appendChild(qElement);
        }

        for (Category sub : category.getSubcategories()) {
            processCategory(sub, parentElement, doc, categoryPath);
        }
    }
}