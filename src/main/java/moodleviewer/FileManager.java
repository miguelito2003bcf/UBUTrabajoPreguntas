package moodleviewer;

import javafx.scene.control.Alert;
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
}