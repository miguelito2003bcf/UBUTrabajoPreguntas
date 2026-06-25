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
import moodleviewer.util.I18n;
import moodleviewer.util.QuestionValidator;
import moodleviewer.util.QuestionValidator.ValidationWarning;

import java.util.List;
import java.util.Optional;

public class PreExportValidatorUI {

    /**
     * Sobrecarga de compatibilidad: ejecuta la validación estándar sin comprobar
     * la incompatibilidad de imágenes embebidas con el formato de destino.
     *
     * @return true si se puede exportar (sin errores, o ignorándolos), false si el usuario cancela.
     */
    public static boolean checkAndConfirmExport(Category rootCategory) {
        return checkAndConfirmExport(rootCategory, false);
    }

    /**
     * Ejecuta el análisis y muestra un cuadro de diálogo con una tabla de avisos si hay errores.
     *
     * @param rootCategory categoría raíz (o subárbol filtrado) a validar antes de exportar.
     * @param checkUnsupportedImages true si el formato de destino no soporta imágenes embebidas
     *                                (por ejemplo, GIFT en bruto sin [html]) y por tanto deben
     *                                añadirse avisos adicionales por cada pregunta con imágenes.
     * @return true si se puede exportar (sin errores, o ignorándolos), false si el usuario cancela.
     */
    public static boolean checkAndConfirmExport(Category rootCategory, boolean checkUnsupportedImages) {
        List<ValidationWarning> warnings = QuestionValidator.validateBank(rootCategory, checkUnsupportedImages);

        if (warnings.isEmpty()) {
            return true; // Todo correcto, continuar exportación
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18n.get("validator.title"));
        alert.setHeaderText(I18n.get("validator.header", warnings.size()));
        alert.setResizable(true);

        Label infoLabel = new Label(I18n.get("validator.info"));
        infoLabel.setWrapText(true);

        TableView<ValidationWarning> table = buildWarningsTable(warnings);

        VBox content = new VBox(10);
        content.getChildren().addAll(infoLabel, table);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefSize(720, 420);

        ButtonType btnExportAnyway = new ButtonType(I18n.get("validator.btnExportAnyway"), ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType(I18n.get("validator.btnCancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(btnExportAnyway, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnExportAnyway;
    }

    /**
     * Construye la tabla de avisos con cuatro columnas (Ruta, Pregunta, Tipo, Error). La columna
     * de error envuelve el texto en varias líneas y la fila ajusta su altura automáticamente,
     * para que los mensajes largos no se corten ni obliguen a hacer scroll horizontal.
     */
    private static TableView<ValidationWarning> buildWarningsTable(List<ValidationWarning> warnings) {
        TableView<ValidationWarning> table = new TableView<>();
        table.setStyle("-fx-control-inner-background: #fff3cd; -fx-text-fill: #856404;");
        table.setPrefHeight(280);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Permite que cada fila crezca verticalmente según el contenido envuelto de sus celdas,
        // en vez de quedarse en una sola línea fija y cortar el texto del mensaje de error.
        table.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        TableColumn<ValidationWarning, String> pathCol = new TableColumn<>(I18n.get("validator.col.path"));
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryPath()));
        pathCol.setCellFactory(wrappingCellFactory());
        pathCol.setPrefWidth(180);

        TableColumn<ValidationWarning, String> nameCol = new TableColumn<>(I18n.get("validator.col.question"));
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().questionName()));
        nameCol.setCellFactory(wrappingCellFactory());
        nameCol.setPrefWidth(150);

        TableColumn<ValidationWarning, String> typeCol = new TableColumn<>(I18n.get("validator.col.type"));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().questionType().toUpperCase()));
        typeCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setPrefWidth(90);

        TableColumn<ValidationWarning, String> messageCol = new TableColumn<>(I18n.get("validator.col.error"));
        messageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().message()));
        messageCol.setCellFactory(wrappingCellFactory());
        messageCol.setPrefWidth(300);

        table.getColumns().setAll(List.of(pathCol, nameCol, typeCol, messageCol));
        table.getItems().setAll(warnings);
        return table;
    }

    /**
     * Crea una fábrica de celdas que envuelve el texto en varias líneas en lugar de cortarlo,
     * usado en las columnas cuyo contenido puede ser largo (ruta, nombre, mensaje de error).
     */
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