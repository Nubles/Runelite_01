package com.example.slayerscape;

import java.util.List;

public class TaskData
{
    private List<Task> easy;
    private List<Task> medium;
    private List<Task> hard;
    private List<Task> elite;
    private List<Task> master;

    public List<Task> getEasy() { return easy; }
    public List<Task> getMedium() { return medium; }
    public List<Task> getHard() { return hard; }
    public List<Task> getElite() { return elite; }
    public List<Task> getMaster() { return master; }
}
