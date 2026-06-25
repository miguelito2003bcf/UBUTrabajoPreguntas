/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.util.I18n;
import javafx.util.Pair;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de la interfaz de usuario para operaciones con archivos.
 * Coordina los diálogos visuales de selección de rutas e interactúa con el 
 * servicio de Entrada/Salida subyacente reflejando el estado de las operaciones mediante alertas.
 */
public class FileManager {

    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());
    private final FileIOService ioService;

    /**
     * Inicializa el gestor de archivos vinculándolo a una instancia limpia de los servicios de Entrada/Salida.
     */
    public FileManager() {
        this.ioService = new FileIOService();
    }

    /**
     * Muestra un selector de archivos para importar un banco XML y delega su lectura en el servicio.
     * * @param stage Ventana contenedora principal sobre la que se ancla el diálogo.
     * @return Un contenedor opcional con la estructura procesada y su archivo asociado en disco.
     */
    public Optional<Pair<Category, File>> openMoodleXML(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("file.ext.xml"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18n.get("file.ext.xml"), "*.xml")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return Optional.empty();
        }

        try {
            Category rootCategory = ioService.loadBankFromXML(file);
            return Optional.of(new Pair<>(rootCategory, file));
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.readXml", ex.getMessage()));
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Excepción controlada al importar el archivo XML", ex);
            return Optional.empty();
        }
    }

    /**
     * Almacena de manera persistente los cambios en el archivo XML actual o solicita una nueva ubicación de guardado.
     * Antes de guardar, pregunta al usuario si desea exportar el banco completo o solo una selección
     * de categorías concretas.
     * * @param stage Ventana contenedora principal.
     * @param rootCategory Jerarquía de categorías completa del banco actual.
     * @param preselectedCategory Categoría actualmente seleccionada en el árbol principal (puede ser null),
     * usada como punto de partida si el usuario elige exportar una selección.
     * @param currentFile Archivo de trabajo actual (puede ser nulo si no se ha guardado previamente).
     * @return El descriptor del archivo físico guardado, o vacío si se cancela el proceso.
     */
    public Optional<File> saveMoodleXML(Stage stage, Category rootCategory, Category preselectedCategory, File currentFile) {

        Optional<Category> scopeChoice = ExportScopeDialog.askExportScope(rootCategory, preselectedCategory);
        if (scopeChoice.isEmpty()) {
            return Optional.empty();
        }
        Category categoryToExport = scopeChoice.get();
        boolean isPartialExport = categoryToExport != rootCategory;

        // --- LINTING PRE-EXPORTACIÓN (XML -> false) ---
        if (!PreExportValidatorUI.checkAndConfirmExport(categoryToExport, false)) {
            return Optional.empty();
        }
        
        File file = isPartialExport ? null : currentFile;
        if (file == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(I18n.get("file.title.saveXml"));
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("file.ext.xml"), "*.xml")
            );
            file = fileChooser.showSaveDialog(stage);
        }

        if (file == null) {
            return Optional.empty();
        }

        try {
            ioService.saveBankToXML(categoryToExport, file);
            showSuccessNotification(stage, I18n.get("file.info.savedXml"));
            return isPartialExport ? Optional.empty() : Optional.of(file);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.saveXml", ex.getMessage()));
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Excepción controlada al exportar el archivo XML", ex);
            return Optional.empty();
        }
    }

    /**
     * Despliega las opciones de exportación tipográfica para compilar el banco a formato LaTeX y PDF de forma asíncrona.
     * * @param stage Ventana contenedora principal.
     * @param rootCategory Jerarquía de categorías completa del banco actual.
     * @param preselectedCategory Categoría actualmente seleccionada en el árbol principal.
     */
    public void exportLaTeX(Stage stage, Category rootCategory, Category preselectedCategory) {

        Optional<Category> scopeChoice = ExportScopeDialog.askExportScope(rootCategory, preselectedCategory);
        if (scopeChoice.isEmpty()) {
            return;
        }
        Category categoryToExport = scopeChoice.get();

        // --- LINTING PRE-EXPORTACIÓN (LaTeX -> false) ---
        if (!PreExportValidatorUI.checkAndConfirmExport(categoryToExport, false)) {
            return;
        }
        
        ButtonType btnTeacher = new ButtonType(I18n.get("file.btn.teacher"), ButtonBar.ButtonData.OK_DONE);
        ButtonType btnStudent = new ButtonType(I18n.get("file.btn.student"), ButtonBar.ButtonData.OTHER);
        ButtonType btnCancel = new ButtonType(I18n.get("file.btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert optionsDlg = new Alert(Alert.AlertType.CONFIRMATION);
        optionsDlg.setTitle(I18n.get("file.dlg.latex.title"));
        optionsDlg.setHeaderText(I18n.get("file.dlg.latex.header"));
        optionsDlg.setContentText(I18n.get("file.dlg.latex.content"));
        optionsDlg.getButtonTypes().setAll(btnTeacher, btnStudent, btnCancel);

        Optional<ButtonType> choice = optionsDlg.showAndWait();
        if (choice.isEmpty() || choice.get() == btnCancel) {
            return;
        }

        boolean showAnswers = (choice.get() == btnTeacher);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("file.title.exportLatex"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18n.get("file.ext.latex"), "*.tex")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            ioService.exportToLaTeX(categoryToExport, file, showAnswers);

            Alert pdfPrompt = new Alert(Alert.AlertType.CONFIRMATION);
            pdfPrompt.setTitle(I18n.get("file.dlg.pdf.title"));
            pdfPrompt.setHeaderText(I18n.get("file.dlg.pdf.header"));
            pdfPrompt.setContentText(I18n.get("file.dlg.pdf.content"));

            Optional<ButtonType> pdfChoice = pdfPrompt.showAndWait();
            if (pdfChoice.isPresent() && pdfChoice.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        ioService.compilePDF(file);
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.pdf") + "\n" + ex.getMessage());
                            err.showAndWait();
                        });
                        LOGGER.log(Level.SEVERE, "Fallo detectado en el motor de compilación externo de PDF", ex);
                    }
                }).start();
            } else {
                showSuccessNotification(stage, I18n.get("file.info.savedLatex"));
            }

        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.saveLatex", ex.getMessage()));
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Fallo estructural en el proceso de exportación LaTeX", ex);
        }
    }

    // =====================================================================
    //                   NUEVOS MÉTODOS PARA FORMATO GIFT
    // =====================================================================

    /**
     * Muestra un selector de archivos para importar un banco en formato GIFT (.txt).
     * * @param stage Ventana contenedora principal.
     * @return Un contenedor opcional con la estructura procesada y su archivo asociado en disco.
     */
    public Optional<Pair<Category, File>> openGIFT(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir archivo GIFT");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Formato Moodle GIFT (*.txt)", "*.txt")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return Optional.empty();
        }

        try {
            Category rootCategory = ioService.loadBankFromGIFT(file);
            return Optional.of(new Pair<>(rootCategory, file));
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Fallo al leer el archivo GIFT:\n" + ex.getMessage());
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Excepción controlada al importar el archivo GIFT", ex);
            return Optional.empty();
        }
    }

    /**
     * Inicia el proceso de exportación a formato de texto plano GIFT.
     * * @param stage Ventana contenedora principal.
     * @param rootCategory Jerarquía de categorías completa del banco actual.
     * @param preselectedCategory Categoría seleccionada como punto de partida si se elige exportación parcial.
     */
    public void exportGIFT(Stage stage, Category rootCategory, Category preselectedCategory) {
        
        Optional<Category> scopeChoice = ExportScopeDialog.askExportScope(rootCategory, preselectedCategory);
        if (scopeChoice.isEmpty()) {
            return;
        }
        Category categoryToExport = scopeChoice.get();

        // --- LINTING PRE-EXPORTACIÓN (GIFT -> true) ---
        // ¡Aquí es donde le pasamos "true" para que salte la alerta si hay imágenes!
        if (!PreExportValidatorUI.checkAndConfirmExport(categoryToExport, true)) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar como Moodle GIFT");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Formato Moodle GIFT (*.txt)", "*.txt")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            ioService.exportToGIFT(categoryToExport, file);
            showSuccessNotification(stage, "El banco de preguntas se ha exportado correctamente en formato GIFT.");
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Error al exportar a formato GIFT:\n" + ex.getMessage());
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Fallo estructural en el proceso de exportación GIFT", ex);
        }
    }

    // =====================================================================

    /**
     * Muestra una notificación emergente no bloqueante en la esquina de la pantalla para confirmar
     * el éxito de una operación.
     */
    private void showSuccessNotification(Stage stage, String message) {
        Notifications.create()
            .title(I18n.get("file.notif.title"))
            .text(message)
            .graphic(new FontIcon(FontAwesomeSolid.CHECK_CIRCLE))
            .hideAfter(javafx.util.Duration.seconds(4))
            .position(javafx.geometry.Pos.BOTTOM_RIGHT)
            .owner(stage)
            .showInformation();
    }
}