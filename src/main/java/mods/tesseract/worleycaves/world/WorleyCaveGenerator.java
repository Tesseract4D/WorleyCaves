package mods.tesseract.worleycaves.world;


import mods.tesseract.worleycaves.Main;
import mods.tesseract.worleycaves.config.Configs;
import mods.tesseract.worleycaves.util.FastNoise;
import mods.tesseract.worleycaves.util.WorleyUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fluids.IFluidBlock;

public class WorleyCaveGenerator extends MapGenCaves {
    int numLogChunks = 500;
    long[] genTime = new long[numLogChunks];
    int currentTimeIndex = 0;
    double sum = 0;

    private WorleyUtil worleyF1divF3 = new WorleyUtil();
    private FastNoise displacementNoisePerlin = new FastNoise();
    private MapGenBase replacementCaves;
    private MapGenBase moddedCaveGen;
    private static Block lava;
    private static int maxCaveHeight;
    private static int minCaveHeight;
    private static float noiseCutoff;
    private static float warpAmplifier;
    private static float easeInDepth;
    private static float yCompression;
    private static float xzCompression;
    private static float surfaceCutoff;
    private static int lavaDepth;
    private static int HAS_CAVES_FLAG = 129;


    public WorleyCaveGenerator() {
        //TODO noise should probably be seeded with world seed
        worleyF1divF3.SetFrequency(0.016f);

        displacementNoisePerlin.SetNoiseType(FastNoise.NoiseType.Perlin);
        displacementNoisePerlin.SetFrequency(0.05f);

        maxCaveHeight = Configs.cavegen.maxCaveHeight;
        minCaveHeight = Configs.cavegen.minCaveHeight;
        noiseCutoff = (float) Configs.cavegen.noiseCutoffValue;
        warpAmplifier = (float) Configs.cavegen.warpAmplifier;
        easeInDepth = (float) Configs.cavegen.easeInDepth;
        yCompression = (float) Configs.cavegen.verticalCompressionMultiplier;
        xzCompression = (float) Configs.cavegen.horizonalCompressionMultiplier;
        surfaceCutoff = (float) Configs.cavegen.surfaceCutoffValue;
        lavaDepth = Configs.cavegen.lavaDepth;

        lava = Blocks.lava;
        if (lava == null) {
            Main.LOGGER.error("Cannont find block " + Configs.cavegen.lavaBlock);
            lava = Blocks.air;
        }

        //try and grab other modded cave gens, like swiss cheese caves or Quark big caves
        //our replace cavegen event will ignore cave events when the original cave class passed in is a Worley cave
        moddedCaveGen = TerrainGen.getModdedMapGen(this, InitMapGenEvent.EventType.CAVE);
        if (moddedCaveGen != this)
            replacementCaves = moddedCaveGen;
        else
            replacementCaves = new MapGenCaves(); //default to vanilla caves if there are no other modded cave gens
    }

    private void debugValueAdjustments() {
        lavaDepth = 10;
        noiseCutoff = 0.18F;
        warpAmplifier = 8.0F;
        easeInDepth = 15;
        xzCompression = 0.5f;
    }

    @Override
    public void func_151539_a(IChunkProvider provider, World worldIn, int x, int z, Block[] blocks) {
        int currentDim = worldIn.provider.dimensionId;
        this.worldObj = worldIn;
        //revert to vanilla cave generation for blacklisted dims
        for (int blacklistedDim : Configs.cavegen.blackListedDims) {
            if (currentDim == blacklistedDim) {
                this.replacementCaves.func_151539_a(provider, worldIn, x, z, blocks);
                return;
            }
        }

        //debugValueAdjustments();
        this.generateWorleyCaves(worldIn, x, z, blocks);
    }

    public int getTopHeight(Block[] blocks) {
        int y, i = getBlockIndex(7, Configs.cavegen.maxCaveHeight, 7);
        for (; (y = (i & 0xff)) > 0; i--) {
            Block b = blocks[i];
            if (canReplaceBlock(b, Blocks.air))
                break;
        }
        return y;
    }

