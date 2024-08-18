package mods.tesseract.worleycaves.event;

import mods.tesseract.worleycaves.world.WorleyCaveGenerator;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;

public class CaveEvent
{
	@SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCaveEvent(InitMapGenEvent event)
	{
		//only replace cave gen if the original gen passed isn't a worley cave
		if (event.type == InitMapGenEvent.EventType.CAVE && !event.originalGen.getClass().equals(WorleyCaveGenerator.class))
	    {
			//Main.LOGGER.info("Replacing cave generation with Worley Caves");
	        event.newGen=new WorleyCaveGenerator();
	    }
	}

}
