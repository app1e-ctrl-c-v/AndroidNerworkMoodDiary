package com.example.androidnetworkmooddiary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emailEditText.getText().length() != 0 || passwordEditText.getText().length() != 0){
                password = passwordEditText.getText().toString();
                email = emailEditText.getText().toString();
                    singIn();
                } else  Toast.makeText(getApplicationContext(), getResources().getText(R.string.warning_empty), Toast.LENGTH_LONG).show();
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emailEditText.getText().length() != 0 || passwordEditText.getText().length() != 0){
                    password = passwordEditText.getText().toString();
                    email = emailEditText.getText().toString();
                    singUp();
                } else  Toast.makeText(getApplicationContext(), getResources().getText(R.string.warning_empty), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void singUp() {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNUP_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
                int responseCode = connection.getResponseCode();
                String response = readStream(connection);
                mainHandler.post(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK ||
                            responseCode == HttpURLConnection.HTTP_CREATED) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.has("error")) {
                                String errorMessage = jsonResponse.getString("message") != null ?
                                        jsonResponse.getString("message") :
                                        "Ошибка регистрации";
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "User registered: " + email);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, "Ошибка обработки ответа", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        }
                    } else {
                        try {
                            JSONObject errorResponse = new JSONObject(response);
                            String errorMessage = errorResponse.getString("error_description") != null ?
                                    errorResponse.getString("error_description") :
                                    errorResponse.getString("message");
                            Toast.makeText(this, "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(this, "Ошибка регистрации. Код: " + responseCode,
                                    Toast.LENGTH_LONG).show();
                        }
                        Log.e(TAG, "Registration failed. Code: " + responseCode + ", Response: " + response);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Сетевая ошибка: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Network error: " + e.getMessage());
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    private void singIn() {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNIN_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
                int responseCode = connection.getResponseCode();
                String response = readStream(connection);
                mainHandler.post(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            String accessToken = obj.getString("access_token");
                            String userId = obj.getJSONObject("user").getString("id");
                            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("access_token", accessToken)
                                    .putString("user_id", userId)
                                    .apply();
                            Log.d(TAG, "User logged in: " + userId);
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } catch (JSONException e) {
                            Toast.makeText(this, "Ошибка обработки ответа сервера", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        }
                    } else {
                        JSONObject errorResponse = null;
                        try {
                            errorResponse = new JSONObject(response);
                            String errorMessage = "Ошибка входа";
                            if (errorResponse.has("error_description")) {
                                errorMessage = errorResponse.getString("error_description");
                            } else if (errorResponse.has("message")) {
                                errorMessage = errorResponse.getString("message");
                            } else if (errorResponse.has("error")) {
                                errorMessage = errorResponse.getString("error");
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Login failed. Code: " + responseCode + ", Error: " + errorMessage);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                         }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Сетевая ошибка: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Network error during sign in: " + e.getMessage());
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
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