    protected void generateWorleyCaves(World worldIn, int chunkX, int chunkZ, Block[] blocks) {
        int seaLevel = 63;
        float[][][] samples = sampleNoise(chunkX, chunkZ, getTopHeight(blocks) - 4);
        float oneQuarter = 0.25F;
        float oneHalf = 0.5F;
        BiomeGenBase currentBiome;
        //float cutoffAdjuster = 0F; //TODO one day, perlin adjustments to cutoff

        //each chunk divided into 4 subchunks along X axis
        for (int x = 0; x < 4; x++) {
            //each chunk divided into 4 subchunks along Z axis
            for (int z = 0; z < 4; z++) {
                int depth = 0;

                //don't bother checking all the other logic if there's nothing to dig in this column
                if (samples[x][HAS_CAVES_FLAG][z] == 0 && samples[x + 1][HAS_CAVES_FLAG][z] == 0 && samples[x][HAS_CAVES_FLAG][z + 1] == 0 && samples[x + 1][HAS_CAVES_FLAG][z + 1] == 0)
                    continue;

                //each chunk divided into 128 subchunks along Y axis. Need lots of y sample points to not break things
                for (int y = (maxCaveHeight / 2) - 1; y >= 0; y--) {
                    //grab the 8 sample points needed from the noise values
                    float x0y0z0 = samples[x][y][z];
                    float x0y0z1 = samples[x][y][z + 1];
                    float x1y0z0 = samples[x + 1][y][z];
                    float x1y0z1 = samples[x + 1][y][z + 1];
                    float x0y1z0 = samples[x][y + 1][z];
                    float x0y1z1 = samples[x][y + 1][z + 1];
                    float x1y1z0 = samples[x + 1][y + 1][z];
                    float x1y1z1 = samples[x + 1][y + 1][z + 1];

                    //how much to increment noise along y value
                    //linear interpolation from start y and end y
                    float noiseStepY00 = (x0y1z0 - x0y0z0) * -oneHalf;
                    float noiseStepY01 = (x0y1z1 - x0y0z1) * -oneHalf;
                    float noiseStepY10 = (x1y1z0 - x1y0z0) * -oneHalf;
                    float noiseStepY11 = (x1y1z1 - x1y0z1) * -oneHalf;

                    //noise values of 4 corners at y=0
                    float noiseStartX0 = x0y0z0;
                    float noiseStartX1 = x0y0z1;
                    float noiseEndX0 = x1y0z0;
                    float noiseEndX1 = x1y0z1;

                    // loop through 2 blocks of the Y subchunk
                    for (int suby = 1; suby >= 0; suby--) {
                        int localY = suby + y * 2;
                        float noiseStartZ = noiseStartX0;
                        float noiseEndZ = noiseStartX1;

                        //how much to increment X values, linear interpolation
                        float noiseStepX0 = (noiseEndX0 - noiseStartX0) * oneQuarter;
                        float noiseStepX1 = (noiseEndX1 - noiseStartX1) * oneQuarter;

                        // loop through 4 blocks of the X subchunk
                        for (int subx = 0; subx < 4; subx++) {
                            int localX = subx + x * 4;
                            int realX = chunkX << 4 | localX;

                            //how much to increment Z values, linear interpolation
                            float noiseStepZ = (noiseEndZ - noiseStartZ) * oneQuarter;

                            //Y and X already interpolated, just need to interpolate final 4 Z block to get final noise value
                            float noiseVal = noiseStartZ;

                            // loop through 4 blocks of the Z subchunk
                            for (int subz = 0; subz < 4; subz++) {
                                int localZ = subz + z * 4;
                                int realZ = chunkZ << 4 | localZ;
                                currentBiome = null;

                                if (depth == 0) {
                                    //only checks depth once per 4x4 subchunk
                                    if (subx == 0 && subz == 0) {
                                        Block currentBlock = blocks[getBlockIndex(localX, localZ, localY)];
                                        currentBiome = worldIn.getBiomeGenForCoords(realX, realZ);


                                        //use isDigable to skip leaves/wood getting counted as surface
                                        if (canReplaceBlock(currentBlock, Blocks.air) || isBiomeBlock(blocks, realX, realZ, currentBlock, currentBiome)) {
                                            depth++;
                                        }
                                    } else {
                                        continue;
                                    }
                                } else if (subx == 0 && subz == 0) {
                                    //already hit surface, simply increment depth counter
                                    depth++;
                                }

                                float adjustedNoiseCutoff = noiseCutoff;// + cutoffAdjuster;
                                if (depth < easeInDepth) {
                                    //higher threshold at surface, normal threshold below easeInDepth
                                    adjustedNoiseCutoff = (float) clampedLerp(noiseCutoff, surfaceCutoff, (easeInDepth - (float) depth) / easeInDepth);

                                }

                                //increase cutoff as we get closer to the minCaveHeight so it's not all flat floors
                                if (localY < (minCaveHeight + 5)) {
                                    adjustedNoiseCutoff += ((minCaveHeight + 5) - localY) * 0.05;
                                }

                                if (noiseVal > adjustedNoiseCutoff) {
                                    Block aboveBlock = blocks[getBlockIndex(localX, localZ + 1, localY)];
                                    if (aboveBlock == null)
                                        aboveBlock = Blocks.air;
                                    if (!isFluidBlock(aboveBlock) || localY <= lavaDepth) {
                                        //if we are in the easeInDepth range or near sea level or subH2O is installed, do some extra checks for water before digging
                                        if ((depth < easeInDepth || localY > (seaLevel - 8)) && localY > lavaDepth) {
                                            if (localX < 15)
                                                if (isFluidBlock(blocks[getBlockIndex(localX + 1, localY, localZ)]))
                                                    continue;
                                            if (localX > 0)
                                                if (isFluidBlock(blocks[getBlockIndex(localX - 1, localY, localZ)]))
                                                    continue;
                                            if (localZ < 15)
                                                if (isFluidBlock(blocks[getBlockIndex(localX, localY, localZ + 1)]))
                                                    continue;
                                            if (localZ > 0)
                                                if (isFluidBlock(blocks[getBlockIndex(localX, localY, localZ - 1)]))
                                                    continue;
                                        }
                                        Block currentBlock = blocks[getBlockIndex(localX, localY, localZ)];
                                        if (currentBiome == null)
                                            currentBiome = worldIn.provider.getBiomeGenForCoords(realX, realZ);

                                        boolean foundTopBlock = isTopBlock(currentBlock, currentBiome);
                                        digBlock(blocks, localX, localY, localZ, chunkX, chunkZ, foundTopBlock, currentBlock, aboveBlock, currentBiome);
                                    }
                                }

                                noiseVal += noiseStepZ;
                            }

                            noiseStartZ += noiseStepX0;
                            noiseEndZ += noiseStepX1;
                        }

                        noiseStartX0 += noiseStepY00;
                        noiseStartX1 += noiseStepY01;
                        noiseEndX0 += noiseStepY10;
                        noiseEndX1 += noiseStepY11;
                    }
                }
            }
        }
    }

