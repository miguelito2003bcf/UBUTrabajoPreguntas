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
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.util.I18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@DisplayName("Tests del Diálogo de Añadir Pregunta (AddQuestionDialog)")
public class AddQuestionDialogTest {

    @Start
    public void start(Stage stage) {
        // SOLUCIÓN: TestFX necesita que el Stage tenga una Scene (aunque sea una caja vacía) 
        // para no lanzar NullPointerException al inspeccionar las ventanas.
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("Debe cargar la interfaz dinámica y permitir cancelar con ESCAPE")
    public void testDialogInitializationAndCancel(FxRobot robot) {
        Category mockCategory = new Category("Física Cuántica");
        
        // CORRECCIÓN: Instanciar el componente de la UI dentro del hilo de JavaFX
        final AddQuestionDialog[] dialogWrapper = new AddQuestionDialog[1];
        robot.interact(() -> {
            dialogWrapper[0] = new AddQuestionDialog(mockCategory);
        });
        
        Platform.runLater(() -> dialogWrapper[0].showAndWait());
        
        robot.sleep(500);

        boolean labelExists = robot.lookup(I18n.get("addq.lbl.name")).tryQuery().isPresent();
        assertTrue(labelExists, "El diálogo debe haberse renderizado y mostrado en pantalla.");

        @SuppressWarnings("unchecked")
        ComboBox<String> typeCombo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertNotNull(typeCombo, "El selector de tipo de pregunta debe existir en la interfaz.");

        robot.type(KeyCode.ESCAPE);
        
        robot.sleep(500);
        
        boolean labelStillExists = robot.lookup(I18n.get("addq.lbl.name")).tryQuery().isPresent();
        assertFalse(labelStillExists, "El diálogo debe haberse cerrado completamente tras pulsar Escape.");
    }
}