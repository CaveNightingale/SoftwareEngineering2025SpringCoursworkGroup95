package io.github.software.coursework.gui;

import javafx.animation.PauseTransition;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * A helper class for JavaFX.
 */
final class Helper {
    /**
     * A debouncer that can be used to debounce events.
     */
    public static final class Debounce implements Runnable, EventHandler<Event> {
        private final PauseTransition pauseTransition;
        public Debounce(Runnable runnable, long delay) {
            pauseTransition = new PauseTransition(Duration.millis(delay));
            pauseTransition.setOnFinished(event -> runnable.run());
        }

        /**
         * Runs the debouncer.
         */
        public void run() {
            pauseTransition.playFromStart();
        }

        /**
         * Handles the event.
         * @param event the event which occurred
         */
        @Override
        public void handle(Event event) {
            run();
        }
    }

    /**
     * Debounces the given runnable.
     * @param runnable the runnable to debounce
     * @return the debouncer
     */
    public static Debounce debounce(Runnable runnable) {
        return debounce(runnable, 250);
    }

    /**
     * Debounces the given runnable.
     * @param runnable the runnable to debounce
     * @param delay the delay in milliseconds
     * @return the debouncer
     */
    public static Debounce debounce(Runnable runnable, long delay) {
        return new Debounce(runnable, delay);
    }
}
