package org.example.arduino.util;

import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Battery;
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
 * Единый анализ цепи для каждого LED: замкнутый контур (+5V → … → GND, UNO → … → UNO
 * или батарейка + → … → −). Расчёт тока: I = (U − V_LED) / R — через {@link CircuitPhysics}.
 */
public final class CircuitAnalyzer {

    public enum CircuitKind {
        OPEN,
        POWER_RAIL,
        LOGIC,
        BATTERY
    }

    public record Result(
        CircuitKind kind,
        double seriesOhms,
        boolean missingResistorWarning,
        double supplyVolts,
        double wireOhms
    ) {
        public Result(CircuitKind kind, double seriesOhms, boolean missingResistorWarning) {
            this(kind, seriesOhms, missingResistorWarning, CircuitPhysics.SUPPLY_V, 0);
        }

        public boolean isClosed() {
            return kind != CircuitKind.OPEN;
        }

        public double ohmsForFormula() {
            if (!isClosed()) {
                return -1;
            }
            return seriesOhms > 0 ? seriesOhms : 0;
        }

        public double supplyVolts() {
            return supplyVolts > 0 ? supplyVolts : CircuitPhysics.SUPPLY_V;
        }

        public static Result open() {
            return new Result(CircuitKind.OPEN, -1, false, 0, 0);
        }
    }

    public record LedOutcome(
        Result circuit,
        CircuitPhysics.CalcResult calc,
        boolean glow,
        boolean burn,
        boolean resistorWarning
    ) {
        public String statusLine() {
            if (circuit == null || !circuit.isClosed()) {
                return "";
            }
            if (burn) {
                return "⚠️ ПЕРЕГРУЗКА LED! " + calc.formulaOneLine()
                    + " — нужен R ≈ " + String.format("%.0f", calc.getRecommendedOhms()) + " Ом";
            }
            if (resistorWarning) {
                return "⚠️ LED без резистора: " + calc.formulaOneLine()
                    + " — рекомендуется R ≈ " + String.format("%.0f", calc.getRecommendedOhms()) + " Ом";
            }
            if (glow) {
                return "✓ " + calc.formulaOneLine();
            }
            if (calc.getSafety() == CircuitPhysics.SafetyLevel.TOO_LOW) {
                return "LED не светится: " + calc.formulaOneLine()
                    + " — уменьшите R (рекомендуется ≈ " + String.format("%.0f", calc.getRecommendedOhms()) + " Ом)";
            }
            return "";
        }
    }

    private record LoopState(
        Component component,
        double ohms,
        double wireOhms,
        boolean hasResistor,
        Set<Wire> usedWires,
        boolean seenLed,
        boolean leftSource
    ) {
    }

    private record WireStep(Component component, Wire wire) {
    }

    private record LoopResult(double seriesOhms, double wireOhms, boolean hasResistor) {
    }

    private CircuitAnalyzer() {
    }

    public static Result analyzeLed(LED led, List<Component> components, List<Wire> wires) {
        if (led == null || wires == null) {
            return Result.open();
        }

        PowerRailSimulator.PowerResult rail = PowerRailSimulator.analyzeLedPowerPath(led, components, wires);
        if (rail.closed()) {
            return new Result(
                CircuitKind.POWER_RAIL,
                rail.seriesOhms(),
                rail.seriesOhms() <= 0,
                CircuitPhysics.SUPPLY_V,
                rail.wireOhms()
            );
        }

        LoopResult bestBattery = null;
        Battery bestBatterySource = null;
        for (Component comp : components) {
            if (!(comp instanceof Battery battery)) {
                continue;
            }
            LoopResult loop = traceBatteryLoop(battery, led, wires);
            if (loop != null && (bestBattery == null || loop.seriesOhms() < bestBattery.seriesOhms())) {
                bestBattery = loop;
                bestBatterySource = battery;
            }
        }
        if (bestBattery != null && bestBatterySource != null) {
            return new Result(
                CircuitKind.BATTERY,
                bestBattery.seriesOhms(),
                !bestBattery.hasResistor(),
                bestBatterySource.getVoltage(),
                bestBattery.wireOhms()
            );
        }

        LoopResult bestUno = null;
        for (Component comp : components) {
            if (!(comp instanceof ArduinoUNO arduino) || !arduino.isPowered()) {
                continue;
            }
            LoopResult loop = tracePoweredUnoLoop(arduino, led, wires);
            if (loop != null && (bestUno == null || loop.seriesOhms() < bestUno.seriesOhms())) {
                bestUno = loop;
            }
        }
        if (bestUno != null) {
            return new Result(
                CircuitKind.LOGIC,
                bestUno.seriesOhms(),
                !bestUno.hasResistor(),
                CircuitPhysics.SUPPLY_V,
                bestUno.wireOhms()
            );
        }
        return Result.open();
    }

