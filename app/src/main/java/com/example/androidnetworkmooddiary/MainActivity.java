package com.example.androidnetworkmooddiary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnetworkmooddiary.adapters.MoodDiaryAdapter;
import com.example.androidnetworkmooddiary.models.MoodDiary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private MoodDiaryAdapter adapter;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String accessToken, idUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
         accessToken = prefs.getString("access_token", null);
         idUser = prefs.getString("user_id", null);
        if (accessToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoodDiaryAdapter();
        recyclerView.setAdapter(adapter);
        loadDiary();
    }
    private void loadDiary() {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL + idUser);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                int code = connection.getResponseCode();
                String response = readStream(connection);
                if (code == 200) {
                    List<MoodDiary> entries = parseEntries(response);
                    mainHandler.post(() -> adapter.setMoodDiaries(entries));
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка загрузки: " + code, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Сетевая ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
    private List<MoodDiary> parseEntries(String json) {
        List<MoodDiary> entriesList = new ArrayList<>();
        try {
            JSONArray entriesArray = new JSONArray(json);
            Log.d(TAG, "Found " + entriesArray.length() + " entries in JSON");
            for (int i = 0; i < entriesArray.length(); i++) {
                JSONObject productObj = entriesArray.getJSONObject(i);
                MoodDiary entry = new MoodDiary();
                entry.setId(productObj.getInt("id"));
                entry.setComment_day(productObj.getString("comment_day"));
                entry.setMood_assessment(productObj.getInt( "mood_assessment"));
                entry.setRecord_date (productObj.getString("record_date"));
                entry.setUser_id(productObj.getString("user_id"));
                entriesList.add(entry);
                if (i == 0) {
                    Log.d(TAG, "Sample entry: " + entry);
                }
            }
            Log.d(TAG,"Successfully parsed " + entriesList.size() + " entries");
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error: " + e.getMessage());
            return null;
        }
        return entriesList;
    }
    private String readStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream;
        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        if (inputStream == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

}