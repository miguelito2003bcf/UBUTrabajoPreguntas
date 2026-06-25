/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import moodleviewer.model.*;
import moodleviewer.util.I18n;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador encargado de gestionar el comportamiento, validación 
 * y enlace de datos del diálogo de creación y edición de preguntas.
 */
public class AddQuestionDialogController {

    @FXML private Label lblType;
    @FXML private Label lblName;
    @FXML private Label lblText;
    @FXML private Label lblGrade;
    @FXML private Label lblPenalty;
    @FXML private Label lblGeneralFeedback;

    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField nameField;
    @FXML private TextArea textField;
    @FXML private TextField defaultGradeField;
    @FXML private TextField penaltyField;
    @FXML private TextArea feedbackField;
    @FXML private VBox specificSettingsContainer;

    private Category targetCategory;
    private Question questionToEdit;

    // Controles específicos dinámicos
    private ComboBox<String> tfCorrectAnswerCombo;
    private TextArea tfTrueFeedback;
    private TextArea tfFalseFeedback;
    private ComboBox<String> mcSingleAnswerCombo;
    private CheckBox mcShuffleCheck;
    private List<MultichoiceOptionUI> mcOptionsList;
    private CheckBox matchShuffleCheck;
    private List<MatchingPairUI> matchingPairsList;
    private List<ShortAnswerOptionUI> saOptionsList;
    private List<NumericalOptionUI> numOptionsList;

