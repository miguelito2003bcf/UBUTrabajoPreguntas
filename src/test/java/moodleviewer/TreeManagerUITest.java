/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
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
@DisplayName("Tests de lógica compleja: Gestor del Árbol (TreeManager)")
public class TreeManagerUITest {

    private Main mainApp;

    @Start
    public void start(Stage stage) throws Exception {
        mainApp = new Main();
        mainApp.start(stage);

        // Inyectamos una categoría anidada vacía
        Category root = new Category("Banco de Preguntas");
        Category subCat = new Category("Categoría Vieja");
        root.addSubcategory(subCat);
        
        mainApp.getCategoryTreeView().setRoot(TreeBuilder.createTreeItem(root));
    }

    @Test
    @DisplayName("Debe renombrar una categoría usando el menú contextual y un cuadro de diálogo")
    public void testRenameCategoryFromContextMenu(FxRobot robot) {
        TreeView<Category> tree = mainApp.getCategoryTreeView();

        // SOLUCIÓN: Buscamos el texto exacto generado por el CellFactory del TreeManager
        robot.rightClickOn("Categoría Vieja (0 | 0)");

        robot.clickOn(I18n.get("tree.ctx.rename"));

        robot.write("Categoría Nueva");
        
        robot.type(KeyCode.ENTER);

        Category modifiedCat = tree.getRoot().getChildren().get(0).getValue();
        assertEquals("Categoría Nueva", modifiedCat.getName(), "La categoría debería haber actualizado su nombre en el modelo de datos.");
    }
}