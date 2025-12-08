package com.example.slayerscape;

public class GridTile
{
    public int x, y;
    public boolean isUnlocked = false; // Can we see the challenge?
    public boolean isCompleted = false; // Have we finished the challenge?
    public String requirementText; // e.g., "Kill 10 Goblins" or "Mining Level 5"
    public int iconId = -1; // Item ID or Sprite ID
    public boolean isSprite = false; // true if iconId is a sprite, false if item ID

    public GridTile(int x, int y, String req)
    {
        this.x = x;
        this.y = y;
        this.requirementText = req;
    }
}
