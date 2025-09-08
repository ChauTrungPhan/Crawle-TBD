package com.example.crawlertbdgemini2modibasicview;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcelXmlExporter {
    private static final int THREAD_COUNT = 4;
    private static final String TAG = "ExcelXmlExporter";
    private static final Logger log = LogManager.getLogger(ExcelXmlExporter.class);

    public static void exportMultiThread(Context context, ExcelExporterOptimized_goc.ExportCallbacks callbacks) {
        SQLiteDatabase db = DBHelperThuoc.getInstance(context).getReadableDatabase();
        CrawlType crawlType = new SettingsRepository(context).getSelectedCrawlType();
        String tableName = crawlType.getTableThuocName();

        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
        countCursor.moveToFirst();
        int totalRows = countCursor.getInt(0);
        countCursor.close();

        if (totalRows == 0) {
            if (callbacks != null) callbacks.onExportFailure("Không có dữ liệu.");
            return;
        }

        int rowsPerThread = totalRows / THREAD_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<File> partFiles = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * rowsPerThread;
            int limit = (i == THREAD_COUNT - 1) ? totalRows - start : rowsPerThread;
            int partIndex = i;

            executor.execute(() -> {
                try {
                    Log.d(TAG, "exportMultiThread: Bắt đầu");
                    exportXmlPart(context, tableName, partIndex, start, limit, partFiles);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi thread " + partIndex + ": " + e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        new Thread(() -> {
            try {
                latch.await();
                File merged = mergeXmlParts(context, partFiles, crawlType);
                if (callbacks != null) callbacks.onExportSuccess(merged.getAbsolutePath());
            } catch (Exception e) {
                if (callbacks != null) callbacks.onExportFailure("Lỗi merge: " + e.getMessage());
            } finally {
                executor.shutdownNow();
            }
        }).start();
    }

    private static void exportXmlPart(Context context, String table, int index, int offset, int limit, List<File> outFiles) throws Exception {
        Log.d(TAG, "exportXmlPart: Danh sách outFiles" + outFiles.toString());
        SQLiteDatabase db = DBHelperThuoc.getInstance(context).getReadableDatabase();
        String query = buildQuery(table) + " LIMIT " + limit + " OFFSET " + offset;
        Cursor cursor = db.rawQuery(query, null);

        File dir = new File(context.getCacheDir(), "xml_parts");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, "part_" + index + ".xml");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        StringBuilder sb = new StringBuilder();

        while (cursor.moveToNext()) {
            // Mỗi vòng lặp sẽ Ghi 1 hàng excel
            sb.append("<Row Color=\"")
                    .append(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelperThuoc.INDEX_COLOR)))
                    .append("\">\n");
            // Lặp cột, ghi giá trị cột cho 1 hàng
            for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
                String val = cursor.getString(cursor.getColumnIndexOrThrow(col.dbColumnName));
                // Bỏ giá tri: "#HangDau"
                if(val.equalsIgnoreCase("#HangDau")){
                    sb.append("  <Cell Header=\"").append(col.headerName).append("\">")
                            .append(escapeXml(null))
                            .append("</Cell>\n");
                } else {
                    sb.append("  <Cell Header=\"").append(col.headerName).append("\">")
                            .append(escapeXml(val))
                            .append("</Cell>\n");
                }
            }

            sb.append("</Row>\n");
        }

        writer.write(sb.toString());
        writer.close();
        cursor.close();
        outFiles.add(outFile);
        Log.d(TAG, "exportXmlPart: Xong outFile" + outFile);
    }

    private static File mergeXmlParts(Context context, List<File> partFiles, CrawlType crawlType) throws Exception {
        File outputDir = new File(context.getExternalFilesDir(null), "Documents");
        if (!outputDir.exists()) outputDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = crawlType.getExcelFileName().replace(".xlsx", "") + "_" + timestamp + ".xml";
        File mergedFile = new File(outputDir, fileName);

        BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile));
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Workbook>\n");

        writer.write("<Title>DANH SÁCH THUỐC BIỆT DƯỢC</Title>\n");
        writer.write("<CreatedAt>" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "</CreatedAt>\n");

        for (File part : partFiles) {
            List<String> lines = Files.readAllLines(part.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                //writer.write(line).append("\n");  // Lỗi
                // Tách thành 2 lệnh
                writer.write(line);
                writer.append("\n");
            }
        }

        writer.write("</Workbook>");
        writer.close();
        Log.d(TAG, "mergeXmlParts: Xong, mergedFile: " + mergedFile.getAbsolutePath());
        return mergedFile;
    }

    private static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String buildQuery(String tableName) {
        StringBuilder sb = new StringBuilder();
        for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
            sb.append(col.dbColumnName).append(", ");
            if (col.dbLinkColumnName != null)
                sb.append(col.dbLinkColumnName).append(", ");
        }
        sb.append(DBHelperThuoc.INDEX_COLOR);
        return "SELECT " + sb.toString().replaceAll(", $", "") + " FROM " + tableName;
    }
}
