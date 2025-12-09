package com.example.slayerscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.runelite.api.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

/**
 * Manages the generation and categorization of tasks for the SlayerScape grid.
 */
@SuppressWarnings("deprecation")
public class TaskGenerator
{
    private final List<Task> easyTasks = new ArrayList<>();
    private final List<Task> mediumTasks = new ArrayList<>();
    private final List<Task> hardTasks = new ArrayList<>();
    private final List<Task> eliteTasks = new ArrayList<>();

    public TaskGenerator()
    {
        generateSkillTasks();
        generateQuestTasks();
        generateMiscTasks();
    }

    /**
     * Retrieves a random task of the specified difficulty.
     * @param difficulty The desired difficulty tier.
     * @param rng The Random instance to use.
     * @return A Task object.
     */
    public Task getRandomTask(Task.Difficulty difficulty, Random rng)
    {
        List<Task> pool;
        switch (difficulty)
        {
            case EASY: pool = easyTasks; break;
            case MEDIUM: pool = mediumTasks; break;
            case HARD: pool = hardTasks; break;
            case ELITE: pool = eliteTasks; break;
            default: pool = mediumTasks; break;
        }

        if (pool.isEmpty()) return new Task("Fallback Task", Task.Difficulty.EASY, Task.Type.MISC, -1, false);
        return pool.get(rng.nextInt(pool.size()));
    }

    private void addTask(Task task)
    {
        switch (task.getDifficulty())
        {
            case EASY: easyTasks.add(task); break;
            case MEDIUM: mediumTasks.add(task); break;
            case HARD: hardTasks.add(task); break;
            case ELITE: eliteTasks.add(task); break;
        }
    }

    private void generateSkillTasks()
    {
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL) continue;
            int icon = getSkillIcon(skill);

