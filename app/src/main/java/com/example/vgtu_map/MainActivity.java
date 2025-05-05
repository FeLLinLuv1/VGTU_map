package com.example.vgtu_map;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DownloadAndParseScheduleTask.ScheduleDownloadListener {

    private EditText groupEditText; // Поле для ввода названия группы
    private Button searchButton;
    private TextView scheduleTextView;
    private File downloadedFile; // Переменная для хранения скачанного файла

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groupEditText = findViewById(R.id.groupEditText);
        searchButton = findViewById(R.id.saveGroupButton); // Используем прежний ID, но меняем смысл
        scheduleTextView = findViewById(R.id.scheduleTextView);

        searchButton.setText("Поиск расписания"); // Изменяем текст кнопки
        scheduleTextView.setText("Результат поиска расписания будет здесь"); // Начальный текст

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredGroupName = groupEditText.getText().toString().trim();
                if (!enteredGroupName.isEmpty()) {
                    // Запускаем DownloadAndParseScheduleTask, передавая контекст и название группы
                    DownloadAndParseScheduleTask task = new DownloadAndParseScheduleTask(MainActivity.this, MainActivity.this);
                    task.execute(enteredGroupName);
                    Toast.makeText(MainActivity.this, "Поиск расписания для группы " + enteredGroupName + "...", Toast.LENGTH_SHORT).show();
                    scheduleTextView.setText("Идет поиск и скачивание расписания..."); // Обновляем текст
                } else {
                    Toast.makeText(MainActivity.this, "Пожалуйста, введите название группы", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onScheduleDownloaded(File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Расписание успешно скачано!", Toast.LENGTH_SHORT).show();
                scheduleTextView.setText("Файл расписания скачан и обрабатывается...");
                Log.d("MainActivity", "Файл расписания скачан: " + file.getAbsolutePath());
                downloadedFile = file; // Сохраняем скачанный файл

                // Запускаем парсинг Excel в отдельном потоке
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadedFile != null) {
                            List<List<String>> scheduleData = ExcelParser.parseSchedule(downloadedFile);
                            // После парсинга передаем данные обратно в UI-поток для отображения
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    displaySchedule(scheduleData);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Ошибка: Не удалось получить скачанный файл.", Toast.LENGTH_LONG).show();
                                    scheduleTextView.setText("Ошибка: Не удалось получить скачанный файл.");
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    private void displaySchedule(List<List<String>> data) {
        StringBuilder sb = new StringBuilder("Расписание:\n");
        if (data != null && !data.isEmpty()) {
            for (List<String> row : data) {
                for (String cell : row) {
                    sb.append(cell).append("\t\t"); // Добавляем табуляцию для разделения ячеек
                }
                sb.append("\n");
            }
        } else {
            sb.append("Данные расписания отсутствуют или не удалось обработать.");
        }
        scheduleTextView.setText(sb.toString());
    }

    @Override
    public void onError(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                scheduleTextView.setText("Ошибка: " + message);
                Log.e("MainActivity", "Ошибка: " + message);
            }
        });
    }
}