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
                List<MatchingPair> pairs = new ArrayList<>();
                NodeList subqs = questionElement.getElementsByTagName("subquestion");
                for (int j = 0; j < subqs.getLength(); j++) {
                    Element sq = (Element) subqs.item(j);
                    String qT = XMLParser.getSimpleText(sq, "text");
                    String aT = XMLParser.getNestedText(sq, "answer", "text");
                    if (qT != null && aT != null) pairs.add(new MatchingPair(qT, aT));
                }
                return new MatchingQuestion(type, name, text, grade, penalty, pairs);

            case "multichoice":
                boolean single = "true".equals(XMLParser.getSimpleText(questionElement, "single"));
                boolean shuffleMulti = "true".equals(XMLParser.getSimpleText(questionElement, "shuffleanswers"));
                List<Answer> multiAnswers = new ArrayList<>();
                NodeList ansNodes = questionElement.getElementsByTagName("answer");
                for (int i = 0; i < ansNodes.getLength(); i++) {
                    Element ansEl = (Element) ansNodes.item(i);
                    String fraction = ansEl.getAttribute("fraction");
                    String aText = XMLParser.getSimpleText(ansEl, "text");
                    String feedback = XMLParser.getNestedText(ansEl, "feedback", "text");
                    multiAnswers.add(new Answer(fraction, aText != null ? aText : "", feedback != null ? feedback : ""));
                }
                return new MultichoiceQuestion(type, name, text, grade, penalty, single, shuffleMulti, multiAnswers);

            case "truefalse":
                Answer tAns = new Answer("100", "Verdadero", "");
                Answer fAns = new Answer("0", "Falso", "");
                NodeList tfAnswers = questionElement.getElementsByTagName("answer");
                for (int j = 0; j < tfAnswers.getLength(); j++) {
                    Element ansEl = (Element) tfAnswers.item(j);
                    String fraction = ansEl.getAttribute("fraction");
                    String ansFeedback = XMLParser.getNestedText(ansEl, "feedback", "text");
                    if ("100".equals(fraction)) {
                        tAns = new Answer("100", "Verdadero", ansFeedback != null ? ansFeedback : "");
                    } else {
                        fAns = new Answer("0", "Falso", ansFeedback != null ? ansFeedback : "");
                    }
                }
                return new TrueFalseQuestion(type, name, text, grade, penalty, tAns, fAns);

            case "numerical":
                Answer numAnswer = new Answer("100", "0", ""); 
                String tolerance = "0";
                NodeList numAnsNodes = questionElement.getElementsByTagName("answer");
                if (numAnsNodes.getLength() > 0) {
                    Element ansElem = (Element) numAnsNodes.item(0);
                    String fraction = ansElem.getAttribute("fraction");
                    String aText = XMLParser.getSimpleText(ansElem, "text");
                    String feedback = XMLParser.getNestedText(ansElem, "feedback", "text");
                    tolerance = XMLParser.getSimpleText(ansElem, "tolerance");
                    numAnswer = new Answer(fraction, aText != null ? aText : "", feedback != null ? feedback : "");
                }
                return new NumericalQuestion(type, name, text, grade, penalty, numAnswer, tolerance != null ? tolerance : "0");

            case "shortanswer":
                boolean caseSensitive = "1".equals(XMLParser.getSimpleText(questionElement, "usecase"));
                List<Answer> shortAnswers = new ArrayList<>();
                NodeList saNodes = questionElement.getElementsByTagName("answer");
                for (int j = 0; j < saNodes.getLength(); j++) {
                    Element ansElem = (Element) saNodes.item(j);
                    String fraction = ansElem.getAttribute("fraction");
                    String aText = XMLParser.getSimpleText(ansElem, "text");
                    String feedback = XMLParser.getNestedText(ansElem, "feedback", "text");
                    shortAnswers.add(new Answer(fraction, aText != null ? aText : "", feedback != null ? feedback : ""));
                }
                return new ShortAnswerQuestion(type, name, text, grade, penalty, caseSensitive, shortAnswers);

            case "cloze":
                return new ClozeQuestion(type, name, text, grade, penalty);

            default:
                return new GenericQuestion(type, name, text, grade, penalty);
        }
    }
}