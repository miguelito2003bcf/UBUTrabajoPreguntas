package moodleviewer;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

/**
 * Clase creada para construir y ensamblar la escena principal de la aplicación. Se encarga de organizar los componentes de la interfaz.
 * Se encarga de separar en dos paneles de manera horizontal, y la parte de la derecha en otros dos partes.
 */
public class LayoutManager {
    
    /**
     * Construye y devuelve la escena principal de la aplicación. Recupera todos los componentes de la instancia del main 
     * mediante sus getters y los organiza en la estructura de paneles.
     * * @param main instancia de main que proporciona todos los componentes de la interfaz.
     * @return escena principal.
     */
    public static Scene buildScene(Main main) {

        HBox questionTopBar = new HBox(10, main.getSearchQuestionField(), main.getTypeFilterMenu(), main.getAddQuestionButton());
        HBox categoryTopBar = new HBox(10, main.getSearchCategoryField(), main.getAddCategoryButton());
        HBox.setHgrow(main.getSearchCategoryField(), Priority.ALWAYS); 

        VBox leftPane = new VBox(5, categoryTopBar, main.getCategoryTreeView());
        VBox rightTopPane = new VBox(5, questionTopBar, main.getQuestionTableView());
        VBox rightBottomPane = new VBox(5, main.getClozeToggle(), main.getDetailsWebView());

        leftPane.setPadding(new Insets(5));
        rightTopPane.setPadding(new Insets(5));
        rightBottomPane.setPadding(new Insets(5));

        VBox.setVgrow(main.getCategoryTreeView(), Priority.ALWAYS);
        VBox.setVgrow(main.getQuestionTableView(), Priority.ALWAYS); 
        VBox.setVgrow(main.getDetailsWebView(), Priority.ALWAYS);

        SplitPane rightSplitPane = new SplitPane();
        rightSplitPane.setOrientation(Orientation.VERTICAL);
        rightSplitPane.getItems().addAll(rightTopPane, rightBottomPane);
        rightSplitPane.setDividerPositions(0.5f);

        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(leftPane, rightSplitPane);
        mainSplitPane.setDividerPositions(0.3f);
        
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(
                main.getOpenButton(), 
                spacer1, 
                main.getFileNameLabel(), 
                spacer2, 
                main.getSaveButton(), 
                main.getExportLatexButton()
        );

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(mainSplitPane);

        return new Scene(root, 1100, 750);
    }
}