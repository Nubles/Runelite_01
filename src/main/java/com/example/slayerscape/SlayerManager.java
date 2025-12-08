package com.example.slayerscape;

import java.util.Random;

public class SlayerManager
{
    public static final int GRID_SIZE = 11; // 11x11 grid
    public GridTile[][] grid = new GridTile[GRID_SIZE][GRID_SIZE];
    public int slayerKeys = 0;

    public SlayerManager()
    {
        generateGrid();
    }

    private void generateGrid()
    {
        Random rng = new Random();
        String[] possibleTasks = {"Kill 10 Cows", "Mining Level 5", "Cook a Shrimp", "Complete Imp Catcher"};

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
