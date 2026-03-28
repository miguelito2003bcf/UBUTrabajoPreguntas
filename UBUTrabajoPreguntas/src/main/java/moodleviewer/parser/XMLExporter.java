package moodleviewer.parser;

import moodleviewer.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XMLExporter {

    public static void exportMoodleXML(Category rootCategory, File file) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        doc.setXmlStandalone(true); 
        
        Element rootQuiz = doc.createElement("quiz");
        doc.appendChild(rootQuiz);

        exportCategoryRecursive(rootCategory, "$course$/top", rootQuiz, doc);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    private static void exportCategoryRecursive(Category category, String currentPath, Element parentNode, Document doc) throws Exception {
        String newPath = currentPath;
        if (!category.getName().equals("Banco de Preguntas")) {
            newPath = currentPath + "/" + category.getName();
            
            Element catQuestion = doc.createElement("question");
            catQuestion.setAttribute("type", "category");
            
            Element cat = doc.createElement("category");
            addTextNode(doc, cat, "text", newPath);
            catQuestion.appendChild(cat);
            
            addCdataNode(doc, catQuestion, "info", ""); 
            parentNode.appendChild(catQuestion);
        }

        for (Question q : category.getQuestions()) writeQuestion(q, parentNode, doc);
        for (Category sub : category.getSubcategories()) exportCategoryRecursive(sub, newPath, parentNode, doc);
    }

    private static void writeQuestion(Question q, Element parentNode, Document doc) throws Exception {
        Element qEl = doc.createElement("question");
        qEl.setAttribute("type", q.getType());
        
        Element nameEl = doc.createElement("name");
        Element nameTxt = doc.createElement("text");
        nameTxt.appendChild(doc.createCDATASection(q.getName() != null ? q.getName() : ""));
        nameEl.appendChild(nameTxt);
        qEl.appendChild(nameEl);

        Element qtextEl = doc.createElement("questiontext");
        qtextEl.setAttribute("format", "html");
        Element txt = doc.createElement("text");
        txt.appendChild(doc.createCDATASection(q.getText() != null ? q.getText() : ""));
        qtextEl.appendChild(txt);
        
        if (q.getFiles() != null && !q.getFiles().isEmpty()) {
            for (MoodleFile f : q.getFiles()) {
                Element fileEl = doc.createElement("file");
                fileEl.setAttribute("name", f.name);
                fileEl.setAttribute("path", f.path);
                fileEl.setAttribute("encoding", f.encoding);
                fileEl.setTextContent(f.content);
                qtextEl.appendChild(fileEl);
            }
        }
        qEl.appendChild(qtextEl);

        addTextNode(doc, qEl, "defaultgrade", q.getDefaultGrade() != null ? q.getDefaultGrade() : "1");
        addTextNode(doc, qEl, "penalty", q.getPenalty() != null ? q.getPenalty() : "0.3333333");
        addTextNode(doc, qEl, "hidden", "0");

        if (q.getGeneralFeedback() != null && !q.getGeneralFeedback().isEmpty()) {
            addCdataNode(doc, qEl, "generalfeedback", q.getGeneralFeedback());
        }

        if (q instanceof TrueFalseQuestion) {
            TrueFalseQuestion tfq = (TrueFalseQuestion) q;
            writeTFAnswer(tfq.getTrueAnswer(), "true", qEl, doc);
            writeTFAnswer(tfq.getFalseAnswer(), "false", qEl, doc);
            
        } else if (q instanceof MultichoiceQuestion) {
            MultichoiceQuestion mcq = (MultichoiceQuestion) q;
            addTextNode(doc, qEl, "single", mcq.isSingleAnswer() ? "true" : "false");
            addTextNode(doc, qEl, "shuffleanswers", mcq.isShuffleAnswers() ? "true" : "false");
            addTextNode(doc, qEl, "answernumbering", "abc");
            for (Answer a : mcq.getAnswers()) writeAnswer(a, qEl, doc);
            
        } else if (q instanceof ShortAnswerQuestion) {
            ShortAnswerQuestion saq = (ShortAnswerQuestion) q;
            addTextNode(doc, qEl, "usecase", saq.isCaseSensitive() ? "1" : "0");
            for (Answer a : saq.getAnswers()) writeAnswer(a, qEl, doc);
            
        } else if (q instanceof MatchingQuestion) {
            MatchingQuestion mq = (MatchingQuestion) q;
            addTextNode(doc, qEl, "shuffleanswers", "true");
            for (MatchingPair p : mq.getPairs()) {
                Element subqEl = doc.createElement("subquestion");
                subqEl.setAttribute("format", "html");
                
                Element subText = doc.createElement("text");
                subText.appendChild(doc.createCDATASection(p.getQuestionText()));
                subqEl.appendChild(subText);
                
                Element ansEl = doc.createElement("answer");
                addTextNode(doc, ansEl, "text", p.getAnswerText());
                subqEl.appendChild(ansEl);
                
                qEl.appendChild(subqEl);
            }
            
        } else if (q instanceof NumericalQuestion) {
            NumericalQuestion nq = (NumericalQuestion) q;
            Element ansEl = doc.createElement("answer");
            ansEl.setAttribute("fraction", nq.getAnswer().getFraction());
            ansEl.setAttribute("format", "moodle_auto_format");
            addTextNode(doc, ansEl, "text", nq.getAnswer().getText());
            if (nq.getAnswer().getFeedback() != null && !nq.getAnswer().getFeedback().isEmpty()) {
                addCdataNode(doc, ansEl, "feedback", nq.getAnswer().getFeedback());
            }
            addTextNode(doc, ansEl, "tolerance", nq.getTolerance());
            qEl.appendChild(ansEl);
        }
        parentNode.appendChild(qEl);
    }

    private static void writeAnswer(Answer a, Element parentNode, Document doc) {
        Element ansEl = doc.createElement("answer");
        ansEl.setAttribute("fraction", a.getFraction() != null ? a.getFraction() : "0");
        ansEl.setAttribute("format", "html");
        Element textEl = doc.createElement("text");
        textEl.appendChild(doc.createCDATASection(a.getText() != null ? a.getText() : ""));
        ansEl.appendChild(textEl);
        if (a.getFeedback() != null && !a.getFeedback().isEmpty()) {
            addCdataNode(doc, ansEl, "feedback", a.getFeedback());
        }
        parentNode.appendChild(ansEl);
    }

    private static void writeTFAnswer(Answer a, String tfValue, Element parentNode, Document doc) {
        Element ansEl = doc.createElement("answer");
        ansEl.setAttribute("fraction", a.getFraction() != null ? a.getFraction() : "0");
        ansEl.setAttribute("format", "moodle_auto_format"); 
        
        Element textEl = doc.createElement("text");
        textEl.setTextContent(tfValue); 
        ansEl.appendChild(textEl);
        
        if (a.getFeedback() != null && !a.getFeedback().isEmpty()) {
            addCdataNode(doc, ansEl, "feedback", a.getFeedback());
        }
        parentNode.appendChild(ansEl);
    }

    private static void addTextNode(Document doc, Element parent, String tagName, String textContent) {
        Element el = doc.createElement(tagName);
        el.setTextContent(textContent != null ? textContent : "");
        parent.appendChild(el);
    }

    private static void addCdataNode(Document doc, Element parent, String tagName, String cdataContent) {
        Element el = doc.createElement(tagName);
        el.setAttribute("format", "html");
        Element textEl = doc.createElement("text");
        textEl.appendChild(doc.createCDATASection(cdataContent != null ? cdataContent : ""));
        el.appendChild(textEl);
        parent.appendChild(el);
    }
}