package com.example.vgtu_map;

import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelParser {

    private static final String TAG = "ExcelParser";
    private static final int[] COLOR_CHISLITEL_RGB = {51, 51, 51}; // Черный
    private static final int[] COLOR_ZNAMENATEL_RGB = {88, 113, 207}; // Синий

    public static String parseScheduleForToday(File file) {
        StringBuilder scheduleToday = new StringBuilder("Расписание на сегодня:\n");
        FileInputStream fis = null;
        Workbook workbook = null;

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String todayName = getDayName(dayOfWeek).toUpperCase(Locale.getDefault());
        boolean isNumeratorWeek = isNumeratorWeek();

        Log.d(TAG, "Сегодня: " + todayName + ", Неделя числителя: " + isNumeratorWeek);

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
                Row currentTimeRow = null;

                while (rowIterator.hasNext()) {
                    Row currentRow = rowIterator.next();
                    Cell dayCell = currentRow.getCell(0);

                    if (!foundDay) {
                        if (dayCell != null && dayCell.getCellType() == CellType.STRING && dayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(todayName)) {
                            foundDay = true;
                            Log.d(TAG, "Найдена строка с днем: " + currentRow.getRowNum());
                        }
                    } else {
                        Cell timeCell = currentRow.getCell(1);
                        if (timeCell != null && timeCell.getCellType() == CellType.STRING && !timeCell.getStringCellValue().trim().isEmpty()) {
                            currentTimeRow = currentRow;
                        } else if (currentTimeRow != null) {
                            Cell timeValueCell = currentTimeRow.getCell(1);
                            String timeValue = getStringCellValue(timeValueCell);
                            String[] times = timeValue.split(" - ");
                            if (times.length == 2) {
                                String startTime = times[0].trim();
                                String endTime = times[1].trim();

                                // --- Первая подгруппа ---
                                Cell subject1Cell = currentRow.getCell(3);
                                String subject1 = extractSubjectName(getStringCellValue(subject1Cell));
                                String room1 = getStringCellValue(currentRow.getCell(4));
                                int[] color1RGB = getFontRGB(workbook, subject1Cell);

                                Log.d(TAG, "Первая подгруппа - Цвет: " + Arrays.toString(color1RGB) + ", Числитель?: " + isNumeratorWeek + ", Предмет?: " + !subject1.isEmpty());

                                if (colorMatches(color1RGB, COLOR_CHISLITEL_RGB) && isNumeratorWeek && !subject1.isEmpty()) {
                                    scheduleToday.append(formatScheduleEntry(startTime, endTime, subject1, room1, " (1 п/г, числитель)"));
                                } else if (colorMatches(color1RGB, COLOR_ZNAMENATEL_RGB) && !isNumeratorWeek && !subject1.isEmpty()) {
                                    scheduleToday.append(formatScheduleEntry(startTime, endTime, subject1, room1, " (1 п/г, знаменатель)"));
                                }

                                // --- Вторая подгруппа ---
                                Cell subject2Cell = currentRow.getCell(5);
                                String subject2 = extractSubjectName(getStringCellValue(subject2Cell));
                                String room2 = getStringCellValue(currentRow.getCell(6));
                                int[] color2RGB = getFontRGB(workbook, subject2Cell);

                                Log.d(TAG, "Вторая подгруппа - Цвет: " + Arrays.toString(color2RGB) + ", Числитель?: " + isNumeratorWeek + ", Предмет?: " + !subject2.isEmpty());

                                if (colorMatches(color2RGB, COLOR_CHISLITEL_RGB) && isNumeratorWeek && !subject2.isEmpty()) {
                                    scheduleToday.append(formatScheduleEntry(startTime, endTime, subject2, room2, " (2 п/г, числитель)"));
                                } else if (colorMatches(color2RGB, COLOR_ZNAMENATEL_RGB) && !isNumeratorWeek && !subject2.isEmpty()) {
                                    scheduleToday.append(formatScheduleEntry(startTime, endTime, subject2, room2, " (2 п/г, знаменатель)"));
                                }

                                currentTimeRow = null;
                            } else {
                                Log.w(TAG, "Неправильный формат времени в строке " + currentTimeRow.getRowNum() + ": " + timeValue);
                                currentTimeRow = null;
                            }
                        }

                        Cell nextDayCell = currentRow.getCell(0);
                        if (nextDayCell != null && nextDayCell.getCellType() == CellType.STRING && nextDayCell.getStringCellValue().trim().toUpperCase(Locale.getDefault()).startsWith(getNextDayName(dayOfWeek).toUpperCase(Locale.getDefault()))) {
                            Log.d(TAG, "Найдено начало следующего дня. Завершаем.");
                            break;
                        }
                    }
                }

                if (scheduleToday.toString().equals("Расписание на сегодня:\n")) {
                    scheduleToday.append("На сегодня расписаний нет.");
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
        return scheduleToday.toString();
    }

    private static boolean isNumeratorWeek() {
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

    private static String getDayName(int dayOfWeek) {
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

    private static boolean colorMatches(int[] rgb1, int[] rgb2) {
        return rgb1 != null && rgb2 != null && Arrays.equals(rgb1, rgb2);
    }

    private static int[] getFontRGB(Workbook workbook, Cell cell) {
        if (cell == null) return null;
        try {
            Font font = workbook.getFontAt(cell.getCellStyle().getFontIndex());
            if (font instanceof XSSFFont) {
                XSSFColor color = ((XSSFFont) font).getXSSFColor();
                if (color != null && color.getRGB() != null) {
                    byte[] rgbBytes = color.getRGB();
                    Log.d(TAG, "RGB цвет (XLSX): " + (rgbBytes[0] & 0xFF) + ", " + (rgbBytes[1] & 0xFF) + ", " + (rgbBytes[2] & 0xFF));
                    return new int[]{(rgbBytes[0] & 0xFF), (rgbBytes[1] & 0xFF), (rgbBytes[2] & 0xFF)};
                }
            } else if (font instanceof HSSFFont && workbook instanceof HSSFWorkbook) {
                HSSFWorkbook hssfWorkbook = (HSSFWorkbook) workbook;
                short colorIndex = ((HSSFFont) font).getColor();
                HSSFPalette palette = hssfWorkbook.getCustomPalette();
                HSSFColor customColor = palette.getColor(colorIndex);
                if (customColor != null) {
                    short[] rgbShort = customColor.getTriplet();
                    return new int[]{(rgbShort[0] & 0xFF), (rgbShort[1] & 0xFF), (rgbShort[2] & 0xFF)};
                } else {
                    // Получаем предопределенные цвета HSSF через константы
                    HSSFColor.HSSFColorPredefined[] predefinedColors = HSSFColor.HSSFColorPredefined.values();
                    for (HSSFColor.HSSFColorPredefined predefinedColor : predefinedColors) {
                        if (predefinedColor.getIndex() == colorIndex) {
                            short[] rgbShort = predefinedColor.getColor().getTriplet();
                            return new int[]{(rgbShort[0] & 0xFF), (rgbShort[1] & 0xFF), (rgbShort[2] & 0xFF)};
                        }
                    }
                    Log.w(TAG, "Не удалось получить RGB цвет для индекса: " + colorIndex + " (.xls)");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Ошибка получения RGB цвета: " + e.getMessage());
        }
        return null;
    }

    private static String formatScheduleEntry(String startTime, String endTime, String subject, String room, String group) {
        return "Время: " + startTime + " - " + endTime + group + "\n" +
                "Предмет: " + subject + "\n" +
                "Аудитория: " + room + "\n\n";
    }
}