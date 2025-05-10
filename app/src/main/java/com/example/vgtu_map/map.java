package com.example.vgtu_map;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.caverock.androidsvg.*; // Импортируем все классы из пакета
import com.caverock.androidsvg.SVG; // Явный импорт SVG
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;

public class map extends AppCompatActivity {

    private ImageView mapImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapImageView = findViewById(R.id.mapImageView);

        // ... (остальной код) ...
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД
    public void onAuditoriumClick(View view) {
        String auditoriumId = "";
        int viewId = view.getId();

        // Получаем ID ресурса и преобразуем его в строковое имя
        String resourceName = getResources().getResourceEntryName(viewId);

        // Извлекаем ID аудитории (например, "aud_2304") из ID кнопки ("aud_2304_button")
        if (resourceName.endsWith("_button")) {
            auditoriumId = resourceName.replace("_button", "");
        } else {
            auditoriumId = resourceName; // Если ID кнопки уже содержит ID аудитории
        }

        String info = getAuditoriumInfo(auditoriumId); // Метод для получения информации об аудитории

        if (info != null) {
            Toast.makeText(this, info, Toast.LENGTH_LONG).show();
            // Или отобразите информацию в более сложном UI (диалоговое окно, TextView и т.д.)
        } else {
            Toast.makeText(this, "Информация об аудитории не найдена для: " + auditoriumId, Toast.LENGTH_SHORT).show();
        }
    }

    private String getAuditoriumInfo(String auditoriumId) {
        // В зависимости от вашего способа хранения информации (Map, JSON, база данных)
        // найдите и верните информацию об аудитории по ее ID.
        switch (auditoriumId) {
            case "aud_2304":
                return "Аудитория 2304: Кафедра Информационных Технологий";
            case "aud_1101":
                return "Аудитория 1101: Лекционный зал №1";
            // ... другие аудитории ...
            default:
                return null;
        }
    }
}