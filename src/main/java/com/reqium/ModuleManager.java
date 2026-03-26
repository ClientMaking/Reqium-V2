package com.reqium;

import com.reqium.modules.client.ColorChat;
import com.reqium.modules.client.GUI;
import com.reqium.modules.client.HUD;
import com.reqium.modules.client.SpotifyPlay;
import com.reqium.modules.combat.AimAssist;
import com.reqium.modules.combat.AnchorMacro;
import com.reqium.modules.combat.AutoCrystal;
import com.reqium.modules.combat.AutoInvTotem;
import com.reqium.modules.combat.AutoTotem;
import com.reqium.modules.combat.DoubleAnchor;
import com.reqium.modules.combat.HoverTotem;
import com.reqium.modules.combat.MaceSwap;
import com.reqium.modules.combat.SafeAnchor;
import com.reqium.modules.combat.ShieldBreaker;
import com.reqium.modules.combat.TriggerBot;
import com.reqium.modules.misc.NameTags;
import com.reqium.modules.render.FakePlayer;
import com.reqium.modules.render.Fullbright;
import com.reqium.modules.render.PlayerESP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> moduleMap = new HashMap<>();

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        if (instance == null) instance = new ModuleManager();
        return instance;
    }

    public void initializeModules() {
        if (!modules.isEmpty()) return;

        registerModule(new MaceSwap());
        registerModule(new AutoTotem());
        registerModule(new HoverTotem());
        registerModule(new AutoInvTotem());
        registerModule(new AnchorMacro());
        registerModule(new AutoCrystal());
        registerModule(new DoubleAnchor());
        registerModule(new TriggerBot());
        registerModule(new ShieldBreaker());
        registerModule(new SafeAnchor());
        registerModule(new AimAssist());

        registerModule(new Fullbright());
        registerModule(new PlayerESP());
        registerModule(new FakePlayer());
        registerModule(new GUI());
        registerModule(new ColorChat());
        registerModule(new NameTags());
        registerModule(new SpotifyPlay());

        HUD hud = new HUD();
        registerModule(hud);
        hud.setEnabled(true);
    }

    public void registerModule(Module module) {
        modules.add(module);
        moduleMap.put(module.getName(), module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModule(String name) {
        return moduleMap.get(name);
    }

    public List<Module> getModulesByCategory(String category) {
        List<Module> result = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory().equals(category)) result.add(m);
        }
        return result;
    }
}
