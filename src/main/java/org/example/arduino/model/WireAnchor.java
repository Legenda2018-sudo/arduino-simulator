package org.example.arduino.model;

/**
 * Точка подключения провода: шина +, шина − или контакт компонента (центр / ножка резистора).
 */
public class WireAnchor {
    public enum Kind {
        RAIL_PLUS,
        RAIL_MINUS,
        COMPONENT
    }

    private final Kind kind;
    private final Component component;
    /** 0 — центр; 1 — левая ножка резистора; 2 — правая ножка. */
    private final int pin;
    private final double railX;

    private WireAnchor(Kind kind, Component component, int pin, double railX) {
        this.kind = kind;
        this.component = component;
        this.pin = pin;
        this.railX = railX;
    }

    public static WireAnchor railPlus(double x) {
        return new WireAnchor(Kind.RAIL_PLUS, null, 0, BreadboardLayout.snapRailX(x));
    }

    public static WireAnchor railMinus(double x) {
        return new WireAnchor(Kind.RAIL_MINUS, null, 0, BreadboardLayout.snapRailX(x));
    }

    public static WireAnchor component(Component component, int pin) {
        return new WireAnchor(Kind.COMPONENT, component, pin, 0);
    }

    public static WireAnchor componentCenter(Component component) {
        return component(component, 0);
    }

    public Kind getKind() {
        return kind;
    }

    public Component getComponent() {
        return component;
    }

    public int getPin() {
        return pin;
    }

    public double getRailX() {
        return railX;
    }

    public double getX() {
        if (kind == Kind.RAIL_PLUS) {
            return railX;
        }
        if (kind == Kind.RAIL_MINUS) {
            return railX;
        }
        if (component == null) {
            return 0;
        }
        if (component instanceof Resistor resistor) {
            if (pin == 1) {
                return resistor.getLeg1X();
            }
            if (pin == 2) {
                return resistor.getLeg2X();
            }
        }
        if (component instanceof ArduinoUNO) {
            if (pin == 1) {
                return component.getX() - 52;
            }
            if (pin == 2) {
                return component.getX() + 52;
            }
        }
        return component.getX();
    }

    public double getY() {
        if (kind == Kind.RAIL_PLUS) {
            return BreadboardLayout.RAIL_PLUS_CENTER_Y;
        }
        if (kind == Kind.RAIL_MINUS) {
            return BreadboardLayout.RAIL_MINUS_CENTER_Y;
        }
        if (component == null) {
            return 0;
        }
        if (component instanceof Resistor resistor && (pin == 1 || pin == 2)) {
            return resistor.getLegY();
        }
        return component.getY();
    }

    public boolean isRailPlus() {
        return kind == Kind.RAIL_PLUS;
    }

    public boolean isRailMinus() {
        return kind == Kind.RAIL_MINUS;
    }

    public boolean isSameEndpoint(WireAnchor other) {
        if (other == null) {
            return false;
        }
        if (kind != other.kind) {
            return false;
        }
        if (kind == Kind.RAIL_PLUS || kind == Kind.RAIL_MINUS) {
            return Math.abs(railX - other.railX) < 0.5;
        }
        return component == other.component && pin == other.pin;
    }

    public String getDisplayName() {
        if (kind == Kind.RAIL_PLUS) {
            return "шина + (5V)";
        }
        if (kind == Kind.RAIL_MINUS) {
            return "шина − (GND)";
        }
        if (component == null) {
            return "?";
        }
        if (component instanceof Resistor && pin == 1) {
            return "резистор (ножка 1)";
        }
        if (component instanceof Resistor && pin == 2) {
            return "резистор (ножка 2)";
        }
        if (component instanceof ArduinoUNO && pin == 1) {
            return "Arduino (левый контакт)";
        }
        if (component instanceof ArduinoUNO && pin == 2) {
            return "Arduino (правый контакт)";
        }
        return component.getType();
    }
}
