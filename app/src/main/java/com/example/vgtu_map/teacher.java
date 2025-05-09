package com.example.vgtu_map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.time.LocalDate;

public class teacher extends AppCompatActivity {

    private TextView dateHeaderTextView;
    private TextView scheduleDisplayTextView;
    private Button todayButton;
    private Button tomorrowButton;
    private Button afterTomorrowButton;
    private Button studentScheduleButton;
    private Button showScheduleForTeacherButton;
    private EditText teacherNameEditText; // Поле для ФИО преподавателя

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher);

        // Инициализация элементов UI
        dateHeaderTextView = findViewById(R.id.dateHeader);
        scheduleDisplayTextView = findViewById(R.id.scheduleTextView);
        todayButton = findViewById(R.id.todayButton);
        tomorrowButton = findViewById(R.id.tomorrowButton);
        afterTomorrowButton = findViewById(R.id.afterTomorrowButton);
        studentScheduleButton = findViewById(R.id.studentButton);
        showScheduleForTeacherButton = findViewById(R.id.showScheduleButton);
        teacherNameEditText = findViewById(R.id.groupEditText); // Используем прежний ID для поля ФИО

        // Обработчик нажатия кнопки "Показать расписание"
        showScheduleForTeacherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredName = teacherNameEditText.getText().toString().trim();
                String teacherFileName = "";
                if (enteredName.equals("Бойматов Ойбекджон Фахрединович") || enteredName.equals("Бойматов ОФ")) {
                    teacherFileName = "boimatov";
                } else {
                    Toast.makeText(teacher.this, "Расписание доступно только для Бойматова О.Ф.", Toast.LENGTH_SHORT).show();
                    return; // Выходим из обработчика, ничего не делаем
                }

                if (!teacherFileName.isEmpty()) {
                    displaySchedule(LocalDate.now(), teacherFileName);
                }
            }
        });

        todayButton.setOnClickListener(v -> {
            String enteredName = teacherNameEditText.getText().toString().trim();
            String teacherFileName = "";
            if (enteredName.equals("Бойматов Ойбекджон Фахрединович") || enteredName.equals("Бойматов ОФ")) {
                teacherFileName = "boimatov";
            } else {
                Toast.makeText(teacher.this, "Расписание доступно только для Бойматова О.Ф.", Toast.LENGTH_SHORT).show();
                return; // Выходим из обработчика
            }
            if (!teacherFileName.isEmpty()) {
                displaySchedule(LocalDate.now(), teacherFileName);
            }
        });
        tomorrowButton.setOnClickListener(v -> {
            String enteredName = teacherNameEditText.getText().toString().trim();
            String teacherFileName = "";
            if (enteredName.equals("Бойматов Ойбекджон Фахрединович") || enteredName.equals("Бойматов ОФ")) {
                teacherFileName = "boimatov";
            } else {
                Toast.makeText(teacher.this, "Расписание доступно только для Бойматова О.Ф.", Toast.LENGTH_SHORT).show();
                return; // Выходим из обработчика
            }
            if (!teacherFileName.isEmpty()) {
                displaySchedule(LocalDate.now().plusDays(1), teacherFileName);
            }
        });
        afterTomorrowButton.setOnClickListener(v -> {
            String enteredName = teacherNameEditText.getText().toString().trim();
            String teacherFileName = "";
            if (enteredName.equals("Бойматов Ойбекджон Фахрединович") || enteredName.equals("Бойматов ОФ")) {
                teacherFileName = "boimatov";
            } else {
                Toast.makeText(teacher.this, "Расписание доступно только для Бойматова О.Ф.", Toast.LENGTH_SHORT).show();
                return; // Выходим из обработчика
            }
            if (!teacherFileName.isEmpty()) {
                displaySchedule(LocalDate.now().plusDays(2), teacherFileName);
            }
        });

        studentScheduleButton.setOnClickListener(v -> {
            Intent intent = new Intent(teacher.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Показать расписание на сегодня при первом запуске (можно убрать или оставить с пустым запросом)
        // displaySchedule(LocalDate.now(), "");
    }

    private void displaySchedule(LocalDate date, String teacherLastName) {
        XWPFDocument document = WordReader.getWordDocument(this, teacherLastName);
        if (document != null) {
            String scheduleText = TeacherScheduleParser.parseScheduleForDay(document, date);
            String dayOfWeek = TeacherScheduleParser.getDayName(date.getDayOfWeek().getValue());
            String month = TeacherScheduleParser.getMonthName(date.getMonthValue());
            dateHeaderTextView.setText("Расписание преподавателя на " + dayOfWeek + ", " + date.getDayOfMonth() + " " + month);
            scheduleDisplayTextView.setText(scheduleText);
        } else {
            scheduleDisplayTextView.setText("Расписание для преподавателя \"" + teacherLastName + "\" не найдено.");
            dateHeaderTextView.setText("");
        }
    }

}