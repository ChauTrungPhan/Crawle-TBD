package com.example.crawlertbdgemini2modibasicview;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;
import com.example.crawlertbdgemini2modibasicview.utils.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportViewModel extends AndroidViewModel  {
    private static final String TAG = "ExportViewModel";
    /**
     * Lưu ý về MyViewModelCreateExcel_Gemini:

     * 1/ Tôi đã thay đổi chức năng của ViewModel này. Ban đầu, nó có vẻ đang cố gắng khởi
     * chạy WorkManager cho việc xuất Excel, nhưng ExcelExporterOptimized đã là
     * một class riêng biệt và không cần WorkManager để chạy.

     * 2/ Bây giờ, ViewModel này sẽ sử dụng một ExecutorService để chạy
     * ExcelExporterOptimized.exportDataToExcel trên một luồng nền, giữ cho UI
     * không bị chặn.thread không bị chặn.

     * 3/ Các phương thức liên quan đến WorkManager đã bị xóa khỏi MyViewModelCreateExcel_Gemini
     * vì chúng không phù hợp với chức năng xuất Excel trực tiếp.
     * WorkManager nên được sử dụng cho các tác vụ nền dai dẳng, không thể bị gián đoạn, như crawler.
     * Xuất Excel là một tác vụ có thể hoàn thành nhanh chóng hơn và được điều khiển trực tiếp
     * từ ViewModel.
     */

    private final MutableLiveData<Integer> progressPercent = new MutableLiveData<>(0);
    private final MutableLiveData<String> messageLog = new MutableLiveData<>();
    // Đổi tên LiveData cho rõ ràng hơn
    private final MutableLiveData<String> recordOnRecods = new MutableLiveData<>();
    private final MutableLiveData<String> maThuoc = new MutableLiveData<>();
    private final MutableLiveData<Long> iD = new MutableLiveData<Long>();

//    private final MutableLiveData<String> elapsedTimeText = new MutableLiveData<>("");
//    private final MutableLiveData<String> remainingTimeText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isExporting = new MutableLiveData<>(false);
    private final MutableLiveData<String> exportStatus = new MutableLiveData<>(""); // "SUCCESS" or error message

    public LiveData<Integer> getProgressPercent() { return progressPercent; }
    public LiveData<String> getMessageLog() { return messageLog; }
    public LiveData<String> getRecordOnRecods() { return recordOnRecods; } // Getter mới
    public LiveData<String> getMaThuoc() { return maThuoc; }
    public LiveData<Long> geID() { return iD; }
    public LiveData<Boolean> getIsExporting() { return isExporting; }
    public LiveData<String> getExportStatus() { return exportStatus; }

    //private ExcelExporter exporter;
    private ExcelExporterOptimized_goc excelExporterOptimized;
    private final DBHelperThuoc dbHelperThuoc; // Để quản lý cơ sở dữ liệu
    //private SQLiteDatabase dbThuoc;

    private final Context applicationContext; // Thêm biến này để lưu application context
    private final SettingsRepository settingsRepository; // Thêm biến này
    // CHỈNH SỬA: Loại bỏ các biến WorkManager không liên quan đến chức năng xuất Excel của ViewModel này
    // private final WorkManager workManager;
    // private final LiveData<List<WorkInfo>> workInfoLiveData;
    private final ExecutorService excelExecutor; // CHỈNH SỬA: Executor để chạy tác vụ xuất Excel trên luồng nền
    public ExportViewModel(@NonNull Application application) {
        super(application);
        this.applicationContext = application.getApplicationContext(); // Gán application context
        // Khởi tạo executorService ở đây nếu chưa có
        excelExecutor = Executors.newSingleThreadExecutor();
        //
        dbHelperThuoc = DBHelperThuoc.getInstance(this.applicationContext); // Hoặc bạn có thể nhận nó qua constructor nếu đang dùng Factory

        this.settingsRepository = new SettingsRepository(application); // Khởi tạo SettingsRepository
        // exporter = new ExcelExporter(executorService, application);
        // Khởi tạo excelExporterOptimized ngay trong constructor
        excelExporterOptimized = new ExcelExporterOptimized_goc(excelExecutor, applicationContext);
    }

    // CHỈNH SỬA: Phương thức mới để bắt đầu xuất Excel
    public void exportToExcel(boolean isXmlExport, DBHelperThuoc dbHelper ,
                              String tableName, String filePath) {
        //SQLiteDatabase db = dbHelper.getReadableDatabase();

        excelExecutor.execute(() -> {
            /** Lỗi này xảy ra khi bạn cố gắng cập nhật giá trị của androidx.lifecycle.LiveData
             * (hoặc MutableLiveData) từ một luồng nền (background thread).
             * LiveData được thiết kế để chỉ được cập nhật trên luồng chính (main thread/UI thread) bằng phương thức setValue().
             * Nếu bạn cố gắng gọi setValue() từ một luồng khác, hệ thống sẽ báo lỗi này để ngăn chặn các vấn đề liên quan đến đồng bộ hóa và cập nhật giao diện người dùng không nhất quán.

             Giải thích:
             LiveData.setValue(): Phương thức này phải được gọi trên luồng chính.
             Nó sẽ cập nhật giá trị ngay lập tức và thông báo cho các quan sát viên (observers) trên luồng chính.
             Vấn đề với luồng nền: Khi bạn thực hiện các tác vụ nặng như xuất Excel trên một luồng nền
             (như pool-6-thread-1 được hiển thị trong log của bạn), bạn không thể trực tiếp thay đổi các thành phần UI hoặc LiveData liên quan đến UI bằng setValue().

             * Để khắc phục lỗi này, bạn cần sử dụng phương thức postValue() của MutableLiveData khi cập nhật
             * giá trị từ một luồng nền.
             */
//            isExporting.setValue(true);     //<-- Dòng này gây lỗi
//            exportStatus.setValue("Đang xuất file...");     // <-- Dòng này gây lỗi

            // Trong luồng nền của ExportViewModel hoặc một luồng khác
            //exportStatus.postValue("Đang xuất file...");
            exportStatus.postValue("PROCESSING...");
            isExporting.postValue(true); // <-- Sử dụng postValue() để cập nhật an toàn từ luồng nền



            Log.d(TAG, "Starting export...");

            // Lấy tableName từ SettingsRepository hoặc CrawlType
            CrawlType currentCrawlType = settingsRepository.getSelectedCrawlType();
            String tableThuoc = currentCrawlType.getTableThuocName();
            String excelFileName = currentCrawlType.getExcelFileName();
            // Tạo tên file duy nhất dựa trên thời gian(giữ nguyên logic đặt tên file)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());
            assert excelFileName != null;
            String ext = FileUtils.getFileExtension(excelFileName); // Đảm bảo xử lý cả .xml và xlsx
            excelFileName = excelFileName.replace(("." + ext), "_" + currentDateTime + ("." + ext));// Đảm bảo xử lý cả .xml
            //File fileExcel = new File(Utils.getExternalFilesDirThuocBietDuoc(applicationContext, false), excelFileName);  // excelFileName);

//            File fileExcel;
//            if (isXmlExport) {
//                String fileName = currentCrawlType.getExcelFileName().replace(".xlsx", ".xml"); // Đảm bảo đúng định dạng XML
//                fileExcel = new File(excelExporterOptimized.getExternalFilesDirThuocBietDuoc(), fileName);  // excelFileName);
//            } else {
//                String fileName = currentCrawlType.getExcelFileName();
//                fileExcel = new File(excelExporterOptimized.getExternalFilesDirThuocBietDuoc(), fileName);  // excelFileName);
//            }

            /// /////////////////////////////////////////////////////////////////////////
            //excelExporterOptimized thay thế cho exporter
            if (excelExporterOptimized == null) {
                excelExporterOptimized = new ExcelExporterOptimized_goc(Executors.newSingleThreadExecutor(), applicationContext);
            }

            // Chọn 3 cách: exportDataToExcelXlsx, exportToXlsxOptimized
            //excelExporterOptimized.exportDataToExcelXlsx(applicationContext,dbThuoc, tableThuoc, fileExcel,
            //        ExcelExporterOptimized.ExportCallbacks callBack);

            //1. pp 1: Xuuaats theo pp POI: Lỗi java.AWT
            // Thực hiện tác vụ xuất Excel trên luồng nền
            //ExcelExporterOptimized.exportDataToExcel(dbHelperThuoc, tableName, filePath, callbacks);
            //ExcelExporterOptimized.exportExcelSmart(isXmlExport, applicationContext, tableName, filePath, 4, callbacks);

            // Gán isXmlExport = true, tạm cho xuất xml
                //isXmlExport = true;
//            ExcelExporterOptimized_goc.exportExcelSmart(true, applicationContext, tableThuoc, excelFileName, 4,
//                    new ExcelExporterOptimized_goc.ExportCallbacks() { tỰ nHIÊN CHAY ĐƯỢC
//
            ExcelExporterOptimized.exportExcelSmart(true, applicationContext, tableThuoc, excelFileName, 4,
                    new ExcelExporterOptimized.ExportCallbacks() {
                        @Override
                        public void onExportStarted() { /* ... */ }

                        @Override
                        public void onExportProgress(int percent) {
                            progressPercent.postValue(percent);
                        }

                        @Override
                        public void onExportRecordsMaThuoc(String recordsOnTotalRecords, String mThuoc) {
                            recordOnRecods.postValue(recordsOnTotalRecords);
                            maThuoc.postValue(mThuoc);
                        }

                        @Override
                        public void onMessageLog(String message) {
                            messageLog.postValue(message);
                        }

                        @Override
                        public void onExportSuccess(String filePath) {
                            messageLog.postValue("Đã xuất Excel thành công!\nFile: " + filePath);
                            exportStatus.postValue("SUCCESS");
                            isExporting.postValue(false);
                        }

                        @Override
                        public void onExportFailure(String errorMessage) {
                            exportStatus.postValue("FAIL");
                            isExporting.postValue(false);
                            messageLog.postValue("Toast");

                        }
                    });
                    // end cũ

            //2. pp2: xuất XML
//            ExcelXmlExporter.exportMultiThread(applicationContext,
//                    new ExcelExporterOptimized.ExportCallbacks() {
//                        @Override
//                        public void onExportStarted() { /* ... */ }
//
//                        @Override
//                        public void onExportProgress(int percent) {
//                            progressPercent.postValue(percent);
//                        }
//
//                        @Override
//                        public void onExportRecordsMaThuoc(String recordsOnTotalRecords, String mThuoc) {
//                            recordOnRecods.postValue(recordsOnTotalRecords);
//                            maThuoc.postValue(mThuoc);
//                        }
//
//                        @Override
//                        public void onMessageLog(String message) {
//                            messageLog.postValue(message);
//                        }
//
//                        @Override
//                        public void onExportSuccess(String filePath) {
//                            messageLog.postValue("Đã xuất Excel thành công!\nFile: " + filePath);
//                            exportStatus.postValue("SUCCESS");
//                            isExporting.postValue(false);
//                        }
//
//                        @Override
//                        public void onExportFailure(String errorMessage) {
//                            exportStatus.postValue("FAIL");
//                            isExporting.postValue(false);
//
//                        }
//                    });
                    // end PP 2

                        // ChatGPT thêm 2 lệnh sau để làm gì?
                exportStatus.postValue("SUCCESS");
                isExporting.postValue(false);

        });


    }


//    // Phương thức mới để bắt đầu quá trình xuất, nhận Context
//    public void startExport(boolean isXmlExport, String exportPathFromActivity) { // Đổi tham số thành exportPathFromActivity
//        isExporting.setValue(true);
//        exportStatus.setValue("Đang xuất file...");
//        Log.d(TAG, "Starting export...");
//
//        // Lấy tableName từ SettingsRepository hoặc CrawlType
//        CrawlType currentCrawlType = settingsRepository.getSelectedCrawlType();
//        String tableThuoc = currentCrawlType.getOldTableThuocName();
//        String excelFileName = currentCrawlType.getExcelFileName();
//        // Tạo tên file duy nhất dựa trên thời gian(giữ nguyên logic đặt tên file)
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
//        String currentDateTime = sdf.format(new Date());
//        assert excelFileName != null;
//        String ext = FileUtils.getFileExtension(excelFileName); // Đảm bảo xử lý cả .xml và xlsx
//        excelFileName = excelFileName.replace(("." + ext), "_" + currentDateTime + ("." + ext));// Đảm bảo xử lý cả .xml
//
//        File fileExcel;
//        if (isXmlExport) {
//            String fileName = currentCrawlType.getExcelFileName().replace(".xlsx", ".xml"); // Đảm bảo đúng định dạng XML
//            fileExcel = new File(excelExporterOptimized.getExternalFilesDirThuocBietDuoc(), excelFileName);
//        } else {
//            String fileName = currentCrawlType.getExcelFileName();
//            fileExcel = new File(excelExporterOptimized.getExternalFilesDirThuocBietDuoc(), excelFileName);
//        }
//
//        //excelExporterOptimized thay thế cho exporter
//        if (excelExporterOptimized == null) {
//            excelExporterOptimized = new ExcelExporterOptimized(Executors.newSingleThreadExecutor(), applicationContext);
//        }
//
//        // Chọn 3 cách: exportDataToExcelXlsx, exportToXlsxOptimized
//        //excelExporterOptimized.exportDataToExcelXlsx(applicationContext,dbThuoc, tableThuoc, fileExcel,
//        //        ExcelExporterOptimized.ExportCallbacks callBack);
//
//        // CHỈNH SỬA: Phương thức mới để bắt đầu xuất Excel
//        public void exportToExcel(SQLiteDatabase db, String tableName, String filePath, ExcelExporterOptimized.ExportCallbacks callbacks) {
//            excelExecutor.execute(() -> {
//                // Thực hiện tác vụ xuất Excel trên luồng nền
//                ExcelExporterOptimized.exportDataToExcel(db, tableName, filePath, callbacks);
//            });
//        }
//
//
//        // Đảm bảo ExcelExporter và DBHelperThuoc đã được khởi tạo
////        if (exporter == null) {
////            exporter = new ExcelExporter(Executors.newSingleThreadExecutor(), applicationContext);
////        }
//
//        if (dbHelperThuoc == null) {
//            dbHelperThuoc = DBHelperThuoc.getInstance(applicationContext);
//        }
//
//        SQLiteDatabase database = dbHelperThuoc.getReadableDatabase();
//
//        // **LOGIC QUAN TRỌNG: TẠO ĐƯỜNG DẪN FILE MỚI**
//        File exportFile = null;
//        Uri fileUri = null;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // Android 10 (API 29) trở lên: Sử dụng MediaStore cho thư mục Downloads
//            ContentValues contentValues = new ContentValues();
//            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, isXmlExport ? "application/xml" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/YourAppNameExports"); // Thư mục con trong Downloads
//            fileUri = applicationContext.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
//
//            if (fileUri == null) {
//                onErrorCallback.accept("Không thể tạo file trong MediaStore.");
//                return;
//            }
//            // Lưu Uri vào LiveData để ExportProgressActivity có thể truy cập
//            // Bạn cần thêm một MutableLiveData<Uri> trong ViewModel
//            // Ví dụ: private final MutableLiveData<Uri> exportedFileUri = new MutableLiveData<>();
//            // Sau đó gọi: exportedFileUri.postValue(fileUri);
//        } else {
//            // Android 9 (API 28) trở xuống: Sử dụng thư mục riêng của ứng dụng
//            // Environment.DIRECTORY_DOCUMENTS có sẵn cho mục đích này.
//            //File dir = new File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "YourAppNameExports");
//            File dir = new File(String.valueOf(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)));
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//            exportFile = new File(dir, fileName);
//            // Lưu đường dẫn file vào LiveData nếu bạn muốn truyền nó
//            // Ví dụ: private final MutableLiveData<File> exportedFile = new MutableLiveData<>();
//            // Sau đó gọi: exportedFile.postValue(exportFile);
//        }
//
//        // Consumer cho progress callback
//        Consumer<ExportProgress> progressConsumer = progress -> {
//            new Handler(Looper.getMainLooper()).post(() -> {
//                progressPercent.setValue(progress.percent);
//                processedRecordsText.setValue(progress.message);
//                elapsedTimeText.setValue(formatTime(progress.elapsedTime));
//                remainingTimeText.setValue(formatTime(progress.remainingTime));
//            });
//        };
//
//        // Runnable cho onComplete callback
//        Runnable onCompleteCallback = () -> {
//            new Handler(Looper.getMainLooper()).post(() -> {
//                isExporting.setValue(false);
//                exportStatus.setValue("SUCCESS");
//                Log.d(TAG, "Export completed successfully.");
//
//                // Lưu đường dẫn file đã xuất thành công vào SharedPreferences
//                if (exportFile != null) {
//                    settingsRepository.saveLastExportPath(exportFile.getAbsolutePath());
//                } else if (fileUri != null) {
//                    // Đối với Uri, có thể lưu Uri.toString() nếu bạn cần phục hồi
//                    settingsRepository.saveLastExportPath(fileUri.toString());
//                }
//            });
//        };
//
//        // Consumer cho onError callback
//        Consumer<String> onErrorCallback = errorMessage -> {
//            new Handler(Looper.getMainLooper()).post(() -> {
//                isExporting.setValue(false);
//                exportStatus.setValue("Lỗi: " + errorMessage);
//                Log.e(TAG, "Export failed: " + errorMessage);
//            });
//        };
//
//        // Truyền filePath hoặc Uri (ExportExporter cần được điều chỉnh để nhận Uri hoặc OutputStream)
//        // Đây là điểm cần điều chỉnh trong ExcelExporter
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                // Truyền Uri và Context để ExcelExporter tự mở OutputStream
//                exporter.exportWithUri(database, tableName, fileUri, isXmlExport, progressConsumer, onCompleteCallback, onErrorCallback);
//            } else {
//                // Truyền FilePath như cũ
//                if (isXmlExport) {
//                    exporter.exportToExcelXml(database, tableName, exportFile.getAbsolutePath(), progressConsumer, onCompleteCallback, onErrorCallback);
//                } else {
//                    exporter.exportDataToExcelXlsx(database, tableName, exportFile.getAbsolutePath(), progressConsumer, onCompleteCallback, onErrorCallback);
//                }
//            }
//        } catch (Exception e) {
//            onErrorCallback.accept("Lỗi khởi tạo xuất file: " + e.getMessage());
//        }
//    }

    // Thêm các LiveData để truyền đường dẫn file đã xuất ra Activity
    private final MutableLiveData<File> exportedFile = new MutableLiveData<>();
    private final MutableLiveData<Uri> exportedFileUri = new MutableLiveData<>();

    public LiveData<File> getExportedFile() {
        return exportedFile;
    }

    public LiveData<Uri> getExportedFileUri() {
        return exportedFileUri;
    }

    // CHỈNH SỬA: Loại bỏ các phương thức WorkManager không liên quan
    /*
    public LiveData<List<WorkInfo>> getWorkInfoLiveData()
    {
        return workInfoLiveData;
    }
    public void startWorkCreateExcel(Data data) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(MyWorkerCrawler.class)
                .addTag("WorkerCreatExcel")
                .setInputData(data)
                .setConstraints(constraints)
                .build();
        workManager.enqueueUniqueWork(
                "sendLogsCreatExcel",
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest);
    }
    */

    @Override
    protected void onCleared() {
        super.onCleared();
        // CHỈNH SỬA: Đảm bảo shutdown ExecutorService khi ViewModel bị xóa
        if (excelExecutor != null && !excelExecutor.isShutdown()) {
            excelExecutor.shutdownNow();
        }
    }
    /// ////////////

    // Hàm định dạng thời gian từ milliseconds sang phút:giây
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

//    @Override
//    public void onProgressUpdate(int percent, String message, long elapsedTime, long remainingTime) {
//
//    }

//    @Override
//    public void onExportComplete(boolean success, String path, String errorMessage, long totalTime) {
//
//    }
}

