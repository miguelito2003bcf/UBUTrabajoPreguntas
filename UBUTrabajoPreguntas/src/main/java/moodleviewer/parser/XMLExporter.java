package moodleviewer.parser;

import moodleviewer.model.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class XMLExporter {

    public static void exportMoodleXML(Category rootCategory, File file) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<quiz>\n\n");
            exportCategoryRecursive(rootCategory, "$course$/top", writer);
            writer.write("</quiz>\n");
        }
    }

    private static void exportCategoryRecursive(Category category, String currentPath, BufferedWriter writer) throws Exception {
        String newPath = currentPath;
        if (!category.getName().equals("Banco de Preguntas")) {
            newPath = currentPath + "/" + escapeXML(category.getName());
            
            writer.write("  \n");
            writer.write("  <question type=\"category\">\n");
            writer.write("    <category>\n");
            writer.write("      <text>" + newPath + "</text>\n");
            writer.write("    </category>\n");
            writer.write("    <info format=\"html\">\n      <text><![CDATA[]]></text>\n    </info>\n");
            writer.write("  </question>\n\n");
        }

        for (Question q : category.getQuestions()) writeQuestion(q, writer);
        for (Category sub : category.getSubcategories()) exportCategoryRecursive(sub, newPath, writer);
    }

    private static void writeQuestion(Question q, BufferedWriter writer) throws Exception {
        writer.write("  <question type=\"" + q.getType() + "\">\n");
        
        writer.write("    <name>\n      <text><![CDATA[" + q.getName() + "]]></text>\n    </name>\n");
        writer.write("    <questiontext format=\"html\">\n");
        writer.write("      <text><![CDATA[" + q.getText() + "]]></text>\n");
        if (q.getFiles() != null && !q.getFiles().isEmpty()) {
            for (MoodleFile f : q.getFiles()) {
                writer.write("      <file name=\"" + escapeXML(f.name) + "\" path=\"" + escapeXML(f.path) + "\" encoding=\"" + escapeXML(f.encoding) + "\">" + f.content + "</file>\n");
            }
        }
        writer.write("    </questiontext>\n");
        writer.write("    <defaultgrade>" + escapeXML(q.getDefaultGrade()) + "</defaultgrade>\n");
        writer.write("    <penalty>" + escapeXML(q.getPenalty()) + "</penalty>\n");
        writer.write("    <hidden>0</hidden>\n");
        writer.write("    <idnumber></idnumber>\n");

        if (q.getGeneralFeedback() != null && !q.getGeneralFeedback().isEmpty()) {
            writer.write("    <generalfeedback format=\"html\">\n      <text><![CDATA[" + q.getGeneralFeedback() + "]]></text>\n    </generalfeedback>\n");
        }

        if (q instanceof TrueFalseQuestion) {
            TrueFalseQuestion tfq = (TrueFalseQuestion) q;
            writeAnswer(tfq.getTrueAnswer(), writer);
            writeAnswer(tfq.getFalseAnswer(), writer);
            
        } else if (q instanceof MultichoiceQuestion) {
            MultichoiceQuestion mcq = (MultichoiceQuestion) q;
            writer.write("    <single>" + (mcq.isSingleAnswer() ? "true" : "false") + "</single>\n");
            writer.write("    <shuffleanswers>" + (mcq.isShuffleAnswers() ? "true" : "false") + "</shuffleanswers>\n");
            writer.write("    <answernumbering>abc</answernumbering>\n");
            for (Answer a : mcq.getAnswers()) writeAnswer(a, writer);
            
        } else if (q instanceof ShortAnswerQuestion) {
            ShortAnswerQuestion saq = (ShortAnswerQuestion) q;
            writer.write("    <usecase>" + (saq.isCaseSensitive() ? "1" : "0") + "</usecase>\n");
            for (Answer a : saq.getAnswers()) writeAnswer(a, writer);
            
        } else if (q instanceof MatchingQuestion) {
            MatchingQuestion mq = (MatchingQuestion) q;
            writer.write("    <shuffleanswers>true</shuffleanswers>\n");
            for (MatchingPair p : mq.getPairs()) {
                writer.write("    <subquestion format=\"html\">\n");
                writer.write("      <text><![CDATA[" + p.getQuestionText() + "]]></text>\n");
                writer.write("      <answer>\n        <text><![CDATA[" + p.getAnswerText() + "]]></text>\n      </answer>\n");
                writer.write("    </subquestion>\n");
            }
            
        } else if (q instanceof NumericalQuestion) {
            NumericalQuestion nq = (NumericalQuestion) q;
            writer.write("    <answer fraction=\"" + nq.getAnswer().getFraction() + "\" format=\"moodle_auto_format\">\n");
            writer.write("      <text>" + escapeXML(nq.getAnswer().getText()) + "</text>\n");
            writer.write("      <feedback format=\"html\">\n        <text><![CDATA[" + nq.getAnswer().getFeedback() + "]]></text>\n      </feedback>\n");
            writer.write("      <tolerance>" + escapeXML(nq.getTolerance()) + "</tolerance>\n");
            writer.write("    </answer>\n");
        }

        writer.write("  </question>\n\n");
    }

    private static void writeAnswer(Answer a, BufferedWriter writer) throws Exception {
        writer.write("    <answer fraction=\"" + escapeXML(a.getFraction()) + "\" format=\"html\">\n");
        writer.write("      <text><![CDATA[" + a.getText() + "]]></text>\n");
        writer.write("      <feedback format=\"html\">\n        <text><![CDATA[" + a.getFeedback() + "]]></text>\n      </feedback>\n");
        writer.write("    </answer>\n");
    }

    private static String escapeXML(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}