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
    private Button todayButton; // Кнопка "Сегодня"
    private Button tomorrowButton; // Кнопка "Завтра"
    private TextView scheduleTextView;
    private File downloadedFile; // Переменная для хранения скачанного файла
    private String currentGroupName = ""; // Храним текущее название группы
    private Button afterTomorrowButton; // Кнопка "Послезавтра"
    private TextView dateHeader; // Добавляем TextView для заголовка даты

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groupEditText = findViewById(R.id.groupEditText);
        searchButton = findViewById(R.id.saveGroupButton); // Используем прежний ID, но меняем смысл
        todayButton = findViewById(R.id.todayButton);
        tomorrowButton = findViewById(R.id.tomorrowButton);
        scheduleTextView = findViewById(R.id.scheduleTextView);
        afterTomorrowButton = findViewById(R.id.afterTomorrowButton);
        dateHeader = findViewById(R.id.dateHeader); // Инициализируем TextView заголовка

        searchButton.setText("Поиск расписания"); // Изменяем текст кнопки
        scheduleTextView.setText("Результат поиска расписания будет здесь"); // Начальный текст

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredGroupName = groupEditText.getText().toString().trim();
                if (!enteredGroupName.isEmpty()) {
                    currentGroupName = enteredGroupName; // Сохраняем название группы
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

        todayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на сегодня...", Toast.LENGTH_SHORT).show();
                    loadScheduleForToday(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
                dateHeader.setText("Расписание на сегодня"); // Обновляем заголовок
            }
        });

        tomorrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на завтра...", Toast.LENGTH_SHORT).show();
                    loadScheduleForTomorrow(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
                dateHeader.setText("Расписание на завтра"); // Обновляем заголовок
            }
        });
        afterTomorrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на послезавтра...", Toast.LENGTH_SHORT).show();
                    loadScheduleForAfterTomorrow(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
                dateHeader.setText("Расписание на послезавтра"); // Обновляем заголовок
            }
        });
    }

    @Override
    public void onScheduleDownloaded(File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Расписание успешно скачано!", Toast.LENGTH_SHORT).show();
                scheduleTextView.setText("Файл расписания скачан.");
                Log.d("MainActivity", "Файл расписания скачан: " + file.getAbsolutePath());
                downloadedFile = file; // Сохраняем скачанный файл
                // После скачивания сразу отображаем расписание на сегодня
                loadScheduleForToday(downloadedFile);
            }
        });
    }

    private void loadScheduleForToday(File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (file != null) {
                    String todaysSchedule = ExcelParser.parseScheduleForToday(file);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scheduleTextView.setText(todaysSchedule);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Ошибка: Файл расписания не найден.", Toast.LENGTH_LONG).show();
                            scheduleTextView.setText("Ошибка: Файл расписания не найден.");
                        }
                    });
                }
            }
        }).start();
    }

    private void loadScheduleForTomorrow(File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (file != null) {
                    String tomorrowSchedule = ExcelParser.parseScheduleForTomorrow(file);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scheduleTextView.setText(tomorrowSchedule);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Ошибка: Файл расписания не найден.", Toast.LENGTH_LONG).show();
                            scheduleTextView.setText("Ошибка: Файл расписания не найден.");
                        }
                    });
                }
            }
        }).start();
    }
    private void loadScheduleForAfterTomorrow(File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (file != null) {
                    String aftertomorrowSchedule = ExcelParser.parseScheduleForAfterTomorrow(file);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scheduleTextView.setText(aftertomorrowSchedule);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Ошибка: Файл расписания не найден.", Toast.LENGTH_LONG).show();
                            scheduleTextView.setText("Ошибка: Файл расписания не найден.");
                        }
                    });
                }
            }
        }).start();
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