    public static LedOutcome computeLedOutcome(LED led, List<Component> components, List<Wire> wires) {
        Result circuit = analyzeLed(led, components, wires);
        if (!circuit.isClosed()) {
            return null;
        }
        CircuitPhysics.CalcResult calc = CircuitPhysics.analyze(
            circuit.supplyVolts(),
            circuit.ohmsForFormula(),
            CircuitPhysics.LED_VF
        );

        if (circuit.kind() == CircuitKind.POWER_RAIL) {
            boolean burn = calc.getSafety() == CircuitPhysics.SafetyLevel.OVERLOAD
                || calc.getSafety() == CircuitPhysics.SafetyLevel.NO_RESISTOR;
            boolean glow = !burn && CircuitPhysics.isLedGlowing(calc.getCurrentMa());
            return new LedOutcome(circuit, calc, glow, burn, false);
        }

        if (circuit.missingResistorWarning()) {
            return new LedOutcome(circuit, calc, true, false, true);
        }
        boolean burn = calc.getSafety() == CircuitPhysics.SafetyLevel.OVERLOAD
            || calc.getSafety() == CircuitPhysics.SafetyLevel.NO_RESISTOR;
        boolean glow = !burn && CircuitPhysics.isLedGlowing(calc.getCurrentMa());
        return new LedOutcome(circuit, calc, glow, burn, false);
    }

