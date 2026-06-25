/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de la lógica base de las Preguntas (Question)")
public class QuestionBaseTest {

    @Test
    @DisplayName("Debe transformar correctamente referencias @@PLUGINFILE@@ en URIs de datos Base64")
    public void testProcessPluginFiles() {
        // 1. Creamos una pregunta genérica para probar el método heredado de la clase abstracta Question
        Question q = new GenericQuestion("essay", "Pregunta con Imagen", "Mira esta imagen: <img src=\"@@PLUGINFILE@@/images/test.png\">", "1", "0");
        
        // 2. Simulamos un archivo adjunto codificado en Base64
        List<MoodleFile> files = new ArrayList<>();
        // El contenido Base64 simulado de una imagen
        MoodleFile mockImage = new MoodleFile("test.png", "/images/", "base64", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        files.add(mockImage);
        q.setFiles(files);

        // 3. Procesamos el HTML del enunciado
        String processedHtml = q.processPluginFiles(q.getText());

        // 4. Verificaciones
        assertFalse(processedHtml.contains("@@PLUGINFILE@@"), "No debería quedar ningún rastro de la etiqueta original de Moodle.");
        assertTrue(processedHtml.contains("data:image/png;base64,"), "Debe haber inyectado la cabecera correcta del MIME type para un PNG.");
        assertTrue(processedHtml.contains("iVBORw0K"), "El string original de la imagen Base64 debe estar embebido en el HTML.");
    }

    @Test
    @DisplayName("No debe alterar el HTML si no hay archivos adjuntos en la pregunta")
    public void testProcessPluginFilesSinArchivos() {
        String originalHtml = "Texto normal sin imágenes y una falsa referencia @@PLUGINFILE@@/nada.jpg";
        Question q = new GenericQuestion("essay", "Pregunta de Texto", originalHtml, "1", "0");
        
        // Nos aseguramos de que no tiene archivos
        q.setFiles(new ArrayList<>());

        String processedHtml = q.processPluginFiles(originalHtml);

        // Como no hay archivos adjuntos para sustituir, el texto debe quedar exactamente igual
        assertEquals(originalHtml, processedHtml, "Si la pregunta no tiene MoodleFiles, el HTML no debe ser modificado.");
    }
}