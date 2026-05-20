package org.example.arduino.util;

import org.example.arduino.model.Component;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.WireAnchor;

/**
 * Безопасное чтение логического сигнала с конца провода (шина +/− или компонент).
 */
public final class WireSignals {

    private WireSignals() {
    }

    public static boolean readSourceSignal(WireAnchor anchor, Component component) {
        if (anchor != null && anchor.isRailPlus()) {
            return true;
        }
        if (anchor != null && anchor.isRailMinus()) {
            return false;
        }
        if (component == null) {
            return false;
        }
        return component.getOutput();
    }

    public static boolean readResistorOutput(Resistor resistor) {
        if (resistor.getResistance() > 5000) {
            return false;
        }
        return resistor.getInput();
    }
}
