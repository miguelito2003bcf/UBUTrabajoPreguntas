/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer.parser;

import moodleviewer.model.*;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase visitante que exporta preguntas al formato GIFT conservando el HTML
 * e incrustando las imágenes nativas en formato Data URI (Base64).
 */
public class GIFTExportVisitor implements QuestionVisitor {
    
    private static final Logger LOGGER = Logger.getLogger(GIFTExportVisitor.class.getName());
    private final BufferedWriter writer;

    public GIFTExportVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public void visit(MultichoiceQuestion q) {
        try {
            writeTitleAndText(q);
            writer.write("{\n");
            for (Answer a : q.getAnswers()) {
                boolean isCorrect = a.getFraction() != null && Double.parseDouble(a.getFraction()) >= 100;
                String prefix = isCorrect ? "=" : "~";
                
                if (!isCorrect && a.getFraction() != null && Double.parseDouble(a.getFraction()) > 0) {
                    prefix = "~%" + a.getFraction() + "%";
                }
                
                // Procesamos las imágenes de la respuesta si las hubiera
                String processedAns = q.processPluginFiles(a.getText());
                writer.write("\t" + prefix + "[html]" + escapeGIFT(processedAns));
                
                if (a.getFeedback() != null && !a.getFeedback().isEmpty()) {
                    writer.write(" #" + "[html]" + escapeGIFT(q.processPluginFiles(a.getFeedback())));
                }
                writer.write("\n");
            }
            writer.write("}\n\n");
        } catch (IOException e) { logError(e); }
    }

    @Override
    public void visit(TrueFalseQuestion q) {
        try {
            writeTitleAndText(q);
            boolean isTrueCorrect = "100".equals(q.getTrueAnswer().getFraction());
            writer.write("{" + (isTrueCorrect ? "T" : "F") + "}\n\n");
        } catch (IOException e) { logError(e); }
    }

    @Override
    public void visit(ShortAnswerQuestion q) {
        try {
            writeTitleAndText(q);
            writer.write("{\n");
            for (Answer a : q.getAnswers()) {
                String processedAns = q.processPluginFiles(a.getText());
                writer.write("\t=[html]" + escapeGIFT(processedAns) + "\n");
            }
            writer.write("}\n\n");
        } catch (IOException e) { logError(e); }
    }

    @Override
    public void visit(NumericalQuestion q) {
        try {
            writeTitleAndText(q);
            String processedAns = q.processPluginFiles(q.getAnswer().getText());
            String tolerance = (q.getTolerance() != null && !q.getTolerance().isEmpty()) ? ":" + q.getTolerance() : "";
            writer.write("{#[html]" + escapeGIFT(processedAns) + tolerance + "}\n\n");
        } catch (IOException e) { logError(e); }
    }

    @Override
    public void visit(MatchingQuestion q) {
        try {
            writeTitleAndText(q);
            writer.write("{\n");
            for (MatchingPair p : q.getPairs()) {
                String processedQ = q.processPluginFiles(p.getQuestionText());
                String processedA = q.processPluginFiles(p.getAnswerText());
                writer.write("\t=[html]" + escapeGIFT(processedQ) + " -> [html]" + escapeGIFT(processedA) + "\n");
            }
            writer.write("}\n\n");
        } catch (IOException e) { logError(e); }
    }

    /**
     * Las preguntas Cloze incrustan su propia sintaxis de huecos directamente en el enunciado
     * (ej. "{1:SA:=Respuesta}"), que GIFT también soporta de forma nativa cuando aparece tal cual
     * en el cuerpo de la pregunta. Por eso, a diferencia del resto de tipos, aquí NO se debe pasar
     * el enunciado por {@link #escapeGIFT}, ya que escaparía las propias llaves de la sintaxis Cloze
     * (convirtiendo "{1:SA:=Respuesta}" en "\{1:SA:=Respuesta\}") y el hueco dejaría de funcionar
     * en Moodle, quedando como texto literal en lugar de como campo interactivo.
     */
    @Override
    public void visit(ClozeQuestion q) {
        try {
            writeTitleAndUnescapedText(q);
            writer.write("\n\n");
        } catch (IOException e) { logError(e); }
    }

    @Override
    public void visit(GenericQuestion q) {
        try {
            writeTitleAndText(q);
            writer.write("{}\n\n");
        } catch (IOException e) { logError(e); }
    }

    private void writeTitleAndText(Question q) throws IOException {
        String name = q.getName() != null ? q.getName() : "Pregunta sin titulo";
        // Convertimos las etiquetas @@PLUGINFILE@@ a Base64 real en el HTML antes de escribir
        String htmlContent = q.processPluginFiles(q.getText());
        writer.write("::" + escapeGIFT(name) + ":: [html]" + escapeGIFT(htmlContent) + "\n");
    }

    /**
     * Variante de {@link #writeTitleAndText} para preguntas Cloze: el título se escapa igual
     * que en el resto de tipos, pero el cuerpo se escribe SIN escapar para no destruir la
     * sintaxis Cloze nativa ('{', '}', '=', '~', '#', ':') incrustada en el enunciado.
     */
    private void writeTitleAndUnescapedText(Question q) throws IOException {
        String name = q.getName() != null ? q.getName() : "Pregunta sin titulo";
        String htmlContent = q.processPluginFiles(q.getText());
        writer.write("::" + escapeGIFT(name) + ":: [html]" + htmlContent + "\n");
    }

    /**
     * Función crítica de filtrado. En GIFT con formato [html], caracteres como
     * '=', '~', '#', '{', '}' y ':' rompen el parser si no llevan una barra invertida detras,
     * especialmente dentro de las cadenas gigantes de Base64 de las imágenes.
     */
    private String escapeGIFT(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\") // Escapar primero la barra de escape
                   .replace("~", "\\~")
                   .replace("=", "\\=")
                   .replace("#", "\\#")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace(":", "\\:");
    }

    private void logError(IOException e) {
        LOGGER.log(Level.SEVERE, "Error exportando pregunta con soporte multimedia a GIFT", e);
    }
}