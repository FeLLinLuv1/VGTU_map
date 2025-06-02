package com.example.vgtu_map;

import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelParser {

    private static final String TAG = "ExcelParser";
    private static final String DEBUG_PARSE_DETAIL = "DEBUG_PARSE_DETAIL"; // Новый тег для детального логирования

    public static String parseScheduleForToday(File file) {
        Log.d(TAG, "--- Начало обработки расписания на сегодня ---");
        return parseScheduleForDay(file, 0);
    }
    public static String parseScheduleForTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на завтра ---");
        return parseScheduleForDay(file, 1);
    }
    public static String parseScheduleForAfterTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на послезавтра ---");
        return parseScheduleForDay(file, 2);
    }
    public static String parseScheduleForDay(File file, int dayOffset) {
        boolean hasCombinedGroups = checkCombinedGroups(file, dayOffset);
        if (hasCombinedGroups) {
            Log.d(TAG, "parseScheduleForDay: Обнаружены объединенные группы. Используем расширенный парсинг (3 подгруппы).");
            return parseScheduleForThreeSubgroups(file, dayOffset);
        } else {
            Log.d(TAG, "parseScheduleForDay: Объединенные группы не обнаружены. Используем парсинг для двух подгрупп.");
            return parseScheduleForTwoSubgroups(file, dayOffset);
        }
    }
    private static boolean checkCombinedGroups(File file, int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundDay = false;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell dayCell = row.getCell(0);

                if (!foundDay) {
                    if (dayCell != null && getStringCellValue(dayCell).trim().toUpperCase(Locale.getDefault()).startsWith(dayName)) {
                        foundDay = true;
                    }
                } else {
                    Cell timeCell = row.getCell(1);
                    if (timeCell != null && !getStringCellValue(timeCell).trim().isEmpty()) {
                        Cell combinedRoomCell = row.getCell(9);
                        if (combinedRoomCell != null && !getStringCellValue(combinedRoomCell).trim().isEmpty()) {
                            Log.d(TAG, "checkCombinedGroups: Обнаружена объединенная группа (3 подгруппы) в строке " + row.getRowNum() + " для дня " + dayName);
                            return true;
                        }
                    } else {
                    }
                    Cell nextDayIndicatorCell = row.getCell(0);
                    if (nextDayIndicatorCell != null && getStringCellValue(nextDayIndicatorCell).trim().toUpperCase(Locale.getDefault()).startsWith(getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                        break;
                    }
                }
            }
            Log.d(TAG, "checkCombinedGroups: Объединенные группы (3 подгруппы) не обнаружены для дня " + dayName + ". Используем парсинг для 2 подгрупп.");
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "checkCombinedGroups: Ошибка чтения файла: " + e.getMessage());
        }
        return false;
    }
    private static String parseScheduleForThreeSubgroups(File file, int dayOffset) {
        StringBuilder scheduleForDay = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        boolean isNumeratorWeek = isNumeratorWeek();
        Log.d(TAG, "parseScheduleForThreeSubgroups: Выбранный день: " + dayName + ", Неделя числителя: " + isNumeratorWeek + ", Смещение: " + dayOffset);
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundDaySection = false;
            int lastRowOfCurrentDay = -1;
            Iterator<Row> rowIterator = sheet.iterator();
            Row currentRow = null;
            String currentTimeForPair = "";

            while (rowIterator.hasNext()) {
                currentRow = rowIterator.next();
                int rowNum = currentRow.getRowNum();
                Cell dayCell = currentRow.getCell(0);
                String currentCellDayValue = (dayCell != null) ? getStringCellValue(dayCell).trim().toUpperCase(Locale.getDefault()) : "";
                Log.d(DEBUG_PARSE_DETAIL, "--- Обработка строки: " + rowNum + " ---");
                Log.d(DEBUG_PARSE_DETAIL, "Cell(0) value: '" + currentCellDayValue + "'");
                Log.d(DEBUG_PARSE_DETAIL, "foundDaySection: " + foundDaySection);
                if (!foundDaySection) {
                    if (currentCellDayValue.startsWith(dayName)) {
                        foundDaySection = true;
                        Log.d(DEBUG_PARSE_DETAIL, "Найдена секция дня: " + dayName + " в строке " + rowNum);
                        lastRowOfCurrentDay = rowNum + 13;
                    }
                }
                if (foundDaySection) {
                    if (rowNum > lastRowOfCurrentDay) {
                        Log.d(TAG, "parseScheduleForThreeSubgroups: Вышли за пределы дня " + dayName + ". Завершаем.");
                        break;
                    }
                    if (!currentCellDayValue.isEmpty() && currentCellDayValue.startsWith(getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                        Log.d(DEBUG_PARSE_DETAIL, "Условие остановки дня сработало в строке " + rowNum + " для дня " + dayName);
                        Log.d(DEBUG_PARSE_DETAIL, "Причина: Обнаружен следующий день: '" + currentCellDayValue + "'. Ожидался: '" + getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()) + "'");
                        break; // Выход из цикла
                    }
                    Cell timeCell = currentRow.getCell(1);
                    String cellTimeValue = (timeCell != null) ? getStringCellValue(timeCell).trim() : "";
                    if (!cellTimeValue.isEmpty()) {
                        currentTimeForPair = cellTimeValue;
                        Log.d(DEBUG_PARSE_DETAIL, "Найдено новое время пары: '" + currentTimeForPair + "' в строке " + rowNum);
                    } else {
                        Log.d(DEBUG_PARSE_DETAIL, "Время пары в строке " + rowNum + " пустое. Используем предыдущее время: '" + currentTimeForPair + "'");
                    }
                    if (!currentTimeForPair.isEmpty()) {
                        String[] times = currentTimeForPair.split(" - ");
                        if (times.length == 2) {
                            String subject1Numerator = extractSubjectName(getStringCellValue(currentRow.getCell(3)));
                            String room1Numerator = getStringCellValue(currentRow.getCell(4));
                            String subject2Numerator = extractSubjectName(getStringCellValue(currentRow.getCell(5)));
                            String room2Numerator = getStringCellValue(currentRow.getCell(6));
                            String subject3Numerator = extractSubjectName(getStringCellValue(currentRow.getCell(7)));
                            String room3Numerator = getStringCellValue(currentRow.getCell(8));
                            String commonRoomNumerator = getStringCellValue(currentRow.getCell(9));

                            String subject1Denominator = "";
                            String room1Denominator = "";
                            String subject2Denominator = "";
                            String room2Denominator = "";
                            String subject3Denominator = "";
                            String room3Denominator = "";
                            String commonRoomDenominator = "";

                            Row nextRowForDenominatorData = sheet.getRow(rowNum + 1);
                            if (nextRowForDenominatorData != null && (rowNum + 1) <= lastRowOfCurrentDay) {
                                Cell nextTimeCell = nextRowForDenominatorData.getCell(1);
                                if (nextTimeCell == null || getStringCellValue(nextTimeCell).trim().isEmpty()) {
                                    subject1Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(3)));
                                    room1Denominator = getStringCellValue(nextRowForDenominatorData.getCell(4));
                                    subject2Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(5)));
                                    room2Denominator = getStringCellValue(nextRowForDenominatorData.getCell(6));
                                    subject3Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(7)));
                                    room3Denominator = getStringCellValue(nextRowForDenominatorData.getCell(8));
                                    commonRoomDenominator = getStringCellValue(nextRowForDenominatorData.getCell(9));
                                }
                            }
                            if (isNumeratorWeek) {
                                if (!cellTimeValue.isEmpty()) { // Обрабатываем только если это строка с временем для числителя
                                    if (!commonRoomNumerator.isEmpty()) {
                                        Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель, совмещенная): Время='" + currentTimeForPair + "', Предмет='" + getNonEmptySubject(subject1Numerator, subject2Numerator, subject3Numerator) + "', Аудитория='" + commonRoomNumerator + "'");
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                                getNonEmptySubject(subject1Numerator, subject2Numerator, subject3Numerator),
                                                commonRoomNumerator,
                                                " (совмещенная, числитель)"));
                                    } else {
                                        if (!subject1Numerator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель 1 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject1Numerator + "', Аудитория='" + room1Numerator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Numerator, room1Numerator, " (1 п/г, числитель)"));
                                        }
                                        if (!subject2Numerator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель 2 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject2Numerator + "', Аудитория='" + room2Numerator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Numerator, room2Numerator, " (2 п/г, числитель)"));
                                        }
                                        if (!subject3Numerator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель 3 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject3Numerator + "', Аудитория='" + room3Numerator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject3Numerator, room3Numerator, " (3 п/г, числитель)"));
                                        }
                                    }
                                }
                            } else {
                                if (!cellTimeValue.isEmpty()) {
                                    if (!commonRoomDenominator.isEmpty()) {
                                        Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель, совмещенная): Время='" + currentTimeForPair + "', Предмет='" + getNonEmptySubject(subject1Denominator, subject2Denominator, subject3Denominator) + "', Аудитория='" + commonRoomDenominator + "'");
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                                getNonEmptySubject(subject1Denominator, subject2Denominator, subject3Denominator),
                                                commonRoomDenominator,
                                                " (совмещенная, знаменатель)"));
                                    } else {
                                        if (!subject1Denominator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель 1 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject1Denominator + "', Аудитория='" + room1Denominator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Denominator, room1Denominator, " (1 п/г, знаменатель)"));
                                        }
                                        if (!subject2Denominator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель 2 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject2Denominator + "', Аудитория='" + room2Denominator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Denominator, room2Denominator, " (2 п/г, знаменатель)"));
                                        }
                                        if (!subject3Denominator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель 3 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject3Denominator + "', Аудитория='" + room3Denominator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject3Denominator, room3Denominator, " (3 п/г, знаменатель)"));
                                        }
                                    }
                                    if (rowIterator.hasNext()) {
                                        Row skippedRow = rowIterator.next(); // Пропускаем строку с данными знаменателя
                                        Log.d(DEBUG_PARSE_DETAIL, "Пропускаем строку знаменателя: " + skippedRow.getRowNum());
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "parseScheduleForThreeSubgroups: Неправильный формат времени в строке " + rowNum + ": " + currentTimeForPair);
                        }
                    }
                }
            }
            if (scheduleForDay.length() == 0 && foundDaySection) {
                scheduleForDay.append("На этот день расписаний нет.");
            } else if (!foundDaySection) {
                scheduleForDay.append("Расписание на этот день не найдено.");
            }
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "parseScheduleForThreeSubgroups: Ошибка чтения файла: " + e.getMessage());
            return "Ошибка при чтении файла расписания.";
        }
        Log.d(TAG, "parseScheduleForThreeSubgroups: --- Конец обработки расписания на " + dayName + " ---");
        Log.d(TAG, "parseScheduleForThreeSubgroups: Итоговое расписание на " + dayName + ":\n" + scheduleForDay.toString());
        return scheduleForDay.toString();
    }
    private static String getNonEmptySubject(String sub1, String sub2, String sub3) {
        if (!sub1.isEmpty()) return sub1;
        if (!sub2.isEmpty()) return sub2;
        if (!sub3.isEmpty()) return sub3;
        return "";
    }

    private static String parseScheduleForTwoSubgroups(File file, int dayOffset) {
        StringBuilder scheduleForDay = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        boolean isNumeratorWeek = isNumeratorWeek();

        Log.d(TAG, "parseScheduleForTwoSubgroups: Выбранный день: " + dayName + ", Неделя числителя: " + isNumeratorWeek + ", Смещение: " + dayOffset);

        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundDaySection = false;
            int lastRowOfCurrentDay = -1;
            Iterator<Row> rowIterator = sheet.iterator();
            Row currentRow = null;
            String currentTimeForPair = "";
            while (rowIterator.hasNext()) {
                currentRow = rowIterator.next();
                int rowNum = currentRow.getRowNum();
                Cell dayCell = currentRow.getCell(0);
                String currentCellDayValue = (dayCell != null) ? getStringCellValue(dayCell).trim().toUpperCase(Locale.getDefault()) : "";

                Log.d(DEBUG_PARSE_DETAIL, "--- Обработка строки: " + rowNum + " ---");
                Log.d(DEBUG_PARSE_DETAIL, "Cell(0) value: '" + currentCellDayValue + "'");
                Log.d(DEBUG_PARSE_DETAIL, "foundDaySection: " + foundDaySection);
                if (!foundDaySection) {
                    if (currentCellDayValue.startsWith(dayName)) {
                        foundDaySection = true;
                        Log.d(DEBUG_PARSE_DETAIL, "Найдена секция дня: " + dayName + " в строке " + rowNum);
                        lastRowOfCurrentDay = rowNum + 13;
                    }
                }
                if (foundDaySection) {
                    if (rowNum > lastRowOfCurrentDay) {
                        Log.d(TAG, "parseScheduleForTwoSubgroups: Вышли за пределы дня " + dayName + ". Завершаем.");
                        break;
                    }
                    if (!currentCellDayValue.isEmpty() && currentCellDayValue.startsWith(getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                        Log.d(DEBUG_PARSE_DETAIL, "Условие остановки дня сработало в строке " + rowNum + " для дня " + dayName);
                        Log.d(DEBUG_PARSE_DETAIL, "Причина: Обнаружен следующий день: '" + currentCellDayValue + "'. Ожидался: '" + getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()) + "'");
                        break;
                    }
                    Cell timeCell = currentRow.getCell(1);
                    String cellTimeValue = (timeCell != null) ? getStringCellValue(timeCell).trim() : "";
                    if (!cellTimeValue.isEmpty()) {
                        currentTimeForPair = cellTimeValue; // Обновляем текущее время пары
                        Log.d(DEBUG_PARSE_DETAIL, "Найдено новое время пары: '" + currentTimeForPair + "' в строке " + rowNum);
                    } else {
                        Log.d(DEBUG_PARSE_DETAIL, "Время пары в строке " + rowNum + " пустое. Используем предыдущее время: '" + currentTimeForPair + "'");
                    }
                    if (!currentTimeForPair.isEmpty()) {
                        String[] times = currentTimeForPair.split(" - ");
                        if (times.length == 2) {
                            String startTime = times[0].trim();
                            String endTime = times[1].trim();
                            String subject1Numerator = extractSubjectName(getStringCellValue(currentRow.getCell(3)));
                            String room1Numerator = getStringCellValue(currentRow.getCell(4));
                            String subject2Numerator = extractSubjectName(getStringCellValue(currentRow.getCell(5)));
                            String room2Numerator = getStringCellValue(currentRow.getCell(6));
                            String commonRoomNumerator = getStringCellValue(currentRow.getCell(7));
                            String subject1Denominator = "";
                            String room1Denominator = "";
                            String subject2Denominator = "";
                            String room2Denominator = "";
                            String commonRoomDenominator = "";

                            Row nextRowForDenominatorData = sheet.getRow(rowNum + 1);
                            if (nextRowForDenominatorData != null && rowNum + 1 <= lastRowOfCurrentDay) {
                                Cell nextTimeCell = nextRowForDenominatorData.getCell(1);
                                // Если следующая строка не является началом новой пары (её время пустое)
                                if (nextTimeCell == null || getStringCellValue(nextTimeCell).trim().isEmpty()) {
                                    subject1Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(3)));
                                    room1Denominator = getStringCellValue(nextRowForDenominatorData.getCell(4));
                                    subject2Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(5)));
                                    room2Denominator = getStringCellValue(nextRowForDenominatorData.getCell(6));
                                    commonRoomDenominator = getStringCellValue(nextRowForDenominatorData.getCell(7));
                                }
                            }
                            if (isNumeratorWeek) {
                                if (!cellTimeValue.isEmpty()) { // Только если это строка с временем для числителя
                                    if (!commonRoomNumerator.isEmpty()) {
                                        Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель, совмещенная): Время='" + currentTimeForPair + "', Предмет='" + (subject1Numerator.isEmpty() ? subject2Numerator : subject1Numerator) + "', Аудитория='" + commonRoomNumerator + "'");
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Numerator.isEmpty() ? subject2Numerator : subject1Numerator, commonRoomNumerator, " (совмещенная, числитель)"));
                                    } else {
                                        if (!subject1Numerator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель 1 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject1Numerator + "', Аудитория='" + room1Numerator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Numerator, room1Numerator, " (1 п/г, числитель)"));
                                        }
                                        if (!subject2Numerator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (числитель 2 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject2Numerator + "', Аудитория='" + room2Numerator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Numerator, room2Numerator, " (2 п/г, числитель)"));
                                        }
                                    }
                                }
                            } else {
                                if (!cellTimeValue.isEmpty()) { // Это строка, где указано время пары (напр., 8:30)
                                    if (!commonRoomDenominator.isEmpty()) {
                                        Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель, совмещенная): Время='" + currentTimeForPair + "', Предмет='" + (subject1Denominator.isEmpty() ? subject2Denominator : subject1Denominator) + "', Аудитория='" + commonRoomDenominator + "'");
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Denominator.isEmpty() ? subject2Denominator : subject1Denominator, commonRoomDenominator, " (совмещенная, знаменатель)"));
                                    } else {
                                        if (!subject1Denominator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель 1 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject1Denominator + "', Аудитория='" + room1Denominator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Denominator, room1Denominator, " (1 п/г, знаменатель)"));
                                        }
                                        if (!subject2Denominator.isEmpty()) {
                                            Log.d(DEBUG_PARSE_DETAIL, "Добавляем запись (знаменатель 2 п/г): Время='" + currentTimeForPair + "', Предмет='" + subject2Denominator + "', Аудитория='" + room2Denominator + "'");
                                            scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Denominator, room2Denominator, " (2 п/г, знаменатель)"));
                                        }
                                    }
                                    if (rowIterator.hasNext()) {
                                        Row skippedRow = rowIterator.next(); // Пропускаем строку с данными знаменателя
                                        Log.d(DEBUG_PARSE_DETAIL, "Пропускаем строку знаменателя: " + skippedRow.getRowNum());
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "parseScheduleForTwoSubgroups: Неправильный формат времени в строке " + rowNum + ": " + currentTimeForPair);
                        }
                    }
                }
            }

            if (scheduleForDay.length() == 0 && foundDaySection) {
                scheduleForDay.append("На этот день расписаний нет.");
            } else if (!foundDaySection) {
                scheduleForDay.append("Расписание на этот день не найдено.");
            }

        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "parseScheduleForTwoSubgroups: Ошибка чтения файла: " + e.getMessage());
            return "Ошибка при чтении файла расписания.";
        }
        Log.d(TAG, "parseScheduleForTwoSubgroups: --- Конец обработки расписания на " + dayName + " ---");
        Log.d(TAG, "parseScheduleForTwoSubgroups: Итоговое расписание на " + dayName + ":\n" + scheduleForDay.toString());
        return scheduleForDay.toString();
    }

    public static boolean isNumeratorWeek() {
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        Calendar septemberFirst = Calendar.getInstance();
        septemberFirst.set(currentYear, Calendar.SEPTEMBER, 1, 0, 0, 0);
        septemberFirst.set(Calendar.MILLISECOND, 0);

        if (now.before(septemberFirst)) {
            septemberFirst.set(currentYear - 1, Calendar.SEPTEMBER, 1, 0, 0, 0);
            septemberFirst.set(Calendar.MILLISECOND, 0);
        }

        long diff = now.getTimeInMillis() - septemberFirst.getTimeInMillis();
        long daysSinceSeptemberFirst = diff / (24 * 60 * 60 * 1000);
        long weekNumber = daysSinceSeptemberFirst / 7;

        return weekNumber % 2 == 0; // Четные недели (начиная с 0) - числитель
    }

    private static String getNextDayName(int currentDayOfWeek) {
        int nextDayOfWeek = (currentDayOfWeek == Calendar.SATURDAY) ? Calendar.SUNDAY : currentDayOfWeek + 1; // Исправлено для субботы
        return getDayName(nextDayOfWeek);
    }

    private static String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "ПОНЕДЕЛЬНИК";
            case Calendar.TUESDAY: return "ВТОРНИК";
            case Calendar.WEDNESDAY: return "СРЕДА";
            case Calendar.THURSDAY: return "ЧЕТВЕРГ";
            case Calendar.FRIDAY: return "ПЯТНИЦА";
            case Calendar.SATURDAY: return "СУББОТА";
            case Calendar.SUNDAY: return "ВОСКРЕСЕНЬЕ";
            default: return "";
        }
    }


    private static String formatScheduleEntry(String time, String subject, String room, String subgroupInfo) {
        // Если предмет и аудитория пустые, не добавляем запись.
        if (subject.isEmpty() && room.isEmpty()) {
            return "";
        }

        StringBuilder entry = new StringBuilder();

        // Добавляем время с информацией о подгруппе
        entry.append("Время: ").append(time);
        if (!subgroupInfo.isEmpty()) {
            entry.append(subgroupInfo); // subgroupInfo уже включает скобки и пробел
        }
        entry.append("\n"); // Новая строка после времени

        // Добавляем предмет
        entry.append("Предмет: ").append(subject).append("\n");

        // Добавляем аудиторию (только если она не пустая)
        if (!room.isEmpty()) {
            entry.append("Аудитория: ").append(room).append("\n\n");
        }
        else{
            entry.append("\n");
        }

        return entry.toString();
    }

    private static String extractSubjectName(String cellValue) {

        Pattern pattern = Pattern.compile("\\s*\\([^)]*\\)\\s*");
        Matcher matcher = pattern.matcher(cellValue);
        String result = matcher.replaceAll("").trim();

        // Дополнительно удаляем " (ЛК)", " (ПР)", " (ЛАБ)"
        result = result.replaceAll("\\s*\\(ЛК\\)", "").trim();
        result = result.replaceAll("\\s*\\(ПР\\)", "").trim();
        result = result.replaceAll("\\s*\\(ЛАБ\\)", "").trim();

        return result;
    }


    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            CellType cellType = cell.getCellType();
            if (cellType == CellType.STRING) return cell.getStringCellValue().trim();
            if (cellType == CellType.NUMERIC) {

                if (DateUtil.isCellDateFormatted(cell)) {
                    // Получаем дату, затем форматируем только время
                    java.util.Date date = cell.getDateCellValue();
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()); // Изменено на HH:mm
                    return timeFormat.format(date);
                } else {
                    // Если это просто число, возвращаем его строковое представление
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            }
            if (cellType == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
            if (cellType == CellType.FORMULA) {
                try {
                    return getStringCellValue(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    return getStringCellValue(cellValue.getCellType(), cellValue);
                }
            }
            return "";
        } catch (Exception e) {
            Log.e(TAG, "getStringCellValue: Ошибка получения значения ячейки: " + e.getMessage() + " для ячейки в строке " + cell.getRowIndex() + ", колонке " + cell.getColumnIndex());
            return "";
        }
    }
    private static String getStringCellValue(CellType cellType, Cell cell) {
        if (cellType == CellType.STRING) return cell.getStringCellValue().trim();
        if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()); // Изменено на HH:mm
                return timeFormat.format(date);
            } else {
                return String.valueOf((int) cell.getNumericCellValue());
            }
        }
        if (cellType == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return "";
    }
    private static String getStringCellValue(CellType cellType, CellValue cellValue) {
        if (cellType == CellType.STRING) return cellValue.getStringValue().trim();
        if (cellType == CellType.NUMERIC) {
            return String.valueOf((int) cellValue.getNumberValue());
        }
        if (cellType == CellType.BOOLEAN) return String.valueOf(cellValue.getBooleanValue());
        return "";
    }
}