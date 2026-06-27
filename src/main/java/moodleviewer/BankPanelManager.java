/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */
package moodleviewer;

import javafx.scene.control.TableView;
import moodleviewer.model.Question;

/**
 * Contrato formal para los gestores de paneles del banco de preguntas.
 *
 * PUNTO 15: definir esta interfaz permite al compilador detectar incompatibilidades
 * de firma si {@code configure} se renombra o cambia en {@link TableManager} o
 * {@link TreeManager} sin actualizar el otro.
 */
public interface BankPanelManager {
    /**
     * Vincula el gestor al componente visual correspondiente y registra todos sus
     * listeners, suscripciones al EventBus y manejadores de eventos.
     *
     * @param table componente principal gestionado (TableView para TableManager).
     */
    void configure(TableView<Question> table);
}