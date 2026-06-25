/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import moodleviewer.model.Category;
import moodleviewer.util.DuplicateQuestionDetector;
import moodleviewer.util.DuplicateQuestionDetector.DuplicateGroup;
import moodleviewer.util.DuplicateQuestionDetector.DuplicateLocation;
import moodleviewer.util.DuplicateQuestionDetector.MatchType;
import moodleviewer.util.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo que ejecuta el análisis de duplicados sobre el banco completo y muestra los
 * grupos encontrados en una tabla, indicando por qué motivo se consideran duplicados
 * (mismo nombre o mismo enunciado) y dónde se encuentra cada copia.
 */
public class DuplicateQuestionsDialog {

    /**
     * Una fila de la tabla: una pregunta concreta dentro de un grupo de duplicados, junto
     * con el motivo de la coincidencia y su ubicación. Aplanar los grupos en filas individuales
     * (en vez de anidar visualmente) mantiene la tabla simple y reutiliza el mismo patrón de
     * presentación que {@code PreExportValidatorUI}.
     */
    private record DuplicateRow(int groupNumber, MatchType matchType, String categoryPath, String questionName, String questionType) {}

    /**
     * Ejecuta el análisis de duplicados sobre el banco indicado y muestra el resultado.
     * Si no se encuentra ningún duplicado, se informa igualmente con un aviso breve en vez
     * de abrir una tabla vacía.
     *
     * @param rootCategory categoría raíz del banco a analizar.
     */
    public static void showDuplicates(Category rootCategory) {
        List<DuplicateGroup> groups = DuplicateQuestionDetector.findDuplicates(rootCategory);

        if (groups.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, I18n.get("duplicates.none")).showAndWait();
            return;
        }

        List<DuplicateRow> rows = flattenGroups(groups);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18n.get("duplicates.title"));
        alert.setHeaderText(I18n.get("duplicates.header", groups.size()));
        alert.setResizable(true);

        Label infoLabel = new Label(I18n.get("duplicates.info"));
        infoLabel.setWrapText(true);

        TableView<DuplicateRow> table = buildTable(rows);

        VBox content = new VBox(10);
        content.getChildren().addAll(infoLabel, table);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefSize(760, 440);
        alert.getButtonTypes().setAll(ButtonType.CLOSE);

        alert.showAndWait();
    }

    /**
     * Convierte la lista de grupos en filas planas para la tabla, numerando cada grupo
     * (1, 2, 3...) para que el usuario pueda identificar visualmente qué filas pertenecen
     * al mismo grupo de duplicados aunque no estén en absoluto agrupadas visualmente.
     */
    private static List<DuplicateRow> flattenGroups(List<DuplicateGroup> groups) {
        List<DuplicateRow> rows = new ArrayList<>();
        int groupNumber = 1;
        for (DuplicateGroup group : groups) {
            for (DuplicateLocation loc : group.locations()) {
                rows.add(new DuplicateRow(
                        groupNumber,
                        group.matchType(),
                        loc.categoryPath(),
                        loc.question().getName() != null ? loc.question().getName() : "(sin nombre)",
                        loc.question().getType() != null ? loc.question().getType().toUpperCase() : "?"
                ));
            }
            groupNumber++;
        }
        return rows;
    }

    private static TableView<DuplicateRow> buildTable(List<DuplicateRow> rows) {
        TableView<DuplicateRow> table = new TableView<>();
        table.setStyle("-fx-control-inner-background: #fff3cd; -fx-text-fill: #856404;");
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        TableColumn<DuplicateRow, String> groupCol = new TableColumn<>(I18n.get("duplicates.col.group"));
        groupCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().groupNumber())));
        groupCol.setStyle("-fx-alignment: CENTER;");
        groupCol.setPrefWidth(50);

        TableColumn<DuplicateRow, String> reasonCol = new TableColumn<>(I18n.get("duplicates.col.reason"));
        reasonCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().matchType() == MatchType.SAME_NAME
                        ? I18n.get("duplicates.reason.name")
                        : I18n.get("duplicates.reason.text")));
        reasonCol.setPrefWidth(130);

        TableColumn<DuplicateRow, String> pathCol = new TableColumn<>(I18n.get("duplicates.col.path"));
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryPath()));
        pathCol.setCellFactory(wrappingCellFactory());
        pathCol.setPrefWidth(180);

        TableColumn<DuplicateRow, String> nameCol = new TableColumn<>(I18n.get("duplicates.col.question"));
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().questionName()));
        nameCol.setCellFactory(wrappingCellFactory());
        nameCol.setPrefWidth(200);

        TableColumn<DuplicateRow, String> typeCol = new TableColumn<>(I18n.get("duplicates.col.type"));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().questionType()));
        typeCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setPrefWidth(90);

        table.getColumns().setAll(List.of(groupCol, reasonCol, pathCol, nameCol, typeCol));
        table.getItems().setAll(rows);
        return table;
    }

    private static <S> javafx.util.Callback<TableColumn<S, String>, TableCell<S, String>> wrappingCellFactory() {
        return column -> new TableCell<>() {
            private final Text textNode = new Text();
            {
                textNode.wrappingWidthProperty().bind(widthProperty().subtract(10));
                setGraphic(textNode);
                setPrefHeight(Region.USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                textNode.setText(empty || value == null ? "" : value);
            }
        };
    }
}