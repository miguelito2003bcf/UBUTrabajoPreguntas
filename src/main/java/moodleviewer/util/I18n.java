/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utilidad de internacionalización (i18n).
 *
 * PUNTO 30: setLocale no recarga el bundle si el locale ya es el activo,
 * evitando trabajo innecesario cuando el usuario pulsa la misma bandera dos veces.
 */
public class I18n {

    private static ResourceBundle bundle;
    private static Locale         currentLocale;

    static {
        setLocale(Locale.getDefault());
    }

    /**
     * Cambia el idioma activo. Si {@code locale} coincide con el ya cargado,
     * la operación es un no-op para evitar recargas innecesarias.
     *
     * @param locale nuevo idioma deseado.
     */
    public static void setLocale(Locale locale) {
        // PUNTO 30: salir inmediatamente si el locale no cambia
        if (locale != null && locale.equals(currentLocale)) return;
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("i18n/messages",
                locale != null ? locale : Locale.getDefault());
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String get(String key, Object... args) {
        try {
            return MessageFormat.format(bundle.getString(key), args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    /** Devuelve el locale activo actualmente. */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}