    public static int getBlockIndex(int x, int y, int z) {
        return x << 12 | z << 8 | y;
    }

    public static double clampedLerp(double lowerBnd, double upperBnd, double slide) {
        if (slide < 0.0D) {
            return lowerBnd;
        } else {
            return slide > 1.0D ? upperBnd : lowerBnd + (upperBnd - lowerBnd) * slide;
        }
    }

    public float[][][] sampleNoise(int chunkX, int chunkZ, int maxSurfaceHeight) {
        int originalMaxHeight = 128;
        float[][][] noiseSamples = new float[5][130][5];
        float noise;
        for (int x = 0; x < 5; x++) {
            int realX = x * 4 + (chunkX << 4);
            for (int z = 0; z < 5; z++) {
                int realZ = z * 4 + (chunkZ << 4);

                int columnHasCaveFlag = 0;

                //loop from top down for y values so we can adjust noise above current y later on
                for (int y = 128; y >= 0; y--) {
                    float realY = y * 2;
                    if (realY > maxSurfaceHeight || realY > maxCaveHeight || realY < minCaveHeight) {
                        //if outside of valid cave range set noise value below normal minimum of -1.0
                        noiseSamples[x][y][z] = -1.1F;
                    } else {
                        //Experiment making the cave system more chaotic the more you descend
                        ///TODO might be too dramatic down at lava level
                        float dispAmp = (float) (warpAmplifier * ((originalMaxHeight - y) / (originalMaxHeight * 0.85)));

                        float xDisp = 0f;
                        float yDisp = 0f;
                        float zDisp = 0f;

                        xDisp = displacementNoisePerlin.GetNoise(realX, realZ) * dispAmp;
                        yDisp = displacementNoisePerlin.GetNoise(realX, realZ + 67.0f) * dispAmp;
                        zDisp = displacementNoisePerlin.GetNoise(realX, realZ + 149.0f) * dispAmp;

                        //doubling the y frequency to get some more caves
                        noise = worleyF1divF3.SingleCellular3Edge(realX * xzCompression + xDisp, realY * yCompression + yDisp, realZ * xzCompression + zDisp);
                        noiseSamples[x][y][z] = noise;

                        if (noise > noiseCutoff) {
                            columnHasCaveFlag = 1;
                            //if noise is below cutoff, adjust values of neighbors
                            //helps prevent caves fracturing during interpolation

                            if (x > 0)
                                noiseSamples[x - 1][y][z] = (noise * 0.2f) + (noiseSamples[x - 1][y][z] * 0.8f);
                            if (z > 0)
                                noiseSamples[x][y][z - 1] = (noise * 0.2f) + (noiseSamples[x][y][z - 1] * 0.8f);

                            //more heavily adjust y above 'air block' noise values to give players more headroom
                            if (y < 128) {
                                float noiseAbove = noiseSamples[x][y + 1][z];
                                if (noise > noiseAbove)
                                    noiseSamples[x][y + 1][z] = (noise * 0.8F) + (noiseAbove * 0.2F);
                                if (y < 127) {
                                    float noiseTwoAbove = noiseSamples[x][y + 2][z];
                                    if (noise > noiseTwoAbove)
                                        noiseSamples[x][y + 2][z] = (noise * 0.35F) + (noiseTwoAbove * 0.65F);
                                }
                            }

                        }
                    }
                }
                noiseSamples[x][HAS_CAVES_FLAG][z] = columnHasCaveFlag; //used to skip cave digging logic when we know there is nothing to dig out
            }
        }
        return noiseSamples;
    }

