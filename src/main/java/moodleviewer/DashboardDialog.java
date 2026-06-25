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
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.util.I18n;
import moodleviewer.util.QuestionValidator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diálogo encargado de compilar de forma recursiva y mostrar visualmente
 * las métricas analíticas e indicadores clave (KPIs) del banco de preguntas activo.
 */
public class DashboardDialog {

    private final Category rootCategory;
    private final Dialog<ButtonType> dialog;

    private int totalCategories = 0;
    private int totalQuestions = 0;
    private int questionsWithImages = 0;
    private final Map<String, Integer> typeCounts = new HashMap<>();
    /** Conteo de preguntas DIRECTAS (no recursivo) por categoría, usado para el ranking Top 5. */
    private final Map<Category, Integer> directQuestionCounts = new HashMap<>();

    public DashboardDialog(Category rootCategory) {
        this.rootCategory = rootCategory;
        this.dialog = new Dialog<>();
        
        // Vinculamos la hoja de estilos para evitar corrupción por modo oscuro del sistema.
        // Comprobamos que el recurso exista antes de usarlo (igual que en AddQuestionDialog),
        // ya que getResource(...) devuelve null si el classpath está roto o el CSS no se
        // empaquetó, y eso provocaría un NullPointerException inmediato al abrir el diálogo.
        if (getClass().getResource("/styles.css") != null) {
            this.dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        this.dialog.setTitle(I18n.get("stats.dialog.title"));
        this.dialog.setHeaderText(I18n.get("stats.dialog.header"));
        this.dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        this.dialog.setResultConverter(dialogButton -> dialogButton);
        this.dialog.setResizable(true);

        // Procesamos el árbol de datos
        calculateStats(this.rootCategory);
        int pendingWarnings = QuestionValidator.validateBank(this.rootCategory).size();

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setPrefSize(820, 560);

        // --- 1. FILA DE TARJETAS INDICADORAS (KPIs) ---
        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER);

        int displayCategories = Math.max(0, totalCategories - 1);
        kpiRow.getChildren().addAll(
                createKPICard(I18n.get("stats.lbl.totalCategories"), String.valueOf(displayCategories), "#0f6cbf"),
                createKPICard(I18n.get("stats.lbl.totalQuestions"), String.valueOf(totalQuestions), "#0f6cbf"),
                createKPICard(I18n.get("stats.lbl.questionsWithImages"), String.valueOf(questionsWithImages), "#6c757d"),
                createKPICard(I18n.get("stats.lbl.pendingWarnings"), String.valueOf(pendingWarnings), pendingWarnings > 0 ? "#c0392b" : "#28a745")
        );

        // --- 2. FILA CENTRAL: GRÁFICO POR TIPO + TOP 5 CATEGORÍAS ---
        HBox centerRow = new HBox(30);
        centerRow.setAlignment(Pos.CENTER);
        centerRow.setPrefHeight(380);

        PieChart pieChart = buildTypeBreakdownChart();

        VBox topCategoriesBlock = buildTopCategoriesBlock();

        centerRow.getChildren().addAll(pieChart, topCategoriesBlock);
        HBox.setHgrow(pieChart, Priority.ALWAYS);
        HBox.setHgrow(topCategoriesBlock, Priority.ALWAYS);

        mainLayout.getChildren().addAll(kpiRow, centerRow);
        this.dialog.getDialogPane().setContent(mainLayout);
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

    /**
     * Recorre el árbol completo acumulando métricas de forma iterativa y segura.
     */
    private void calculateStats(Category cat) {
        if (cat == null) return;
        
        totalCategories++;
        directQuestionCounts.put(cat, cat.getQuestions().size());

        for (Question q : cat.getQuestions()) {
            totalQuestions++;
            typeCounts.put(q.getType(), typeCounts.getOrDefault(q.getType(), 0) + 1);
            if (QuestionValidator.questionContainsImages(q)) {
                questionsWithImages++;
            }
        }
        for (Category sub : cat.getSubcategories()) {
            calculateStats(sub);
        }
    }

    /**
     * Construye el gráfico de tarta con el desglose de preguntas por tipo de Moodle.
     */
    private PieChart buildTypeBreakdownChart() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle(I18n.get("stats.chart.title"));
        pieChart.setLegendVisible(true);
        pieChart.setLegendSide(Side.BOTTOM);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        typeCounts.forEach((type, count) -> {
            double percentage = totalQuestions > 0 ? ((double) count / totalQuestions) * 100 : 0;
            String label = String.format("%s (%d | %.1f%%)", type.toUpperCase(), count, percentage);
            pieChartData.add(new PieChart.Data(label, count));
        });
        pieChart.setData(pieChartData);
        return pieChart;
    }

