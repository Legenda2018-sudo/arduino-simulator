package org.example.arduino.model;

import java.util.ArrayList;
import java.util.List;

public class Circuit {
    private String name;
    private List<Component> components;
    private List<Wire> wires;

    public Circuit(String name) {
        this.name = name;
        this.components = new ArrayList<>();
        this.wires = new ArrayList<>();
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void removeComponent(Component component) {
        components.remove(component);
    }

    public void addWire(Wire wire) {
        wires.add(wire);
    }

    public void removeWire(Wire wire) {
        wires.remove(wire);
    }

    public List<Component> getComponents() {
        return components;
    }

    public List<Wire> getWires() {
        return wires;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Метод для получения данных компонентов для сериализации
    public List<ComponentData> getComponentsData() {
        List<ComponentData> data = new ArrayList<>();
        for (Component comp : components) {
            ComponentData compData = new ComponentData();
            compData.setX(comp.getX());
            compData.setY(comp.getY());
            
            if (comp instanceof LED) {
                LED led = (LED) comp;
                compData.setType("LED");
                compData.setState(led.isOn());
                // Сохраняем состояние перегорания в resistance (0 = не перегорел, 1 = перегорел)
                compData.setResistance(led.isBurned() ? 1 : 0);
            } else if (comp instanceof Button) {
                compData.setType("Button");
                compData.setState(((Button) comp).isPressed());
            } else if (comp instanceof Resistor) {
                compData.setType("Resistor");
                compData.setResistance(((Resistor) comp).getResistance());
            } else if (comp instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) comp;
                compData.setType("ArduinoUNO"); // в БД ключ без пробела, иначе при загрузке не распознаётся
                compData.setState(arduino.isPowered());
            } else if (comp instanceof Timer) {
                compData.setType("Timer");
                Timer timer = (Timer) comp;
                compData.setState(timer.isActive());
                // Сохраняем интервал в resistance (в миллисекундах)
                compData.setResistance(timer.getInterval());
            }
            
            data.add(compData);
        }
        return data;
    }

    // Метод для получения данных проводов для сериализации
    public List<WireData> getWiresData() {
        List<WireData> data = new ArrayList<>();
        for (Wire wire : wires) {
            int fromIndex = components.indexOf(wire.getFrom());
            int toIndex = components.indexOf(wire.getTo());
            if (fromIndex >= 0 && toIndex >= 0) {
                data.add(new WireData(fromIndex, toIndex));
            }
        }
        return data;
    }
}


