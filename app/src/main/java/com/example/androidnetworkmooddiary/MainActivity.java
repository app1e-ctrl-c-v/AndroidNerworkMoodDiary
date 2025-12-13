package com.example.androidnetworkmooddiary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnetworkmooddiary.adapters.MoodDiaryAdapter;
import com.example.androidnetworkmooddiary.models.MoodDiary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private FloatingActionButton addButton;
    @SuppressLint("MissingInflatedId")
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
        addButton = findViewById(R.id.addButton);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoodDiaryAdapter();
        recyclerView.setAdapter(adapter);
        loadDiary();
        adapter.setOnItemLongClickListener((position, itemId) -> {
            MoodDiary entry = adapter.getMoodDiaries().get(position);
            showEditDialog(entry.getId(), entry.getComment_day(),
                    entry.getMood_assessment(), entry.getRecord_date(), position);
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddDialog();
                loadDiary();
            }
        });
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
    private void showAddDialog() {
        final EditText etComment = new EditText(this);
        etComment.setHint("Комментарий дня");
        final EditText etGrade = new EditText(this);
        etGrade.setHint("Оценка настроения (число от 0 до 10)");
        etGrade.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText etDueDate = new EditText(this);
        etDueDate.setHint("Дата (MM.DD.YYYY)");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(etComment);
        container.addView(etGrade);
        container.addView(etDueDate);
        new AlertDialog.Builder(this)
                .setTitle("Новая запись")
                .setView(container)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String comment = etComment.getText().toString().trim();
                    String gradeStr = etGrade.getText().toString().trim();
                    String dueDate = etDueDate.getText().toString().trim();
                    if (comment.isEmpty() || gradeStr.isEmpty()) {
                        Toast.makeText(this, "Заполните комментарий и оценку", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int grade = Integer.parseInt(gradeStr);
                    addEntry(comment, grade, dueDate);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addEntry(String comment, int grade, String dueDate) {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Prefer", "return=representation");
                connection.setDoOutput(true);
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user_id", idUser);
                jsonBody.put("record_date", dueDate);
                jsonBody.put("comment_day", comment);
                jsonBody.put("mood_assessment", grade);
                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();
                int code = connection.getResponseCode();
                String response = readStream(connection);
                Log.d(TAG, "POST response code: " + code);
                Log.d(TAG, "POST response: " + response);
                if (code == 201) {
                    JSONArray createdArray = new JSONArray(response);
                    if (createdArray.length() > 0) {
                        JSONObject createdObj = createdArray.getJSONObject(0);
                        MoodDiary newEntry = new MoodDiary();
                        newEntry.setId(createdObj.getInt("id"));
                        newEntry.setComment_day(createdObj.getString("comment_day"));
                        newEntry.setMood_assessment(createdObj.getInt("mood_assessment"));
                        newEntry.setRecord_date(createdObj.getString("record_date"));
                        newEntry.setUser_id(createdObj.getString("user_id"));
                        mainHandler.post(() -> {
                            adapter.getMoodDiaries().add(0, newEntry);
                            loadDiary();
                            Toast.makeText(MainActivity.this, "Запись добавлена", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(MainActivity.this, "Ошибка добавления: " + code, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении записи", e);
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }


    public void deleteTask(int id, int position) {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.ID_ROW_URL + id);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                int code = connection.getResponseCode();
                String response = readStream(connection);
                if (code == 200 || code == 204) { mainHandler.post(() -> {
                            adapter.getMoodDiaries().remove(position);
                            loadDiary();
                            Toast.makeText(this, "запись удалена", Toast.LENGTH_SHORT).show();
                        }
                    );
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка удаления: " + code, Toast.LENGTH_SHORT).show()
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

    public void showEditDialog(int id, String commentDay, int moodAssessment, String recordDate, int position) {
        final EditText etComment = new EditText(this);
        etComment.setText(commentDay);
        final EditText etGrade = new EditText(this);
        etGrade.setText(moodAssessment+"");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(etComment);
        container.addView(etGrade);
        new AlertDialog.Builder(this)
                .setTitle("Редактировать запись")
                .setView(container)
                .setPositiveButton("Сохранить", (dialog, which) ->{
                    String comment = etComment.getText().toString().trim();
                    String gradeStr = etGrade.getText().toString().trim();
                    if (comment.isEmpty() || gradeStr.isEmpty()) {
                        Toast.makeText(this, "Заполните комментарий и оценку", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int grade = Integer.parseInt(gradeStr);
                    editEntry(id, comment, grade, position);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void editEntry(int id, String comment, int grade, int position) {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.ID_ROW_URL+id);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PATCH");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Prefer", "return=representation");
                connection.setDoOutput(true);
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("comment_day", comment);
                jsonBody.put("mood_assessment", grade);
                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();
                int code = connection.getResponseCode();
                String response = readStream(connection);
                Log.d(TAG, "PATCH response code: " + code);
                Log.d(TAG, "PATCH response: " + response);
                if (code == 200 || code == 204) {
                    mainHandler.post(() -> {
                        List<MoodDiary> entries = adapter.getMoodDiaries();
                        if (position < entries.size()) {
                            MoodDiary updatedEntry = entries.get(position);
                            updatedEntry.setComment_day(comment);
                            updatedEntry.setMood_assessment(grade);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(MainActivity.this, "Запись обновлена", Toast.LENGTH_SHORT).show();
                            loadDiary();
                        }
                    });
                } else {
                    mainHandler.post(() ->
                            Toast.makeText(MainActivity.this, "Ошибка изменения: " + code, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при изменении записи", e);
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}