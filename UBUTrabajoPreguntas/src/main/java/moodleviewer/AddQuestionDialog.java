package moodleviewer;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import moodleviewer.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddQuestionDialog {

    private Category targetCategory;
    private ComboBox<String> typeComboBox;
    private TextField nameField;
    private TextArea textField;
    private TextField defaultGradeField;
    private TextField penaltyField;
    private TextArea feedbackField;
    private TextField idNumberField;
    private VBox specificSettingsContainer;
    private ComboBox<String> tfCorrectAnswerCombo;
    private TextArea tfTrueFeedback;
    private TextArea tfFalseFeedback;
    private ComboBox<String> mcSingleAnswerCombo;
    private CheckBox mcShuffleCheck;
    private List<MultichoiceOptionUI> mcOptionsList;
    private CheckBox matchShuffleCheck;
    private List<MatchingPairUI> matchingPairsList;
    private ComboBox<String> saCaseSensitiveCombo;
    private List<ShortAnswerOptionUI> saOptionsList;
    private List<NumericalOptionUI> numOptionsList;

    public AddQuestionDialog(Category targetCategory) {
        this.targetCategory = targetCategory;
    }

    public Optional<Question> showAndWait() {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Añadir Nueva Pregunta");
        dialog.setHeaderText("Categoría: " + targetCategory.getName());
        dialog.setResizable(true);

        ButtonType btnAceptar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);

        VBox mainForm = new VBox(20);
        mainForm.setPadding(new Insets(20));
        mainForm.setStyle("-fx-background-color: white;");

        GridPane commonGrid = createMoodleGrid();

        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "Verdadero / Falso", "Opción Múltiple", "Respuesta Corta", "Emparejamiento", "Numérica", "Anidada (Cloze)", "Ensayo (Genérica)"
        ));
        typeComboBox.setValue("Opción Múltiple"); 
        typeComboBox.setStyle("-fx-font-size: 14px;");
        
        nameField = new TextField();
        
        textField = new TextArea();
        textField.setPrefRowCount(6);
        textField.setWrapText(true);

        defaultGradeField = new TextField("1");
        penaltyField = new TextField("0.3333333");
        idNumberField = new TextField(); 
        
        feedbackField = new TextArea();
        feedbackField.setPrefRowCount(3);
        feedbackField.setWrapText(true);

        commonGrid.add(createMoodleLabel("Tipo de pregunta"), 0, 0);
        commonGrid.add(typeComboBox, 1, 0);
        commonGrid.add(createMoodleLabel("Nombre de la pregunta"), 0, 1);
        commonGrid.add(nameField, 1, 1);
        commonGrid.add(createMoodleLabel("Enunciado de la pregunta"), 0, 2);
        commonGrid.add(textField, 1, 2);
        commonGrid.add(createMoodleLabel("Puntuación por defecto"), 0, 3);
        commonGrid.add(defaultGradeField, 1, 3);
        commonGrid.add(createMoodleLabel("Retroalimentación general"), 0, 4);
        commonGrid.add(feedbackField, 1, 4);
        commonGrid.add(createMoodleLabel("Número de ID"), 0, 5);
        commonGrid.add(idNumberField, 1, 5);
        commonGrid.add(createMoodleLabel("Penalización por intento"), 0, 6);
        commonGrid.add(penaltyField, 1, 6);

        specificSettingsContainer = new VBox(20);

        mainForm.getChildren().addAll(commonGrid, new Separator(), specificSettingsContainer);

        ScrollPane scrollPane = new ScrollPane(mainForm);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(900, 600); 
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);

        typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            buildSpecificSettingsPanel(newVal);
        });

        buildSpecificSettingsPanel(typeComboBox.getValue());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAceptar) {
                String typeSelection = typeComboBox.getValue();
                String name = nameField.getText().isEmpty() ? "Nueva Pregunta" : nameField.getText();
                String text = textField.getText().isEmpty() ? "Sin enunciado" : textField.getText();
                String defaultGrade = defaultGradeField.getText().isEmpty() ? "1" : defaultGradeField.getText();
                String penalty = penaltyField.getText().isEmpty() ? "0.3333333" : penaltyField.getText();
                String genFeedback = feedbackField.getText();

                Question newQ = null;

                switch (typeSelection) {
                    case "Verdadero / Falso":
                        String isTrueCorrect = tfCorrectAnswerCombo.getValue().equals("Verdadero") ? "100" : "0";
                        String isFalseCorrect = isTrueCorrect.equals("100") ? "0" : "100";
                        Answer tAns = new Answer(isTrueCorrect, "Verdadero", tfTrueFeedback.getText());
                        Answer fAns = new Answer(isFalseCorrect, "Falso", tfFalseFeedback.getText());
                        newQ = new TrueFalseQuestion("truefalse", name, text, defaultGrade, penalty, tAns, fAns);
                        break;
                        
                    case "Opción Múltiple":
                        boolean isSingle = mcSingleAnswerCombo.getValue().contains("Sólo una");
                        boolean shuffle = mcShuffleCheck.isSelected();
                        List<Answer> mcAnswers = new ArrayList<>();
                        for (MultichoiceOptionUI optUI : mcOptionsList) {
                            String ansText = optUI.answerField.getText().trim();
                            if (!ansText.isEmpty()) { 
                                String fraction = optUI.gradeCombo.getValue().replace("%", "").trim();
                                if (fraction.equals("Ninguno")) fraction = "0";
                                mcAnswers.add(new Answer(fraction, ansText, optUI.feedbackField.getText()));
                            }
                        }
                        if (mcAnswers.isEmpty()) mcAnswers.add(new Answer("100", "Opción Correcta", ""));
                        newQ = new MultichoiceQuestion("multichoice", name, text, defaultGrade, penalty, isSingle, shuffle, mcAnswers);
                        break;
                        
                    case "Respuesta Corta":
                        boolean caseSensitive = saCaseSensitiveCombo.getValue().contains("deben coincidir");
                        List<Answer> saAnswers = new ArrayList<>();
                        for (ShortAnswerOptionUI optUI : saOptionsList) {
                            String ansText = optUI.answerField.getText().trim();
                            if (!ansText.isEmpty()) {
                                String fraction = optUI.gradeCombo.getValue().replace("%", "").trim();
                                if (fraction.equals("Ninguno")) fraction = "0";
                                saAnswers.add(new Answer(fraction, ansText, optUI.feedbackField.getText()));
                            }
                        }
                        if (saAnswers.isEmpty()) saAnswers.add(new Answer("100", "Respuesta esperada", ""));
                        newQ = new ShortAnswerQuestion("shortanswer", name, text, defaultGrade, penalty, caseSensitive, saAnswers);
                        break;

                    case "Emparejamiento":
                        List<MatchingPair> pairs = new ArrayList<>();
                        for (MatchingPairUI pairUI : matchingPairsList) {
                            String qText = pairUI.questionField.getText().trim();
                            String aText = pairUI.answerField.getText().trim();
                            if (!qText.isEmpty() || !aText.isEmpty()) pairs.add(new MatchingPair(qText, aText));
                        }
                        if (pairs.isEmpty()) pairs.add(new MatchingPair("Pregunta 1", "Respuesta 1"));
                        newQ = new MatchingQuestion("matching", name, text, defaultGrade, penalty, pairs);
                        break;

                    case "Numérica":
                        Answer numAns = new Answer("100", "0", "");
                        String tol = "0";
                        for (NumericalOptionUI opt : numOptionsList) {
                            String ansText = opt.answerField.getText().trim();
                            if (!ansText.isEmpty()) {
                                String fraction = opt.gradeCombo.getValue().replace("%", "").trim();
                                if (fraction.equals("Ninguno")) fraction = "0";
                                numAns = new Answer(fraction, ansText, opt.feedbackField.getText());
                                tol = opt.errorField.getText().trim();
                                if(tol.isEmpty()) tol = "0";
                                break; 
                            }
                        }
                        newQ = new NumericalQuestion("numerical", name, text, defaultGrade, penalty, numAns, tol);
                        break;

                    case "Anidada (Cloze)":
                        newQ = new ClozeQuestion("cloze", name, text, defaultGrade, penalty);
                        break;

                    case "Ensayo (Genérica)":
                    default:
                        newQ = new GenericQuestion("essay", name, text, defaultGrade, penalty);
                        break;
                }
                
                if (newQ != null) {
                    newQ.setGeneralFeedback(genFeedback);
                    try { newQ.getClass().getMethod("setIdnumber", String.class).invoke(newQ, idNumberField.getText()); } catch(Exception e){}
                }
                return newQ;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void buildSpecificSettingsPanel(String type) {
        specificSettingsContainer.getChildren().clear(); 

        if (type.equals("Verdadero / Falso")) {
            GridPane grid = createMoodleGrid();
            tfCorrectAnswerCombo = new ComboBox<>(FXCollections.observableArrayList("Falso", "Verdadero"));
            tfCorrectAnswerCombo.setValue("Falso"); 
            
            tfTrueFeedback = new TextArea(); tfTrueFeedback.setPrefRowCount(3);
            tfFalseFeedback = new TextArea(); tfFalseFeedback.setPrefRowCount(3);

            grid.add(createMoodleLabel("Respuesta correcta"), 0, 0);
            grid.add(tfCorrectAnswerCombo, 1, 0);
            grid.add(createMoodleLabel("Retroalimentación para la respuesta 'Verdadero'"), 0, 1);
            grid.add(tfTrueFeedback, 1, 1);
            grid.add(createMoodleLabel("Retroalimentación para la respuesta 'Falso'"), 0, 2);
            grid.add(tfFalseFeedback, 1, 2);
            specificSettingsContainer.getChildren().add(grid);

        } else if (type.equals("Opción Múltiple")) {
            mcSingleAnswerCombo = new ComboBox<>(FXCollections.observableArrayList("Sólo una respuesta", "Se permiten varias respuestas"));
            mcSingleAnswerCombo.setValue("Sólo una respuesta");
            mcShuffleCheck = new CheckBox("¿Barajar respuestas?");
            mcShuffleCheck.setSelected(true);
            
            GridPane topGrid = createMoodleGrid();
            topGrid.add(createMoodleLabel("¿Una o varias respuestas?"), 0, 0);
            topGrid.add(mcSingleAnswerCombo, 1, 0);
            topGrid.add(createMoodleLabel("¿Barajar respuestas?"), 0, 1);
            topGrid.add(mcShuffleCheck, 1, 1);
            
            Label titleRespuestas = new Label("Respuestas");
            titleRespuestas.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1177d1;");

            specificSettingsContainer.getChildren().addAll(topGrid, titleRespuestas, new Separator());

            mcOptionsList = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                MultichoiceOptionUI opt = new MultichoiceOptionUI(i);
                mcOptionsList.add(opt);
                specificSettingsContainer.getChildren().add(opt.getPanel());
            }

        } else if (type.equals("Emparejamiento")) {
            matchShuffleCheck = new CheckBox("Barajar");
            matchShuffleCheck.setSelected(true);

            GridPane topGrid = createMoodleGrid();
            topGrid.add(createMoodleLabel("Barajar"), 0, 0);
            topGrid.add(matchShuffleCheck, 1, 0);

            Label infoLabel = new Label("Debe proporcionar al menos dos preguntas y tres respuestas. Puede incluir respuestas erróneas extra (distractores) dando una respuesta con una pregunta en blanco.");
            infoLabel.setStyle("-fx-text-fill: #5f6368; -fx-font-size: 13px; -fx-wrap-text: true;");

            specificSettingsContainer.getChildren().addAll(topGrid, infoLabel, new Separator());

            matchingPairsList = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                MatchingPairUI pair = new MatchingPairUI(i);
                matchingPairsList.add(pair);
                specificSettingsContainer.getChildren().add(pair.getPanel());
            }

        } else if (type.equals("Respuesta Corta")) {
            saCaseSensitiveCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Igual mayúsculas que minúsculas", 
                "Mayúsculas y minúsculas deben coincidir"
            ));
            saCaseSensitiveCombo.setValue("Igual mayúsculas que minúsculas");

            GridPane topGrid = createMoodleGrid();
            topGrid.add(createMoodleLabel("Diferencia entre mayúsculas y minúsculas"), 0, 0);
            topGrid.add(saCaseSensitiveCombo, 1, 0);

            Label infoLabel = new Label("Debe proporcionar al menos una respuesta posible. Las respuestas en blanco no se utilizarán. Se usará '*' como comodín para cualquier carácter.");
            infoLabel.setStyle("-fx-text-fill: #5f6368; -fx-font-size: 13px; -fx-wrap-text: true;");
            
            Label titleRespuestas = new Label("Respuestas");
            titleRespuestas.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1177d1;");

            specificSettingsContainer.getChildren().addAll(topGrid, infoLabel, titleRespuestas, new Separator());

            saOptionsList = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                ShortAnswerOptionUI opt = new ShortAnswerOptionUI(i);
                saOptionsList.add(opt);
                specificSettingsContainer.getChildren().add(opt.getPanel());
            }

        } else if (type.equals("Numérica")) {
            Label titleRespuestas = new Label("Respuestas");
            titleRespuestas.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1177d1;");
            
            specificSettingsContainer.getChildren().addAll(titleRespuestas, new Separator());
            
            numOptionsList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                NumericalOptionUI opt = new NumericalOptionUI(i);
                numOptionsList.add(opt);
                specificSettingsContainer.getChildren().add(opt.getPanel());
            }
            
        } else {
            Label placeholder = new Label("Los ajustes estilo Moodle para este tipo de pregunta se añadirán próximamente.");
            placeholder.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            specificSettingsContainer.getChildren().add(placeholder);
        }
    }

    private GridPane createMoodleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30); 
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70); 
        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }

    private Label createMoodleLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
        return label;
    }

    private class MultichoiceOptionUI {
        VBox panel; TextArea answerField; ComboBox<String> gradeCombo; TextArea feedbackField;

        public MultichoiceOptionUI(int index) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
            
            Label title = new Label("Elección " + index);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            answerField = new TextArea();
            answerField.setPrefRowCount(3);
            
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Ninguno", "100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "-10%", "-25%", "-33.333%", "-50%", "-100%"
            ));
            gradeCombo.setValue(index == 1 ? "100%" : "Ninguno"); 

            feedbackField = new TextArea();
            feedbackField.setPrefRowCount(3);

            GridPane grid = createMoodleGrid();
            grid.add(createMoodleLabel("Elección " + index), 0, 0);
            grid.add(answerField, 1, 0);
            grid.add(createMoodleLabel("Calificación"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Retroalimentación"), 0, 2);
            grid.add(feedbackField, 1, 2);

            panel.getChildren().addAll(title, grid);
        }
        public VBox getPanel() { return panel; }
    }

    private class MatchingPairUI {
        VBox panel; TextArea questionField; TextField answerField;

        public MatchingPairUI(int index) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
            
            Label title = new Label("Conjunto " + index);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            questionField = new TextArea();
            questionField.setPrefRowCount(3);
            answerField = new TextField();

            GridPane grid = createMoodleGrid();
            grid.add(createMoodleLabel("Pregunta"), 0, 0);
            grid.add(questionField, 1, 0);
            grid.add(createMoodleLabel("Respuesta"), 0, 1);
            grid.add(answerField, 1, 1);

            panel.getChildren().addAll(title, grid);
        }
        public VBox getPanel() { return panel; }
    }

    private class ShortAnswerOptionUI {
        VBox panel; TextField answerField; ComboBox<String> gradeCombo; TextArea feedbackField;

        public ShortAnswerOptionUI(int index) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
            
            Label title = new Label("Respuesta " + index);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            answerField = new TextField();
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Ninguno", "100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "-10%", "-25%", "-33.333%", "-50%", "-100%"
            ));
            gradeCombo.setValue(index == 1 ? "100%" : "Ninguno"); 

            feedbackField = new TextArea();
            feedbackField.setPrefRowCount(3);

            GridPane grid = createMoodleGrid();
            grid.add(createMoodleLabel("Respuesta " + index), 0, 0);
            grid.add(answerField, 1, 0);
            grid.add(createMoodleLabel("Calificación"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Retroalimentación"), 0, 2);
            grid.add(feedbackField, 1, 2);

            panel.getChildren().addAll(title, grid);
        }
        public VBox getPanel() { return panel; }
    }

    private class NumericalOptionUI {
        VBox panel; TextField answerField; TextField errorField; ComboBox<String> gradeCombo; TextArea feedbackField;

        public NumericalOptionUI(int index) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
            
            Label title = new Label("Respuesta " + index);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            answerField = new TextField();
            errorField = new TextField("0");
            
            HBox ansErrorBox = new HBox(15, answerField, createMoodleLabel("Error"), errorField);
            
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Ninguno", "100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "-10%", "-25%", "-33.333%", "-50%", "-100%"
            ));
            gradeCombo.setValue(index == 1 ? "100%" : "Ninguno"); 

            feedbackField = new TextArea();
            feedbackField.setPrefRowCount(3);

            GridPane grid = createMoodleGrid();
            grid.add(createMoodleLabel("Respuesta " + index), 0, 0);
            grid.add(ansErrorBox, 1, 0); 
            grid.add(createMoodleLabel("Calificación"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Retroalimentación"), 0, 2);
            grid.add(feedbackField, 1, 2);

            panel.getChildren().addAll(title, grid);
        }
        public VBox getPanel() { return panel; }
    }
}