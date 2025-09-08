package com.example.crawlertbdgemini2modibasicview;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

//import com.example.crawlertbdgemini2modibasicview.models.CrawlType;
//import com.example.crawlertbdgemini2modibasicview.utils.FileUtils;
//import com.example.crawlertbdgemini2modibasicview.utils.Utils;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;
import com.example.crawlertbdgemini2modibasicview.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExcelExporterOptimized {

    private static final String TAG = "ExcelExporterOptimized";
    private static ThreadPoolExecutor excelExecutor;
    private static Context applicationContext;

    // Định nghĩa các STYLE_INDEX từ styles.xml của bạn
//    "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n" +                                  // 0 - default
//    "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"1\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 1 - Nền yellow + Chữ đen mặc định
//    "        <xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 2 - Nền red + Chữ trắng
//    "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 3 - Nền green + Chữ đen
//    "        <xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 4 - Nền trong suốt(TITLE) + Chữ Xanh Blue, ĐẬM
//    "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"4\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM
//    "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 6 - Nền CAM ORANGE(LỖI) + Chữ TRẮNG, ĐẬM: Cho lỗi err
//    // Hyperlink + nền
//    "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 7 - hyperlink default
    private static final int STYLE_INDEX_NORMAL = 0;
    private static final int STYLE_INDEX_YELLOW = 1;
    // TITLE + HEADER
    private static final int STYLE_INDEX_TITLE = 4;
    private static final int STYLE_INDEX_HEADER = 5;

    private static final int STYLE_INDEX_DATE = 6;


    // hyperlink + nền
    private static final int STYLE_INDEX_HYPER_BOLD = 5;
    private static final int STYLE_INDEX_HYPERLINK_NORMAL = 7;
    private static final int STYLE_INDEX_HYPER_YELLOW = 7;

    // Giới hạn số dòng trên mỗi sheet Excel (5.000 dòng như yêu cầu)
    private static final int ROWS_PER_PAGINATION_SHEET = 5000;
    // Kích thước lô dữ liệu đọc từ DB mỗi lần
    private static final int BATCH_SIZE = 1000;

    public ExcelExporterOptimized(ThreadPoolExecutor executor, Context context) {
        excelExecutor = executor;
        applicationContext = context.getApplicationContext();
    }

    public interface ExportCallbacks {
        void onExportStarted();
        void onExportProgress(int percent);
        void onExportRecordsMaThuoc(String recordsOnTotalRecords, String mThuoc);
        void onMessageLog(String message);
        void onExportSuccess(String filePath);
        void onExportFailure(String errorMessage);
    }

    /**
     * Phương thức chính để bắt đầu quá trình xuất Excel.
     * Quản lý việc mở và đóng kết nối cơ sở dữ liệu.
     *
     * @param isXmlExport Cờ chỉ định có xuất XML hay không.
     * @param context Context của ứng dụng.
     * @param tableThuoc Tên bảng cần xuất.
     * @param excelFileName Tên file Excel.
     * @param nThread Số luồng (hiện tại không dùng cho XML).
     * @param callbacks Callback để báo cáo trạng thái.
     */
    public static void exportExcelSmart(boolean isXmlExport, Context context, String tableThuoc, String excelFileName, int nThread, ExportCallbacks callbacks) {
        SQLiteDatabase db = null;
        Cursor countCursor = null;

        try {
            // Mở kết nối cơ sở dữ liệu MỘT LẦN duy nhất cho toàn bộ quá trình xuất
            db = DBHelperThuoc.getInstance(context).getReadableDatabase();
            Log.d(TAG, "Database opened in exportExcelSmart.");

            // Đếm tổng số hàng (Sử dụng Cursor cục bộ và đóng ngay)
            countCursor = db.rawQuery("SELECT COUNT(*) FROM " + tableThuoc, null);
            countCursor.moveToFirst();
            int totalRecordsInDb = countCursor.getInt(0);
            countCursor.close();
            Log.d(TAG, "Total records in DB: " + totalRecordsInDb);

            if (callbacks != null) callbacks.onExportStarted();
            
            // BẮT ĐẦU XUẤT exportToExcelXML TẠO FILE EXCEL
            if (isXmlExport) {
                // Truyền đối tượng db đã mở xuống phương thức con
                exportToExcelXML(db, context, tableThuoc, excelFileName, totalRecordsInDb, callbacks);
            } else {
                boolean useStreaming = totalRecordsInDb > 10000;
                exportDataToExcel(db, context, callbacks, useStreaming);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in exportExcelSmart: " + e.getMessage(), e);
            if (callbacks != null) {
                callbacks.onExportFailure("Lỗi xuất Excel: " + e.getMessage());
            }
        } finally {
            // Đảm bảo đóng kết nối cơ sở dữ liệu khi toàn bộ quá trình xuất hoàn tất
            if (db != null && db.isOpen()) {
                db.close();
                Log.d(TAG, "SQLiteDatabase closed in exportExcelSmart.");
            }
        }
    }

    /**
     * Phương thức xuất dữ liệu vào file Excel (XML/ZIP).
     * Nhận đối tượng SQLiteDatabase đã mở từ phương thức gọi.
     *
     * @param db Đối tượng SQLiteDatabase đã được mở.
     * @param context Context của ứng dụng.
     * @param tableThuoc Tên bảng cần xuất.
     * @param excelFileName Tên file Excel.
     * @param totalRecordsInDb Tổng số bản ghi trong DB (để tính tiến độ).
     * @param callbacks Callback để báo cáo trạng thái.
     */
    public static void exportToExcelXML(SQLiteDatabase db, Context context, String tableThuoc, String excelFileName, int totalRecordsInDb, ExportCallbacks callbacks) {
        try {
            File exportFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), excelFileName);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exportFile));

            writeContentTypes(zos);
            writeStyles(zos);
            writeAppXml(zos);
            writeCoreXml(zos);

            // Truyền đối tượng db và tổng số bản ghi xuống writeSheetData
            // Cũ: Cha+con Chung 1 bảng thuóc
            //int totalSheets = writeSheetData(db, context, tableThuoc, zos, totalRecordsInDb, callbacks);
            //Tách Riêng theo quan hệ: Cha+con
            int totalSheets = writeSheetData_Cha_Con(db, context, tableThuoc, zos, totalRecordsInDb, callbacks);

            writeWorkbook(zos, totalSheets);
            writeWorkbookRels(zos, totalSheets);
            writeRels(zos);

            zos.close();
            callbacks.onExportSuccess(exportFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "exportToExcelXML: Error during export: " + e.getMessage(), e);
            callbacks.onExportFailure("Lỗi export Excel: " + e.getMessage());
        }
    }

    /** CHẠY THEO QUAN HỆ CHA CON
     * Phương thức ghi dữ liệu vào sheet và shared strings, hỗ trợ phân trang và xử lý theo lô.
     * Nhận đối tượng SQLiteDatabase đã mở từ phương thức gọi.
     *
     * @param db Đối tượng SQLiteDatabase đã được mở.
     * @param context Context của ứng dụng.
     * @param tableThuoc Tên bảng cần xuất.
     * @param zos ZipOutputStream để ghi dữ liệu.
     * @param totalRecordsInDb Tổng số bản ghi trong DB (để tính tiến độ).
     * @param callbacks Callback để báo cáo trạng thái.
     * @return Số lượng sheet đã ghi.
     */
    public static int writeSheetData_Cha_Con(SQLiteDatabase db, Context context,
                                     String tableThuoc, ZipOutputStream zos, int totalRecordsInDb, ExportCallbacks callbacks) throws Exception {

        // DÙNG CrawlType để lấy thông số:
        CrawlType crawlType = new SettingsRepository(context.getApplicationContext()).getSelectedCrawlType();
        String tableThuocCon = crawlType.getTableThuocName();   // Alias: Bảng con
        String tableUrlsParent;                                 // Alias: Bảng Cha
        if (crawlType.getSettingIdName().endsWith("2kt")) {
            tableUrlsParent = DBHelperThuoc.TABLE_PARENT_URLS_2KT;

        } else if (crawlType.getSettingIdName().endsWith("3kt")) {
            tableUrlsParent = DBHelperThuoc.TABLE_PARENT_URLS_3KT;
        } else {
            tableUrlsParent = DBHelperThuoc.TABLE_PARENT_URLS_CUSTOM;
        }

        // Các cấu trúc dữ liệu toàn cục cho quá trình xuất (chia sẻ giữa các sheet)
        Map<String, Integer> sharedStringsMap = new HashMap<>();
        List<String> sharedStringList = new ArrayList<>();

        // Các cấu trúc dữ liệu cục bộ cho từng sheet
        StringBuilder sheetContent = new StringBuilder();
        List<String> hyperlinks = new ArrayList<>();
        List<String> hyperlinkCellRefs = new ArrayList<>();
        List<String> mergeCells = new ArrayList<>();

        int currentSheetIndex = 1; // Chỉ số của sheet hiện tại (1-indexed)
        int currentRowInSheet = 1; // Số dòng hiện tại trong sheet (1-indexed, bao gồm cả headers)
        int totalExportedRecords = 0; // Tổng số bản ghi đã xuất ra (chỉ đếm data rows)
        int lastPercent = 0;

        // Lấy MIN và MAX date một lần duy nhất
        String minDate = getMinOrMaxDate(db, tableUrlsParent, "MIN");
        String maxDate = getMinOrMaxDate(db, tableUrlsParent, "MAX");
        Log.d(TAG, "Min Date: " + minDate + ", Max Date: " + maxDate);
        //
        // Chuẩn bị danh sách tất cả các cột cần truy vấn
        List<String> allColumns = new ArrayList<>();
        for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
            allColumns.add(col.dbColumnName);
            if (col.dbLinkColumnName != null) allColumns.add(col.dbLinkColumnName);
        }
        allColumns.add(DBHelperThuoc.INDEX_COLOR);
        String queryCols = TextUtils.join(", ", allColumns);

        // --- Khởi tạo sheet đầu tiên với tiêu đề và header ---
        initNewSheetXml(sheetContent, 1);   //Sheet1: active
        //sheetView(sheetContent, 1);   //Sheet1: active
        // writeInitialSheetHeaders: tạo 5 hàng. // Từ A1 đên dòng tiêu đề (A5): 5 dòng
        writeInitialSheetHeaders(sheetContent, sharedStringsMap, sharedStringList, mergeCells, minDate, maxDate);
        currentRowInSheet = 6; // Ghi record bắt đầu là 5: Sau tiêu đề, ngày tạo, ngày cập nhật, header (4 dòng)
        int lastUrlId = -1;

        // === Vòng lặp chính để đọc dữ liệu theo lô và ghi vào sheet ===
        int offset = 0;
        // Khai báo Map để lưu trữ chỉ mục cột MỘT LẦN DUY NHẤT
        Map<String, Integer> cursorColumnIndices = new HashMap<>();
        int colorIndex = -1; // Khởi tạo với giá trị không hợp lệ
        int rawColor = 0;

        // Truy vấn dữ liệu kết hợp bảng cha và bảng con (LEFT JOIN)
        //DBHelperThuoc.URL_ID = url_id
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT\n")
        //CHA
        .append("p").append(".").append(DBHelperThuoc.ID).append(" AS url_id,\n")    // CẦN
        //.append("p").append("." + DBHelperThuoc.PARENT_ID + " AS p_parent_id,\n")
        .append("p").append("." + DBHelperThuoc.PARENT_ID + " AS parent_id,\n") // NOT NULL

        //.append("p").append("." + DBHelperThuoc.LEVEL + " AS p_level,\n")
        .append("p").append("." + DBHelperThuoc.LEVEL + ",\n")                  // NOT NULL
        //.append("p").append("." + DBHelperThuoc.URL_P + " AS url_p,\n") // link
        .append("p").append("." + DBHelperThuoc.URL + " AS " + DBHelperThuoc.URL_P + ",\n")    // link cha: KHÔNG THỂ NULL
                .append("p").append("." + DBHelperThuoc.MA_THUOC + " AS " + DBHelperThuoc.MA_THUOC_P + ",\n")   // Giả: lấy từ url

        //.append("p").append("." + DBHelperThuoc.GHI_CHU + " AS p_ghi_chu,\n")
        .append("p").append("." + DBHelperThuoc.GHI_CHU + ",\n")
        //.append("p").append("." + DBHelperThuoc.INDEX_COLOR + " AS p_index_color,\n")
        .append("p").append("." + DBHelperThuoc.INDEX_COLOR + ",\n")
        //.append("p").append("." + DBHelperThuoc.LAST_UPDATED + " AS p_last_updated,\n");
        .append("p").append("." + DBHelperThuoc.LAST_UPDATED + ",\n");

        //CON
            // VẪN LẤY PARENT_ID, LEVEL VÌ MỤC ĐÍCH XẮP XẾP EXCEL: NẾU BỎ SẼ LẤY GIÁ TRỊ TỪ BẢNG CHA
        //sb.append("c").append(".").append(DBHelperThuoc.PARENT_ID).append(",\n");   // CÓ THỂ KHÔNG CẦN VÌ CHA ĐẪ CÓ, SẼ CẤU HÌNH LẠI TABLE THUOC CON
        //sb.append("c").append("." + DBHelperThuoc.LEVEL + ",\n");           // CÓ THỂ KHÔNG CẦN VÌ CHA ĐẪ CÓ, SẼ CẤU HÌNH LẠI TABLE THUOC CON
        //sb.append("c").append("." + DBHelperThuoc.KY_TU_SEARCH + ",\n");    // LẤY TỪ URL
        sb.append("c").append("." + DBHelperThuoc.MA_THUOC + ",\n");
        sb.append("c").append("." + DBHelperThuoc.MA_THUOC_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.TEN_THUOC + ",\n");
        sb.append("c").append("." + DBHelperThuoc.THANH_PHAN + ",\n");
        sb.append("c").append("." + DBHelperThuoc.THANH_PHAN_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.NHOM_THUOC + ",\n");
        sb.append("c").append("." + DBHelperThuoc.NHOM_THUOC_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.DANG_THUOC + ",\n");
        sb.append("c").append("." + DBHelperThuoc.DANG_THUOC_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.SAN_XUAT + ",\n");
        sb.append("c").append("." + DBHelperThuoc.SAN_XUAT_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.DANG_KY + ",\n");
        sb.append("c").append("." + DBHelperThuoc.DANG_KY_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.PHAN_PHOI + ",\n");
        sb.append("c").append("." + DBHelperThuoc.PHAN_PHOI_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.SDK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.SDK_LINK + ",\n");
        sb.append("c").append("." + DBHelperThuoc.CAC_THUOC + ",\n");
        sb.append("c").append("." + DBHelperThuoc.CAC_THUOC_LINK + "\n");
        //sb.append("c").append("." + DBHelperThuoc.GHI_CHU + ",\n"); // CÓ THỂ KHÔNG CẦN VÌ CHA ĐẪ CÓ, SẼ CẤU HÌNH LẠI TABLE THUOC CON
        //sb.append("c").append("." + DBHelperThuoc.INDEX_COLOR + "\n");

        sb.append("FROM ").append(tableUrlsParent).append(" p\n"); //p là alias của  tableUrlsParent
        sb.append("LEFT JOIN ").append(tableThuocCon).append(" c\n")            //c là alias của tableThuocCon
                .append(" ON ").append("p").append(".id = ")     // alias parent_id=id cha
                .append("c").append(".url_id \n");
        sb.append("ORDER BY ").append("p").append(".parent_id ASC, ")
                .append("p").append(".level ASC \n")
                .append("LIMIT ? OFFSET ?");

        String batchQuery = sb.toString();
        // Tạo bảng mới từ kết quả JOIN: XEM XONG RỒI BỎ
        // Xoá nếu bảng new_table đã tồn tại
        db.execSQL("DROP TABLE IF EXISTS new_table");

