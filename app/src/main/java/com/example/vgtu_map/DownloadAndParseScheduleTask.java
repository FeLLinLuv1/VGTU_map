package com.example.vgtu_map;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadAndParseScheduleTask extends AsyncTask<String, Void, List<String>> {

    private static final String TAG = "DownloadParseTask";
    private final OnScheduleDownloadedAndParsedListener listener;
    private final String targetGroupName;
    private final String baseUrl = "https://cchgeu.ru/upload/iblock/1f2/"; // Базовая часть URL
    private final String fileSuffix = ".xlsx"; // Расширение файла

    public interface OnScheduleDownloadedAndParsedListener {
        void onScheduleDownloadedAndParsed(List<String> data);
        void onError(String message);
    }

    public DownloadAndParseScheduleTask(OnScheduleDownloadedAndParsedListener listener, String targetGroupName) {
        this.listener = listener;
        this.targetGroupName = targetGroupName;
    }

    @Override
    protected List<String> doInBackground(String... params) {
        if (targetGroupName == null || targetGroupName.isEmpty()) {
            listener.onError("Название группы не указано.");
            return null;
        }
        String fileUrl = baseUrl + targetGroupName.toLowerCase(Locale.getDefault()) + fileSuffix;
        return downloadAndParseExcel(fileUrl);
    }

    private List<String> downloadAndParseExcel(String fileUrl) {
        Log.d(TAG, "Начинаем загрузку расписания для группы: " + targetGroupName + " по URL: " + fileUrl);
        List<String> scheduleForToday = new ArrayList<>();
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Workbook workbook = null;

        // Исправленный расчёт даты начала учебного года
        LocalDate currentDate = LocalDate.now();
        LocalDate academicYearStart;
        if (currentDate.getMonthValue() < 9) {
            academicYearStart = LocalDate.of(currentDate.getYear() - 1, 9, 1);
        } else {
            academicYearStart = LocalDate.of(currentDate.getYear(), 9, 1);
        }

        long weeksSinceStart = Math.max(0, ChronoUnit.WEEKS.between(academicYearStart, currentDate) + 1);
        boolean isNumerator = (weeksSinceStart % 2 != 0);

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE", new Locale("ru", "RU"));
        String today = currentDate.format(dayFormatter);
        today = today.substring(0, 1).toUpperCase() + today.substring(1);

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                try {
                    if (fileUrl.endsWith(".xlsx")) {
                        workbook = new XSSFWorkbook(inputStream);
                    } else {
                        workbook = new HSSFWorkbook(inputStream);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при открытии Workbook: " + e.getMessage());
                    listener.onError("Не удалось открыть файл расписания.");
                    return null;
                }

                if (workbook != null) {
                    Sheet sheet = workbook.getSheetAt(0);
                    int startRow = 3;
                    int dayColumn = 0;
                    int timeColumn = 1;
                    int subjectStartColumn = 3;
                    int subjectEndColumn = 6;
                    int audienceColumn = 7;

                    for (int rowNum = startRow; rowNum <= sheet.getLastRowNum(); rowNum += 2) {
                        Row dayOfWeekRow = sheet.getRow(rowNum - 1);
                        if (dayOfWeekRow != null) {
                            Cell dayOfWeekCell = dayOfWeekRow.getCell(dayColumn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            String dayOfWeek = getCellValueAsString(dayOfWeekCell);
                            if (dayOfWeek.equalsIgnoreCase(today)) {
                                Row numeratorRow = sheet.getRow(rowNum - 1);
                                Row denominatorRow = sheet.getRow(rowNum);

                                if (numeratorRow != null) {
                                    Cell timeCell = numeratorRow.getCell(timeColumn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                    StringBuilder subjectNumeratorBuilder = new StringBuilder();
                                    for (int i = subjectStartColumn; i <= subjectEndColumn; i++) {
                                        subjectNumeratorBuilder.append(getCellValueAsString(numeratorRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))).append(" ");
                                    }
                                    String subjectNumerator = subjectNumeratorBuilder.toString().trim();
                                    Cell audienceCellNumerator = numeratorRow.getCell(audienceColumn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                    String audienceNumerator = getCellValueAsString(audienceCellNumerator);

                                    if (isNumerator && !subjectNumerator.isEmpty()) {
                                        scheduleForToday.add(String.format("%s\t%s\t%s (числитель)", getCellValueAsString(timeCell), subjectNumerator, audienceNumerator));
                                    }
                                }

                                if (denominatorRow != null) {
                                    Cell timeCell = denominatorRow.getCell(timeColumn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                    StringBuilder subjectDenominatorBuilder = new StringBuilder();
                                    for (int i = subjectStartColumn; i <= subjectEndColumn; i++) {
                                        subjectDenominatorBuilder.append(getCellValueAsString(denominatorRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))).append(" ");
                                    }
                                    String subjectDenominator = subjectDenominatorBuilder.toString().trim();
                                    Cell audienceCellDenominator = denominatorRow.getCell(audienceColumn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                    String audienceDenominator = getCellValueAsString(audienceCellDenominator);

                                    if (!isNumerator && !subjectDenominator.isEmpty()) {
                                        scheduleForToday.add(String.format("%s\t%s\t%s (знаменатель)", getCellValueAsString(timeCell), subjectDenominator, audienceDenominator));
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                listener.onError("Ошибка загрузки файла расписания. Код: " + connection.getResponseCode());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при загрузке или парсинге Excel файла: " + e.getMessage());
            listener.onError("Ошибка при обработке файла расписания: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Неизвестная ошибка: " + e.getMessage());
            listener.onError("Неизвестная ошибка: " + e.getMessage());
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {}
        }
        return scheduleForToday;
    }

    // Вспомогательный метод для получения значения ячейки как строки (оставлен без изменений)
    private String getCellValueAsString(Cell cell) {
        // ... (код метода оставлен без изменений)
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return new DecimalFormat("0.#").format(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        return new DecimalFormat("0.#").format(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        Log.e(TAG, "Ошибка при получении значения формулы: " + ex.getMessage());
                        return "Формула";
                    }
                }
            case BLANK:
                return "";
            case ERROR:
                return "Ошибка";
            default:
                return "Неизвестный тип";
        }
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (listener != null) {
            if (result != null) {
                listener.onScheduleDownloadedAndParsed(result);
            }
        }
    }

    @Overrideg
    protected void onCancelled() {
        super.onCancelled();
        Log.i(TAG, "Задача загрузки и парсинга отменена");
    }
}