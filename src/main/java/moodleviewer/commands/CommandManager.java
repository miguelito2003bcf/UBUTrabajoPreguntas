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
 *
 * También actúa como fuente de verdad de si el documento tiene cambios sin guardar
 * ({@link #isDirty()}), reutilizando el propio historial de comandos: cualquier ejecución
 * de un comando nuevo (o un undo/redo que cambie el estado) marca el documento como "sucio"
 * hasta que se llame explícitamente a {@link #markAsSaved()} (tras guardar con éxito) o a
 * {@link #clear()} (tras cargar un documento nuevo desde disco, donde no tiene sentido hablar
 * de "cambios sin guardar" porque no hay edición todavía).
 */
public class CommandManager {
    
    private static final CommandManager INSTANCE = new CommandManager();
    
    // Usamos ArrayDeque por ser más rápido y eficiente que el Stack tradicional en Java
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private boolean dirty = false;

    private CommandManager() {}

    public static CommandManager getInstance() { 
        return INSTANCE; 
    }

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Si se hace una acción nueva, se pierde la línea temporal "rehacer"
        dirty = true;
        notifyStateChanged();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            dirty = true;
            notifyStateChanged();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute(); // Rehacer es, conceptualmente, volver a ejecutar
            undoStack.push(command);
            dirty = true;
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
     * Indica si el documento tiene cambios realizados desde la última carga o el último
     * guardado con éxito. Pensado para que la interfaz pueda avisar al usuario antes de
     * cerrar la aplicación o de abrir/importar un nuevo banco, evitando pérdidas de trabajo
     * accidentales.
     *
     * @return true si hay cambios sin guardar.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Marca el documento como modificado sin pasar por el historial de deshacer/rehacer.
     * Pensado para operaciones que cambian datos en memoria pero que conceptualmente no son
     * "comandos" reversibles por el usuario con Ctrl+Z (por ejemplo, fusionar un banco importado
     * dentro del actual): no tendría sentido que un undo deshaga una fusión completa de
     * categorías importadas, así que esto solo actualiza el indicador de "hay cambios sin
     * guardar" sin tocar ninguna pila.
     */
    public void markAsDirty() {
        dirty = true;
        notifyStateChanged();
    }

    /**
     * Marca el documento como guardado, sin alterar el historial de deshacer/rehacer (a
     * diferencia de {@link #clear()}). Debe invocarse justo después de que una exportación a
     * XML (el único formato "de guardado" propiamente dicho del proyecto, ya que LaTeX y GIFT
     * son exportaciones derivadas) se complete con éxito.
     */
    public void markAsSaved() {
        dirty = false;
        notifyStateChanged();
    }

    /**
     * Vacía por completo el historial de deshacer/rehacer y limpia el estado de "modificado".
     * Debe invocarse al cargar un nuevo documento, ya que los comandos acumulados
     * harían referencia a categorías/preguntas del banco anterior.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        dirty = false;
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