//        String createNewTable = "CREATE TABLE new_table AS " + batchQuery.replaceAll("\\s*LIMIT.*$", "");
//        db.execSQL(createNewTable);
        ///

        while (true) {
            Cursor cursor = null;
            try {
                // Truy vấn dữ liệu theo lô
//                String batchQuery = "SELECT " + queryCols + " FROM " + tableThuoc +
//                        " ORDER BY " + DBHelperThuoc.PARENT_ID + ", " + DBHelperThuoc.LEVEL +
//                        " LIMIT " + BATCH_SIZE + " OFFSET " + offset;

                //String batchQuery = // Sử dụng đối số cho LIMIT và OFFSET
                cursor = db.rawQuery(batchQuery, new String[]{String.valueOf(BATCH_SIZE), String.valueOf(offset)});

                Log.d(TAG, "Executing batch query: " + batchQuery);

                if (cursor.getCount() == 0) {
                    Log.d(TAG, "No more records in batch. Breaking loop.");
                    break; // Hết dữ liệu
                }

                // Lấy chỉ mục cột từ Cursor CHỈ MỘT LẦN (khi xử lý lô đầu tiên)
                if (cursorColumnIndices.isEmpty()) { // Kiểm tra nếu chưa được khởi tạo
                    for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
                        cursorColumnIndices.put(col.dbColumnName, cursor.getColumnIndex(col.dbColumnName));
                        if (col.dbLinkColumnName != null) {
                            cursorColumnIndices.put(col.dbLinkColumnName, cursor.getColumnIndex(col.dbLinkColumnName));
                        }
                    }
                    colorIndex = cursor.getColumnIndex(DBHelperThuoc.INDEX_COLOR);  // Lấy theo Tham chiếu bảng CHA
                    Log.d(TAG, "Column indices initialized for the first batch.");
                }

                int colorIndexCha = -1; // Khởi tạo với giá trị không hợp lệ
                //colorIndexCha = cursor.getColumnIndex(p_ALIAS_INDEX_COLOR);  // Lấy theo Tham chiếu bảng CHA
                colorIndexCha = cursor.getColumnIndex(DBHelperThuoc.INDEX_COLOR);  // Lấy theo Tham chiếu bảng CHA
                // Duyệt qua từng record trong lô hiện tại
                if (cursor.moveToFirst()) {
                    do {
                        // Kiểm tra xem có cần tạo sheet mới không (phân trang)
                        // currentRowInSheet - 1 để loại bỏ 3 dòng header cố định và tính toán cho dòng dữ liệu
                        if ((currentRowInSheet - 1) % ROWS_PER_PAGINATION_SHEET == 0 && currentRowInSheet > 1) {
                            Log.d(TAG, "Max rows reached for sheet " + currentSheetIndex + ". Finalizing sheet.");
                            // ✅ KẾT THÚC SHEET HIỆN TẠI VÀ GHI VÀO ZIP
                            writeCurrentSheetToZip(zos, sheetContent, hyperlinks, hyperlinkCellRefs, currentSheetIndex, mergeCells);
                            currentSheetIndex++; // Tăng chỉ số sheet

                            // Reset các biến cho sheet mới
                            hyperlinks.clear();
                            hyperlinkCellRefs.clear();
                            mergeCells.clear();
                            sheetContent.setLength(0);

                            // ✅ Khởi tạo sheet mới với tiêu đề và header
                            // FREEZE SHEET KHÔNG ACTIVE:
                            initNewSheetXml(sheetContent, -1);  //-1 hoặc currentSheetIndex
                            //sheetView(sheetContent, -1);   //-1 hoặc currentSheetIndex
                            // writeInitialSheetHeaders:  Từ A1 đên dòng tiêu đề (A5): 5 dòng
                            writeInitialSheetHeaders(sheetContent, sharedStringsMap, sharedStringList, mergeCells, minDate, maxDate); // Chiếm 4 Row (0-4)
                            currentRowInSheet = 6; // Sau tiêu đề, ngày tạo, ngày cập nhật, header (5 dòng)

                            Log.d(TAG, "New sheet " + currentSheetIndex + " initialized with headers.");
                        }

                        // Lấy dữ liệu từ kết quả JOIN
                        // Các thông số từ cột cha
                        int urlId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelperThuoc.URL_ID));
                        //String url = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.URL_P));
                        //String url = cursor.getString(cursor.getColumnIndexOrThrow(p_ALIAS_URL));
                        //String createdAt = cursor.getString(cursor.getColumnIndex("created_at"));
                        String errMessage = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.GHI_CHU));
                        //String errMessage = cursor.getString(cursor.getColumnIndexOrThrow(p_ALIAS_GHI_CHU));
                        rawColor = cursor.isNull(colorIndexCha) ? 0 : cursor.getInt(colorIndexCha);

                        // Nếu là dòng đầu tiên của một URL mới, ghi thông tin bảng cha
                        if (urlId != lastUrlId) {
                            // Ghi thông tin bảng cha
                            // Ghi dữ liệu của hàng hiện tại vào sheetContent
                            writeRowCha(sheetContent, cursor, currentRowInSheet, DBHelperThuoc.EXPORT_COLUMNS_CHA,
                                    sharedStringsMap, sharedStringList, hyperlinks, hyperlinkCellRefs, colorIndexCha);

//                            writeRow(sheetContent, cursor, currentRowInSheet, DBHelperThuoc.EXPORT_COLUMNS,
//                                    sharedStringsMap, sharedStringList, hyperlinks, hyperlinkCellRefs, rawColor, cursorColumnIndices);

//                            tableUrlsParent + "." + DBHelperThuoc.ID + " AS url_id, " +
//                                    tableUrlsParent + "." + DBHelperThuoc.PARENT_ID +", " +
//                                    tableUrlsParent + "." + DBHelperThuoc.LEVEL +", " +
//                                    tableUrlsParent + "." + DBHelperThuoc.URL +", " +
//                                    tableUrlsParent + "." + DBHelperThuoc.GHI_CHU +", " +
//                                    tableUrlsParent + "." + DBHelperThuoc.INDEX_COLOR +", " +
//                                    tableUrlsParent + "." + DBHelperThuoc.LAST_UPDATED +", " +
                            currentRowInSheet++; // Tăng 1 dòng CHO HÀNG SAU trong sheet
                        }
                        // Ghi dữ liệu của hàng hiện tại (con) vào sheetContent: rawColor= 0: CÁC HÀNG CON KHÔNG TÔ MÀU
                        writeRow(sheetContent, cursor, currentRowInSheet, DBHelperThuoc.EXPORT_COLUMNS,
                                sharedStringsMap, sharedStringList, hyperlinks, hyperlinkCellRefs, colorIndex, cursorColumnIndices);
                        currentRowInSheet++; // Tăng 1 dòng CHO HÀNG SAU trong sheet
                        totalExportedRecords++; // Tăng tổng số bản ghi đã xuất

                        // Cập nhật tiến độ
                        int percent = (int) (totalExportedRecords * 100.0 / (totalRecordsInDb == 0 ? 1 : totalRecordsInDb));
                        if (callbacks != null && percent > lastPercent) {
                            callbacks.onExportProgress(percent);
                            lastPercent = percent;
                        }

                        lastUrlId = urlId;  // Cập nhật lastUrlId để theo dõi URL mới

                    } while (cursor.moveToNext());
                }

                offset += BATCH_SIZE; // Tăng offset cho lô tiếp theo

            } catch (Exception e) {
                Log.e(TAG, "❌ Error processing batch at offset " + offset + ": " + e.getMessage(), e);
                throw e; // Ném lại để bắt ở cấp trên
            } finally {
                if (cursor != null) {
                    cursor.close(); // Đóng Cursor sau mỗi lô
                    Log.d(TAG, "Cursor for batch at offset " + offset + " closed.");
                }
            }
        } // End while loop for batches

        // ✅ Ghi sheet cuối cùng (sau khi tất cả các lô đã được xử lý)
        // Chỉ ghi nếu có dữ liệu trong sheet cuối cùng hoặc nếu đây là sheet đầu tiên và không có record nào
        if (totalExportedRecords > 0 || currentSheetIndex == 1) {
            Log.d(TAG, "Finalizing last sheet " + currentSheetIndex + ".");
            writeCurrentSheetToZip(zos, sheetContent, hyperlinks, hyperlinkCellRefs, currentSheetIndex, mergeCells);
        } else {
            Log.d(TAG, "No records exported, and no initial sheet written. Skipping final sheet write.");
        }


        // ✅ Ghi sharedStrings.xml (sau khi tất cả các sheet đã được xử lý)
        if (!sharedStringList.isEmpty()) {
            StringBuilder shared = new StringBuilder();
            shared.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            shared.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                    .append(sharedStringList.size()).append("\" uniqueCount=\"").append(sharedStringList.size()).append("\">\n");
            for (String s : sharedStringList)
                shared.append("<si><t>").append(s).append("</t></si>\n");    // escapeXml(s) đã escapeXml trong writeCell
            shared.append("</sst>\n");

            zos.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zos.write(shared.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            Log.d(TAG, "sharedStrings.xml written.");
            // --- DEBUG: Print sharedStrings.xml content ---
            System.out.println("--- sharedStrings.xml Content ---");
            System.out.println(shared);
            System.out.println("---------------------------------");
        } else {
            // Ghi sharedStrings.xml rỗng nếu không có chuỗi nào
            zos.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\"/>\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            Log.d(TAG, "Empty sharedStrings.xml written.");
            // --- DEBUG: Print empty sharedStrings.xml content ---
            System.out.println("--- sharedStrings.xml (Empty) Content ---");
            System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\"/>\n");
            System.out.println("-----------------------------------------");
        }

        return currentSheetIndex; // Trả về số sheet thực tế đã ghi
    }
    // MỚI: xuất excel theo quan hệ cha-con

    /** GỐC CHẠY ỔN ĐỊNH
     * Phương thức ghi dữ liệu vào sheet và shared strings, hỗ trợ phân trang và xử lý theo lô.
     * Nhận đối tượng SQLiteDatabase đã mở từ phương thức gọi.
     *
     * @param db Đối tượng SQLiteDatabase đã được mở.
     * @param context Context của ứng dụng.
     * @param tableThuoc Tên bảng cần xuất.
     * @param zos ZipOutputStream để ghi dữ liệu.
     * @param totalRecordsInDb Tổng số bản ghi trong DB (để tính tiến độ).
     * @param callbacks Callback để báo cáo trạng thái.
     * @return Số lượng sheet đã ghi.
     */
    public static int writeSheetData(SQLiteDatabase db, Context context,
                                     String tableThuoc, ZipOutputStream zos, int totalRecordsInDb, ExportCallbacks callbacks) throws Exception {
        // Các cấu trúc dữ liệu toàn cục cho quá trình xuất (chia sẻ giữa các sheet)
        Map<String, Integer> sharedStringsMap = new HashMap<>();
        List<String> sharedStringList = new ArrayList<>();

        // Các cấu trúc dữ liệu cục bộ cho từng sheet
        StringBuilder sheetContent = new StringBuilder();
        List<String> hyperlinks = new ArrayList<>();
        List<String> hyperlinkCellRefs = new ArrayList<>();
        List<String> mergeCells = new ArrayList<>();

        int currentSheetIndex = 1; // Chỉ số của sheet hiện tại (1-indexed)
        int currentRowInSheet = 1; // Số dòng hiện tại trong sheet (1-indexed, bao gồm cả headers)
        int totalExportedRecords = 0; // Tổng số bản ghi đã xuất ra (chỉ đếm data rows)
        int lastPercent = 0;

        // Lấy MIN và MAX date một lần duy nhất
        String minDate = getMinOrMaxDate(db, tableThuoc, "MIN");
        String maxDate = getMinOrMaxDate(db, tableThuoc, "MAX");
        Log.d(TAG, "Min Date: " + minDate + ", Max Date: " + maxDate);

        // Chuẩn bị danh sách tất cả các cột cần truy vấn
        List<String> allColumns = new ArrayList<>();
        for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
            allColumns.add(col.dbColumnName);
            if (col.dbLinkColumnName != null) allColumns.add(col.dbLinkColumnName);
        }
        allColumns.add(DBHelperThuoc.INDEX_COLOR);
        String queryCols = TextUtils.join(", ", allColumns);

        // --- Khởi tạo sheet đầu tiên với tiêu đề và header ---
        initNewSheetXml(sheetContent, 1);   //Sheet1: active
        //sheetView(sheetContent, 1);   //Sheet1: active
        // writeInitialSheetHeaders: tạo 5 hàng. // Từ A1 đên dòng tiêu đề (A5): 5 dòng
        writeInitialSheetHeaders(sheetContent, sharedStringsMap, sharedStringList, mergeCells, minDate, maxDate);
        currentRowInSheet = 6; // Ghi record bắt đầu là 5: Sau tiêu đề, ngày tạo, ngày cập nhật, header (4 dòng)

        // === Vòng lặp chính để đọc dữ liệu theo lô và ghi vào sheet ===
        int offset = 0;
        // Khai báo Map để lưu trữ chỉ mục cột MỘT LẦN DUY NHẤT
        Map<String, Integer> cursorColumnIndices = new HashMap<>();
        int colorIndex = -1; // Khởi tạo với giá trị không hợp lệ
        int rawColor = 0;

        while (true) {
            Cursor cursor = null;
            try {
                // Truy vấn dữ liệu theo lô
                String batchQuery = "SELECT " + queryCols + " FROM " + tableThuoc +
                        " ORDER BY " + DBHelperThuoc.PARENT_ID + ", " + DBHelperThuoc.LEVEL +
                        " LIMIT " + BATCH_SIZE + " OFFSET " + offset;
                cursor = db.rawQuery(batchQuery, null);
                Log.d(TAG, "Executing batch query: " + batchQuery);

                if (cursor.getCount() == 0) {
                    Log.d(TAG, "No more records in batch. Breaking loop.");
                    break; // Hết dữ liệu
                }

                // Lấy chỉ mục cột từ Cursor CHỈ MỘT LẦN (khi xử lý lô đầu tiên)
                if (cursorColumnIndices.isEmpty()) { // Kiểm tra nếu chưa được khởi tạo
                    for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
                        cursorColumnIndices.put(col.dbColumnName, cursor.getColumnIndex(col.dbColumnName));
                        if (col.dbLinkColumnName != null) {
                            cursorColumnIndices.put(col.dbLinkColumnName, cursor.getColumnIndex(col.dbLinkColumnName));
                        }
                    }
                    colorIndex = cursor.getColumnIndex(DBHelperThuoc.INDEX_COLOR);
                    Log.d(TAG, "Column indices initialized for the first batch.");
                }

                // Duyệt qua từng record trong lô hiện tại
                if (cursor.moveToFirst()) {
                    do {
                        // Kiểm tra xem có cần tạo sheet mới không (phân trang)
                        // currentRowInSheet - 1 để loại bỏ 3 dòng header cố định và tính toán cho dòng dữ liệu
                        if ((currentRowInSheet - 1) % ROWS_PER_PAGINATION_SHEET == 0 && currentRowInSheet > 1) {
                            Log.d(TAG, "Max rows reached for sheet " + currentSheetIndex + ". Finalizing sheet.");
                            // ✅ KẾT THÚC SHEET HIỆN TẠI VÀ GHI VÀO ZIP
                            writeCurrentSheetToZip(zos, sheetContent, hyperlinks, hyperlinkCellRefs, currentSheetIndex, mergeCells);
                            currentSheetIndex++; // Tăng chỉ số sheet

                            // Reset các biến cho sheet mới
                            hyperlinks.clear();
                            hyperlinkCellRefs.clear();
                            mergeCells.clear();
                            sheetContent.setLength(0);

                            // ✅ Khởi tạo sheet mới với tiêu đề và header
                            // FREEZE SHEET KHÔNG ACTIVE:
                            initNewSheetXml(sheetContent, -1);  //-1 hoặc currentSheetIndex
                            //sheetView(sheetContent, -1);   //-1 hoặc currentSheetIndex
                            // writeInitialSheetHeaders:  Từ A1 đên dòng tiêu đề (A5): 5 dòng
                            writeInitialSheetHeaders(sheetContent, sharedStringsMap, sharedStringList, mergeCells, minDate, maxDate); // Chiếm 4 Row (0-4)
                            currentRowInSheet = 6; // Sau tiêu đề, ngày tạo, ngày cập nhật, header (5 dòng)

                            Log.d(TAG, "New sheet " + currentSheetIndex + " initialized with headers.");
                        }
                        rawColor = cursor.isNull(colorIndex) ? 0 : cursor.getInt(colorIndex);

                        // Ghi dữ liệu của hàng hiện tại vào sheetContent
                        writeRow(sheetContent, cursor, currentRowInSheet, DBHelperThuoc.EXPORT_COLUMNS,
                                sharedStringsMap, sharedStringList, hyperlinks, hyperlinkCellRefs, rawColor, cursorColumnIndices);
                        currentRowInSheet++; // Tăng số dòng trong sheet
                        totalExportedRecords++; // Tăng tổng số bản ghi đã xuất

                        // Cập nhật tiến độ
                        int percent = (int) (totalExportedRecords * 100.0 / (totalRecordsInDb == 0 ? 1 : totalRecordsInDb));
                        if (callbacks != null && percent > lastPercent) {
                            callbacks.onExportProgress(percent);
                            lastPercent = percent;
                        }

                    } while (cursor.moveToNext());
                }

                offset += BATCH_SIZE; // Tăng offset cho lô tiếp theo

            } catch (Exception e) {
                Log.e(TAG, "❌ Error processing batch at offset " + offset + ": " + e.getMessage(), e);
                throw e; // Ném lại để bắt ở cấp trên
            } finally {
                if (cursor != null) {
                    cursor.close(); // Đóng Cursor sau mỗi lô
                    Log.d(TAG, "Cursor for batch at offset " + offset + " closed.");
                }
            }
        } // End while loop for batches

        // ✅ Ghi sheet cuối cùng (sau khi tất cả các lô đã được xử lý)
        // Chỉ ghi nếu có dữ liệu trong sheet cuối cùng hoặc nếu đây là sheet đầu tiên và không có record nào
        if (totalExportedRecords > 0 || currentSheetIndex == 1) {
            Log.d(TAG, "Finalizing last sheet " + currentSheetIndex + ".");
            writeCurrentSheetToZip(zos, sheetContent, hyperlinks, hyperlinkCellRefs, currentSheetIndex, mergeCells);
        } else {
            Log.d(TAG, "No records exported, and no initial sheet written. Skipping final sheet write.");
        }


        // ✅ Ghi sharedStrings.xml (sau khi tất cả các sheet đã được xử lý)
        if (!sharedStringList.isEmpty()) {
            StringBuilder shared = new StringBuilder();
            shared.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            shared.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                    .append(sharedStringList.size()).append("\" uniqueCount=\"").append(sharedStringList.size()).append("\">\n");
            for (String s : sharedStringList)
                shared.append("<si><t>").append(s).append("</t></si>\n");    // escapeXml(s) đã escapeXml trong writeCell
            shared.append("</sst>\n");

            zos.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zos.write(shared.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            Log.d(TAG, "sharedStrings.xml written.");
            // --- DEBUG: Print sharedStrings.xml content ---
            System.out.println("--- sharedStrings.xml Content ---");
            System.out.println(shared);
            System.out.println("---------------------------------");
        } else {
            // Ghi sharedStrings.xml rỗng nếu không có chuỗi nào
            zos.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\"/>\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            Log.d(TAG, "Empty sharedStrings.xml written.");
            // --- DEBUG: Print empty sharedStrings.xml content ---
            System.out.println("--- sharedStrings.xml (Empty) Content ---");
            System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\"/>\n");
            System.out.println("-----------------------------------------");
        }

        return currentSheetIndex; // Trả về số sheet thực tế đã ghi
    }
    // End gốc: chạy ổn định

    /**
     * Ghi các dòng tiêu đề và header ban đầu vào sheet XML.
     *
     * @param sheetContent StringBuilder chứa XML của sheet.
     * @param sharedStringsMap Map các chuỗi dùng chung.
     * @param sharedStringList List các chuỗi dùng chung.
     * @param mergeCells List các tham chiếu ô hợp nhất.
     * @param minDate Ngày nhỏ nhất.
     * @param maxDate Ngày lớn nhất.
     */
    private static void writeInitialSheetHeaders(StringBuilder sheetContent,
                                                 Map<String, Integer> sharedStringsMap,
                                                 List<String> sharedStringList,
                                                 List<String> mergeCells,
                                                 String minDate, String maxDate) {
        int tempRowNum = 1; // Dòng tạm thời cho headers

        //1. ✅ Tiêu đề lớn: Row thứ 1 (index 0)
        String title = "DANH MỤC THUỐC BIỆT DƯỢC";
        int titleIndex = sharedStringsMap.computeIfAbsent(title, k -> {
            sharedStringList.add(k);
            return sharedStringList.size() - 1;
        });
        sheetContent.append("<row r=\"").append(tempRowNum).append("\">\n");
        sheetContent.append("<c r=\"").append(getCellRef(5, tempRowNum - 1)).append("\" t=\"s\" s=\"") // 5 = CỘT F
                .append(STYLE_INDEX_TITLE).append("\"><v>").append(titleIndex).append("</v></c>\n");
        sheetContent.append("</row>\n");
        // Thêm mergeCell cho tiêu đề (giả định merge từ A đến M): KHÔNG MERGER TITILE NỮA: TITLE NÀM CỘT F
        //mergeCells.add("<mergeCell ref=\"A" + tempRowNum + ":M" + tempRowNum + "\"/>");

        //2. ✅ Dòng ngày tạo : Row thứ 2 (index 1)
        tempRowNum++;
        String createdFile = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String createdAt = "Ngày tạo file: " + createdFile;
        int createdIndex = sharedStringsMap.computeIfAbsent(createdAt, k -> {
            sharedStringList.add(k);
            return sharedStringList.size() - 1;
        });
        // Nàm cột F, KHÔNG MERGE
        sheetContent.append("<row r=\"").append(tempRowNum).append("\">\n");
        sheetContent.append("<c r=\"F").append(tempRowNum).append("\" t=\"s\" s=\"").append(STYLE_INDEX_NORMAL).append("\"><v>").append(createdIndex).append("</v></c>\n");
        sheetContent.append("</row>\n");

        //3. ✅ Dòng ngày cập nhật: Row thứ 3 (index 2)
        tempRowNum++;
        String updated = "Cập nhật từ: " + minDate + " đến: " + maxDate;
        int updateIndex = sharedStringsMap.computeIfAbsent(updated, k -> {
            sharedStringList.add(k);
            return sharedStringList.size() - 1;
        });
        // CỘT F
        sheetContent.append("<row r=\"").append(tempRowNum).append("\">\n");
        sheetContent.append("<c r=\"F").append(tempRowNum).append("\" t=\"s\" s=\"").append(STYLE_INDEX_NORMAL).append("\"><v>").append(updateIndex).append("</v></c>\n");
        sheetContent.append("</row>\n");

        //4. Hàng (Row) trống, cách Header
        tempRowNum++;   // Táng 1 hàng trống
        //5. ✅ Header: Row thứ 5 (index 4)
        tempRowNum++;
        sheetContent.append("<row r=\"").append(tempRowNum).append("\">\n");
        int colIndexHeader = 0;
        for (DBHelperThuoc.ColumnInfo col : DBHelperThuoc.EXPORT_COLUMNS) {
            if (DBHelperThuoc.INDEX_COLOR.equalsIgnoreCase(col.dbColumnName)) continue;
            String cellRef = getCellRef(colIndexHeader, tempRowNum - 1);
            int strIdx = sharedStringsMap.computeIfAbsent(col.headerName, k -> {
                sharedStringList.add(k);
                return sharedStringList.size() - 1;
            });
            sheetContent.append("<c r=\"").append(cellRef).append("\" t=\"s\" s=\"")
                    .append(STYLE_INDEX_HEADER).append("\"><v>").append(strIdx).append("</v></c>\n");
            colIndexHeader++;
        }
        sheetContent.append("</row>\n");
    }

    private static void writeRowCha(StringBuilder sheetContent,
                                 Cursor cursor,
                                 int rowNumInSheet,
                                 List<DBHelperThuoc.ColumnInfoUrlCha> columns,
                                 Map<String, Integer> sharedStringsMap,
                                 List<String> sharedStringList,
                                 List<String> hyperlinks,
                                 List<String> hyperlinkCellRefs,
                                 int colorIndex) { // KHÔNG CẦN THÂM SỐ : Map<String, Integer> cursorColumnIndices


//        tableUrlsParent + "." + DBHelperThuoc.ID + " AS url_id, " +
//                tableUrlsParent + "." + DBHelperThuoc.PARENT_ID +", " +
//                tableUrlsParent + "." + DBHelperThuoc.LEVEL +", " +
//                tableUrlsParent + "." + DBHelperThuoc.URL +", " +
//                tableUrlsParent + "." + DBHelperThuoc.GHI_CHU +", " +
//                tableUrlsParent + "." + DBHelperThuoc.INDEX_COLOR +", " +
//                tableUrlsParent + "." + DBHelperThuoc.LAST_UPDATED +", " +

                sheetContent.append("<row r=\"").append(rowNumInSheet).append("\">\n");

        int excelColIndex = 0;
        // Chỉ tô màu hàng cha
        int rawColor = cursor.isNull(colorIndex) ? 0 : cursor.getInt(colorIndex);

        for (DBHelperThuoc.ColumnInfoUrlCha col : columns) {
            if (DBHelperThuoc.INDEX_COLOR.equalsIgnoreCase(col.dbColumnName)) continue;

            // Nguyên mẫu của Con khi chạy chưa có quan hệ cha con

//            //String kyTuSearch = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
//            int currentColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbColumnName), -1);
//            // Có thể bỏ cột KY_TU_SEARCH trong table Url cha, sẽ tính toán từ url
//            if (col.dbColumnName.equals(DBHelperThuoc.KY_TU_SEARCH)) {
//                //String url = cursorColumnIndices.get(DBHelperThuoc.URL);
//                //columns.get(2).dbColumnName=?
//
//            }
//            String value = (currentColumnIdx != -1) ? cursor.getString(currentColumnIdx) : "";
//
//            String link = null;
//            if (col.dbLinkColumnName != null) {
//                int linkColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbLinkColumnName), -1);
//                link = (linkColumnIdx != -1 && !cursor.isNull(linkColumnIdx)) ? cursor.getString(linkColumnIdx) : null;
//            }
            // end nguyên mẫu

            // Sửa lại theo quan hệ cha con
            String value;
            String link = null;
            if (col.dbColumnName.equals(DBHelperThuoc.KY_TU_SEARCH)) {
                String linkKyTuSearch = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.URL_P));
                value = Utils.getKyTuSeach(linkKyTuSearch);   // link để lấy Ky_tu_search, lấy xong gfasn lại là null
            } else {
                value = cursor.getString(cursor.getColumnIndexOrThrow(col.dbColumnName));
            }

            if (col.dbColumnName.equals(DBHelperThuoc.MA_THUOC_P)) {
                link = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.URL_P));
                value = cursor.getString(cursor.getColumnIndexOrThrow(col.dbColumnName));
            }
            if (col.dbColumnName.equals(DBHelperThuoc.TEN_THUOC)) {
                value = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.GHI_CHU));
            }
            //if (value == null || value.isEmpty()) continue; // VẪN PHẢI GHI MÀU SĂC


            if (col.dbLinkColumnName != null) {
                link = cursor.getString(cursor.getColumnIndexOrThrow(col.dbLinkColumnName));
            }

            String cellRef = getCellRef(excelColIndex, rowNumInSheet - 1);

            writeCell(sheetContent, cellRef, value, link,
                    sharedStringsMap, sharedStringList,
                    hyperlinks, hyperlinkCellRefs,
                    rawColor);

            // Cho nhanh
            if (rawColor == 0 && col.dbColumnName.equals(DBHelperThuoc.TEN_THUOC) && (value == null||value.isEmpty())) {
                // kết thúc
                break;
            }

            excelColIndex++;
        }

        sheetContent.append("</row>\n");


    }

    /**
     * Phương thức ghi một hàng dữ liệu vào StringBuilder XML từ Cursor.
     *
     * @param sheetContent StringBuilder chứa XML của sheet.
     * @param cursor Cursor hiện tại đang trỏ đến hàng cần đọc.
     * @param rowNumInSheet Số thứ tự hàng trong Excel (1-indexed).
     * @param columns Danh sách thông tin cột.
     * @param sharedStringsMap Map các chuỗi dùng chung.
     * @param sharedStringList List các chuỗi dùng chung.
     * @param hyperlinks List các hyperlink.
     * @param hyperlinkCellRefs List các tham chiếu ô của hyperlink.
     * //@param colorIndex Chỉ mục cột màu trong Cursor.
     * @param cursorColumnIndices Map tên cột -> chỉ mục cột trong Cursor.
     */
    private static void writeRow(StringBuilder sheetContent,
                                 Cursor cursor,
                                 int rowNumInSheet,
                                 List<DBHelperThuoc.ColumnInfo> columns,
                                 Map<String, Integer> sharedStringsMap,
                                 List<String> sharedStringList,
                                 List<String> hyperlinks,
                                 List<String> hyperlinkCellRefs,
                                 int colorIndex,
                                 Map<String, Integer> cursorColumnIndices) {

        sheetContent.append("<row r=\"").append(rowNumInSheet).append("\">\n");

        int excelColIndex = 0;
        //rawColor= 0: CÁC HÀNG CON KHÔNG TÔ MÀU
        //int rawColor = cursor.isNull(colorIndex) ? 0 : cursor.getInt(colorIndex);
        int rawColor = 0;

        for (DBHelperThuoc.ColumnInfo col : columns) {
            if (DBHelperThuoc.INDEX_COLOR.equalsIgnoreCase(col.dbColumnName)) continue;

            String value;
            String link = null;
            if (col.dbColumnName.equals(DBHelperThuoc.KY_TU_SEARCH)) {
                String linkKyTuSearch = cursor.getString(cursor.getColumnIndexOrThrow(DBHelperThuoc.URL_P));
                value = Utils.getKyTuSeach(linkKyTuSearch);   // link để lấy Ky_tu_search, lấy xong gfasn lại là null
            } else {
                //value = cursor.getString(cursor.getColumnIndexOrThrow(col.dbColumnName));
                int currentColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbColumnName), -1);
                value = (currentColumnIdx != -1 && !cursor.isNull(currentColumnIdx)) ? cursor.getString(currentColumnIdx) : "";
                if (col.dbLinkColumnName != null) {
                    int linkColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbLinkColumnName), -1);
                    link = (linkColumnIdx != -1 && !cursor.isNull(linkColumnIdx)) ? cursor.getString(linkColumnIdx) : null;
                }
            }

            // Cũ Khi cột Ky Tu Search của Cha (hoặc con) vẫn Còn
