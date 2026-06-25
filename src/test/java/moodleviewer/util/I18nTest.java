/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de la utilidad de Internacionalización (I18n)")
public class I18nTest {

    @Test
    @DisplayName("Debe cargar correctamente las traducciones en Español")
    public void testSpanishTranslations() {
        I18n.setLocale(Locale.of("es", "ES"));
        
        // Comprobamos una traducción simple
        assertEquals("Verdadero / Falso", I18n.get("qtype.truefalse"), "La traducción simple en español falla.");
        
        // Comprobamos una traducción con parámetros insertados ({0})
        assertEquals("Archivo actual: examen.xml", I18n.get("main.lbl.currentFile", "examen.xml"), "La inyección de parámetros en español falla.");
    }

    @Test
    @DisplayName("Debe cargar correctamente las traducciones en Inglés")
    public void testEnglishTranslations() {
        I18n.setLocale(Locale.of("en", "GB"));
        
        // Comprobamos la misma clave pero en inglés
        assertEquals("True / False", I18n.get("qtype.truefalse"), "La traducción simple en inglés falla.");
        
        // Comprobamos una traducción con parámetros insertados ({0})
        assertEquals("Current file: exam.xml", I18n.get("main.lbl.currentFile", "exam.xml"), "La inyección de parámetros en inglés falla.");
    }

    @Test
    @DisplayName("Debe devolver la clave entre exclamaciones si la traducción no existe")
    public void testMissingKey() {
        // Si pedimos una clave que no existe en el archivo properties, debe devolver !clave!
        String missingKey = "clave.falsa.inexistente";
        assertEquals("!" + missingKey + "!", I18n.get(missingKey), "El manejo de claves no encontradas ha fallado.");
    }
}