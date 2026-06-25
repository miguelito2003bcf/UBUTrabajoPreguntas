/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import moodleviewer.model.Category;
import moodleviewer.model.GenericQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de Integridad para el Servicio de Entrada/Salida (FileIOService)")
public class FileIOServiceTest {

    private FileIOService service;

    @BeforeEach
    public void setUp() {
        this.service = new FileIOService();
    }

    @Test
    @DisplayName("Debe lanzar una excepción controlada al intentar cargar descriptores nulos o inexistentes")
    public void testLoadBankFromXML_InvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.loadBankFromXML(null);
        }, "El servicio no debe aceptar descriptores de archivo nulos.");

        assertThrows(IllegalArgumentException.class, () -> {
            service.loadBankFromXML(new File("banco_inexistente_sistema_tfg.xml"));
        }, "El servicio debe validar la presencia física del archivo antes de procesar el parseo.");
    }

    @Test
    @DisplayName("Debe lanzar una excepción al intentar salvar estructuras o rutas nulas")
    public void testSaveBankToXML_InvalidInputs() throws IOException {
        File tempFile = Files.createTempFile("moodle_test_io", ".xml").toFile();
        tempFile.deleteOnExit();

        assertThrows(IllegalArgumentException.class, () -> {
            service.saveBankToXML(null, tempFile);
        }, "No se debe permitir la serialización de categorías raíz inexistentes.");

        assertThrows(IllegalArgumentException.class, () -> {
            service.saveBankToXML(new Category("Test"), null);
        }, "No se debe permitir la exportación a descriptores de destino nulos.");
    }

    @Test
    @DisplayName("Debe verificar el ciclo completo de exportación e importación XML sin pérdidas de información")
    public void testFullCycleXMLPersistence() throws Exception {
        // 1. Configuración de datos en memoria para el escenario controlado
        Category root = new Category("Banco de Preguntas");
        Category subCat = new Category("Sistemas Operativos");
        subCat.addQuestion(new GenericQuestion("essay", "Planificación de Procesos", "Explica el algoritmo Round Robin.", "2.0", "0.0"));
        root.addSubcategory(subCat);

        File tempFile = Files.createTempFile("moodle_full_cycle", ".xml").toFile();

        try {
            // 2. Ejecución de la escritura a través del servicio puro
            service.saveBankToXML(root, tempFile);
            assertTrue(tempFile.exists() && tempFile.length() > 0, "El archivo XML físico debe haberse generado con contenido estructurado.");

            // 3. Ejecución de la lectura inversa mediante el servicio puro
            Category loadedRoot = service.loadBankFromXML(tempFile);

            // 4. Verificación rigurosa de la integridad de los datos restituidos
            assertNotNull(loadedRoot);
            assertEquals(1, loadedRoot.getSubcategories().size());
            
            Category loadedSub = loadedRoot.getSubcategories().get(0);
            assertEquals("Sistemas Operativos", loadedSub.getName());
            assertEquals(1, loadedSub.getQuestions().size());
            assertEquals("Planificación de Procesos", loadedSub.getQuestions().get(0).getName());
            assertEquals("essay", loadedSub.getQuestions().get(0).getType());

        } finally {
            // Limpieza del entorno de pruebas para evitar residuos en disco
            tempFile.delete();
        }
    }

    @Test
    @DisplayName("Debe validar la consistencia estructural del archivo de exportación LaTeX generado por el servicio")
    public void testExportToLaTeX_StructureVerification() throws Exception {
        Category root = new Category("Banco de Preguntas");
        Category examenCat = new Category("Criptografía");
        examenCat.addQuestion(new GenericQuestion("essay", "Cifrado AES", "¿Cuál es el tamaño de bloque de AES?", "1.0", "0.0"));
        root.addSubcategory(examenCat);

        File tempTexFile = Files.createTempFile("moodle_latex_verify", ".tex").toFile();

        try {
            service.exportToLaTeX(root, tempTexFile, false);
            assertTrue(tempTexFile.exists() && tempTexFile.length() > 0, "El archivo .tex estructurado debe guardarse en el sistema de archivos.");

            String content = Files.readString(tempTexFile.toPath());
            
            // Verificaciones de las directivas básicas del preámbulo y contenido de LaTeX
            assertTrue(content.contains("\\documentclass"), "El documento exportado debe incluir la definición de clase de LaTeX.");
            assertTrue(content.contains("\\begin{document}"), "El documento debe delimitar correctamente el cuerpo del texto.");
            assertTrue(content.contains("Cifrado AES"), "El nombre de la pregunta debe integrarse sanitizado en el documento de salida.");
            assertTrue(content.contains("\\end{questions}"), "La estructura de preguntas del paquete exam debe cerrarse explícitamente.");

        } finally {
            tempTexFile.delete();
        }
    }

    @Test
    @DisplayName("Debe lanzar una excepción al intentar compilar archivos LaTeX nulos o no creados")
    public void testCompilePDF_InvalidFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.compilePDF(null);
        }, "El motor de compilación debe rechazar descriptores de archivos nulos.");

        assertThrows(IllegalArgumentException.class, () -> {
            service.compilePDF(new File("archivo_inexistente_tfg.tex"));
        }, "El motor de compilación debe abortar la ejecución si el archivo fuente no se encuentra en el almacenamiento local.");
    }
}