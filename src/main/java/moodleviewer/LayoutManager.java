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

public class LayoutManager {
    
    public static Scene buildScene(Main main) {

        HBox questionTopBar = new HBox(10, main.getSearchQuestionField(), main.getTypeFilterMenu(), main.getAddQuestionButton());
        HBox categoryTopBar = new HBox(10, main.getSearchCategoryField(), main.getAddCategoryButton());
        HBox.setHgrow(main.getSearchCategoryField(), Priority.ALWAYS); 

        VBox leftPane = new VBox(5, categoryTopBar, main.getCategoryTreeView());
        VBox rightTopPane = new VBox(5, questionTopBar, main.getQuestionTableView()); 
        VBox rightBottomPane = new VBox(5, main.getDetailsWebView());

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
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(main.getOpenButton(), spacer, main.getSaveButton(), main.getExportLatexButton());

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(mainSplitPane);

        return new Scene(root, 1100, 750);
    }
}