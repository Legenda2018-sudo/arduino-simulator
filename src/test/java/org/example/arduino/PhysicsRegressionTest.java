package org.example.arduino;

import javafx.application.Platform;
import org.example.arduino.model.*;
import org.example.arduino.model.Component;
import org.example.arduino.util.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsRegressionTest {
    @BeforeAll static void toolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) { }
        Platform.setImplicitExit(false);
    }
    @Test void belowThresholdIsZeroEvenWithNoResistance() {
        assertEquals(0, CircuitPhysics.currentMilliAmps(1.5,0,2));
        assertEquals(0, CircuitPhysics.currentMilliAmps(1.5,220,2));
        assertEquals(0, CircuitPhysics.recommendedResistorOhms(1.5,2,20));
        assertEquals(CircuitPhysics.SafetyLevel.TOO_LOW,CircuitPhysics.analyze(1.5,0,2).getSafety());
        assertThrows(IllegalArgumentException.class,()->CircuitPhysics.currentMilliAmps(Double.NaN,220,2));
        Resistor resistor=new Resistor(100,100);
        assertThrows(IllegalArgumentException.class,()->resistor.setResistance(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->resistor.setResistance(Double.POSITIVE_INFINITY));
        assertEquals(1000.0*3/220,CircuitPhysics.currentMilliAmps(5,220,2),1e-10);
    }
    @Test void threeParallelResistorsMatchAnalyticalConductanceIncludingWireResistance() {
        Battery b=new Battery(40,100); b.setVoltage(5);
        LED led=new LED(600,100);
        Resistor r1=new Resistor(200,100),r2=new Resistor(300,100),r3=new Resistor(400,100);
        for(Resistor r:List.of(r1,r2,r3)) r.setResistance(220);
        List<Component> cs=List.of(b,led,r1,r2,r3);
        List<Wire> ws=new ArrayList<>();
        // Three separate resistor branches share the battery terminal and one resistor terminal as a junction.
        Wire feed=new Wire(WireAnchor.component(b,2),WireAnchor.component(r1,1));
        Wire bridge21=new Wire(WireAnchor.component(r1,1),WireAnchor.component(r2,1));
        Wire bridge31=new Wire(WireAnchor.component(r1,1),WireAnchor.component(r3,1));
        Wire bridge22=new Wire(WireAnchor.component(r1,2),WireAnchor.component(r2,2));
        Wire bridge32=new Wire(WireAnchor.component(r1,2),WireAnchor.component(r3,2));
        Wire load=new Wire(WireAnchor.component(r1,2),WireAnchor.componentCenter(led));
        Wire back=new Wire(WireAnchor.componentCenter(led),WireAnchor.component(b,1));
        ws.addAll(List.of(feed,bridge21,bridge31,bridge22,bridge32,load,back));
        double parallel=1/(1/220.0+1/(220+bridge21.getResistanceOhms()+bridge22.getResistanceOhms())
            +1/(220+bridge31.getResistanceOhms()+bridge32.getResistanceOhms()));
        double expected=feed.getResistanceOhms()+parallel+load.getResistanceOhms()+back.getResistanceOhms();
        var result=CircuitAnalyzer.analyzeLed(led,cs,ws);
        assertTrue(result.isClosed());
        assertEquals(expected,result.seriesOhms(),1e-6);
        Collections.reverse(ws);
        assertEquals(expected,CircuitAnalyzer.analyzeLed(led,cs,ws).seriesOhms(),1e-6);
    }
    @Test void resistorSameLegIsBypassedNotCounted() {
        Battery b=new Battery(50,100); b.setVoltage(5);
        Resistor r=new Resistor(200,100); r.setResistance(220);
        LED led=new LED(300,100);
        List<Wire> ws=List.of(new Wire(WireAnchor.component(b,2),WireAnchor.component(r,1)),
            new Wire(WireAnchor.component(r,1),WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led),WireAnchor.component(b,1)));
        var out=CircuitAnalyzer.computeLedOutcome(led,List.of(b,r,led),ws);
        assertNotNull(out); assertTrue(out.burn()); assertFalse(out.glow());
        assertEquals(ws.stream().mapToDouble(Wire::getResistanceOhms).sum(),out.circuit().seriesOhms(),1e-6);
    }
    @Test void sameBatteryTerminalDoesNotSupplyVoltageAndDanglingLedIsOpen() {
        Battery b=new Battery(50,100); LED led=new LED(300,100);
        List<Wire> ws=List.of(new Wire(WireAnchor.component(b,2),WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led),WireAnchor.component(b,2)));
        assertFalse(CircuitAnalyzer.analyzeLed(led,List.of(b,led),ws).isClosed());
        assertNull(CircuitAnalyzer.computeLedOutcome(led,List.of(b,led),ws.subList(0,1)));
    }
    @Test void railAndBatteryWithoutResistorHaveSameOverloadRule() {
        Battery b=new Battery(50,100); b.setVoltage(5); LED led=new LED(300,100);
        var out=CircuitAnalyzer.computeLedOutcome(led,List.of(b,led),List.of(
            new Wire(WireAnchor.component(b,2),WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led),WireAnchor.component(b,1))));
        assertNotNull(out);assertTrue(out.burn());assertFalse(out.glow());
    }
    @Test void voltageDividerUsesTheveninVoltageRatherThanFullSourceVoltage() {
        Battery b=new Battery(40,100);b.setVoltage(5);
        Resistor upper=new Resistor(200,100),lower=new Resistor(400,100);
        upper.setResistance(220);lower.setResistance(220);
        LED led=new LED(600,100);
        Wire feed=new Wire(WireAnchor.component(b,2),WireAnchor.component(upper,1));
        Wire bridge=new Wire(WireAnchor.component(upper,2),WireAnchor.component(lower,1));
        Wire ground=new Wire(WireAnchor.component(lower,2),WireAnchor.component(b,1));
        Wire load=new Wire(WireAnchor.component(lower,1),WireAnchor.componentCenter(led));
        Wire back=new Wire(WireAnchor.componentCenter(led),WireAnchor.component(b,1));
        var result=CircuitAnalyzer.analyzeLed(led,List.of(b,upper,lower,led),List.of(feed,bridge,ground,load,back));
        double a=220+feed.getResistanceOhms()+bridge.getResistanceOhms();
        double c=220+ground.getResistanceOhms();
        assertEquals(5*c/(a+c),result.supplyVolts(),1e-7);
        assertEquals(a*c/(a+c)+load.getResistanceOhms()+back.getResistanceOhms(),result.seriesOhms(),1e-7);
    }
    @Test void directShortIsReportedButResistorLoadIsNotShort() {
        assertNotNull(CircuitAnalyzer.shortCircuitWarning(List.of(),List.of(new Wire(WireAnchor.railPlus(200),WireAnchor.railMinus(400)))));
        Battery b=new Battery(40,100);Resistor r=new Resistor(200,100);
        assertNull(CircuitAnalyzer.shortCircuitWarning(List.of(b,r),List.of(
            new Wire(WireAnchor.component(b,2),WireAnchor.component(r,1)),
            new Wire(WireAnchor.component(r,2),WireAnchor.component(b,1)))));
        assertNotNull(CircuitAnalyzer.shortCircuitWarning(List.of(b,r),List.of(
            new Wire(WireAnchor.component(b,2),WireAnchor.component(r,1)),
            new Wire(WireAnchor.component(r,1),WireAnchor.component(b,1)))));
    }
    @Test void ambiguousLedAndMixedSourcesAreExplicitlyRejected() {
        Battery b=new Battery(50,100);LED led=new LED(200,100);
        List<Wire> wires=List.of(new Wire(WireAnchor.component(b,2),WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led),WireAnchor.railMinus(200)));
        assertNotNull(CircuitAnalyzer.limitation(led,wires));
        assertNull(CircuitAnalyzer.computeLedOutcome(led,List.of(b,led),wires));
    }
}