    @FXML
    public void initialize() {
        // Internacionalización segura con valores de respaldo automáticos
        safeSetLabel(lblType, "addq.lbl.type", "Tipo:");
        safeSetLabel(lblName, "addq.lbl.name", "Nombre:");
        safeSetLabel(lblText, "addq.lbl.text", "Enunciado:");
        safeSetLabel(lblGrade, "addq.lbl.grade", "Calificación por defecto:");
        safeSetLabel(lblPenalty, "addq.lbl.penalty", "Penalización:");
        safeSetLabel(lblGeneralFeedback, "addq.lbl.generalFeedback", "Retroalimentación general:");

        typeComboBox.setItems(FXCollections.observableArrayList(
            "essay", "truefalse", "multichoice", "shortanswer", "numerical", "matching", "cloze"
        ));
        
        defaultGradeField.setText("1.0");
        penaltyField.setText("0.3333333");

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSpecificFields(newVal));
    }

    public void initData(Category targetCategory, Question questionToEdit) {
        this.targetCategory = targetCategory;
        this.questionToEdit = questionToEdit;

        if (this.questionToEdit != null) {
            loadQuestionData();
        } else {
            typeComboBox.setValue("essay");
        }
    }

    private void loadQuestionData() {
        typeComboBox.setValue(questionToEdit.getType());
        typeComboBox.setDisable(true); 

        nameField.setText(stripHtml(questionToEdit.getName()));
        textField.setText(stripHtml(questionToEdit.getText()));
        defaultGradeField.setText(questionToEdit.getDefaultGrade());
        penaltyField.setText(questionToEdit.getPenalty());
        
        if (questionToEdit.getGeneralFeedback() != null) {
            feedbackField.setText(stripHtml(questionToEdit.getGeneralFeedback()));
        }

        if (questionToEdit instanceof TrueFalseQuestion tf) {
            tfCorrectAnswerCombo.setValue("100".equals(tf.getTrueAnswer().getFraction()) ? "Verdadero" : "Falso");
            tfTrueFeedback.setText(stripHtml(tf.getTrueAnswer().getFeedback()));
            tfFalseFeedback.setText(stripHtml(tf.getFalseAnswer().getFeedback()));
        } 
        else if (questionToEdit instanceof MultichoiceQuestion mc) {
            mcSingleAnswerCombo.setValue(mc.isSingleAnswer() ? "Una sola respuesta" : "Múltiples respuestas");
            mcShuffleCheck.setSelected(mc.isShuffleAnswers());
            for (int i = 0; i < mc.getAnswers().size() && i < mcOptionsList.size(); i++) {
                Answer a = mc.getAnswers().get(i);
                mcOptionsList.get(i).setText(stripHtml(a.getText()));
                mcOptionsList.get(i).setFraction(a.getFraction());
                mcOptionsList.get(i).setFeedback(stripHtml(a.getFeedback()));
            }
        } 
        else if (questionToEdit instanceof ShortAnswerQuestion sa) {
            for (int i = 0; i < sa.getAnswers().size() && i < saOptionsList.size(); i++) {
                Answer a = sa.getAnswers().get(i);
                saOptionsList.get(i).setText(stripHtml(a.getText()));
                saOptionsList.get(i).setFraction(a.getFraction());
                saOptionsList.get(i).setFeedback(stripHtml(a.getFeedback()));
            }
        } 
        else if (questionToEdit instanceof NumericalQuestion nq) {
            if (!numOptionsList.isEmpty()) {
                numOptionsList.get(0).setAnswer(stripHtml(nq.getAnswer().getText()));
                numOptionsList.get(0).setTolerance(nq.getTolerance());
                numOptionsList.get(0).setFraction(nq.getAnswer().getFraction());
                numOptionsList.get(0).setFeedback(stripHtml(nq.getAnswer().getFeedback()));
            }
        } 
        else if (questionToEdit instanceof MatchingQuestion mq) {
            for (int i = 0; i < mq.getPairs().size() && i < matchingPairsList.size(); i++) {
                MatchingPair pair = mq.getPairs().get(i);
                matchingPairsList.get(i).setQuestionText(stripHtml(pair.getQuestionText()));
                matchingPairsList.get(i).setAnswerText(stripHtml(pair.getAnswerText()));
            }
        }
    }

    private void updateSpecificFields(String type) {
        specificSettingsContainer.getChildren().clear();
        switch (type) {
            case "truefalse" -> setupTrueFalseUI();
            case "multichoice" -> setupMultichoiceUI();
            case "shortanswer" -> setupShortAnswerUI();
            case "numerical" -> setupNumericalUI();
            case "matching" -> setupMatchingUI();
            case "cloze" -> {
                Label info = new Label("La sintaxis Cloze se extraerá del enunciado al guardar.");
                info.setStyle("-fx-font-style: italic; -fx-text-fill: #6c757d;");
                specificSettingsContainer.getChildren().add(info);
            }
        }
    }

    private void setupTrueFalseUI() {
        GridPane tfGrid = createMoodleGrid();
        tfCorrectAnswerCombo = new ComboBox<>(FXCollections.observableArrayList("Verdadero", "Falso"));
        tfCorrectAnswerCombo.setValue("Verdadero");
        tfTrueFeedback = new TextArea(); tfTrueFeedback.setPrefRowCount(2);
        tfFalseFeedback = new TextArea(); tfFalseFeedback.setPrefRowCount(2);

        tfGrid.add(createMoodleLabel("Respuesta Correcta:"), 0, 0);
        tfGrid.add(tfCorrectAnswerCombo, 1, 0);
        tfGrid.add(createMoodleLabel("Feedback Verdadero:"), 0, 1);
        tfGrid.add(tfTrueFeedback, 1, 1);
        tfGrid.add(createMoodleLabel("Feedback Falso:"), 0, 2);
        tfGrid.add(tfFalseFeedback, 1, 2);
        specificSettingsContainer.getChildren().add(tfGrid);
    }

    private void setupMultichoiceUI() {
        GridPane mcGrid = createMoodleGrid();
        mcSingleAnswerCombo = new ComboBox<>(FXCollections.observableArrayList("Una sola respuesta", "Múltiples respuestas"));
        mcSingleAnswerCombo.setValue("Una sola respuesta");
        mcShuffleCheck = new CheckBox("Barajar respuestas");

        mcGrid.add(createMoodleLabel("Tipo de opción:"), 0, 0);
        mcGrid.add(mcSingleAnswerCombo, 1, 0);
        mcGrid.add(mcShuffleCheck, 1, 1);

        VBox optionsContainer = new VBox(10);
        mcOptionsList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MultichoiceOptionUI opt = new MultichoiceOptionUI(i);
            mcOptionsList.add(opt);
            optionsContainer.getChildren().add(opt.getPanel());
        }
        specificSettingsContainer.getChildren().addAll(mcGrid, optionsContainer);
    }

    private void setupShortAnswerUI() {
        VBox saContainer = new VBox(10);
        saOptionsList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ShortAnswerOptionUI opt = new ShortAnswerOptionUI(i);
            saOptionsList.add(opt);
            saContainer.getChildren().add(opt.getPanel());
        }
        specificSettingsContainer.getChildren().add(saContainer);
    }

    private void setupNumericalUI() {
        VBox numContainer = new VBox(10);
        numOptionsList = new ArrayList<>();
        NumericalOptionUI opt = new NumericalOptionUI(0);
        numOptionsList.add(opt);
        numContainer.getChildren().add(opt.getPanel());
        specificSettingsContainer.getChildren().add(numContainer);
    }

    private void setupMatchingUI() {
        GridPane mGrid = createMoodleGrid();
        matchShuffleCheck = new CheckBox("Barajar respuestas");
        mGrid.add(matchShuffleCheck, 1, 0);

        VBox pairsContainer = new VBox(10);
        matchingPairsList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MatchingPairUI pair = new MatchingPairUI(i);
            matchingPairsList.add(pair);
            pairsContainer.getChildren().add(pair.getPanel());
        }
        specificSettingsContainer.getChildren().addAll(mGrid, pairsContainer);
    }

    public void saveQuestion() {
        String type = typeComboBox.getValue();
        String name = nameField.getText();
        String text = textField.getText();
        String grade = defaultGradeField.getText();
        String penalty = penaltyField.getText();
        String generalFeedback = feedbackField.getText();

        Question newQuestion = switch (type) {
            case "truefalse" -> {
                boolean isTrueCorrect = "Verdadero".equals(tfCorrectAnswerCombo.getValue());
                Answer trueAns = new Answer(isTrueCorrect ? "100" : "0", "Verdadero", tfTrueFeedback.getText());
                Answer falseAns = new Answer(!isTrueCorrect ? "100" : "0", "Falso", tfFalseFeedback.getText());
                yield new TrueFalseQuestion(type, name, text, grade, penalty, trueAns, falseAns);
            }
            case "multichoice" -> {
                boolean single = "Una sola respuesta".equals(mcSingleAnswerCombo.getValue());
                List<Answer> answers = new ArrayList<>();
                for (MultichoiceOptionUI ui : mcOptionsList) {
                    if (!ui.getText().isEmpty()) answers.add(new Answer(ui.getFraction(), ui.getText(), ui.getFeedback()));
                }
                yield new MultichoiceQuestion(type, name, text, grade, penalty, single, mcShuffleCheck.isSelected(), answers);
            }
            case "shortanswer" -> {
                List<Answer> answers = new ArrayList<>();
                for (ShortAnswerOptionUI ui : saOptionsList) {
                    if (!ui.getText().isEmpty()) answers.add(new Answer(ui.getFraction(), ui.getText(), ui.getFeedback()));
                }
                yield new ShortAnswerQuestion(type, name, text, grade, penalty, false, answers);
            }
            case "numerical" -> {
                NumericalOptionUI ui = numOptionsList.get(0);
                Answer numAnswer = new Answer(ui.getFraction(), ui.getAnswer(), ui.getFeedback());
                yield new NumericalQuestion(type, name, text, grade, penalty, numAnswer, ui.getTolerance());
            }
            case "matching" -> {
                List<MatchingPair> pairs = new ArrayList<>();
                for (MatchingPairUI ui : matchingPairsList) {
                    if (!ui.getQuestionText().isEmpty() && !ui.getAnswerText().isEmpty()) {
                        pairs.add(new MatchingPair(ui.getQuestionText(), ui.getAnswerText()));
                    }
                }
                yield new MatchingQuestion(type, name, text, grade, penalty, pairs);
            }
            case "cloze" -> new ClozeQuestion(type, name, text, grade, penalty);
            default -> new GenericQuestion(type, name, text, grade, penalty);
        };

        newQuestion.setGeneralFeedback(generalFeedback);

        if (questionToEdit != null) {
            int index = targetCategory.getQuestions().indexOf(questionToEdit);
            if (index != -1) {
                targetCategory.getQuestions().set(index, newQuestion);
            }
        } else {
            targetCategory.addQuestion(newQuestion);
        }
    }

    private void safeSetLabel(Label label, String key, String fallback) {
        if (label != null) {
            String val = I18n.get(key);
            label.setText((val.startsWith("!") && val.endsWith("!")) ? fallback : val);
        }
    }

    /**
     * Utilizza JSoup per rimuovere i tag HTML ma conservare l'impaginazione strutturale.
     */
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            
            // Sostituisce i tag di salto con veri ritorni a capo
            doc.select("br").append("\n");
            doc.select("p").prepend("\n\n");
            
            String plainText = doc.text().trim();
            
            // Normalizza spaziature eccessive
            return plainText.replaceAll("\n{3,}", "\n\n");
        } catch (Exception e) {
            return html;
        }
    }

    private Label createMoodleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        return label;
    }

    private GridPane createMoodleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }

    /* Subpaneles dinámicos internos */
    private abstract class AbstractOptionUI {
        protected VBox panel;
        protected GridPane grid;

        public AbstractOptionUI(String titleText) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
            Label title = new Label(titleText);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #343a40;");
            grid = createMoodleGrid();
            panel.getChildren().addAll(title, grid);
            buildSpecificContent();
        }
        public VBox getPanel() { return panel; }
        protected abstract void buildSpecificContent();
    }

    private class MultichoiceOptionUI extends AbstractOptionUI {
        private TextField answerField;
        private ComboBox<String> gradeCombo;
        private TextArea feedbackTextArea;

        public MultichoiceOptionUI(int index) { super("Opción " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Ninguno", "100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "-10%", "-25%", "-33.333%", "-50%", "-100%"
            ));
            gradeCombo.setValue("Ninguno");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Respuesta:"), 0, 0);
            grid.add(answerField, 1, 0);
            grid.add(createMoodleLabel("Calificación:"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Feedback:"), 0, 2);
            grid.add(feedbackTextArea, 1, 2);
        }
        public String getText() { return answerField.getText(); }
        public String getFraction() { return "Ninguno".equals(gradeCombo.getValue()) ? "0" : gradeCombo.getValue().replace("%", ""); }
        public String getFeedback() { return feedbackTextArea.getText(); }
        public void setText(String text) { answerField.setText(text); }
        public void setFraction(String fraction) { gradeCombo.setValue((fraction == null || fraction.equals("0")) ? "Ninguno" : fraction + "%"); }
        public void setFeedback(String feedback) { feedbackTextArea.setText(feedback); }
    }

    private class ShortAnswerOptionUI extends AbstractOptionUI {
        private TextField answerField;
        private ComboBox<String> gradeCombo;
        private TextArea feedbackTextArea;

        public ShortAnswerOptionUI(int index) { super("Respuesta Aceptada " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList("100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "Ninguno"));
            gradeCombo.setValue("100%");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Respuesta:"), 0, 0);
            grid.add(answerField, 1, 0);
            grid.add(createMoodleLabel("Calificación:"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Feedback:"), 0, 2);
            grid.add(feedbackTextArea, 1, 2);
        }
        public String getText() { return answerField.getText(); }
        public String getFraction() { return "Ninguno".equals(gradeCombo.getValue()) ? "0" : gradeCombo.getValue().replace("%", ""); }
        public String getFeedback() { return feedbackTextArea.getText(); }
        public void setText(String text) { answerField.setText(text); }
        public void setFraction(String fraction) { gradeCombo.setValue((fraction == null || fraction.equals("0")) ? "Ninguno" : fraction + "%"); }
        public void setFeedback(String feedback) { feedbackTextArea.setText(feedback); }
    }

    private class NumericalOptionUI extends AbstractOptionUI {
        private TextField answerField;
        private TextField errorField;
        private ComboBox<String> gradeCombo;
        private TextArea feedbackTextArea;

        public NumericalOptionUI(int index) { super("Respuesta Correcta Numérica"); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            errorField = new TextField("0");
            HBox ansErrorBox = new HBox(15, answerField, createMoodleLabel("Margen ±"), errorField);
            gradeCombo = new ComboBox<>(FXCollections.observableArrayList("100%", "90%", "80%", "50%", "33.333%", "25%", "20%", "10%", "Ninguno"));
            gradeCombo.setValue("100%");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Valor correcto:"), 0, 0);
            grid.add(ansErrorBox, 1, 0);
            grid.add(createMoodleLabel("Calificación:"), 0, 1);
            grid.add(gradeCombo, 1, 1);
            grid.add(createMoodleLabel("Feedback:"), 0, 2);
            grid.add(feedbackTextArea, 1, 2);
        }
        public String getAnswer() { return answerField.getText(); }
        public String getTolerance() { return errorField.getText(); }
        public String getFraction() { return "Ninguno".equals(gradeCombo.getValue()) ? "0" : gradeCombo.getValue().replace("%", ""); }
        public String getFeedback() { return feedbackTextArea.getText(); }
        public void setAnswer(String text) { answerField.setText(text); }
        public void setTolerance(String tol) { errorField.setText(tol); }
        public void setFraction(String fraction) { gradeCombo.setValue((fraction == null || fraction.equals("0")) ? "Ninguno" : fraction + "%"); }
        public void setFeedback(String feedback) { feedbackTextArea.setText(feedback); }
    }

    private class MatchingPairUI extends AbstractOptionUI {
        private TextArea questionField;
        private TextField answerField;

        public MatchingPairUI(int index) { super("Par de Emparejamiento " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            questionField = new TextArea(); questionField.setPrefRowCount(2);
            answerField = new TextField();

            grid.add(createMoodleLabel("Enunciado par:"), 0, 0);
            grid.add(questionField, 1, 0);
            grid.add(createMoodleLabel("Respuesta par:"), 0, 1);
            grid.add(answerField, 1, 1);
        }
        public String getQuestionText() { return questionField.getText(); }
        public String getAnswerText() { return answerField.getText(); }
        public void setQuestionText(String text) { questionField.setText(text); }
        public void setAnswerText(String text) { answerField.setText(text); }
    }
}