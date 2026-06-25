/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.util;

/**
 * Clase de utilidad que centraliza las etiquetas (Magic Strings) utilizadas 
 * en las operaciones de arrastrar y soltar (Drag and Drop) del portapapeles.
 */
public final class DragAndDropConstants {
    
    // Evitamos que la clase se pueda instanciar
    private DragAndDropConstants() {}

    public static final String MOVE_QUESTIONS = "MOVER_PREGUNTAS";
    public static final String MOVE_CATEGORY = "MOVER_CATEGORIA";
    
}