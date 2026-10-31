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
        if (targetLed == null || wires == null || CircuitAnalyzer.limitation(targetLed,wires) != null) return PowerResult.open();
        PassiveNetwork.Equivalent result=PassiveNetwork.analyze(targetLed,null,CircuitPhysics.SUPPLY_V,wires);
        return result == null || result.volts()<1e-9 ? PowerResult.open()
            : new PowerResult(result.ohms(),result.wireOhms(),true);
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

    public static boolean isTopologicallyBetweenRails(LED led, List<Wire> wires) {
        if (led == null) {
            return false;
        }
        return hasOpenPathToRail(led, wires, true) && hasOpenPathToRail(led, wires, false);
    }

    private static List<NeighborStep> wireNeighbors(Component component,List<Wire> wires) {
        List<NeighborStep> result=new ArrayList<>();
        for(Wire wire:wires) {
            Component next=wire.getFrom()==component?wire.getTo():wire.getTo()==component?wire.getFrom():null;
            if(next!=null && next!=component)result.add(new NeighborStep(next,wire));
        }
        return result;
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
