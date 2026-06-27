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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import moodleviewer.commands.CommandManager;
import moodleviewer.model.*;
import moodleviewer.util.I18n;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador del diálogo de creación y edición de preguntas.
 *
 * PUNTO F: validación en tiempo real. El botón Aceptar del diálogo (obtenido
 * desde el DialogPane que lo contiene) se deshabilita automáticamente cuando:
 *   - El nombre de la pregunta está vacío.
 *   - En preguntas Multichoice, ninguna opción tiene fracción > 0.
 *   - En Emparejamiento, hay menos de 2 pares con texto en ambos campos.
 *   - En Respuesta Corta / Numérica, ninguna opción tiene fracción > 0.
 * La comprobación se recalcula en cada cambio relevante mediante listeners,
 * sin bloquear la UI (toda la lógica es O(n) sobre pocos elementos).
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
    @FXML private TextArea  textField;
    @FXML private TextField defaultGradeField;
    @FXML private TextField penaltyField;
    @FXML private TextArea  feedbackField;
    @FXML private VBox      specificSettingsContainer;

    // Referencia al botón Aceptar del DialogPane padre; se inyecta desde AddQuestionDialog
    // a través de injectSaveButton() justo después de cargar el FXML.
    private Button saveButton;

    private Category targetCategory;
    private Question questionToEdit;

    // Controles específicos dinámicos
    private ComboBox<String>          tfCorrectAnswerCombo;
    private TextArea                  tfTrueFeedback;
    private TextArea                  tfFalseFeedback;
    private ComboBox<String>          mcSingleAnswerCombo;
    private CheckBox                  mcShuffleCheck;
    private List<MultichoiceOptionUI> mcOptionsList;
    private CheckBox                  matchShuffleCheck;
    private List<MatchingPairUI>      matchingPairsList;
    private List<ShortAnswerOptionUI> saOptionsList;
    private List<NumericalOptionUI>   numOptionsList;

    // -------------------------------------------------------------------------
    //  Ciclo de vida FXML
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        safeSetLabel(lblType,            "addq.lbl.type",            "Tipo:");
        safeSetLabel(lblName,            "addq.lbl.name",            "Nombre:");
        safeSetLabel(lblText,            "addq.lbl.text",            "Enunciado:");
        safeSetLabel(lblGrade,           "addq.lbl.grade",           "Calificación por defecto:");
        safeSetLabel(lblPenalty,         "addq.lbl.penalty",         "Penalización:");
        safeSetLabel(lblGeneralFeedback, "addq.lbl.generalFeedback", "Retroalimentación general:");

        typeComboBox.setItems(FXCollections.observableArrayList(
                "essay", "truefalse", "multichoice", "shortanswer", "numerical", "matching", "cloze"));

        defaultGradeField.setText("1.0");
        penaltyField.setText("0.3333333");

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSpecificFields(newVal);
            revalidate();
        });

        // PUNTO F: el nombre vacío siempre invalida el formulario
        nameField.textProperty().addListener((obs, o, n) -> revalidate());
    }

    /**
     * PUNTO F: inyecta la referencia al botón Aceptar del DialogPane para poder
     * habilitarlo/deshabilitarlo en función del estado de validación.
     * Llamado desde {@link AddQuestionDialog} justo después de cargar el FXML.
     */
    public void injectSaveButton(Button btn) {
        this.saveButton = btn;
        revalidate();   // estado inicial correcto antes de que el usuario escriba nada
    }

    public void initData(Category targetCategory, Question questionToEdit) {
        this.targetCategory = targetCategory;
        this.questionToEdit = questionToEdit;

        if (this.questionToEdit != null) {
            loadQuestionData();
        } else {
            typeComboBox.setValue("essay");
        }
        // Estado inicial de validación una vez cargados los datos
        Platform.runLater(this::revalidate);
    }

    // -------------------------------------------------------------------------
    //  PUNTO F: motor de validación en tiempo real
    // -------------------------------------------------------------------------

    /**
     * Comprueba si el formulario actual es válido y habilita/deshabilita el botón Aceptar.
     * Se llama desde cada listener relevante (nombre, opciones de respuesta, pares...).
     */
    private void revalidate() {
        if (saveButton == null) return;

        boolean valid = isFormValid();
        saveButton.setDisable(!valid);

        // Feedback visual ligero: borde rojo en nameField si está vacío
        if (nameField.getText().isBlank()) {
            nameField.setStyle("-fx-border-color: #dc3545; -fx-border-radius: 3;");
        } else {
            nameField.setStyle("");
        }
    }

    /**
     * Reglas de validación por tipo de pregunta:
     * <ul>
     *   <li>Nombre no vacío → obligatorio para todos los tipos.</li>
     *   <li>Multichoice → al menos 1 opción con fracción > 0.</li>
     *   <li>Respuesta corta → al menos 1 respuesta con fracción > 0.</li>
     *   <li>Numérica → respuesta no vacía.</li>
     *   <li>Emparejamiento → al menos 2 pares completos (pregunta + respuesta).</li>
     *   <li>TrueFalse, Cloze, Essay → solo requieren nombre.</li>
     * </ul>
     */
    private boolean isFormValid() {
        if (nameField.getText().isBlank()) return false;

        String type = typeComboBox.getValue();
        if (type == null) return false;

        return switch (type) {
            case "multichoice" -> {
                if (mcOptionsList == null) yield false;
                yield mcOptionsList.stream().anyMatch(o -> {
                    try { return Double.parseDouble(o.getFraction()) > 0; }
                    catch (NumberFormatException e) { return false; }
                });
            }
            case "shortanswer" -> {
                if (saOptionsList == null) yield false;
                yield saOptionsList.stream().anyMatch(o -> {
                    if (o.getText().isBlank()) return false;
                    try { return Double.parseDouble(o.getFraction()) > 0; }
                    catch (NumberFormatException e) { return false; }
                });
            }
            case "numerical" -> {
                if (numOptionsList == null || numOptionsList.isEmpty()) yield false;
                yield !numOptionsList.get(0).getAnswer().isBlank();
            }
            case "matching" -> {
                if (matchingPairsList == null) yield false;
                long completePairs = matchingPairsList.stream()
                        .filter(p -> !p.getQuestionText().isBlank() && !p.getAnswerText().isBlank())
                        .count();
                yield completePairs >= 2;
            }
            // truefalse, cloze, essay: solo nombre
            default -> true;
        };
    }

    // -------------------------------------------------------------------------
    //  Carga de datos al editar
    // -------------------------------------------------------------------------

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
            tfCorrectAnswerCombo.setValue(
                    "100".equals(tf.getTrueAnswer().getFraction()) ? "Verdadero" : "Falso");
            tfTrueFeedback.setText(stripHtml(tf.getTrueAnswer().getFeedback()));
            tfFalseFeedback.setText(stripHtml(tf.getFalseAnswer().getFeedback()));
        } else if (questionToEdit instanceof MultichoiceQuestion mc) {
            mcSingleAnswerCombo.setValue(mc.isSingleAnswer()
                    ? "Una sola respuesta" : "Múltiples respuestas");
            mcShuffleCheck.setSelected(mc.isShuffleAnswers());
            for (int i = 0; i < mc.getAnswers().size() && i < mcOptionsList.size(); i++) {
                Answer a = mc.getAnswers().get(i);
                mcOptionsList.get(i).setText(stripHtml(a.getText()));
                mcOptionsList.get(i).setFraction(a.getFraction());
                mcOptionsList.get(i).setFeedback(stripHtml(a.getFeedback()));
            }
        } else if (questionToEdit instanceof ShortAnswerQuestion sa) {
            for (int i = 0; i < sa.getAnswers().size() && i < saOptionsList.size(); i++) {
                Answer a = sa.getAnswers().get(i);
                saOptionsList.get(i).setText(stripHtml(a.getText()));
                saOptionsList.get(i).setFraction(a.getFraction());
                saOptionsList.get(i).setFeedback(stripHtml(a.getFeedback()));
            }
        } else if (questionToEdit instanceof NumericalQuestion nq) {
            if (!numOptionsList.isEmpty()) {
                numOptionsList.get(0).setAnswer(stripHtml(nq.getAnswer().getText()));
                numOptionsList.get(0).setTolerance(nq.getTolerance());
                numOptionsList.get(0).setFraction(nq.getAnswer().getFraction());
                numOptionsList.get(0).setFeedback(stripHtml(nq.getAnswer().getFeedback()));
            }
        } else if (questionToEdit instanceof MatchingQuestion mq) {
            for (int i = 0; i < mq.getPairs().size() && i < matchingPairsList.size(); i++) {
                MatchingPair pair = mq.getPairs().get(i);
                matchingPairsList.get(i).setQuestionText(stripHtml(pair.getQuestionText()));
                matchingPairsList.get(i).setAnswerText(stripHtml(pair.getAnswerText()));
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Construcción de controles específicos por tipo
    // -------------------------------------------------------------------------

    private void updateSpecificFields(String type) {
        specificSettingsContainer.getChildren().clear();
        switch (type) {
            case "truefalse"   -> setupTrueFalseUI();
            case "multichoice" -> setupMultichoiceUI();
            case "shortanswer" -> setupShortAnswerUI();
            case "numerical"   -> setupNumericalUI();
            case "matching"    -> setupMatchingUI();
            case "cloze" -> {
                Label info = new Label(I18n.get("addq.cloze.info"));
                info.setStyle("-fx-font-style: italic; -fx-text-fill: #6c757d;");
                specificSettingsContainer.getChildren().add(info);
            }
        }
    }

    private void setupTrueFalseUI() {
        GridPane tfGrid = createMoodleGrid();
        tfCorrectAnswerCombo = new ComboBox<>(
                FXCollections.observableArrayList("Verdadero", "Falso"));
        tfCorrectAnswerCombo.setValue("Verdadero");
        tfTrueFeedback  = new TextArea(); tfTrueFeedback.setPrefRowCount(2);
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
        mcSingleAnswerCombo = new ComboBox<>(
                FXCollections.observableArrayList("Una sola respuesta", "Múltiples respuestas"));
        mcSingleAnswerCombo.setValue("Una sola respuesta");
        mcShuffleCheck = new CheckBox("Barajar respuestas");

        mcGrid.add(createMoodleLabel("Tipo de opción:"), 0, 0);
        mcGrid.add(mcSingleAnswerCombo, 1, 0);
        mcGrid.add(mcShuffleCheck, 1, 1);

        VBox optionsContainer = new VBox(10);
        mcOptionsList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MultichoiceOptionUI opt = new MultichoiceOptionUI(i);
            // PUNTO F: revalidar al cambiar cualquier fracción de opción
            opt.addFractionListener(this::revalidate);
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
            // PUNTO F: revalidar al cambiar texto o fracción
            opt.addChangeListener(this::revalidate);
            saOptionsList.add(opt);
            saContainer.getChildren().add(opt.getPanel());
        }
        specificSettingsContainer.getChildren().add(saContainer);
    }

    private void setupNumericalUI() {
        VBox numContainer = new VBox(10);
        numOptionsList = new ArrayList<>();
        NumericalOptionUI opt = new NumericalOptionUI(0);
        // PUNTO F: revalidar al cambiar el campo de respuesta numérica
        opt.addAnswerListener(this::revalidate);
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
            // PUNTO F: revalidar al cambiar cualquier par
            pair.addChangeListener(this::revalidate);
            matchingPairsList.add(pair);
            pairsContainer.getChildren().add(pair.getPanel());
        }
        specificSettingsContainer.getChildren().addAll(mGrid, pairsContainer);
    }

    // -------------------------------------------------------------------------
    //  Guardar
    // -------------------------------------------------------------------------

    public void saveQuestion() {
        String type           = typeComboBox.getValue();
        String name           = nameField.getText();
        String text           = textField.getText();
        String grade          = defaultGradeField.getText();
        String penalty        = penaltyField.getText();
        String generalFeedback = feedbackField.getText();

        Question newQuestion = switch (type) {
            case "truefalse" -> {
                boolean isTrueCorrect = "Verdadero".equals(tfCorrectAnswerCombo.getValue());
                Answer ta = new Answer(isTrueCorrect ? "100" : "0", "Verdadero", tfTrueFeedback.getText());
                Answer fa = new Answer(!isTrueCorrect ? "100" : "0", "Falso", tfFalseFeedback.getText());
                yield new TrueFalseQuestion(type, name, text, grade, penalty, ta, fa);
            }
            case "multichoice" -> {
                boolean single = "Una sola respuesta".equals(mcSingleAnswerCombo.getValue());
                List<Answer> answers = new ArrayList<>();
                for (MultichoiceOptionUI ui : mcOptionsList) {
                    if (!ui.getText().isEmpty())
                        answers.add(new Answer(ui.getFraction(), ui.getText(), ui.getFeedback()));
                }
                yield new MultichoiceQuestion(type, name, text, grade, penalty,
                        single, mcShuffleCheck.isSelected(), answers);
            }
            case "shortanswer" -> {
                List<Answer> answers = new ArrayList<>();
                for (ShortAnswerOptionUI ui : saOptionsList) {
                    if (!ui.getText().isEmpty())
                        answers.add(new Answer(ui.getFraction(), ui.getText(), ui.getFeedback()));
                }
                yield new ShortAnswerQuestion(type, name, text, grade, penalty, false, answers);
            }
            case "numerical" -> {
                NumericalOptionUI ui = numOptionsList.get(0);
                yield new NumericalQuestion(type, name, text, grade, penalty,
                        new Answer(ui.getFraction(), ui.getAnswer(), ui.getFeedback()),
                        ui.getTolerance());
            }
            case "matching" -> {
                List<MatchingPair> pairs = new ArrayList<>();
                for (MatchingPairUI ui : matchingPairsList) {
                    if (!ui.getQuestionText().isEmpty() && !ui.getAnswerText().isEmpty())
                        pairs.add(new MatchingPair(ui.getQuestionText(), ui.getAnswerText()));
                }
                yield new MatchingQuestion(type, name, text, grade, penalty, pairs);
            }
            case "cloze"   -> new ClozeQuestion(type, name, text, grade, penalty);
            default        -> new GenericQuestion(type, name, text, grade, penalty);
        };

        newQuestion.setGeneralFeedback(generalFeedback);
        CommandManager.getInstance().markAsDirty();

        if (questionToEdit != null) {
            int index = targetCategory.getQuestions().indexOf(questionToEdit);
            if (index != -1) targetCategory.getQuestions().set(index, newQuestion);
        } else {
            targetCategory.addQuestion(newQuestion);
        }
    }

    // -------------------------------------------------------------------------
    //  Utilidades
    // -------------------------------------------------------------------------

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            doc.select("br").append("\n");
            doc.select("p").prepend("\n\n");
            return doc.text().trim().replaceAll("\n{3,}", "\n\n");
        } catch (Exception e) { return html; }
    }

    private void safeSetLabel(Label label, String key, String fallback) {
        if (label == null) return;
        String val = I18n.get(key);
        label.setText((val.startsWith("!") && val.endsWith("!")) ? fallback : val);
    }

    private Label createMoodleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        return label;
    }

    private GridPane createMoodleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(150);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    // =========================================================================
    //  Subpaneles dinámicos internos
    // =========================================================================

    private abstract class AbstractOptionUI {
        protected VBox      panel;
        protected GridPane  grid;

        public AbstractOptionUI(String titleText) {
            panel = new VBox(10);
            panel.setStyle("-fx-background-color:#f1f3f5;-fx-padding:15;"
                    + "-fx-border-color:#dee2e6;-fx-border-radius:5;");
            Label title = new Label(titleText);
            title.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#343a40;");
            grid = createMoodleGrid();
            panel.getChildren().addAll(title, grid);
            buildSpecificContent();
        }
        public VBox getPanel() { return panel; }
        protected abstract void buildSpecificContent();
    }

    private class MultichoiceOptionUI extends AbstractOptionUI {
        private TextField       answerField;
        private ComboBox<String> gradeCombo;
        private TextArea        feedbackTextArea;

        public MultichoiceOptionUI(int index) { super("Opción " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            gradeCombo  = new ComboBox<>(FXCollections.observableArrayList(
                    "Ninguno","100%","90%","80%","50%","33.333%","25%","20%","10%",
                    "-10%","-25%","-33.333%","-50%","-100%"));
            gradeCombo.setValue("Ninguno");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Respuesta:"),   0, 0); grid.add(answerField,      1, 0);
            grid.add(createMoodleLabel("Calificación:"),0, 1); grid.add(gradeCombo,        1, 1);
            grid.add(createMoodleLabel("Feedback:"),    0, 2); grid.add(feedbackTextArea,  1, 2);
        }

        /** PUNTO F: permite al controlador padre suscribirse a cambios de fracción. */
        public void addFractionListener(Runnable onChanged) {
            gradeCombo.valueProperty().addListener((obs, o, n) -> onChanged.run());
        }

        public String getText()     { return answerField.getText(); }
        public String getFraction() {
            return "Ninguno".equals(gradeCombo.getValue()) ? "0"
                    : gradeCombo.getValue().replace("%", "");
        }
        public String getFeedback() { return feedbackTextArea.getText(); }
        public void setText(String t)     { answerField.setText(t); }
        public void setFraction(String f) {
            gradeCombo.setValue((f == null || f.equals("0")) ? "Ninguno" : f + "%");
        }
        public void setFeedback(String fb) { feedbackTextArea.setText(fb); }
    }

    private class ShortAnswerOptionUI extends AbstractOptionUI {
        private TextField        answerField;
        private ComboBox<String> gradeCombo;
        private TextArea         feedbackTextArea;

        public ShortAnswerOptionUI(int index) { super("Respuesta Aceptada " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            gradeCombo  = new ComboBox<>(FXCollections.observableArrayList(
                    "100%","90%","80%","50%","33.333%","25%","20%","10%","Ninguno"));
            gradeCombo.setValue("100%");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Respuesta:"),   0, 0); grid.add(answerField,     1, 0);
            grid.add(createMoodleLabel("Calificación:"),0, 1); grid.add(gradeCombo,       1, 1);
            grid.add(createMoodleLabel("Feedback:"),    0, 2); grid.add(feedbackTextArea, 1, 2);
        }

        /** PUNTO F: suscripción a cambios de texto y fracción. */
        public void addChangeListener(Runnable onChanged) {
            answerField.textProperty().addListener((obs, o, n) -> onChanged.run());
            gradeCombo.valueProperty().addListener((obs, o, n) -> onChanged.run());
        }

        public String getText()     { return answerField.getText(); }
        public String getFraction() {
            return "Ninguno".equals(gradeCombo.getValue()) ? "0"
                    : gradeCombo.getValue().replace("%", "");
        }
        public String getFeedback() { return feedbackTextArea.getText(); }
        public void setText(String t)     { answerField.setText(t); }
        public void setFraction(String f) {
            gradeCombo.setValue((f == null || f.equals("0")) ? "Ninguno" : f + "%");
        }
        public void setFeedback(String fb) { feedbackTextArea.setText(fb); }
    }

    private class NumericalOptionUI extends AbstractOptionUI {
        private TextField        answerField;
        private TextField        errorField;
        private ComboBox<String> gradeCombo;
        private TextArea         feedbackTextArea;

        public NumericalOptionUI(int index) { super("Respuesta Correcta Numérica"); }

        @Override
        protected void buildSpecificContent() {
            answerField = new TextField();
            errorField  = new TextField("0");
            HBox row    = new HBox(15, answerField, createMoodleLabel("Margen ±"), errorField);
            gradeCombo  = new ComboBox<>(FXCollections.observableArrayList(
                    "100%","90%","80%","50%","33.333%","25%","20%","10%","Ninguno"));
            gradeCombo.setValue("100%");
            feedbackTextArea = new TextArea(); feedbackTextArea.setPrefRowCount(2);

            grid.add(createMoodleLabel("Valor correcto:"), 0, 0); grid.add(row,             1, 0);
            grid.add(createMoodleLabel("Calificación:"),   0, 1); grid.add(gradeCombo,       1, 1);
            grid.add(createMoodleLabel("Feedback:"),       0, 2); grid.add(feedbackTextArea, 1, 2);
        }

        /** PUNTO F: suscripción al campo de respuesta. */
        public void addAnswerListener(Runnable onChanged) {
            answerField.textProperty().addListener((obs, o, n) -> onChanged.run());
        }

        public String getAnswer()    { return answerField.getText(); }
        public String getTolerance() { return errorField.getText(); }
        public String getFraction()  {
            return "Ninguno".equals(gradeCombo.getValue()) ? "0"
                    : gradeCombo.getValue().replace("%", "");
        }
        public String getFeedback()  { return feedbackTextArea.getText(); }
        public void setAnswer(String t)    { answerField.setText(t); }
        public void setTolerance(String t) { errorField.setText(t); }
        public void setFraction(String f)  {
            gradeCombo.setValue((f == null || f.equals("0")) ? "Ninguno" : f + "%");
        }
        public void setFeedback(String fb) { feedbackTextArea.setText(fb); }
    }

    private class MatchingPairUI extends AbstractOptionUI {
        private TextArea  questionField;
        private TextField answerField;

        public MatchingPairUI(int index) { super("Par de Emparejamiento " + (index + 1)); }

        @Override
        protected void buildSpecificContent() {
            questionField = new TextArea(); questionField.setPrefRowCount(2);
            answerField   = new TextField();

            grid.add(createMoodleLabel("Enunciado par:"), 0, 0); grid.add(questionField, 1, 0);
            grid.add(createMoodleLabel("Respuesta par:"), 0, 1); grid.add(answerField,   1, 1);
        }

        /** PUNTO F: suscripción a cambios en ambos campos. */
        public void addChangeListener(Runnable onChanged) {
            questionField.textProperty().addListener((obs, o, n) -> onChanged.run());
            answerField.textProperty().addListener((obs, o, n) -> onChanged.run());
        }

        public String getQuestionText() { return questionField.getText(); }
        public String getAnswerText()   { return answerField.getText(); }
        public void setQuestionText(String t) { questionField.setText(t); }
        public void setAnswerText(String t)   { answerField.setText(t); }
    }
}