/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import moodleviewer.model.*;
import moodleviewer.parser.GIFTExportVisitor;
import moodleviewer.parser.LaTeXExporter;
import moodleviewer.util.I18n;
import moodleviewer.util.DragAndDropConstants;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import moodleviewer.commands.Command;
import moodleviewer.commands.CommandManager;
import moodleviewer.commands.DeleteQuestionsCommand;
import moodleviewer.commands.MoveQuestionsCommand;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Gestiona el comportamiento de la tabla de preguntas.
 *
 * PUNTO  4: {@code dragSourceIndex} se resetea en {@code onDragDone} de la fila
 *           para cubrir el caso en que el drag se cancela sin disparar {@code onDragDropped}.
 * PUNTO 15: implementa {@link BankPanelManager} para que el compilador detecte
 *           incompatibilidades si se cambia la firma de {@code configure}.
 * PUNTO 22: columna de número de orden {@code #} añadida a la tabla.
 */
public class TableManager implements BankPanelManager {

    // -------------------------------------------------------------------------
    //  Estado de instancia
    // -------------------------------------------------------------------------

    private Category currentCategory;
    private final Supplier<Category> rootCategorySupplier;
    private int dragSourceIndex = -1;

    private static final String REORDER_QUESTIONS = "REORDER_QUESTIONS";

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    public TableManager(Supplier<Category> rootCategorySupplier) {
        this.rootCategorySupplier = rootCategorySupplier;
    }

    // -------------------------------------------------------------------------
    //  BankPanelManager
    // -------------------------------------------------------------------------

    /**
     * Configura la tabla sin etiqueta de contador (compatibilidad con código existente).
     */
    @Override
    public void configure(TableView<Question> table) {
        configure(table, null);
    }

    /**
     * Configura la tabla con etiqueta de contador (punto D).
     *
     * @param table        tabla de preguntas.
     * @param counterLabel etiqueta donde mostrar "X de Y preguntas" (puede ser null).
     */
    public void configure(TableView<Question> table, Label counterLabel) {

        // Suscripciones al Event Bus
        EventBus.getInstance().subscribe(AppEvents.CategorySelectedEvent.class, event -> {
            currentCategory = event.category();
        });

        EventBus.getInstance().subscribe(AppEvents.MoveQuestionsEvent.class, event -> {
            List<Question> dragged = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (currentCategory != null
                    && currentCategory != event.destCategory()
                    && !dragged.isEmpty()) {
                Command cmd = new MoveQuestionsCommand(dragged, currentCategory, event.destCategory());
                CommandManager.getInstance().executeCommand(cmd);
            }
        });

        EventBus.getInstance().subscribe(AppEvents.CategoryUpdatedEvent.class, event -> {
            table.refresh();
            updateCounter(table, counterLabel);
        });

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // PUNTO 22: columna de número de orden
        TableColumn<Question, String> numCol = new TableColumn<>("#");
        numCol.setCellValueFactory(data -> {
            if (currentCategory == null) return new SimpleStringProperty("-");
            int idx = currentCategory.getQuestions().indexOf(data.getValue());
            return new SimpleStringProperty(idx >= 0 ? String.valueOf(idx + 1) : "-");
        });
        numCol.setStyle("-fx-alignment: CENTER;");
        numCol.setSortable(false);

        TableColumn<Question, String> nameCol = new TableColumn<>(I18n.get("table.col.name"));
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Question, String> typeCol = new TableColumn<>(I18n.get("table.col.type"));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().toUpperCase()));
        typeCol.setStyle("-fx-alignment: CENTER;");

        numCol.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
        nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.70));
        typeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.23));

        table.getColumns().addAll(numCol, nameCol, typeCol);

        // Punto D: contador al cambiar la lista
        table.itemsProperty().addListener((obs, oldList, newList) -> {
            updateCounter(table, counterLabel);
            if (newList != null) {
                newList.addListener(
                    (javafx.collections.ListChangeListener<Question>) c ->
                            updateCounter(table, counterLabel));
            }
        });

        // Punto G: Ctrl+A selecciona todo
        table.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN).match(event)) {
                table.getSelectionModel().selectAll();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                handleDeleteQuestions(table);
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.D) {
                handleDuplicateQuestions(table);
            }
        });

        table.setRowFactory(tv -> buildRow(table));
    }

    // -------------------------------------------------------------------------
    //  Fábrica de filas
    // -------------------------------------------------------------------------

    private TableRow<Question> buildRow(TableView<Question> table) {
        TableRow<Question> row = new TableRow<>();

        row.setOnDragDetected(event -> {
            if (!row.isEmpty()) {
                dragSourceIndex = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(table.getSelectionModel().getSelectedItems().size() > 1
                        ? DragAndDropConstants.MOVE_QUESTIONS : REORDER_QUESTIONS);
                db.setContent(cc);
                SnapshotParameters p = new SnapshotParameters();
                p.setFill(Color.TRANSPARENT);
                db.setDragView(row.snapshot(p, null));
                event.consume();
            }
        });

        // PUNTO 4: resetear dragSourceIndex cuando el drag termina (con o sin drop exitoso)
        row.setOnDragDone(event -> {
            dragSourceIndex = -1;
            event.consume();
        });

        row.setOnDragOver(event -> {
            if (event.getDragboard().hasString()
                    && REORDER_QUESTIONS.equals(event.getDragboard().getString())
                    && !row.isEmpty()
                    && row.getIndex() != dragSourceIndex) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        row.setOnDragEntered(event -> {
            if (event.getDragboard().hasString()
                    && REORDER_QUESTIONS.equals(event.getDragboard().getString())
                    && !row.isEmpty()
                    && row.getIndex() != dragSourceIndex) {
                row.setStyle("-fx-background-color: #d0e8ff;");
            }
        });

        row.setOnDragExited(event -> row.setStyle(""));

        row.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasString()
                    && REORDER_QUESTIONS.equals(event.getDragboard().getString())
                    && currentCategory != null
                    && dragSourceIndex >= 0
                    && !row.isEmpty()) {
                int dest = row.getIndex();
                List<Question> qs = currentCategory.getQuestions();
                if (dragSourceIndex < qs.size() && dest < qs.size()
                        && dragSourceIndex != dest) {
                    Question moved = qs.remove(dragSourceIndex);
                    qs.add(dest, moved);
                    CommandManager.getInstance().markAsDirty();
                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                    table.getSelectionModel().select(dest);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
            // dragSourceIndex se resetea en onDragDone, no aquí
        });

        ContextMenu menu = buildContextMenu(row, table);
        row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
        return row;
    }

    // -------------------------------------------------------------------------
    //  Menú contextual
    // -------------------------------------------------------------------------

    private ContextMenu buildContextMenu(TableRow<Question> row, TableView<Question> table) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem(I18n.get("table.ctx.edit"));
        editItem.setGraphic(IconFactory.of(FontAwesomeSolid.EDIT, 13, "#495057"));
        editItem.setOnAction(e -> {
            Question q = row.getItem();
            if (q != null && currentCategory != null) {
                new AddQuestionDialog(currentCategory, q).showAndWait();
                EventBus.getInstance().publish(new AppEvents.QuestionUpdatedEvent(q));
                EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
            }
        });

        MenuItem dupItem = new MenuItem(I18n.get("table.ctx.duplicate"));
        dupItem.setGraphic(IconFactory.of(FontAwesomeSolid.COPY, 13, "#495057"));
        dupItem.setOnAction(e -> handleDuplicateQuestions(table));

        // Submenú "Mover a categoría..."
        Menu moveToMenu = new Menu(I18n.get("table.ctx.moveTo"));
        moveToMenu.setGraphic(IconFactory.of(FontAwesomeSolid.SHARE, 13, "#495057"));
        moveToMenu.setOnShowing(e -> buildMoveToSubMenu(moveToMenu, table));

        MenuItem expGiftItem = new MenuItem(I18n.get("table.ctx.exportSingle"));
        expGiftItem.setGraphic(IconFactory.of(FontAwesomeSolid.FILE_EXPORT, 13, "#495057"));
        expGiftItem.setOnAction(e -> {
            Question q = row.getItem();
            if (q != null) handleExportSingleGIFT(q, table.getScene().getWindow());
        });

        MenuItem expLatexItem = new MenuItem(I18n.get("table.ctx.exportSingleLatex"));
        expLatexItem.setGraphic(IconFactory.of(FontAwesomeSolid.FILE_ALT, 13, "#495057"));
        expLatexItem.setOnAction(e -> {
            Question q = row.getItem();
            if (q != null) handleExportSingleLaTeX(q, table.getScene().getWindow());
        });

        MenuItem delItem = new MenuItem(I18n.get("table.ctx.delete"));
        delItem.setGraphic(IconFactory.of(FontAwesomeSolid.TRASH_ALT, 13, "#c0392b"));
        delItem.setOnAction(e -> handleDeleteQuestions(table));

        menu.getItems().addAll(editItem, dupItem, moveToMenu,
                new SeparatorMenuItem(), expGiftItem, expLatexItem,
                new SeparatorMenuItem(), delItem);
        return menu;
    }

    // -------------------------------------------------------------------------
    //  Submenú "Mover a categoría..."
    // -------------------------------------------------------------------------

    private void buildMoveToSubMenu(Menu moveToMenu, TableView<Question> table) {
        moveToMenu.getItems().clear();
        Category root = rootCategorySupplier.get();
        if (root == null) return;
        List<Question> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        addCategoryItems(moveToMenu, root, selected, 0);
        if (moveToMenu.getItems().isEmpty()) {
            MenuItem none = new MenuItem(I18n.get("table.ctx.moveTo.none"));
            none.setDisable(true);
            moveToMenu.getItems().add(none);
        }
    }

    private void addCategoryItems(Menu parent, Category cat,
                                  List<Question> selected, int depth) {
        if (cat != currentCategory) {
            String indent = "  ".repeat(depth);
            MenuItem item = new MenuItem(indent + cat.getName());
            item.setOnAction(e -> {
                if (currentCategory != null && !selected.isEmpty()) {
                    Command cmd = new MoveQuestionsCommand(selected, currentCategory, cat);
                    CommandManager.getInstance().executeCommand(cmd);
                }
            });
            parent.getItems().add(item);
        }
        for (Category sub : cat.getSubcategories()) {
            addCategoryItems(parent, sub, selected, depth + 1);
        }
    }

    // -------------------------------------------------------------------------
    //  Contador (punto D)
    // -------------------------------------------------------------------------

    private void updateCounter(TableView<Question> table, Label counterLabel) {
        if (counterLabel == null) return;
        int visible = table.getItems() != null ? table.getItems().size() : 0;
        int total   = currentCategory != null ? currentCategory.getQuestions().size() : 0;
        counterLabel.setText(visible == total
                ? I18n.get("table.counter.all", total)
                : I18n.get("table.counter.filtered", visible, total));
    }

    // -------------------------------------------------------------------------
    //  Exportación LaTeX individual (punto H)
    // -------------------------------------------------------------------------

    private void handleExportSingleLaTeX(Question question, Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.get("file.title.exportSingleLatex"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("file.ext.latex"), "*.tex"));
        String safe = question.getName() != null
                ? question.getName().replaceAll("[\\\\/:*?\"<>|]", "_") : "pregunta";
        fc.setInitialFileName(safe + ".tex");
        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        Category tmp = new Category("tmp");
        tmp.addQuestion(question);
        try {
            LaTeXExporter.exportToLaTeX(tmp, file, true);
            new Alert(Alert.AlertType.INFORMATION,
                    I18n.get("file.info.exportedSingleLatex")).showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    I18n.get("file.err.exportSingle", e.getMessage())).showAndWait();
        }
    }

    // -------------------------------------------------------------------------
    //  Exportación GIFT individual (punto 9 anterior)
    // -------------------------------------------------------------------------

    private void handleExportSingleGIFT(Question question, Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.get("file.title.exportSingleGift"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("file.ext.gift"), "*.txt"));
        String safe = question.getName() != null
                ? question.getName().replaceAll("[\\\\/:*?\"<>|]", "_") : "pregunta";
        fc.setInitialFileName(safe + ".txt");
        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            question.accept(new GIFTExportVisitor(bw));
            new Alert(Alert.AlertType.INFORMATION,
                    I18n.get("file.info.exportedSingle")).showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    I18n.get("file.err.exportSingle", e.getMessage())).showAndWait();
        }
    }

    // -------------------------------------------------------------------------
    //  Acciones internas
    // -------------------------------------------------------------------------

    private void handleDuplicateQuestions(TableView<Question> table) {
        List<Question> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        if (!sel.isEmpty() && currentCategory != null) {
            sel.forEach(q -> currentCategory.getQuestions().add(createDeepCopy(q)));
            CommandManager.getInstance().markAsDirty();
            EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
        }
    }

    private void handleDeleteQuestions(TableView<Question> table) {
        List<Question> toDelete = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        if (!toDelete.isEmpty() && currentCategory != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(I18n.get("table.dlg.del.title"));
            confirm.setHeaderText(I18n.get("table.dlg.del.header", toDelete.size()));
            confirm.setContentText(I18n.get("table.dlg.del.content"));
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    CommandManager.getInstance().executeCommand(
                            new DeleteQuestionsCommand(toDelete, currentCategory));
                }
            });
        }
    }

    private Question createDeepCopy(Question o) {
        String n = o.getName() + " (Copia)";
        Question c;
        if (o instanceof TrueFalseQuestion tf) {
            c = new TrueFalseQuestion(tf.getType(), n, tf.getText(),
                    tf.getDefaultGrade(), tf.getPenalty(),
                    new Answer(tf.getTrueAnswer().getFraction(),  tf.getTrueAnswer().getText(),  tf.getTrueAnswer().getFeedback()),
                    new Answer(tf.getFalseAnswer().getFraction(), tf.getFalseAnswer().getText(), tf.getFalseAnswer().getFeedback()));
        } else if (o instanceof MultichoiceQuestion mc) {
            List<Answer> ans = new ArrayList<>();
            mc.getAnswers().forEach(a -> ans.add(new Answer(a.getFraction(), a.getText(), a.getFeedback())));
            c = new MultichoiceQuestion(mc.getType(), n, mc.getText(),
                    mc.getDefaultGrade(), mc.getPenalty(), mc.isSingleAnswer(), mc.isShuffleAnswers(), ans);
        } else if (o instanceof ShortAnswerQuestion sa) {
            List<Answer> ans = new ArrayList<>();
            sa.getAnswers().forEach(a -> ans.add(new Answer(a.getFraction(), a.getText(), a.getFeedback())));
            c = new ShortAnswerQuestion(sa.getType(), n, sa.getText(),
                    sa.getDefaultGrade(), sa.getPenalty(), sa.isCaseSensitive(), ans);
        } else if (o instanceof NumericalQuestion nq) {
            Answer a = nq.getAnswer();
            c = new NumericalQuestion(nq.getType(), n, nq.getText(),
                    nq.getDefaultGrade(), nq.getPenalty(),
                    new Answer(a.getFraction(), a.getText(), a.getFeedback()), nq.getTolerance());
        } else if (o instanceof MatchingQuestion mq) {
            List<MatchingPair> pairs = new ArrayList<>();
            mq.getPairs().forEach(p -> pairs.add(new MatchingPair(p.getQuestionText(), p.getAnswerText())));
            c = new MatchingQuestion(mq.getType(), n, mq.getText(),
                    mq.getDefaultGrade(), mq.getPenalty(), pairs);
        } else if (o instanceof ClozeQuestion cq) {
            c = new ClozeQuestion(cq.getType(), n, cq.getText(), cq.getDefaultGrade(), cq.getPenalty());
        } else {
            c = new GenericQuestion(o.getType(), n, o.getText(), o.getDefaultGrade(), o.getPenalty());
        }
        c.setGeneralFeedback(o.getGeneralFeedback());
        return c;
    }
}