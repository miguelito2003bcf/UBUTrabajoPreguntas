package moodleviewer.parser;

import moodleviewer.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LaTeXExporter {

    public static void exportToLaTeX(Category rootCategory, File file, boolean showAnswers) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            
            String docClass = showAnswers ? "\\documentclass[12pt,a4paper,answers]{exam}\n" : "\\documentclass[12pt,a4paper]{exam}\n";
            writer.write(docClass);
            
            writer.write("\\usepackage[T1]{fontenc}\n"); 
            writer.write("\\usepackage[utf8]{inputenc}\n");
            writer.write("\\usepackage{lmodern}\n"); 
            writer.write("\\usepackage[spanish]{babel}\n");
            writer.write("\\usepackage{amsmath, amsfonts, amssymb}\n");
            writer.write("\\usepackage{enumitem}\n");
            writer.write("\\usepackage{graphicx}\n");
            writer.write("\\usepackage[margin=2cm]{geometry}\n");
            writer.write("\\providecommand{\\pandocbounded}[1]{#1}\n");
            
            // Se ha eliminado adjustbox y setkeys para usar control local por imagen
            writer.write("\\usepackage{longtable, booktabs, array, calc}\n");
            writer.write("\\usepackage{textcomp}\n");
            writer.write("\\providecommand{\\tightlist}{\\setlength{\\itemsep}{0pt}\\setlength{\\parskip}{0pt}}\n");
            writer.write("\\usepackage{xcolor}\n");
            writer.write("\\colorlet{azul}{blue!40!black}\n");
            
            writer.write("\\newenvironment{unaRespuesta}{\\begin{checkboxes}}{\\end{checkboxes}}\n");
            writer.write("\\newenvironment{unaRespuestaEnLinea}{\\begin{oneparcheckboxes}}{\\end{oneparcheckboxes}}\n");
            writer.write("\\newenvironment{variasRespuestas}{\\begin{checkboxes}\\checkboxchar{{\\Large$\\Box$}}}{\\end{checkboxes}}\n");
            writer.write("\\newenvironment{variasRespuestasEnLinea}{\\begin{oneparcheckboxes}\\checkboxchar{{\\Large$\\Box$}}}{\\end{oneparcheckboxes}}\n");
            writer.write("\\newenvironment{emparejar}{\\begin{checkboxes}\\checkboxchar{\\color{azul}{\\huge$\\Box$}}}{\\end{checkboxes}}\n");
            writer.write("\\newcommand{\\cgoCorrectChoice}[1]{\\ifprintanswers\\item[\\fcolorbox{blue}{white}{\\begin{minipage}[c][.7em]{.7em}\\centering\\color{azul}\\bf#1\\end{minipage}}]\\else\\item[\\fcolorbox{blue}{white}{\\begin{minipage}[c][.7em]{.7em}\\centering\\color{white}\\bf#1\\end{minipage}}]\\fi}\n");
            writer.write("\\CorrectChoiceEmphasis{\\bfseries\\color{azul}}\n");
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
            
            LaTeXExportVisitor visitor = new LaTeXExportVisitor(writer, imagesDir, imagesDir.getName(), showAnswers);
            
            writer.write("\\begin{questions}\n\n");
            exportCategoryRecursive(rootCategory, writer, visitor, 1);
            writer.write("\\end{questions}\n\n");
            
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