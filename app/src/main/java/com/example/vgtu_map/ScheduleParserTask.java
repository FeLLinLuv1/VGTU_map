package com.example.vgtu_map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScheduleParserTask extends AsyncTask<String, Void, Map<String, String>> {

    private static final String TAG = "ScheduleParserTask";
    private OnScheduleParsedListener mListener;
    private final Context mContext; // Добавляем поле для Context

    public interface OnScheduleParsedListener {
        void onScheduleParsed(Map<String, String> faculties); // Название факультета -> URL факультета
        void onParsingError(String errorMessage);
    }

    public ScheduleParserTask(Context context) { // Добавляем конструктор, принимающий Context
        mContext = context;
    }

    public void setOnScheduleParsedListener(OnScheduleParsedListener listener) {
        mListener = listener;
    }

    @Override
    protected Map<String, String> doInBackground(String... params) {
        String mainUrl = params[0];
        Map<String, String> facultiesMap = new HashMap<>();

        try {
            Log.d(TAG, "Начало парсинга страницы факультетов: " + mainUrl);
            Document doc = Jsoup.connect(mainUrl).get();
            // Ищем элементы, содержащие ссылки на страницы факультетов.
            // Вам может потребоваться изучить HTML-структуру страницы
            // https://cchgeu.ru/studentu/schedule/ и подобрать
            // более точный CSS-селектор.
            // Пример: ищем div с классом 'faculty-item' и внутри него ссылку.
            Elements facultyItems = doc.select("div.cat-list div.cat-desc a");

            for (Element link : facultyItems) {
                String facultyName = link.text().trim();
                String facultyUrl = link.attr("abs:href").trim();
                // Проверяем, что название и URL не пустые и URL ведет на страницу расписаний
                if (!facultyName.isEmpty() && !facultyUrl.isEmpty() && facultyUrl.contains("/schedule/")) {
                    facultiesMap.put(facultyName, facultyUrl);
                    Log.d(TAG, "Найден факультет: " + facultyName + ", URL: " + facultyUrl);
                }
            }
            Log.d(TAG, "Парсинг страницы факультетов завершен. Найдено факультетов: " + facultiesMap.size());
            return facultiesMap;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка при парсинге страницы факультетов: " + e.getMessage());
            if (mListener != null) {
                mListener.onParsingError("Ошибка при загрузке или парсинге списка факультетов: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(Map<String, String> result) {
        if (mListener != null && result != null) {
            mListener.onScheduleParsed(result);
        } else if (mListener != null) {
            mListener.onParsingError("Ошибка при получении списка факультетов с сайта.");
        }
    }
}