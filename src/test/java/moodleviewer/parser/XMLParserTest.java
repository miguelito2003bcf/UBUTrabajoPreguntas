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
import moodleviewer.model.MultichoiceQuestion;
import moodleviewer.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests del Parseador XML de Moodle (XMLParser)")
public class XMLParserTest {

    @Test
    @DisplayName("Debe parsear correctamente un XML básico con categorías y preguntas")
    public void testParseMoodleXML() throws Exception {
        // 1. Preparamos un XML de prueba simulando la exportación de Moodle
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<quiz>\n" +
                "  <question type=\"category\">\n" +
                "    <category>\n" +
                "      <text>$course$/top/Matemáticas</text>\n" +
                "    </category>\n" +
                "  </question>\n" +
                "  <question type=\"multichoice\">\n" +
                "    <name>\n" +
                "      <text>Pregunta de Suma</text>\n" +
                "    </name>\n" +
                "    <questiontext format=\"html\">\n" +
                "      <text><![CDATA[<p>¿Cuánto es 2+2?</p>]]></text>\n" +
                "    </questiontext>\n" +
                "    <defaultgrade>1.0000000</defaultgrade>\n" +
                "    <penalty>0.3333333</penalty>\n" +
                "    <single>true</single>\n" +
                "    <answer fraction=\"100\" format=\"html\">\n" +
                "      <text><![CDATA[<p>4</p>]]></text>\n" +
                "    </answer>\n" +
                "    <answer fraction=\"0\" format=\"html\">\n" +
                "      <text><![CDATA[<p>5</p>]]></text>\n" +
                "    </answer>\n" +
                "  </question>\n" +
                "</quiz>";

        // 2. Creamos un archivo temporal para que el parser lo lea
        File tempFile = Files.createTempFile("moodle_test", ".xml").toFile();
        Files.writeString(tempFile.toPath(), xmlContent);

        try {
            // 3. Ejecutamos el parser
            Category root = XMLParser.parseMoodleXML(tempFile);

            // 4. Verificamos la estructura general
            assertNotNull(root, "La categoría raíz no debe ser nula");
            assertEquals("Banco de Preguntas", root.getName());
            assertEquals(1, root.getSubcategories().size(), "Debe haberse creado la subcategoría 'Matemáticas'");

            Category matesCategory = root.getSubcategories().get(0);
            assertEquals("Matemáticas", matesCategory.getName());
            assertEquals(1, matesCategory.getQuestions().size(), "Debe haber 1 pregunta en la categoría 'Matemáticas'");

            // 5. Verificamos los atributos de la pregunta parseada
            Question q = matesCategory.getQuestions().get(0);
            assertTrue(q instanceof MultichoiceQuestion, "La pregunta debe ser de tipo Multichoice");
            assertEquals("multichoice", q.getType());
            assertEquals("Pregunta de Suma", q.getName());
            assertTrue(q.getText().contains("¿Cuánto es 2+2?"), "El enunciado HTML debe extraerse correctamente");
            assertEquals("1.0000000", q.getDefaultGrade());

            MultichoiceQuestion mcq = (MultichoiceQuestion) q;
            assertTrue(mcq.isSingleAnswer(), "Debe configurarse como respuesta única");
            assertEquals(2, mcq.getAnswers().size(), "Debe haber parseado 2 opciones de respuesta");
            assertEquals("100", mcq.getAnswers().get(0).getFraction(), "La primera opción debe tener 100% de puntuación");

        } finally {
            // Limpieza del archivo temporal
            tempFile.delete();
        }
    }
    
    @Test
    @DisplayName("Debe lanzar una excepción al intentar parsear un XML malformado")
    public void testParseMalformedXML() throws Exception {
        // XML inválido (etiquetas sin cerrar, estructura rota)
        String malformedXml = "<?xml version=\"1.0\"?><quiz><question type=\"essay\"><name>Falta cerrar tags";
        
        File tempFile = Files.createTempFile("moodle_malformed", ".xml").toFile();
        Files.writeString(tempFile.toPath(), malformedXml);

        try {
            // Verificamos que lance una excepción (SAXParseException típicamente, envuelta en Exception)
            assertThrows(Exception.class, () -> {
                XMLParser.parseMoodleXML(tempFile);
            }, "El parser debería lanzar una excepción al procesar un XML con sintaxis inválida.");
        } finally {
            tempFile.delete();
        }
    }

    @Test
    @DisplayName("Debe usar GenericQuestion como fallback para tipos de pregunta desconocidos")
    public void testParseUnknownQuestionType() throws Exception {
        // Simulamos un XML con un tipo de pregunta que Moodle invente en el futuro o un plugin de terceros
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<quiz>\n" +
                "  <question type=\"tipo_inventado_super_nuevo\">\n" +
                "    <name><text>Pregunta Rara</text></name>\n" +
                "    <questiontext format=\"html\"><text>¿Qué es esto?</text></questiontext>\n" +
                "  </question>\n" +
                "</quiz>";

        File tempFile = Files.createTempFile("moodle_unknown_type", ".xml").toFile();
        Files.writeString(tempFile.toPath(), xmlContent);

        try {
            Category root = XMLParser.parseMoodleXML(tempFile);
            
            // Verificamos que no ha colapsado y ha creado la pregunta
            assertEquals(1, root.getQuestions().size(), "Debería haber procesado la pregunta desconocida.");
            
            Question q = root.getQuestions().get(0);
            // El fallback en tu QuestionFactory.java es instanciar una GenericQuestion
            assertTrue(q instanceof moodleviewer.model.GenericQuestion, "Los tipos desconocidos deben mapearse a GenericQuestion (fallback).");
            assertEquals("tipo_inventado_super_nuevo", q.getType(), "Debe conservar el identificador del tipo original aunque use la clase genérica.");
            assertEquals("Pregunta Rara", q.getName());
            
        } finally {
            tempFile.delete();
        }
    }
}