    /**
     * Construye el bloque "Top 5 categorías con más preguntas", usando barras horizontales
     * proporcionales sencillas (sin depender de ninguna librería de gráficos adicional) para
     * mostrar de un vistazo qué categorías concentran más contenido. Solo cuenta preguntas
     * DIRECTAS de cada categoría (no las de sus subcategorías), ya que mezclar ambos criterios
     * haría que categorías "contenedoras" de alto nivel siempre encabezasen el ranking sin
     * aportar información útil sobre dónde está realmente el contenido.
     */
    private VBox buildTopCategoriesBlock() {
        VBox block = new VBox(10);
        block.setAlignment(Pos.TOP_LEFT);
        block.setPrefWidth(320);

        Label title = new Label(I18n.get("stats.lbl.topCategories"));
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");
        block.getChildren().add(title);

        List<Map.Entry<Category, Integer>> sorted = new ArrayList<>(directQuestionCounts.entrySet());
        sorted.removeIf(e -> e.getValue() == 0 || "Banco de Preguntas".equals(e.getKey().getName()));
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        if (sorted.isEmpty()) {
            Label empty = new Label(I18n.get("stats.lbl.noCategoryData"));
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
            block.getChildren().add(empty);
            return block;
        }

        int maxCount = sorted.get(0).getValue();
        int limit = Math.min(5, sorted.size());

        for (int i = 0; i < limit; i++) {
            Map.Entry<Category, Integer> entry = sorted.get(i);
            block.getChildren().add(buildCategoryBarRow(entry.getKey().getName(), entry.getValue(), maxCount));
        }
        return block;
    }

    /**
     * Construye una fila individual del ranking: nombre de categoría, barra horizontal
     * proporcional al máximo del ranking, y el número absoluto de preguntas.
     */
    private HBox buildCategoryBarRow(String categoryName, int count, int maxCount) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(categoryName);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");
        nameLabel.setPrefWidth(120);
        nameLabel.setWrapText(true);

        Region barTrack = new Region();
        barTrack.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 3;");
        barTrack.setPrefHeight(14);
        barTrack.setPrefWidth(150);

        Region barFill = new Region();
        double ratio = maxCount > 0 ? (double) count / maxCount : 0;
        barFill.setStyle("-fx-background-color: #0f6cbf; -fx-background-radius: 3;");
        barFill.setPrefHeight(14);
        barFill.setPrefWidth(150 * ratio);
        barFill.setMaxWidth(150 * ratio);

        javafx.scene.layout.StackPane barStack = new javafx.scene.layout.StackPane();
        barStack.setAlignment(Pos.CENTER_LEFT);
        barStack.getChildren().addAll(barTrack, barFill);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0f6cbf;");
        countLabel.setPrefWidth(30);

        row.getChildren().addAll(nameLabel, barStack, countLabel);
        return row;
    }

    /**
     * Helper encargado de fabricar tarjetas visuales limpias y contrastadas para métricas.
     *
     * @param accentColor color de acento para el valor numérico, permitiendo distinguir
     *                     visualmente las tarjetas neutras de las que requieren atención
     *                     (por ejemplo, en rojo cuando hay avisos de validación pendientes).
     */
    private VBox createKPICard(String title, String value, String accentColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 18, 15, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dee2e6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 4, 0, 0, 2);");
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-weight: bold; -fx-text-transform: uppercase;");
        lblTitle.setWrapText(true);
        
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 24px; -fx-text-fill: " + accentColor + "; -fx-font-weight: bold;");
        
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }
}