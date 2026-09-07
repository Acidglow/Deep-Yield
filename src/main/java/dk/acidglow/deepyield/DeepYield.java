package dk.acidglow.deepyield;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(DeepYield.MODID)
public class DeepYield {
    public static final String MODID = "deepyield";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeepYield(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, DeepYieldConfig.SPEC);
        modEventBus.addListener(DeepYieldGameplay::registerAttachments);
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            modEventBus.addListener(DeepYieldGameTests::register);
        }
        DeepYieldGameplay.register();
    }
}
