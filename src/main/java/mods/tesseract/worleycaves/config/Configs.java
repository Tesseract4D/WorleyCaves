package mods.tesseract.worleycaves.config;


public class Configs
{
	public static ConfigCaveGen cavegen = new ConfigCaveGen();

	public static class ConfigCaveGen
	{

		public double noiseCutoffValue = -0.18;

		public double surfaceCutoffValue = -0.081;

		public double warpAmplifier = 8.0;

		public int easeInDepth = 15;

		public double verticalCompressionMultiplier = 2.0;

	    public double horizonalCompressionMultiplier = 1.0;

		public int[] blackListedDims = {};

		public int maxCaveHeight = 128;

		public int minCaveHeight = 1;

		public String lavaBlock = "minecraft:lava";

		public int lavaDepth = 10;

		public boolean allowReplaceMoreBlocks = true;
	}
}
