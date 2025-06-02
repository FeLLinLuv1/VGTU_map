package com.example.vgtu_map;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity implements DownloadAndParseScheduleTask.ScheduleDownloadListener {

    private AutoCompleteTextView facultySpinner;
    private EditText groupEditText; // Поле для ввода названия группы
    private Button searchButton;
    private Button todayButton; // Кнопка "Сегодня"
    private Button tomorrowButton; // Кнопка "Завтра"
    private TextView scheduleTextView;
    private File downloadedFile; // Переменная для хранения скачанного файла
    private String currentGroupName = ""; // Храним текущее название группы
    private Button afterTomorrowButton; // Кнопка "Послезавтра"
    private TextView dateHeader; // Заголовок для отображения выбранной даты
    private Button teacherButton;
    private Button openMapButton;
    private String selectedFacultyUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        facultySpinner = findViewById(R.id.facultySpinner);
        groupEditText = findViewById(R.id.groupEditText);
        searchButton = findViewById(R.id.saveGroupButton); // Используем прежний ID, но меняем смысл
        todayButton = findViewById(R.id.todayButton);
        tomorrowButton = findViewById(R.id.tomorrowButton);
        scheduleTextView = findViewById(R.id.scheduleTextView);
        afterTomorrowButton = findViewById(R.id.afterTomorrowButton);
        dateHeader = findViewById(R.id.dateHeader); // Инициализация заголовка
        teacherButton = findViewById(R.id.teachersButton);
        openMapButton = findViewById(R.id.openMapButton);


        searchButton.setText("Поиск расписания"); // Изменяем текст кнопки
        scheduleTextView.setText("Результат поиска расписания будет здесь"); // Начальный текст

        // Настройка Spinner для выбора факультета
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.faculty_array,
                android.R.layout.simple_dropdown_item_1line
        );
        facultySpinner.setAdapter(adapter);

        facultySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedFaculty = (String) parent.getItemAtPosition(position);
                if (selectedFaculty.equals("Факультет СПО")) {
                    selectedFacultyUrl = "https://cchgeu.ru/studentu/schedule/spo/";
                } else if (selectedFaculty.equals("Факультет ДТФ")) {
                    selectedFacultyUrl = "https://cchgeu.ru/studentu/schedule/dtf/";
                } else {
                    selectedFacultyUrl = null;
                }
                Log.d("MainActivity", "Выбран факультет: " + selectedFaculty + ", URL: " + selectedFacultyUrl);
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredGroupName = groupEditText.getText().toString().trim().toUpperCase();
                if (!enteredGroupName.isEmpty() && selectedFacultyUrl != null) {
                    currentGroupName = enteredGroupName; // Сохраняем название группы
                    // Запускаем DownloadAndParseScheduleTask, передавая контекст, название группы и URL
                    DownloadAndParseScheduleTask task = new DownloadAndParseScheduleTask(MainActivity.this, MainActivity.this);
                    task.execute(enteredGroupName, selectedFacultyUrl);
                    Toast.makeText(MainActivity.this, "Поиск расписания для группы " + enteredGroupName + "...", Toast.LENGTH_SHORT).show();
                    scheduleTextView.setText("Идет поиск и скачивание расписания..."); // Обновляем текст
                } else if (enteredGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Пожалуйста, введите название группы", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Пожалуйста, выберите факультет", Toast.LENGTH_LONG).show();
                }
            }
        });

        teacherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, teacher.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Опциональная анимация
            }
        });

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, map.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Опциональная анимация
            }
        });

        todayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateHeader.setText("Расписание на сегодня"); // Обновляем заголовок
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на сегодня...", Toast.LENGTH_SHORT).show();
                    loadScheduleForToday(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
            }
        });

        tomorrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateHeader.setText("Расписание на завтра"); // Обновляем заголовок
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на завтра...", Toast.LENGTH_SHORT).show();
                    loadScheduleForTomorrow(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
            }
        });
        afterTomorrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateHeader.setText("Расписание на послезавтра"); // Обновляем заголовок
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Загрузка расписания на послезавтра...", Toast.LENGTH_SHORT).show();
                    loadScheduleForAfterTomorrow(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
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
                scheduleTextView.setText("Файл расписания скачан.");
                Log.d("MainActivity", "Файл расписания скачан: " + file.getAbsolutePath());
                downloadedFile = file; // Сохраняем скачанный файл
                dateHeader.setText("Расписание на сегодня"); // Устанавливаем начальный заголовок
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
                    String aftertomorrowSchedule = ExcelParser.parseScheduleForDay(file, 2);
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