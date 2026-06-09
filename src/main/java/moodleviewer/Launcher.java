/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

/**
 * Clase principal de lanzamiento de la aplicación.
 * Esta clase actúa como un lanzador para inicializar el entorno.
 * Su propósito es evitar las restricciones del sistema de módulos de Java al ejecutar la
 * aplicación desde un archivo empaquetado (.jar) con JavaFX.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}