    private int getSurfaceHeight(Block[] blocks, int localX, int localZ) {
        //Using a recursive binary search to find the surface
        return recursiveBinarySurfaceSearch(blocks, localX, localZ, 255, 0);
    }

    //Recursive binary search, this search always converges on the surface in 8 in cycles for the range 255 >= y >= 0
    private int recursiveBinarySurfaceSearch(Block[] blocks, int localX, int localZ, int searchTop, int searchBottom) {
        int top = searchTop;
        if (searchTop > searchBottom) {
            int searchMid = (searchBottom + searchTop) / 2;
            if (canReplaceBlock(blocks[getBlockIndex(localX, searchMid, localZ)], Blocks.air)) {
                top = recursiveBinarySurfaceSearch(blocks, localX, localZ, searchTop, searchMid + 1);
            } else {
                top = recursiveBinarySurfaceSearch(blocks, localX, localZ, searchMid, searchBottom);
            }
        }
        return top;
    }

    //tests 6 points in hexagon pattern get max height of chunk
    private int getMaxSurfaceHeight(Block[] blocks) {
        int max = 0;
        int[][] testcords = {{2, 6}, {3, 11}, {7, 2}, {9, 13}, {12, 4}, {13, 9}};

        for (int n = 0; n < testcords.length; n++) {

            int testmax = getSurfaceHeight(blocks, testcords[n][0], testcords[n][1]);
            if (testmax > max) {
                max = testmax;
                if (max > maxCaveHeight)
                    return max;
            }

        }
        return max;
    }

    //returns true if block matches the top or filler block of the location biome
    private boolean isBiomeBlock(Block[] blocks, int realX, int realZ, Block block, BiomeGenBase biome) {
        return block == biome.topBlock || block == biome.fillerBlock;
    }

    //returns true if block is fluid, trying to play nice with modded liquid
    private boolean isFluidBlock(Block blocky) {
        return blocky instanceof BlockLiquid || blocky instanceof IFluidBlock;
    }

    //Because it's private in MapGenCaves this is reimplemented
    //Determine if the block at the specified location is the top block for the biome, we take into account
    //Vanilla bugs to make sure that we generate the map the same way vanilla does.
    private boolean isTopBlock(Block block, BiomeGenBase biome) {
        //IBlockState state = data.getBlockState(x, y, z);
        return (isExceptionBiome(biome) ? block == Blocks.grass : block == biome.topBlock);
    }

    //Exception biomes to make sure we generate like vanilla
    private boolean isExceptionBiome(BiomeGenBase biome) {
        return biome == BiomeGenBase.desert || biome == BiomeGenBase.beach || biome == BiomeGenBase.mushroomIsland;
    }

    protected boolean canReplaceBlock(Block block, Block blockUp) {
        if (block == null || blockUp.getMaterial() == Material.water)
            return false;
        return (Configs.cavegen.allowReplaceMoreBlocks && block.getMaterial() == Material.rock)
            || block == Blocks.stone
            || block == Blocks.dirt
            || block == Blocks.grass
            || block == Blocks.hardened_clay
            || block == Blocks.stained_hardened_clay
            || block == Blocks.sandstone
            || block == Blocks.mycelium
            || block == Blocks.snow_layer
            || block == Blocks.sand || block == Blocks.gravel;
    }

    /**
     * Digs out the current block, default implementation removes stone, filler, and top block
     * Sets the block to lava if y is less then 10, and air other wise.
     * If setting to air, it also checks to see if we've broken the surface and if so
     * tries to make the floor the biome's top block
     *
     * @param data     Block data array
     * @param index    Pre-calculated index into block data
     * @param x        local X position
     * @param y        local Y position
     * @param z        local Z position
     * @param chunkX   Chunk X position
     * @param chunkZ   Chunk Y position
     * @param foundTop True if we've encountered the biome's top block. Ideally if we've broken the surface.
     */
    protected void digBlock(Block[] data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop, Block block, Block up, BiomeGenBase biome) {
        Block top = biome.topBlock;
        Block filler = biome.fillerBlock;
        int index = getBlockIndex(x, y, z);


        if (this.canReplaceBlock(block, up) || block == top || block == filler) {
            if (y <= lavaDepth) {
                data[index] = lava;
            } else {
                data[index] = Blocks.air;

                if (foundTop && data[index - 1] == filler) {
                    data[index - 1] = top;
                }

                //replace floating sand with sandstone
                if (up == Blocks.sand) {
                    data[index + 1] = Blocks.sandstone;
                }
            }
        }
    }
}
