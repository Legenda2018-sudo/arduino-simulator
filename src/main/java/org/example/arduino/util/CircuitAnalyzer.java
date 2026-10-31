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

    private CircuitAnalyzer() {
    }

    public static boolean controllerClosed(ArduinoUNO uno) {
        return uno.isPowered() && (!uno.hasProgram() || uno.getOutput());
    }

    /** External supplies provide voltage; a UNO in that circuit is an educational controlled switch. */
    public static boolean hasExternalSupply(Component start, List<Wire> wires) {
        Set<Component> seen = new HashSet<>(); Queue<Component> queue = new ArrayDeque<>();
        seen.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            Component current = queue.remove();
            if (current instanceof Battery) return true;
            for (Wire w : wires) {
                if (w.getFrom() != current && w.getTo() != current) continue;
                Component next = w.getFrom() == current ? w.getTo() : w.getFrom();
                if (next == null) return true;
                if (seen.add(next)) queue.add(next);
            }
        }
        return false;
    }

    private record Contact(Object component, int pin) { }
    private static Contact contact(WireAnchor anchor) {
        if (anchor.isRailPlus()) return new Contact("rail",2);
        if (anchor.isRailMinus()) return new Contact("rail",1);
        return new Contact(anchor.getComponent(),anchor.getPin());
    }
    private static Contact wireContact(WireAnchor anchor, Wire wire) {
        // The visual center of a LED is not an electrical junction between its two leads.
        return anchor.getComponent() instanceof LED ? new Contact(wire,anchor == wire.getFromAnchor()?1:2) : contact(anchor);
    }

    /** Detect a wire-only bypass of a source; resistors and LED loads are not conductors here. */
    public static String shortCircuitWarning(List<Component> components, List<Wire> wires) {
        java.util.Map<Contact,java.util.Set<Contact>> graph = new java.util.HashMap<>();
        for (Wire wire : wires) {
            Contact a=wireContact(wire.getFromAnchor(),wire),b=wireContact(wire.getToAnchor(),wire);
            graph.computeIfAbsent(a,k->new HashSet<>()).add(b);
            graph.computeIfAbsent(b,k->new HashSet<>()).add(a);
        }
        for(Component c:components) if(c instanceof ArduinoUNO u && hasExternalSupply(u,wires) && controllerClosed(u)) {
            Contact a=new Contact(u,1),b=new Contact(u,2);
            graph.computeIfAbsent(a,k->new HashSet<>()).add(b);
            graph.computeIfAbsent(b,k->new HashSet<>()).add(a);
        }
        java.util.List<Object> sources=new ArrayList<>(); sources.add("rail");
        for(Component c:components) if(c instanceof Battery || c instanceof ArduinoUNO u && !hasExternalSupply(u,wires) && u.isPowered()
            && (!u.hasProgram() || u.getOutput())) sources.add(c);
        for(Object source:sources) {
            Set<Contact> seen=new HashSet<>(); Queue<Contact> queue=new ArrayDeque<>();
            Contact start=new Contact(source,2),end=new Contact(source,1);
            seen.add(start);queue.add(start);
            while(!queue.isEmpty()) {
                Contact current=queue.remove();
                if(current.equals(end)) return "Короткое замыкание источника: соединены + и − проводами без нагрузки";
                for(Contact next:graph.getOrDefault(current,Set.of())) if(seen.add(next))queue.add(next);
            }
        }
        return null;
    }

    /** Refuse ambiguous legacy loads instead of displaying a plausible but false current. */
    public static String limitation(LED target, List<Wire> wires) {
        long count = wires.stream().filter(w -> w.getFrom() == target || w.getTo() == target).count();
        if (count > 2) return "LED: нужны ровно два провода; отдельные анод и катод пока не заданы";
        Set<Component> visited = new HashSet<>();
        Set<Component> sources = new HashSet<>();
        Queue<Component> queue = new ArrayDeque<>();
        queue.add(target); visited.add(target); boolean rails = false;
        while (!queue.isEmpty()) {
            Component current = queue.remove();
            for (Wire wire : wires) {
                if (wire.getFrom() != current && wire.getTo() != current) continue;
                Component next = wire.getFrom() == current ? wire.getTo() : wire.getFrom();
                if (next == null) { rails = true; continue; }
                if (next instanceof Battery) { sources.add(next); continue; }
                if (next instanceof ArduinoUNO && !hasExternalSupply(next,wires)) {
                    sources.add(next); continue;
                }
                if (next instanceof LED && next != target) return "Общая цепь нескольких LED пока не поддерживается; используйте отдельные ветви";
                if (visited.add(next)) queue.add(next);
            }
        }
        for(Component component : visited) if(component instanceof ArduinoUNO) {
            List<WireAnchor> contacts=new ArrayList<>();
            for(Wire wire:wires) {
                if(wire.getFrom()==component)contacts.add(wire.getFromAnchor());
                if(wire.getTo()==component)contacts.add(wire.getToAnchor());
            }
            if(contacts.size()>=2 && contacts.stream().map(WireAnchor::getPin).distinct().count()==1)
                return "Оба провода UNO подключены к одной стороне: используйте левый и правый контакты";
        }
        if (!rails && sources.isEmpty()) for (Component component : visited) {
            if (component instanceof ArduinoUNO) sources.add(component);
        }
        if (sources.size() + (rails ? 1 : 0) > 1) return "Два источника питания в одной цепи: оставьте шины, одну батарейку или один UNO. UNO с внешним питанием работает как учебный ключ";
        return null;
    }

    public static String explainOpen(LED led, List<Wire> wires) {
        long contacts = wires.stream().filter(w -> w.getFrom() == led || w.getTo() == led).count();
        if (contacts < 2) return "LED: подключите два провода и замкните цепь через источник питания";
        Set<Component> seen=new HashSet<>(); Queue<Component> queue=new ArrayDeque<>();
        seen.add(led);queue.add(led);
        while(!queue.isEmpty()) {
            Component current=queue.remove();
            if(current instanceof ArduinoUNO uno && !controllerClosed(uno)) return uno.isPowered()
                ? "UNO: D13 = LOW — цепь выключена программой" : "UNO выключен — нажмите на плату для включения";
            if(current instanceof Button b && !b.isPressed()) return "Кнопка отпущена — нажмите её, чтобы замкнуть цепь";
            if(current instanceof Timer t && !t.getOutput()) return t.isActive() && t.isRunning()
                ? "Таймер: пауза между импульсами" : "Таймер выключен — включите его";
            for(Component next:neighbors(current,wires)) if(seen.add(next)) queue.add(next);
        }
        return "Цепь разомкнута или нет питания: проверьте возврат к GND/минусу и разные контакты UNO";
    }

    public static Result analyzeLed(LED led, List<Component> components, List<Wire> wires) {
        if (led == null || wires == null || components == null || limitation(led, wires) != null) {
            return Result.open();
        }

        Result best = fromNetwork(led, null, CircuitKind.POWER_RAIL, CircuitPhysics.SUPPLY_V, wires);
        for (Component component : components) {
            Result candidate = Result.open();
            if (component instanceof Battery battery) {
                candidate = fromNetwork(led, battery, CircuitKind.BATTERY, battery.getVoltage(), wires);
            } else if (component instanceof ArduinoUNO uno && uno.isPowered() && (!uno.hasProgram() || uno.getOutput())) {
                if (hasExternalSupply(uno, wires)) continue;
                candidate = fromNetwork(led, uno, CircuitKind.LOGIC, CircuitPhysics.SUPPLY_V, wires);
            }
            if (candidate.isClosed() && (!best.isClosed() || current(candidate) > current(best))) best = candidate;
        }
        return best;
    }

    private static double current(Result result) {
        return CircuitPhysics.currentMilliAmps(result.supplyVolts(),result.seriesOhms(),CircuitPhysics.LED_VF);
    }

    private static Result fromNetwork(LED led, Component source, CircuitKind kind, double volts, List<Wire> wires) {
        PassiveNetwork.Equivalent equivalent = PassiveNetwork.analyze(led,source,volts,wires);
        if (equivalent == null || equivalent.volts() < 1e-9) return Result.open();
        return new Result(kind,equivalent.ohms(),equivalent.ohms()-equivalent.wireOhms()<1e-6,
            equivalent.volts(),equivalent.wireOhms());
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

        boolean burn = calc.getSafety() == CircuitPhysics.SafetyLevel.OVERLOAD
            || calc.getSafety() == CircuitPhysics.SafetyLevel.NO_RESISTOR;
        boolean glow = !burn && CircuitPhysics.isLedGlowing(calc.getCurrentMa());
        return new LedOutcome(circuit, calc, glow, burn, circuit.missingResistorWarning());
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

    /** Два резистора с проводами ножка1–ножка1 и ножка2–ножка2 включены параллельно. */
    public static boolean areResistorsParallel(Resistor r1, Resistor r2, List<Wire> wires) {
        return (hasResistorPinWire(r1, 1, r2, 1, wires)
            && hasResistorPinWire(r1, 2, r2, 2, wires))
            || (hasResistorPinWire(r1, 1, r2, 2, wires)
            && hasResistorPinWire(r1, 2, r2, 1, wires));
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
