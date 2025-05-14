package com.example.vgtu_map;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class map extends AppCompatActivity {

    private WebView mapWebView;
    private TextView infoTextView;
    private DatabaseReference mDatabase;
    private Spinner corpusSpinner;
    private Spinner floorSpinner;
    private String selectedCorpus = "1";
    private String selectedFloor = "4";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapWebView = findViewById(R.id.mapWebView);
        infoTextView = findViewById(R.id.infoTextView);
        corpusSpinner = findViewById(R.id.corpusSpinner);
        floorSpinner = findViewById(R.id.floorSpinner);

        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        mapWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Инициализация Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference("auditoriums"); // Узел 'auditoriums'

        // Настройка выпадающего списка корпусов
        ArrayAdapter<String> corpusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"1"});
        corpusSpinner.setAdapter(corpusAdapter);
        corpusSpinner.setSelection(0); // По умолчанию выбираем 1 корпус
        corpusSpinner.setEnabled(false); // Запрещаем выбор корпуса

        // Настройка выпадающего списка этажей
        ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"4"});
        floorSpinner.setAdapter(floorAdapter);
        floorSpinner.setSelection(0); // По умолчанию выбираем 4 этаж
        floorSpinner.setEnabled(false); // Запрещаем выбор этажа

        loadSvgInWebView("your_map.svg");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadSvgInWebView(String assetName) {
        String html = generateHtmlWithSvg(assetName);
        mapWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
    }

    private String generateHtmlWithSvg(String assetName) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html>");
        htmlBuilder.append("<head>");
        htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">");
        htmlBuilder.append("<style>");
        htmlBuilder.append("svg { width: 100%; height: auto; }");
        htmlBuilder.append(".auditorium { cursor: pointer; fill: #f0f0f0; stroke: #333; stroke-width: 1px; }");
        htmlBuilder.append(".auditorium:hover { fill: lightblue; }");
        htmlBuilder.append("</style>");
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");
        htmlBuilder.append(loadSvgFromAsset(assetName));
        htmlBuilder.append("<script>");
        htmlBuilder.append("document.addEventListener('DOMContentLoaded', function() {");
        htmlBuilder.append("  var auditoriums = document.querySelectorAll('.auditorium');");
        htmlBuilder.append("  auditoriums.forEach(function(auditorium) {");
        htmlBuilder.append("    auditorium.addEventListener('click', function() {");
        htmlBuilder.append("      var auditoriumId = this.id;");
        htmlBuilder.append("      console.log('JavaScript: Клик на аудитории ' + auditoriumId);");
        htmlBuilder.append("      handleAuditoriumClick(auditoriumId);");
        htmlBuilder.append("    });");
        htmlBuilder.append("  });");
        htmlBuilder.append("  function handleAuditoriumClick(auditoriumId) {");
        htmlBuilder.append("    console.log('JavaScript: Вызвана handleAuditoriumClick с ID: ' + auditoriumId);");
        htmlBuilder.append("    if (window.Android) {");
        htmlBuilder.append("      console.log('JavaScript: Вызов fetchAuditoriumDetails');");
        htmlBuilder.append("      window.Android.fetchAuditoriumDetails(auditoriumId);");
        htmlBuilder.append("    } else {");
        htmlBuilder.append("      console.log('JavaScript: Объект window.Android НЕ существует!');");
        htmlBuilder.append("    }");
        htmlBuilder.append("  }");
        htmlBuilder.append("});");
        htmlBuilder.append("</script>");
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");
        return htmlBuilder.toString();
    }

    private String loadSvgFromAsset(String assetName) {
        try (InputStream inputStream = getAssets().open(assetName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder svgContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                svgContent.append(line);
            }
            return svgContent.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<svg width=\"100\" height=\"100\"><text x=\"10\" y=\"50\" fill=\"red\">Ошибка загрузки SVG</text></svg>";
        }
    }

    public void fetchAuditoriumDetailsFromDB(String auditoriumId) {
        mDatabase.child(auditoriumId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    java.util.Map<String, Object> auditoriumInfo = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if (auditoriumInfo != null) {
                        String name = (String) auditoriumInfo.get("name");
                        String description = (String) auditoriumInfo.get("description");
                        Long capacity = (Long) auditoriumInfo.get("capacity");

                        StringBuilder detailsBuilder = new StringBuilder();
                        if (name != null) {
                            detailsBuilder.append("Название: ").append(name).append("\n");
                        }
                        if (description != null) {
                            detailsBuilder.append("Тип: ").append(description).append("\n");
                        }
                        if (capacity != null) {
                            detailsBuilder.append("Вместительность: ").append(capacity).append(" человек");
                        }

                        String details = detailsBuilder.toString().trim();
                        updateAuditoriumInfoTextView(details);
                    } else {
                        updateAuditoriumInfoTextView("Информация не найдена");
                    }
                } else {
                    updateAuditoriumInfoTextView("Информация не найдена");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                updateAuditoriumInfoTextView("Ошибка при получении данных: " + databaseError.getMessage());
                Log.e("Firebase", "Ошибка чтения данных: " + databaseError.getMessage());
            }
        });
    }

    private void updateAuditoriumInfoTextView(final String text) {
        runOnUiThread(() -> {
            if (infoTextView != null) {
                infoTextView.setText(text);
            } else {
                Toast.makeText(map.this, "TextView не найден", Toast.LENGTH_SHORT).show();
            }
        });
    }
}