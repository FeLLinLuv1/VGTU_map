package com.example.vgtu_map;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupListParserTask extends AsyncTask<String, Void, List<String>> {
    private static final String TAG = "GroupListParserTask";
    private final OnGroupListParsedListener listener;

    public interface OnGroupListParsedListener {
        void onGroupListParsed(List<String> groups);
        void onError(String message);
    }

    public GroupListParserTask(OnGroupListParsedListener listener) {
        this.listener = listener;
    }

    @Override
    protected List<String> doInBackground(String... urls) {
        String facultyUrl = urls[0];
        List<String> groupsList = new ArrayList<>();

        try {
            Log.d(TAG, "Начало парсинга списка групп на странице: " + facultyUrl);
            Document doc = Jsoup.connect(facultyUrl).get();
            // Вам необходимо исследовать HTML-структуру страницы факультета
            // и найти CSS-селекторы, которые позволяют извлечь названия групп.
            // Это может быть список <a> тегов, <li> тегов или других элементов.
            // Пример (вам нужно адаптировать селектор):
            Elements groupElements = doc.select("div.schedule-group-list a"); // Пример селектора

            for (Element groupElement : groupElements) {
                String groupName = groupElement.text().trim();
                if (!groupName.isEmpty()) {
                    groupsList.add(groupName);
                    Log.d(TAG, "Найдена группа: " + groupName);
                }
            }
            Log.d(TAG, "Парсинг списка групп завершен. Найдено групп: " + groupsList.size());
            return groupsList;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка при парсинге списка групп: " + e.getMessage());
            if (listener != null) {
                listener.onError("Ошибка при загрузке или парсинге списка групп: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (listener != null && result != null) {
            listener.onGroupListParsed(result);
        } else if (listener != null) {
            listener.onError("Не удалось получить список групп.");
        }
    }
}