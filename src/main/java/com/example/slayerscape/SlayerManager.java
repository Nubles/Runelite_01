package com.example.slayerscape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.runelite.api.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

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

    public SlayerManager(SlayerScapeConfig config)
    {
        this.config = config;
        generateGrid();
    }

    private void generateGrid()
    {
        Random rng = new Random();
        List<String> possibleTasks = new ArrayList<>();

        // 1. Add All Skills 1-99
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL) continue; // Skip Total Level
            // Add intervals or all? "All skilling tasks that are possible"
            // Let's add every 5 levels to keep it sane but extensive?
            // Or truly all? 1-99 is 23 * 99 = 2277 tasks. That's fine.
            // But maybe too much noise? User said "all skilling tasks".
            // I'll add levels 5, 10, 15, ... 95, 99.
            // Actually, let's do 1-99 for variety.
            for (int level = 5; level <= 99; level++) {
                possibleTasks.add(skill.getName() + " Level " + level);
            }
        }

        // 2. Add All Quests
        for (Quest quest : Quest.values())
        {
            possibleTasks.add("Complete " + quest.getName());
        }

        // 3. Manual Tasks (Combat/Misc)
        String[] manualTasks = {
            // Kill Tasks
            "Kill 10 Cows", "Kill 10 Chickens", "Kill 10 Goblins", "Kill 5 Giant Rats",
            "Kill 5 Al-Kharid Warriors", "Kill 1 Hill Giant", "Kill 1 Moss Giant",
            "Kill 5 Guards", "Kill 5 Dwarves", "Kill 5 Skeletons", "Kill 1 Blue Dragon",
            "Kill 1 Green Dragon", "Kill 1 Lesser Demon", "Kill 1 Greater Demon",

            // Miscellaneous
            "Cook a Shrimp", "Burn a normal log", "Smelt a Bronze Bar", "Catch a Shrimp",
            "Bury a Big Bones", "Craft a Leather Cowl", "Craft a Gold Amulet",
            "Teleport to Varrock", "Teleport to Falador", "Teleport to Lumbridge"
        };

        for (String t : manualTasks) possibleTasks.add(t);

        for (int x = 0; x < GRID_SIZE; x++)
        {
            for (int y = 0; y < GRID_SIZE; y++)
            {
                // Pick a random task for every tile
                String randomTask = possibleTasks.get(rng.nextInt(possibleTasks.size()));
                grid[x][y] = new GridTile(x, y, randomTask);
                assignIcon(grid[x][y]);
            }
        }

        // Unlock the center tile immediately to start
        int center = GRID_SIZE / 2;
        grid[center][center].isUnlocked = true;
        grid[center][center].isCompleted = true;
        grid[center][center].requirementText = "Start";
        grid[center][center].iconId = -1;
        grid[center][center].isSprite = false;
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
    }

    private void assignIcon(GridTile tile)
    {
        String req = tile.requirementText.toLowerCase();

        // Check Skills
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;
            if (req.contains(skill.getName().toLowerCase() + " level")) {
                tile.isSprite = true;
                switch (skill) {
                    case ATTACK: tile.iconId = SpriteID.SKILL_ATTACK; return;
                    case DEFENCE: tile.iconId = SpriteID.SKILL_DEFENCE; return;
                    case STRENGTH: tile.iconId = SpriteID.SKILL_STRENGTH; return;
                    case HITPOINTS: tile.iconId = SpriteID.SKILL_HITPOINTS; return;
                    case RANGED: tile.iconId = SpriteID.SKILL_RANGED; return;
                    case PRAYER: tile.iconId = SpriteID.SKILL_PRAYER; return;
                    case MAGIC: tile.iconId = SpriteID.SKILL_MAGIC; return;
                    case COOKING: tile.iconId = SpriteID.SKILL_COOKING; return;
                    case WOODCUTTING: tile.iconId = SpriteID.SKILL_WOODCUTTING; return;
                    case FLETCHING: tile.iconId = SpriteID.SKILL_FLETCHING; return;
                    case FISHING: tile.iconId = SpriteID.SKILL_FISHING; return;
                    case FIREMAKING: tile.iconId = SpriteID.SKILL_FIREMAKING; return;
                    case CRAFTING: tile.iconId = SpriteID.SKILL_CRAFTING; return;
                    case SMITHING: tile.iconId = SpriteID.SKILL_SMITHING; return;
                    case MINING: tile.iconId = SpriteID.SKILL_MINING; return;
                    case HERBLORE: tile.iconId = SpriteID.SKILL_HERBLORE; return;
                    case AGILITY: tile.iconId = SpriteID.SKILL_AGILITY; return;
                    case THIEVING: tile.iconId = SpriteID.SKILL_THIEVING; return;
                    case SLAYER: tile.iconId = SpriteID.SKILL_SLAYER; return;
                    case FARMING: tile.iconId = SpriteID.SKILL_FARMING; return;
                    case RUNECRAFT: tile.iconId = SpriteID.SKILL_RUNECRAFT; return;
                    case HUNTER: tile.iconId = SpriteID.SKILL_HUNTER; return;
                    case CONSTRUCTION: tile.iconId = SpriteID.SKILL_CONSTRUCTION; return;
                    default: tile.iconId = SpriteID.SKILL_TOTAL; return;
                }
            }
        }

        // Quests
        if (req.contains("complete ")) {
             tile.iconId = SpriteID.TAB_QUESTS;
             tile.isSprite = true;
             return;
        }

        // Mobs / Items - Use Item IDs
        if (req.contains("cow")) { tile.iconId = ItemID.COWHIDE; tile.isSprite = false; }
        else if (req.contains("chicken")) { tile.iconId = ItemID.FEATHER; tile.isSprite = false; }
        else if (req.contains("goblin")) { tile.iconId = ItemID.GOBLIN_MAIL; tile.isSprite = false; }
        else if (req.contains("rat")) { tile.iconId = ItemID.BONES; tile.isSprite = false; }
        else if (req.contains("giant")) { tile.iconId = ItemID.BIG_BONES; tile.isSprite = false; }
        else if (req.contains("guard")) { tile.iconId = ItemID.IRON_BOOTS; tile.isSprite = false; }
        else if (req.contains("dwarf")) { tile.iconId = ItemID.DWARF_REMAINS; tile.isSprite = false; }
        else if (req.contains("skeleton")) { tile.iconId = ItemID.BONES; tile.isSprite = false; }
        else if (req.contains("dragon")) { tile.iconId = ItemID.DRAGON_BONES; tile.isSprite = false; }
        else if (req.contains("demon")) { tile.iconId = ItemID.ASHES; tile.isSprite = false; }

        // Misc
        else if (req.contains("shrimp")) { tile.iconId = ItemID.RAW_SHRIMPS; tile.isSprite = false; }
        else if (req.contains("log")) { tile.iconId = ItemID.LOGS; tile.isSprite = false; }
        else if (req.contains("bronze bar")) { tile.iconId = ItemID.BRONZE_BAR; tile.isSprite = false; }
        else if (req.contains("bones")) { tile.iconId = ItemID.BIG_BONES; tile.isSprite = false; }
        else if (req.contains("cowl")) { tile.iconId = ItemID.LEATHER_COWL; tile.isSprite = false; }
        else if (req.contains("amulet")) { tile.iconId = ItemID.GOLD_AMULET; tile.isSprite = false; }
        else if (req.contains("teleport")) { tile.iconId = SpriteID.SPELL_VARROCK_TELEPORT; tile.isSprite = true; }
        else {
             // Fallback
             tile.iconId = -1;
             tile.isSprite = false;
        }
    }
}
