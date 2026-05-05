package net.oldmanyounger.isometricrendersforked.property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Mutable value container used by render controls and export options.
 *
 * <p>Properties keep a default value, a current value, and a list of listeners
 * that are notified whenever the current value changes.</p>
 *
 * @param <T> the stored value type
 */
public class Property<T> implements BiConsumer<Property<T>, T> {
    protected T defaultValue;
    protected T value;
    protected final List<BiConsumer<Property<T>, T>> changeListeners;

    /**
     * Creates a property whose current value starts at its default value.
     *
     * @param defaultValue the initial and reset value
     */
    public Property(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.changeListeners = new ArrayList<>();
    }

    // Creates a generic property with the given default value.
    public static <T> Property<T> of(T defaultValue) {
        return new Property<>(defaultValue);
    }

    // Updates the current value and notifies listeners.
    public void set(T value) {
        this.value = value;
        this.invokeListeners();
    }

    // Updates the default value without changing the current value.
    public Property<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    // Resets the current value to the default value.
    public void setToDefault() {
        this.value = this.defaultValue;
        this.invokeListeners();
    }

    // Adds a listener and immediately invokes it with the current value.
    public void listen(BiConsumer<Property<T>, T> listener) {
        this.changeListeners.add(listener);
        listener.accept(this, this.value);
    }

    // Returns the current value.
    public T get() {
        return this.value;
    }

    // Copies value/default state from another property.
    public void copyFrom(Property<T> source) {
        this.defaultValue = source.defaultValue;
        this.value = source.value;
        this.invokeListeners();
    }

    // Notifies all registered listeners.
    protected void invokeListeners() {
        this.changeListeners.forEach(listener -> listener.accept(this, this.value));
    }

    // Allows one property to be used as another property's listener.
    @Override
    public void accept(Property<T> property, T value) {
        this.set(value);
    }
}
