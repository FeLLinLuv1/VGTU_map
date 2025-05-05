package com.example.vgtu_map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelParser {

    public static List<List<String>> parseSchedule(File file) {
        List<List<String>> scheduleData = new ArrayList<>();
        FileInputStream fis = null;
        Workbook workbook = null;

        try {
            fis = new FileInputStream(file);
            if (file.getName().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (file.getName().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0); // Предполагаем, что данные на первом листе

                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    List<String> rowData = new ArrayList<>();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        switch (cell.getCellType()) {
                            case STRING:
                                rowData.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowData.add(cell.getDateCellValue().toString());
                                } else {
                                    rowData.add(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            case BOOLEAN:
                                rowData.add(String.valueOf(cell.getBooleanCellValue()));
                                break;
                            case BLANK:
                                rowData.add("");
                                break;
                            default:
                                rowData.add("");
                        }
                    }
                    scheduleData.add(rowData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Обработка ошибки чтения файла
        } finally {
            try {
                if (fis != null) fis.close();
                if (workbook != null) workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return scheduleData;
    }
}