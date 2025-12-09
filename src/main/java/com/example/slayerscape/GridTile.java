package com.example.slayerscape;

public class GridTile
{
    public int x, y;
    public boolean isUnlocked = false; // Can we see the challenge?
    public boolean isCompleted = false; // Have we finished the challenge?
    public String requirementText; // e.g., "Kill 10 Goblins" or "Mining Level 5"
    public int iconId = -1; // Item ID or Sprite ID
    public boolean isSprite = false; // true if iconId is a sprite, false if item ID
    public Task task; // The associated task object

    public GridTile(int x, int y, Task task)
    {
        this.x = x;
        this.y = y;
        this.task = task;
        if (task != null) {
            this.requirementText = task.getName();
            this.iconId = task.getDisplayItemId();
            this.isSprite = false; // Always Item ID from task list
        } else {
            this.requirementText = "Start";
            this.iconId = -1;
        }
    }
}
