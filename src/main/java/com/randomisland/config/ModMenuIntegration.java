package com.randomisland.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // getConfigScreen 返回的是 Supplier<Screen>，不是 Optional，直接 get() 即可
        return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}
