package com.example.slayerscape;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.runelite.api.ItemID;
import net.runelite.api.SpriteID;

@SuppressWarnings("deprecation")
public class SlayerManager
{
    public static final int GRID_SIZE = 50; // 50x50 grid

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
        TaskData taskData = loadTaskData();
        List<Task> allTasks = new ArrayList<>();
        if (taskData != null)
        {
            if (taskData.getEasy() != null) allTasks.addAll(taskData.getEasy());
            if (taskData.getMedium() != null) allTasks.addAll(taskData.getMedium());
            if (taskData.getHard() != null) allTasks.addAll(taskData.getHard());
            if (taskData.getElite() != null) allTasks.addAll(taskData.getElite());
            if (taskData.getMaster() != null) allTasks.addAll(taskData.getMaster());
        }

        // Shuffle all tasks to distribute them randomly, or we could do tiered distribution
        // For now, random distribution but prioritizing tiered distribution would be better.
        // Let's do a simple tiered distribution based on distance from center.

        int center = GRID_SIZE / 2;
        Random rng = new Random();

        for (int x = 0; x < GRID_SIZE; x++)
        {
            for (int y = 0; y < GRID_SIZE; y++)
            {
                int dist = Math.max(Math.abs(x - center), Math.abs(y - center));
                Task task = null;

                // Tier logic
                List<Task> tierTasks = null;
                if (taskData != null) {
                    if (dist < 5) tierTasks = taskData.getEasy();
                    else if (dist < 10) tierTasks = taskData.getMedium();
                    else if (dist < 18) tierTasks = taskData.getHard();
                    else if (dist < 23) tierTasks = taskData.getElite();
                    else tierTasks = taskData.getMaster();
                }

                if (tierTasks == null || tierTasks.isEmpty()) {
                    // Fallback if null or empty
                    if (!allTasks.isEmpty()) {
                        task = allTasks.get(rng.nextInt(allTasks.size()));
                    }
                } else {
                    task = tierTasks.get(rng.nextInt(tierTasks.size()));
                }

                if (task == null) {
                    // Ultimate fallback
                    task = new Task(); // empty task?
                }

                grid[x][y] = new GridTile(x, y, task);
            }
        }

        // Unlock the center tile immediately to start
        grid[center][center].isUnlocked = true;
        grid[center][center].isCompleted = true;
        grid[center][center].requirementText = "Start";
        grid[center][center].iconId = -1;
        grid[center][center].isSprite = false;
        grid[center][center].task = null; // Clear task for start tile
        revealNeighbors(center, center);
    }

    private TaskData loadTaskData()
    {
        try (InputStream is = getClass().getResourceAsStream("/task-list.json"))
        {
            if (is == null) return null;
            return new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), TaskData.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
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
        if (slayerKeys > 0 && !grid[x][y].isUnlocked && isNeighborUnlocked(x, y))
        {
            slayerKeys--;
            grid[x][y].isUnlocked = true;
            return true; // Success
        }
        return false;
    }

    public void completeTask(GridTile tile)
    {
        if (!tile.isCompleted)
        {
            tile.isCompleted = true;
            // No key awarded for manual grid completion
        }
    }

    public boolean isNeighborUnlocked(int x, int y)
    {
        // Check adjacent tiles (N, S, E, W)
        // If center is already completed, we can unlock neighbors.
        // Actually, logic: Can unlock if ANY neighbor is Unlocked OR Completed.
        // If we are at the center, we are unlocked.

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (int[] dir : dirs)
        {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE)
            {
                if (grid[nx][ny].isUnlocked || grid[nx][ny].isCompleted)
                {
                    return true;
                }
            }
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
