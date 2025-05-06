package com.example.vgtu_map;

import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelParser {

    public static String parseScheduleForToday(File file) {
        StringBuilder scheduleToday = new StringBuilder("Расписание на сегодня:\n");
        FileInputStream fis = null;
        Workbook workbook = null;

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String todayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        Log.d("ExcelParser", "Сегодняшний день недели (верхний регистр): " + todayName);

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
                    Row row = rowIterator.next();
                    Cell dayCell = row.getCell(0);

                    if (!foundDay) {
                        if (dayCell != null && dayCell.getCellType() == CellType.STRING) {
                            String cellDay = dayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault());
                            Log.d("ExcelParser", "Найден день недели в файле (верхний регистр): " + cellDay);
                            if (cellDay.startsWith(todayName)) {
                                foundDay = true;
                                Log.d("ExcelParser", "Совпадение найдено! foundDay = " + foundDay);
                            }
                        }
                    } else {
                        Log.d("ExcelParser", "Начало обработки расписания после нахождения дня.");
                        Log.d("ExcelParser", "Обрабатывается строка №: " + row.getRowNum());
                        Cell timeCell = row.getCell(1);
                        Cell subjectInfoCellPg1 = row.getCell(3); // Предмет и препод. для 1 п/г (или общие)
                        Cell subjectInfoCellPg2 = row.getCell(4); // Предмет и препод. для 2 п/г
                        Cell roomCellPg1 = row.getCell(5);       // Аудитория для 1 п/г
                        Cell roomCellCommon = row.getCell(7);    // Общая аудитория
                        Cell roomCellPg2 = row.getCell(8);       // Аудитория для 2 п/г

                        // Проверяем, не начался ли следующий день
                        if (dayCell != null && dayCell.getCellType() == CellType.STRING) {
                            String nextDay = getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault());
                            String cellDay = dayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault());
                            if (cellDay.startsWith(nextDay)) {
                                Log.d("ExcelParser", "Найдено начало следующего дня: " + cellDay + ". Завершаем чтение.");
                                break; // Выходим из цикла, так как расписание на сегодня закончилось
                            }
                        }

                        String startTime = "";
                        String endTime = "";

                        if (timeCell != null && timeCell.getCellType() == CellType.STRING) {
                            String timeValue = timeCell.getStringCellValue().trim();
                            String[] times = timeValue.split(" - ");
                            if (times.length == 2) {
                                startTime = times[0].trim();
                                endTime = times[1].trim();
                            } else {
                                Log.w("ExcelParser", "Неправильный формат времени в строке " + row.getRowNum() + ": " + timeValue);
                            }
                            Log.d("ExcelParser", "startTime: " + startTime + ", endTime: " + endTime);
                        } else {
                            Log.w("ExcelParser", "Ячейка времени пуста или имеет неправильный формат в строке " + row.getRowNum());
                        }

                        // Обработка первой подгруппы (или общих занятий)
                        if (!startTime.isEmpty() && subjectInfoCellPg1 != null && subjectInfoCellPg1.getCellType() == CellType.STRING && !getStringCellValue(subjectInfoCellPg1).isEmpty()) {
                            String subjectWithTeacher = getStringCellValue(subjectInfoCellPg1).trim();
                            String teacher = extractTeacherName(subjectWithTeacher);
                            String subjectOnly = subjectWithTeacher.replaceAll("\\s*\\([^\\)]*\\)\\s*", "");
                            Cell firstGroupRoomCell = row.getCell(4); // Явно указываем индекс 4 для аудитории 1 п/г
                            String room = getStringCellValue(firstGroupRoomCell != null ? firstGroupRoomCell : roomCellCommon); // Используем общую, если нет для подгруппы

                            scheduleToday.append("Время: ").append(startTime).append(" - ").append(endTime);
                            if (subjectInfoCellPg2 != null && subjectInfoCellPg2.getCellType() == CellType.STRING && !getStringCellValue(subjectInfoCellPg2).isEmpty()) {
                                scheduleToday.append(" (1 пг)");
                            }
                            scheduleToday.append("\nПредмет: ").append(subjectOnly).append("\n");
                            scheduleToday.append("Преподаватель: ").append(teacher).append("\n");
                            scheduleToday.append("Аудитория: ").append(room).append("\n\n");
                        }

                        // Обработка второй подгруппы
                        if (!startTime.isEmpty() && subjectInfoCellPg2 != null && subjectInfoCellPg2.getCellType() == CellType.STRING && !getStringCellValue(subjectInfoCellPg2).isEmpty()) {
                            Cell secondGroupSubjectCell = row.getCell(5); // Явно указываем индекс 5 для предмета/преподавателя 2 п/г
                            String subjectWithTeacher = getStringCellValue(secondGroupSubjectCell).trim();
                            String teacher = extractTeacherName(subjectWithTeacher);
                            String subjectOnly = subjectWithTeacher.replaceAll("\\s*\\([^\\)]*\\)\\s*", "");
                            Cell secondGroupRoomCell = row.getCell(7); // Явно указываем индекс 7 для аудитории 2 п/г
                            String room = getStringCellValue(secondGroupRoomCell);

                            scheduleToday.append("Время: ").append(startTime).append(" - ").append(endTime).append(" (2 пг)\n");
                            scheduleToday.append("Предмет: ").append(subjectOnly).append("\n");
                            scheduleToday.append("Преподаватель: ").append(teacher).append("\n");
                            scheduleToday.append("Аудитория: ").append(room).append("\n\n");
                        }

                        if (timeCell == null && subjectInfoCellPg1 == null && subjectInfoCellPg2 == null && roomCellPg1 == null && roomCellCommon == null && roomCellPg2 == null) {
                            Log.d("ExcelParser", "Предполагаемый конец расписания на день.");
                            break;
                        }
                    }
                }

                if (scheduleToday.toString().equals("Расписание на сегодня:\n")) {
                    scheduleToday.append("На сегодня расписаний нет.");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Ошибка при чтении файла расписания.";
        } finally {
            try {
                if (fis != null) fis.close();
                if (workbook != null) workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return scheduleToday.toString();
    }

    private static String getNextDayName(int currentDayOfWeek) {
        int nextDayOfWeek;
        if (currentDayOfWeek == Calendar.SUNDAY) {
            nextDayOfWeek = Calendar.MONDAY;
        } else {
            nextDayOfWeek = currentDayOfWeek + 1;
        }
        return getDayName(nextDayOfWeek);
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    private static String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "Понедельник";
            case Calendar.TUESDAY:
                return "Вторник";
            case Calendar.WEDNESDAY:
                return "Среда";
            case Calendar.THURSDAY:
                return "Четверг";
            case Calendar.FRIDAY:
                return "Пятница";
            case Calendar.SATURDAY:
                return "Суббота";
            case Calendar.SUNDAY:
                return "Воскресенье";
            default:
                return "";
        }
    }

    private static String extractTeacherName(String subjectWithTeacher) {
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(subjectWithTeacher);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}