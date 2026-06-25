/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.events;

import moodleviewer.model.Category;
import moodleviewer.model.Question;
import java.util.List;

/**
 * Contenedor de los distintos eventos de la aplicación utilizando Records (Java 14+).
 */
public class AppEvents {
    // Se emite cuando el usuario selecciona una categoría en el árbol
    public record CategorySelectedEvent(Category category) {}
    
    // Se emite cuando el árbol sufre modificaciones estructurales (renombrar, borrar, mover)
    public record CategoryUpdatedEvent() {}
    
    // Se emite cuando se eliminan preguntas de la tabla
    public record QuestionDeletedEvent(List<Question> questions, Category fromCategory) {}
    
    // Se emite cuando se modifican los datos de una pregunta (edición)
    public record QuestionUpdatedEvent(Question question) {}
    
    // Añadir este evento dentro de AppEvents.java
    public record MoveQuestionsEvent(Category destCategory) {}
    
    // Se emite cuando cambia el estado de las pilas de deshacer/rehacer del CommandManager
    public record UndoRedoStateChangedEvent(boolean canUndo, boolean canRedo) {}
}