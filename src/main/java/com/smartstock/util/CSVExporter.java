package com.smartstock.util;

import com.smartstock.service.ReportService.Report;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class CSVExporter {
  private CSVExporter() {}

  public static void export(Path file, Report report) {
    try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      write(writer, report.columns());
      for (Object[] row : report.rows()) write(writer, row);
    } catch (IOException e) {
      throw new AppException("Cannot export CSV. Choose a writable file location.", e);
    }
  }

  private static void write(BufferedWriter w, Object[] row) throws IOException {
    for (int i = 0; i < row.length; i++) {
      if (i > 0) w.write(',');
      String text = row[i] == null ? "" : row[i].toString();
      // Neutralize spreadsheet formula injection for user-entered text.
      if (text.matches("(?s)^[=+@\\-\\t\\r].*")) text = "'" + text;
      w.write('"');
      w.write(text.replace("\"", "\"\""));
      w.write('"');
    }
    w.newLine();
  }
}
