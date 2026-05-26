package dev.lvstrng.aidsfuscator.exclude.preset;

import dev.lvstrng.aidsfuscator.exclude.IExclusionPreset;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;

public class MinecraftFabricExclusionPreset implements IExclusionPreset {
    @Override
    public void load() {
        Exclusions.GLOBAL.addAnnotation("org/spongepowered/asm/mixin/Mixin");
    }
}
