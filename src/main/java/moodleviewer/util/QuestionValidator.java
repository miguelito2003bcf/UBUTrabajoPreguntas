/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.util;

import moodleviewer.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de validación (Linter) pre-exportación.
 * Revisa reglas de negocio, integridad de datos y sintaxis de Moodle.
 */
public class QuestionValidator {

    /** Nombre de la categoría raíz implícita, que no debe aparecer en las rutas mostradas al usuario. */
    private static final String ROOT_CATEGORY_NAME = "Banco de Preguntas";

    /**
     * Representa un único aviso de validación, con sus datos de localización separados
     * (en vez de un único String ya formateado), para poder presentarlos en columnas
     * independientes en una tabla: ruta de categoría, nombre de la pregunta, tipo y mensaje.
     *
     * @param categoryPath  ruta legible de la categoría (ej. "Tema 1 > Cinemática"), o cadena
     *                      vacía si se desconoce o la pregunta está en la raíz del banco.
     * @param questionName  nombre de la pregunta, o un texto descriptivo entre paréntesis
     *                      ("(sin nombre)") si la propia pregunta carece de nombre.
     * @param questionType  tipo de Moodle de la pregunta (ej. "multichoice", "essay").
     * @param message       descripción concreta del problema detectado.
     */
    public record ValidationWarning(String categoryPath, String questionName, String questionType, String message) {

        /**
         * Representación de una sola línea, usada como respaldo en contextos que aún
         * esperen un texto plano (por ejemplo, registros de log).
         */
        @Override
        public String toString() {
            String location = categoryPath.isEmpty() ? "" : "[" + categoryPath + "] ";
            return "• " + location + questionName + " (" + questionType + "): " + message;
        }
    }

    /**
     * Recorre el banco de preguntas completo y devuelve una lista con todos los avisos,
     * identificando junto a cada uno la ruta de categoría y la pregunta donde se ha detectado.
     * Sobrecarga de compatibilidad que no comprueba la presencia de imágenes embebidas.
     */
    public static List<ValidationWarning> validateBank(Category rootCategory) {
        return validateBank(rootCategory, false);
    }

    /**
     * Recorre el banco de preguntas completo y devuelve una lista con todos los avisos.
     *
     * @param rootCategory categoría raíz (o subárbol filtrado) a recorrer.
     * @param checkUnsupportedImages true si el formato de destino no admite imágenes embebidas
     *                                (por ejemplo, una exportación GIFT en texto plano), en cuyo
     *                                caso se añade un aviso por cada pregunta que contenga
     *                                imágenes ({@code @@PLUGINFILE@@} o ficheros adjuntos).
     */
    public static List<ValidationWarning> validateBank(Category rootCategory, boolean checkUnsupportedImages) {
        List<ValidationWarning> allWarnings = new ArrayList<>();
        traverseAndValidate(rootCategory, "", allWarnings, checkUnsupportedImages);
        return allWarnings;
    }

    private static void traverseAndValidate(Category cat, String parentPath, List<ValidationWarning> warnings, boolean checkUnsupportedImages) {
        if (cat == null) return;

        String currentPath = parentPath;
        if (!ROOT_CATEGORY_NAME.equals(cat.getName())) {
            currentPath = parentPath.isEmpty() ? cat.getName() : parentPath + " > " + cat.getName();
        }

        for (Question q : cat.getQuestions()) {
            warnings.addAll(validate(q, currentPath, checkUnsupportedImages));
        }
        for (Category sub : cat.getSubcategories()) {
            traverseAndValidate(sub, currentPath, warnings, checkUnsupportedImages);
        }
    }

    /**
     * Valida una pregunta individual contra las reglas estructurales de Moodle, sin contexto
     * de categoría. Mantenido por compatibilidad con quien solo quiera validar una pregunta
     * suelta (por ejemplo, al guardar desde el editor); la ruta de categoría quedará vacía.
     */
    public static List<ValidationWarning> validate(Question q) {
        return validate(q, "", false);
    }

    /**
     * Valida una pregunta individual contra las reglas estructurales de Moodle.
     *
     * @param q pregunta a validar.
     * @param categoryPath ruta legible de la categoría a la que pertenece (ej. "Tema 1 > Cinemática"),
     *                     o cadena vacía/null si se desconoce.
     */
    public static List<ValidationWarning> validate(Question q, String categoryPath) {
        return validate(q, categoryPath, false);
    }

