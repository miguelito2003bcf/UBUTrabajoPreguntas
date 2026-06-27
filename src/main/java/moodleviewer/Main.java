/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 *
 * Este archivo forma parte de Gestión del banco de preguntas de Moodle.
 *
 * Se distribuye bajo la licencia MIT. Consulta el archivo LICENSE
 * en la raíz del proyecto para más detalles.
 */

package moodleviewer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import moodleviewer.model.Category;
import moodleviewer.model.Question;
import moodleviewer.model.ClozeQuestion;
import moodleviewer.parser.GIFTParser;
import moodleviewer.util.CategoryMerger;
import moodleviewer.util.I18n;
import moodleviewer.util.IconFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import moodleviewer.events.AppEvents;
import moodleviewer.events.EventBus;
import moodleviewer.commands.CommandManager;
import org.controlsfx.control.textfield.CustomTextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Clase principal de la aplicación.
 *
 * Cambios respecto a la versión anterior:
 * - TableManager y TreeManager son ahora instancias (punto I).
 * - El constructor de TableManager recibe un Supplier<Category> para el submenú "Mover a" (punto B).
 * - configure() de TableManager recibe la Label del contador (punto D).
 * - Nuevo botón "Importar GIFT desde portapapeles" (punto C).
 * - LayoutManager recibe la counterLabel para integrarla bajo la tabla (punto D).
 * - CORRECCIÓN: Uso de new Locale() compatible con Java 17.
 */
public class Main extends Application {

    // -------------------------------------------------------------------------
    //  Componentes visuales
    // -------------------------------------------------------------------------

    private final TreeView<Category>   categoryTreeView   = new TreeView<>();
    private final TableView<Question>  questionTableView  = new TableView<>();
    private final WebView              detailsWebView     = new WebView();

    // Punto D: etiqueta de contador bajo la tabla
    private final Label counterLabel = new Label();

    private final FileManager fileManager = new FileManager();

    // Punto I: gestores como instancias
    private final TreeManager  treeManager  = new TreeManager();
    private       TableManager tableManager;   // se crea en initBasicComponents tras tener el supplier

    private final Button openBankButton    = new Button(I18n.get("main.btn.openBank"));
    private final Button saveButton        = new Button(I18n.get("main.btn.saveXml"));
    private final Button addQuestionButton = new Button(I18n.get("main.btn.addQuestion"));
    private final Button exportLatexButton = new Button(I18n.get("main.btn.saveLatex"));
    // Punto 3: nombre refleja que siempre abre FileChooser
    private final Button exportGiftButton  = new Button(I18n.get("main.btn.exportGift"));
    private final Button addCategoryButton = new Button(I18n.get("main.btn.addCategory"));
    private final Button statsButton       = new Button(I18n.get("main.btn.stats"));
    private final Button duplicatesButton  = new Button(I18n.get("main.btn.duplicates"));
    // Punto C: importar GIFT desde portapapeles
    private final Button importClipboardButton = new Button(I18n.get("main.btn.importClipboard"));
    private final Button undoButton        = new Button();
    private final Button redoButton        = new Button();
    private final CheckBox clozeToggle     = new CheckBox(I18n.get("main.toggle.cloze"));

    private final CustomTextField searchCategoryField = new CustomTextField();
    private final CustomTextField searchQuestionField = new CustomTextField();
    private final ComboBox<String> searchCriteriaCombo = new ComboBox<>();
    private final MenuButton typeFilterMenu = new MenuButton(I18n.get("main.filter.type"));

    private Category currentRootCategory;
    private final Label fileNameLabel = new Label(I18n.get("main.lbl.noFile"));
    private final Set<String> activeTypeFilters = new HashSet<>();
    private final HBox languageBox = new HBox(8);
    private File loadedFile = null;

    // -------------------------------------------------------------------------
    //  Ciclo de vida JavaFX
    // -------------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(I18n.get("main.window.title"));

        initBasicComponents(primaryStage);

