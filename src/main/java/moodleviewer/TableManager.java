/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import moodleviewer.model.*;
import moodleviewer.util.I18n;
import moodleviewer.util.DragAndDropConstants;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import moodleviewer.commands.Command;
import moodleviewer.commands.CommandManager;
import moodleviewer.commands.DeleteQuestionsCommand;

import java.util.ArrayList;
import java.util.List;

public class TableManager {
    
    private static Category currentCategory;

    public static void configure(TableView<Question> table) {
        
        // --- SUSCRIPCIONES AL EVENT BUS ---
        EventBus.getInstance().subscribe(AppEvents.CategorySelectedEvent.class, event -> {
            currentCategory = event.category();
        });

        EventBus.getInstance().subscribe(AppEvents.MoveQuestionsEvent.class, event -> {
            List<Question> draggedQs = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (currentCategory != null && currentCategory != event.destCategory() && !draggedQs.isEmpty()) {
                currentCategory.getQuestions().removeAll(draggedQs);
                event.destCategory().getQuestions().addAll(draggedQs);
                EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
            }
        });

        EventBus.getInstance().subscribe(AppEvents.CategoryUpdatedEvent.class, event -> {
            table.refresh();
        });

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Question, String> nameColumn = new TableColumn<>(I18n.get("table.col.name"));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        
        TableColumn<Question, String> typeColumn = new TableColumn<>(I18n.get("table.col.type"));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().toUpperCase()));
        typeColumn.setStyle("-fx-alignment: CENTER;"); 
        
        nameColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.75));
        typeColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.24));
        
        table.getColumns().add(nameColumn);
        table.getColumns().add(typeColumn);

        // --- GESTIÓN DE EVENTOS DE TECLADO ---
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                handleDeleteQuestions(table);
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.D) {
                handleDuplicateQuestions(table);
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Question> row = new TableRow<>();
            
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(DragAndDropConstants.MOVE_QUESTIONS);
                    db.setContent(content);
                    
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    db.setDragView(row.snapshot(params, null));
                    event.consume();
                }
            });

            ContextMenu questionMenu = new ContextMenu(); 
            
            MenuItem editQuestionItem = new MenuItem(I18n.get("table.ctx.edit"));
            editQuestionItem.setGraphic(IconFactory.of(FontAwesomeSolid.EDIT, 13, "#495057"));
            editQuestionItem.setOnAction(event -> {
                Question q = row.getItem();
                if (q != null && currentCategory != null) {
                    AddQuestionDialog dialog = new AddQuestionDialog(currentCategory, q);
                    dialog.showAndWait();
                    
                    EventBus.getInstance().publish(new AppEvents.QuestionUpdatedEvent(q));
                    EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
                }
            });

            MenuItem duplicateQuestionItem = new MenuItem(I18n.get("table.ctx.duplicate"));
            duplicateQuestionItem.setGraphic(IconFactory.of(FontAwesomeSolid.COPY, 13, "#495057"));
            duplicateQuestionItem.setOnAction(event -> handleDuplicateQuestions(table));

            MenuItem deleteQuestionItem = new MenuItem(I18n.get("table.ctx.delete"));
            deleteQuestionItem.setGraphic(IconFactory.of(FontAwesomeSolid.TRASH_ALT, 13, "#c0392b"));
            deleteQuestionItem.setOnAction(event -> handleDeleteQuestions(table));
            
            questionMenu.getItems().addAll(editQuestionItem, duplicateQuestionItem, deleteQuestionItem);
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(questionMenu));
            return row;
        });
    }

    private static void handleDuplicateQuestions(TableView<Question> table) {
        List<Question> selectedQs = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        
        if (!selectedQs.isEmpty() && currentCategory != null) {
            for (Question q : selectedQs) {
                Question clone = createDeepCopy(q);
                currentCategory.getQuestions().add(clone);
            }
            EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
        }
    }

    private static void handleDeleteQuestions(TableView<Question> table) {
        List<Question> questionsToDelete = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        
        if (!questionsToDelete.isEmpty() && currentCategory != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(I18n.get("table.dlg.del.title"));
            confirm.setHeaderText(I18n.get("table.dlg.del.header", questionsToDelete.size()));
            confirm.setContentText(I18n.get("table.dlg.del.content"));

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // PATRÓN COMMAND: Encapsulamos el borrado en un comando y lo enviamos al gestor
                    Command deleteCmd = new DeleteQuestionsCommand(questionsToDelete, currentCategory);
                    CommandManager.getInstance().executeCommand(deleteCmd);
                }
            });
        }
    }

    private static Question createDeepCopy(Question original) {
        String newName = original.getName() + " (Copia)";
        Question copy = null;

        if (original instanceof TrueFalseQuestion tf) {
            Answer ta = new Answer(tf.getTrueAnswer().getFraction(), tf.getTrueAnswer().getText(), tf.getTrueAnswer().getFeedback());
            Answer fa = new Answer(tf.getFalseAnswer().getFraction(), tf.getFalseAnswer().getText(), tf.getFalseAnswer().getFeedback());
            copy = new TrueFalseQuestion(tf.getType(), newName, tf.getText(), tf.getDefaultGrade(), tf.getPenalty(), ta, fa);
        } else if (original instanceof MultichoiceQuestion mc) {
            List<Answer> ansCopy = new ArrayList<>();
            for (Answer a : mc.getAnswers()) ansCopy.add(new Answer(a.getFraction(), a.getText(), a.getFeedback()));
            copy = new MultichoiceQuestion(mc.getType(), newName, mc.getText(), mc.getDefaultGrade(), mc.getPenalty(), mc.isSingleAnswer(), mc.isShuffleAnswers(), ansCopy);
        } else if (original instanceof ShortAnswerQuestion sa) {
            List<Answer> ansCopy = new ArrayList<>();
            for (Answer a : sa.getAnswers()) ansCopy.add(new Answer(a.getFraction(), a.getText(), a.getFeedback()));
            copy = new ShortAnswerQuestion(sa.getType(), newName, sa.getText(), sa.getDefaultGrade(), sa.getPenalty(), sa.isCaseSensitive(), ansCopy);
        } else if (original instanceof NumericalQuestion nq) {
            Answer a = nq.getAnswer();
            Answer ansCopy = new Answer(a.getFraction(), a.getText(), a.getFeedback());
            copy = new NumericalQuestion(nq.getType(), newName, nq.getText(), nq.getDefaultGrade(), nq.getPenalty(), ansCopy, nq.getTolerance());
        } else if (original instanceof MatchingQuestion mq) {
            List<MatchingPair> pairsCopy = new ArrayList<>();
            for (MatchingPair p : mq.getPairs()) pairsCopy.add(new MatchingPair(p.getQuestionText(), p.getAnswerText()));
            copy = new MatchingQuestion(mq.getType(), newName, mq.getText(), mq.getDefaultGrade(), mq.getPenalty(), pairsCopy);
        } else if (original instanceof ClozeQuestion cq) {
            copy = new ClozeQuestion(cq.getType(), newName, cq.getText(), cq.getDefaultGrade(), cq.getPenalty());
        } else {
            copy = new GenericQuestion(original.getType(), newName, original.getText(), original.getDefaultGrade(), original.getPenalty());
        }

        copy.setGeneralFeedback(original.getGeneralFeedback());
        return copy;
    }
}