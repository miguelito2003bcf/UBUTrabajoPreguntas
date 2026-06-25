/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.util.I18n;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Envoltura para el diálogo estructurado mediante FXML.
 */
public class AddQuestionDialog {

    private final Dialog<ButtonType> dialog;
    private AddQuestionDialogController controller;

    public AddQuestionDialog(Category targetCategory) {
        this(targetCategory, null);
    }

    public AddQuestionDialog(Category targetCategory, Question questionToEdit) {
        this.dialog = new Dialog<>();
        
        // Estilos globales de la aplicación
        if (getClass().getResource("/styles.css") != null) {
            this.dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        
        if (questionToEdit == null) {
            this.dialog.setTitle(I18n.get("addq.title"));
            this.dialog.setHeaderText(I18n.get("addq.header", targetCategory.getName()));
        } else {
            this.dialog.setTitle(I18n.get("editq.title"));
            this.dialog.setHeaderText(I18n.get("editq.header", questionToEdit.getName()));
        }

        // Vinculación con el botón de aceptación nativo del proyecto
        ButtonType saveButtonType = new ButtonType(I18n.get("main.dlg.btnAccept"), ButtonBar.ButtonData.OK_DONE);
        this.dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        try {
            URL fxmlLocation = getClass().getResource("/fxml/AddQuestionDialog.fxml");
            if (fxmlLocation == null) {
                throw new IOException("Descriptor FXML ausente. Asegúrate de que el archivo se encuentra exactamente en: src/main/resources/fxml/AddQuestionDialog.fxml");
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            ScrollPane root = loader.load();
            this.dialog.getDialogPane().setContent(root);
            
            this.controller = loader.getController();
            this.controller.initData(targetCategory, questionToEdit);
            
        } catch (IOException e) {
            System.err.println("Fallo al inicializar el componente FXML: " + e.getMessage());
            e.printStackTrace();
        }
        
        this.dialog.setResizable(true);
    }

    public void showAndWait() {
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (controller != null) {
                controller.saveQuestion();
            }
        }
    }
}