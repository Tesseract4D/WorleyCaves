package mods.tesseract.worleycaves;


import mods.tesseract.worleycaves.event.CaveEvent;
import mods.tesseract.worleycaves.util.Reference;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION, acceptableRemoteVersions="*")
public class Main
{
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

	@EventHandler
	public static void preInit(FMLPreInitializationEvent e)
	{
        MinecraftForge.TERRAIN_GEN_BUS.register(new CaveEvent());
	}
}
