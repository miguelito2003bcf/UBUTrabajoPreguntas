/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.parser.XMLExporter;
import moodleviewer.parser.XMLParser;
import java.io.File;
import javafx.util.Pair;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase creada para gestionar las operaciones de apertura, guardado y exportacion de ficheros.
 * Se centra en la lógica de diálogos de fichero y la gestión de errores.
 */
public class FileManager {
	
	private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());

	/**
	 * Abre un diálogo de selección de fichero para cargar un XML de Moodle.
	 * Parsea el fichero y devuelve un par con la categoría raíz y el archivo, o vacío si se cancela o falla.
	 * 
	 * @param stage ventana padre sobre la que se muestra el diálogo.
	 * @return un Pair con la categoría raíz y el archivo si la carga fue exitosa.
	 */
	public static Optional<Pair<Category, File>> openXML(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                Category root = XMLParser.parseMoodleXML(file);
                return Optional.of(new Pair<>(root, file));
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al leer el archivo XML: " + ex.getMessage());
                alert.showAndWait();
            }
        }
        return Optional.empty();
    }

    /**
     * Abre un diálogo de guardado para exportar el árbol como XML de Moodle.
     * 
     * @param stage ventana padre.
     * @param currentRootCategory categoría raíz del árbol a guardar.
     */
    public static void saveXML(Stage stage, Category currentRootCategory) {
        if (currentRootCategory == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar XML de Moodle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                XMLExporter.exportMoodleXML(currentRootCategory, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Archivo guardado con éxito.");
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar el archivo XML: " + ex.getMessage());
                alert.showAndWait();
                LOGGER.log(Level.SEVERE, "Excepción durante la exportación del archivo LaTeX", ex);
            }
        }
    }
    
    /**
     * Muestra un diálogo de opciones de exportación LaTeX y luego el diálogo de guardado. Pregunta
     * al usuario si desea el solucionario o el examen.
     * 
     * @param stage ventana padre.
     * @param currentRootCategory categoría raíz del árbol a exportar.
     */
    public static void exportLaTeX(Stage stage, Category currentRootCategory) {
        if (currentRootCategory == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Opciones de Exportación LaTeX");
        alert.setHeaderText("¿Cómo deseas exportar el documento?");
        alert.setContentText("Elige si quieres incluir las respuestas (para el profesor) o solo los enunciados (para los alumnos).");

        ButtonType btnTeacher = new ButtonType("Con respuestas (Solucionario)");
        ButtonType btnStudent = new ButtonType("Sin respuestas (Examen)");
        ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnTeacher, btnStudent, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() == btnCancel) {
            return;
        }
        
        boolean showAnswers = (result.get() == btnTeacher);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar a LaTeX");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo LaTeX", "*.tex"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                moodleviewer.parser.LaTeXExporter.exportToLaTeX(currentRootCategory, file, showAnswers);
                Alert info = new Alert(Alert.AlertType.INFORMATION, "Archivo LaTeX exportado con éxito.");
                info.setHeaderText(null);
                info.showAndWait();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Error al exportar a LaTeX: " + ex.getMessage());
                err.showAndWait();
                LOGGER.log(Level.SEVERE, "Excepción durante la exportación del archivo LaTeX", ex);
            }
        }
    }
}