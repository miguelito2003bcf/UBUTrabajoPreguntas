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
import javafx.scene.layout.VBox;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.util.I18n;
import java.util.HashMap;
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
    private final Map<String, Integer> typeCounts = new HashMap<>();

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

        // SOLUCIÓN 1: Le decimos explícitamente al diálogo que procese el cierre del botón
        this.dialog.setResultConverter(dialogButton -> dialogButton);

        // Procesamos el árbol de datos
        calculateStats(this.rootCategory);

        // Contenedor principal horizontal
        HBox mainLayout = new HBox(30);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);
        
        // SOLUCIÓN 2: Aumentamos la altura de 420 a 480 para que respire mejor
        mainLayout.setPrefSize(750, 480);

        // --- 1. BLOQUE DE TARJETAS INDICADORAS (KPIs) ---
        VBox kpiContainer = new VBox(20);
        kpiContainer.setAlignment(Pos.CENTER);
        kpiContainer.setPrefWidth(240);

        // Restamos 1 al total de categorías para ignorar la raíz principal
        int displayCategories = Math.max(0, totalCategories - 1);
        VBox catCard = createKPICard(I18n.get("stats.lbl.totalCategories"), String.valueOf(displayCategories));
        
        VBox qCard = createKPICard(I18n.get("stats.lbl.totalQuestions"), String.valueOf(totalQuestions));
        
        kpiContainer.getChildren().addAll(catCard, qCard);

        // --- 2. BLOQUE GRÁFICO (PieChart) ---
        PieChart pieChart = new PieChart();
        pieChart.setTitle(I18n.get("stats.chart.title"));
        pieChart.setLegendVisible(true);
        
        // SOLUCIÓN 3: Movemos la leyenda a la derecha. Evita el desbordamiento inferior y queda más limpio
        pieChart.setLegendSide(Side.RIGHT);
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        typeCounts.forEach((type, count) -> {
            double percentage = totalQuestions > 0 ? ((double) count / totalQuestions) * 100 : 0;
            // Formateamos la etiqueta para incluir el conteo absoluto y su porcentaje
            String label = String.format("%s (%d | %.1f%%)", type.toUpperCase(), count, percentage);
            pieChartData.add(new PieChart.Data(label, count));
        });
        
        pieChart.setData(pieChartData);

        mainLayout.getChildren().addAll(kpiContainer, pieChart);
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
        for (Question q : cat.getQuestions()) {
            totalQuestions++;
            typeCounts.put(q.getType(), typeCounts.getOrDefault(q.getType(), 0) + 1);
        }
        for (Category sub : cat.getSubcategories()) {
            calculateStats(sub);
        }
    }

    /**
     * Helper encargado de fabricar tarjetas visuales limpias y contrastadas para métricas.
     */
    private VBox createKPICard(String title, String value) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dee2e6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 4, 0, 0, 2);");
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-weight: bold; -fx-text-transform: uppercase;");
        
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 26px; -fx-text-fill: #0f6cbf; -fx-font-weight: bold;");
        
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }
}