package com.example.slayerscape;

import java.util.Random;
import net.runelite.api.ItemID;

/**
 * The core logic manager for the SlayerScape plugin.
 * Handles the grid state, key management, and task progression.
 */
@SuppressWarnings("deprecation")
public class SlayerManager
{
    public static final int GRID_SIZE = 30; // 30x30 grid

    public GridTile[][] grid = new GridTile[GRID_SIZE][GRID_SIZE];
    public int slayerKeys = 1;

    // Bad Luck Mitigation / Pity Systems
    public int xpTowardNextKey = 0;
    public int slayerTaskDryStreak = 0;

    private final SlayerScapeConfig config;
    private final TaskGenerator taskGenerator;

    public SlayerManager(SlayerScapeConfig config)
    {
        this.config = config;
        this.taskGenerator = new TaskGenerator();
        generateGrid();
    }

    /**
     * Generates the initial grid with difficulty scaling from the center outward.
     */
    private void generateGrid()
    {
        Random rng = new Random();
        int center = GRID_SIZE / 2;

        for (int x = 0; x < GRID_SIZE; x++)
        {
            for (int y = 0; y < GRID_SIZE; y++)
            {
                // Calculate Chebyshev distance (kings move distance) from center
                int dist = Math.max(Math.abs(x - center), Math.abs(y - center));

                Task.Difficulty diff;
                if (dist <= 4) diff = Task.Difficulty.EASY;
                else if (dist <= 9) diff = Task.Difficulty.MEDIUM;
                else if (dist <= 13) diff = Task.Difficulty.HARD;
                else diff = Task.Difficulty.ELITE;

                Task task = taskGenerator.getRandomTask(diff, rng);
                grid[x][y] = new GridTile(x, y, task);
            }
        }

        // Initialize the center "Start" tile
        Task startTask = new Task("Start", Task.Difficulty.EASY, Task.Type.MISC, -1, false);
        grid[center][center] = new GridTile(center, center, startTask);
        grid[center][center].setUnlocked(true);
        grid[center][center].setCompleted(true);
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

    /**
     * Spends a key to unlock a tile at (x, y).
     * @param x Grid X coordinate.
     * @param y Grid Y coordinate.
     * @return true if successful, false otherwise.
     */
    public boolean spendKey(int x, int y)
    {
        if (slayerKeys > 0 && !grid[x][y].isUnlocked() && isNeighborUnlocked(x, y))
        {
            slayerKeys--;
            grid[x][y].setUnlocked(true);
            return true; // Success
        }
        return false;
    }

    /**
     * Marks a tile as completed.
     * @param tile The tile to complete.
     */
    public void completeTask(GridTile tile)
    {
        if (!tile.isCompleted())
        {
            tile.setCompleted(true);
            // No key awarded for manual grid completion
        }
    }

    /**
     * Checks if any neighbor (N, S, E, W) is unlocked or completed.
     * Used for determining reachable tiles (Fog of War).
     * @param x Grid X
     * @param y Grid Y
     * @return true if at least one neighbor is accessible.
     */
    public boolean isNeighborUnlocked(int x, int y)
    {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (int[] dir : dirs)
        {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE)
            {
                if (grid[nx][ny].isUnlocked() || grid[nx][ny].isCompleted())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void revealNeighbors(int x, int y)
    {
        // Placeholder for future logic where completing a tile might auto-unlock neighbors visually
    }
}
