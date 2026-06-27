/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.util.I18n;
import moodleviewer.util.QuestionValidator;

import java.util.*;

/**
 * Diálogo de estadísticas del banco de preguntas.
 *
 * PUNTO 13: el cálculo de métricas se ejecuta en un {@link Task} de JavaFX para no
 *           bloquear el hilo de UI con bancos grandes.
 * PUNTO 23: colores fijos por tipo de pregunta en el gráfico de tarta, usando una
 *           paleta predefinida que no varía entre ejecuciones ni entre distintos bancos.
 */
public class DashboardDialog {

    // -------------------------------------------------------------------------
    //  Paleta de colores fija por tipo de pregunta (punto 23)
    // -------------------------------------------------------------------------
    private static final Map<String, String> TYPE_COLORS = new LinkedHashMap<>();
    static {
        TYPE_COLORS.put("multichoice",  "#0f6cbf");
        TYPE_COLORS.put("truefalse",    "#28a745");
        TYPE_COLORS.put("shortanswer",  "#fd7e14");
        TYPE_COLORS.put("numerical",    "#6f42c1");
        TYPE_COLORS.put("matching",     "#17a2b8");
        TYPE_COLORS.put("cloze",        "#e83e8c");
        TYPE_COLORS.put("essay",        "#6c757d");
        TYPE_COLORS.put("description",  "#adb5bd");
        // cualquier tipo desconocido recibirá un color de la paleta de fallback abajo
    }
    private static final List<String> FALLBACK_COLORS = List.of(
            "#c0392b","#8e44ad","#2980b9","#27ae60","#f39c12","#16a085");

    // -------------------------------------------------------------------------
    //  Estado de cálculo
    // -------------------------------------------------------------------------

    private final Category rootCategory;
    private final Dialog<ButtonType> dialog;

    private int totalCategories    = 0;
    private int totalQuestions     = 0;
    private int questionsWithImages = 0;
    private final Map<String, Integer> typeCounts            = new LinkedHashMap<>();
    private final Map<Category, Integer> directQuestionCounts = new LinkedHashMap<>();

