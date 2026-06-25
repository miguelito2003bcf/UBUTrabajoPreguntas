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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import moodleviewer.commands.CommandManager;
import moodleviewer.model.Category;
import moodleviewer.parser.GIFTParser.GiftImportResult;
import moodleviewer.parser.GIFTParser.GiftParseIssue;
import moodleviewer.util.CategoryMerger;
import moodleviewer.util.I18n;
import javafx.util.Pair;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import java.io.File;
import java.util.List;
import java.util.Locale;
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
     * Resultado de una operación de apertura unificada: la categoría raíz resultante y si la
     * operación sustituyó el banco anterior (false) o lo combinó con el existente (true).
     * Se distingue para que la interfaz sepa si debe tratar el resultado como "documento nuevo"
     * (limpiar historial de deshacer/rehacer, resetear el archivo de trabajo) o como una
     * modificación del documento actual (mantener historial, marcar como modificado).
     *
     * @param rootCategory categoría raíz resultante tras la operación.
     * @param file fichero físico que se acaba de abrir (el recién elegido, no el ya existente).
     * @param wasMerge true si el contenido se combinó con el banco que ya estaba cargado;
     *                 false si sustituyó por completo cualquier banco anterior.
     */
    public record OpenResult(Category rootCategory, File file, boolean wasMerge) {}

    /**
     * Inicializa el gestor de archivos vinculándolo a una instancia limpia de los servicios de Entrada/Salida.
     */
    public FileManager() {
        this.ioService = new FileIOService();
    }

    // =====================================================================
    //                 APERTURA UNIFICADA (XML + GIFT EN UNO)
    // =====================================================================

    /**
     * Punto de entrada único para abrir un banco de preguntas, sea XML o GIFT: el selector de
     * archivos acepta ambos formatos a la vez y el formato real se detecta por la extensión
     * del fichero elegido.
     *
     * Si no hay ningún banco cargado todavía ({@code currentRoot} es null o no tiene ni
     * preguntas ni subcategorías), el fichero se abre directamente sin preguntar nada.
     * Si ya hay un banco cargado con contenido, se pregunta al usuario si quiere SUSTITUIRLO
     * por el nuevo o COMBINAR ambos (el nuevo se añade como rama(s) nueva(s) bajo la raíz
     * del banco actual, ver {@link CategoryMerger}).
     *
     * @param stage ventana contenedora principal sobre la que se ancla el diálogo.
     * @param currentRoot categoría raíz actualmente cargada, o null si no hay ninguna.
     * @return el resultado de la operación, o vacío si el usuario cancela el selector de
     *         archivos o el diálogo de sustituir/combinar.
     */
    public Optional<OpenResult> openOrMergeBank(Stage stage, Category currentRoot) {
        File file = chooseBankFileToOpen(stage);
        if (file == null) {
            return Optional.empty();
        }

        boolean hasExistingBank = currentRoot != null
                && (!currentRoot.getQuestions().isEmpty() || !currentRoot.getSubcategories().isEmpty());

        if (!hasExistingBank) {
            return loadBankReplacing(stage, file);
        }

        Optional<Boolean> mergeChoice = askReplaceOrMerge(file);
        if (mergeChoice.isEmpty()) {
            return Optional.empty();
        }

        if (mergeChoice.get()) {
            return loadBankMerging(stage, file, currentRoot);
        } else {
            return loadBankReplacing(stage, file);
        }
    }

    private File chooseBankFileToOpen(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("file.title.openBank"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(I18n.get("file.ext.bank"), "*.xml", "*.txt"),
            new FileChooser.ExtensionFilter(I18n.get("file.ext.xml"), "*.xml"),
            new FileChooser.ExtensionFilter(I18n.get("file.ext.gift"), "*.txt")
        );
        return fileChooser.showOpenDialog(stage);
    }

    /**
     * Pregunta al usuario si el banco recién elegido debe sustituir al que ya está cargado
     * o combinarse con él.
     *
     * @return true si el usuario eligió combinar, false si eligió sustituir, vacío si canceló.
     */
    private Optional<Boolean> askReplaceOrMerge(File newFile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("file.dlg.replaceOrMerge.title"));
        alert.setHeaderText(I18n.get("file.dlg.replaceOrMerge.header", newFile.getName()));
        alert.setContentText(I18n.get("file.dlg.replaceOrMerge.content"));

        ButtonType btnMerge = new ButtonType(I18n.get("file.dlg.replaceOrMerge.btnMerge"), ButtonBar.ButtonData.YES);
        ButtonType btnReplace = new ButtonType(I18n.get("file.dlg.replaceOrMerge.btnReplace"), ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType(I18n.get("file.dlg.replaceOrMerge.btnCancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnMerge, btnReplace, btnCancel);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == btnCancel) {
            return Optional.empty();
        }
        return Optional.of(choice.get() == btnMerge);
    }

    /**
     * Carga el fichero detectando su formato por extensión, sustituyendo cualquier banco anterior.
     */
    private Optional<OpenResult> loadBankReplacing(Stage stage, File file) {
        try {
            Category rootCategory;
            if (isGiftFile(file)) {
                GiftImportResult result = ioService.loadBankFromGIFTWithReport(file);
                reportGiftIssuesIfAny(stage, result.issues());
                rootCategory = result.rootCategory();
            } else {
                rootCategory = ioService.loadBankFromXML(file);
            }
            return Optional.of(new OpenResult(rootCategory, file, false));
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.readBank", ex.getMessage()));
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Excepción controlada al importar un banco de preguntas", ex);
            return Optional.empty();
        }
    }

    /**
     * Carga el fichero detectando su formato por extensión y lo combina dentro del banco
     * actualmente cargado, marcando el documento como modificado (la fusión vive solo en
     * memoria hasta que el usuario guarde explícitamente).
     */
    private Optional<OpenResult> loadBankMerging(Stage stage, File file, Category currentRoot) {
        try {
            Category importedRoot;
            if (isGiftFile(file)) {
                GiftImportResult result = ioService.loadBankFromGIFTWithReport(file);
                reportGiftIssuesIfAny(stage, result.issues());
                importedRoot = result.rootCategory();
            } else {
                importedRoot = ioService.loadBankFromXML(file);
            }

            int added = CategoryMerger.merge(currentRoot, importedRoot);
            CommandManager.getInstance().markAsDirty();
            showSuccessNotification(stage, I18n.get("file.info.mergedBank", added));
            return Optional.of(new OpenResult(currentRoot, file, true));
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.readBank", ex.getMessage()));
            err.showAndWait();
            LOGGER.log(Level.SEVERE, "Excepción controlada al fusionar un banco de preguntas", ex);
            return Optional.empty();
        }
    }

    private boolean isGiftFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".txt");
    }

    /**
     * Si la importación GIFT ha encontrado bloques de pregunta que no pudo interpretar,
     * muestra un aviso detallando cuáles fueron y por qué, sin impedir que el resto de la
     * importación (que sí tuvo éxito) se utilice con normalidad.
     */
    private void reportGiftIssuesIfAny(Stage stage, List<GiftParseIssue> issues) {
        if (issues == null || issues.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18n.get("gift.issues.title"));
        alert.setHeaderText(I18n.get("gift.issues.header", issues.size()));

        StringBuilder details = new StringBuilder();
        for (GiftParseIssue issue : issues) {
            details.append(I18n.get("gift.issues.line", issue.startLine()))
                   .append(" — ").append(issue.reason()).append("\n")
                   .append("   \"").append(issue.blockPreview()).append("\"\n\n");
        }

        TextArea textArea = new TextArea(details.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(560, 260);

        VBox content = new VBox(8, new Label(I18n.get("gift.issues.info")), textArea);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefSize(600, 360);
        alert.showAndWait();
    }

    // =====================================================================
    //                          GUARDADO / EXPORTACIÓN
    // =====================================================================

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
            // Solo una exportación COMPLETA del banco actual representa "el documento está guardado":
            // una exportación parcial es una foto derivada, no el propio documento de trabajo.
            if (!isPartialExport) {
                CommandManager.getInstance().markAsSaved();
            }
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
        fileChooser.setTitle(I18n.get("file.title.exportGift"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18n.get("file.ext.gift"), "*.txt")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            ioService.exportToGIFT(categoryToExport, file);
            showSuccessNotification(stage, I18n.get("file.info.savedGift"));
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, I18n.get("file.err.saveGift", ex.getMessage()));
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