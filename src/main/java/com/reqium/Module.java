package com.reqium;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private String name;
    private String description;
    private String category;
    private boolean enabled = false;
    private int keybind = 0; // 0 = none
    private final List<Setting<?>> settings = new ArrayList<>();
    
    public Module(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
    
    public void addSetting(Setting<?> setting) {
        settings.add(setting);
    }
    
    public List<Setting<?>> getSettings() {
        return settings;
    }
    
    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onTick();
    
    public void setEnabled(boolean enabled) {
        if (enabled && !this.enabled) {
            this.enabled = true;
            onEnable();
        } else if (!enabled && this.enabled) {
            this.enabled = false;
            onDisable();
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getKeybind() {
        return keybind;
    }
    
    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }
    
    public String getCategory() {
        return category;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
