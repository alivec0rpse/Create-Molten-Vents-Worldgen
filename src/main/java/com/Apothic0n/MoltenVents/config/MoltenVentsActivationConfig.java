package com.Apothic0n.MoltenVents.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON config for Molten Vents activation.
 *
 * Located at: config/molten_vents_activation.json
 *
 * Format:
 * {
 *   "activationTicks": 200,
 *   "ventPairs": [
 *     { "dormant": "modid:dormant_vent_block", "active": "modid:active_vent_block" }
 *   ]
 * }
 */
public class MoltenVentsActivationConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Singleton instance, populated on load */
    public static MoltenVentsActivationConfig INSTANCE = new MoltenVentsActivationConfig();

    /** How many ticks the blaze burner must be superheated before conversion (default 200 = 10 seconds) */
    public int activationTicks = 200;

    /** List of dormant → active vent block ID pairs */
    public List<VentPair> ventPairs = new ArrayList<>();

    public MoltenVentsActivationConfig() {}

    // -------------------------------------------------------------------------
    //  Inner type
    // -------------------------------------------------------------------------

    public static class VentPair {
        /** Registry ID of the dormant vent block, e.g. "create_resource_vents:dormant_asurine_vent" */
        public String dormant;
        /** Registry ID of the active vent block, e.g. "create_resource_vents:active_asurine_vent" */
        public String active;

        public VentPair() {}

        public VentPair(String dormant, String active) {
            this.dormant = dormant;
            this.active = active;
        }
    }

    // -------------------------------------------------------------------------
    //  Loading
    // -------------------------------------------------------------------------

    /**
     * Loads (or creates) the config file.
     * Call during mod construction / FMLLoadCompleteEvent.
     */
    public static void load() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("molten_vents_activation.json");

        if (!Files.exists(configPath)) {
            writeDefault(configPath);
        } else {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                MoltenVentsActivationConfig loaded = GSON.fromJson(reader, MoltenVentsActivationConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    LOGGER.info("[MoltenVents] Loaded activation config — {} vent pair(s), activationTicks={}",
                            INSTANCE.ventPairs.size(), INSTANCE.activationTicks);
                } else {
                    LOGGER.warn("[MoltenVents] Config file was empty or invalid, using defaults.");
                }
            } catch (IOException e) {
                LOGGER.error("[MoltenVents] Failed to read activation config, using defaults.", e);
            }
        }
    }

    private static void writeDefault(Path configPath) {
        MoltenVentsActivationConfig defaults = new MoltenVentsActivationConfig();
        defaults.activationTicks = 200;

        // Default pairs for Create: Resource Vents
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_asurine_vent",   "create_resource_vents:active_asurine_vent"));
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_crimsite_vent",   "create_resource_vents:active_crimsite_vent"));
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_ochrum_vent",     "create_resource_vents:active_ochrum_vent"));
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_scorchia_vent",   "create_resource_vents:active_scorchia_vent"));
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_scoria_vent",     "create_resource_vents:active_scoria_vent"));
        defaults.ventPairs.add(new VentPair("create_resource_vents:dormant_veridium_vent",   "create_resource_vents:active_veridium_vent"));

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(defaults), StandardCharsets.UTF_8);
            LOGGER.info("[MoltenVents] Created default activation config at {}", configPath);
        } catch (IOException e) {
            LOGGER.error("[MoltenVents] Failed to write default activation config.", e);
        }

        INSTANCE = defaults;
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the active block registry ID for a given dormant block ID, or {@code null} if no
     * mapping is configured.
     */
    public String getActiveId(String dormantBlockId) {
        for (VentPair pair : ventPairs) {
            if (pair.dormant != null && pair.dormant.equals(dormantBlockId)) {
                return pair.active;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the given block registry ID is listed as a dormant vent.
     */
    public boolean isDormantVent(String blockId) {
        for (VentPair pair : ventPairs) {
            if (pair.dormant != null && pair.dormant.equals(blockId)) {
                return true;
            }
        }
        return false;
    }
}
