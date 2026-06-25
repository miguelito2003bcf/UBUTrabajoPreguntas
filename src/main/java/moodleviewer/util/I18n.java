/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Clase de utilidad para gestionar la internacionalización (i18n) de la aplicación.
 */
public class I18n {
    private static ResourceBundle bundle;

    static {
        // Por defecto carga el idioma del sistema operativo. 
        // Si no encuentra el archivo (ej. un SO en francés), caerá en el idioma base (messages.properties)
        setLocale(Locale.getDefault());
    }

    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("i18n/messages", locale);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!"; // Retorna la clave entre exclamaciones si falta en el properties
        }
    }

    public static String get(String key, Object... args) {
        try {
            return MessageFormat.format(bundle.getString(key), args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
}