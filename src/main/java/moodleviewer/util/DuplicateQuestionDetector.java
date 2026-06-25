/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.util;

import moodleviewer.model.Category;
import moodleviewer.model.Question;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analiza el banco de preguntas en busca de posibles duplicados, ya sea por nombre idéntico
 * o por enunciado equivalente. A diferencia de {@link QuestionValidator}, esto no es un error
 * de integridad que bloquee la exportación: es un aviso de calidad orientado a que el usuario
 * pueda limpiar bancos de preguntas que crecen por copia/pega entre cursos o categorías.
 */
public final class DuplicateQuestionDetector {

    private DuplicateQuestionDetector() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }

    /** Nombre de la categoría raíz implícita, que no debe aparecer en las rutas mostradas al usuario. */
    private static final String ROOT_CATEGORY_NAME = "Banco de Preguntas";

    /**
     * Una localización concreta (categoría + pregunta) donde aparece una de las preguntas
     * implicadas en un grupo de duplicados.
     *
     * @param categoryPath ruta legible de la categoría que contiene la pregunta.
     * @param question     la propia pregunta encontrada en esa categoría.
     */
    public record DuplicateLocation(String categoryPath, Question question) {}

    /**
     * Representa un grupo de dos o más preguntas que se consideran duplicadas entre sí.
     *
     * @param matchType  tipo de coincidencia detectada (por nombre o por enunciado).
     * @param locations  todas las localizaciones (categoría + pregunta) que forman el grupo,
     *                   en el orden en que se encontraron al recorrer el árbol.
     */
    public record DuplicateGroup(MatchType matchType, List<DuplicateLocation> locations) {}

    /**
     * Tipo de coincidencia que originó el agrupamiento de duplicados.
     */
    public enum MatchType {
        /** Mismo nombre de pregunta (ignorando mayúsculas/minúsculas y espacios en los extremos). */
        SAME_NAME,
        /** Mismo enunciado (comparando el texto plano, sin etiquetas HTML ni diferencias de formato). */
        SAME_TEXT
    }

    /**
     * Recorre el banco completo y devuelve todos los grupos de preguntas duplicadas detectadas,
     * tanto por nombre como por enunciado. Una misma pregunta puede aparecer en como máximo un
     * grupo de cada tipo (no se mezclan ambos criterios en un mismo grupo, para que el motivo
     * de la coincidencia sea siempre inequívoco al mostrarlo al usuario).
     *
     * @param rootCategory categoría raíz (o subárbol) a analizar.
     * @return lista de grupos de duplicados; vacía si no se ha encontrado ninguno.
     */
    public static List<DuplicateGroup> findDuplicates(Category rootCategory) {
        List<DuplicateLocation> allLocations = new ArrayList<>();
        collectLocations(rootCategory, "", allLocations);

        List<DuplicateGroup> groups = new ArrayList<>();
        groups.addAll(groupBy(allLocations, MatchType.SAME_NAME, loc -> normalizeName(loc.question().getName())));
        groups.addAll(groupBy(allLocations, MatchType.SAME_TEXT, loc -> normalizeText(loc.question().getText())));
        return groups;
    }

    private static void collectLocations(Category cat, String parentPath, List<DuplicateLocation> out) {
        if (cat == null) return;

        String currentPath = parentPath;
        if (!ROOT_CATEGORY_NAME.equals(cat.getName())) {
            currentPath = parentPath.isEmpty() ? cat.getName() : parentPath + " > " + cat.getName();
        }

        for (Question q : cat.getQuestions()) {
            out.add(new DuplicateLocation(currentPath, q));
        }
        for (Category sub : cat.getSubcategories()) {
            collectLocations(sub, currentPath, out);
        }
    }

    /**
     * Agrupa las localizaciones según una clave normalizada, descartando las claves vacías
     * (preguntas sin nombre o sin enunciado, que no deben considerarse "duplicadas entre sí"
     * solo por compartir ese vacío) y los grupos de un único elemento (no son duplicados).
     */
    private static List<DuplicateGroup> groupBy(List<DuplicateLocation> locations, MatchType type, java.util.function.Function<DuplicateLocation, String> keyFn) {
        Map<String, List<DuplicateLocation>> byKey = new HashMap<>();
        for (DuplicateLocation loc : locations) {
            String key = keyFn.apply(loc);
            if (key == null || key.isEmpty()) continue;
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(loc);
        }

        List<DuplicateGroup> result = new ArrayList<>();
        for (List<DuplicateLocation> group : byKey.values()) {
            if (group.size() > 1) {
                result.add(new DuplicateGroup(type, group));
            }
        }
        return result;
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    /**
     * Normaliza el enunciado a texto plano (sin HTML) para que dos preguntas con el mismo
     * contenido pero distinto marcado (espacios, saltos de línea, <p> vs <div>, etc.) se
     * sigan considerando duplicadas. Las imágenes embebidas en Base64 no afectan a esta
     * comparación, ya que JSoup descarta los atributos al extraer el texto.
     */
    private static String normalizeText(String html) {
        if (html == null) return "";
        String plain = Jsoup.parse(html).text();
        return plain.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}