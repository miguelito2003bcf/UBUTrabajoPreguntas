package moodleviewer;

import javafx.scene.control.TreeItem;
import moodleviewer.model.Category;

import java.util.ArrayList;
import java.util.List;

public class TreeBuilder {

    public static TreeItem<Category> createTreeItem(Category category) {
        TreeItem<Category> item = new TreeItem<>(category);
        
        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            item.getChildren().add(createTreeItem(sub));
        }
        item.setExpanded(true); 
        return item;
    }

    public static TreeItem<Category> createFilteredTreeItem(Category category, String searchText) {
        boolean matches = category.getName().toLowerCase().contains(searchText);
        TreeItem<Category> item = new TreeItem<>(category);
        boolean hasMatchingChild = false;

        List<Category> sortedSubcategories = new ArrayList<>(category.getSubcategories());
        sortedSubcategories.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Category sub : sortedSubcategories) {
            TreeItem<Category> filteredSub = createFilteredTreeItem(sub, searchText);
            if (filteredSub != null) {
                item.getChildren().add(filteredSub);
                hasMatchingChild = true;
            }
        }

        if (matches || hasMatchingChild) {
            item.setExpanded(true); 
            return item;
        }
        return null;
    }
}