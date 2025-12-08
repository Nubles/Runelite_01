package com.example.slayerscape;

import java.util.Random;

public class SlayerManager
{
    public static final int GRID_SIZE = 11; // 11x11 grid

    public GridTile[][] grid = new GridTile[GRID_SIZE][GRID_SIZE];
    public int slayerKeys = 0;

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
}
