package com.example.slayerscape;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Task
{
    private String id;
    private String name;
    private String tip;
    private String wikiLink;
    private String imageLink;
    private int displayItemId;
    @SerializedName("verification")
    private Verification verification;

    public static class Verification
    {
        private String method;
        private List<Integer> itemIds;
        private int count;
        private String region;
        private String difficulty;
        // Other fields if needed
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTip() { return tip; }
    public String getWikiLink() { return wikiLink; }
    public String getImageLink() { return imageLink; }
    public int getDisplayItemId() { return displayItemId; }
    public Verification getVerification() { return verification; }
}
