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
    private static final String DEBUG_PARSE_DETAIL = "DEBUG_PARSE_DETAIL";

    // Изменяем эти методы, чтобы они передавали текущую дату для isNumeratorWeek
    public static String parseScheduleForToday(File file) {
        Log.d(TAG, "--- Начало обработки расписания на сегодня ---");
        return parseScheduleForDay(file, 0, Calendar.getInstance()); // Передаем текущий календарь
    }
    public static String parseScheduleForTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на завтра ---");
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        return parseScheduleForDay(file, 1, tomorrow); // Передаем календарь для завтра
    }
    public static String parseScheduleForAfterTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на послезавтра ---");
        Calendar afterTomorrow = Calendar.getInstance();
        afterTomorrow.add(Calendar.DATE, 2);
        return parseScheduleForDay(file, 2, afterTomorrow); // Передаем календарь для послезавтра
    }

    // Основной метод парсинга. Теперь он принимает Calendar, чтобы определить числитель/знаменатель
    public static String parseScheduleForDay(File file, int dayOffset, Calendar baseCalendarForWeekCheck) {
        // Проверяем на объединенные группы. Здесь dayOffset используется для определения дня недели,
        // а isNumeratorWeek будет использоваться для определения типа недели.
        // Передаем baseCalendarForWeekCheck для isNumeratorWeek
        boolean hasCombinedGroups = checkCombinedGroups(file, dayOffset, baseCalendarForWeekCheck);

        if (hasCombinedGroups) {
            Log.d(TAG, "parseScheduleForDay: Обнаружены объединенные группы. Используем расширенный парсинг (3 подгруппы).");
            return parseScheduleForThreeSubgroups(file, dayOffset, baseCalendarForWeekCheck);
        } else {
            Log.d(TAG, "parseScheduleForDay: Объединенные группы не обнаружены. Используем парсинг для двух подгрупп.");
            return parseScheduleForTwoSubgroups(file, dayOffset, baseCalendarForWeekCheck);
        }
    }

    // checkCombinedGroups также должен принимать Calendar для корректного isNumeratorWeek
    private static boolean checkCombinedGroups(File file, int dayOffset, Calendar baseCalendarForWeekCheck) {
        Calendar calendar = (Calendar) baseCalendarForWeekCheck.clone(); // Клонируем, чтобы не менять оригинал
        calendar.add(Calendar.DATE, dayOffset); // Смещаем на нужный день
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());

        // isNumeratorWeek() будет вызван внутри parseScheduleForThreeSubgroups/parseScheduleForTwoSubgroups
        // и он получит нужный calendar.

        // Здесь эта логика checkCombinedGroups не зависит напрямую от числителя/знаменателя,
        // она лишь определяет формат таблицы. Но для единообразия сигнатуры
        // и чтобы избежать лишних вызовов isNumeratorWeek внутри этого метода,
        // оставим так.

        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0); // Предполагаем, что всегда работаем с первым листом
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
                        Cell combinedRoomCell = row.getCell(9); // Колонка для 3-й подгруппы
                        if (combinedRoomCell != null && !getStringCellValue(combinedRoomCell).trim().isEmpty()) {
                            Log.d(TAG, "checkCombinedGroups: Обнаружена объединенная группа (3 подгруппы) в строке " + row.getRowNum() + " для дня " + dayName);
                            return true;
                        }
                    } else {
                        // Если timeCell пуст, это может быть строка знаменателя, игнорируем ее для определения формата
                    }
                    Cell nextDayIndicatorCell = row.getCell(0);
                    // Проверяем, не является ли следующая строка началом нового дня
                    if (nextDayIndicatorCell != null && getStringCellValue(nextDayIndicatorCell).trim().toUpperCase(Locale.getDefault()).startsWith(getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                        break; // Достигли следующего дня, прекращаем поиск
                    }
                }
            }
            Log.d(TAG, "checkCombinedGroups: Объединенные группы (3 подгруппы) не обнаружены для дня " + dayName + ". Используем парсинг для 2 подгрупп.");
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "checkCombinedGroups: Ошибка чтения файла: " + e.getMessage());
        }
        return false;
    }

    // Модифицируем parseScheduleForThreeSubgroups
    private static String parseScheduleForThreeSubgroups(File file, int dayOffset, Calendar baseCalendarForWeekCheck) {
        StringBuilder scheduleForDay = new StringBuilder();
        Calendar calendar = (Calendar) baseCalendarForWeekCheck.clone();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset); // Добавляем смещение к базовому календарю
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());

        // Теперь isNumeratorWeek() принимает calendar, который мы передали из MainActivity
        boolean isNumeratorWeek = isNumeratorWeek(calendar);
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
                        lastRowOfCurrentDay = rowNum + 13; // Предполагаемая последняя строка для дня
                    }
                }

                if (foundDaySection) {
                    // Условие для определения конца секции дня
                    if (rowNum > lastRowOfCurrentDay && !currentCellDayValue.startsWith(dayName)) {
                        Log.d(TAG, "parseScheduleForThreeSubgroups: Вышли за пределы дня " + dayName + " (по счетчику строк). Завершаем.");
                        break;
                    }
                    if (!currentCellDayValue.isEmpty() && !currentCellDayValue.startsWith(dayName) && currentCellDayValue.length() > 2) { // Проверяем, что это не просто пустая ячейка или число
                        String nextDayPrefix = getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()).substring(0, Math.min(3, getNextDayName(dayOfWeek).length()));
                        if (currentCellDayValue.startsWith(nextDayPrefix)) { // Проверка на совпадение первых 3-х символов следующего дня
                            Log.d(DEBUG_PARSE_DETAIL, "Условие остановки дня сработало в строке " + rowNum + " для дня " + dayName);
                            Log.d(DEBUG_PARSE_DETAIL, "Причина: Обнаружен следующий день: '" + currentCellDayValue + "'. Ожидался префикс: '" + nextDayPrefix + "'");
                            break; // Выход из цикла
                        }
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
                        // Здесь ваша логика парсинга строк для числителя/знаменателя
                        // Убедитесь, что rowIterator.next() для пропуска строки знаменателя
                        // вызывается корректно, чтобы не пропустить реальные данные.

                        // Определяем, является ли текущая строка строкой числителя по наличию времени
                        if (!cellTimeValue.isEmpty()) { // Это строка с данными числителя
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

                            Row nextRowForDenominatorData = null;
                            if (rowIterator.hasNext()) { // Проверяем, есть ли следующая строка
                                nextRowForDenominatorData = rowIterator.next();
                                // Убедимся, что эта строка действительно является строкой знаменателя
                                // (у нее пустое время, но есть данные)
                                Cell potentialNextTimeCell = nextRowForDenominatorData.getCell(1);
                                if (potentialNextTimeCell == null || getStringCellValue(potentialNextTimeCell).trim().isEmpty()) {
                                    subject1Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(3)));
                                    room1Denominator = getStringCellValue(nextRowForDenominatorData.getCell(4));
                                    subject2Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(5)));
                                    room2Denominator = getStringCellValue(nextRowForDenominatorData.getCell(6));
                                    subject3Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(7)));
                                    room3Denominator = getStringCellValue(nextRowForDenominatorData.getCell(8));
                                    commonRoomDenominator = getStringCellValue(nextRowForDenominatorData.getCell(9));
                                    Log.d(DEBUG_PARSE_DETAIL, "Пропускаем строку знаменателя: " + nextRowForDenominatorData.getRowNum());
                                } else {
                                    // Если следующая строка не пустая по времени, значит, это не строка знаменателя
                                    // для текущей пары. Возвращаем итератор, если это возможно, или обрабатываем как следующую пару.
                                    // Для POIIterator, это не так просто. Проще не вызывать next() если не уверены.
                                    // В данном случае, мы уже вызвали next(), поэтому нужно убедиться,
                                    // что мы не пропустили реальную следующую пару.
                                    // Здесь логика может усложниться, если формат не строгий "числитель, затем знаменатель".
                                    // Но для текущей структуры, предположим, что либо это знаменатель, либо следующая пара.
                                    // Если это следующая пара, она будет обработана на следующей итерации while.
                                }
                            }

                            if (isNumeratorWeek) {
                                if (!commonRoomNumerator.isEmpty()) {
                                    scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                            getNonEmptySubject(subject1Numerator, subject2Numerator, subject3Numerator),
                                            commonRoomNumerator,
                                            " (совмещенная, числитель)"));
                                } else {
                                    if (!subject1Numerator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Numerator, room1Numerator, " (1 п/г, числитель)"));
                                    }
                                    if (!subject2Numerator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Numerator, room2Numerator, " (2 п/г, числитель)"));
                                    }
                                    if (!subject3Numerator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject3Numerator, room3Numerator, " (3 п/г, числитель)"));
                                    }
                                }
                            } else { // Это знаменатель
                                if (!commonRoomDenominator.isEmpty()) {
                                    scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                            getNonEmptySubject(subject1Denominator, subject2Denominator, subject3Denominator),
                                            commonRoomDenominator,
                                            " (совмещенная, знаменатель)"));
                                } else {
                                    if (!subject1Denominator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Denominator, room1Denominator, " (1 п/г, знаменатель)"));
                                    }
                                    if (!subject2Denominator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Denominator, room2Denominator, " (2 п/г, знаменатель)"));
                                    }
                                    if (!subject3Denominator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject3Denominator, room3Denominator, " (3 п/г, знаменатель)"));
                                    }
                                }
                            }
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


    // Модифицируем parseScheduleForTwoSubgroups
    private static String parseScheduleForTwoSubgroups(File file, int dayOffset, Calendar baseCalendarForWeekCheck) {
        StringBuilder scheduleForDay = new StringBuilder();
        Calendar calendar = (Calendar) baseCalendarForWeekCheck.clone();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset); // Добавляем смещение к базовому календарю
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());

        // Теперь isNumeratorWeek() принимает calendar
        boolean isNumeratorWeek = isNumeratorWeek(calendar);
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
                        lastRowOfCurrentDay = rowNum + 13; // Предполагаемая последняя строка для дня
                    }
                }

                if (foundDaySection) {
                    // Условие для определения конца секции дня
                    if (rowNum > lastRowOfCurrentDay && !currentCellDayValue.startsWith(dayName)) {
                        Log.d(TAG, "parseScheduleForTwoSubgroups: Вышли за пределы дня " + dayName + " (по счетчику строк). Завершаем.");
                        break;
                    }
                    if (!currentCellDayValue.isEmpty() && !currentCellDayValue.startsWith(dayName) && currentCellDayValue.length() > 2) {
                        String nextDayPrefix = getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()).substring(0, Math.min(3, getNextDayName(dayOfWeek).length()));
                        if (currentCellDayValue.startsWith(nextDayPrefix)) {
                            Log.d(DEBUG_PARSE_DETAIL, "Условие остановки дня сработало в строке " + rowNum + " для дня " + dayName);
                            Log.d(DEBUG_PARSE_DETAIL, "Причина: Обнаружен следующий день: '" + currentCellDayValue + "'. Ожидался префикс: '" + nextDayPrefix + "'");
                            break;
                        }
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
                        // Здесь ваша логика парсинга строк для числителя/знаменателя
                        // Убедитесь, что rowIterator.next() для пропуска строки знаменателя
                        // вызывается корректно, чтобы не пропустить реальные данные.

                        // Определяем, является ли текущая строка строкой числителя по наличию времени
                        if (!cellTimeValue.isEmpty()) { // Это строка с данными числителя
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

                            Row nextRowForDenominatorData = null;
                            if (rowIterator.hasNext()) { // Проверяем, есть ли следующая строка
                                nextRowForDenominatorData = rowIterator.next();
                                // Убедимся, что эта строка действительно является строкой знаменателя
                                // (у нее пустое время, но есть данные)
                                Cell potentialNextTimeCell = nextRowForDenominatorData.getCell(1);
                                if (potentialNextTimeCell == null || getStringCellValue(potentialNextTimeCell).trim().isEmpty()) {
                                    subject1Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(3)));
                                    room1Denominator = getStringCellValue(nextRowForDenominatorData.getCell(4));
                                    subject2Denominator = extractSubjectName(getStringCellValue(nextRowForDenominatorData.getCell(5)));
                                    room2Denominator = getStringCellValue(nextRowForDenominatorData.getCell(6));
                                    commonRoomDenominator = getStringCellValue(nextRowForDenominatorData.getCell(7));
                                    Log.d(DEBUG_PARSE_DETAIL, "Пропускаем строку знаменателя: " + nextRowForDenominatorData.getRowNum());
                                }
                            }

                            if (isNumeratorWeek) {
                                if (!commonRoomNumerator.isEmpty()) {
                                    scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                            getNonEmptySubject(subject1Numerator, subject2Numerator),
                                            commonRoomNumerator,
                                            " (совмещенная, числитель)"));
                                } else {
                                    if (!subject1Numerator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Numerator, room1Numerator, " (1 п/г, числитель)"));
                                    }
                                    if (!subject2Numerator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Numerator, room2Numerator, " (2 п/г, числитель)"));
                                    }
                                }
                            } else { // Это знаменатель
                                if (!commonRoomDenominator.isEmpty()) {
                                    scheduleForDay.append(formatScheduleEntry(currentTimeForPair,
                                            getNonEmptySubject(subject1Denominator, subject2Denominator),
                                            commonRoomDenominator,
                                            " (совмещенная, знаменатель)"));
                                } else {
                                    if (!subject1Denominator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject1Denominator, room1Denominator, " (1 п/г, знаменатель)"));
                                    }
                                    if (!subject2Denominator.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(currentTimeForPair, subject2Denominator, room2Denominator, " (2 п/г, знаменатель)"));
                                    }
                                }
                            }
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
    private static String getNonEmptySubject(String sub1, String sub2) {
        if (!sub1.isEmpty()) return sub1;
        if (!sub2.isEmpty()) return sub2;
        return "";
    }
    private static String getNonEmptySubject(String sub1, String sub2, String sub3) {
        if (!sub1.isEmpty()) return sub1;
        if (!sub2.isEmpty()) return sub2;
        if (!sub3.isEmpty()) return sub3;
        return "";
    }
    // Изменяем isNumeratorWeek, чтобы он принимал Calendar
    public static boolean isNumeratorWeek(Calendar calendar) {
        Calendar septemberFirst = Calendar.getInstance();
        septemberFirst.set(calendar.get(Calendar.YEAR), Calendar.SEPTEMBER, 1, 0, 0, 0);
        septemberFirst.set(Calendar.MILLISECOND, 0);

        // Если текущая дата раньше 1 сентября текущего года, значит, отсчет идет от 1 сентября прошлого года
        if (calendar.before(septemberFirst)) {
            septemberFirst.set(calendar.get(Calendar.YEAR) - 1, Calendar.SEPTEMBER, 1, 0, 0, 0);
            septemberFirst.set(Calendar.MILLISECOND, 0);
        }

        long diff = calendar.getTimeInMillis() - septemberFirst.getTimeInMillis();
        long daysSinceSeptemberFirst = diff / (24 * 60 * 60 * 1000);
        long weekNumber = daysSinceSeptemberFirst / 7;

        Log.d(TAG, "isNumeratorWeek: Календарь: " + calendar.getTime() + ", Дней с 1 сентября: " + daysSinceSeptemberFirst + ", Номер недели (от 0): " + weekNumber + ", Числитель: " + (weekNumber % 2 == 0));
        return weekNumber % 2 == 0; // Четные недели (начиная с 0) - числитель
    }


    private static String getNextDayName(int currentDayOfWeek) {
        int nextDayOfWeek = (currentDayOfWeek == Calendar.SATURDAY) ? Calendar.SUNDAY : currentDayOfWeek + 1;
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
        if (subject.isEmpty() && room.isEmpty()) {
            return "";
        }

        StringBuilder entry = new StringBuilder();
        entry.append("Время: ").append(time);
        if (!subgroupInfo.isEmpty()) {
            entry.append(subgroupInfo);
        }
        entry.append("\n");

        entry.append("Предмет: ").append(subject).append("\n");

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
                    java.util.Date date = cell.getDateCellValue();
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
                    return timeFormat.format(date);
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            }
            if (cellType == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
            if (cellType == CellType.FORMULA) {
                try {
                    // Используем getStringCellValue для рекурсивного получения значения
                    return getStringCellValue(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    // Если формула не может быть вычислена, или результат - ошибка
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue.getCellType() == CellType.STRING) {
                        return cellValue.getStringValue().trim();
                    } else if (cellValue.getCellType() == CellType.NUMERIC) {
                        return String.valueOf((int) cellValue.getNumberValue());
                    } else {
                        Log.w(TAG, "getStringCellValue: Ошибка вычисления формулы или неизвестный тип результата: " + cell.getCellFormula() + ", Ошибка: " + e.getMessage());
                        return ""; // Возвращаем пустую строку в случае ошибки
                    }
                }
            }
            return "";
        } catch (Exception e) {
            Log.e(TAG, "getStringCellValue: Ошибка при получении значения ячейки: " + e.getMessage());
            return "";
        }
    }
    // Вспомогательный метод для getStringCellValue, который получает тип кэшированного результата формулы
    private static String getStringCellValue(CellType cellType, Cell cell) {
        if (cellType == CellType.STRING) return cell.getStringCellValue().trim();
        if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
                return timeFormat.format(date);
            } else {
                return String.valueOf((int) cell.getNumericCellValue());
            }
        }
        if (cellType == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return "";
    }
}