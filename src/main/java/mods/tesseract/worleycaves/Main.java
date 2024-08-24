package mods.tesseract.worleycaves;


import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import mods.tesseract.worleycaves.config.Configs;
import mods.tesseract.worleycaves.event.CaveEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "worleycaves", name = "Worley Caves", version = "1.5.2", acceptableRemoteVersions = "*")
public class Main
{
    public static final Logger LOGGER = LogManager.getLogger("worleycaves");

	@EventHandler
	public static void preInit(FMLPreInitializationEvent e)
	{
        MinecraftForge.TERRAIN_GEN_BUS.register(new CaveEvent());
        Configuration cfg = new Configuration(e.getSuggestedConfigurationFile());
        Configs.noiseCutoffValue = cfg.getFloat("noiseCutoffValue", "cave", Configs.noiseCutoffValue, -1f, 1f, "Controls size of caves. Smaller values = larger caves. Between -1.0 and 1.0");
        Configs.surfaceCutoffValue = cfg.getFloat("surfaceCutoffValue", "cave", Configs.surfaceCutoffValue, -1f, 1f, "Controls size of caves at the surface. Smaller values = more caves break through the surface. Between -1.0 and 1.0");
        Configs.warpAmplifier = cfg.getFloat("warpAmplifier", "cave", Configs.warpAmplifier, 0f, Float.MAX_VALUE, "Controls how much to warp caves. Lower values = straighter caves");
        Configs.easeInDepth = cfg.getInt("easeInDepth", "cave", Configs.easeInDepth, 0, Integer.MAX_VALUE, "Reduces number of caves at surface level, becoming more common until caves generate normally X number of blocks below the surface");
        Configs.verticalCompressionMultiplier = cfg.getFloat("verticalCompressionMultiplier", "cave", Configs.verticalCompressionMultiplier, 0, Float.MAX_VALUE, "Squishes caves on the Y axis. Lower values = taller caves and more steep drops");
        Configs.horizonalCompressionMultiplier = cfg.getFloat("horizonalCompressionMultiplier", "cave", Configs.horizonalCompressionMultiplier, 0, Float.MAX_VALUE, "Streches (when < 1.0) or compresses (when > 1.0) cave generation along X and Z axis");
        Configs.blackListedDims = cfg.get("cave", "blackListedDims", Configs.blackListedDims, "Dimension IDs that will use Vanilla cave generation rather than Worley's Caves").getIntList();
        Configs.maxCaveHeight = cfg.getInt("maxCaveHeight", "cave", Configs.maxCaveHeight, 1, 256, "Caves will not attempt to generate above this y level. Range 1-256");
        Configs.minCaveHeight = cfg.getInt("minCaveHeight", "cave", Configs.minCaveHeight, 1, 256, "Caves will not attempt to generate below this y level. Range 1-256");
        Configs.lavaBlock = cfg.getString("lavaBlock", "cave", Configs.lavaBlock, "Block to use when generating large lava lakes below lavaDepth (usually y=10)");
        Configs.lavaDepth = cfg.getInt("lavaDepth", "cave", Configs.lavaDepth, 1, 256, "Air blocks at or below this y level will generate as lavaBlock");
        Configs.allowReplaceMoreBlocks = cfg.getBoolean("allowReplaceMoreBlocks", "cave", Configs.allowReplaceMoreBlocks, "Allow replacing more blocks with caves (useful for mods which completely overwrite world gen)");
        if (cfg.hasChanged()) cfg.save();
    }
}
