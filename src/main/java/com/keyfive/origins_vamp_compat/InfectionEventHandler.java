package com.keyfive.origins_vamp_compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OriginsVampCompatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfectionEventHandler {

    private static final Map<UUID, Boolean> FACTION_SET = new HashMap<>();
    private static final Map<UUID, ResourceLocation> PLAYER_ORIGINS = new HashMap<>();

    private static final ResourceLocation VAMPIRE_ORIGIN =
        ResourceLocation.tryParse("origins_vamp_compat:vampire");
    private static final ResourceLocation WEREWOLF_ORIGIN =
        ResourceLocation.tryParse("origins_vamp_compat:werewolf");
    private static final ResourceLocation HUNTER_ORIGIN =
        ResourceLocation.tryParse("origins_vamp_compat:hunter");
    private static final ResourceLocation HUMAN_ORIGIN =
        ResourceLocation.tryParse("origins:human");
    private static final ResourceLocation EMPTY_ORIGIN =
        ResourceLocation.tryParse("origins:empty");

    private static final ResourceKey<DamageType> WEREWOLF_BITE =
        ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryParse("werewolves:bite"));

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side == LogicalSide.CLIENT) return;
        if (event.player.tickCount % 40 != 0) return;

        processPlayer(event.player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FACTION_SET.remove(event.getEntity().getUUID());
        PLAYER_ORIGINS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent event) {
        try {
            IVampirePlayer vp = (IVampirePlayer) event.getVampire();
            Player vampirePlayer = vp.getRepresentingPlayer();
            if (vampirePlayer == null || vampirePlayer.level().isClientSide) return;

            var bloodSource = event.getBloodSource().getEntity().orElse(null);
            if (!(bloodSource instanceof Player targetPlayer)) return;

            ResourceLocation targetOrigin = getPlayerOriginId(targetPlayer);
            if (targetOrigin == null) return;

            if (VAMPIRE_ORIGIN.equals(targetOrigin)) return;
            if (WEREWOLF_ORIGIN.equals(targetOrigin)) return;
            if (HUNTER_ORIGIN.equals(targetOrigin)) return;
            if (EMPTY_ORIGIN.equals(targetOrigin)) return;

            if (HUMAN_ORIGIN.equals(targetOrigin)) {
                if (Config.witherOnHumanDrink()) {
                    vampirePlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true, true));
                    System.out.println("[OriginsVampCompat] Wither aplicado em " + vampirePlayer.getName().getString() + " por sugar sangue humano");
                }
                return;
            }

            if (Config.witherOnProtectedDrink()) {
                vampirePlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true, true));
                System.out.println("[OriginsVampCompat] Wither aplicado em " + vampirePlayer.getName().getString() + " por sugar sangue de raca protegida");
            }
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro em onPlayerDrinkBlood: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onWerewolfBite(LivingHurtEvent event) {
        try {
            if (!event.getSource().is(WEREWOLF_BITE)) return;
            if (!(event.getSource().getEntity() instanceof Player werewolfPlayer)) return;
            if (!(event.getEntity() instanceof Player targetPlayer)) return;
            if (targetPlayer.level().isClientSide) return;

            ResourceLocation attackerOrigin = getPlayerOriginId(werewolfPlayer);
            if (!WEREWOLF_ORIGIN.equals(attackerOrigin)) return;

            ResourceLocation targetOrigin = getPlayerOriginId(targetPlayer);
            if (targetOrigin == null) return;
            if (VAMPIRE_ORIGIN.equals(targetOrigin)) return;
            if (WEREWOLF_ORIGIN.equals(targetOrigin)) return;
            if (HUNTER_ORIGIN.equals(targetOrigin)) return;
            if (EMPTY_ORIGIN.equals(targetOrigin)) return;

            if (HUMAN_ORIGIN.equals(targetOrigin)) {
                if (Config.witherOnHumanDrink()) {
                    werewolfPlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true, true));
                    System.out.println("[OriginsVampCompat] Wither aplicado em " + werewolfPlayer.getName().getString() + " por morder humano");
                }
                return;
            }

            if (Config.witherOnProtectedDrink()) {
                werewolfPlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true, true));
                System.out.println("[OriginsVampCompat] Wither aplicado em " + werewolfPlayer.getName().getString() + " por morder raca protegida");
            }
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro em onWerewolfBite: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onFactionChangePre(PlayerFactionEvent.FactionLevelChangePre event) {
        try {
            Player player = event.getPlayer().getPlayer();
            if (player == null || player.level().isClientSide) return;

            IPlayableFaction<?> currentFaction = event.getCurrentFaction();
            IPlayableFaction<?> newFaction = event.getNewFaction();

            if (newFaction == null) return;
            if (currentFaction != null && currentFaction.equals(newFaction)) return;

            ResourceLocation originId = getPlayerOriginId(player);
            if (originId == null) return;

            if (VAMPIRE_ORIGIN.equals(originId)) return;
            if (WEREWOLF_ORIGIN.equals(originId)) return;
            if (HUNTER_ORIGIN.equals(originId)) return;
            if (EMPTY_ORIGIN.equals(originId)) return;

            if (HUMAN_ORIGIN.equals(originId)) {
                if (Config.protectHumanRace()) {
                    event.setCanceled(true);
                    System.out.println("[OriginsVampCompat] Bloqueada infeccao para humano " + player.getName().getString());
                }
                return;
            }

            if (!Config.infectProtectedRaces()) {
                event.setCanceled(true);
                System.out.println("[OriginsVampCompat] Bloqueada infeccao para " + player.getName().getString() + " (origem: " + originId + ")");
            }
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro em onFactionChangePre: " + e.getMessage());
        }
    }

    private static void processPlayer(Player player) {
        try {
            UUID uuid = player.getUUID();
            ResourceLocation originId = getPlayerOriginId(player);

            if (originId == null) return;

            ResourceLocation previousOrigin = PLAYER_ORIGINS.get(uuid);
            if (previousOrigin != null && !previousOrigin.equals(originId)) {
                System.out.println("[OriginsVampCompat] Origem mudou de " + previousOrigin + " para " + originId);
                removeFactionForOrigin(player, previousOrigin);
                if (isCompatOrigin(originId) && ModList.get().isLoaded("pehkui")) {
                    PehkuiCompat.resetPlayerScale(player);
                }
                FACTION_SET.put(uuid, false);
            }
            PLAYER_ORIGINS.put(uuid, originId);

            boolean factionSet = FACTION_SET.getOrDefault(uuid, false);

            if (VAMPIRE_ORIGIN.equals(originId)) {
                if (!factionSet) {
                    setFactionLevel(player, "vampirism:vampire", 5);
                    FACTION_SET.put(uuid, true);
                }
                return;
            }

            if (WEREWOLF_ORIGIN.equals(originId)) {
                if (!factionSet) {
                    setFactionLevel(player, "werewolves:werewolf", 5);
                    FACTION_SET.put(uuid, true);
                }
                return;
            }

            if (HUNTER_ORIGIN.equals(originId)) {
                if (!factionSet) {
                    setFactionLevel(player, "vampirism:hunter", 5);
                    FACTION_SET.put(uuid, true);
                }
                return;
            }

            if (HUMAN_ORIGIN.equals(originId)) {
                if (Config.protectHumanRace()) {
                    applyProtectiveEffects(player);
                }
                return;
            }

            if (EMPTY_ORIGIN.equals(originId)) return;

            applyProtectiveEffects(player);

        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro processando " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    private static ResourceLocation getPlayerOriginId(Player player) {
        IOriginContainer container = IOriginContainer.get(player).resolve().orElse(null);
        if (container == null) {
            return null;
        }

        var layers = OriginsAPI.getActiveLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }

        ResourceLocation mainLayerId = ResourceLocation.tryParse("origins:origin");
        for (var layerHolder : layers) {
            ResourceKey<OriginLayer> layerKey = layerHolder.unwrapKey().orElse(null);
            if (layerKey == null) continue;
            if (!mainLayerId.equals(layerKey.location())) continue;

            ResourceKey<?> originKey = container.getOrigin(layerKey);
            if (originKey == null) continue;

            return originKey.location();
        }

        return null;
    }

    private static boolean isCompatOrigin(ResourceLocation originId) {
        return VAMPIRE_ORIGIN.equals(originId)
            || WEREWOLF_ORIGIN.equals(originId)
            || HUNTER_ORIGIN.equals(originId)
            || HUMAN_ORIGIN.equals(originId);
    }

    private static MobEffect cachedSanguinare = null;

    private static void applyProtectiveEffects(Player player) {
        try {
            if (cachedSanguinare == null) {
                cachedSanguinare = ForgeRegistries.MOB_EFFECTS.getValue(
                    ResourceLocation.tryParse("vampirism:sanguinare"));
            }
            if (cachedSanguinare != null && player.hasEffect(cachedSanguinare)) {
                player.removeEffect(cachedSanguinare);
            }
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro ao aplicar efeitos protetivos: " + e.getMessage());
        }
    }

    private static void setFactionLevel(Player player, String factionId, int level) {
        try {
            IFactionPlayerHandler handler = VampirismAPI.getFactionPlayerHandler(player)
                .resolve().orElse(null);
            if (handler == null) {
                System.out.println("[OriginsVampCompat] Faction handler nao disponivel para " + player.getName().getString());
                return;
            }

            IPlayableFaction<?> faction = (IPlayableFaction<?>)
                VampirismAPI.factionRegistry().getFactionByID(ResourceLocation.tryParse(factionId));

            if (faction == null) {
                System.out.println("[OriginsVampCompat] Faction nao encontrada: " + factionId);
                return;
            }

            handler.setFactionAndLevel(faction, level);
            System.out.println("[OriginsVampCompat] Setado " + player.getName().getString() + " para " + factionId + " nivel " + level);
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Falha ao setar faction: " + e.getMessage());
        }
    }

    private static void removeFactionForOrigin(Player player, ResourceLocation originId) {
        try {
            String factionId = null;
            if (VAMPIRE_ORIGIN.equals(originId)) {
                factionId = "vampirism:vampire";
            } else if (WEREWOLF_ORIGIN.equals(originId)) {
                factionId = "werewolves:werewolf";
            } else if (HUNTER_ORIGIN.equals(originId)) {
                factionId = "vampirism:hunter";
            }

            if (factionId == null) return;

            IPlayableFaction<?> faction = (IPlayableFaction<?>)
                VampirismAPI.factionRegistry().getFactionByID(ResourceLocation.tryParse(factionId));
            if (faction == null) return;

            IFactionPlayerHandler handler = VampirismAPI.getFactionPlayerHandler(player)
                .resolve().orElse(null);
            if (handler == null) return;

            IPlayableFaction<?> currentFaction = handler.getCurrentFaction();
            if (currentFaction != null && currentFaction.equals(faction)) {
                handler.leaveFaction(true);
                System.out.println("[OriginsVampCompat] Removida faccao " + factionId + " para " + player.getName().getString());
            }
        } catch (Exception e) {
            System.out.println("[OriginsVampCompat] Erro ao remover faccao: " + e.getMessage());
        }
    }
}