    public static boolean hasPathFromArduino(Component target, List<Component> components, List<Wire> wires) {
        if (target == null) {
            return false;
        }
        Queue<Component> queue = new ArrayDeque<>();
        Set<Component> visited = new HashSet<>();
        for (Component comp : components) {
            if (comp instanceof ArduinoUNO arduino && arduino.isPowered()) {
                queue.offer(arduino);
                visited.add(arduino);
            }
        }
        while (!queue.isEmpty()) {
            Component current = queue.poll();
            if (current == target) {
                return true;
            }
            if (current instanceof Timer timer && !timer.getOutput()) {
                continue;
            }
            if (current instanceof Button button && !button.isPressed()) {
                continue;
            }
            for (Component next : neighbors(current, wires)) {
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
            for (Component connected : current.getConnections()) {
                if (!visited.contains(connected)) {
                    visited.add(connected);
                    queue.offer(connected);
                }
            }
        }
        return false;
    }

    /**
     * Замкнутый контур через UNO: UNO → … → LED → … → UNO.
     */
    private static LoopResult tracePoweredUnoLoop(ArduinoUNO uno, LED targetLed, List<Wire> wires) {
        Queue<LoopState> queue = new ArrayDeque<>();
        queue.offer(new LoopState(uno, 0, 0, false, Set.of(), false, false));

        while (!queue.isEmpty()) {
            LoopState state = queue.poll();
            Component current = state.component();
            boolean seenLed = state.seenLed() || current == targetLed;

            if (current == uno && seenLed && state.leftSource() && state.usedWires().size() >= 2) {
                return new LoopResult(state.ohms(), state.wireOhms(), state.hasResistor());
            }
            if (!conductsForTraversal(current)) {
                continue;
            }

            for (WireStep step : wireNeighbors(current, wires)) {
                Component next = step.component();
                Wire wire = step.wire();
                if (state.usedWires().contains(wire)) {
                    continue;
                }
                if (next instanceof LED && next != targetLed) {
                    continue;
                }
                if (next == uno) {
                    if (seenLed && state.leftSource() && state.usedWires().size() >= 1) {
                        Set<Wire> withReturn = new HashSet<>(state.usedWires());
                        withReturn.add(wire);
                        if (withReturn.size() >= 2) {
                            double wireAdd = wire.getResistanceOhms();
                            return new LoopResult(
                                state.ohms() + wireAdd,
                                state.wireOhms() + wireAdd,
                                state.hasResistor()
                            );
                        }
                    }
                    continue;
                }
                double wireAdd = wire.getResistanceOhms();
                double compAdd = resistanceStep(current, next, wires);
                boolean hasR = state.hasResistor() || next instanceof Resistor;
                Set<Wire> used = new HashSet<>(state.usedWires());
                used.add(wire);
                boolean leftSource = state.leftSource() || current == uno;
                queue.offer(new LoopState(
                    next,
                    state.ohms() + wireAdd + compAdd,
                    state.wireOhms() + wireAdd,
                    hasR,
                    used,
                    seenLed || next == targetLed,
                    leftSource
                ));
            }
        }
        return null;
    }

    /**
     * Замкнутый контур: батарейка + → … → LED → … → батарейка −.
     */
    private static LoopResult traceBatteryLoop(Battery battery, LED targetLed, List<Wire> wires) {
        Queue<LoopState> queue = new ArrayDeque<>();
        for (WireStep step : wireNeighborsAtPin(battery, 1, wires)) {
            Wire wire = step.wire();
            Component next = step.component();
            if (next == battery || next instanceof LED && next != targetLed) {
                continue;
            }
            double wireAdd = wire.getResistanceOhms();
            double compAdd = resistanceStep(battery, next, wires);
            boolean hasR = next instanceof Resistor;
            Set<Wire> used = new HashSet<>();
            used.add(wire);
            queue.offer(new LoopState(
                next,
                wireAdd + compAdd,
                wireAdd,
                hasR,
                used,
                next == targetLed,
                true
            ));
        }

        while (!queue.isEmpty()) {
            LoopState state = queue.poll();
            Component current = state.component();
            boolean seenLed = state.seenLed() || current == targetLed;

            if (!conductsForTraversal(current)) {
                continue;
            }

            for (WireStep step : wireNeighbors(current, wires)) {
                Component next = step.component();
                Wire wire = step.wire();
                if (state.usedWires().contains(wire)) {
                    continue;
                }
                if (next instanceof LED && next != targetLed) {
                    continue;
                }
                if (next == battery) {
                    if (seenLed && wireConnectsPin(wire, battery, 2)) {
                        double wireAdd = wire.getResistanceOhms();
                        return new LoopResult(
                            state.ohms() + wireAdd,
                            state.wireOhms() + wireAdd,
                            state.hasResistor()
                        );
                    }
                    continue;
                }
                double wireAdd = wire.getResistanceOhms();
                double compAdd = resistanceStep(current, next, wires);
                boolean hasR = state.hasResistor() || next instanceof Resistor;
                Set<Wire> used = new HashSet<>(state.usedWires());
                used.add(wire);
                queue.offer(new LoopState(
                    next,
                    state.ohms() + wireAdd + compAdd,
                    state.wireOhms() + wireAdd,
                    hasR,
                    used,
                    seenLed || next == targetLed,
                    state.leftSource()
                ));
            }
        }
        return null;
    }

    private static boolean wireConnectsPin(Wire wire, Battery battery, int pin) {
        WireAnchor anchor = WireAnchor.component(battery, pin);
        return wire.getFromAnchor().isSameEndpoint(anchor) || wire.getToAnchor().isSameEndpoint(anchor);
    }

    private static List<WireStep> wireNeighborsAtPin(Component component, int pin, List<Wire> wires) {
        WireAnchor anchor = WireAnchor.component(component, pin);
        List<WireStep> result = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getFromAnchor().isSameEndpoint(anchor)) {
                Component to = wire.getTo();
                if (to != null && to != component) {
                    result.add(new WireStep(to, wire));
                }
            } else if (wire.getToAnchor().isSameEndpoint(anchor)) {
                Component from = wire.getFrom();
                if (from != null && from != component) {
                    result.add(new WireStep(from, wire));
                }
            }
        }
        return result;
    }

    private static List<WireStep> wireNeighbors(Component component, List<Wire> wires) {
        List<WireStep> result = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getFrom() == component) {
                Component to = wire.getTo();
                if (to != null && to != component) {
                    result.add(new WireStep(to, wire));
                }
            } else if (wire.getTo() == component) {
                Component from = wire.getFrom();
                if (from != null && from != component) {
                    result.add(new WireStep(from, wire));
                }
            }
        }
        return result;
    }

    /** Два резистора с проводами ножка1–ножка1 и ножка2–ножка2 включены параллельно. */
    public static boolean areResistorsParallel(Resistor r1, Resistor r2, List<Wire> wires) {
        return hasResistorPinWire(r1, 1, r2, 1, wires)
            && hasResistorPinWire(r1, 2, r2, 2, wires);
    }

    public static double parallelOhms(double r1, double r2) {
        if (r1 <= 0 || r2 <= 0) {
            return 0;
        }
        return (r1 * r2) / (r1 + r2);
    }

    public static double parallelGroupOhms(Resistor resistor, List<Component> components, List<Wire> wires) {
        if (resistor == null) {
            return 0;
        }
        if (components == null || wires == null) {
            return resistor.getResistance();
        }
        double equivalent = resistor.getResistance();
        Set<Resistor> inGroup = new HashSet<>();
        inGroup.add(resistor);
        boolean expanded;
        do {
            expanded = false;
            for (Component comp : components) {
                if (!(comp instanceof Resistor other) || inGroup.contains(other)) {
                    continue;
                }
                for (Resistor member : new ArrayList<>(inGroup)) {
                    if (areResistorsParallel(member, other, wires)) {
                        equivalent = parallelOhms(equivalent, other.getResistance());
                        inGroup.add(other);
                        expanded = true;
                        break;
                    }
                }
            }
        } while (expanded);
        return equivalent;
    }

    private static boolean hasResistorPinWire(
        Resistor r1, int pin1, Resistor r2, int pin2, List<Wire> wires
    ) {
        WireAnchor a = WireAnchor.component(r1, pin1);
        WireAnchor b = WireAnchor.component(r2, pin2);
        for (Wire wire : wires) {
            if (wire.connects(a, b)) {
                return true;
            }
        }
        return false;
    }

    private static double resistanceStep(Component current, Component next, List<Wire> wires) {
        if (!(next instanceof Resistor nextR)) {
            return 0;
        }
        if (current instanceof Resistor currentR && areResistorsParallel(currentR, nextR, wires)) {
            double parallel = parallelOhms(currentR.getResistance(), nextR.getResistance());
            return parallel - currentR.getResistance();
        }
        return nextR.getResistance();
    }

    private static boolean conductsForTraversal(Component component) {
        if (component instanceof Timer timer) {
            return timer.isActive() && timer.isRunning() && timer.getOutputState();
        }
        if (component instanceof Button button) {
            return button.isPressed();
        }
        if (component instanceof ArduinoUNO arduino) {
            return arduino.isPowered();
        }
        if (component instanceof Battery) {
            return false;
        }
        return component instanceof Resistor || component instanceof LED;
    }

    private static List<Component> neighbors(Component component, List<Wire> wires) {
        List<Component> result = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getFrom() == component) {
                Component to = wire.getTo();
                if (to != null && to != component) {
                    result.add(to);
                }
            } else if (wire.getTo() == component) {
                Component from = wire.getFrom();
                if (from != null && from != component) {
                    result.add(from);
                }
            }
        }
        return result;
    }
}
