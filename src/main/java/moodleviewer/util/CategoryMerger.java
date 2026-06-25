/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.util;

import moodleviewer.model.Category;

/**
 * Combina el contenido de un banco de preguntas importado dentro de un banco ya existente,
 * pensado para la apertura unificada de bancos (ver {@code FileManager.openOrMergeBank}), que
 * ofrece "Combinar" como alternativa a sustituir el banco actual.
 *
 * Estrategia de fusión: TODAS las subcategorías de primer nivel del árbol importado se añaden
 * siempre como subcategorías nuevas directamente bajo la raíz del árbol destino, tal cual,
 * sin comprobar si ya existe alguna con el mismo nombre en ningún nivel. Esto es deliberado:
 * no se intenta adivinar coincidencias de categorías por nombre (dos categorías "Tema 1" de
 * bancos distintos pueden no tener absolutamente nada que ver entre sí), así que cada
 * importación queda siempre como una rama nueva e independiente colgando de la raíz, fácil
 * de identificar y, si se quiere, de revisar o eliminar después sin afectar al resto del banco.
 * Las preguntas que cuelguen directamente de la raíz del árbol importado (sin categoría propia)
 * se añaden directamente a las preguntas de la raíz destino.
 *
 * Esta fusión no intenta limpiar duplicados de preguntas entre el banco destino y el importado:
 * para eso, el usuario puede ejecutar el análisis de {@link DuplicateQuestionDetector} después
 * de fusionar.
 */
public final class CategoryMerger {

    private CategoryMerger() {
        throw new UnsupportedOperationException("Clase de utilidad");
    }

    /**
     * Fusiona el árbol importado dentro del árbol destino, modificando este último in-situ.
     * Todas las subcategorías de primer nivel del árbol importado se añaden siempre como
     * subcategorías nuevas bajo la raíz destino, sin comprobar coincidencias de nombre.
     *
     * @param destinationRoot raíz del banco actualmente cargado, que recibirá el contenido nuevo.
     * @param importedRoot    raíz del banco recién importado (de un fichero XML o GIFT), cuyo
     *                        contenido se añadirá al destino. No se modifica.
     * @return el número total de preguntas añadidas al banco destino (útil para informar al
     *         usuario del resultado de la fusión).
     */
    public static int merge(Category destinationRoot, Category importedRoot) {
        if (destinationRoot == null || importedRoot == null) return 0;

        int addedCount = importedRoot.getQuestions().size();
        destinationRoot.getQuestions().addAll(importedRoot.getQuestions());

        for (Category importedSub : importedRoot.getSubcategories()) {
            destinationRoot.addSubcategory(importedSub);
            addedCount += countQuestionsRecursive(importedSub);
        }

        return addedCount;
    }

    private static int countQuestionsRecursive(Category category) {
        int count = category.getQuestions().size();
        for (Category sub : category.getSubcategories()) {
            count += countQuestionsRecursive(sub);
        }
        return count;
    }
}