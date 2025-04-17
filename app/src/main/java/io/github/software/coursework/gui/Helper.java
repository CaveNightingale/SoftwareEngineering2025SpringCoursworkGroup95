package io.github.software.coursework.gui;

import javafx.animation.PauseTransition;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.util.Duration;

public final class Helper {
    public static final class Debounce implements Runnable, EventHandler<Event> {
        private final PauseTransition pauseTransition;
        public Debounce(Runnable runnable, long delay) {
            pauseTransition = new PauseTransition(Duration.millis(delay));
            pauseTransition.setOnFinished(event -> runnable.run());
        }

        public void run() {
            pauseTransition.playFromStart();
        }

        @Override
        public void handle(Event event) {
            run();
        }
    }

    public static Debounce debounce(Runnable runnable) {
        return debounce(runnable, 250);
    }

    public static Debounce debounce(Runnable runnable, long delay) {
        return new Debounce(runnable, delay);
    }
}
