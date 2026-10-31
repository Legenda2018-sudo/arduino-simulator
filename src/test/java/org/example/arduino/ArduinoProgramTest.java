package org.example.arduino;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.example.arduino.model.*;
import org.example.arduino.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ArduinoProgramTest {
    @BeforeAll static void toolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) { }
        Platform.setImplicitExit(false);
    }
    private static ArduinoUNO blink() {
        ArduinoUNO uno = new ArduinoUNO(80, 180);
        uno.setProgramSource(ArduinoUNO.BLINK_PROGRAM);
        uno.setPowered(true);
        return uno;
    }
    @Test void blinkUsesNonBlockingDelaysAndRestartsOnPowerCycle() {
        ArduinoUNO uno = blink();
        uno.tickProgram(1000); assertTrue(uno.getOutput());
        uno.tickProgram(1499); assertTrue(uno.getOutput());
        uno.tickProgram(1500); assertFalse(uno.getOutput());
        uno.tickProgram(2000); assertTrue(uno.getOutput());
        uno.setPowered(false); assertFalse(uno.getOutput());
        uno.setPowered(true); uno.tickProgram(3000); assertTrue(uno.getOutput());
    }
    @Test void invalidProgramDoesNotReplaceWorkingProgram() {
        ArduinoUNO uno = blink();
        assertThrows(IllegalArgumentException.class, () -> uno.setProgramSource("void setup() {} void loop() { while(true) {} }"));
        assertEquals(ArduinoUNO.BLINK_PROGRAM, uno.getProgramSource());
        uno.tickProgram(0); assertTrue(uno.getOutput());
    }
    @Test void rejectsInvalidArgumentsAndUnknownStatements() {
        for (String command : List.of("digitalWrite(14, HIGH);", "delay(60001);", "delay(-1);", "pinMode(13, FOO);", "digitalWrite(13, HIGH); junk;")) {
            assertThrows(IllegalArgumentException.class, () -> ArduinoProgram.compile("void setup() {} void loop() {" + command + "}"));
        }
    }
    @Test void commentsInputModeAndEmptyLoopAreSafe() {
        ArduinoUNO uno = new ArduinoUNO(0, 0);
        uno.setProgramSource("// comment\nvoid setup() { pinMode(13, INPUT); digitalWrite(13, HIGH); } /* comment */ void loop() {}");
        uno.setPowered(true);
        for (int i = 0; i < 100; i++) uno.tickProgram(i);
        assertFalse(uno.getOutput());
        uno.setProgramSource(""); uno.setInputSignal(true); assertTrue(uno.getOutput());
    }
    @Test void programActuallyControlsLedCircuit() {
        ArduinoUNO uno = blink();
        Resistor resistor = new Resistor(200, 180); resistor.setResistance(220);
        LED led = new LED(300, 180);
        List<Component> components = List.of(uno, resistor, led);
        List<Wire> wires = List.of(
            new Wire(WireAnchor.component(uno, 2), WireAnchor.component(resistor, 1)),
            new Wire(WireAnchor.component(resistor, 2), WireAnchor.componentCenter(led)),
            new Wire(WireAnchor.componentCenter(led), WireAnchor.component(uno, 1)));
        uno.tickProgram(0);
        assertTrue(CircuitAnalyzer.computeLedOutcome(led, components, wires).glow());
        uno.tickProgram(500);
        assertNull(CircuitAnalyzer.computeLedOutcome(led, components, wires));
    }
    @Test void batteryRejectsNonFiniteVoltageAndIsSerialized() {
        Battery battery = new Battery(0,0);
        assertThrows(IllegalArgumentException.class, () -> battery.setVoltage(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> battery.setVoltage(Double.POSITIVE_INFINITY));
        battery.setVoltage(6);
        Circuit circuit = new Circuit("battery"); circuit.addComponent(battery);
        assertEquals("Battery", circuit.getComponentsData().get(0).getType());
        assertEquals(6, circuit.getComponentsData().get(0).getVoltage());
    }
    @Test void duplicateRailAttachmentsAreParallelConductors() {
        Resistor r = new Resistor(200, 180); r.setResistance(220);
        LED led = new LED(300, 180);
        Wire first = new Wire(WireAnchor.component(r,2), WireAnchor.railPlus(300));
        Wire second = new Wire(WireAnchor.component(r,2), WireAnchor.railPlus(600));
        Wire ground = new Wire(WireAnchor.railMinus(200),WireAnchor.componentCenter(led));
        Wire load = new Wire(WireAnchor.componentCenter(led),WireAnchor.component(r,1));
        var result=PowerRailSimulator.analyzeLedPowerPath(led,List.of(r,led),List.of(first,second,ground,load));
        assertTrue(result.closed());
        double expected=220+ground.getResistanceOhms()+load.getResistanceOhms()
            +1/(1/first.getResistanceOhms()+1/second.getResistanceOhms());
        assertEquals(expected,result.seriesOhms(),1e-6);
    }
    @Test void actualControllerRoundTripsProgramBatteryAndRailsAndAcceptsBatteryCircuit() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("arduino-view.fxml"));
            Parent root = loader.load();
            ArduinoController controller = loader.getController();
            Method load = ArduinoController.class.getDeclaredMethod("loadCircuitFromLocalJson",JsonObject.class);
            Method save = ArduinoController.class.getDeclaredMethod("serializeComponentsToJsonArray");
            Method saveWires = ArduinoController.class.getDeclaredMethod("serializeWiresToJsonArray");
            Method validate = ArduinoController.class.getDeclaredMethod("validateCircuitForSimulation");
            for (Method method : List.of(load,save,saveWires,validate)) method.setAccessible(true);
            JsonObject data = com.google.gson.JsonParser.parseString("{\"name\":\"test\",\"components\":[{\"type\":\"Battery\",\"x\":80,\"y\":180,\"voltage\":6},{\"type\":\"Resistor\",\"x\":200,\"y\":180,\"resistance\":220},{\"type\":\"LED\",\"x\":300,\"y\":180}],\"wires\":[{\"fromKind\":\"COMPONENT\",\"fromIndex\":0,\"fromPin\":2,\"toKind\":\"COMPONENT\",\"toIndex\":1,\"toPin\":1},{\"fromKind\":\"COMPONENT\",\"fromIndex\":1,\"fromPin\":2,\"toKind\":\"COMPONENT\",\"toIndex\":2},{\"fromKind\":\"COMPONENT\",\"fromIndex\":2,\"toKind\":\"COMPONENT\",\"toIndex\":0,\"toPin\":1}]} ").getAsJsonObject();
            load.invoke(controller,data);
            assertNull(validate.invoke(controller),"Battery-only circuit must be accepted");
            JsonArray components = (JsonArray) save.invoke(controller);
            assertEquals(6, components.get(0).getAsJsonObject().get("voltage").getAsDouble());
            JsonObject uno = new JsonObject();
            uno.addProperty("type","ArduinoUNO"); uno.addProperty("x",400); uno.addProperty("y",200);
            uno.addProperty("programSource",ArduinoUNO.BLINK_PROGRAM);
            components.add(uno);
            data.add("components",components);
            JsonArray wires = (JsonArray) saveWires.invoke(controller);
            JsonObject railWire = new JsonObject();
            railWire.addProperty("fromKind","RAIL_PLUS"); railWire.addProperty("fromRailX",200);
            railWire.addProperty("toKind","COMPONENT"); railWire.addProperty("toIndex",1); railWire.addProperty("toPin",1);
            wires.add(railWire); data.add("wires",wires);
            load.invoke(controller,data);
            JsonArray saved = (JsonArray) save.invoke(controller);
            assertEquals(ArduinoUNO.BLINK_PROGRAM,saved.get(3).getAsJsonObject().get("programSource").getAsString());
            assertEquals(4, ((JsonArray)saveWires.invoke(controller)).size());
            // Same JSON serializers serve both local files and Firebase.
            assertNotNull(root);
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    @Test void editorAppliesExampleAndRenders() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ArduinoUNO uno = new ArduinoUNO(0, 0);
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            Platform.runLater(() -> {
                javafx.stage.Stage stage = null;
                try {
                    stage = (javafx.stage.Stage) javafx.stage.Window.getWindows().stream()
                        .filter(w -> w instanceof javafx.stage.Stage s && s.getTitle().equals("Настройка Arduino UNO"))
                        .findFirst().orElseThrow();
                    Parent root = stage.getScene().getRoot();
                    javafx.scene.control.Button example = (javafx.scene.control.Button) root.lookupAll(".button").stream()
                        .filter(n -> n instanceof javafx.scene.control.Button b && b.getText().equals("Пример мигания")).findFirst().orElseThrow();
                    example.fire();
                    javafx.scene.control.Button apply = (javafx.scene.control.Button) root.lookupAll(".button").stream()
                        .filter(n -> n instanceof javafx.scene.control.Button b && b.getText().equals("Применить программу")).findFirst().orElseThrow();
                    apply.fire();
                    assertEquals(ArduinoUNO.BLINK_PROGRAM, uno.getProgramSource());
                    // Expand only this test window to inspect the complete editor in the QA artifact.
                    stage.setWidth(600); stage.setHeight(1300);
                    root.applyCss(); root.layout();
                    String output = System.getProperty("arduino.editor.snapshot");
                    if (output != null) {
                        javafx.scene.image.WritableImage image = root.snapshot(null, null);
                        java.awt.image.BufferedImage png = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                        javax.imageio.ImageIO.write(png, "png", new java.io.File(output));
                    }
                } catch (Throwable error) { failure.set(error); }
                finally { if (stage != null) stage.close(); }
            });
            ArduinoConfigWindow.showConfig(uno, List.of(uno), List.of());
            if (failure.get() != null) throw new AssertionError(failure.get());
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }
}
