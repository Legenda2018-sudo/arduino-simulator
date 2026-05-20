package org.example.arduino;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.example.arduino.util.CircuitPhysics;
import org.example.arduino.util.ResistorColorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Модульные тесты формата локальной схемы (JSON), совместимого с ArduinoController.
 */
class CircuitJsonTest {

    @Test
    void testSerializeCircuitToJson() {
        JsonObject root = new JsonObject();
        root.addProperty("name", "Demo");
        JsonArray components = new JsonArray();
        JsonObject led = new JsonObject();
        led.addProperty("type", "LED");
        led.addProperty("x", 10.0);
        led.addProperty("y", 20.0);
        led.addProperty("state", false);
        led.addProperty("burned", false);
        components.add(led);
        root.add("components", components);
        root.add("wires", new JsonArray());

        String json = new Gson().toJson(root);
        JsonObject back = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(back.has("components"));
        assertEquals(1, back.getAsJsonArray("components").size());
        assertEquals("LED", back.getAsJsonArray("components").get(0).getAsJsonObject().get("type").getAsString());
        assertTrue(back.getAsJsonArray("wires").isEmpty());
    }

    @Test
    void testDeserializeCircuitFromJson() {
        String json = "{\"name\":\"Local\",\"components\":[{\"type\":\"LED\",\"x\":1,\"y\":2,\"state\":false,\"burned\":false}],\"wires\":[]}";
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("Local", root.get("name").getAsString());
        assertEquals(1, root.getAsJsonArray("components").size());
    }

    @Test
    void testDeserializeInvalidJson() {
        assertThrows(JsonSyntaxException.class, () -> JsonParser.parseString("{not json").getAsJsonObject());
        assertThrows(IllegalStateException.class, () -> JsonParser.parseString("\"oops\"").getAsJsonObject());
    }

    @Test
    void testLedResistorCurrent() {
        double i = CircuitPhysics.currentMilliAmps(5.0, 220, 2.0);
        assertTrue(i > 10 && i < 20);
        assertTrue(CircuitPhysics.isLedSafe(i));
    }

    @Test
    void testResistorColorSnap() {
        assertEquals(220.0, ResistorColorCode.snapToE12(210), 0.1);
        assertEquals(1000.0, ResistorColorCode.snapToE12(950), 0.1);
    }

    @Test
    void testWireIndexesConsistency() {
        JsonObject valid = circuitWithTwoLedsAndWire(0, 1);
        assertTrue(wireIndicesConsistent(valid));

        JsonObject bad = circuitWithTwoLedsAndWire(0, 5);
        assertFalse(wireIndicesConsistent(bad));
    }

    private static JsonObject circuitWithTwoLedsAndWire(int fromIndex, int toIndex) {
        JsonObject root = new JsonObject();
        root.addProperty("name", "W");
        JsonArray components = new JsonArray();
        for (int i = 0; i < 2; i++) {
            JsonObject led = new JsonObject();
            led.addProperty("type", "LED");
            led.addProperty("x", i * 10.0);
            led.addProperty("y", 0.0);
            led.addProperty("state", false);
            led.addProperty("burned", false);
            components.add(led);
        }
        root.add("components", components);
        JsonArray wires = new JsonArray();
        JsonObject w = new JsonObject();
        w.addProperty("fromIndex", fromIndex);
        w.addProperty("toIndex", toIndex);
        wires.add(w);
        root.add("wires", wires);
        return root;
    }

    /** Как в ArduinoController.loadCircuitFromLocalJson: индексы проводов должны попадать в список компонентов. */
    private static boolean wireIndicesConsistent(JsonObject root) {
        if (!root.has("components") || !root.get("components").isJsonArray()) {
            return false;
        }
        if (!root.has("wires") || !root.get("wires").isJsonArray()) {
            return true;
        }
        JsonArray comps = root.getAsJsonArray("components");
        int n = comps.size();
        JsonArray wires = root.getAsJsonArray("wires");
        for (int i = 0; i < wires.size(); i++) {
            JsonObject wireObj = wires.get(i).getAsJsonObject();
            if (!wireObj.has("fromIndex") || !wireObj.has("toIndex")) {
                continue;
            }
            int fromIndex = wireObj.get("fromIndex").getAsInt();
            int toIndex = wireObj.get("toIndex").getAsInt();
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= n || toIndex >= n) {
                return false;
            }
        }
        return true;
    }
}
