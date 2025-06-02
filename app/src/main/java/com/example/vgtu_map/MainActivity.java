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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements DownloadAndParseScheduleTask.ScheduleDownloadListener {

    private AutoCompleteTextView facultySpinner;
    private EditText groupEditText;
    private Button searchButton;
    private Button todayButton;
    private Button tomorrowButton;
    private TextView scheduleTextView;
    private File downloadedFile;
    private String currentGroupName = "";
    private Button afterTomorrowButton;
    private TextView dateHeader;
    private Button teacherButton;
    private Button openMapButton;
    private String selectedFacultyUrl;

    private Button weekButton;
    private Button nextWeekButton;
    private CardView weeklyScheduleCard;
    private TextView scheduleDayHeader;
    private TextView scheduleWeeklyTextView;
    private Button prevDayButton;
    private Button nextDayButton;
    private LinearLayout weeklyNavigationButtons;

    private Calendar currentWeeklyScheduleCalendar;

    // Эти календари определяют границы просматриваемой НЕДЕЛИ,
    // а не просто дня, к которому относится текущий день.
    // Они обновляются только при нажатии "Эта неделя" или "След. неделя".
    private Calendar startOfWeekCalendar;
    private Calendar endOfWeekCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        facultySpinner = findViewById(R.id.facultySpinner);
        groupEditText = findViewById(R.id.groupEditText);
        searchButton = findViewById(R.id.saveGroupButton);
        todayButton = findViewById(R.id.todayButton);
        tomorrowButton = findViewById(R.id.tomorrowButton);
        scheduleTextView = findViewById(R.id.scheduleTextView);
        afterTomorrowButton = findViewById(R.id.afterTomorrowButton);
        dateHeader = findViewById(R.id.dateHeader);
        teacherButton = findViewById(R.id.teachersButton);
        openMapButton = findViewById(R.id.openMapButton);

        weekButton = findViewById(R.id.weekButton);
        nextWeekButton = findViewById(R.id.nextWeekButton);
        weeklyScheduleCard = findViewById(R.id.weeklyScheduleCard);
        scheduleDayHeader = findViewById(R.id.scheduleDayHeader);
        scheduleWeeklyTextView = findViewById(R.id.scheduleWeeklyTextView);
        prevDayButton = findViewById(R.id.prevDayButton);
        nextDayButton = findViewById(R.id.nextDayButton);
        weeklyNavigationButtons = findViewById(R.id.weeklyNavigationButtons);


        searchButton.setText("Поиск расписания");
        scheduleTextView.setText("Результат поиска расписания будет здесь");

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
                    currentGroupName = enteredGroupName;
                    DownloadAndParseScheduleTask task = new DownloadAndParseScheduleTask(MainActivity.this, MainActivity.this);
                    task.execute(enteredGroupName, selectedFacultyUrl);
                    Toast.makeText(MainActivity.this, "Поиск расписания для группы " + enteredGroupName + "...", Toast.LENGTH_SHORT).show();
                    scheduleTextView.setText("Идет поиск и скачивание расписания...");
                    dateHeader.setText("Расписание");
                    weeklyScheduleCard.setVisibility(View.GONE);
                    weeklyNavigationButtons.setVisibility(View.GONE);
                    scheduleTextView.setVisibility(View.VISIBLE);
                    dateHeader.setVisibility(View.VISIBLE);
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
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, map.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        todayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    scheduleTextView.setVisibility(View.VISIBLE);
                    dateHeader.setVisibility(View.VISIBLE);
                    weeklyScheduleCard.setVisibility(View.GONE);
                    weeklyNavigationButtons.setVisibility(View.GONE);

                    dateHeader.setText("Расписание на сегодня");
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
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    scheduleTextView.setVisibility(View.VISIBLE);
                    dateHeader.setVisibility(View.VISIBLE);
                    weeklyScheduleCard.setVisibility(View.GONE);
                    weeklyNavigationButtons.setVisibility(View.GONE);

                    dateHeader.setText("Расписание на завтра");
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
                if (downloadedFile != null && !currentGroupName.isEmpty()) {
                    scheduleTextView.setVisibility(View.VISIBLE);
                    dateHeader.setVisibility(View.VISIBLE);
                    weeklyScheduleCard.setVisibility(View.GONE);
                    weeklyNavigationButtons.setVisibility(View.GONE);

                    dateHeader.setText("Расписание на послезавтра");
                    Toast.makeText(MainActivity.this, "Загрузка расписания на послезавтра...", Toast.LENGTH_SHORT).show();
                    loadScheduleForAfterTomorrow(downloadedFile);
                } else {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                }
            }
        });

        weekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile == null || currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                    return;
                }

                weeklyScheduleCard.setVisibility(View.VISIBLE);
                weeklyNavigationButtons.setVisibility(View.VISIBLE);
                scheduleTextView.setVisibility(View.GONE);
                dateHeader.setVisibility(View.GONE);

                // Устанавливаем currentWeeklyScheduleCalendar на понедельник ТЕКУЩЕЙ календарной недели
                currentWeeklyScheduleCalendar = Calendar.getInstance();
                currentWeeklyScheduleCalendar.setFirstDayOfWeek(Calendar.MONDAY);
                currentWeeklyScheduleCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                currentWeeklyScheduleCalendar.set(Calendar.HOUR_OF_DAY, 0);
                currentWeeklyScheduleCalendar.set(Calendar.MINUTE, 0);
                currentWeeklyScheduleCalendar.set(Calendar.SECOND, 0);
                currentWeeklyScheduleCalendar.set(Calendar.MILLISECOND, 0);

                // Устанавливаем границы текущей календарной недели
                startOfWeekCalendar = (Calendar) currentWeeklyScheduleCalendar.clone();
                endOfWeekCalendar = (Calendar) currentWeeklyScheduleCalendar.clone();
                endOfWeekCalendar.add(Calendar.DATE, 6);
                endOfWeekCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endOfWeekCalendar.set(Calendar.MINUTE, 59);
                endOfWeekCalendar.set(Calendar.SECOND, 59);
                endOfWeekCalendar.set(Calendar.MILLISECOND, 999);

                loadScheduleForCurrentWeeklyDay(); // Загружаем расписание для понедельника текущей недели
            }
        });

        nextWeekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedFile == null || currentGroupName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Сначала выполните поиск расписания", Toast.LENGTH_LONG).show();
                    return;
                }

                weeklyScheduleCard.setVisibility(View.VISIBLE);
                weeklyNavigationButtons.setVisibility(View.VISIBLE);
                scheduleTextView.setVisibility(View.GONE);
                dateHeader.setVisibility(View.GONE);

                // Устанавливаем currentWeeklyScheduleCalendar на понедельник СЛЕДУЮЩЕЙ календарной недели
                currentWeeklyScheduleCalendar = Calendar.getInstance();
                currentWeeklyScheduleCalendar.add(Calendar.WEEK_OF_YEAR, 1); // Переходим на следующую календарную неделю
                currentWeeklyScheduleCalendar.setFirstDayOfWeek(Calendar.MONDAY);
                currentWeeklyScheduleCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                currentWeeklyScheduleCalendar.set(Calendar.HOUR_OF_DAY, 0);
                currentWeeklyScheduleCalendar.set(Calendar.MINUTE, 0);
                currentWeeklyScheduleCalendar.set(Calendar.SECOND, 0);
                currentWeeklyScheduleCalendar.set(Calendar.MILLISECOND, 0);

                // Устанавливаем границы СЛЕДУЮЩЕЙ календарной недели
                startOfWeekCalendar = (Calendar) currentWeeklyScheduleCalendar.clone();
                endOfWeekCalendar = (Calendar) currentWeeklyScheduleCalendar.clone();
                endOfWeekCalendar.add(Calendar.DATE, 6);
                endOfWeekCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endOfWeekCalendar.set(Calendar.MINUTE, 59);
                endOfWeekCalendar.set(Calendar.SECOND, 59);
                endOfWeekCalendar.set(Calendar.MILLISECOND, 999);

                loadScheduleForCurrentWeeklyDay(); // Загружаем расписание для понедельника следующей календарной недели
            }
        });

        prevDayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (weeklyScheduleCard.getVisibility() == View.VISIBLE && prevDayButton.isEnabled()) {
                    currentWeeklyScheduleCalendar.add(Calendar.DATE, -1);
                    loadScheduleForCurrentWeeklyDay();
                }
            }
        });

        nextDayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (weeklyScheduleCard.getVisibility() == View.VISIBLE && nextDayButton.isEnabled()) {
                    currentWeeklyScheduleCalendar.add(Calendar.DATE, 1);
                    loadScheduleForCurrentWeeklyDay();
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
                Log.d("MainActivity", "Файл расписания скачан: " + file.getAbsolutePath());
                downloadedFile = file;

                dateHeader.setText("Расписание на сегодня");
                scheduleTextView.setVisibility(View.VISIBLE);
                dateHeader.setVisibility(View.VISIBLE);
                weeklyScheduleCard.setVisibility(View.GONE);
                weeklyNavigationButtons.setVisibility(View.GONE);

                loadScheduleForToday(downloadedFile);
            }
        });
    }

    // Модифицированный метод loadScheduleForSpecificDay
    // Теперь он не принимает weekOffsetForParser, а сам определяет, какой календарь передать в парсер.
    // Парсер уже использует переданный календарь для isNumeratorWeek.
    private void loadScheduleForSpecificDay(File file, Calendar calendar, TextView targetTextView, TextView targetDayHeader) {
        if (file == null) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Ошибка: Файл расписания не скачан.", Toast.LENGTH_LONG).show();
                targetTextView.setText("Ошибка: Файл расписания не скачан.");
                targetDayHeader.setText("Ошибка!");
                weeklyNavigationButtons.setVisibility(View.GONE);
                prevDayButton.setEnabled(false);
                nextDayButton.setEnabled(false);
                prevDayButton.setAlpha(0.5f);
                nextDayButton.setAlpha(0.5f);
            });
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru", "RU"));
        String dayName = dateFormat.format(calendar.getTime());
        targetDayHeader.setText("Расписание на " + dayName);
        targetTextView.setText("Загрузка расписания...");

        // ЛОГИКА ДЛЯ АКТИВАЦИИ/ДЕАКТИВАЦИИ КНОПОК ПЕРЕЛИСТЫВАНИЯ
        if (weeklyScheduleCard.getVisibility() == View.VISIBLE) {
            Calendar tempCurrent = (Calendar) calendar.clone();
            tempCurrent.set(Calendar.HOUR_OF_DAY, 0);
            tempCurrent.set(Calendar.MINUTE, 0);
            tempCurrent.set(Calendar.SECOND, 0);
            tempCurrent.set(Calendar.MILLISECOND, 0);

            Calendar tempStart = (Calendar) startOfWeekCalendar.clone();
            tempStart.set(Calendar.HOUR_OF_DAY, 0);
            tempStart.set(Calendar.MINUTE, 0);
            tempStart.set(Calendar.SECOND, 0);
            tempStart.set(Calendar.MILLISECOND, 0);

            Calendar tempEnd = (Calendar) endOfWeekCalendar.clone();
            tempEnd.set(Calendar.HOUR_OF_DAY, 0);
            tempEnd.set(Calendar.MINUTE, 0);
            tempEnd.set(Calendar.SECOND, 0);
            tempEnd.set(Calendar.MILLISECOND, 0);

            if (tempCurrent.equals(tempStart) || tempCurrent.before(tempStart)) {
                prevDayButton.setEnabled(false);
                prevDayButton.setAlpha(0.5f);
            } else {
                prevDayButton.setEnabled(true);
                prevDayButton.setAlpha(1.0f);
            }

            if (tempCurrent.equals(tempEnd) || tempCurrent.after(tempEnd)) {
                nextDayButton.setEnabled(false);
                nextDayButton.setAlpha(0.5f);
            } else {
                nextDayButton.setEnabled(true);
                nextDayButton.setAlpha(1.0f);
            }
        } else {
            weeklyNavigationButtons.setVisibility(View.GONE);
            prevDayButton.setEnabled(false);
            nextDayButton.setEnabled(false);
            prevDayButton.setAlpha(0.5f);
            nextDayButton.setAlpha(0.5f);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar targetDay = (Calendar) calendar.clone();
                targetDay.set(Calendar.HOUR_OF_DAY, 0);
                targetDay.set(Calendar.MINUTE, 0);
                targetDay.set(Calendar.SECOND, 0);
                targetDay.set(Calendar.MILLISECOND, 0);

                long diffMillis = targetDay.getTimeInMillis() - today.getTimeInMillis();
                int dayOffset = (int) (diffMillis / (1000 * 60 * 60 * 24));

                // ЗДЕСЬ ПЕРЕДАЕМ targetDay (или calendar) В ПАРСЕР
                // ExcelParser.parseScheduleForDay теперь принимает календарь, который будет использоваться для isNumeratorWeek
                String scheduleContent = ExcelParser.parseScheduleForDay(file, dayOffset, calendar);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        targetTextView.setText(scheduleContent);
                    }
                });
            }
        }).start();
    }

    private void loadScheduleForCurrentWeeklyDay() {
        // currentWeeklyScheduleCalendar уже содержит нужную дату (и неделю)
        loadScheduleForSpecificDay(downloadedFile, currentWeeklyScheduleCalendar, scheduleWeeklyTextView, scheduleDayHeader);
    }

    private void loadScheduleForToday(File file) {
        weeklyNavigationButtons.setVisibility(View.GONE);
        prevDayButton.setEnabled(false);
        nextDayButton.setEnabled(false);
        prevDayButton.setAlpha(0.5f);
        nextDayButton.setAlpha(0.5f);
        loadScheduleForSpecificDay(file, Calendar.getInstance(), scheduleTextView, dateHeader);
    }

    private void loadScheduleForTomorrow(File file) {
        weeklyNavigationButtons.setVisibility(View.GONE);
        prevDayButton.setEnabled(false);
        nextDayButton.setEnabled(false);
        prevDayButton.setAlpha(0.5f);
        nextDayButton.setAlpha(0.5f);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        loadScheduleForSpecificDay(file, tomorrow, scheduleTextView, dateHeader);
    }

    private void loadScheduleForAfterTomorrow(File file) {
        weeklyNavigationButtons.setVisibility(View.GONE);
        prevDayButton.setEnabled(false);
        nextDayButton.setEnabled(false);
        prevDayButton.setAlpha(0.5f);
        nextDayButton.setAlpha(0.5f);
        Calendar afterTomorrow = Calendar.getInstance();
        afterTomorrow.add(Calendar.DATE, 2);
        loadScheduleForSpecificDay(file, afterTomorrow, scheduleTextView, dateHeader);
    }

    @Override
    public void onError(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                scheduleTextView.setText("Ошибка: " + message);
                Log.e("MainActivity", "Ошибка: " + message);
                scheduleWeeklyTextView.setText("Ошибка загрузки расписания.");
                weeklyNavigationButtons.setVisibility(View.GONE);
                prevDayButton.setEnabled(false);
                nextDayButton.setEnabled(false);
                prevDayButton.setAlpha(0.5f);
                nextDayButton.setAlpha(0.5f);
            }
        });
    }
}