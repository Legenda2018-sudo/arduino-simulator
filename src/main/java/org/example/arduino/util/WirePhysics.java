package org.example.arduino.util;

import org.example.arduino.model.WireAnchor;

/**
 * Сопротивление провода: R = k × L (не 1:1 с реальным миром, но по формуле).
 * L — длина линии провода на плате в пикселях.
 */
public final class WirePhysics {

    /** Ом на пиксель длины провода (упрощённая модель, не 1:1 с реальностью). */
    public static final double OHMS_PER_PIXEL = 0.01;
    public static final double MIN_OHMS = 0.01;

    private WirePhysics() {
    }

    public static double lengthPixels(WireAnchor from, WireAnchor to) {
        if (from == null || to == null) {
            return 0;
        }
        return Math.hypot(from.getX() - to.getX(), from.getY() - to.getY());
    }

    public static double resistanceOhms(double lengthPixels) {
        if (lengthPixels <= 0) {
            return 0;
        }
        return Math.max(MIN_OHMS, lengthPixels * OHMS_PER_PIXEL);
    }

    public static double resistanceOhms(WireAnchor from, WireAnchor to) {
        return resistanceOhms(lengthPixels(from, to));
    }

    public static String formulaLine(double lengthPx, double ohms) {
        return String.format(
            "R_провод = k × L = %.2f × %.0f = %.2f Ом",
            OHMS_PER_PIXEL,
            lengthPx,
            ohms
        );
    }
}
