/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.commands;

import moodleviewer.model.Category;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;

/**
 * Comando reversible para renombrar una categoría (punto E).
 * Almacena el nombre anterior para poder deshacerlo con Ctrl+Z.
 */
public class RenameCategoryCommand implements Command {

    private final Category category;
    private final String oldName;
    private final String newName;

    public RenameCategoryCommand(Category category, String newName) {
        this.category = category;
        this.oldName  = category.getName();
        this.newName  = newName;
    }

    @Override
    public void execute() {
        category.setName(newName);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }

    @Override
    public void undo() {
        category.setName(oldName);
        EventBus.getInstance().publish(new AppEvents.CategoryUpdatedEvent());
    }
}