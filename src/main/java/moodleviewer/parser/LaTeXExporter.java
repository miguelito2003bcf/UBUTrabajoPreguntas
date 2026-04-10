package moodleviewer.parser;

import moodleviewer.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LaTeXExporter {

    public static void exportToLaTeX(Category rootCategory, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.write("\\documentclass[12pt,a4paper]{article}\n");
            writer.write("\\usepackage[utf8]{inputenc}\n");
            writer.write("\\usepackage[spanish]{babel}\n");
            writer.write("\\usepackage{amsmath, amsfonts, amssymb}\n");
            writer.write("\\usepackage{enumitem}\n");
            writer.write("\\usepackage{graphicx}\n"); 
            writer.write("\\usepackage[margin=2cm]{geometry}\n\n");
            writer.write("\\begin{document}\n\n");
            writer.write("\\title{Banco de Preguntas}\n");
            writer.write("\\date{\\today}\n");
            writer.write("\\maketitle\n\n");

            File parentDir = file.getParentFile() != null ? file.getParentFile() : new File(".");
            String fileNameWithoutExt = file.getName().replaceFirst("[.][^.]+$", "");
            File imagesDir = new File(parentDir, fileNameWithoutExt + "_imagenes");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs(); 
            }
            
            LaTeXExportVisitor visitor = new LaTeXExportVisitor(writer, imagesDir, imagesDir.getName());
            exportCategoryRecursive(rootCategory, writer, visitor, 1);
            writer.write("\\end{document}\n");
        }
    }

    private static void exportCategoryRecursive(Category category, BufferedWriter writer, LaTeXExportVisitor visitor, int depth) throws IOException {
        if (!category.getQuestions().isEmpty() && !category.getName().equals("Banco de Preguntas")) {
            if (depth == 1) {
                writer.write("\\section*{" + LaTeXExportVisitor.escapeLatex(category.getName()) + "}\n\n");
            } else {
                writer.write("\\subsection*{" + LaTeXExportVisitor.escapeLatex(category.getName()) + "}\n\n");
            }
        }
        for (Question q : category.getQuestions()) {
            q.accept(visitor); 
        }
        
        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            exportCategoryRecursive(sub, writer, visitor, depth + 1);
        }
    }
}