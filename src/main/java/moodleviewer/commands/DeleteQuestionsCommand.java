/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.commands;

import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import java.util.ArrayList;
import java.util.List;

public class DeleteQuestionsCommand implements Command {
    
    private final List<Question> deletedQuestions;
    private final Category targetCategory;

    public DeleteQuestionsCommand(List<Question> questions, Category category) {
        // Guardamos una copia exacta para evitar alteraciones por referencia
        this.deletedQuestions = new ArrayList<>(questions);
        this.targetCategory = category;
    }

    @Override
    public void execute() {
        targetCategory.getQuestions().removeAll(deletedQuestions);
        EventBus.getInstance().publish(new AppEvents.QuestionDeletedEvent(deletedQuestions, targetCategory));
    }

    @Override
    public void undo() {
        targetCategory.getQuestions().addAll(deletedQuestions);
        // Avisamos al sistema de que la categoría ha recuperado elementos
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }
}