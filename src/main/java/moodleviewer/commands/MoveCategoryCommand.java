/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.commands;

import moodleviewer.model.Category;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;

public class MoveCategoryCommand implements Command {
    
    private final Category categoryToMove;
    private final Category sourceParent;
    private final Category destParent;

    public MoveCategoryCommand(Category categoryToMove, Category sourceParent, Category destParent) {
        this.categoryToMove = categoryToMove;
        this.sourceParent = sourceParent;
        this.destParent = destParent;
    }

    @Override
    public void execute() {
        sourceParent.getSubcategories().remove(categoryToMove);
        destParent.getSubcategories().add(categoryToMove);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }

    @Override
    public void undo() {
        destParent.getSubcategories().remove(categoryToMove);
        sourceParent.getSubcategories().add(categoryToMove);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }
}