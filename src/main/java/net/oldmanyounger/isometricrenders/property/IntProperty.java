package net.oldmanyounger.isometricrenders.property;

/**
 * Integer property with minimum and maximum bounds.
 *
 * <p>This is used heavily by render controls such as scale, rotation, slant,
 * resolution, and animation settings.</p>
 */
public class IntProperty extends Property<Integer> {
    private final int max;
    private final int min;
    private final int span;

    private boolean allowRollover = false;

    /**
     * Creates an integer property constrained to the supplied range.
     *
     * @param defaultValue the initial and reset value
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     */
    private IntProperty(int defaultValue, int min, int max) {
        super(defaultValue);

        this.min = min;
        this.max = max;
        this.span = this.max - this.min;
    }

    // Creates a bounded integer property.
    public static IntProperty of(int defaultValue, int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("'min' must be less than 'max'");
        }

        return new IntProperty(defaultValue, min, max);
    }

    // Enables wraparound behavior when modifying beyond the range.
    public IntProperty withRollover() {
        this.allowRollover = true;
        return this;
    }

    // Adjusts the value by the supplied delta.
    public void modify(int by) {
        if (this.allowRollover) {
            this.value += by;
            if (this.value > this.max) this.value -= this.span;
            if (this.value < this.min) this.value += this.span;
        } else {
            this.value = Math.clamp(this.value + by, this.min, this.max);
        }

        this.invokeListeners();
    }

    // Returns the current value as slider progress from 0.0 to 1.0.
    public double progress() {
        return (this.value - this.min) / (double) this.span;
    }

    // Updates the current value from slider progress from 0.0 to 1.0.
    public void setFromProgress(double progress) {
        this.value = (int) Math.round(this.min + progress * this.span);
        this.invokeListeners();
    }

    // Returns the maximum allowed value.
    public int max() {
        return this.max;
    }

    // Returns the minimum allowed value.
    public int min() {
        return this.min;
    }
}
