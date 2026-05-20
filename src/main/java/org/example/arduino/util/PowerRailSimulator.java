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
 * Сопротивление проводов: R = k × L.
 */
public final class PowerRailSimulator {

    public record PowerResult(double seriesOhms, double wireOhms, boolean closed) {
        public static PowerResult open() {
            return new PowerResult(-1, 0, false);
        }
    }

    private record State(Component component, double ohms, double wireOhms, Set<Component> visited, boolean seenTarget) {
    }

    private record NeighborStep(Component component, Wire wire) {
    }

    private PowerRailSimulator() {
    }

    public static PowerResult analyzeLedPowerPath(LED targetLed, List<Component> components, List<Wire> wires) {
        if (targetLed == null || wires == null) {
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
        seedFromRail(fromMinusRail, targetLed, wires, queue);

        while (!queue.isEmpty()) {
            State state = queue.poll();
            Component current = state.component();
            if (!passesCurrent(current)) {
                continue;
            }

            double ohms = state.ohms() + resistanceOf(current);
            double wireOhms = state.wireOhms();
            boolean seenTarget = state.seenTarget() || current == targetLed;
            boolean endAtPlusRail = fromMinusRail;

            if (seenTarget && touchesRail(current, wires, endAtPlusRail)) {
                double railWire = railConnectionOhms(current, wires, endAtPlusRail);
                return new PowerResult(ohms + railWire, wireOhms + railWire, true);
            }

            for (NeighborStep step : wireNeighbors(current, wires)) {
                Component next = step.component();
                Wire wire = step.wire();
                if (state.visited().contains(next)) {
                    continue;
                }
                if (next instanceof ArduinoUNO) {
                    continue;
                }
                double wireAdd = wire.getResistanceOhms();
                Set<Component> visited = new HashSet<>(state.visited());
                visited.add(next);
                queue.offer(new State(
                    next,
                    ohms + wireAdd,
                    wireOhms + wireAdd,
                    visited,
                    seenTarget || next == targetLed
                ));
            }
        }
        return PowerResult.open();
    }

    private static void seedFromRail(boolean minusRail, LED targetLed, List<Wire> wires, Queue<State> queue) {
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
            double wireAdd = wire.getResistanceOhms();
            Set<Component> visited = new HashSet<>();
            visited.add(start);
            queue.offer(new State(start, wireAdd, wireAdd, visited, start == targetLed));
        }
    }

    private static List<NeighborStep> wireNeighbors(Component component, List<Wire> wires) {
        List<NeighborStep> result = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getFrom() == component) {
                if (wire.getTo() != null && wire.getTo() != component) {
                    result.add(new NeighborStep(wire.getTo(), wire));
                }
            } else if (wire.getTo() == component) {
                if (wire.getFrom() != null && wire.getFrom() != component) {
                    result.add(new NeighborStep(wire.getFrom(), wire));
                }
            }
        }
        return result;
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

    private static double railConnectionOhms(Component component, List<Wire> wires, boolean plusRail) {
        double sum = 0;
        for (Wire wire : wires) {
            if (wire.getFrom() == component && isTargetRail(wire.getToAnchor(), plusRail)) {
                sum += wire.getResistanceOhms();
            }
            if (wire.getTo() == component && isTargetRail(wire.getFromAnchor(), plusRail)) {
                sum += wire.getResistanceOhms();
            }
        }
        return sum;
    }

    private static boolean isTargetRail(WireAnchor anchor, boolean plusRail) {
        if (anchor == null) {
            return false;
        }
        return plusRail ? anchor.isRailPlus() : anchor.isRailMinus();
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
            for (NeighborStep step : wireNeighbors(current, wires)) {
                Component next = step.component();
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
}
