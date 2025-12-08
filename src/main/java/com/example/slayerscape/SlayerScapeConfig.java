package com.example.slayerscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("slayerscape")
public interface SlayerScapeConfig extends Config
{
    @ConfigItem(
        keyName = "enableXpPity",
        name = "Enable XP Pity System",
        description = "Awards a key periodically based on XP gained to prevent soft-locks."
    )
    default boolean enableXpPity()
    {
        return true;
    }

    @ConfigItem(
        keyName = "xpThreshold",
        name = "XP Threshold per Key",
        description = "Amount of XP required to earn a pity key."
    )
    default int xpThreshold()
    {
        return 25000;
    }

    @ConfigItem(
        keyName = "enableDryStreak",
        name = "Enable Dry Streak Mitigation",
        description = "Increases key drop chance after every task that doesn't yield a key."
    )
    default boolean enableDryStreak()
    {
        return true;
    }
}
