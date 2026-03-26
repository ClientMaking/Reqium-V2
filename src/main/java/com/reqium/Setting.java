package com.reqium;

import java.util.function.Consumer;

public class Setting<T> {
    private String name;
    private T value;
    private Consumer<T> callback;

    public Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public Setting(String name, T value, Consumer<T> callback) {
        this.name = name;
        this.value = value;
        this.callback = callback;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        if (callback != null) {
            callback.accept(value);
        }
    }
}