//            int currentColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbColumnName), -1);
//            String value = (currentColumnIdx != -1 && !cursor.isNull(currentColumnIdx)) ? cursor.getString(currentColumnIdx) : "";
//
//            String link = null;
//            if (col.dbLinkColumnName != null) {
//                int linkColumnIdx = Objects.requireNonNullElse(cursorColumnIndices.get(col.dbLinkColumnName), -1);
//                link = (linkColumnIdx != -1 && !cursor.isNull(linkColumnIdx)) ? cursor.getString(linkColumnIdx) : null;
//            }
            // end cũ

            String cellRef = getCellRef(excelColIndex, rowNumInSheet - 1);
            // XEM
            if (col.dbColumnName.equals(DBHelperThuoc.TEN_THUOC)) {
                int Stop = 0;
            }
            writeCell(sheetContent, cellRef, value, link,
                    sharedStringsMap, sharedStringList,
                    hyperlinks, hyperlinkCellRefs,
                    rawColor);

            excelColIndex++;
        }

        sheetContent.append("</row>\n");
    }

    private static void writeCell(StringBuilder sheetContent,
                                  String cellRef,
                                  String rawValue,
                                  String hyperlink,
                                  Map<String, Integer> sharedStringsMap,
                                  List<String> sharedStringList,
                                  List<String> hyperlinks,
                                  List<String> hyperlinkCellRefs,
                                  int rawColor) {

        if (rawValue == null) rawValue = "";
        String cleaned = escapeXml(rawValue.trim());    // escapeXml lần 1

        String type = "s";
        String value = "";
        boolean hasHyperlink = hyperlink != null && !hyperlink.isEmpty() && isValidUrl(hyperlink);

        // Xem:
//        if (rawValue.contains("TRÙNG 1 PHẦN:")) {
//            int stop=0;
//        }
        //int styleIndex = getFullStyleIndex(hasHyperlink, rawColor); // Mất công tính
        //int styleIndex = hasHyperlink? STYLE_INDEX_HYPERLINK_NORMAL:rawColor;
        // Nếu là Row con thì KHÔNG TÔ MÀU: styleIndex, rawColor LUÔN = 0
        int styleIndex = 0;
        if (hasHyperlink) {
            hyperlinks.add(hyperlink);
            hyperlinkCellRefs.add(cellRef);
        }

        if (isNumeric(cleaned)) {
            type = "n";
            value = cleaned;
        } else if (isDateTime(cleaned)) {
            type = "n";
            value = String.valueOf(convertToExcelDate(cleaned));
            styleIndex = STYLE_INDEX_DATE;
        } else {
            int sIndex = sharedStringsMap.computeIfAbsent(cleaned, k -> {
                sharedStringList.add(k);
                return sharedStringList.size() - 1;
            });
            type = "s";
            value = String.valueOf(sIndex);
        }

        if (rawValue.isEmpty()) {
            sheetContent.append("<c r=\"").append(cellRef)
                    .append("\" t=\"").append(type)
                    .append("\" s=\"").append(styleIndex)
                    .append("\"/>").append("\n");
        } else {
            sheetContent.append("<c r=\"").append(cellRef)
                    .append("\" t=\"").append(type)
                    .append("\" s=\"").append(styleIndex)
                    .append("\"><v>").append(value).append("</v></c>\n");
        }
    }

    /**
     * Phương thức lấy giá trị MIN hoặc MAX từ cột 'created_at'.
     * Nhận đối tượng SQLiteDatabase đã mở từ phương thức gọi.
     * Sử dụng DatabaseUtils.stringForQuery() để đảm bảo an toàn và đơn giản.
     *
     * @param db Đối tượng SQLiteDatabase đã được mở.
     * @param tableUrlCha Tên bảng.
     * @param type Loại truy vấn ("MIN" hoặc "MAX").
     * @return Giá trị String của MIN/MAX hoặc null nếu không có.
     */
    private static synchronized String getMinOrMaxDate(SQLiteDatabase db, String tableUrlCha, String type) {
        String result = null;
        long seconds = 0;   // Xem lại , vì time java bắt đầu 1/1/1970?
        //String sql = "SELECT " + type + "(created_at) AS result FROM " + tableName; //LAST_UPDATED
        String sql = "SELECT " + type + "("+DBHelperThuoc.LAST_UPDATED+") AS result FROM " + tableUrlCha; //LAST_UPDATED
        try {
            //result = DatabaseUtils.stringForQuery(db, sql, null);       // Trả về số Time giây INTEGER
            seconds = Long.parseLong(DatabaseUtils.stringForQuery(db, sql, null));       // Trả về số Time giây INTEGER
            Log.d(TAG, "getMinOrMaxDate (" + type + ") result: " + result);     // Trả về số Time giây INTEGER
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in getMinOrMaxDate using DatabaseUtils: " + e.getMessage(), e);
            result = null;
        }
        //result = Utils.getTimeFormatStringHHMMSS(seconds*1000);
        result = Utils.getFormatDateDDMMYYYsuyet_HHMMSS(seconds*1000); // dd/mm/yyyy hh:mm:ss
        return result;
    }

    // --- Các phương thức hỗ trợ ghi XML cho Excel ---

    private static void writeContentTypes(ZipOutputStream zos) throws IOException {
        String content =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                        "    <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                        "    <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                        "    <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
                        "    <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                        "    <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n" +
                        "    <Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>\n" +
                        "    <Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>\n" +
                        "    <Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>" +
                        "</Types>";

        zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- [Content_Types].xml Content ---");
        System.out.println(content);
        System.out.println("-----------------------------------");
    }

    private static void writeRels(ZipOutputStream zos) throws IOException {
        String content =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                        "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
                        "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>" +
                        "  <Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>" +
                        "</Relationships>";

        zos.putNextEntry(new ZipEntry("_rels/.rels"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- _rels/.rels Content ---");
        System.out.println(content);
        System.out.println("---------------------------");
    }

    private static void writeWorkbook(ZipOutputStream zos, int totalSheets) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"\n");
        sb.append("          xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n");
        sb.append("  <sheets>\n");

        for (int i = 1; i <= totalSheets; i++) {
            sb.append("    <sheet name=\"Sheet").append(i)
                    .append("\" sheetId=\"").append(i)
                    .append("\" r:id=\"rId").append(i).append("\"/>\n");
        }

        sb.append("  </sheets>\n");
        sb.append("</workbook>\n");

        String content = sb.toString();
        zos.putNextEntry(new ZipEntry("xl/workbook.xml"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- xl/workbook.xml Content ---");
        System.out.println(content);
        System.out.println("-------------------------------");
    }

    private static void writeWorkbookRels(ZipOutputStream zos, int totalSheets) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");

        for (int i = 1; i <= totalSheets; i++) {
            sb.append("  <Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"")
                    .append(" Target=\"worksheets/sheet").append(i).append(".xml\"/>\n");
        }
        sb.append("  <Relationship Id=\"rId").append(totalSheets + 1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        sb.append("  <Relationship Id=\"rId").append(totalSheets + 2).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>\n");

        sb.append("</Relationships>\n");

        String content = sb.toString();
        zos.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- xl/_rels/workbook.xml.rels Content ---");
        System.out.println(content);
        System.out.println("------------------------------------------");
    }

    private static void writeStyles(ZipOutputStream zos) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
                "    <numFmts count=\"0\"/>\n" +

                "    <fonts count=\"5\">\n" +
                            // Chữ: mặc định
                "        <font><sz val=\"11\"/><name val=\"Calibri\"/></font>\n" +  // 0 - default
                            // Font cho hyperlink: chữ xanh (mặc định)
                "        <font><sz val=\"11\"/><color theme=\"10\"/><name val=\"Calibri\"/><u val=\"single\"/></font>\n" + // 1 - hyperlink, Chũ xanh
                        // Font cho hyperlink: chữ trắng
                "        <font><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><u/><name val=\"Calibri\"/><u val=\"single\"/></font>\n" + // fontId=2: hyperlink Trắng, gạch dưới
                            // Title Chữ: XANH ĐẬM, CỠ 16
                "        <font><sz val=\"16\"/><color rgb=\"FF0000FF\"/><b/><name val=\"Calibri\"/></font>\n" + // fontId=3: Chữ xanh blue, đậm, 16: Title
                            // Header Chữ: TRẮNG ĐẬM, CỠ 11
                "        <font><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><b/><name val=\"Calibri\"/></font>\n" + // fontId=4: TRẮNG ĐẬM, CỠ 11: HEADER
                "    </fonts>\n" +

                "    <fills count=\"6\">\n" +
                "        <fill><patternFill patternType=\"none\"/></fill>\n" +       // 0 - default
                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFFF99\"/></patternFill></fill>\n" + // 1 - yellow
                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFF9999\"/></patternFill></fill>\n" + // 2 - red
                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF99FF99\"/></patternFill></fill>\n" + // 3 - green
                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0000FF\"/></patternFill></fill>\n" + // 4 - BLUE
                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFB266\"/></patternFill></fill>\n" + // 5 - Nền Cam(Orange)
                "    </fills>\n" +

                "    <borders count=\"1\">\n" +
                "        <border><left/><right/><top/><bottom/><diagonal/></border>\n" +
                "    </borders>\n" +

                "    <cellStyleXfs count=\"1\">\n" +
                "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>\n" +
                "    </cellStyleXfs>\n" +

                "    <cellXfs count=\"11\">\n" +
                            // NỀN + CHỮ
                "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n" +                                  // 0 - default
                "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"1\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 1 - Nền yellow + Chữ đen mặc định
                "        <xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 2 - Nền red + Chữ trắng
                "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 3 - Nền green + Chữ đen
                "        <xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 4 - Nền trong suốt(TITLE) + Chữ Xanh Blue, ĐẬM
                "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"4\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM
                "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 6 - Nền CAM ORANGE(LỖI) + Chữ TRẮNG, ĐẬM: Cho lỗi err
                        // Hyperlink + nền
                "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 7 - hyperlink default
                "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"1\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 8 - hyperlink default + yellow
                "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 9 - hyperlink trắng + red
                "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 10 - hyperlink default + green
                "    </cellXfs>\n" +
                "    <cellStyles count=\"1\">\n" +
                "        <cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/>\n" +
                "    </cellStyles>\n" +
                "</styleSheet>\n";



        // Cũ
//                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
//                "    <fonts count=\"8\">\n" +
//                "        <font><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/></font>\n" +  // fontId=0:
//                        // Chữ Hyperlink
//                "        <font><sz val=\"11\"/><color rgb=\"FF0000FF\"/><u/><name val=\"Calibri\"/></font>\n" + // fontId=1: hyperlink blue, gạch dưới
//                "        <font><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><u/><name val=\"Calibri\"/></font>\n" + // fontId=2: hyperlink Trắng, gạch dưới
//                        // Chữ thường
//                "        <font><sz val=\"16\"/><color rgb=\"FF0000FF\"/><b/><name val=\"Calibri\"/></font>\n" + // fontId=3: Chữ xanh blue, đậm, 16: Title
//                "        <font><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><b/><name val=\"Calibri\"/></font>\n" + //fontId=4: Chữ Trắng, 11
//                "        <font><sz val=\"11\"/><color rgb=\"FF000000\"/><name val=\"Calibri\"/></font> <!-- //fontId=5: Chữ đen không in đậm -->\n" +
//                "    </fonts>\n" +
//                "    <fills count=\"8\">\n" +
//                "    <fill><patternFill patternType=\"none\"/></fill>\n" +                                      // 0: None
//                "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFFF99\"/></patternFill></fill>\n" + // 1: Yellow
//                "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFF9999\"/></patternFill></fill>\n" + // 2: Red
//                "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF99FF99\"/></patternFill></fill>\n" + // 3: Green
//                "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFB266\"/><bgColor indexed=\"64\"/></patternFill></fill> <!--4: Nền cam nhạt -->\n" +
//                "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF90EE90\"/><bgColor indexed=\"64\"/></patternFill></fill> <!--5: Nền xanh lá nhạt -->\n" +
//
//                "        <fill><patternFill patternType=\"gray125\"/></fill>\n" +   // 6: Xám
////                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFFF00\"/><bgColor indexed=\"64\"/></patternFill></fill>\n" +
////                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0070C0\"/><bgColor indexed=\"64\"/></patternFill></fill>\n" +
////                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFF0000\"/><bgColor indexed=\"64\"/></patternFill></fill>\n" +
////                "        <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFA500\"/><bgColor indexed=\"64\"/></patternFill></fill>\n" +
//
//                "    </fills>\n" +
//                "    <borders count=\"1\">\n" +
//                "        <border><left/><right/><top/><bottom/><diagonal/></border>\n" +
//                "    </borders>\n" +
//                "    <cellStyleXfs count=\"1\">\n" +
//                "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>\n" +
//                "    </cellStyleXfs>\n" +
//                "    <cellXfs count=\"12\">\n" +
//
//                        // 8 styles
//                "   <xf xfId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/>\n" +   // 3: Title
//                "   <xf xfId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" numFmtId=\"0\"/>\n" +    // 0: Normal
//                "   <xf xfId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/>\n" +   // 1: Hyperlink + nền trong suốt
//                "   <xf xfId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" applyFont=\"1\"/>\n" +   // 2: Hyperlink trắng + nền đỏ
//
//                "   <xf xfId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" applyFill=\"1\"/>\n" +   // 3: Yellow
//                        // Hyperlink + nền
//                "   <xf xfId=\"0\" fontId=\"1\" fillId=\"1\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!-- 4: Hyperlink xanh + vàng  -->\n" +
//                "   <xf xfId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!-- 5: Hyperlink xanh + vàng  -->\n" +
//
//                "   <xf xfId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" applyFont=\"1\" applyFill=\"2\"/> <!--8: Chữ: Trắng (#FFFFFF), Nền: Đỏ (#FF0000) -->\n" +
//                "   <xf xfId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/>\n" +   // 5: Hyperlink + đậm
//                "   <xf xfId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" numFmtId=\"14\" applyNumberFormat=\"1\"/>\n" + // 6: Date
//                "   <xf xfId=\"0\" fontId=\"4\" fillId=\"3\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">\n" + // 7: Header (white on blue)
//                "       <alignment horizontal=\"center\" vertical=\"center\"/>\n" +
//                "   </xf>\n" +
//
//
//                        //
//                "        <xf xfId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" numFmtId=\"0\"/> <!--0: Chữ: Đen (mặc định), Nền: Không có -->\n" +
//                "        <xf xfId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/> <!--1: hyperlink: Chữ: Xanh dương (#0000FF), Nền: Không có -->\n" +
//                "        <xf xfId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/> <!--2: Chữ: Xanh lam (#0070C0, in đậm), Nền: Không có -->\n" +
//                "        <xf xfId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" applyFill=\"1\"/> <!--3: Chữ: Đen (mặc định), Nền: Vàng (#FFFF00) -->\n" +
//                "        <xf xfId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!--4: Chữ: Xanh dương (#0000FF), Nền: Vàng (#FFFF00) -->\n" +
//                "        <xf xfId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/> <!--5: Chữ: Xanh dương (#0000FF, in đậm, gạch chân), Nền: Không có -->\n" +
//                "        <xf xfId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" numFmtId=\"14\" applyNumberFormat=\"1\"/> <!--6: Chữ: Đen (mặc định), Nền: Không có, Định dạng ngày -->\n" +
//                "        <xf xfId=\"0\" fontId=\"4\" fillId=\"3\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">\n" +
//                "            <alignment horizontal=\"center\" vertical=\"center\"/>\n" +
//                "        </xf> <!--7: Chữ: Trắng (#FFFFFF, in đậm), Nền: Xanh lam (#0070C0), Căn giữa -->\n" +
//                "        <xf xfId=\"0\" fontId=\"6\" fillId=\"4\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!--8: Chữ: Trắng (#FFFFFF), Nền: Đỏ (#FF0000) -->\n" +
//                "        <xf xfId=\"0\" fontId=\"6\" fillId=\"5\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!--9: Chữ: Trắng (#FFFFFF), Nền: Cam (#FFA500) -->\n" +
//                "        <xf xfId=\"0\" fontId=\"7\" fillId=\"6\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!--10: Chữ: Đen (#000000), Nền: Cam nhạt (#FFB266) -->\n" +
//                "        <xf xfId=\"0\" fontId=\"7\" fillId=\"7\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/> <!--11: Chữ: Đen (#000000), Nền: Xanh lá nhạt (#90EE90) -->\n" +
//                "    </cellXfs>\n" +
//                "</styleSheet>";

        zos.putNextEntry(new ZipEntry("xl/styles.xml"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- xl/styles.xml Content ---");
        System.out.println(content);
        System.out.println("-----------------------------");
    }

    private static void writeAppXml(ZipOutputStream zos) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">" +
                "<Application>Android Excel Exporter</Application>" +
                "<DocSecurity>0</DocSecurity>" +
                "<ScaleCrop>false</ScaleCrop>" +
                "<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\"><vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant><vt:variant><vt:i4>1</vt:i4></vt:variant></vt:vector></HeadingPairs>" +
                "<TitlesOfParts><vt:vector size=\"1\" baseType=\"lpstr\"><vt:lpstr>Sheet1</vt:lpstr></vt:vector></TitlesOfParts>" +
                "<LinksUpToDate>false</LinksUpToDate>" +
                "<SharedDoc>false</SharedDoc>" +
                "<HyperlinksChanged>false</HyperlinksChanged>" +
                "<AppVersion>1.0</AppVersion>" +
                "</Properties>";
        zos.putNextEntry(new ZipEntry("docProps/app.xml"));
        zos.write(content.getBytes());
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- docProps/app.xml Content ---");
        System.out.println(content);
        System.out.println("--------------------------------");
    }

    private static void writeCoreXml(ZipOutputStream zos) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<dc:title>Android Excel Exporter</dc:title>" +
                "<dc:creator>Android App</dc:creator>" +
                "<cp:lastModifiedBy>Android App</cp:lastModifiedBy>" +
                "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()) + "</dcterms:created>" +
                "<dcterms:modified xsi:type=\"dcterms:W3CDTF\">" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()) + "</dcterms:modified>" +
                "</cp:coreProperties>";
        zos.putNextEntry(new ZipEntry("docProps/core.xml"));
        zos.write(content.getBytes());
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- docProps/core.xml Content ---");
        System.out.println(content);
        System.out.println("---------------------------------");
    }

    private static void initNewSheetXml(StringBuilder sheetContent, int activeSheetIndex) {
        sheetContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
//        sheetContent.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"" +
//                " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n"); // Cũ Chạy được
        //Mới:
        sheetContent.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"" +
                " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"" +
                " xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"" +
                " xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\"" +
                " xmlns:xr=\"http://schemas.microsoft.com/office/spreadsheetml/2014/revision\"" +
                " xmlns:xr2=\"http://schemas.microsoft.com/office/spreadsheetml/2015/revision2\"" +
                " xmlns:xr3=\"http://schemas.microsoft.com/office/spreadsheetml/2016/revision3\"" +
                " mc:Ignorable=\"x14ac xr xr2 xr3\" xr:uid=\"{9B326C1F-1462-486D-BD7A-C21D7617BBBC}\">\n");

        sheetView( sheetContent, activeSheetIndex); //TRƯỚC: sheetContent.append("<sheetData>\n");

        sheetContent.append("<sheetData>\n");
        // --- DEBUG: Print XML content ---
        System.out.println("--- New Sheet XML Start ---");
        System.out.println(sheetContent);
        System.out.println("---------------------------");
    }

    private static void sheetView (StringBuilder sheetContent, int activeSheetIndex) {
        String tabSelected = "";
        if (activeSheetIndex >= 1) {    //<0 KHÔNG LẤY
            tabSelected = "tabSelected=\"" + activeSheetIndex +"\"";  //activeSheetIndex = 1 mặc định
        }
        sheetContent.append(
                "<sheetViews>\n" +
                "   <sheetView ").append(tabSelected).append(" workbookViewId=\"0\">\n" +
                "       <pane xSplit=\"2\" ySplit=\"5\" topLeftCell=\"C6\" activePane=\"bottomRight\" state=\"frozen\"/>\n" +
                "       <selection pane=\"topRight\" activeCell=\"C1\" sqref=\"C1\"/>\n" +
                "       <selection pane=\"bottomLeft\" activeCell=\"A6\" sqref=\"A6\"/>\n" +
                "       <selection pane=\"bottomRight\" activeCell=\"C6\" sqref=\"C6\"/>\n" +
                "   </sheetView>\n" +
                "</sheetViews>\n" +
                "<sheetFormatPr defaultRowHeight=\"14.4\" x14ac:dyDescent=\"0.3\"/>\n");

        //sheetContent.append("<dimension ref=\"B3:G8\"/>\n");    //→ vùng dữ liệu "tồn tại" trong sheet, từ ô B3:B8
    }
    private static void writeCurrentSheetToZip(ZipOutputStream zos, StringBuilder sheetContent,
                                               List<String> hyperlinks, List<String> hyperlinkCellRefs,
                                               int sheetIndex, List<String> mergeCells) throws IOException {
        sheetContent.append("</sheetData>\n");

        if (!mergeCells.isEmpty()) {
            sheetContent.append("<mergeCells count=\"").append(mergeCells.size()).append("\">\n");
            for (String merge : mergeCells)
                sheetContent.append(merge).append("\n");
            sheetContent.append("</mergeCells>\n");
        }

        if (!hyperlinks.isEmpty()) {
            sheetContent.append("<hyperlinks>\n");
            for (int i = 0; i < hyperlinks.size(); i++) {
                sheetContent.append("<hyperlink ref=\"").append(hyperlinkCellRefs.get(i))
                        .append("\" r:id=\"rId").append(i + 1).append("\"/>\n");
            }
            sheetContent.append("</hyperlinks>\n");
        }
        sheetContent.append("</worksheet>\n");

        String sheetXmlContent = sheetContent.toString();
        zos.putNextEntry(new ZipEntry("xl/worksheets/sheet" + sheetIndex + ".xml"));
        zos.write(sheetXmlContent.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // --- DEBUG: Print XML content ---
        System.out.println("--- xl/worksheets/sheet" + sheetIndex + ".xml Content ---");
        System.out.println(sheetXmlContent);
        System.out.println("----------------------------------------");


        if (!hyperlinks.isEmpty()) {
            zos.putNextEntry(new ZipEntry("xl/worksheets/_rels/sheet" + sheetIndex + ".xml.rels"));
            StringBuilder relsContent = new StringBuilder();
            relsContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            relsContent.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
            for (int i = 0; i < hyperlinks.size(); i++) {
                relsContent.append("<Relationship Id=\"rId").append(i + 1)
                        .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\"")
                        .append(" Target=\"").append(escapeXml(hyperlinks.get(i))).append("\" TargetMode=\"External\"/>\n");
            }
            relsContent.append("</Relationships>\n");
            String relsXmlContent = relsContent.toString();
            zos.write(relsXmlContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            // --- DEBUG: Print XML content ---
            System.out.println("--- xl/worksheets/_rels/sheet" + sheetIndex + ".xml.rels Content ---");
            System.out.println(relsXmlContent);
            System.out.println("--------------------------------------------------");
        }
    }

    private static String getCellRef(int colIndex, int rowIndex) {
        StringBuilder sb = new StringBuilder();
        int current = colIndex;
        while (current >= 0) {
            sb.insert(0, (char) ('A' + (current % 26)));
            current = (current / 26) - 1;
        }
        sb.append(rowIndex + 1);
        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDateTime(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        };
        for (SimpleDateFormat format : formats) {
            try {
                format.setLenient(false);
                format.parse(str);
                return true;
            } catch (ParseException e) {
            }
        }
        return false;
    }

    private static double convertToExcelDate(String dateString) {
        SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        };
        Date date = null;
        for (SimpleDateFormat format : formats) {
            try {
                format.setLenient(false);
                date = format.parse(dateString);
                break;
            } catch (ParseException e) {
            }
        }

        if (date == null) {
            return 0.0;
        }

        long javaTime = date.getTime();
        return (double) (javaTime / (1000 * 60 * 60 * 24)) + 25569.0 + 1;
    }

    private static boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://"));
    }

    private static int getFullStyleIndex(boolean isHyperlink, int rawColor) {
// 0 - default:                                     "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n" +                                  // 0 - default
// 1 - Nền yellow + Chữ đen:               "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"1\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 1 - Nền yellow + Chữ đen mặc định
// 2 - Nền red + Chữ trắng:                         "        <xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 2 - Nền red + Chữ trắng
// 3 - Nền green + Chữ đen:                         "        <xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>\n" +                  // 3 - Nền green + Chữ đen
// 4 - Nền trong suốt(TITLE)+ Chữ Blue, ĐẬM:        "        <xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 4 - Nền trong suốt(TITLE) + Chữ Xanh Blue, ĐẬM
// 5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM:      "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"4\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM
// 6 - Nền CAM ORANGE(ERR-LỖI) + Chữ TRẮNG, ĐẬM:    "        <xf numFmtId=\"0\" fontId=\"4\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>\n" +  // 6 - Nền CAM ORANGE(LỖI) + Chữ TRẮNG, ĐẬM: Cho lỗi err
//    // Hyperlink + nền
// 7 - hyperlink default                            "        <xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n" +                  // 7 - hyperlink default
        if (isHyperlink) {  // Nếu cell nào chứa link thì mặc định Nền trắng chư xanh
            return STYLE_INDEX_HYPERLINK_NORMAL;   // 7 - hyperlink default
            //
//            if (rawColor == 1) {
//                return STYLE_INDEX_HYPER_YELLOW;    // 7 - hyperlink default + yellow
//            } else if (rawColor == 2) {
//                return 8;    // 8 - hyperlink trắng + red
//            } else if (rawColor == 3) {
//                return 9;    //9 - hyperlink default + green
//            } else if (false /* cần in đậm */) { // Thay đổi logic này nếu có điều kiện in đậm
//                return STYLE_INDEX_HYPER_BOLD;
//            } else {    //=0: 6 - hyperlink default
//                return STYLE_INDEX_HYPERLINK;   // 6 - hyperlink default
//            }
        } else {    // KHÔNG PHẢI HYPERLONK
            // Của record
            if (rawColor == 1) {
                return STYLE_INDEX_YELLOW;  //1 - Nền yellow + Chữ đen
            } else if (rawColor == 2){
                return 2;       // 2 - Nền red + Chữ trắng
            } else if (rawColor == 3){
                return 3;       // 3 - Nền green + Chữ đen
                //end Của record
            } else if (rawColor == 4){
                return 4;  // 4 - Nền trong suốt(TITLE)+ Chữ Blue, ĐẬM:
            } else if (rawColor == 5){
                return 5;  //5 - Nền XANH BLUE(HEADER) + Chữ TRẮNG, ĐẬM
            } else if (rawColor == 6){
                return 6;  //Nền CAM ORANGE(ERR-LỖI) + Chữ TRẮNG, ĐẬM:
            } else { //0 - default
                return STYLE_INDEX_NORMAL;  //=0
            }
        }
    }

    public static void exportDataToExcel(SQLiteDatabase db, Context context, ExportCallbacks callbacks, boolean useStreaming) {
        Log.d(TAG, "exportDataToExcel called (placeholder for XLSX export)");
        callbacks.onExportSuccess("Placeholder XLSX Export Completed.");
    }
}
