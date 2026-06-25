/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.parser;

import moodleviewer.model.Category;
import moodleviewer.model.GenericQuestion;
import moodleviewer.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests del Exportador XML de Moodle (XMLExporter)")
public class XMLExporterTest {

    @Test
    @DisplayName("Debe exportar la jerarquía de categorías y preguntas a un archivo XML válido")
    public void testExportMoodleXML() throws Exception {
        // 1. Preparamos los datos en memoria
        Category root = new Category("Banco de Preguntas"); // Raíz requerida
        Category ciencias = new Category("Ciencias");
        
        Question q = new GenericQuestion("essay", "Explicación del Universo", "Explica la teoría del Big Bang.", "1", "0");
        q.setGeneralFeedback("El Big Bang es el modelo cosmológico predominante.");
        
        ciencias.addQuestion(q);
        root.addSubcategory(ciencias);

        // 2. Creamos un archivo temporal para la exportación
        File tempFile = Files.createTempFile("export_test", ".xml").toFile();

        try {
            // 3. Ejecutamos el exportador
            XMLExporter.exportMoodleXML(root, tempFile);

            // 4. Verificaciones
            assertTrue(tempFile.exists(), "El archivo XML debe haber sido creado en disco.");
            assertTrue(tempFile.length() > 0, "El archivo XML no debe estar vacío.");

            // 5. Leemos el contenido del archivo generado
            String xmlContent = Files.readString(tempFile.toPath());

            // 6. Comprobamos que las etiquetas y datos críticos están presentes en el XML
            assertTrue(xmlContent.contains("<quiz>"), "Debe contener la etiqueta raíz <quiz> de Moodle.");
            
            // Verificamos la categoría
            assertTrue(xmlContent.contains("<question type=\"category\">"), "Debe generar un nodo para la categoría.");
            assertTrue(xmlContent.contains("$course$/top/Ciencias"), "La ruta de la categoría debe estar bien formateada.");
            
            // Verificamos los atributos de la pregunta
            assertTrue(xmlContent.contains("<question type=\"essay\">"), "El tipo de pregunta debe ser 'essay'.");
            assertTrue(xmlContent.contains("Explicación del Universo"), "El nombre de la pregunta debe aparecer.");
            assertTrue(xmlContent.contains("Explica la teoría del Big Bang."), "El texto de la pregunta debe aparecer.");
            assertTrue(xmlContent.contains("El Big Bang es el modelo cosmológico predominante."), "La retroalimentación debe guardarse.");

        } finally {
            // Limpieza: borramos el archivo temporal para no dejar basura en el sistema
            tempFile.delete();
        }
    }
}