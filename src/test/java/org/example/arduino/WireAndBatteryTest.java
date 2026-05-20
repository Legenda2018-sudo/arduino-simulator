package org.example.arduino;

import org.example.arduino.model.Battery;
import org.example.arduino.model.Component;
import org.example.arduino.model.LED;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Wire;
import org.example.arduino.model.WireAnchor;
import org.example.arduino.util.CircuitAnalyzer;
import org.example.arduino.util.CircuitPhysics;
import org.example.arduino.util.WirePhysics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireAndBatteryTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
    }

    private static double totalWireOhms(List<Wire> wires) {
        return wires.stream().mapToDouble(Wire::getResistanceOhms).sum();
    }

    @Test
    void wireResistanceFollowsFormula() {
        WireAnchor a = WireAnchor.componentCenter(new LED(0, 0));
        WireAnchor b = WireAnchor.componentCenter(new LED(100, 0));
        double length = WirePhysics.lengthPixels(a, b);
        double ohms = WirePhysics.resistanceOhms(length);
        assertEquals(100.0, length, 0.01);
        assertEquals(100.0 * WirePhysics.OHMS_PER_PIXEL, ohms, 0.001);

        Wire wire = new Wire(a, b);
        assertEquals(ohms, wire.getResistanceOhms(), 0.001);
    }

    @Test
    void powerRailIncludesWireResistance() {
        Resistor r1 = new Resistor(100, 150);
        r1.setResistance(100);
        Resistor r2 = new Resistor(150, 150);
        r2.setResistance(120);
        LED led = new LED(200, 200);
        List<Component> comps = List.of(r1, r2, led);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(100), WireAnchor.componentCenter(r1)));
        wires.add(new Wire(WireAnchor.componentCenter(r1), WireAnchor.componentCenter(r2)));
        wires.add(new Wire(WireAnchor.componentCenter(r2), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        CircuitAnalyzer.Result r = CircuitAnalyzer.analyzeLed(led, comps, wires);
        assertTrue(r.isClosed());
        assertEquals(220.0 + totalWireOhms(wires), r.seriesOhms(), 0.01);
        assertTrue(r.wireOhms() > 0);
    }

    @Test
    void battery15VDoesNotBurnLed() {
        Battery bat = new Battery(50, 200);
        bat.setVoltage(1.5);
        Resistor res = new Resistor(150, 200);
        res.setResistance(220);
        LED led = new LED(250, 200);
        List<Component> comps = List.of(bat, res, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(bat, 1), WireAnchor.component(res, 1)),
            new Wire(WireAnchor.component(res, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(bat, 2))
        );
        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertFalse(outcome.burn());
        assertFalse(outcome.glow());
        assertEquals(CircuitPhysics.SafetyLevel.TOO_LOW, outcome.calc().getSafety());
    }

    @Test
    void battery9VUsesCustomSupplyInFormula() {
        Battery bat = new Battery(50, 200);
        bat.setVoltage(9.0);
        Resistor res = new Resistor(150, 200);
        res.setResistance(220);
        LED led = new LED(250, 200);
        List<Component> comps = List.of(bat, res, led);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.component(bat, 1), WireAnchor.component(res, 1)));
        wires.add(new Wire(WireAnchor.component(res, 2), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.component(bat, 2)));

        CircuitAnalyzer.Result circuit = CircuitAnalyzer.analyzeLed(led, comps, wires);
        assertEquals(CircuitAnalyzer.CircuitKind.BATTERY, circuit.kind());
        assertEquals(9.0, circuit.supplyVolts(), 0.01);

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.burn());
        double expectedI = CircuitPhysics.currentMilliAmps(9.0, circuit.ohmsForFormula(), CircuitPhysics.LED_VF);
        assertEquals(expectedI, outcome.calc().getCurrentMa(), 0.5);
        assertTrue(expectedI > CircuitPhysics.currentMilliAmps(5.0, 220, CircuitPhysics.LED_VF));
    }
}
