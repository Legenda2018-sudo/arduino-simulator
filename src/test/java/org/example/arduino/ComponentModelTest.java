package org.example.arduino;

import javafx.application.Platform;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Button;
import org.example.arduino.model.LED;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Timer;
import org.example.arduino.model.Wire;
import org.example.arduino.model.WireAnchor;
import org.example.arduino.util.CircuitPhysics;
import org.example.arduino.util.PowerRailSimulator;
import org.example.arduino.util.ResistorColorCode;
import org.example.arduino.util.WireSignals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentModelTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
    }

    @Test
    void resistorPinAtAndContainsDoNotRecurse() {
        Resistor resistor = new Resistor(100, 100);
        assertDoesNotThrow(() -> {
            assertEquals(1, resistor.pinAt(resistor.getLeg1X(), resistor.getLegY()));
            assertEquals(2, resistor.pinAt(resistor.getLeg2X(), resistor.getLegY()));
            assertEquals(0, resistor.pinAt(100, 100));
            assertTrue(resistor.contains(100, 100));
            assertFalse(resistor.contains(0, 0));
        });
    }

    @Test
    void railAnchorHasNoComponent() {
        WireAnchor plus = WireAnchor.railPlus(100);
        WireAnchor minus = WireAnchor.railMinus(100);
        assertNull(plus.getComponent());
        assertNull(minus.getComponent());
        assertTrue(plus.isRailPlus());
        assertTrue(minus.isRailMinus());
    }

    @Test
    void wireSignalsFromRailAndComponent() {
        WireAnchor plus = WireAnchor.railPlus(100);
        Button button = new Button(50, 50);
        assertTrue(WireSignals.readSourceSignal(plus, null));
        assertFalse(WireSignals.readSourceSignal(WireAnchor.railMinus(100), null));
        assertFalse(WireSignals.readSourceSignal(WireAnchor.componentCenter(button), button));
        button.setPressed(true);
        assertTrue(WireSignals.readSourceSignal(WireAnchor.componentCenter(button), button));
    }

    @Test
    void wireBetweenComponentAndRail() {
        LED led = new LED(200, 200);
        Wire toPlus = new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200));
        assertEquals(led, toPlus.getFrom());
        assertNull(toPlus.getTo());

        Wire fromPlus = new Wire(WireAnchor.railPlus(200), WireAnchor.componentCenter(led));
        assertNull(fromPlus.getFrom());
        assertEquals(led, fromPlus.getTo());
    }

    @Test
    void ledTurnsOnFromInput() {
        LED led = new LED(10, 10);
        assertFalse(led.isOn());
        led.setInput(true);
        assertTrue(led.isOn());
        led.burn();
        led.setInput(true);
        assertFalse(led.isOn());
        assertTrue(led.isBurned());
    }

    @Test
    void resistorBlocksWhenOpenCircuit() {
        Resistor resistor = new Resistor(0, 0);
        resistor.setInput(false);
        assertFalse(WireSignals.readResistorOutput(resistor));
        resistor.setInput(true);
        assertTrue(WireSignals.readResistorOutput(resistor));
        resistor.setResistance(10000);
        assertFalse(WireSignals.readResistorOutput(resistor));
    }

    @Test
    void resistorE12Step() {
        assertEquals(270, ResistorColorCode.stepE12(220, 1), 0.1);
        assertEquals(220, ResistorColorCode.stepE12(270, -1), 0.1);
    }

    @Test
    void arduinoSideAnchors() {
        ArduinoUNO uno = new ArduinoUNO(200, 200);
        WireAnchor left = WireAnchor.component(uno, 1);
        WireAnchor right = WireAnchor.component(uno, 2);
        assertTrue(left.getX() < uno.getX());
        assertTrue(right.getX() > uno.getX());
    }

    @Test
    void circuitPhysicsAnalyze220Ohm() {
        CircuitPhysics.CalcResult r = CircuitPhysics.analyze(220);
        assertEquals(CircuitPhysics.SafetyLevel.NORMAL, r.getSafety());
        assertTrue(r.getCurrentMa() > 10 && r.getCurrentMa() < 20);
        assertTrue(r.formulaSteps().contains("220"));
    }

    @Test
    void circuitPhysicsOverloadWithoutResistor() {
        CircuitPhysics.CalcResult r = CircuitPhysics.analyze(1);
        assertEquals(CircuitPhysics.SafetyLevel.OVERLOAD, r.getSafety());
    }

    @Test
    void powerRailDirectLedOverload() {
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(200), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        assertTrue(PowerRailSimulator.isTopologicallyBetweenRails(led, wires));
        PowerRailSimulator.PowerResult path = PowerRailSimulator.analyzeLedPowerPath(
            led, List.of(led), wires);
        assertTrue(path.closed());
        assertEquals(1.0, path.seriesOhms(), 0.01);
    }

    @Test
    void powerRailTimerBlocksWhenInactive() {
        Timer timer = new Timer(100, 100);
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(100), WireAnchor.componentCenter(timer)));
        wires.add(new Wire(WireAnchor.componentCenter(timer), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        assertTrue(PowerRailSimulator.isTopologicallyBetweenRails(led, wires));
        PowerRailSimulator.PowerResult path = PowerRailSimulator.analyzeLedPowerPath(
            led, List.of(timer, led), wires);
        assertFalse(path.closed());
    }

    @Test
    void powerRailWithResistorIsSafe() {
        Resistor resistor = new Resistor(150, 150);
        resistor.setResistance(220);
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(150), WireAnchor.componentCenter(resistor)));
        wires.add(new Wire(WireAnchor.componentCenter(resistor), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        PowerRailSimulator.PowerResult path = PowerRailSimulator.analyzeLedPowerPath(
            led, List.of(resistor, led), wires);
        assertTrue(path.closed());
        assertEquals(220.0, path.seriesOhms(), 0.01);
    }
}
