package com.reqium;

public class SliderSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public SliderSetting(String name, double value, double min, double max, double step) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public String getDisplayValue() {
        double v = getValue();
        if (step >= 1.0 && v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((int) v);
        }
        if (step >= 0.1) {
            return String.format("%.1f", v);
        }
        return String.format("%.2f", v);
    }
}