    /**
     * Valida una pregunta individual contra las reglas estructurales de Moodle.
     *
     * @param q pregunta a validar.
     * @param categoryPath ruta legible de la categoría a la que pertenece (ej. "Tema 1 > Cinemática"),
     *                     o cadena vacía/null si se desconoce.
     * @param checkUnsupportedImages true si debe avisarse cuando la pregunta contenga imágenes
     *                                embebidas que el formato de destino no podrá representar.
     */
    public static List<ValidationWarning> validate(Question q, String categoryPath, boolean checkUnsupportedImages) {
        List<ValidationWarning> warnings = new ArrayList<>();
        String path = categoryPath != null ? categoryPath : "";
        boolean hasName = q.getName() != null && !q.getName().trim().isEmpty();
        String displayName = hasName ? q.getName() : "(sin nombre)";
        String qType = q.getType() != null ? q.getType() : "?";

        // REGLA 1: Falta de nombre o enunciado
        if (!hasName) {
            warnings.add(new ValidationWarning(path, displayName, qType, "Pregunta sin nombre detectada."));
        }
        if (q.getText() == null || q.getText().trim().isEmpty()) {
            warnings.add(new ValidationWarning(path, displayName, qType, "No tiene enunciado (texto principal vacío)."));
        }

        // REGLA 2: Lógica de respuestas correctas asignadas y límites matemáticos
        if (q instanceof MultichoiceQuestion mc) {
            double sumFractions = 0.0;
            boolean hasCorrectAnswer = false;
            for (Answer a : mc.getAnswers()) {
                try {
                    double fraction = Double.parseDouble(a.getFraction());
                    if (fraction > 0) {
                        sumFractions += fraction;
                        hasCorrectAnswer = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (!hasCorrectAnswer) {
                warnings.add(new ValidationWarning(path, displayName, qType, "Opción Múltiple sin ninguna respuesta correcta (> 0%)."));
            }
            if (!mc.isSingleAnswer() && hasCorrectAnswer && Math.abs(sumFractions - 100.0) > 0.5) {
                warnings.add(new ValidationWarning(path, displayName, qType,
                        "La suma de las opciones correctas debe ser 100% (Actual: " + String.format("%.1f", sumFractions) + "%)."));
            }
        } 
        else if (q instanceof ShortAnswerQuestion sa) {
            boolean hasCorrect = sa.getAnswers().stream().anyMatch(a -> {
                try { return Double.parseDouble(a.getFraction()) > 0; } catch (Exception e) { return false; }
            });
            if (!hasCorrect) {
                warnings.add(new ValidationWarning(path, displayName, qType, "Respuesta Corta sin ninguna opción validada como correcta."));
            }
        }
        else if (q instanceof MatchingQuestion mq) {
            if (mq.getPairs() == null || mq.getPairs().size() < 2) {
                warnings.add(new ValidationWarning(path, displayName, qType, "Emparejamiento necesita al menos 2 pares para ser válido."));
            }
        }
        else if (q instanceof NumericalQuestion nq) {
            try {
                double tolerance = Double.parseDouble(nq.getTolerance());
                if (tolerance < 0) {
                    warnings.add(new ValidationWarning(path, displayName, qType, "La tolerancia de error no puede ser negativa."));
                }
            } catch (NumberFormatException e) {
                if (nq.getTolerance() != null && !nq.getTolerance().isEmpty()) {
                    warnings.add(new ValidationWarning(path, displayName, qType, "La tolerancia numérica contiene caracteres inválidos."));
                }
            }
        }
        
        // REGLA 3: Sintaxis Cloze desbalanceada o vacía
        else if (q instanceof ClozeQuestion) {
            String text = q.getText() != null ? q.getText() : "";
            long openBraces = text.chars().filter(ch -> ch == '{').count();
            long closeBraces = text.chars().filter(ch -> ch == '}').count();
            
            if (openBraces == 0) {
                warnings.add(new ValidationWarning(path, displayName, qType, "Es tipo Cloze pero no contiene ningún hueco o subpregunta '{...}'."));
            } else if (openBraces != closeBraces) {
                warnings.add(new ValidationWarning(path, displayName, qType, "Sintaxis Cloze desbalanceada (Las llaves '{' y '}' no coinciden en cantidad)."));
            } else {
                // Validación extra: Verificar si hay un formato de puntos y tipo válido, ej: {1:SA:=Respuesta}
                Matcher matcher = Pattern.compile("\\{\\d*:[A-Z_]+:.*?\\}").matcher(text);
                if (!matcher.find() && openBraces > 0) {
                    warnings.add(new ValidationWarning(path, displayName, qType, "Las llaves están presentes pero la sintaxis interna Cloze parece malformada."));
                }
            }
        }

        // REGLA 4: Imágenes embebidas no soportadas por el formato de destino (p.ej. GIFT en texto plano)
        if (checkUnsupportedImages && questionContainsImages(q)) {
            warnings.add(new ValidationWarning(path, displayName, qType,
                    "Contiene imágenes embebidas que pueden no representarse correctamente en este formato de exportación."));
        }

        return warnings;
    }

    /**
     * Comprueba si una pregunta contiene imágenes embebidas, bien como ficheros adjuntos
     * (@@PLUGINFILE@@ ya resueltos en archivos) o referenciadas directamente en su HTML
     * (incluyendo el texto de sus respuestas, cuando el tipo de pregunta las tiene). Expuesto
     * como público porque también lo reutiliza {@code DashboardDialog} para mostrar cuántas
     * preguntas del banco contienen imágenes, fuera del contexto de validación pre-exportación.
     */
    public static boolean questionContainsImages(Question q) {
        if (q.getFiles() != null && !q.getFiles().isEmpty()) {
            return true;
        }
        if (containsImageTag(q.getText())) {
            return true;
        }

        if (q instanceof MultichoiceQuestion mc) {
            for (Answer a : mc.getAnswers()) {
                if (containsImageTag(a.getText())) return true;
            }
        } else if (q instanceof ShortAnswerQuestion sa) {
            for (Answer a : sa.getAnswers()) {
                if (containsImageTag(a.getText())) return true;
            }
        } else if (q instanceof NumericalQuestion nq) {
            if (containsImageTag(nq.getAnswer().getText())) return true;
        } else if (q instanceof MatchingQuestion mq) {
            for (MatchingPair p : mq.getPairs()) {
                if (containsImageTag(p.getQuestionText()) || containsImageTag(p.getAnswerText())) return true;
            }
        }
        return false;
    }

    private static boolean containsImageTag(String html) {
        return html != null && (html.contains("<img") || html.contains("@@PLUGINFILE@@") || html.contains("data:image"));
    }
}