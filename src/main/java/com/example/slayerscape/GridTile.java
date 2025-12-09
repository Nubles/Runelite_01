package com.example.slayerscape;

/**
 * Represents a tile on the SlayerScape grid.
 * Holds the coordinates, state (locked/completed), and the assigned Task.
 */
public class GridTile
{
    private final int x;
    private final int y;

    private boolean isUnlocked = false; // Is the tile visible/active?
    private boolean isCompleted = false; // Is the task finished?

    private final Task task;

    public GridTile(int x, int y, Task task)
    {
        this.x = x;
        this.y = y;
        this.task = task;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public boolean isUnlocked()
    {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked)
    {
        isUnlocked = unlocked;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }

    public void setCompleted(boolean completed)
    {
        isCompleted = completed;
    }

    public Task getTask()
    {
        return task;
    }
}
