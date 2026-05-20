package org.example.arduino.model;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.example.arduino.util.ResistorColorCode;

public class Resistor extends Component {
    private static final double LEG_OFFSET_X = 28;
    private static final double BODY_W = 44;
    private static final double BODY_H = 16;

    private double resistance;
    private boolean input;
    private final Rectangle body;
    private final Circle leg1;
    private final Circle leg2;
    private final Rectangle[] bands = new Rectangle[4];

    public Resistor(double x, double y) {
        super(x, y, "Resistor");
        this.resistance = 220.0;
        this.input = false;

        Group group = new Group();

        leg1 = new Circle(0, 0, 6);
        leg1.setFill(Color.rgb(200, 200, 200));
        leg1.setStroke(Color.web("#555555"));
        leg1.setStrokeWidth(1.5);

        leg2 = new Circle(0, 0, 6);
        leg2.setFill(Color.rgb(200, 200, 200));
        leg2.setStroke(Color.web("#555555"));
        leg2.setStrokeWidth(1.5);

        body = new Rectangle(0, 0, BODY_W, BODY_H);
        body.setFill(Color.BEIGE);
        body.setStroke(Color.BROWN);
        body.setStrokeWidth(2);
        body.setArcWidth(6);
        body.setArcHeight(6);

        double bandW = 5;
        double bandGap = 7;
        double startX = -BODY_W / 2 + 8;
        for (int i = 0; i < bands.length; i++) {
            Rectangle band = new Rectangle(startX + i * bandGap, -BODY_H / 2 + 2, bandW, BODY_H - 4);
            bands[i] = band;
            group.getChildren().add(band);
        }

        group.getChildren().addAll(body, leg1, leg2);
        this.shape = group;
        setResistance(resistance);
        updateShape();
    }

    @Override
    protected void updateShape() {
        if (!(shape instanceof Group)) {
            return;
        }
        leg1.setCenterX(x - LEG_OFFSET_X);
        leg1.setCenterY(y);
        leg2.setCenterX(x + LEG_OFFSET_X);
        leg2.setCenterY(y);
        body.setX(x - BODY_W / 2);
        body.setY(y - BODY_H / 2);
        double bandW = 5;
        double bandGap = 7;
        double startX = x - BODY_W / 2 + 8;
        for (int i = 0; i < bands.length; i++) {
            bands[i].setX(startX + i * bandGap);
            bands[i].setY(y - BODY_H / 2 + 2);
        }
        ResistorColorCode.applyBands(bands, resistance);
    }

    public double getLeg1X() {
        return x - LEG_OFFSET_X;
    }

    public double getLeg2X() {
        return x + LEG_OFFSET_X;
    }

    public double getLegY() {
        return y;
    }

    private static final double LEG_HIT_RADIUS = 12;

    /** @return 0 — тело, 1 — левая ножка, 2 — правая ножка */
    public int pinAt(double px, double py) {
        if (distance(px, py, getLeg1X(), getLegY()) <= LEG_HIT_RADIUS) {
            return 1;
        }
        if (distance(px, py, getLeg2X(), getLegY()) <= LEG_HIT_RADIUS) {
            return 2;
        }
        if (containsBody(px, py)) {
            return 0;
        }
        return -1;
    }

    private boolean containsBody(double px, double py) {
        double bx = x - BODY_W / 2;
        double by = y - BODY_H / 2;
        return px >= bx && px <= bx + BODY_W && py >= by && py <= by + BODY_H;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public Footprint getFootprint() {
        return new Footprint(34, 10);
    }

    @Override
    public boolean contains(double px, double py) {
        return pinAt(px, py) >= 0;
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = ResistorColorCode.snapToE12(resistance);
        updateShape();
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public boolean getInput() {
        return input;
    }

    @Override
    public boolean getOutput() {
        return input;
    }

    @Override
    public String getType() {
        return "Resistor";
    }

    public String getColorCodeText() {
        return ResistorColorCode.describeBands(resistance);
    }
}
