package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Button extends Component {
    private boolean isPressed;

    public Button(double x, double y) {
        super(x, y, "Button");
        this.isPressed = false;
        Rectangle rect = new Rectangle(x - 28, y - 14, 56, 28);
        rect.setArcWidth(14);
        rect.setArcHeight(14);
        rect.setFill(Color.web("#636E72"));
        rect.setStroke(Color.web("#2D3436"));
        rect.setStrokeWidth(2);
        this.shape = rect;
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setX(x - 28);
            rect.setY(y - 14);
        }
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            if (pressed) {
                rect.setFill(Color.web("#27AE60"));
                rect.setStroke(Color.web("#1E8449"));
            } else {
                rect.setFill(Color.web("#636E72"));
                rect.setStroke(Color.web("#2D3436"));
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

