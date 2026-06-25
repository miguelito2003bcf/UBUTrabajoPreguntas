/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.commands;

/**
 * Interfaz base para el Patrón Command.
 * Permite encapsular operaciones para poder ejecutarlas y revertirlas.
 */
public interface Command {
    void execute();
    void undo();
}