        // Punto I: instancias, no estáticos
        treeManager.configure(categoryTreeView);
        tableManager.configure(questionTableView, counterLabel);

        Scene scene = LayoutManager.buildScene(this);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(event -> {
            if (!confirmDiscardUnsavedChanges(primaryStage)) event.consume();
        });
        primaryStage.show();
    }

    // -------------------------------------------------------------------------
    //  Inicialización
    // -------------------------------------------------------------------------

    private void initBasicComponents(Stage stage) {

        // Punto I: TableManager instanciable con supplier de la raíz actual
        tableManager = new TableManager(() -> currentRootCategory);

        // Buscador de categorías
        searchCategoryField.setPromptText(I18n.get("main.search.category"));
        searchCategoryField.textProperty().addListener((obs, o, n) -> applyCategoryFilter(n));
        searchCategoryField.setLeft(IconFactory.of(FontAwesomeSolid.SEARCH, 13, "#868e96"));
        Label clearCat = makeClearButton(searchCategoryField);
        searchCategoryField.setRight(clearCat);

        // Buscador de preguntas
        searchQuestionField.setPromptText(I18n.get("main.search.question"));
        searchQuestionField.textProperty().addListener((obs, o, n) -> refreshQuestionTable());
        searchQuestionField.setLeft(IconFactory.of(FontAwesomeSolid.SEARCH, 13, "#868e96"));
        Label clearQ = makeClearButton(searchQuestionField);
        searchQuestionField.setRight(clearQ);
        HBox.setHgrow(searchQuestionField, Priority.ALWAYS);

        searchCriteriaCombo.setItems(FXCollections.observableArrayList(
                I18n.get("main.search.byName"), I18n.get("main.search.byText")));
        searchCriteriaCombo.getSelectionModel().selectFirst();
        searchCriteriaCombo.valueProperty().addListener((obs, o, n) -> refreshQuestionTable());

        typeFilterMenu.setDisable(true);

        // Punto D: estilo del contador
        counterLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-padding: 2 10 4 10;");

        fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");

        // --- Botón abrir ---
        openBankButton.setId("btn-primary");
        openBankButton.setGraphic(IconFactory.of(FontAwesomeSolid.FOLDER_OPEN, 14, "white"));
        openBankButton.setGraphicTextGap(8);
        openBankButton.setOnAction(e -> handleOpenBank(stage));

        // --- Botón guardar XML ---
        saveButton.setDisable(true);
        saveButton.setTooltip(new Tooltip(I18n.get("main.btn.saveXml.tooltip.new")));
        saveButton.setOnAction(e -> {
            Category pre = getSelectedTreeCategory();
            fileManager.saveMoodleXML(stage, currentRootCategory, pre, loadedFile)
                    .ifPresent(f -> {
                        loadedFile = f;
                        updateFileNameLabel();
                        saveButton.setTooltip(new Tooltip(
                                I18n.get("main.btn.saveXml.tooltip.overwrite", f.getName())));
                    });
        });

        // --- Botón añadir pregunta ---
        addQuestionButton.setDisable(true);
        addQuestionButton.setOnAction(e -> showAddQuestionDialog());
        addQuestionButton.setId("btn-success");
        addQuestionButton.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 14, "white"));
        addQuestionButton.setGraphicTextGap(8);

        // --- Botón exportar LaTeX ---
        exportLatexButton.setDisable(true);
        exportLatexButton.setOnAction(e ->
                fileManager.exportLaTeX(stage, currentRootCategory, getSelectedTreeCategory()));

        // --- Botón exportar GIFT ---
        exportGiftButton.setDisable(true);
        exportGiftButton.setOnAction(e -> {
            if (currentRootCategory != null)
                fileManager.exportGIFT(stage, currentRootCategory, getSelectedTreeCategory());
        });

        // --- Botón añadir categoría ---
        addCategoryButton.setDisable(true);
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());
        addCategoryButton.setId("btn-success");
        addCategoryButton.setGraphic(IconFactory.of(FontAwesomeSolid.PLUS, 14, "white"));
        addCategoryButton.setGraphicTextGap(8);

        // --- Estadísticas ---
        statsButton.setDisable(true);
        statsButton.setGraphic(IconFactory.of(FontAwesomeSolid.CHART_BAR, 14, "#495057"));
        statsButton.setGraphicTextGap(8);
        statsButton.setOnAction(e -> {
            if (currentRootCategory != null)
                new DashboardDialog(currentRootCategory).showAndWait();
        });

        // --- Duplicados ---
        duplicatesButton.setDisable(true);
        duplicatesButton.setGraphic(IconFactory.of(FontAwesomeSolid.CLONE, 14, "#495057"));
        duplicatesButton.setGraphicTextGap(8);
        duplicatesButton.setOnAction(e -> {
            if (currentRootCategory != null)
                DuplicateQuestionsDialog.showDuplicates(currentRootCategory);
        });

        // --- Punto C: Importar GIFT desde portapapeles ---
        importClipboardButton.setDisable(true);
        importClipboardButton.setGraphic(IconFactory.of(FontAwesomeSolid.PASTE, 14, "#495057"));
        importClipboardButton.setGraphicTextGap(8);
        importClipboardButton.setTooltip(new Tooltip(I18n.get("main.btn.importClipboard.tooltip")));
        importClipboardButton.setOnAction(e -> handleImportFromClipboard());

        // --- Deshacer / Rehacer ---
        undoButton.setGraphic(IconFactory.of(FontAwesomeSolid.UNDO, 16, "#495057"));
        undoButton.setTooltip(new Tooltip(I18n.get("main.btn.undo.tooltip")));
        undoButton.setStyle("-fx-padding: 0 10 0 10;");
        undoButton.setDisable(true);
        undoButton.setOnAction(e -> CommandManager.getInstance().undo());

        redoButton.setGraphic(IconFactory.of(FontAwesomeSolid.REDO, 16, "#495057"));
        redoButton.setTooltip(new Tooltip(I18n.get("main.btn.redo.tooltip")));
        redoButton.setStyle("-fx-padding: 0 10 0 10;");
        redoButton.setDisable(true);
        redoButton.setOnAction(e -> CommandManager.getInstance().redo());

        EventBus.getInstance().subscribe(AppEvents.UndoRedoStateChangedEvent.class, event -> {
            undoButton.setDisable(!event.canUndo());
            redoButton.setDisable(!event.canRedo());
            updateFileNameLabel();
        });

        // --- Toggle Cloze ---
        clozeToggle.setVisible(false);
        clozeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-text-fill: #1177d1; "
                + "-fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-border-color: #dee2e6; "
                + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.1),3,0,0,1);");
        clozeToggle.selectedProperty().addListener((obs, old, sel) -> {
            ClozeQuestion.MODO_PREVIA_ALUMNO = sel;
            Question cur = questionTableView.getSelectionModel().getSelectedItem();
            if (cur != null) detailsWebView.getEngine().loadContent(cur.getDetails());
        });

        // --- Bus de eventos ---
        EventBus.getInstance().subscribe(AppEvents.CategorySelectedEvent.class, event -> {
            refreshQuestionTable();
            detailsWebView.getEngine().loadContent("");
            clozeToggle.setVisible(false);
        });
        EventBus.getInstance().subscribe(AppEvents.CategoryUpdatedEvent.class, event -> {
            applyCategoryFilter(searchCategoryField.getText());
            refreshQuestionTable();
        });
        EventBus.getInstance().subscribe(AppEvents.QuestionDeletedEvent.class, event -> {
            refreshQuestionTable();
            categoryTreeView.refresh();
            populateTypeFilterMenu(currentRootCategory);
        });

        questionTableView.getSelectionModel().selectedItemProperty().addListener((obs, o, newVal) -> {
            if (newVal != null) {
                clozeToggle.setVisible(newVal instanceof ClozeQuestion);
                detailsWebView.getEngine().loadContent(newVal.getDetails());
            } else {
                clozeToggle.setVisible(false);
                detailsWebView.getEngine().loadContent("");
            }
        });

        // --- Banderas de idioma (CORRECCIÓN Java 17) ---
        double fw = 21, fh = 14;
        languageBox.getChildren().addAll(
                buildFlagNode("/es.png", "🇪🇸", fw, fh,
                        () -> { I18n.setLocale(new Locale("es","ES")); updateLanguage(stage); }),
                buildFlagNode("/uk.png", "🇬🇧", fw, fh,
                        () -> { I18n.setLocale(new Locale("en","GB")); updateLanguage(stage); })
        );
        languageBox.setAlignment(Pos.CENTER);
    }

    // -------------------------------------------------------------------------
    //  Punto C: importar GIFT desde portapapeles
    // -------------------------------------------------------------------------

    private void handleImportFromClipboard() {
        String text = Clipboard.getSystemClipboard().getString();
        if (text == null || text.isBlank()) {
            new Alert(Alert.AlertType.WARNING,
                    I18n.get("gift.clipboard.empty")).showAndWait();
            return;
        }

        // Mostramos una vista previa del texto en un diálogo de confirmación
        Alert preview = new Alert(Alert.AlertType.CONFIRMATION);
        preview.setTitle(I18n.get("gift.clipboard.title"));
        preview.setHeaderText(I18n.get("gift.clipboard.header"));

        TextArea ta = new TextArea(text.length() > 800
                ? text.substring(0, 800) + "\n…" : text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(10);
        preview.getDialogPane().setContent(ta);
        preview.getDialogPane().setPrefWidth(560);
        preview.setResizable(true);

        ButtonType btnImport = new ButtonType(I18n.get("gift.clipboard.btnImport"),
                ButtonBar.ButtonData.OK_DONE);
        preview.getButtonTypes().setAll(btnImport, ButtonType.CANCEL);

        Optional<ButtonType> choice = preview.showAndWait();
        if (choice.isEmpty() || choice.get() != btnImport) return;

        // Escribimos el texto a un fichero temporal y lo parseamos con GIFTParser
        try {
            File tmp = File.createTempFile("moodleviewer_clipboard_", ".txt");
            tmp.deleteOnExit();
            Files.writeString(tmp.toPath(), text, java.nio.charset.StandardCharsets.UTF_8);

            GIFTParser.GiftImportResult result = GIFTParser.parseGIFTWithReport(tmp);
            Category imported = result.rootCategory();

            // Destino: categoría seleccionada o raíz
            TreeItem<Category> sel = categoryTreeView.getSelectionModel().getSelectedItem();
            Category dest = (sel != null && sel.getValue() != null)
                    ? sel.getValue() : currentRootCategory;

            int added = CategoryMerger.merge(dest, imported);
            CommandManager.getInstance().markAsDirty();
            applyCategoryFilter(searchCategoryField.getText());
            refreshQuestionTable();
            populateTypeFilterMenu(currentRootCategory);

            // Informar sobre resultados e issues
            if (result.issues().isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        I18n.get("gift.clipboard.success", added)).showAndWait();
            } else {
                StringBuilder sb = new StringBuilder(
                        I18n.get("gift.clipboard.successWithIssues", added,
                                result.issues().size())).append("\n\n");
                result.issues().forEach(iss ->
                        sb.append("• ").append(I18n.get("gift.issues.line", iss.startLine()))
                          .append(": ").append(iss.blockPreview()).append("\n"));
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle(I18n.get("gift.issues.title"));
                warn.setHeaderText(I18n.get("gift.clipboard.header"));
                TextArea issueArea = new TextArea(sb.toString());
                issueArea.setEditable(false);
                issueArea.setPrefRowCount(8);
                warn.getDialogPane().setContent(issueArea);
                warn.setResizable(true);
                warn.showAndWait();
            }
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    I18n.get("file.err.readBank", ex.getMessage())).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    I18n.get("file.err.readBank", ex.getMessage())).showAndWait();
        }
    }

    // -------------------------------------------------------------------------
    //  Apertura de banco
    // -------------------------------------------------------------------------

    private void handleOpenBank(Stage stage) {
        boolean hasBank = currentRootCategory != null
                && (!currentRootCategory.getQuestions().isEmpty()
                    || !currentRootCategory.getSubcategories().isEmpty());
        if (hasBank && !confirmDiscardUnsavedChanges(stage)) return;

        Optional<FileManager.OpenResult> result = fileManager.openOrMergeBank(stage, currentRootCategory);
        if (result.isEmpty()) return;

        FileManager.OpenResult open = result.get();
        currentRootCategory = open.rootCategory();
        loadedFile = open.file();
        updateFileNameLabel();

        if (loadedFile != null && loadedFile.getName().toLowerCase().endsWith(".xml")) {
            saveButton.setTooltip(new Tooltip(
                    I18n.get("main.btn.saveXml.tooltip.overwrite", loadedFile.getName())));
        } else {
            saveButton.setTooltip(new Tooltip(I18n.get("main.btn.saveXml.tooltip.new")));
        }

        populateTypeFilterMenu(currentRootCategory);
        applyCategoryFilter(searchCategoryField.getText());
        categoryTreeView.setShowRoot(false);
        refreshQuestionTable();

        if (!open.wasMerge()) {
            questionTableView.getItems().clear();
            detailsWebView.getEngine().loadContent("");
            CommandManager.getInstance().clear();
        }

        setControlsEnabled(true);
    }

    private void setControlsEnabled(boolean enabled) {
        saveButton.setDisable(!enabled);
        addQuestionButton.setDisable(!enabled);
        addCategoryButton.setDisable(!enabled);
        exportLatexButton.setDisable(!enabled);
        exportGiftButton.setDisable(!enabled);
        statsButton.setDisable(!enabled);
        duplicatesButton.setDisable(!enabled);
        importClipboardButton.setDisable(!enabled);
    }

    // -------------------------------------------------------------------------
    //  Cambios sin guardar
    // -------------------------------------------------------------------------

    private boolean confirmDiscardUnsavedChanges(Stage stage) {
        if (!CommandManager.getInstance().isDirty()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("file.dlg.unsavedChanges.title"));
        alert.setHeaderText(I18n.get("file.dlg.unsavedChanges.header"));
        alert.setContentText(I18n.get("file.dlg.unsavedChanges.content"));

        ButtonType btnSave    = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnSave"),    ButtonBar.ButtonData.YES);
        ButtonType btnDiscard = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnDiscard"), ButtonBar.ButtonData.NO);
        ButtonType btnCancel  = new ButtonType(I18n.get("file.dlg.unsavedChanges.btnCancel"),  ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSave, btnDiscard, btnCancel);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == btnCancel) return false;
        if (choice.get() == btnSave) {
            boolean saved = fileManager.saveMoodleXML(
                    stage, currentRootCategory, getSelectedTreeCategory(), loadedFile).isPresent();
            return saved || !CommandManager.getInstance().isDirty();
        }
        return true;
    }

    // -------------------------------------------------------------------------
    //  Punto 5: label con asterisco
    // -------------------------------------------------------------------------

    private void updateFileNameLabel() {
        String base = loadedFile == null
                ? I18n.get("main.lbl.noFile")
                : I18n.get("main.lbl.currentFile", loadedFile.getName());
        boolean dirty = CommandManager.getInstance().isDirty();
        fileNameLabel.setText(dirty ? "* " + base : base);
        fileNameLabel.setStyle(dirty
                ? "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #c0392b;"
                : "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");
    }

    // -------------------------------------------------------------------------
    //  Filtro y refresco
    // -------------------------------------------------------------------------

    public void applyCategoryFilter(String searchText) {
        if (currentRootCategory == null) return;
        TreeItem<Category> prev = categoryTreeView.getSelectionModel().getSelectedItem();
        Category prevCat = prev != null ? prev.getValue() : null;

        TreeItem<Category> newRoot = (searchText == null || searchText.isBlank())
                ? TreeBuilder.createTreeItem(currentRootCategory)
                : TreeBuilder.createFilteredTreeItem(currentRootCategory, searchText.toLowerCase());
        categoryTreeView.setRoot(newRoot);

        if (prevCat != null && newRoot != null) {
            TreeItem<Category> match = findTreeItem(newRoot, prevCat);
            if (match != null) {
                categoryTreeView.getSelectionModel().select(match);
            } else {
                categoryTreeView.getSelectionModel().clearSelection();
                questionTableView.setItems(FXCollections.observableArrayList());
                detailsWebView.getEngine().loadContent("");
                clozeToggle.setVisible(false);
            }
        }
    }

    private TreeItem<Category> findTreeItem(TreeItem<Category> node, Category target) {
        if (node == null) return null;
        if (node.getValue() == target) return node;
        for (TreeItem<Category> child : node.getChildren()) {
            TreeItem<Category> f = findTreeItem(child, target);
            if (f != null) return f;
        }
        return null;
    }

    public void refreshQuestionTable() {
        TreeItem<Category> sel = categoryTreeView.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getValue() != null) {
            Category cat = sel.getValue();
            ObservableList<Question> obs = FXCollections.observableArrayList(cat.getQuestions());
            FilteredList<Question> filtered = new FilteredList<>(obs, q -> {
                boolean matchesType = activeTypeFilters.isEmpty()
                        || activeTypeFilters.contains(q.getType());
                String txt = searchQuestionField.getText();
                if (txt == null || txt.isBlank()) return matchesType;
                String low = txt.toLowerCase();
                int idx = searchCriteriaCombo.getSelectionModel().getSelectedIndex();
                boolean matchesTxt = idx == 0
                        ? q.getName().toLowerCase().contains(low)
                        : q.getText() != null && q.getText().toLowerCase().contains(low);
                return matchesTxt && matchesType;
            });
            SortedList<Question> sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(questionTableView.comparatorProperty());
            questionTableView.setItems(sorted);
        } else {
            questionTableView.setItems(FXCollections.observableArrayList());
        }
    }

    // -------------------------------------------------------------------------
    //  Filtro por tipo
    // -------------------------------------------------------------------------

    private void populateTypeFilterMenu(Category root) {
        typeFilterMenu.getItems().clear();
        activeTypeFilters.clear();
        updateFilterButtonStyle();
        Set<String> types = new HashSet<>();
        collectTypes(root, types);
        types.forEach(type -> {
            CheckMenuItem item = new CheckMenuItem(type.toUpperCase());
            item.selectedProperty().addListener((obs, was, is) -> {
                if (is) activeTypeFilters.add(type); else activeTypeFilters.remove(type);
                updateFilterButtonStyle();
                refreshQuestionTable();
            });
            typeFilterMenu.getItems().add(item);
        });
        typeFilterMenu.setDisable(false);
    }

    private void collectTypes(Category cat, Set<String> out) {
        cat.getQuestions().forEach(q -> out.add(q.getType()));
        cat.getSubcategories().forEach(s -> collectTypes(s, out));
    }

    private void updateFilterButtonStyle() {
        if (activeTypeFilters.isEmpty()) {
            typeFilterMenu.setStyle("");
            typeFilterMenu.setText(I18n.get("main.filter.type"));
        } else {
            typeFilterMenu.setStyle("-fx-background-color:#dc3545;-fx-border-color:#c82333;"
                    + "-fx-text-fill:white;-fx-font-weight:bold;");
            typeFilterMenu.setText(I18n.get("main.filter.typeActive",
                    String.valueOf(activeTypeFilters.size())));
        }
    }

    // -------------------------------------------------------------------------
    //  Diálogos
    // -------------------------------------------------------------------------

    private void showAddQuestionDialog() {
        TreeItem<Category> sel = categoryTreeView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, I18n.get("main.alert.selectCat")).showAndWait();
            return;
        }
        new AddQuestionDialog(sel.getValue()).showAndWait();
        populateTypeFilterMenu(currentRootCategory);
        refreshQuestionTable();
        categoryTreeView.refresh();
    }

    private void showAddCategoryDialog() {
        if (currentRootCategory == null) return;
        TreeItem<Category> selItem = categoryTreeView.getSelectionModel().getSelectedItem();
        Category parentCat = selItem != null ? selItem.getValue() : currentRootCategory;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("main.dlg.cat.title"));
        dialog.setHeaderText(I18n.get("main.dlg.cat.header"));
        ButtonType btnOk = new ButtonType(I18n.get("main.dlg.btnAccept"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField nameField = new TextField();
        ToggleGroup tg = new ToggleGroup();
        RadioButton rbRoot = new RadioButton(I18n.get("main.dlg.cat.root"));
        RadioButton rbSub  = new RadioButton(I18n.get("main.dlg.cat.sub",
                selItem != null ? parentCat.getName() : ""));
        rbRoot.setToggleGroup(tg); rbSub.setToggleGroup(tg);
        if (selItem != null) rbSub.setSelected(true);
        else { rbRoot.setSelected(true); rbSub.setDisable(true); }
        grid.add(new Label(I18n.get("main.dlg.cat.location")), 0, 0);
        grid.add(new VBox(5, rbRoot, rbSub), 1, 0);
        grid.add(new Label(I18n.get("main.dlg.cat.name")), 0, 1);
        grid.add(nameField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == btnOk && !nameField.getText().isBlank()) {
                Category newCat = new Category(nameField.getText().trim());
                if (rbRoot.isSelected()) currentRootCategory.getSubcategories().add(newCat);
                else parentCat.getSubcategories().add(newCat);
                applyCategoryFilter(searchCategoryField.getText());
            }
            return null;
        });
        dialog.showAndWait();
    }

    // -------------------------------------------------------------------------
    //  Cambio de idioma
    // -------------------------------------------------------------------------

    private void updateLanguage(Stage stage) {
        stage.setTitle(I18n.get("main.window.title"));
        openBankButton.setText(I18n.get("main.btn.openBank"));
        saveButton.setText(I18n.get("main.btn.saveXml"));
        addQuestionButton.setText(I18n.get("main.btn.addQuestion"));
        exportLatexButton.setText(I18n.get("main.btn.saveLatex"));
        exportGiftButton.setText(I18n.get("main.btn.exportGift"));
        addCategoryButton.setText(I18n.get("main.btn.addCategory"));
        statsButton.setText(I18n.get("main.btn.stats"));
        duplicatesButton.setText(I18n.get("main.btn.duplicates"));
        importClipboardButton.setText(I18n.get("main.btn.importClipboard"));
        importClipboardButton.setTooltip(new Tooltip(I18n.get("main.btn.importClipboard.tooltip")));
        undoButton.getTooltip().setText(I18n.get("main.btn.undo.tooltip"));
        redoButton.getTooltip().setText(I18n.get("main.btn.redo.tooltip"));
        clozeToggle.setText(I18n.get("main.toggle.cloze"));
        searchCategoryField.setPromptText(I18n.get("main.search.category"));
        searchQuestionField.setPromptText(I18n.get("main.search.question"));

        int idx = searchCriteriaCombo.getSelectionModel().getSelectedIndex();
        searchCriteriaCombo.setItems(FXCollections.observableArrayList(
                I18n.get("main.search.byName"), I18n.get("main.search.byText")));
        searchCriteriaCombo.getSelectionModel().select(Math.max(0, idx));

        updateFileNameLabel();
        if (loadedFile != null && loadedFile.getName().toLowerCase().endsWith(".xml")) {
            saveButton.setTooltip(new Tooltip(
                    I18n.get("main.btn.saveXml.tooltip.overwrite", loadedFile.getName())));
        } else {
            saveButton.setTooltip(new Tooltip(I18n.get("main.btn.saveXml.tooltip.new")));
        }
        updateFilterButtonStyle();
        if (!questionTableView.getColumns().isEmpty()) {
            questionTableView.getColumns().get(0).setText(I18n.get("table.col.name"));
            questionTableView.getColumns().get(1).setText(I18n.get("table.col.type"));
        }
        categoryTreeView.refresh();
        questionTableView.refresh();
    }

    // -------------------------------------------------------------------------
    //  Utilidades
    // -------------------------------------------------------------------------

    private Label makeClearButton(CustomTextField field) {
        Label btn = new Label("×");
        btn.setStyle("-fx-cursor:hand;-fx-text-fill:#868e96;-fx-font-size:14px;-fx-padding:0 4 0 0;");
        btn.setOnMouseClicked(e -> field.clear());
        btn.visibleProperty().bind(field.textProperty().isNotEmpty());
        btn.managedProperty().bind(field.textProperty().isNotEmpty());
        return btn;
    }

    private javafx.scene.Node buildFlagNode(String resource, String emoji,
                                            double w, double h, Runnable action) {
        try {
            ImageView img = new ImageView(
                    new Image(getClass().getResourceAsStream(resource)));
            img.setFitWidth(w); img.setFitHeight(h);
            img.setPreserveRatio(false);
            img.setStyle("-fx-cursor:hand;");
            img.setOnMouseClicked(e -> action.run());
            return img;
        } catch (Exception ex) {
            Label lbl = new Label(emoji);
            lbl.setStyle("-fx-cursor:hand;-fx-font-size:14px;");
            lbl.setOnMouseClicked(e -> action.run());
            return lbl;
        }
    }

    private Category getSelectedTreeCategory() {
        TreeItem<Category> sel = categoryTreeView.getSelectionModel().getSelectedItem();
        if (sel == null) return null;
        return sel.getValue() == currentRootCategory ? null : sel.getValue();
    }

    // -------------------------------------------------------------------------
    //  Getters para LayoutManager
    // -------------------------------------------------------------------------

    public TreeView<Category>  getCategoryTreeView()    { return categoryTreeView; }
    public TableView<Question> getQuestionTableView()   { return questionTableView; }
    public WebView             getDetailsWebView()      { return detailsWebView; }
    public Label               getCounterLabel()        { return counterLabel; }
    public TextField           getSearchCategoryField() { return searchCategoryField; }
    public TextField           getSearchQuestionField() { return searchQuestionField; }
    public ComboBox<String>    getSearchCriteriaCombo() { return searchCriteriaCombo; }
    public MenuButton          getTypeFilterMenu()      { return typeFilterMenu; }
    public Button              getAddQuestionButton()   { return addQuestionButton; }
    public Button              getAddCategoryButton()   { return addCategoryButton; }
    public Button              getOpenBankButton()      { return openBankButton; }
    public Button              getSaveButton()          { return saveButton; }
    public Button              getExportLatexButton()   { return exportLatexButton; }
    public Button              getExportGiftButton()    { return exportGiftButton; }
    public Button              getImportClipboardButton(){ return importClipboardButton; }
    public Button              getStatsButton()         { return statsButton; }
    public Button              getDuplicatesButton()    { return duplicatesButton; }
    public Button              getUndoButton()          { return undoButton; }
    public Button              getRedoButton()          { return redoButton; }
    public CheckBox            getClozeToggle()         { return clozeToggle; }
    public Label               getFileNameLabel()       { return fileNameLabel; }
    public HBox                getLanguageBox()         { return languageBox; }

    public static void main(String[] args) { launch(args); }
}