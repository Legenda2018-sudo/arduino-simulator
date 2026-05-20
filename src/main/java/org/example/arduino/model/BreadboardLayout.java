package org.example.arduino.model;

/**
 * Геометрия макетной платы: сетка компонентов и шины питания +/− снизу.
 */
public final class BreadboardLayout {
    public static final double BOARD_VIEW_WIDTH = 1000;
    public static final double BOARD_VIEW_HEIGHT = 750;

    public static final double BOARD_GRID_LEFT = 30;
    public static final double BOARD_GRID_TOP = 30;
    public static final double BOARD_CELL = 20;
    public static final int BOARD_COLS = 45;
    /** Рядов отверстий для компонентов (ниже — зона шин). */
    public static final int BOARD_ROWS = 24;

    public static final double BOARD_HOLE_MIN_X = BOARD_GRID_LEFT + BOARD_CELL / 2;
    public static final double BOARD_HOLE_MIN_Y = BOARD_GRID_TOP + BOARD_CELL / 2;
    public static final double BOARD_HOLE_MAX_X = BOARD_GRID_LEFT + (BOARD_COLS - 1) * BOARD_CELL + BOARD_CELL / 2;
    public static final double BOARD_HOLE_MAX_Y = BOARD_GRID_TOP + (BOARD_ROWS - 1) * BOARD_CELL + BOARD_CELL / 2;

    public static final double RAIL_ZONE_TOP = 600;
    public static final double RAIL_PLUS_CENTER_Y = 655;
    public static final double RAIL_MINUS_CENTER_Y = 700;
    public static final double RAIL_HEIGHT = 28;
    public static final double RAIL_HIT_PADDING = 12;
    /** Ширина колонки с подписями +5V / GND (не точки подключения). */
    public static final double RAIL_LABEL_WIDTH = 58;
    public static final double RAIL_HOLES_MIN_X = BOARD_GRID_LEFT + RAIL_LABEL_WIDTH;

    private BreadboardLayout() {
    }

    public static double snapRailX(double x) {
        double snapped = Math.round((x - BOARD_HOLE_MIN_X) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_X;
        return Math.max(BOARD_HOLE_MIN_X, Math.min(BOARD_HOLE_MAX_X, snapped));
    }

    public static boolean isInRailZone(double y) {
        return y >= RAIL_ZONE_TOP - RAIL_HIT_PADDING && y <= BOARD_VIEW_HEIGHT - 5;
    }

    public static boolean isPlusRail(double y) {
        return y < (RAIL_PLUS_CENTER_Y + RAIL_MINUS_CENTER_Y) / 2.0;
    }

    /** Точка на шине питания: только красные/синие контакты, не подписи слева. */
    public static WireAnchor railAt(double x, double y) {
        if (!isInRailZone(y) || x < RAIL_HOLES_MIN_X) {
            return null;
        }
        double snapX = snapRailX(x);
        return isPlusRail(y) ? WireAnchor.railPlus(snapX) : WireAnchor.railMinus(snapX);
    }
}
