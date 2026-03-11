package moodleviewer.parser;

import moodleviewer.model.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.List;

public class QuestionFactory {

    public static Question createQuestion(String type, String name, String text, String grade, String penalty, Element questionElement) {
        
        switch (type) {
            case "matching":
                List<String> pairs = new ArrayList<>();
                NodeList subqs = questionElement.getElementsByTagName("subquestion");
                for (int j = 0; j < subqs.getLength(); j++) {
                    Element sq = (Element) subqs.item(j);
                    String qT = XMLParser.getSimpleText(sq, "text");
                    String aT = XMLParser.getNestedText(sq, "answer", "text");
                    if (qT != null && aT != null) pairs.add(qT + "  ->  " + aT);
                }
                return new MatchingQuestion(type, name, text, grade, penalty, pairs);

            case "multichoice":
                boolean single = "true".equals(XMLParser.getSimpleText(questionElement, "single"));
                boolean shuffleMulti = "true".equals(XMLParser.getSimpleText(questionElement, "shuffleanswers"));
                List<String> answers = new ArrayList<>();
                NodeList ansNodes = questionElement.getElementsByTagName("answer");
                for (int j = 0; j < ansNodes.getLength(); j++) {
                    Element ansElem = (Element) ansNodes.item(j);
                    String fraction = ansElem.getAttribute("fraction");
                    String aText = XMLParser.getSimpleText(ansElem, "text");
                    if (aText != null) answers.add(aText + " (Valor: " + fraction + "%)");
                }
                return new MultichoiceQuestion(type, name, text, grade, penalty, single, shuffleMulti, answers);

            case "truefalse":
                String correctTf = "Desconocida";
                NodeList tfNodes = questionElement.getElementsByTagName("answer");
                for (int j = 0; j < tfNodes.getLength(); j++) {
                    Element ansElem = (Element) tfNodes.item(j);
                    if ("100".equals(ansElem.getAttribute("fraction"))) {
                        correctTf = XMLParser.getSimpleText(ansElem, "text");
                    }
                }
                return new TrueFalseQuestion(type, name, text, grade, penalty, correctTf);

            case "numerical":
                String numAns = XMLParser.getNestedText(questionElement, "answer", "text");
                String tolerance = XMLParser.getNestedText(questionElement, "answer", "tolerance");
                return new NumericalQuestion(type, name, text, grade, penalty, numAns, tolerance);

            case "shortanswer":
                boolean caseSensitive = "1".equals(XMLParser.getSimpleText(questionElement, "usecase"));
                String shortAns = "";
                NodeList saNodes = questionElement.getElementsByTagName("answer");
                for (int j = 0; j < saNodes.getLength(); j++) {
                    Element ansElem = (Element) saNodes.item(j);
                    if ("100".equals(ansElem.getAttribute("fraction"))) {
                        shortAns = XMLParser.getSimpleText(ansElem, "text");
                    }
                }
                return new ShortAnswerQuestion(type, name, text, grade, penalty, shortAns, caseSensitive);

            case "cloze":
                return new ClozeQuestion(type, name, text, grade, penalty);

            default:
                return new GenericQuestion(type, name, text, grade, penalty);
        }
    }
}