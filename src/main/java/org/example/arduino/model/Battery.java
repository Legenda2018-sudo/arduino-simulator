package org.example.arduino.model;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Источник питания с настраиваемым напряжением U (как номинал у резистора).
 * Клемма + (правая), клемма − (левая).
 */
public class Battery extends Component {
    private static final double LEG_OFFSET_X = 32;
    private static final double BODY_W = 48;
    private static final double BODY_H = 28;

    private double voltage;
    private final Rectangle body;
    private final Circle plus;
    private final Circle minus;
    private final Text label;

    public Battery(double x, double y) {
        super(x, y, "Battery");
        this.voltage = 9.0;

        Group group = new Group();
        body = new Rectangle(0, 0, BODY_W, BODY_H);
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setFill(Color.web("#2ECC71"));
        body.setStroke(Color.web("#1E8449"));
        body.setStrokeWidth(2);

        plus = new Circle(0, 0, 7);
        plus.setFill(Color.web("#E74C3C"));
        plus.setStroke(Color.web("#922B21"));

        minus = new Circle(0, 0, 7);
        minus.setFill(Color.web("#3498DB"));
        minus.setStroke(Color.web("#1F618D"));

        label = new Text(formatVoltage(voltage));
        label.setFont(Font.font("Consolas", 11));
        label.setFill(Color.web("#ECF0F1"));

        group.getChildren().addAll(body, plus, minus, label);
        this.shape = group;
        updateShape();
    }

    @Override
    protected void updateShape() {
        body.setX(x - BODY_W / 2);
        body.setY(y - BODY_H / 2);
        plus.setCenterX(x + LEG_OFFSET_X);
        plus.setCenterY(y);
        minus.setCenterX(x - LEG_OFFSET_X);
        minus.setCenterY(y);
        label.setText(formatVoltage(voltage));
        label.setX(x - 14);
        label.setY(y + 5);
    }

    private static String formatVoltage(double volts) {
        if (Math.abs(volts - Math.round(volts)) < 0.05) {
            return String.format("%.0fV", volts);
        }
        return String.format("%.1fV", volts);
    }

    public double getPlusX() {
        return x + LEG_OFFSET_X;
    }

    public double getMinusX() {
        return x - LEG_OFFSET_X;
    }

    public double getLegY() {
        return y;
    }

    /** @return 1 — минус, 2 — плюс, 0 — тело */
    public int pinAt(double px, double py) {
        if (distance(px, py, getMinusX(), getLegY()) <= 12) {
            return 1;
        }
        if (distance(px, py, getPlusX(), getLegY()) <= 12) {
            return 2;
        }
        if (containsBody(px, py)) {
            return 0;
        }
        return -1;
    }

    private boolean containsBody(double px, double py) {
        return px >= x - BODY_W / 2 && px <= x + BODY_W / 2
            && py >= y - BODY_H / 2 && py <= y + BODY_H / 2;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    @Override
    public boolean contains(double px, double py) {
        return pinAt(px, py) >= 0;
    }

    @Override
    public Footprint getFootprint() {
        return new Footprint(38, 16);
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = Math.max(1.5, Math.min(12.0, voltage));
        updateShape();
    }

    @Override
    public boolean getOutput() {
        return true;
    }

    @Override
    public String getType() {
        return "Battery";
    }
}
