package com.example.vgtu_map;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DownloadAndParseScheduleTask.OnScheduleDownloadedAndParsedListener {

    private EditText groupEditText; // Поле для ввода названия группы
    private Button searchButton;
    private TextView scheduleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groupEditText = findViewById(R.id.groupEditText);
        searchButton = findViewById(R.id.saveGroupButton); // Используем прежний ID, но меняем смысл
        scheduleTextView = findViewById(R.id.scheduleTextView);

        searchButton.setText("Поиск расписания"); // Изменяем текст кнопки

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredGroupName = groupEditText.getText().toString().trim();
                if (!enteredGroupName.isEmpty()) {
                    // Запускаем DownloadAndParseScheduleTask, передавая название группы
                    new DownloadAndParseScheduleTask(MainActivity.this, enteredGroupName).execute(enteredGroupName);
                    Toast.makeText(MainActivity.this, "Поиск расписания для группы " + enteredGroupName + "...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Пожалуйста, введите название группы", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onScheduleDownloadedAndParsed(List<String> data) {
        StringBuilder sb = new StringBuilder("Расписание:\n");
        for (String item : data) {
            sb.append(item).append("\n");
        }
        scheduleTextView.setText(sb.toString());
    }

    @Override
    public void onError(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Ошибка: " + message);
            }
        });
    }
}