    public DashboardDialog(Category rootCategory) {
        this.rootCategory = rootCategory;
        this.dialog       = new Dialog<>();

        if (getClass().getResource("/styles.css") != null) {
            dialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/styles.css").toExternalForm());
        }
        dialog.setTitle(I18n.get("stats.dialog.title"));
        dialog.setHeaderText(I18n.get("stats.dialog.header"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResultConverter(b -> b);
        dialog.setResizable(true);

        // PUNTO 13: mostrar spinner mientras se calculan las métricas en background
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(60, 60);
        VBox loading = new VBox(spinner, new Label(I18n.get("stats.loading")));
        loading.setAlignment(Pos.CENTER);
        loading.setSpacing(12);
        loading.setPrefSize(820, 560);
        dialog.getDialogPane().setContent(loading);

        Task<Void> calcTask = new Task<>() {
            @Override
            protected Void call() {
                calculateStats(rootCategory);
                return null;
            }
        };
        calcTask.setOnSucceeded(e -> {
            int pendingWarnings = QuestionValidator.validateBank(rootCategory).size();
            dialog.getDialogPane().setContent(buildContent(pendingWarnings));
        });
        calcTask.setOnFailed(e -> {
            dialog.getDialogPane().setContent(
                    new Label(I18n.get("stats.error")));
        });
        new Thread(calcTask, "dashboard-calc").start();
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

    // -------------------------------------------------------------------------
    //  Construcción del contenido (solo en el hilo JavaFX, tras el Task)
    // -------------------------------------------------------------------------

    private VBox buildContent(int pendingWarnings) {
        VBox main = new VBox(20);
        main.setPadding(new Insets(20));
        main.setPrefSize(820, 560);

        // KPIs
        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER);
        int displayCats = Math.max(0, totalCategories - 1);
        kpiRow.getChildren().addAll(
                createKPICard(I18n.get("stats.lbl.totalCategories"),    String.valueOf(displayCats),       "#0f6cbf"),
                createKPICard(I18n.get("stats.lbl.totalQuestions"),     String.valueOf(totalQuestions),    "#0f6cbf"),
                createKPICard(I18n.get("stats.lbl.questionsWithImages"),String.valueOf(questionsWithImages),"#6c757d"),
                createKPICard(I18n.get("stats.lbl.pendingWarnings"),    String.valueOf(pendingWarnings),
                        pendingWarnings > 0 ? "#c0392b" : "#28a745")
        );

        // Gráfico + top categorías
        HBox centerRow = new HBox(30);
        centerRow.setAlignment(Pos.CENTER);
        centerRow.setPrefHeight(380);
        centerRow.getChildren().addAll(buildTypeChart(), buildTopCategoriesBlock());
        HBox.setHgrow(centerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(centerRow.getChildren().get(1), Priority.ALWAYS);

        main.getChildren().addAll(kpiRow, centerRow);
        return main;
    }

    // -------------------------------------------------------------------------
    //  PUNTO 23: gráfico de tarta con colores fijos por tipo
    // -------------------------------------------------------------------------

    private PieChart buildTypeChart() {
        PieChart chart = new PieChart();
        chart.setTitle(I18n.get("stats.chart.title"));
        chart.setLegendSide(Side.BOTTOM);

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        int fallbackIdx = 0;

        for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
            String type  = e.getKey();
            int    count = e.getValue();
            double pct   = totalQuestions > 0 ? (double) count / totalQuestions * 100 : 0;
            String label = String.format("%s (%d | %.1f%%)", type.toUpperCase(), count, pct);
            data.add(new PieChart.Data(label, count));
        }
        chart.setData(data);

        // Aplicar colores después de que JavaFX haya creado los nodos del gráfico
        chart.setAnimated(false);  // sin animación para que los nodos existan inmediatamente
        int fi = 0;
        for (PieChart.Data slice : chart.getData()) {
            // Extraer el tipo del label (formato "TIPO (n | x%)")
            String rawType  = slice.getName().split(" ")[0].toLowerCase();
            String color    = TYPE_COLORS.getOrDefault(rawType,
                    FALLBACK_COLORS.get(fi % FALLBACK_COLORS.size()));
            if (!TYPE_COLORS.containsKey(rawType)) fi++;
            slice.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
        return chart;
    }

    // -------------------------------------------------------------------------
    //  Top 5 categorías
    // -------------------------------------------------------------------------

    private VBox buildTopCategoriesBlock() {
        VBox block = new VBox(10);
        block.setAlignment(Pos.TOP_LEFT);
        block.setPrefWidth(320);

        Label title = new Label(I18n.get("stats.lbl.topCategories"));
        title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#333;");
        block.getChildren().add(title);

        List<Map.Entry<Category, Integer>> sorted = new ArrayList<>(directQuestionCounts.entrySet());
        sorted.removeIf(e -> e.getValue() == 0 || "Banco de Preguntas".equals(e.getKey().getName()));
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        if (sorted.isEmpty()) {
            Label empty = new Label(I18n.get("stats.lbl.noCategoryData"));
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#6c757d;-fx-font-style:italic;");
            block.getChildren().add(empty);
            return block;
        }

        int maxCount = sorted.get(0).getValue();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            block.getChildren().add(buildBarRow(
                    sorted.get(i).getKey().getName(), sorted.get(i).getValue(), maxCount));
        }
        return block;
    }

    private HBox buildBarRow(String name, int count, int max) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#495057;");
        nameLabel.setPrefWidth(120);
        nameLabel.setWrapText(true);

        Region track = new Region();
        track.setStyle("-fx-background-color:#e9ecef;-fx-background-radius:3;");
        track.setPrefSize(150, 14);

        Region fill = new Region();
        double ratio = max > 0 ? (double) count / max : 0;
        fill.setStyle("-fx-background-color:#0f6cbf;-fx-background-radius:3;");
        fill.setPrefSize(150 * ratio, 14);
        fill.setMaxWidth(150 * ratio);

        javafx.scene.layout.StackPane bar = new javafx.scene.layout.StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#0f6cbf;");
        countLabel.setPrefWidth(30);

        row.getChildren().addAll(nameLabel, bar, countLabel);
        return row;
    }

    // -------------------------------------------------------------------------
    //  KPI card
    // -------------------------------------------------------------------------

    private VBox createKPICard(String title, String value, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 18, 15, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color:#ffffff;-fx-border-color:#dee2e6;"
                + "-fx-border-radius:6;-fx-background-radius:6;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.04),4,0,0,2);");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#6c757d;"
                + "-fx-font-weight:bold;-fx-text-transform:uppercase;");
        lblTitle.setWrapText(true);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size:24px;-fx-text-fill:" + color
                + ";-fx-font-weight:bold;");

        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    // -------------------------------------------------------------------------
    //  Cálculo de métricas (se ejecuta en el Task, fuera del hilo UI)
    // -------------------------------------------------------------------------

    private void calculateStats(Category cat) {
        if (cat == null) return;
        totalCategories++;
        directQuestionCounts.put(cat, cat.getQuestions().size());
        for (Question q : cat.getQuestions()) {
            totalQuestions++;
            typeCounts.merge(q.getType(), 1, Integer::sum);
            if (QuestionValidator.questionContainsImages(q)) questionsWithImages++;
        }
        for (Category sub : cat.getSubcategories()) calculateStats(sub);
    }
}