            for (int level = 5; level <= 99; level++)
            {
                Task.Difficulty diff;
                if (level <= 25) diff = Task.Difficulty.EASY;
                else if (level <= 55) diff = Task.Difficulty.MEDIUM;
                else if (level <= 85) diff = Task.Difficulty.HARD;
                else diff = Task.Difficulty.ELITE;

                addTask(new Task(skill.getName() + " Level " + level, diff, Task.Type.SKILL, icon, true));
            }
        }
    }

    private void generateQuestTasks()
    {
        // Difficulty heuristic:
        // Quests don't have built-in difficulty in the Enum.
        // We will randomize them into Medium/Hard/Elite, with F2P-ish ones manually in Easy if possible?
        // Checking F2P vs P2P via API isn't easy without client.
        // We'll treat all Quests as MEDIUM/HARD/ELITE pool, avoiding EASY to keep the center specific to basic skilling/kills.

        for (Quest quest : Quest.values())
        {
            // Heuristic: Longer names -> Harder? No.
            // Just Randomly assign to Medium, Hard, Elite
            // actually, we should just put them all in a pool and let the manager decide?
            // No, the prompt wants difficulty scaling.
            // Let's put Quests primarily in Medium and Hard.

            // We can check some known easy ones.
            String name = quest.getName().toLowerCase();
            Task.Difficulty diff = Task.Difficulty.HARD; // Default

            if (name.contains("cook's assistant") || name.contains("sheep shearer") || name.contains("rune mysteries") ||
                name.contains("imp catcher") || name.contains("restless ghost") || name.contains("vampire slayer") ||
                name.contains("demon slayer") || name.contains("dorics quest") || name.contains("romeo") ||
                name.contains("gertrude") || name.contains("x marks the spot"))
            {
                diff = Task.Difficulty.EASY;
            }
            else if (name.contains("dragon slayer") || name.contains("monkey madness") || name.contains("desert treasure") ||
                     name.contains("recipe for disaster") || name.contains("grand tree") || name.contains("heroes") ||
                     name.contains("legends") || name.contains("regicide") || name.contains("mourning"))
            {
                diff = Task.Difficulty.ELITE;
            }
            else
            {
                // Split remainder between Medium and Hard
                diff = (name.length() % 2 == 0) ? Task.Difficulty.MEDIUM : Task.Difficulty.HARD;
            }

            addTask(new Task("Complete " + quest.getName(), diff, Task.Type.QUEST, SpriteID.TAB_QUESTS, true));
        }
    }

    private void generateMiscTasks()
    {
        // Easy
        addTask(new Task("Kill 10 Cows", Task.Difficulty.EASY, Task.Type.COMBAT, ItemID.COWHIDE, false));
        addTask(new Task("Kill 10 Chickens", Task.Difficulty.EASY, Task.Type.COMBAT, ItemID.FEATHER, false));
        addTask(new Task("Kill 10 Goblins", Task.Difficulty.EASY, Task.Type.COMBAT, ItemID.GOBLIN_MAIL, false));
        addTask(new Task("Kill 5 Giant Rats", Task.Difficulty.EASY, Task.Type.COMBAT, ItemID.BONES, false));
        addTask(new Task("Cook a Shrimp", Task.Difficulty.EASY, Task.Type.MISC, ItemID.SHRIMPS, false));
        addTask(new Task("Burn a normal log", Task.Difficulty.EASY, Task.Type.MISC, ItemID.LOGS, false));
        addTask(new Task("Catch a Shrimp", Task.Difficulty.EASY, Task.Type.MISC, ItemID.RAW_SHRIMPS, false));
        addTask(new Task("Bury a Big Bones", Task.Difficulty.EASY, Task.Type.MISC, ItemID.BIG_BONES, false));

        // Medium
        addTask(new Task("Kill 1 Hill Giant", Task.Difficulty.MEDIUM, Task.Type.COMBAT, ItemID.BIG_BONES, false));
        addTask(new Task("Kill 1 Moss Giant", Task.Difficulty.MEDIUM, Task.Type.COMBAT, ItemID.BIG_BONES, false));
        addTask(new Task("Kill 5 Guards", Task.Difficulty.MEDIUM, Task.Type.COMBAT, ItemID.IRON_BOOTS, false));
        addTask(new Task("Smelt a Iron Bar", Task.Difficulty.MEDIUM, Task.Type.MISC, ItemID.IRON_BAR, false));
        addTask(new Task("Teleport to Varrock", Task.Difficulty.MEDIUM, Task.Type.MISC, SpriteID.SPELL_VARROCK_TELEPORT, true));

        // Hard
        addTask(new Task("Kill 1 Blue Dragon", Task.Difficulty.HARD, Task.Type.COMBAT, ItemID.DRAGON_BONES, false));
        addTask(new Task("Kill 1 Green Dragon", Task.Difficulty.HARD, Task.Type.COMBAT, ItemID.DRAGON_BONES, false));
        addTask(new Task("Kill 1 Lesser Demon", Task.Difficulty.HARD, Task.Type.COMBAT, ItemID.ASHES, false));
        addTask(new Task("Teleport to Camelot", Task.Difficulty.HARD, Task.Type.MISC, SpriteID.SPELL_CAMELOT_TELEPORT, true));

        // Elite
        addTask(new Task("Kill 1 Black Dragon", Task.Difficulty.ELITE, Task.Type.COMBAT, ItemID.DRAGON_BONES, false));
        addTask(new Task("Kill 1 Jad", Task.Difficulty.ELITE, Task.Type.COMBAT, ItemID.FIRE_CAPE, false));
    }

    private int getSkillIcon(Skill skill)
    {
        switch (skill) {
            case ATTACK: return SpriteID.SKILL_ATTACK;
            case DEFENCE: return SpriteID.SKILL_DEFENCE;
            case STRENGTH: return SpriteID.SKILL_STRENGTH;
            case HITPOINTS: return SpriteID.SKILL_HITPOINTS;
            case RANGED: return SpriteID.SKILL_RANGED;
            case PRAYER: return SpriteID.SKILL_PRAYER;
            case MAGIC: return SpriteID.SKILL_MAGIC;
            case COOKING: return SpriteID.SKILL_COOKING;
            case WOODCUTTING: return SpriteID.SKILL_WOODCUTTING;
            case FLETCHING: return SpriteID.SKILL_FLETCHING;
            case FISHING: return SpriteID.SKILL_FISHING;
            case FIREMAKING: return SpriteID.SKILL_FIREMAKING;
            case CRAFTING: return SpriteID.SKILL_CRAFTING;
            case SMITHING: return SpriteID.SKILL_SMITHING;
            case MINING: return SpriteID.SKILL_MINING;
            case HERBLORE: return SpriteID.SKILL_HERBLORE;
            case AGILITY: return SpriteID.SKILL_AGILITY;
            case THIEVING: return SpriteID.SKILL_THIEVING;
            case SLAYER: return SpriteID.SKILL_SLAYER;
            case FARMING: return SpriteID.SKILL_FARMING;
            case RUNECRAFT: return SpriteID.SKILL_RUNECRAFT;
            case HUNTER: return SpriteID.SKILL_HUNTER;
            case CONSTRUCTION: return SpriteID.SKILL_CONSTRUCTION;
            default: return SpriteID.SKILL_TOTAL;
        }
    }

}
