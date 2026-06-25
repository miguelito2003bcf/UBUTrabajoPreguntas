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
 * Clase de utilidad que centraliza TODAS las constantes de estilo CSS usadas al generar
 * la vista previa HTML de las preguntas (clases {@code Question} y sus subclases en
 * {@code moodleviewer.model}).
 *
 * El objetivo es que ningún estilo inline "suelto" (un {@code style="..."} escrito a mano
 * dentro de una clase de pregunta) quede fuera de aquí: así, cambiar el aspecto visual de
 * cualquier elemento de la vista previa (colores, tipografía, espaciados, bordes...) se hace
 * en un único punto, sin tener que rastrear cada subclase de Question una por una.
 *
 * Organización: las constantes se agrupan por bloque funcional (estructura, tipografía,
 * formularios deshabilitados, feedback, Cloze...) en vez de en una lista plana, para que
 * añadir un nuevo bloque en el futuro sea tan sencillo como añadir una nueva sección con
 * el mismo patrón de nombrado ({@code BLOQUE_ELEMENTO}).
 */
public final class HtmlConstants {

    private HtmlConstants() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }

    // =====================================================================
    //  1. ESTRUCTURA GENERAL DE LA TARJETA (envoltorio de toda vista previa)
    // =====================================================================

    public static final String HTML_BODY = "padding: 5px 5px 20px 5px; min-height: 100vh; box-sizing: border-box;";
    public static final String HTML_CARD = "background-color: #fff; border: 1px solid #dee2e6; border-radius: 4px; padding: 25px 20px 25px 20px; box-shadow: 0 1px 3px rgba(0,0,0,.05);";
    public static final String HEADER_FLEX = "border-bottom: 1px solid #eee; padding-bottom: 12px; margin-bottom: 15px; display: flex; justify-content: center; align-items: center;";

    // =====================================================================
    //  2. TIPOGRAFÍA Y ETIQUETAS DE TEXTO
    // =====================================================================

    public static final String TITLE = "color: #000000; font-size: 1.3rem; font-weight: bold; text-align: center;";
    public static final String BADGE = "margin-left: 100px; background-color: #6c757d; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;";
    public static final String TEXT_BASE = "font-size: 15px; margin-bottom: 25px; color: #212529; line-height: 1.5;";
    public static final String LABEL_BOLD = "margin-bottom: 15px; font-size: 14px; font-weight: bold; color: #333;";

    /** Texto en negrita usado como etiqueta inline antes de un campo (ej. "Respuesta:"). */
    public static final String LABEL_INLINE_BOLD = "margin-right: 15px;";

    // =====================================================================
    //  3. BLOQUES DE FEEDBACK Y ALERTAS
    // =====================================================================

    public static final String FEEDBACK_GENERAL = "margin-top: 25px; padding: 15px; background-color: #d1ecf1; border: 1px solid #bee5eb; border-radius: 4px; color: #0c5460; font-size: 14px;";
    public static final String FEEDBACK_WARNING = "margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;";

    // =====================================================================
    //  4. FORMULARIOS DESHABILITADOS Y DISPOSICIÓN GENERAL
    //     (controles de solo-lectura que simulan lo que vería el alumno)
    // =====================================================================

    public static final String FLEX_ROW = "display: flex; align-items: center; margin-bottom: 20px; font-size: 15px; color: #212529;";
    public static final String FLEX_ROW_START = "display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 15px; color: #212529;";
    public static final String INPUT_BASE = "padding: 8px; border: 1px solid #ccc; border-radius: 4px; background-color: #f8f9fa;";
    public static final String TABLE_LAYOUT = "width: 100%; border-collapse: separate; border-spacing: 0 15px; font-size: 15px; color: #212529;";

    /** Radio button de opción única (Verdadero/Falso), agrandado para visibilidad. */
    public static final String RADIO_OPTION = "margin-top: 5px; margin-right: 12px; transform: scale(1.2);";

    /** Área de texto deshabilitada de altura fija que simula el editor de respuesta de un ensayo. */
    public static final String ESSAY_PLACEHOLDER = INPUT_BASE + " height: 150px; display: flex; align-items: center; justify-content: center; color: #6c757d; font-style: italic;";

    // =====================================================================
    //  5. TABLA DE EMPAREJAMIENTO (MatchingQuestion)
    // =====================================================================

    public static final String MATCHING_CELL_QUESTION = "vertical-align: middle; width: 45%; text-align: right; padding-right: 20px;";
    public static final String MATCHING_CELL_ANSWER = "vertical-align: middle; width: 55%;";
    public static final String MATCHING_SELECT = INPUT_BASE + " width: 100%; max-width: 250px;";

    // =====================================================================
    //  6. SELECTOR DE OPCIÓN MÚLTIPLE (MultichoiceQuestion)
    // =====================================================================

    public static final String MULTICHOICE_SELECT = INPUT_BASE + " width: 100%;";

    // =====================================================================
    //  7. ESPECÍFICOS DE CLOZE — bloques de sintaxis y vista previa de alumno
    // =====================================================================

    public static final String CLOZE_CODE_BLOCK = "padding: 15px; background-color: #ffffff; border: 1px dashed #adb5bd; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;";
    public static final String CLOZE_RENDER_BLOCK = "padding: 15px; background-color: #fcfcfc; border: 1px solid #dee2e6; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;";
    public static final String CLOZE_HIGHLIGHT = "background-color: #e9ecef; color: #0056b3; font-family: 'Courier New', monospace; padding: 2px 6px; border-radius: 4px; font-weight: bold;";

    /** Contenedor exterior del bloque "Sintaxis Cloze:" (separación inferior respecto al resto). */
    public static final String CLOZE_SECTION_WRAPPER = "margin-bottom: 25px;";

    /** Selector desplegable Cloze de tipo MULTICHOICE (una sola respuesta). */
    public static final String CLOZE_SELECT = INPUT_BASE + " width: auto; max-width: 600px; font-size: 15px; color: #495057; cursor: pointer;";

    /** Campo de texto Cloze de tipo SHORTANSWER/NUMERICAL, con aspecto de placeholder. */
    public static final String CLOZE_TEXT_INPUT = "border:1px solid #ced4da; border-radius: 4px; padding:4px 8px; font-style:italic; color:#495057; background-color:#e9ecef;";

    /** Contenedor de opciones Cloze tipo MULTICHOICE_V / MULTIRESPONSE (layout vertical). */
    public static final String CLOZE_OPTIONS_WRAPPER_VERTICAL = "display:inline-block; vertical-align:top; border:1px dashed #ccc; padding:5px; border-radius:4px;";

    /** Contenedor de opciones Cloze tipo MULTICHOICE_H / MULTIRESPONSE_H (layout horizontal). */
    public static final String CLOZE_OPTIONS_WRAPPER_HORIZONTAL = "display:inline-block; vertical-align:top; border:1px dashed #ccc; padding:2px 8px; border-radius:4px;";

    /** Línea de opción dentro de un contenedor vertical (radio/checkbox + texto en su propia fila). */
    public static final String CLOZE_OPTION_LABEL_VERTICAL = "display:block; margin-bottom:2px;";

    /** Línea de opción dentro de un contenedor horizontal (separación entre opciones consecutivas). */
    public static final String CLOZE_OPTION_LABEL_HORIZONTAL = "margin-right:15px;";

    /** Color de la opción marcada como correcta dentro de un <option> de un <select> Cloze. */
    public static final String CLOZE_OPTION_CORRECT = "color:#15803d; font-weight:bold;";

    /** Color por defecto de una opción no marcada como correcta dentro de un <option> Cloze. */
    public static final String CLOZE_OPTION_DEFAULT = "color:#333;";

    /** Color usado para mostrar el porcentaje/fracción de puntuación junto a cada alternativa Cloze. */
    public static final String CLOZE_FRACTION_LABEL = "color:#c00;";
}