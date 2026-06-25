/*
 * Copyright (c) 2026 Miguel Alonso Alonso
 * Se distribuye bajo la licencia MIT.
 */

package moodleviewer.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Sistema centralizado de publicación y suscripción de eventos (Event Bus).
 * Permite desacoplar completamente los componentes de la interfaz gráfica.
 *
 * NOTA DE CONCURRENCIA: aunque en la práctica casi todas las publicaciones se originan en el
 * hilo de JavaFX, algunas operaciones de la aplicación (p.ej. la compilación de PDF en
 * FileManager) se ejecutan en hilos de fondo. Por eso este registro usa estructuras seguras
 * para concurrencia (ConcurrentHashMap + CopyOnWriteArrayList) en lugar de HashMap/ArrayList,
 * evitando ConcurrentModificationException o condiciones de carrera si en el futuro se publica
 * o suscribe algún listener fuera del hilo de UI. Esto NO sustituye a Platform.runLater():
 * si un listener manipula controles de JavaFX, sigue siendo responsabilidad de quien publica
 * el evento (o del propio listener) asegurarse de hacerlo en el hilo de la aplicación.
 */
public class EventBus {
    
    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() { 
        return INSTANCE; 
    }

    /**
     * Suscribe un componente a un tipo de evento específico.
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(obj -> listener.accept(eventType.cast(obj)));
    }

    /**
     * Emite un evento a todos los suscriptores.
     */
    public void publish(Object event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                listener.accept(event);
            }
        }
    }
}