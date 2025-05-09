package com.example.vgtu_map;

import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelParser {

    private static final String TAG = "ExcelParser";

    public static String parseScheduleForToday(File file) {
        Log.d(TAG, "--- Начало обработки расписания на сегодня ---");
        return parseScheduleForDay(file, 0); // 0 - смещение для текущего дня
    }

    public static String parseScheduleForTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на завтра ---");
        return parseScheduleForDay(file, 1); // 1 - смещение для следующего дня
    }

    public static String parseScheduleForAfterTomorrow(File file) {
        Log.d(TAG, "--- Начало обработки расписания на послезавтра ---");
        return parseScheduleForDay(file, 2); // 2 - смещение для следующего дня
    }

    public static String parseScheduleForDayOfWeek(File file, int dayOfWeek, boolean isNumeratorWeek, int weekOffset) {
        StringBuilder scheduleForDay = new StringBuilder("Расписание на ");
        FileInputStream fis = null;
        Workbook workbook = null;

        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        scheduleForDay.append(getDayName(dayOfWeek)).append(":\n");

        Log.d(TAG, "Запрос расписания на: " + dayName + ", Неделя числителя: " + isNumeratorWeek + ", Смещение недели: " + weekOffset + ", dayOfWeek: " + dayOfWeek);

        try {
            fis = new FileInputStream(file);
            if (file.getName().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (file.getName().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                boolean foundDaySection = false;

                while (rowIterator.hasNext()) {
                    Row currentRow = rowIterator.next();
                    Cell dayCell = currentRow.getCell(0);

                    if (!foundDaySection) {
                        if (dayCell != null && dayCell.getCellType() == CellType.STRING &&
                                dayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(dayName)) {
                            foundDaySection = true;
                            Log.d(TAG, "Найдена секция для дня: " + dayName + " (строка " + currentRow.getRowNum() + ")");
                        }
                    } else {
                        Cell timeCell = currentRow.getCell(1);
                        if (timeCell != null && timeCell.getCellType() == CellType.STRING && !timeCell.getStringCellValue().trim().isEmpty()) {
                            String timeValue = getStringCellValue(timeCell);
                            String[] times = timeValue.split(" - ");
                            if (times.length == 2) {
                                String startTime = times[0].trim();
                                String endTime = times[1].trim();

                                Cell subject1Cell = currentRow.getCell(3);
                                String subject1 = extractSubjectName(getStringCellValue(subject1Cell));
                                String room1 = getStringCellValue(currentRow.getCell(4));
                                Cell subject2Cell = currentRow.getCell(5);
                                String subject2 = extractSubjectName(getStringCellValue(subject2Cell));
                                String room2 = getStringCellValue(currentRow.getCell(6));
                                String room3 = getStringCellValue(currentRow.getCell(7));

                                boolean isNumeratorRow = room3.isEmpty(); // Предположение, основанное на вашем коде

                                if (isNumeratorWeek == isNumeratorRow) {
                                    if (!subject1.isEmpty() && isNumeratorRow) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1, room1, " (1 п/г)"));
                                    } else if (!subject1.isEmpty() && !isNumeratorRow) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1, room3, ""));
                                    }
                                    if (!subject2.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject2, room2, " (2 п/г)"));
                                    }
                                }
                            }
                        } else if (currentRow.getCell(0) != null && currentRow.getCell(0).getCellType() == CellType.STRING &&
                                currentRow.getCell(0).getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(getNextDayNameForParser(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                            Log.d(TAG, "Конец секции для дня: " + dayName);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка чтения файла: " + e.getMessage());
            return "Ошибка при чтении файла расписания.";
        } finally {
            try {
                if (fis != null) fis.close();
                if (workbook != null) workbook.close();
            } catch (IOException e) {
                Log.e(TAG, "Ошибка закрытия файла: " + e.getMessage());
            }
        }

        if (scheduleForDay.toString().equals("Расписание на " + getDayName(dayOfWeek) + ":\n")) {
            return "На этот день расписаний нет.";
        }

        return scheduleForDay.toString();
    }


    public static String parseScheduleForDay(File file, int dayOffset) {
        StringBuilder scheduleForDay = new StringBuilder();
        FileInputStream fis = null;
        Workbook workbook = null;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, dayOffset); // Прибавляем смещение к текущему дню
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        boolean isNumeratorWeek = isNumeratorWeek();

        Log.d(TAG, "Выбранный день: " + dayName + ", Неделя числителя: " + isNumeratorWeek + ", Смещение: " + dayOffset);

        try {
            fis = new FileInputStream(file);
            if (file.getName().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (file.getName().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                boolean foundDay = false;

                while (rowIterator.hasNext()) {
                    Row currentRow = rowIterator.next();
                    Cell dayCell = currentRow.getCell(0);

                    if (!foundDay) {
                        if (dayCell != null && dayCell.getCellType() == CellType.STRING && dayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(dayName)) {
                            foundDay = true;
                            Log.d(TAG, "Найдена строка с днем: " + currentRow.getRowNum() + " (" + dayName + ")");
                        }
                    } else if (foundDay) {
                        Log.d(TAG, "Обрабатываем строку " + currentRow.getRowNum());
                        Cell timeCell = currentRow.getCell(1);
                        if (timeCell != null && timeCell.getCellType() == CellType.STRING && !timeCell.getStringCellValue().trim().isEmpty()) {
                            String timeValue = getStringCellValue(timeCell);
                            String[] times = timeValue.split(" - ");
                            if (times.length == 2) {
                                String startTime = times[0].trim();
                                String endTime = times[1].trim();
                                Log.d(TAG, "Время: " + startTime + " - " + endTime);

                                // Обработка текущей строки
                                Cell subject1Cell = currentRow.getCell(3);
                                String subject1 = extractSubjectName(getStringCellValue(subject1Cell));
                                String room1 = getStringCellValue(currentRow.getCell(4));
                                Cell subject2Cell = currentRow.getCell(5);
                                String subject2 = extractSubjectName(getStringCellValue(subject2Cell));
                                String room2 = getStringCellValue(currentRow.getCell(6));
                                String room3 = getStringCellValue(currentRow.getCell(7));

                                if (isNumeratorWeek) {
                                    Log.d(TAG, "Проверка числителя/знаменателя (текущая строка " + currentRow.getRowNum() + "): Неделя числителя - " + isNumeratorWeek + ", Предмет 1 - " + !subject1.isEmpty() + ", Предмет 2 - " + !subject2.isEmpty());
                                    if (!subject1.isEmpty() && room3.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1, room1, " (1 п/г, числитель)"));
                                    }
                                    if (!subject1.isEmpty() && !room3.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1, room3, " (числитель)"));
                                    }
                                    if (!subject2.isEmpty()) {
                                        scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject2, room2, " (2 п/г, числитель)"));
                                    }
                                } else {
                                    // Проверяем следующую строку ДЛЯ ТОЙ ЖЕ ВРЕМЕННОЙ ПАРЫ, НО ДЛЯ ДРУГОЙ НЕДЕЛИ
                                    if (rowIterator.hasNext()) {
                                        Row nextRow = rowIterator.next();
                                        Cell nextTimeCell = nextRow.getCell(1);

                                        // ЕСЛИ В СЛЕДУЮЩЕЙ СТРОКЕ ВРЕМЯ НЕ УКАЗАНО, ЗНАЧИТ ЭТО РАСПИСАНИЕ НА ДРУГУЮ НЕДЕЛЮ
                                        if (nextTimeCell == null || nextTimeCell.getCellType() == CellType.BLANK || getStringCellValue(nextTimeCell).trim().isEmpty()) {
                                            Log.d(TAG, "Следующая строка (" + nextRow.getRowNum() + ") - нет времени. Обрабатываем как другую неделю для той же пары.");
                                            Cell subject1NextRowCell = nextRow.getCell(3);
                                            String subject1NextRow = extractSubjectName(getStringCellValue(subject1NextRowCell));
                                            String room1NextRow = getStringCellValue(nextRow.getCell(4));
                                            Cell subject2NextRowCell = nextRow.getCell(5);
                                            String subject2NextRow = extractSubjectName(getStringCellValue(subject2NextRowCell));
                                            String room2NextRow = getStringCellValue(nextRow.getCell(6));
                                            String room3NextRow = getStringCellValue(nextRow.getCell(7));

                                            if (!subject1NextRow.isEmpty() && room3NextRow.isEmpty()) {
                                                scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1NextRow, room1NextRow, " (1 п/г, знаменатель)"));
                                            }
                                            if (!subject2NextRow.isEmpty()) {
                                                scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject2NextRow, room2NextRow, " (2 п/г, знаменатель)"));
                                            }
                                            if (!subject1NextRow.isEmpty() && !room3NextRow.isEmpty()) {
                                                scheduleForDay.append(formatScheduleEntry(startTime, endTime, subject1NextRow, room3NextRow, " (знаменатель)"));
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.w(TAG, "Неправильный формат времени в строке " + currentRow.getRowNum() + ": " + timeValue);
                            }
                        }

                        // Проверка на начало следующего дня (с учетом смещения, чтобы не выйти за пределы нужного дня)
                        Calendar nextDayCalendar = Calendar.getInstance();
                        nextDayCalendar.add(Calendar.DAY_OF_WEEK, dayOffset);
                        int currentDayForCheck = nextDayCalendar.get(Calendar.DAY_OF_WEEK);

                        Cell nextDayCellForBreak = currentRow.getCell(0);
                        if (nextDayCellForBreak != null && nextDayCellForBreak.getCellType() == CellType.STRING && nextDayCellForBreak.getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(getNextDayName(currentDayForCheck).toUpperCase(Locale.getDefault()))) {
                            Log.d(TAG, "Найдено начало следующего дня. Завершаем для " + dayName);
                            break;
                        }
                    }
                }

                if (scheduleForDay.toString().equals("Расписание на выбранный день:\n")) {
                    scheduleForDay.append("На этот день расписаний нет.");
                }

            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка чтения файла: " + e.getMessage());
            return "Ошибка при чтении файла расписания.";
        } finally {
            try {
                if (fis != null) fis.close();
                if (workbook != null) workbook.close();
            } catch (IOException e) {
                Log.e(TAG, "Ошибка закрытия файла: " + e.getMessage());
            }
        }
        Log.d(TAG, "--- Конец обработки расписания на " + dayName + " ---");
        Log.d(TAG, "Итоговое расписание на " + dayName + ":\n" + scheduleForDay.toString());
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
        int nextDayOfWeek = (currentDayOfWeek == Calendar.SUNDAY) ? Calendar.MONDAY : currentDayOfWeek + 1;
        return getDayName(nextDayOfWeek);
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) return cell.getStringCellValue().trim();
        if (cellType == CellType.NUMERIC) return String.valueOf((int) cell.getNumericCellValue());
        return "";
    }
    


    public static String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case Calendar.MONDAY -> "Понедельник";
            case Calendar.TUESDAY -> "Вторник";
            case Calendar.WEDNESDAY -> "Среда";
            case Calendar.THURSDAY -> "Четверг";
            case Calendar.FRIDAY -> "Пятница";
            case Calendar.SATURDAY -> "Суббота";
            case Calendar.SUNDAY -> "Воскресенье";
            default -> "";
        };
    }

    private static String extractSubjectName(String subjectWithTeacher) {
        Matcher matcher = Pattern.compile("^([^\\(]+)").matcher(subjectWithTeacher.trim());
        return matcher.find() ? matcher.group(1).trim() : subjectWithTeacher.trim();
    }

    private static String formatScheduleEntry(String startTime, String endTime, String subject, String room, String group) {
        return "Время: " + startTime + " - " + endTime + group + "\n" +
                "Предмет: " + subject + "\n" +
                "Аудитория: " + room + "\n\n";
    }

    private static String getNextDayNameForParser(int dayOfWeek) {
        return getDayName(dayOfWeek % 7 + 1); // Cyclic next day (Sun -> Mon)
    }
}