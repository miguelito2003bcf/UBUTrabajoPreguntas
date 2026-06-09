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
 * Clase creada de utilidad que centraliza todas las constantes relacionadas con la estructura HTML, los estilos CSS y la configuración de scripts.
 * Su propósito es separar la lógica de presentación visual del modelo de datos.
 */
public final class HtmlConstants {

    //Estructura principal.
    public static final String HTML_BODY = "padding: 20px; min-height: 100vh; box-sizing: border-box;";
    public static final String HTML_CARD = "background-color: #fff; border: 1px solid #dee2e6; border-radius: 4px; padding: 25px; box-shadow: 0 1px 3px rgba(0,0,0,.05);";
    public static final String HEADER_FLEX = "border-bottom: 1px solid #eee; padding-bottom: 15px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;";
    
    //Tipografía y etiquetas.
    public static final String TITLE = "color: #1177d1; font-size: 1.2rem;";
    public static final String BADGE = "background-color: #6c757d; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;";
    public static final String TEXT_BASE = "font-size: 15px; margin-bottom: 25px; color: #212529; line-height: 1.5;";
    public static final String LABEL_BOLD = "margin-bottom: 15px; font-size: 14px; font-weight: bold; color: #333;";
    
    //Feedback y alertas.
    public static final String FEEDBACK_GENERAL = "margin-top: 25px; padding: 15px; background-color: #d1ecf1; border: 1px solid #bee5eb; border-radius: 4px; color: #0c5460; font-size: 14px;";
    public static final String FEEDBACK_WARNING = "margin-top: 30px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; font-size: 14px; color: #8a6d3b;";
    
    //Formularios y disposición.
    public static final String FLEX_ROW = "display: flex; align-items: center; margin-bottom: 20px; font-size: 15px; color: #212529;";
    public static final String FLEX_ROW_START = "display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 15px; color: #212529;";
    public static final String INPUT_BASE = "padding: 8px; border: 1px solid #ccc; border-radius: 4px; background-color: #f8f9fa;";
    public static final String TABLE_LAYOUT = "width: 100%; border-collapse: separate; border-spacing: 0 15px; font-size: 15px; color: #212529;";
    
    //Específicos de Cloze.
    public static final String CLOZE_CODE_BLOCK = "padding: 15px; background-color: #ffffff; border: 1px dashed #adb5bd; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;";
    public static final String CLOZE_RENDER_BLOCK = "padding: 15px; background-color: #fcfcfc; border: 1px solid #dee2e6; border-radius: 4px; font-size: 15px; color: #212529; line-height: 1.6;";
    public static final String CLOZE_HIGHLIGHT = "background-color: #e9ecef; color: #0056b3; font-family: 'Courier New', monospace; padding: 2px 6px; border-radius: 4px; font-weight: bold;";

    /**
     * Constructor privado para evitar la instanciación de esta clase de utilidad.
     */
    private HtmlConstants() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }
}