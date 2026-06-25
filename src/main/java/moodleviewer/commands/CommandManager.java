/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.commands;

import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gestor centralizado del historial de comandos.
 * Mantiene las pilas (Stacks) para deshacer y rehacer las operaciones del usuario.
 */
public class CommandManager {
    
    private static final CommandManager INSTANCE = new CommandManager();
    
    // Usamos ArrayDeque por ser más rápido y eficiente que el Stack tradicional en Java
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    private CommandManager() {}

    public static CommandManager getInstance() { 
        return INSTANCE; 
    }

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Si se hace una acción nueva, se pierde la línea temporal "rehacer"
        notifyStateChanged();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            notifyStateChanged();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute(); // Rehacer es, conceptualmente, volver a ejecutar
            undoStack.push(command);
            notifyStateChanged();
        }
    }

    /**
     * Indica si existe alguna acción disponible para deshacer.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Indica si existe alguna acción disponible para rehacer.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Vacía por completo el historial de deshacer/rehacer.
     * Debe invocarse al cargar un nuevo documento, ya que los comandos acumulados
     * harían referencia a categorías/preguntas del banco anterior.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyStateChanged();
    }

    /**
     * Notifica a la interfaz de que el estado de las pilas de deshacer/rehacer ha cambiado,
     * para que los botones correspondientes puedan habilitarse o deshabilitarse.
     */
    private void notifyStateChanged() {
        EventBus.getInstance().publish(new AppEvents.UndoRedoStateChangedEvent(canUndo(), canRedo()));
    }
}