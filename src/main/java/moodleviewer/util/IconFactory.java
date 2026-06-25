/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.util;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Clase de utilidad que centraliza la creación de iconos vectoriales (Ikonli + FontAwesome5)
 * usados en los botones y menús de la aplicación. Sustituye al antiguo uso de emojis Unicode
 * directamente en el texto de los controles, que se veían borrosos o inconsistentes entre
 * sistemas operativos y monitores de alta resolución (4K).
 */
public final class IconFactory {

    /** Tamaño de icono por defecto para botones de la barra de herramientas. */
    public static final int DEFAULT_SIZE = 16;

    private IconFactory() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }

    /**
     * Crea un icono con el tamaño por defecto de la aplicación.
     *
     * @param code código del icono dentro del set FontAwesome5 Solid.
     * @return un nuevo FontIcon listo para usar como gráfico de un control.
     */
    public static FontIcon of(FontAwesomeSolid code) {
        return of(code, DEFAULT_SIZE);
    }

    /**
     * Crea un icono con un tamaño concreto en píxeles.
     *
     * @param code código del icono dentro del set FontAwesome5 Solid.
     * @param size tamaño en píxeles.
     * @return un nuevo FontIcon listo para usar como gráfico de un control.
     */
    public static FontIcon of(FontAwesomeSolid code, int size) {
        FontIcon icon = new FontIcon(code);
        icon.setIconSize(size);
        return icon;
    }

    /**
     * Crea un icono con un tamaño y color concretos.
     *
     * @param code código del icono dentro del set FontAwesome5 Solid.
     * @param size tamaño en píxeles.
     * @param cssColor color en formato CSS (ej. "white", "#0f6cbf").
     * @return un nuevo FontIcon listo para usar como gráfico de un control.
     */
    public static FontIcon of(FontAwesomeSolid code, int size, String cssColor) {
        FontIcon icon = of(code, size);
        icon.setIconColor(javafx.scene.paint.Color.web(cssColor));
        return icon;
    }
}