package com.example.slayerscape;

/**
 * Represents a single task assignment for a grid tile.
 * Contains the description, difficulty tier, and icon information.
 */
public class Task
{
    public enum Difficulty
    {
        EASY,
        MEDIUM,
        HARD,
        ELITE
    }

    public enum Type
    {
        SKILL,
        QUEST,
        COMBAT,
        MISC
    }

    private final String description;
    private final Difficulty difficulty;
    private final Type type;
    private final int iconId;
    private final boolean isSprite;

    public Task(String description, Difficulty difficulty, Type type, int iconId, boolean isSprite)
    {
        this.description = description;
        this.difficulty = difficulty;
        this.type = type;
        this.iconId = iconId;
        this.isSprite = isSprite;
    }

    public String getDescription()
    {
        return description;
    }

    public Difficulty getDifficulty()
    {
        return difficulty;
    }

    public Type getType()
    {
        return type;
    }

    public int getIconId()
    {
        return iconId;
    }

    public boolean isSprite()
    {
        return isSprite;
    }
}
