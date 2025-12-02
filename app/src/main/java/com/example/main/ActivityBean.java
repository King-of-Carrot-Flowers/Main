package com.example.main;
public class ActivityBean {
    private int id;
    private String title;
    private String type;
    private String time;
    private String location;
    private String description;
    private String organizer;
    private int maxPeople;

    private String tag;
    // 构造方法
    public ActivityBean() {}

    public ActivityBean(int id, String title, String type, String time, String location,
                    String description, String organizer, int maxPeople,String tag) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.time = time;
        this.location = location;
        this.description = description;
        this.organizer = organizer;
        this.maxPeople = maxPeople;
        this.tag = tag;
    }

    // Getter和Setter方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public int getMaxPeople() { return maxPeople; }
    public void setMaxPeople(int maxPeople) { this.maxPeople = maxPeople; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}

