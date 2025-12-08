package com.example.slayerscape;

import java.util.Random;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

@SuppressWarnings("deprecation")
public class SlayerManager
{
    public static final int GRID_SIZE = 11; // 11x11 grid

    public GridTile[][] grid = new GridTile[GRID_SIZE][GRID_SIZE];
    public int slayerKeys = 1;

    // Bad Luck Mitigation / Pity Systems
    public int xpTowardNextKey = 0;
    public int slayerTaskDryStreak = 0;

    private final SlayerScapeConfig config;

    public SlayerManager(SlayerScapeConfig config)
    {
        this.config = config;
        generateGrid();
    }

    private void generateGrid()
    {
        Random rng = new Random();
        String[] possibleTasks = {
            // Skilling
            "Mining Level 5", "Mining Level 10", "Mining Level 20",
            "Woodcutting Level 5", "Woodcutting Level 10", "Woodcutting Level 20",
            "Fishing Level 5", "Fishing Level 10", "Fishing Level 20",
            "Cooking Level 5", "Cooking Level 10", "Cooking Level 20",
            "Firemaking Level 5", "Firemaking Level 10", "Firemaking Level 20",
            "Smithing Level 5", "Smithing Level 10", "Smithing Level 20",
            "Crafting Level 5", "Crafting Level 10", "Crafting Level 20",

            // Combat
            "Attack Level 5", "Attack Level 10", "Strength Level 5", "Strength Level 10",
            "Defense Level 5", "Defense Level 10", "Ranged Level 5", "Ranged Level 10",
            "Magic Level 5", "Magic Level 10", "Prayer Level 5", "Prayer Level 10",

            // Kill Tasks
            "Kill 10 Cows", "Kill 10 Chickens", "Kill 10 Goblins", "Kill 5 Giant Rats",
            "Kill 5 Al-Kharid Warriors", "Kill 1 Hill Giant", "Kill 1 Moss Giant",
            "Kill 5 Guards", "Kill 5 Dwarves", "Kill 5 Skeletons",

            // Quests (F2P)
            "Complete Cook's Assistant", "Complete Sheep Shearer", "Complete Rune Mysteries",
            "Complete Imp Catcher", "Complete The Restless Ghost", "Complete Vampire Slayer",
            "Complete Demon Slayer", "Complete Dragon Slayer I", "Complete Ernest the Chicken",

            // Miscellaneous
            "Cook a Shrimp", "Burn a normal log", "Smelt a Bronze Bar", "Catch a Shrimp",
            "Bury a Big Bones", "Craft a Leather Cowl"
        };

        for (int x = 0; x < GRID_SIZE; x++)
        {
            for (int y = 0; y < GRID_SIZE; y++)
            {
                // Pick a random task for every tile
                String randomTask = possibleTasks[rng.nextInt(possibleTasks.length)];
                grid[x][y] = new GridTile(x, y, randomTask);
                assignIcon(grid[x][y]);
            }
        }

        // Unlock the center tile immediately to start
        int center = GRID_SIZE / 2;
        grid[center][center].isUnlocked = true;
        grid[center][center].isCompleted = true;
        revealNeighbors(center, center);
    }

    public void addKey()
    {
        slayerKeys++;
    }

    /**
     * Adds XP to the pity counter.
     * @param amount The amount of XP gained.
     * @return The number of keys awarded (usually 0 or 1).
     */
    public int addXp(int amount)
    {
        if (amount <= 0 || !config.enableXpPity()) return 0;

        xpTowardNextKey += amount;
        int keysAwarded = 0;
        int threshold = config.xpThreshold();

        while (xpTowardNextKey >= threshold)
        {
            xpTowardNextKey -= threshold;
            addKey();
            keysAwarded++;
        }
        return keysAwarded;
    }

    /**
     * Attempts to award a key from a Slayer Task with bad luck mitigation.
     * @return true if a key was awarded.
     */
    public boolean attemptSlayerTaskKey()
    {
        double chance = 0.50; // Base chance

        if (config.enableDryStreak())
        {
            // Every failure adds 10% to the chance
            chance += (slayerTaskDryStreak * 0.10);
        }

        if (new Random().nextDouble() < chance)
        {
            addKey();
            slayerTaskDryStreak = 0;
            return true;
        }
        else
        {
            slayerTaskDryStreak++;
            return false;
        }
    }

