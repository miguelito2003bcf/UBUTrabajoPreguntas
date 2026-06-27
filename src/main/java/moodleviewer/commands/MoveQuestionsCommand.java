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

/**
 * Comando reversible para mover una lista de preguntas de una categoría origen a una destino
 * (punto B — mover desde menú contextual, y punto E — hacerlo reversible).
 * Reutilizado también por el drag & drop entre categorías del árbol.
 */
public class MoveQuestionsCommand implements Command {

    private final List<Question> questions;
    private final Category source;
    private final Category dest;

    public MoveQuestionsCommand(List<Question> questions, Category source, Category dest) {
        this.questions = new ArrayList<>(questions);
        this.source    = source;
        this.dest      = dest;
    }

    @Override
    public void execute() {
        source.getQuestions().removeAll(questions);
        dest.getQuestions().addAll(questions);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }

    @Override
    public void undo() {
        dest.getQuestions().removeAll(questions);
        source.getQuestions().addAll(questions);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }
}