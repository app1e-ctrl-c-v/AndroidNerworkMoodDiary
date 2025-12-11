package com.example.androidnetworkmooddiary.models;

import java.util.Date;

public class MoodDiary {
    private int id;
    private String user_id;
    private String record_date;
    private String comment_day;
    private int mood_assessment;

    public MoodDiary() {
    }

    public String getComment_day() {
        return comment_day;
    }

    public void setComment_day(String comment_day) {
        this.comment_day = comment_day;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMood_assessment() {
        return mood_assessment;
    }

    public void setMood_assessment(int mood_assessment) {
        this.mood_assessment = mood_assessment;
    }

    public String getRecord_date() {
        return record_date;
    }

    public void setRecord_date(String record_date) {
        this.record_date = record_date;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