    public boolean spendKey(int x, int y)
    {
        if (slayerKeys > 0 && !grid[x][y].isUnlocked)
        {
            slayerKeys--;
            grid[x][y].isUnlocked = true;
            return true; // Success
        }
        return false;
    }

    // When a tile is "Completed" (task done), reveal surroundings
    public void completeTile(int x, int y)
    {
        grid[x][y].isCompleted = true;
        revealNeighbors(x, y);
    }

    private void revealNeighbors(int x, int y)
    {
        // Simple logic to unlock adjacent tiles visually (Fog of War removal)
        // In a real app, you'd mark them as "Visible" here.
    }

    private void assignIcon(GridTile tile)
    {
        String req = tile.requirementText.toLowerCase();

        // Skills - Default to using Skill Sprites
        // Note: In a real plugin you might map these via an Enum or Map
        if (req.contains("mining")) { tile.iconId = SpriteID.SKILL_MINING; tile.isSprite = true; }
        else if (req.contains("woodcutting")) { tile.iconId = SpriteID.SKILL_WOODCUTTING; tile.isSprite = true; }
        else if (req.contains("fishing")) { tile.iconId = SpriteID.SKILL_FISHING; tile.isSprite = true; }
        else if (req.contains("cooking")) { tile.iconId = SpriteID.SKILL_COOKING; tile.isSprite = true; }
        else if (req.contains("firemaking")) { tile.iconId = SpriteID.SKILL_FIREMAKING; tile.isSprite = true; }
        else if (req.contains("smithing")) { tile.iconId = SpriteID.SKILL_SMITHING; tile.isSprite = true; }
        else if (req.contains("crafting")) { tile.iconId = SpriteID.SKILL_CRAFTING; tile.isSprite = true; }
        else if (req.contains("attack")) { tile.iconId = SpriteID.SKILL_ATTACK; tile.isSprite = true; }
        else if (req.contains("strength")) { tile.iconId = SpriteID.SKILL_STRENGTH; tile.isSprite = true; }
        else if (req.contains("defense")) { tile.iconId = SpriteID.SKILL_DEFENCE; tile.isSprite = true; }
        else if (req.contains("ranged")) { tile.iconId = SpriteID.SKILL_RANGED; tile.isSprite = true; }
        else if (req.contains("magic")) { tile.iconId = SpriteID.SKILL_MAGIC; tile.isSprite = true; }
        else if (req.contains("prayer")) { tile.iconId = SpriteID.SKILL_PRAYER; tile.isSprite = true; }

        // Mobs / Items - Use Item IDs
        else if (req.contains("cow")) { tile.iconId = ItemID.COWHIDE; tile.isSprite = false; }
        else if (req.contains("chicken")) { tile.iconId = ItemID.FEATHER; tile.isSprite = false; }
        else if (req.contains("goblin")) { tile.iconId = ItemID.GOBLIN_MAIL; tile.isSprite = false; }
        else if (req.contains("rat")) { tile.iconId = ItemID.BONES; tile.isSprite = false; } // Close enough (Rat tail not in generic ItemID?)
        else if (req.contains("giant")) { tile.iconId = ItemID.BIG_BONES; tile.isSprite = false; }
        else if (req.contains("guard")) { tile.iconId = ItemID.IRON_BOOTS; tile.isSprite = false; } // Close enough
        else if (req.contains("dwarf")) { tile.iconId = ItemID.DWARF_REMAINS; tile.isSprite = false; }
        else if (req.contains("skeleton")) { tile.iconId = ItemID.BONES; tile.isSprite = false; } // BONE_SHARDS might be too new

        // Quests - Use Quest Sprite
        else if (req.contains("complete")) { tile.iconId = SpriteID.TAB_QUESTS; tile.isSprite = true; }

        // Misc
        else if (req.contains("shrimp")) { tile.iconId = ItemID.RAW_SHRIMPS; tile.isSprite = false; }
        else if (req.contains("log")) { tile.iconId = ItemID.LOGS; tile.isSprite = false; }
        else if (req.contains("bronze bar")) { tile.iconId = ItemID.BRONZE_BAR; tile.isSprite = false; }
        else if (req.contains("bones")) { tile.iconId = ItemID.BIG_BONES; tile.isSprite = false; }
        else if (req.contains("cowl")) { tile.iconId = ItemID.LEATHER_COWL; tile.isSprite = false; }
    }
}
