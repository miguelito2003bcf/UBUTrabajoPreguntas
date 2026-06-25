/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import moodleviewer.util.I18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@DisplayName("Tests de la Interfaz Gráfica (JavaFX) con TestFX")
public class MainUITest {

    private Main mainApp;

    /**
     * Este método es llamado por TestFX antes de cada test. 
     * Arranca la aplicación en un entorno de pruebas.
     */
    @Start
    public void start(Stage stage) throws Exception {
        mainApp = new Main();
        mainApp.start(stage);
    }

    @Test
    @DisplayName("Debe inicializar los botones con el estado correcto")
    public void testInitialButtonStates(FxRobot robot) {
        // Buscamos los botones en la pantalla por su ID CSS (los configuraste en Main.java)
        Button openBtn = robot.lookup("#btn-primary").queryAs(Button.class);
        Button addQuestionBtn = robot.lookup("#btn-success").queryAs(Button.class);

        // Verificamos que se han renderizado en pantalla
        assertNotNull(openBtn, "El botón de abrir XML debe existir en la interfaz.");
        assertNotNull(addQuestionBtn, "El botón de añadir pregunta debe existir en la interfaz.");

        // Verificamos sus estados iniciales lógicos
        assertFalse(openBtn.isDisabled(), "El botón de cargar XML debe estar habilitado al inicio.");
        assertTrue(addQuestionBtn.isDisabled(), "El botón de añadir pregunta debe estar deshabilitado hasta cargar un XML.");
        
        // Verificamos que la internacionalización se ha aplicado al texto del botón
        assertEquals(I18n.get("main.btn.loadXml"), openBtn.getText());
    }

    @Test
    @DisplayName("Debe permitir al usuario escribir en los campos de búsqueda")
    public void testSearchFieldsInteraction(FxRobot robot) {
        // Buscamos el campo de búsqueda de categorías usando su PromptText (Placeholder)
        TextField searchCategoryField = robot.lookup(".text-field").match(node -> {
            if (node instanceof TextField) {
                return I18n.get("main.search.category").equals(((TextField) node).getPromptText());
            }
            return false;
        }).queryAs(TextField.class);

        assertNotNull(searchCategoryField, "El buscador de categorías debe estar renderizado.");

        // Simulamos que el usuario hace click en el campo y escribe algo
        robot.clickOn(searchCategoryField).write("Matemáticas");

        // Verificamos que el texto de la interfaz se ha actualizado con la pulsación de teclas del robot
        assertEquals("Matemáticas", searchCategoryField.getText());
    }
}