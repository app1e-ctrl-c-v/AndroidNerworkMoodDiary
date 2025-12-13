package com.example.androidnetworkmooddiary;

public class SupabaseConfig {
    public static final String SUPABASE_URL = "https://sjjisresdqkcmjlztxbh.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNqamlzcmVzZHFrY21qbHp0eGJoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU0MDM2MDYsImV4cCI6MjA4MDk3OTYwNn0.dq1PZDCMl9TMWTJFZVkke-ksH4At3anOI6GrqbLxQ9E";
    public static final String AUTH_SIGNUP_URL =
            SUPABASE_URL + "/auth/v1/signup";
    public static final String AUTH_SIGNIN_URL =
            SUPABASE_URL + "/auth/v1/token?grant_type=password";
    public static final String TABLE_URL =
            SUPABASE_URL + "/rest/v1/MoodDiary?user_id=eq.";
    public static final String ID_ROW_URL =
            SUPABASE_URL + "/rest/v1/MoodDiary?id=eq.";

}
