package moodleviewer.model;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;
    private List<Question> questions;
    private List<Category> subcategories; // NUEVO: Lista para guardar las subcategorías

    public Category(String name) {
        this.name = name;
        this.questions = new ArrayList<>();
        this.subcategories = new ArrayList<>(); // Inicializamos la lista vacía
    }

    public String getName() {
        return name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public List<Category> getSubcategories() {
        return subcategories;
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
    }

    public void addSubcategory(Category subcategory) {
        this.subcategories.add(subcategory);
    }

    @Override
    public String toString() {
        return name;
    }
}