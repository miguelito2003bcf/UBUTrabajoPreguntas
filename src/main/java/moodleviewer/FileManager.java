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
import java.util.Optional;

public class FileManager {

    public static Optional<Category> openXML(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                Category root = XMLParser.parseMoodleXML(file);
                return Optional.of(root);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al leer el archivo XML: " + ex.getMessage());
                alert.showAndWait();
            }
        }
        return Optional.empty();
    }

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
                ex.printStackTrace();
            }
        }
    }
    
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
                ex.printStackTrace();
            }
        }
    }
}