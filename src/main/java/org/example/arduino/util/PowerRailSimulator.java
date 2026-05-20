package org.example.arduino.util;

import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Button;
import org.example.arduino.model.Component;
import org.example.arduino.model.LED;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Timer;
import org.example.arduino.model.Wire;
import org.example.arduino.model.WireAnchor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Упрощённая модель цепи питания через шины +5V и GND (последовательное включение).
 */
public final class PowerRailSimulator {

    public record PowerResult(double seriesOhms, boolean closed) {
        public static PowerResult open() {
            return new PowerResult(-1, false);
        }
    }

    private PowerRailSimulator() {
    }

    /**
     * Ищет замкнутый путь GND → … → LED → … → +5V (или наоборот) через проводящие компоненты.
     */
    public static PowerResult analyzeLedPowerPath(LED targetLed, List<Component> components, List<Wire> wires) {
        if (targetLed == null || targetLed.isBurned() || wires == null) {
            return PowerResult.open();
        }

        PowerResult fromMinus = traceFromRail(true, targetLed, wires);
        if (fromMinus.closed()) {
            return fromMinus;
        }
        return traceFromRail(false, targetLed, wires);
    }

    private static PowerResult traceFromRail(boolean fromMinusRail, LED targetLed, List<Wire> wires) {
        Queue<State> queue = new ArrayDeque<>();
        seedFromRail(fromMinusRail, wires, queue);

        while (!queue.isEmpty()) {
            State state = queue.poll();
            Component current = state.component();
            if (!passesCurrent(current)) {
                continue;
            }

            double ohms = state.ohms() + resistanceOf(current);
            boolean seenLed = state.seenLed() || current == targetLed;

            if (seenLed && touchesRail(current, wires, fromMinusRail)) {
                return new PowerResult(Math.max(ohms, 1.0), true);
            }

            for (Component next : neighbors(current, wires)) {
                if (state.visited().contains(next)) {
                    continue;
                }
                if (next instanceof ArduinoUNO) {
                    continue;
                }
                Set<Component> visited = new HashSet<>(state.visited());
                visited.add(next);
                queue.offer(new State(next, ohms, visited, seenLed));
            }
        }
        return PowerResult.open();
    }

    private static void seedFromRail(boolean minusRail, List<Wire> wires, Queue<State> queue) {
        for (Wire wire : wires) {
            Component start = null;
            if (minusRail) {
                if (wire.getFromAnchor().isRailMinus()) {
                    start = wire.getTo();
                } else if (wire.getToAnchor().isRailMinus()) {
                    start = wire.getFrom();
                }
            } else if (wire.getFromAnchor().isRailPlus()) {
                start = wire.getTo();
            } else if (wire.getToAnchor().isRailPlus()) {
                start = wire.getFrom();
            }
            if (start == null || start instanceof ArduinoUNO) {
                continue;
            }
            Set<Component> visited = new HashSet<>();
            visited.add(start);
            queue.offer(new State(start, 0, visited, start instanceof LED));
        }
    }

    private static boolean touchesRail(Component component, List<Wire> wires, boolean plusRail) {
        for (Wire wire : wires) {
            if (wire.getFrom() == component && isTargetRail(wire.getToAnchor(), plusRail)) {
                return true;
            }
            if (wire.getTo() == component && isTargetRail(wire.getFromAnchor(), plusRail)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTargetRail(WireAnchor anchor, boolean plusRail) {
        if (anchor == null) {
            return false;
        }
        return plusRail ? anchor.isRailPlus() : anchor.isRailMinus();
    }

    private static List<Component> neighbors(Component component, List<Wire> wires) {
        List<Component> result = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getFrom() == component) {
                if (wire.getTo() != null && wire.getTo() != component) {
                    result.add(wire.getTo());
                }
            } else if (wire.getTo() == component) {
                if (wire.getFrom() != null && wire.getFrom() != component) {
                    result.add(wire.getFrom());
                }
            }
        }
        return result;
    }

    private static boolean passesCurrent(Component component) {
        if (component instanceof Timer timer) {
            return timer.isActive() && timer.isRunning() && timer.getOutputState();
        }
        if (component instanceof Button button) {
            return button.isPressed();
        }
        return component instanceof Resistor || component instanceof LED;
    }

    private static double resistanceOf(Component component) {
        if (component instanceof Resistor resistor) {
            return resistor.getResistance();
        }
        return 0;
    }

    /** Есть ли физическое соединение LED с обеими шинами (без учёта кнопок/таймера). */
    public static boolean isTopologicallyBetweenRails(LED led, List<Wire> wires) {
        if (led == null) {
            return false;
        }
        return hasOpenPathToRail(led, wires, true) && hasOpenPathToRail(led, wires, false);
    }

    private static boolean hasOpenPathToRail(LED target, List<Wire> wires, boolean plusRail) {
        Queue<Component> queue = new ArrayDeque<>();
        Set<Component> visited = new HashSet<>();
        queue.offer(target);
        visited.add(target);

        while (!queue.isEmpty()) {
            Component current = queue.poll();
            if (touchesRail(current, wires, plusRail)) {
                return true;
            }
            for (Component next : neighbors(current, wires)) {
                if (next instanceof ArduinoUNO || visited.contains(next)) {
                    continue;
                }
                if (next instanceof Timer || next instanceof Button || next instanceof Resistor || next instanceof LED) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        return false;
    }

    private record State(Component component, double ohms, Set<Component> visited, boolean seenLed) {
    }
}
