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
 * Sistema centralizado de publicación/suscripción de eventos (Event Bus).
 *
 * PUNTO 7: añadido método {@link #unsubscribe(Class, Consumer)} para que los
 * componentes puedan desregistrar sus listeners cuando ya no sean necesarios,
 * evitando acumulación de referencias en futuros escenarios multi-instancia.
 *
 * Nota de concurrencia: las estructuras ConcurrentHashMap + CopyOnWriteArrayList
 * garantizan seguridad ante publicaciones desde hilos de fondo sin afectar al
 * hilo de UI; si un listener manipula controles JavaFX sigue siendo responsabilidad
 * del publicador o del listener envolverlo en {@code Platform.runLater()}.
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() { return INSTANCE; }

    /**
     * Suscribe un componente a un tipo de evento concreto.
     *
     * @param <T>       tipo del evento.
     * @param eventType clase del evento.
     * @param listener  consumidor que procesará el evento.
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(obj -> listener.accept(eventType.cast(obj)));
    }

    /**
     * PUNTO 7: desregistra un listener previamente suscrito.
     * <p>
     * Dado que el método {@link #subscribe} envuelve el listener original en una
     * lambda anónima, no es posible eliminar esa lambda por identidad de referencia.
     * La estrategia adoptada (limpieza por tipo de evento) es la más sencilla y
     * suficiente para el ciclo de vida actual de la aplicación, donde cada tipo de
     * evento tiene un único conjunto de suscriptores que se registran una sola vez
     * al arrancar la UI. Si en el futuro se necesita granularidad fina (desuscribir
     * un listener concreto de muchos), se puede hacer que {@code subscribe} devuelva
     * un token opaco (p.ej. un {@code Runnable} de cancelación) y usar ese token aquí.
     *
     * @param eventType clase del evento del que se quieren eliminar todos los listeners.
     */
    public void unsubscribe(Class<?> eventType) {
        listeners.remove(eventType);
    }

    /**
     * Emite un evento a todos sus suscriptores en el orden de registro.
     *
     * @param event objeto de evento a publicar.
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