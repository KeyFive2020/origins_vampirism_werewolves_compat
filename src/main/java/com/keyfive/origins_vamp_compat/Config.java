package com.keyfive.origins_vamp_compat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.BooleanValue witherOnProtectedDrink;
        public final ForgeConfigSpec.BooleanValue infectProtectedRaces;
        public final ForgeConfigSpec.BooleanValue protectHumanRace;
        public final ForgeConfigSpec.BooleanValue witherOnHumanDrink;

        ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            witherOnProtectedDrink = builder
                .comment("Aplica efeito Wither no vampiro ao sugar sangue de racas protegidas")
                .define("witherOnProtectedDrink", true);
            infectProtectedRaces = builder
                .comment("Permite que racas protegidas sejam infectadas (Sanguinare)")
                .define("infectProtectedRaces", false);
            protectHumanRace = builder
                .comment("Bloqueia infeccao (sanguinare) na raca humana")
                .define("protectHumanRace", true);
            witherOnHumanDrink = builder
                .comment("Aplica efeito Wither no vampiro ao sugar sangue de humanos")
                .define("witherOnHumanDrink", false);
            builder.pop();
        }
    }

    public static boolean witherOnProtectedDrink() { return SERVER.witherOnProtectedDrink.get(); }
    public static boolean infectProtectedRaces() { return SERVER.infectProtectedRaces.get(); }
    public static boolean protectHumanRace() { return SERVER.protectHumanRace.get(); }
    public static boolean witherOnHumanDrink() { return SERVER.witherOnHumanDrink.get(); }
}
