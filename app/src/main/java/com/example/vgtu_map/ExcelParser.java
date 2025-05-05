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
        Log.d("ExcelParser", "Сегодняшний день недели (верхний регистр): " + todayName); // Логируем todayName один раз

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
                            Log.d("ExcelParser", "Найден день недели в файле (верхний регистр): " + cellDay); // Логируем cellDay перед сравнением
                            if (cellDay.startsWith(todayName)) {
                                foundDay = true;
                                Log.d("ExcelParser", "Совпадение найдено!");
                            }
                        }
                    } else {
                        Cell startTimeCell = row.getCell(1);
                        Cell endTimeCell = row.getCell(2);
                        Cell subjectInfoCell = row.getCell(3);
                        Cell roomCell = row.getCell(7);

                        if (startTimeCell != null && startTimeCell.getCellType() == CellType.STRING && endTimeCell != null && endTimeCell.getCellType() == CellType.STRING && subjectInfoCell != null && subjectInfoCell.getCellType() == CellType.STRING) {
                            String startTime = startTimeCell.getStringCellValue().trim();
                            String endTime = endTimeCell.getStringCellValue().trim();
                            String subjectWithTeacher = subjectInfoCell.getStringCellValue().trim();
                            String room = (roomCell != null && roomCell.getCellType() == CellType.STRING) ? roomCell.getStringCellValue().trim() : "";

                            String teacher = extractTeacherName(subjectWithTeacher);
                            String subjectOnly = subjectWithTeacher.replaceAll("\\s*\\([^\\)]*\\)\\s*", "");

                            scheduleToday.append("Время: ").append(startTime).append(" - ").append(endTime).append("\n");
                            scheduleToday.append("Предмет: ").append(subjectOnly).append("\n");
                            scheduleToday.append("Преподаватель: ").append(teacher).append("\n");
                            scheduleToday.append("Аудитория: ").append(room).append("\n\n");
                        } else if (startTimeCell == null && endTimeCell == null && subjectInfoCell == null) {
                            // Предполагаем, что пустые ячейки времени и предмета означают конец расписания на этот день
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