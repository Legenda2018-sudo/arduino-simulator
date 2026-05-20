package org.example.arduino.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Цветовая маркировка резистора (4 полосы, упрощённый ряд E12).
 */
public final class ResistorColorCode {
    private static final int[] E12 = {
        10, 12, 15, 18, 22, 27, 33, 39, 47, 56, 68, 82,
        100, 120, 150, 180, 220, 270, 330, 390, 470, 560, 680, 820,
        1000, 1200, 1500, 1800, 2200, 2700, 3300, 3900, 4700, 5600, 6800, 8200,
        10000, 12000, 15000, 18000, 22000, 27000, 33000, 39000, 47000, 56000, 68000, 82000,
        100000, 120000, 150000, 180000, 220000, 270000, 330000, 390000, 470000, 560000, 680000, 820000,
        1000000
    };

    private ResistorColorCode() {
    }

    public static double snapToE12(double ohms) {
        if (ohms <= 0) {
            return 220;
        }
        double best = E12[0];
        double bestDiff = Math.abs(ohms - best);
        for (int value : E12) {
            double diff = Math.abs(ohms - value);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = value;
            }
        }
        return best;
    }

    /** Следующее/предыдущее значение ряда E12. */
    public static double stepE12(double ohms, int direction) {
        double snapped = snapToE12(ohms);
        int idx = 0;
        double minDiff = Double.MAX_VALUE;
        for (int i = 0; i < E12.length; i++) {
            double diff = Math.abs(E12[i] - snapped);
            if (diff < minDiff) {
                minDiff = diff;
                idx = i;
            }
        }
        int next = Math.max(0, Math.min(E12.length - 1, idx + direction));
        return E12[next];
    }

    public static Color[] bandsForOhms(double ohms) {
        int normalized = (int) Math.round(snapToE12(ohms));
        String s = String.valueOf(normalized);
        if (s.length() < 2) {
            s = "220";
        }
        int d1 = Character.getNumericValue(s.charAt(0));
        int d2 = Character.getNumericValue(s.charAt(1));
        int zeros = s.length() - 2;
        return new Color[] {
            digitColor(d1),
            digitColor(d2),
            multiplierColor(zeros),
            Color.web("#D4AF37")
        };
    }

    public static void applyBands(Rectangle[] bandRects, double ohms) {
        Color[] colors = bandsForOhms(ohms);
        for (int i = 0; i < bandRects.length && i < colors.length; i++) {
            bandRects[i].setFill(colors[i]);
        }
    }

    public static String describeBands(double ohms) {
        double r = snapToE12(ohms);
        if (r >= 1000) {
            return String.format("%.1f кОм ±5%%", r / 1000.0);
        }
        return String.format("%.0f Ом ±5%%", r);
    }

    private static Color digitColor(int digit) {
        return switch (digit) {
            case 0 -> Color.BLACK;
            case 1 -> Color.BROWN;
            case 2 -> Color.RED;
            case 3 -> Color.ORANGERED;
            case 4 -> Color.ORANGE;
            case 5 -> Color.web("#FFD700");
            case 6 -> Color.GREEN;
            case 7 -> Color.web("#8B4513");
            case 8 -> Color.GRAY;
            case 9 -> Color.WHITE;
            default -> Color.GRAY;
        };
    }

    private static Color multiplierColor(int zeros) {
        return switch (zeros) {
            case 0 -> Color.BLACK;
            case 1 -> Color.BROWN;
            case 2 -> Color.RED;
            case 3 -> Color.ORANGERED;
            case 4 -> Color.ORANGE;
            case 5 -> Color.web("#FFD700");
            default -> Color.BLUE;
        };
    }
}
