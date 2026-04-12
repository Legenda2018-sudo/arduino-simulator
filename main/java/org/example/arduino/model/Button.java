package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Button extends Component {
    private boolean isPressed;

    public Button(double x, double y) {
        super(x, y, "Button");
        this.isPressed = false;
        this.shape = new Rectangle(x - 25, y - 12, 50, 24);
        this.shape.setFill(Color.GRAY);
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(2);
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setX(x - 25);
            rect.setY(y - 12);
        }
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            if (pressed) {
                rect.setFill(Color.GREEN);
                rect.setStroke(Color.DARKGREEN);
            } else {
                rect.setFill(Color.GRAY);
                rect.setStroke(Color.BLACK);
            }
        }
    }

    public boolean isPressed() {
        return isPressed;
    }

    public void toggle() {
        setPressed(!isPressed);
    }

    @Override
    public boolean getOutput() {
        return isPressed;
    }

    @Override
    public String getType() {
        return "Button";
    }
}

