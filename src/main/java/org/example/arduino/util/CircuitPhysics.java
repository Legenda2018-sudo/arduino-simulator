package org.example.arduino.util;

/**
 * Упрощённые расчёты цепи (без SPICE): закон Ома для LED + резистор.
 */
public final class CircuitPhysics {
    public static final double SUPPLY_V = 5.0;
    public static final double LED_VF = 2.0;
    public static final double LED_I_NOM_MA = 20.0;
    public static final double LED_I_MAX_MA = 30.0;

    public enum SafetyLevel {
        NO_RESISTOR("Нет ограничения тока — LED может перегореть", "#C0392B"),
        OVERLOAD("Перегрузка — ток выше безопасного предела", "#E74C3C"),
        HIGH("Повышенный ток — лучше увеличить резистор", "#F39C12"),
        NORMAL("Ток в норме для светодиода", "#27AE60");

        private final String message;
        private final String color;

        SafetyLevel(String message, String color) {
            this.message = message;
            this.color = color;
        }

        public String getMessage() {
            return message;
        }

        public String getColor() {
            return color;
        }
    }

    public static final class CalcResult {
        private final double supplyV;
        private final double ledVf;
        private final double resistorOhms;
        private final double currentMa;
        private final double recommendedOhms;
        private final SafetyLevel safety;

        public CalcResult(double supplyV, double ledVf, double resistorOhms,
                          double currentMa, double recommendedOhms, SafetyLevel safety) {
            this.supplyV = supplyV;
            this.ledVf = ledVf;
            this.resistorOhms = resistorOhms;
            this.currentMa = currentMa;
            this.recommendedOhms = recommendedOhms;
            this.safety = safety;
        }

        public double getSupplyV() {
            return supplyV;
        }

        public double getLedVf() {
            return ledVf;
        }

        public double getResistorOhms() {
            return resistorOhms;
        }

        public double getCurrentMa() {
            return currentMa;
        }

        public double getRecommendedOhms() {
            return recommendedOhms;
        }

        public SafetyLevel getSafety() {
            return safety;
        }

        public String formulaSteps() {
            if (resistorOhms <= 0) {
                return String.format(
                    "I = (U − V_LED) / R = (%.1f − %.1f) / 0 → ∞ мА",
                    supplyV, ledVf
                );
            }
            String iStr = Double.isInfinite(currentMa) ? "∞" : String.format("%.2f", currentMa);
            return String.format(
                "I = (U − V_LED) / R%n  = (%.1f − %.1f) / %.0f%n  = %s мА",
                supplyV, ledVf, resistorOhms, iStr
            );
        }
    }

    private CircuitPhysics() {
    }

    public static double currentMilliAmps(double supplyV, double resistorOhms, double ledVf) {
        if (resistorOhms <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double i = (supplyV - ledVf) / resistorOhms;
        return i * 1000.0;
    }

    public static double recommendedResistorOhms(double supplyV, double ledVf, double targetMa) {
        if (targetMa <= 0) {
            return 220;
        }
        return (supplyV - ledVf) / (targetMa / 1000.0);
    }

    public static boolean isLedSafe(double currentMa) {
        return currentMa > 0 && currentMa <= LED_I_MAX_MA;
    }

    public static boolean isLedOverloaded(double currentMa) {
        return currentMa > LED_I_MAX_MA;
    }

    public static String shortStatus(double currentMa) {
        if (Double.isInfinite(currentMa) || currentMa <= 0) {
            return "ток: опасно (нет резистора)";
        }
        return String.format("ток ≈ %.1f мА", currentMa);
    }

    public static SafetyLevel classifyCurrent(double currentMa, double resistorOhms) {
        if (resistorOhms <= 0 || Double.isInfinite(currentMa)) {
            return SafetyLevel.NO_RESISTOR;
        }
        if (currentMa <= 0) {
            return SafetyLevel.NO_RESISTOR;
        }
        if (currentMa > LED_I_MAX_MA) {
            return SafetyLevel.OVERLOAD;
        }
        if (currentMa > LED_I_NOM_MA) {
            return SafetyLevel.HIGH;
        }
        return SafetyLevel.NORMAL;
    }

    public static CalcResult analyze(double supplyV, double resistorOhms, double ledVf) {
        double iMa = currentMilliAmps(supplyV, resistorOhms, ledVf);
        double recommended = recommendedResistorOhms(supplyV, ledVf, LED_I_NOM_MA);
        return new CalcResult(
            supplyV,
            ledVf,
            resistorOhms,
            iMa,
            recommended,
            classifyCurrent(iMa, resistorOhms)
        );
    }

    public static CalcResult analyze(double resistorOhms) {
        return analyze(SUPPLY_V, resistorOhms, LED_VF);
    }

    public static String detailedCalculation(double supplyV, double resistorOhms, double ledVf) {
        CalcResult r = analyze(supplyV, resistorOhms, ledVf);
        String iStr = Double.isInfinite(r.currentMa) ? "∞" : String.format("%.2f", r.currentMa);
        return String.format(
            "U = %.1f В, V_LED ≈ %.1f В%nI = (U − V_LED) / R = (%.1f − %.1f) / %.0f = %s мА%n"
                + "Номинал для LED: R ≈ %.0f Ом при %.0f мА",
            supplyV,
            ledVf,
            supplyV,
            ledVf,
            resistorOhms,
            iStr,
            r.recommendedOhms,
            LED_I_NOM_MA
        );
    }
}
