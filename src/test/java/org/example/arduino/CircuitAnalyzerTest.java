package org.example.arduino;

import javafx.application.Platform;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Button;
import org.example.arduino.model.Component;
import org.example.arduino.model.LED;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Wire;
import org.example.arduino.model.WireAnchor;
import org.example.arduino.util.CircuitAnalyzer;
import org.example.arduino.util.CircuitPhysics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitAnalyzerTest {

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
    void onlyPlusRailWithoutGndIsOpen() {
        LED led = new LED(200, 200);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200))
        );
        CircuitAnalyzer.Result r = CircuitAnalyzer.analyzeLed(led, List.of(led), wires);
        assertFalse(r.isClosed());
        assertEquals(CircuitAnalyzer.CircuitKind.OPEN, r.kind());
    }

    @Test
    void twoResistorsInSeriesOnPowerRail() {
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
        assertEquals(CircuitAnalyzer.CircuitKind.POWER_RAIL, r.kind());
        assertEquals(220.0 + totalWireOhms(wires), r.seriesOhms(), 0.01);

        double iMa = CircuitPhysics.currentMilliAmps(
            CircuitPhysics.SUPPLY_V, r.ohmsForFormula(), CircuitPhysics.LED_VF);
        assertTrue(iMa <= CircuitPhysics.LED_I_MAX_MA);
    }

    @Test
    void eachLedAnalyzedSeparately() {
        Resistor r = new Resistor(150, 150);
        r.setResistance(220);
        LED led1 = new LED(100, 200);
        LED led2 = new LED(200, 200);
        List<Component> comps = List.of(r, led1, led2);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(150), WireAnchor.componentCenter(r)));
        wires.add(new Wire(WireAnchor.componentCenter(r), WireAnchor.componentCenter(led1)));
        wires.add(new Wire(WireAnchor.componentCenter(led1), WireAnchor.railPlus(100)));
        wires.add(new Wire(WireAnchor.railMinus(200), WireAnchor.componentCenter(led2)));
        wires.add(new Wire(WireAnchor.componentCenter(led2), WireAnchor.railPlus(200)));

        CircuitAnalyzer.Result r1 = CircuitAnalyzer.analyzeLed(led1, comps, wires);
        CircuitAnalyzer.Result r2 = CircuitAnalyzer.analyzeLed(led2, comps, wires);

        assertTrue(r1.isClosed());
        double led1WireR = wires.get(0).getResistanceOhms() + wires.get(1).getResistanceOhms() + wires.get(2).getResistanceOhms();
        assertEquals(220.0 + led1WireR, r1.seriesOhms(), 0.01);
        assertTrue(r2.isClosed());
        double led2WireR = wires.get(3).getResistanceOhms() + wires.get(4).getResistanceOhms();
        assertEquals(led2WireR, r2.seriesOhms(), 0.01);
    }

    @Test
    void powerRailWithoutResistorBurns() {
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(200), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, List.of(led), wires);
        assertNotNull(outcome);
        assertTrue(outcome.burn());
        // Провода дают небольшое R — формула даёт OVERLOAD, не NO_RESISTOR (R > 0)
        assertEquals(CircuitPhysics.SafetyLevel.OVERLOAD, outcome.calc().getSafety());
    }

    @Test
    void powerRail220OhmNormalCurrent() {
        Resistor r = new Resistor(150, 150);
        r.setResistance(220);
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(150), WireAnchor.componentCenter(r)));
        wires.add(new Wire(WireAnchor.componentCenter(r), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(
            led, List.of(r, led), wires);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertFalse(outcome.burn());
        assertEquals(CircuitPhysics.SafetyLevel.NORMAL, outcome.calc().getSafety());
        assertTrue(outcome.calc().getCurrentMa() > 10 && outcome.calc().getCurrentMa() < 20);
    }

    @Test
    void arduinoDirectWithoutResistorNeedsClosedLoop() {
        ArduinoUNO uno = new ArduinoUNO(50, 50);
        uno.setPowered(true);
        LED led = new LED(150, 50);
        List<Component> comps = List.of(uno, led);
        List<Wire> oneWire = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(led))
        );
        assertFalse(CircuitAnalyzer.analyzeLed(led, comps, oneWire).isClosed());

        List<Wire> loop = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(uno, 1))
        );
        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, loop);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertTrue(outcome.resistorWarning());
    }

    @Test
    void unoParallelWiresToLedAndButton_buttonOff_noGlow() {
        ArduinoUNO uno = new ArduinoUNO(220, 220);
        uno.setPowered(true);
        Button button = new Button(260, 80);
        button.setPressed(false);
        LED led = new LED(140, 80);
        List<Component> comps = List.of(uno, button, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 1), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led))
        );
        assertFalse(CircuitAnalyzer.analyzeLed(led, comps, wires).isClosed());
        assertNull(CircuitAnalyzer.computeLedOutcome(led, comps, wires));
    }

    @Test
    void unoParallelWiresToLedAndButton_buttonOn_glowsWithWarning() {
        ArduinoUNO uno = new ArduinoUNO(220, 220);
        uno.setPowered(true);
        Button button = new Button(260, 80);
        button.setPressed(true);
        LED led = new LED(140, 80);
        List<Component> comps = List.of(uno, button, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 1), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led))
        );
        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertTrue(outcome.resistorWarning());
    }

    @Test
    void unoLedButtonUno_buttonOff_doesNotGlow() {
        ArduinoUNO uno = new ArduinoUNO(50, 50);
        uno.setPowered(true);
        Button button = new Button(100, 50);
        button.setPressed(false);
        LED led = new LED(150, 50);
        List<Component> comps = List.of(uno, button, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(uno, 1))
        );
        assertNull(CircuitAnalyzer.computeLedOutcome(led, comps, wires));
    }

    @Test
    void unoLedButtonUno_buttonOn_glowsWithWarning() {
        ArduinoUNO uno = new ArduinoUNO(50, 50);
        uno.setPowered(true);
        Button button = new Button(100, 50);
        button.setPressed(true);
        LED led = new LED(150, 50);
        List<Component> comps = List.of(uno, button, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(uno, 1))
        );
        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertTrue(outcome.resistorWarning());
    }

    @Test
    void unoButtonLedOpen_buttonPressed_doesNotGlow() {
        ArduinoUNO uno = new ArduinoUNO(50, 50);
        uno.setPowered(true);
        Button button = new Button(100, 50);
        button.setPressed(true);
        LED led = new LED(150, 50);
        List<Component> comps = List.of(uno, button, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led))
        );
        assertNull(CircuitAnalyzer.computeLedOutcome(led, comps, wires));
    }

    @Test
    void megaOhmResistorLedDoesNotGlow() {
        Resistor r = new Resistor(150, 150);
        r.setResistance(1_000_000);
        LED led = new LED(200, 200);
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire(WireAnchor.railMinus(150), WireAnchor.componentCenter(r)));
        wires.add(new Wire(WireAnchor.componentCenter(r), WireAnchor.componentCenter(led)));
        wires.add(new Wire(WireAnchor.componentCenter(led), WireAnchor.railPlus(200)));

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(
            led, List.of(r, led), wires);
        assertNotNull(outcome);
        assertFalse(outcome.glow());
        assertFalse(outcome.burn());
        assertEquals(CircuitPhysics.SafetyLevel.TOO_LOW, outcome.calc().getSafety());
    }

    @Test
    void buttonStarToManyLedsWithoutResistorNoneGlow() {
        ArduinoUNO uno = new ArduinoUNO(200, 100);
        uno.setPowered(true);
        uno.setInputSignal(true);
        Button button = new Button(150, 150);
        button.setPressed(true);
        LED led1 = new LED(50, 100);
        LED led2 = new LED(50, 150);
        LED led3 = new LED(50, 200);
        List<Component> comps = List.of(uno, button, led1, led2, led3);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 1), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led1)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led2)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(led3))
        );

        for (LED led : List.of(led1, led2, led3)) {
            assertNull(CircuitAnalyzer.computeLedOutcome(led, comps, wires));
        }
    }

    @Test
    void unoButtonLedUno_closed_buttonOn_glowsWithWarning() {
        ArduinoUNO uno = new ArduinoUNO(200, 100);
        uno.setPowered(true);
        Button button = new Button(150, 150);
        button.setPressed(true);
        LED led = new LED(50, 100);
        Resistor r = new Resistor(120, 100);
        r.setResistance(220);
        List<Component> comps = List.of(uno, button, led, r);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.componentCenter(r)),
            new Wire(WireAnchor.componentCenter(r), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(uno, 1))
        );
        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertFalse(outcome.resistorWarning());
    }

    @Test
    void unoOff_openCircuit() {
        ArduinoUNO uno = new ArduinoUNO(200, 100);
        uno.setPowered(false);
        Button button = new Button(360, 100);
        button.setPressed(true);
        Resistor r1 = new Resistor(360, 160);
        r1.setResistance(220);
        Resistor r2 = new Resistor(360, 200);
        r2.setResistance(220);
        LED led = new LED(220, 260);
        List<Component> comps = List.of(uno, button, r1, r2, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(r1, 1), WireAnchor.component(r2, 1)),
            new Wire(WireAnchor.component(r1, 2), WireAnchor.component(r2, 2)),
            new Wire(WireAnchor.component(r1, 1), WireAnchor.component(uno, 1)),
            new Wire(WireAnchor.component(r2, 1), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(uno, 1)),
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(r1, 2))
        );
        assertFalse(CircuitAnalyzer.analyzeLed(led, comps, wires).isClosed());
    }

    @Test
    void unoOn_singleResistorInLoop_overload() {
        ArduinoUNO uno = new ArduinoUNO(200, 100);
        uno.setPowered(true);
        Button button = new Button(360, 100);
        button.setPressed(true);
        Resistor r1 = new Resistor(360, 160);
        r1.setResistance(68);
        Resistor r2 = new Resistor(360, 200);
        r2.setResistance(68);
        LED led = new LED(220, 260);
        List<Component> comps = List.of(uno, button, r1, r2, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(r1, 1), WireAnchor.component(r2, 1)),
            new Wire(WireAnchor.component(r1, 2), WireAnchor.component(r2, 2)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(uno, 1)),
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(r1, 1)),
            new Wire(WireAnchor.component(r1, 1), WireAnchor.componentCenter(led))
        );
        CircuitAnalyzer.Result circuit = CircuitAnalyzer.analyzeLed(led, comps, wires);
        assertTrue(circuit.isClosed());
        assertEquals(68.0, circuit.seriesOhms(), totalWireOhms(wires) + 1.0);

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.burn());
    }

    @Test
    void parallelResistorsInLoop_effective34Ohm_overload() {
        ArduinoUNO uno = new ArduinoUNO(200, 100);
        uno.setPowered(true);
        Button button = new Button(360, 100);
        button.setPressed(true);
        Resistor r1 = new Resistor(360, 160);
        r1.setResistance(68);
        Resistor r2 = new Resistor(360, 200);
        r2.setResistance(68);
        LED led = new LED(180, 240);
        List<Component> comps = List.of(uno, button, r1, r2, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(r1, 1), WireAnchor.component(r2, 1)),
            new Wire(WireAnchor.component(r1, 2), WireAnchor.component(r2, 2)),
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(r1, 1)),
            new Wire(WireAnchor.component(uno, 1), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.component(r2, 1), WireAnchor.componentCenter(led))
        );
        CircuitAnalyzer.Result circuit = CircuitAnalyzer.analyzeLed(led, comps, wires);
        assertTrue(circuit.isClosed());
        assertEquals(34.0, circuit.seriesOhms(), totalWireOhms(wires) + 1.0);

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, comps, wires);
        assertNotNull(outcome);
        assertTrue(outcome.burn());
        assertFalse(outcome.glow());
    }

    @Test
    void parallelResistors_detectedByBothLegWires() {
        Resistor r1 = new Resistor(100, 100);
        r1.setResistance(100);
        Resistor r2 = new Resistor(150, 100);
        r2.setResistance(100);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(r1, 1), WireAnchor.component(r2, 1)),
            new Wire(WireAnchor.component(r1, 2), WireAnchor.component(r2, 2))
        );
        assertTrue(CircuitAnalyzer.areResistorsParallel(r1, r2, wires));
        assertEquals(50.0, CircuitAnalyzer.parallelOhms(100, 100), 0.01);
    }

    @Test
    void railLedWithoutResistor_doesNotAffectUnoCircuitLeds() {
        ArduinoUNO uno = new ArduinoUNO(140, 100);
        uno.setPowered(true);
        Button button = new Button(300, 100);
        button.setPressed(true);
        Resistor r1 = new Resistor(420, 120);
        r1.setResistance(220);
        LED led1 = new LED(329, 222);
        LED led2 = new LED(480, 200);
        LED led3 = new LED(540, 380);
        List<Component> comps = List.of(uno, button, r1, led1, led2, led3);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.componentCenter(button)),
            new Wire(WireAnchor.componentCenter(button), WireAnchor.component(r1, 1)),
            new Wire(WireAnchor.component(uno, 1), WireAnchor.componentCenter(led1)),
            new Wire(WireAnchor.component(r1, 2), WireAnchor.componentCenter(led2)),
            new Wire(WireAnchor.componentCenter(led2), WireAnchor.componentCenter(led1)),
            new Wire(WireAnchor.componentCenter(led2), WireAnchor.component(uno, 1)),
            new Wire(WireAnchor.component(r1, 1), WireAnchor.componentCenter(led1)),
            new Wire(WireAnchor.componentCenter(led3), WireAnchor.railPlus(540)),
            new Wire(WireAnchor.railMinus(540), WireAnchor.componentCenter(led3))
        );

        CircuitAnalyzer.LedOutcome unoLed1 = CircuitAnalyzer.computeLedOutcome(led1, comps, wires);
        CircuitAnalyzer.LedOutcome unoLed2 = CircuitAnalyzer.computeLedOutcome(led2, comps, wires);
        CircuitAnalyzer.LedOutcome railLed3 = CircuitAnalyzer.computeLedOutcome(led3, comps, wires);
        assertNotNull(unoLed1);
        assertNotNull(unoLed2);
        assertNotNull(railLed3);

        assertTrue(unoLed1.glow());
        assertFalse(unoLed1.burn());
        assertTrue(unoLed2.glow());
        assertFalse(unoLed2.burn());
        assertTrue(railLed3.burn());
        assertEquals(CircuitAnalyzer.CircuitKind.LOGIC, unoLed1.circuit().kind());
        assertEquals(CircuitAnalyzer.CircuitKind.POWER_RAIL, railLed3.circuit().kind());
    }

    @Test
    void powerRailLedWithResistorOnPlusSide_glows() {
        Resistor r = new Resistor(380, 400);
        r.setResistance(220);
        LED led = new LED(480, 400);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(r, 1), WireAnchor.railPlus(380)),
            new Wire(WireAnchor.component(r, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.railMinus(480))
        );
        CircuitAnalyzer.Result circuit = CircuitAnalyzer.analyzeLed(led, List.of(r, led), wires);
        assertTrue(circuit.isClosed());
        assertEquals(220.0 + totalWireOhms(wires), circuit.seriesOhms(), 0.01);

        CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, List.of(r, led), wires);
        assertNotNull(outcome);
        assertTrue(outcome.glow());
        assertFalse(outcome.burn());
    }
}
