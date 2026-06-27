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
 * Comando reversible para la operación "Fusionar preguntas" del drag & drop del árbol (punto E).
 *
 * Al ejecutar: las preguntas de {@code sourceCategory} se añaden a {@code destCategory} y
 * {@code sourceCategory} se elimina del árbol de su padre.
 * Al deshacer: las preguntas vuelven a {@code sourceCategory} y esta se reinserta en su posición
 * original dentro de las subcategorías del padre.
 */
public class MergeIntoCategoryCommand implements Command {

    private final Category sourceCategory;
    private final Category sourceParent;
    private final Category destCategory;
    /** Copia de las preguntas que se movieron, para poder revertirlas. */
    private final List<Question> movedQuestions;
    /** Posición que ocupaba sourceCategory en la lista de subcategorías de su padre. */
    private final int originalIndex;

    public MergeIntoCategoryCommand(Category sourceCategory,
                                    Category sourceParent,
                                    Category destCategory) {
        this.sourceCategory  = sourceCategory;
        this.sourceParent    = sourceParent;
        this.destCategory    = destCategory;
        this.movedQuestions  = new ArrayList<>(sourceCategory.getQuestions());
        this.originalIndex   = sourceParent.getSubcategories().indexOf(sourceCategory);
    }

    @Override
    public void execute() {
        destCategory.getQuestions().addAll(movedQuestions);
        sourceParent.getSubcategories().remove(sourceCategory);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }

    @Override
    public void undo() {
        destCategory.getQuestions().removeAll(movedQuestions);
        // Reinsertamos en la posición original (o al final si el índice ya no es válido)
        int idx = Math.min(originalIndex, sourceParent.getSubcategories().size());
        sourceParent.getSubcategories().add(idx, sourceCategory);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }
}