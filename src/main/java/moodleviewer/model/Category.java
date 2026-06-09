/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase creada que representa una categoría del banco de preguntas de Moodle.
 */
public class Category {
	
    private String name;
    private List<Question> questions;
    private List<Category> subcategories;

    /**
     * Construye una nueva categoría vacía con el nombre indicado.
     * 
     * @param name nombre de la categoría.
     */
    public Category(String name) {
        this.name = name;
        this.questions = new ArrayList<>();
        this.subcategories = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<Category> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Category> subcategories) {
        this.subcategories = subcategories;
    }

    public void addQuestion(Question question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(question);
    }
    
    public void addSubcategory(Category category) {
        if (this.subcategories == null) {
            this.subcategories = new ArrayList<>();
        }
        this.subcategories.add(category);
    }

    @Override
    public String toString() {
        return name;
    }
}