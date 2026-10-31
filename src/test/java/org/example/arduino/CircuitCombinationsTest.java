package org.example.arduino;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.example.arduino.model.*;
import org.example.arduino.model.Component;
import org.example.arduino.model.Timer;
import org.example.arduino.util.CircuitAnalyzer;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Enumerates the supported educational series circuits, not arbitrary real electronics. */
class CircuitCombinationsTest {
    @BeforeAll static void toolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) { }
        Platform.setImplicitExit(false);
    }
    static List<List<Integer>> permutations(List<Integer> items) {
        if(items.isEmpty()) return List.of(List.of());
        List<List<Integer>> result=new ArrayList<>();
        for(int value:items) {
            List<Integer> rest=new ArrayList<>(items);rest.remove(Integer.valueOf(value));
            for(List<Integer> tail:permutations(rest)) {
                List<Integer> next=new ArrayList<>();next.add(value);next.addAll(tail);result.add(next);
            }
        }
        return result;
    }
    private record Scheme(List<Component> components,List<Wire> wires,LED led) { }
    private static Scheme series(int supply,boolean timer,boolean pressed,int gate,List<Integer> order,boolean reverse) {
        List<Component> cs=new ArrayList<>();
        Component source=supply==0?null:supply==1?new Battery(60,100):new ArduinoUNO(60,100);
        if(source instanceof Battery b)b.setVoltage(5);
        if(source instanceof ArduinoUNO u)u.setPowered(true);
        if(source!=null)cs.add(source);
        Resistor r=new Resistor(200,100);r.setResistance(220);
        LED led=new LED(400,100);
        Component sw;
        if(timer) {
            Timer t=new Timer(600,100);t.setActive(pressed);if(pressed)t.start();sw=t;
        } else {
            org.example.arduino.model.Button b=new org.example.arduino.model.Button(600,100);b.setPressed(pressed);sw=b;
        }
        List<Component> chain=new ArrayList<>(List.of(r,led,sw));
        if(gate>=0) {
            ArduinoUNO uno=new ArduinoUNO(800,100);
            if(gate==2||gate==3)uno.setProgramSource("void setup(){pinMode(13,OUTPUT);digitalWrite(13,"+(gate==2?"HIGH":"LOW")+");} void loop(){}");
            uno.setPowered(gate!=0);uno.tickProgram(0);chain.add(uno);
        }
        cs.addAll(chain);
        WireAnchor at=source==null?WireAnchor.railPlus(100):WireAnchor.component(source,2);
        List<Wire> ws=new ArrayList<>();
        for(int index:order) {
            Component c=chain.get(index);
            WireAnchor in=c instanceof Resistor||c instanceof ArduinoUNO?WireAnchor.component(c,1):WireAnchor.componentCenter(c);
            WireAnchor out=c instanceof Resistor||c instanceof ArduinoUNO?WireAnchor.component(c,2):WireAnchor.componentCenter(c);
            ws.add(reverse?new Wire(in,at):new Wire(at,in));at=out;
        }
        WireAnchor end=source==null?WireAnchor.railMinus(900):WireAnchor.component(source,1);
        ws.add(reverse?new Wire(end,at):new Wire(at,end));
        if(reverse)Collections.reverse(ws);
        return new Scheme(cs,ws,led);
    }
    private static void verify(Scheme s,boolean on) {
        assertNull(CircuitAnalyzer.limitation(s.led,s.wires));
        assertNull(CircuitAnalyzer.shortCircuitWarning(s.components,s.wires));
        var result=CircuitAnalyzer.computeLedOutcome(s.led,s.components,s.wires);
        if(on) {
            assertNotNull(result);assertTrue(result.glow());assertFalse(result.burn());
            double expected=220+s.wires.stream().mapToDouble(Wire::getResistanceOhms).sum();
            assertEquals(expected,result.circuit().seriesOhms(),1e-5);
            assertEquals(3000/expected,result.calc().getCurrentMa(),1e-5);
        } else {
            assertNull(result);
            assertFalse(CircuitAnalyzer.explainOpen(s.led,s.wires).isBlank());
        }
    }
    @TestFactory Stream<DynamicTest> basicSeriesPermutations() {
        List<DynamicTest> tests=new ArrayList<>();
        for(int supply=0;supply<3;supply++)for(boolean timer:List.of(false,true))
            for(boolean pressed:List.of(false,true))for(boolean reverse:List.of(false,true))
                for(var order:permutations(List.of(0,1,2))) {
                    int power=supply;
                    tests.add(DynamicTest.dynamicTest("source="+supply+" timer="+timer+" on="+pressed+" reverse="+reverse+" order="+order,
                        ()->verify(series(power,timer,pressed,-1,order,reverse),pressed)));
                }
        return tests.stream();
    }
    @TestFactory Stream<DynamicTest> externalSupplyWithUnoInEveryPosition() {
        List<DynamicTest> tests=new ArrayList<>();
        for(int supply=0;supply<2;supply++)for(int gate=0;gate<4;gate++)for(boolean pressed:List.of(false,true))
            for(boolean reverse:List.of(false,true))for(var order:permutations(List.of(0,1,2,3))) {
                int power=supply,mode=gate;
                tests.add(DynamicTest.dynamicTest("external="+power+" UNO="+mode+" button="+pressed+" reverse="+reverse+" order="+order,
                    ()->verify(series(power,false,pressed,mode,order,reverse),pressed&&(mode==1||mode==2))));
            }
        return tests.stream();
    }
    @Test void screenshotChainIsAcceptedByActualControllerAndBothAnchorSidesAreStable() throws Exception {
        java.util.concurrent.FutureTask<Void> task=new java.util.concurrent.FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(HelloApplication.class.getResource("arduino-view.fxml"));
            javafx.scene.Parent root=loader.load();
            new javafx.scene.Scene(root,1280,920);root.applyCss();root.layout();
            ArduinoController controller=loader.getController();
            Scheme scheme=series(0,false,true,1,List.of(0,1,2,3),false);
            var cf=ArduinoController.class.getDeclaredField("components");cf.setAccessible(true);
            var wf=ArduinoController.class.getDeclaredField("wires");wf.setAccessible(true);
            cf.set(controller,new ArrayList<>(scheme.components));wf.set(controller,new ArrayList<>(scheme.wires));
            var validate=ArduinoController.class.getDeclaredMethod("validateCircuitForSimulation");validate.setAccessible(true);
            assertNull(validate.invoke(controller));
            var simulate=ArduinoController.class.getDeclaredMethod("simulate");simulate.setAccessible(true);simulate.invoke(controller);
            assertTrue(scheme.led.isOn());
            String screenshot=System.getProperty("arduino.circuit.snapshot");
            if(screenshot!=null) {
                javafx.scene.image.WritableImage image=root.snapshot(null,null);
                java.awt.image.BufferedImage png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                javax.imageio.ImageIO.write(png,"png",new java.io.File(screenshot));
            }
            var anchor=ArduinoController.class.getDeclaredMethod("connectionAnchor",Component.class,Component.class,javafx.scene.input.MouseEvent.class);anchor.setAccessible(true);
            ArduinoUNO uno=(ArduinoUNO)scheme.components.stream().filter(c->c instanceof ArduinoUNO).findFirst().orElseThrow();
            assertEquals(1,((WireAnchor)anchor.invoke(controller,uno,new LED(uno.getX()-100,100),null)).getPin());
            assertEquals(2,((WireAnchor)anchor.invoke(controller,uno,new LED(uno.getX()+100,100),null)).getPin());
            return null;
        });
        Platform.runLater(task);task.get(15,java.util.concurrent.TimeUnit.SECONDS);
    }
    @Test void ledItselfIsNotReportedAsShortCircuit() {
        ArduinoUNO source=new ArduinoUNO(50,100);source.setPowered(true);LED led=new LED(400,100);
        List<Wire> ws=List.of(new Wire(WireAnchor.component(source,2),WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led),WireAnchor.component(source,1)));
        assertNull(CircuitAnalyzer.shortCircuitWarning(List.of(source,led),ws));
        assertTrue(CircuitAnalyzer.computeLedOutcome(led,List.of(source,led),ws).burn());
    }
    @Test void unrelatedUnconnectedResistorDoesNotDisableWorkingChain() {
        Scheme s=series(0,false,true,1,List.of(0,1,2,3),false);
        Resistor dangling=new Resistor(100,400);LED isolated=new LED(300,400);
        List<Component> cs=new ArrayList<>(s.components);cs.add(dangling);cs.add(isolated);
        List<Wire> ws=new ArrayList<>(s.wires);ws.add(new Wire(WireAnchor.componentCenter(dangling),WireAnchor.componentCenter(isolated)));
        var expected=CircuitAnalyzer.computeLedOutcome(s.led,s.components,s.wires);
        var actual=CircuitAnalyzer.computeLedOutcome(s.led,cs,ws);
        assertNotNull(actual);assertTrue(actual.glow());
        assertEquals(expected.calc().getCurrentMa(),actual.calc().getCurrentMa(),1e-8);
    }
    @Test void externalUnoBlinkRespondsToBothProgramPhases() {
        Scheme s=series(0,false,true,2,List.of(0,1,2,3),false);
        ArduinoUNO uno=(ArduinoUNO)s.components.stream().filter(c->c instanceof ArduinoUNO).findFirst().orElseThrow();
        uno.setProgramSource(ArduinoUNO.BLINK_PROGRAM);uno.tickProgram(0);verify(s,true);
        uno.tickProgram(500);verify(s,false);
        uno.tickProgram(1000);verify(s,true);
        uno.setPowered(false);verify(s,false);
        uno.setPowered(true);uno.tickProgram(2000);verify(s,true);
    }
    @Test void sameUnoContactGetsActionableMessage() {
        Scheme s=series(0,false,true,1,List.of(0,1,2,3),false);
        List<Wire> ws=new ArrayList<>(s.wires);
        Wire last=ws.get(ws.size()-1);
        ws.set(ws.size()-1,new Wire(WireAnchor.component(last.getFrom(),1),last.getToAnchor()));
        assertTrue(CircuitAnalyzer.limitation(s.led,ws).contains("одной стороне"));
    }
}
