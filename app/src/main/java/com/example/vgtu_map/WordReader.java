package com.example.vgtu_map;

import android.content.Context;
import android.util.Log;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;

public class WordReader {

    private static final String TAG = "WordReader";
    private static final String FILE_EXTENSION = ".docx";

    public static XWPFDocument getWordDocument(Context context, String teacherLastName) {
        String fileName = teacherLastName.toLowerCase() + FILE_EXTENSION; // Формируем имя файла
        InputStream inputStream = getTeacherScheduleInputStream(context, fileName);
        XWPFDocument document = null;
        if (inputStream != null) {
            try {
                document = new XWPFDocument(inputStream);
                Log.d(TAG, "Word-документ успешно открыт: " + fileName);
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при открытии Word-документа " + fileName + ": " + e.getMessage());
            } finally {
                try {
                    inputStream.close(); // Важно закрывать поток
                } catch (IOException e) {
                    Log.e(TAG, "Ошибка при закрытии InputStream: " + e.getMessage());
                }
            }
        }
        return document;
    }

    private static InputStream getTeacherScheduleInputStream(Context context, String fileName) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(fileName);
            Log.d(TAG, "Успешно открыт файл из assets: " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при открытии файла из assets " + fileName + ": " + e.getMessage());
        }
        return inputStream;
    }
}