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
import moodleviewer.model.Question;
import moodleviewer.parser.XMLExporter;
import moodleviewer.parser.XMLParser;
import moodleviewer.parser.LaTeXExporter;
import moodleviewer.parser.GIFTExportVisitor;
import moodleviewer.parser.GIFTParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Servicio encargado de gestionar de forma exclusiva la persistencia y exportación 
 * del banco de preguntas en disco. Esta clase carece de referencias a componentes visuales 
 * (JavaFX), lo que permite su ejecución asíncrona en segundo plano y su testeo unitario.
 */
public class FileIOService {

    /**
     * Carga y procesa un banco de preguntas desde un archivo XML de Moodle.
     * * @param file Archivo XML de origen.
     * @return Categoría raíz que contiene la estructura jerárquica de preguntas.
     * @throws Exception Si el archivo no es accesible o la estructura XML es inválida.
     */
    public Category loadBankFromXML(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo de origen no existe o es nulo.");
        }
        return XMLParser.parseMoodleXML(file);
    }

    /**
     * Serializa y guarda la jerarquía de categorías y preguntas en un archivo XML compatible con Moodle.
     * * @param rootCategory Categoría raíz del banco de preguntas.
     * @param file Archivo XML de destino.
     * @throws Exception Si ocurre un fallo en la manipulación del DOM o en la escritura en disco.
     */
    public void saveBankToXML(Category rootCategory, File file) throws Exception {
        if (rootCategory == null || file == null) {
            throw new IllegalArgumentException("La categoría raíz y el archivo de destino no pueden ser nulos.");
        }
        XMLExporter.exportMoodleXML(rootCategory, file);
    }

    /**
     * Exporta la jerarquía de preguntas actual a un documento estructurado en formato LaTeX (.tex).
     * * @param rootCategory Categoría raíz del banco.
     * @param file Archivo .tex de destino.
     * @param showAnswers Determina si se añaden las claves de respuestas correctas al documento.
     * @throws IOException Si ocurre un problema durante el flujo de escritura física del archivo.
     */
    public void exportToLaTeX(Category rootCategory, File file, boolean showAnswers) throws IOException {
        if (rootCategory == null || file == null) {
            throw new IllegalArgumentException("La categoría raíz y el archivo de destino no pueden ser nulos.");
        }
        LaTeXExporter.exportToLaTeX(rootCategory, file, showAnswers);
    }

    /**
     * Invoca el compilador del sistema para transformar el código LaTeX en un documento PDF interactivo.
     * * @param texFile Archivo fuente .tex generado previamente.
     * @throws IOException Si el ejecutable externo falla o no se encuentra el archivo PDF resultante.
     * @throws InterruptedException Si el subproceso del sistema operativo es interrumpido inesperadamente.
     */
    public void compilePDF(File texFile) throws IOException, InterruptedException {
        if (texFile == null || !texFile.exists()) {
            throw new IllegalArgumentException("El archivo LaTeX especificado no existe o es nulo.");
        }
        LaTeXExporter.compileAndOpenPDF(texFile);
    }

    // =====================================================================
    //                   NUEVA IMPLEMENTACIÓN FORMATO GIFT
    // =====================================================================

    /**
     * Carga y procesa un banco de preguntas desde un archivo de texto plano en formato GIFT.
     * * @param file Archivo GIFT de origen.
     * @return Categoría raíz que contiene la estructura jerárquica de preguntas.
     * @throws Exception Si el archivo no es accesible o la sintaxis GIFT es inválida.
     */
    public Category loadBankFromGIFT(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo GIFT de origen no existe o es nulo.");
        }
        return GIFTParser.parseGIFT(file);
    }

    /**
     * Exporta la jerarquía de preguntas actual a un archivo de texto en formato GIFT.
     * * @param rootCategory Categoría raíz del banco.
     * @param file Archivo .txt (GIFT) de destino.
     * @throws IOException Si ocurre un problema durante el flujo de escritura física del archivo.
     */
    public void exportToGIFT(Category rootCategory, File file) throws IOException {
        if (rootCategory == null || file == null) {
            throw new IllegalArgumentException("La categoría raíz y el archivo de destino no pueden ser nulos.");
        }
        
        // Escribimos con codificación UTF-8 para evitar problemas de caracteres (tildes, eñes) en Moodle.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            exportCategoryToGIFT(rootCategory, "$course$/top", writer);
        }
    }

    /**
     * Recorre el árbol de forma recursiva inyectando la macro $CATEGORY de GIFT y aplicando 
     * el Visitor a cada pregunta encontrada.
     * * @param category Categoría actual a exportar.
     * @param path Ruta acumulada dentro de Moodle.
     * @param writer Flujo de escritura del archivo final.
     * @throws IOException Si falla la escritura en disco.
     */
    private void exportCategoryToGIFT(Category category, String path, BufferedWriter writer) throws IOException {
        String newPath = path;
        
        if (!category.getName().equals("Banco de Preguntas")) {
            newPath = path + "/" + category.getName();
            // Escribe el cambio de directorio en la sintaxis de GIFT
            writer.write("$CATEGORY: " + newPath + "\n\n");
        }
        
        GIFTExportVisitor visitor = new GIFTExportVisitor(writer);
        for (Question q : category.getQuestions()) {
            q.accept(visitor);
        }
        
        // Llamada recursiva para todas las subcarpetas
        for (Category sub : category.getSubcategories()) {
            exportCategoryToGIFT(sub, newPath, writer);
        }
    }
}