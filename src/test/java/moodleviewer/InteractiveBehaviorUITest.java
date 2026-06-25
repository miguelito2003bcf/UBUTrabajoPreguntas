/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import moodleviewer.model.ClozeQuestion;
import moodleviewer.model.GenericQuestion;
import moodleviewer.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@DisplayName("Tests de Comportamiento Interactivo de la Interfaz (JavaFX)")
public class InteractiveBehaviorUITest {

    private Main mainApp;

    @Start
    public void start(Stage stage) throws Exception {
        // Arrancamos la aplicación
        mainApp = new Main();
        mainApp.start(stage);

        // INYECCIÓN DE DATOS: Metemos manualmente dos preguntas en la tabla para no depender de cargar un archivo XML
        Question clozeMock = new ClozeQuestion("cloze", "Pregunta de Huecos", "Enunciado...", "1", "0");
        Question essayMock = new GenericQuestion("essay", "Pregunta de Redacción", "Enunciado...", "1", "0");
        
        mainApp.getQuestionTableView().getItems().addAll(clozeMock, essayMock);
    }

    @Test
    @DisplayName("El botón 'Ver como el alumno' solo debe ser visible al seleccionar preguntas Cloze")
    public void testClozeToggleVisibility(FxRobot robot) {
        // Recuperamos el CheckBox a través del getter
        CheckBox clozeToggle = mainApp.getClozeToggle();
        
        // 1. Estado inicial
        assertFalse(clozeToggle.isVisible(), "Al iniciar, sin nada seleccionado, el botón debe estar oculto.");

        // 2. El robot hace clic en la fila de la pregunta Cloze usando su texto
        robot.clickOn("Pregunta de Huecos");
        
        // CORRECCIÓN: Pausa para permitir que la interfaz se actualice tras el clic
        robot.sleep(250); 
        
        // Verificamos que el listener ha reaccionado y lo ha hecho visible
        assertTrue(clozeToggle.isVisible(), "Al seleccionar una pregunta Cloze, el botón DEBE ser visible.");

        // 3. El robot hace clic en la pregunta de redacción normal
        robot.clickOn("Pregunta de Redacción");
        
        // CORRECCIÓN: Pausa de nuevo
        robot.sleep(250);
        
        // Verificamos que se vuelve a ocultar
        assertFalse(clozeToggle.isVisible(), "Al seleccionar una pregunta normal (Ensayo), el botón DEBE ocultarse.");
    }
}