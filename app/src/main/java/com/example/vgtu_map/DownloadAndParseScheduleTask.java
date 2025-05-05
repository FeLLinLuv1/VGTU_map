package com.example.vgtu_map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class DownloadAndParseScheduleTask extends AsyncTask<String, Void, List<String>> {

    private static final String TAG = "DownloadParseTask";
    private final Context context;
    private final ScheduleDownloadListener listener;
    private String targetGroupName;

    public interface ScheduleDownloadListener {
        void onScheduleDownloaded(File file);
        void onError(String message);
    }

    public DownloadAndParseScheduleTask(Context context, ScheduleDownloadListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected List<String> doInBackground(String... params) {
        targetGroupName = params[0];
        if (targetGroupName == null || targetGroupName.isEmpty()) {
            listener.onError("Название группы не указано.");
            return null;
        }

        String schedulePageUrl = "https://cchgeu.ru/studentu/schedule/spo/";
        String excelUrl = null;

        try {
            Log.d(TAG, "Начинаем поиск Excel-файла для группы: " + targetGroupName);
            Document doc = Jsoup.connect(schedulePageUrl).get();
            Elements links = doc.select("a[href$=.xlsx], a[href$=.xls]");
            String normalizedTargetGroupName = targetGroupName.trim().toUpperCase();

            for (Element link : links) {
                String linkText = link.text().trim();
                String href = link.absUrl("href");
                Log.d(TAG, "Найдена ссылка на странице: Текст='" + linkText + "', URL='" + href + "'");

                // Проверяем, начинается ли текст ссылки (без учета регистра) с названия группы
                if (linkText.toUpperCase().startsWith(normalizedTargetGroupName)) {
                    // Затем проверяем, заканчивается ли URL на .xlsx или .xls
                    if (href.toLowerCase().endsWith(".xlsx") || href.toLowerCase().endsWith(".xls")) {
                        excelUrl = href;
                        Log.d(TAG, "Найдена целевая ссылка: " + excelUrl);
                        break;
                    }
                }
            }

            if (excelUrl == null) {
                Log.d(TAG, "Excel-файл для группы " + targetGroupName + " не найден на сайте.");
                listener.onError("Excel-файл для группы не найден на сайте.");
                return null;
            }

            Log.d(TAG, "Попытка скачивания файла по URL: " + excelUrl);
            File excelFile = downloadFile(excelUrl, targetGroupName);
            if (excelFile != null) {
                listener.onScheduleDownloaded(excelFile);
            } else {
                listener.onError("Ошибка при скачивании файла.");
            }

        } catch (IOException e) {
            Log.e(TAG, "Ошибка при поиске или скачивании файла: " + e.getMessage());
            listener.onError("Ошибка при поиске или скачивании файла: " + e.getMessage());
        }

        return null; // Мы не возвращаем список строк, а работаем с файлом напрямую
    }

    private File downloadFile(String fileUrl, String groupName) {
        try {
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            connection.connect();

            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            File outputFile = new File(context.getExternalFilesDir(null), groupName + "_" + System.currentTimeMillis() + (fileUrl.endsWith(".xlsx") ? ".xlsx" : ".xls"));
            FileOutputStream output = new FileOutputStream(outputFile);

            byte[] data = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();
            return outputFile;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка при скачивании файла: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<String> result) {
        // Больше не используется
    }
}