/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.parser;

import moodleviewer.model.Answer;
import moodleviewer.model.MultichoiceQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tests del Visitante de Exportación LaTeX")
public class LaTeXExportVisitorTest {

    @Test
    @DisplayName("Debe generar la sintaxis LaTeX correcta para una pregunta Multichoice con respuestas visibles")
    public void testMultichoiceExportWithAnswers() throws Exception {
        // 1. Preparamos un "falso" escritor en memoria (StringWriter) en lugar de un archivo real
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        
        // 2. Instanciamos el Visitor en modo profesor (showAnswers = true)
        LaTeXExportVisitor visitor = new LaTeXExportVisitor(bufferedWriter, new File("."), "imagenes_test", true);

        // 3. Creamos una pregunta de prueba de una sola respuesta correcta
        List<Answer> answers = Arrays.asList(
            new Answer("100", "Opción Correcta", ""),
            new Answer("0", "Opción Incorrecta", "")
        );
        MultichoiceQuestion mq = new MultichoiceQuestion("multichoice", "Capitales", "¿Capital de Francia?", "1", "0", true, false, answers);

        // 4. Hacemos que la pregunta acepte al visitante para que la procese
        mq.accept(visitor);
        bufferedWriter.flush();

        // 5. Capturamos el texto generado y verificamos
        String output = stringWriter.toString();
        
        // Comprobamos la estructura básica de LaTeX
        assertTrue(output.contains("\\begin{unaRespuesta}"), "Debe usar el entorno 'unaRespuesta' para preguntas de respuesta única.");
        assertTrue(output.contains("\\CorrectChoice"), "Debe marcar la opción correcta con \\CorrectChoice.");
        assertTrue(output.contains("Opción Correcta"), "El texto de la respuesta correcta debe estar presente.");
        assertTrue(output.contains("\\choice"), "Debe marcar las opciones incorrectas con \\choice.");
        assertTrue(output.contains("Opción Incorrecta"), "El texto de la respuesta incorrecta debe estar presente.");
        assertTrue(output.contains("\\end{unaRespuesta}"), "Debe cerrar el entorno LaTeX.");
    }
}