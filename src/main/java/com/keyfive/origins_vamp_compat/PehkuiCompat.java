package com.keyfive.origins_vamp_compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import virtuoel.pehkui.api.ScaleTypes;

/** Optional Pehkui integration. This class is only loaded when Pehkui is installed. */
public final class PehkuiCompat {
    private static final String PEHKUI_MOD_ID = "pehkui";

    private PehkuiCompat() {
    }

    public static void resetPlayerScale(Player player) {
        if (!ModList.get().isLoaded(PEHKUI_MOD_ID)) {
            return;
        }

        try {
            ScaleTypes.BASE.getScaleData(player).resetScale();
            player.refreshDimensions();
            System.out.println("[OriginsVampCompat] Escala Pehkui restaurada para " + player.getName().getString());
        } catch (LinkageError | RuntimeException error) {
            System.out.println("[OriginsVampCompat] Falha ao restaurar escala Pehkui: " + error.getMessage());
        }
    }
}
