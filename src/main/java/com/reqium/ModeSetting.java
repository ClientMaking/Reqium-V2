package com.reqium;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        List<String> values = new ArrayList<>();
        if (modes != null) {
            values.addAll(Arrays.asList(modes));
        }
        if (values.isEmpty()) {
            values.add(defaultValue);
        }
        if (!values.contains(defaultValue)) {
            values.add(0, defaultValue);
        }
        this.modes = Collections.unmodifiableList(values);
    }

    public List<String> getModes() {
        return modes;
    }

    public void next() {
        int index = modes.indexOf(getValue());
        if (index < 0) {
            setValue(modes.get(0));
            return;
        }
        setValue(modes.get((index + 1) % modes.size()));
    }

    public boolean is(String mode) {
        return getValue().equalsIgnoreCase(mode);
    }
}
