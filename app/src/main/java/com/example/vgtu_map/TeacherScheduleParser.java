package com.example.vgtu_map;

import android.util.Log;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

public class TeacherScheduleParser {

    private static final String TAG = "TeacherScheduleParser";

    public static String parseScheduleForDay(XWPFDocument document, LocalDate date) {
        StringBuilder scheduleForDay = new StringBuilder("Расписание на " + getDayName(date.getDayOfWeek().getValue()) + ", " + getMonthName(date.getMonthValue()) + ":\n");
        if (document != null) {
            try {
                XWPFTable table = document.getTables().get(0); // Получаем первую таблицу

                DayOfWeek dayOfWeek = date.getDayOfWeek();
                boolean isNumerator = isNumeratorWeek(); // Реализуй эту функцию

                int startRowIndex = -1;
                switch (dayOfWeek) {
                    case MONDAY:
                        startRowIndex = isNumerator ? 2 : 8;
                        break;
                    case TUESDAY:
                        startRowIndex = isNumerator ? 3 : 9;
                        break;
                    case WEDNESDAY:
                        startRowIndex = isNumerator ? 4 : 10;
                        break;
                    case THURSDAY:
                        startRowIndex = isNumerator ? 5 : 11;
                        break;
                    case FRIDAY:
                        startRowIndex = isNumerator ? 6 : 12;
                        break;
                    case SATURDAY:
                        startRowIndex = isNumerator ? 7 : 13;
                        break;
                    case SUNDAY:
                        return "На этот день расписаний нет.";
                }

                if (startRowIndex != -1 && startRowIndex < table.getRows().size()) {
                    XWPFTableRow dayRow = table.getRow(startRowIndex);
                    XWPFTableRow timeHeaderRow = table.getRow(1);

                    if (timeHeaderRow != null && dayRow != null) {
                        for (int i = 1; i < timeHeaderRow.getTableCells().size(); i++) {
                            XWPFTableCell timeCell = timeHeaderRow.getCell(i);
                            if (timeCell != null) {
                                String time = timeCell.getText().trim();
                                if (!time.isEmpty() && i < dayRow.getTableCells().size()) {
                                    XWPFTableCell activityCell = dayRow.getCell(i);
                                    if (activityCell != null) {
                                        String activityInfo = activityCell.getText().trim();
                                        if (!activityInfo.isEmpty()) {
                                            scheduleForDay.append("Время: ").append(time).append("\n");
                                            scheduleForDay.append(activityInfo).append("\n\n");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Ошибка: Не найдены строки с временем или расписанием для дня.");
                        return "Ошибка: Некорректная структура таблицы.";
                    }
                } else {
                    return "На этот день расписаний нет.";
                }

            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Ошибка: Некорректная структура таблицы: " + e.getMessage());
                return "Ошибка: Некорректная структура таблицы.";
            }
        } else {
            return "Ошибка: Word-документ не был загружен.";
        }

        if (scheduleForDay.toString().endsWith(":\n")) {
            return "На этот день расписаний нет.";
        }
        return scheduleForDay.toString();
    }

    public static String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Понедельник";
            case 2 -> "Вторник";
            case 3 -> "Среда";
            case 4 -> "Четверг";
            case 5 -> "Пятница";
            case 6 -> "Суббота";
            case 7 -> "Воскресенье";
            default -> "";
        };
    }

    public static String getMonthName(int month) {
        return switch (month) {
            case 1 -> "января";
            case 2 -> "февраля";
            case 3 -> "марта";
            case 4 -> "апреля";
            case 5 -> "мая";
            case 6 -> "июня";
            case 7 -> "июля";
            case 8 -> "августа";
            case 9 -> "сентября";
            case 10 -> "октября";
            case 11 -> "ноября";
            case 12 -> "декабря";
            default -> "";
        };
    }

    // ВАЖНО: Реализуй эту функцию на основе своей логики определения числителя/знаменателя недели
    public static boolean isNumeratorWeek() {
        // Пример (тебе нужно реализовать свою логику):
        LocalDate now = LocalDate.now();
        // Например, считаем, что первая неделя года - знаменатель, вторая - числитель и т.д.
        int weekOfYear = now.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfYear());
        return weekOfYear % 2 == 0; // Четная неделя - числитель (пример)
    }
}