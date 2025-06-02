package com.example.vgtu_map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // Важно: этот импорт нужен, если вы используете LinearLayout в dialog_control_menu.xml
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class map extends AppCompatActivity {

    private WebView mapWebView;
    private TextView infoTextView;
    private DatabaseReference mDatabase;
    private FloatingActionButton fabMenu;

    // Переменные для элементов управления в диалоге
    private Spinner dialogCorpusSpinner;
    private Spinner dialogFloorSpinner;
    private EditText dialogSearchAuditoriumEditText;
    private Button dialogSearchButton;

    // Эти переменные пока не используются для смены карт, но сохранены
    // для потенциального будущего функционала (например, для загрузки разных SVG)
    private String selectedCorpus = "1";
    private String selectedFloor = "4";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapWebView = findViewById(R.id.mapWebView);
        infoTextView = findViewById(R.id.infoTextView);
        fabMenu = findViewById(R.id.fab_menu);

        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Устанавливаем WebViewClient для обработки навигации внутри WebView
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebView", "Страница WebView полностью загружена.");
            }
        });

        // Устанавливаем WebChromeClient для обработки консольных сообщений JavaScript (для отладки)
        mapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message() + " -- Строка: "
                        + consoleMessage.lineNumber() + " из: "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        // Добавляем JavaScript-интерфейс для взаимодействия между WebView и Java
        mapWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Инициализация Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference("auditoriums"); // Получаем ссылку на узел 'auditoriums'

        // Загрузка SVG файла карты в WebView
        loadSvgInWebView("your_map.svg");

        // Устанавливаем обработчик нажатия на FloatingActionButton для открытия диалога
        fabMenu.setOnClickListener(v -> showControlMenuDialog());
    }

    // Метод для загрузки SVG-карты в WebView
    @SuppressLint("SetJavaScriptEnabled")
    private void loadSvgInWebView(String assetName) {
        String html = generateHtmlWithSvg(assetName);
        mapWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
    }

    // Метод для генерации HTML-страницы, содержащей SVG и JavaScript
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
        htmlBuilder.append(".highlighted { stroke: red; stroke-width: 3px; }");
        htmlBuilder.append("</style>");
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");
        htmlBuilder.append(loadSvgFromAsset(assetName)); // Вставляем содержимое SVG
        htmlBuilder.append("<script>");

        htmlBuilder.append("var lastHighlighted = null;"); // Переменная для отслеживания последнего выделенного элемента

        // Обработчик события DOMContentLoaded, чтобы убедиться, что SVG загружен
        htmlBuilder.append("document.addEventListener('DOMContentLoaded', function() {");
        htmlBuilder.append("  var auditoriums = document.querySelectorAll('.auditorium');");
        htmlBuilder.append("  auditoriums.forEach(function(auditorium) {");
        htmlBuilder.append("    auditorium.addEventListener('click', function() {");
        htmlBuilder.append("      var auditoriumId = this.id;");
        htmlBuilder.append("      console.log('JavaScript: Клик на аудитории ' + auditoriumId);");
        htmlBuilder.append("      highlightAuditorium(auditoriumId);");
        htmlBuilder.append("      if (window.Android) {");
        htmlBuilder.append("        window.Android.fetchAuditoriumDetails(auditoriumId);"); // Передаем чистый ID
        htmlBuilder.append("      }");
        htmlBuilder.append("    });");
        htmlBuilder.append("  });");
        htmlBuilder.append("});");

        // Функция для выделения аудитории на карте
        htmlBuilder.append("function highlightAuditorium(id) {");
        htmlBuilder.append("  if (lastHighlighted) {");
        htmlBuilder.append("    lastHighlighted.classList.remove('highlighted');"); // Снимаем выделение с предыдущего
        htmlBuilder.append("  }");
        htmlBuilder.append("  var currentElement = document.getElementById(id);");
        htmlBuilder.append("  if (currentElement) {");
        htmlBuilder.append("    currentElement.classList.add('highlighted');"); // Добавляем выделение текущему
        htmlBuilder.append("    lastHighlighted = currentElement;"); // Сохраняем как последний выделенный
        htmlBuilder.append("  }");
        htmlBuilder.append("}");

        // Функция поиска, вызываемая из Android для поиска и выделения аудитории
        htmlBuilder.append("function searchAndHighlightAuditorium(auditoriumNumber) {");
        htmlBuilder.append("  console.log('JavaScript: Поиск аудитории: ' + auditoriumNumber);");
        htmlBuilder.append("  var element = null;");
        htmlBuilder.append("  var targetAuditoriumId = null;"); // Переменная для хранения ID, который будет передан в Android (с суффиксом)

        // Попытка найти элемент с суффиксом 'ad' (например, 1401ad)
        htmlBuilder.append("  element = document.getElementById(auditoriumNumber + 'ad');");
        htmlBuilder.append("  if (element) {");
        htmlBuilder.append("    targetAuditoriumId = auditoriumNumber + 'ad';");
        htmlBuilder.append("  }");

        // Если не нашли, пробуем с суффиксом 'qw' (если такие есть)
        htmlBuilder.append("  if (!element) {");
        htmlBuilder.append("    element = document.getElementById(auditoriumNumber + 'qw');");
        htmlBuilder.append("    if (element) {");
        htmlBuilder.append("      targetAuditoriumId = auditoriumNumber + 'qw';");
        htmlBuilder.append("    }");
        htmlBuilder.append("  }");

        // Если все еще не нашли, пробуем без суффикса (для аудиторий типа 'WC', 'podsobka' и т.д.)
        htmlBuilder.append("  if (!element) {");
        htmlBuilder.append("    element = document.getElementById(auditoriumNumber);");
        htmlBuilder.append("    if (element) {");
        htmlBuilder.append("      targetAuditoriumId = auditoriumNumber;");
        htmlBuilder.append("    }");
        htmlBuilder.append("  }");

        htmlBuilder.append("  if (element && targetAuditoriumId) {"); // Убедимся, что элемент найден и определен ID для передачи
        htmlBuilder.append("    console.log('JavaScript: Аудитория найдена: ' + element.id + '. Передаем в Android ID: ' + targetAuditoriumId + ' (для Firebase)');");
        htmlBuilder.append("    highlightAuditorium(element.id);"); // Выделяем найденную аудиторию на карте
        htmlBuilder.append("    if (window.Android) {");
        // Передаем в Android ТОТ ЖЕ ID (targetAuditoriumId), который есть в Firebase (например, "1401ad")
        htmlBuilder.append("      window.Android.fetchAuditoriumDetails(targetAuditoriumId);");
        htmlBuilder.append("    }");
        htmlBuilder.append("  } else {");
        htmlBuilder.append("    console.log('JavaScript: Аудитория не найдена в SVG: ' + auditoriumNumber);");
        htmlBuilder.append("    if (window.Android) {");
        htmlBuilder.append("      window.Android.onAuditoriumNotFound(auditoriumNumber);");
        htmlBuilder.append("    }");
        htmlBuilder.append("  }");
        htmlBuilder.append("}");

        htmlBuilder.append("</script>");
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");
        return htmlBuilder.toString();
    }

    // Метод для чтения SVG-файла из папки assets
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

    // НОВОЕ: Метод для показа диалога с элементами управления (восстановлен)
    private void showControlMenuDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_control_menu, null);
        builder.setView(dialogView);

        // Инициализация элементов управления в диалоге
        dialogCorpusSpinner = dialogView.findViewById(R.id.dialogCorpusSpinner);
        dialogFloorSpinner = dialogView.findViewById(R.id.dialogFloorSpinner);
        dialogSearchAuditoriumEditText = dialogView.findViewById(R.id.dialogSearchAuditoriumEditText);
        dialogSearchButton = dialogView.findViewById(R.id.dialogSearchButton);

        // Настройка спиннеров в диалоге (пока оставляем фиксированные значения)
        ArrayAdapter<String> corpusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"1"});
        dialogCorpusSpinner.setAdapter(corpusAdapter);
        dialogCorpusSpinner.setSelection(0);
        dialogCorpusSpinner.setEnabled(false); // Корпус пока неактивен, если у вас только один

        ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"4"});
        dialogFloorSpinner.setAdapter(floorAdapter);
        dialogFloorSpinner.setSelection(0);
        dialogFloorSpinner.setEnabled(false); // Этаж пока неактивен, если у вас только один

        // Обработчик нажатия на кнопку поиска в диалоге
        dialogSearchButton.setOnClickListener(v -> {
            String auditoriumNumber = dialogSearchAuditoriumEditText.getText().toString().trim();
            if (!auditoriumNumber.isEmpty()) {
                // Вызываем JavaScript-функцию searchAndHighlightAuditorium в WebView
                // Она сама определит полный ID (например, 1401ad) и передаст его в Java
                mapWebView.evaluateJavascript("searchAndHighlightAuditorium('" + auditoriumNumber + "');", null);
            } else {
                Toast.makeText(map.this, "Пожалуйста, введите номер аудитории", Toast.LENGTH_SHORT).show();
            }
        });

        builder.create().show(); // Показываем диалог
    }


    // Метод для получения информации об аудитории из Firebase Database
    // Теперь этот метод принимает ID, который ТОЧНО соответствует ключу в Firebase (например, "1401ad", "WC")
    public void fetchAuditoriumDetailsFromDB(String firebaseKey) {
        // Создаем "чистый" номер для отображения пользователю (без суффиксов 'ad'/'qw')
        String displayAuditoriumNumber = firebaseKey.replace("ad", "").replace("qw", "");

        Log.d("Firebase", "Запрашиваем данные из Firebase по ключу: " + firebaseKey);

        mDatabase.child(firebaseKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("Firebase", "Данные для ключа " + firebaseKey + " найдены. Значение: " + dataSnapshot.getValue());
                    // Преобразуем полученные данные в Map
                    Map<String, Object> auditoriumInfo = (Map<String, Object>) dataSnapshot.getValue();
                    if (auditoriumInfo != null) {
                        String name = (String) auditoriumInfo.get("name");
                        String description = (String) auditoriumInfo.get("description");
                        Long capacity = (Long) auditoriumInfo.get("capacity");

                        StringBuilder detailsBuilder = new StringBuilder();
                        // Используем "чистый" номер для вывода информации
                        detailsBuilder.append("Аудитория: ").append(displayAuditoriumNumber).append("\n");
                        if (name != null && !name.isEmpty()) {
                            detailsBuilder.append("Название: ").append(name).append("\n");
                        }
                        if (description != null && !description.isEmpty()) {
                            detailsBuilder.append("Тип: ").append(description).append("\n");
                        }
                        if (capacity != null) {
                            detailsBuilder.append("Вместительность: ").append(capacity).append(" человек");
                        }

                        String details = detailsBuilder.toString().trim();
                        updateAuditoriumInfoTextView(details);
                    } else {
                        updateAuditoriumInfoTextView("Информация по аудитории " + displayAuditoriumNumber + " не найдена.");
                    }
                } else {
                    // Если данные не найдены по переданному ключу
                    updateAuditoriumInfoTextView("Аудитория " + displayAuditoriumNumber + " не найдена в базе данных.");
                    Log.w("Firebase", "Ключ " + firebaseKey + " не найден в базе данных Firebase.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                updateAuditoriumInfoTextView("Ошибка при получении данных: " + databaseError.getMessage());
                Log.e("Firebase", "Ошибка чтения данных: " + databaseError.getMessage(), databaseError.toException());
            }
        });
    }

    // Метод для обновления текстового поля информации об аудитории
    private void updateAuditoriumInfoTextView(final String text) {
        runOnUiThread(() -> {
            if (infoTextView != null) {
                infoTextView.setText(text);
            } else {
                Toast.makeText(map.this, "TextView infoTextView не найден", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // JavaScript-интерфейс для взаимодействия с Android-кодом
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void fetchAuditoriumDetails(String auditoriumId) {
            // auditoriumId здесь может быть как "1401ad", так и "WC"
            Log.d("WebAppInterface", "fetchAuditoriumDetails вызван с ID: " + auditoriumId + " (будет использоваться как ключ Firebase)");
            map.this.fetchAuditoriumDetailsFromDB(auditoriumId);
        }

        @JavascriptInterface
        public void onAuditoriumNotFound(String auditoriumNumber) {
            runOnUiThread(() -> {
                updateAuditoriumInfoTextView("Аудитория " + auditoriumNumber + " не найдена на карте.");
                Toast.makeText(mContext, "Аудитория " + auditoriumNumber + " не найдена на карте.", Toast.LENGTH_LONG).show();
            });
        }
    }
}