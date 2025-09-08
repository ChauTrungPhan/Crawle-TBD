// File: MyViewModelCreateExcel_Gemini.java (Cập nhật)
package com.example.crawlertbdgemini2modibasicview;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyViewModelCreateExcel_Gemini extends AndroidViewModel {
    private static final String TAG = "MyViewModelCreateExcel"; // CHỈNH SỬA: Thêm TAG
    private final WorkManager workManager;
    private final LiveData<List<WorkInfo>> workInfoLiveData;
    private final ExecutorService excelExportExecutor; // CHỈNH SỬA: ExecutorService cho tác vụ xuất Excel
    private final ExcelExporterOptimized_goc excelExporterOptimized;

    public MyViewModelCreateExcel_Gemini(@NonNull Application application, ExcelExporterOptimized_goc excelExporterOptimized) {
        super(application);
        workManager = WorkManager.getInstance(application);
        this.excelExporterOptimized = excelExporterOptimized;
        workInfoLiveData = workManager.getWorkInfosByTagLiveData("CrawlerWorker"); // Tag này có thể cần thay đổi nếu WorkManager cho Excel có tag riêng
        excelExportExecutor = Executors.newSingleThreadExecutor(); // CHỈNH SỬA: Khởi tạo Executor
    }

    public LiveData<List<WorkInfo>> getWorkInfoLiveData() {
        return workInfoLiveData;
    }

    // CHỈNH SỬA: Phương thức startWorkCreateExcel hiện tại đang gọi MyWorkerCrawler,
    // Nếu bạn muốn dùng Worker cho Excel, cần tạo MyWorkerExcel.class riêng.
    // Nếu không, có thể bỏ qua hoặc đổi tên để tránh nhầm lẫn.
    public void startWorkCreateExcel(Data data) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // CHỈNH SỬA: Nếu bạn muốn xuất Excel bằng WorkManager, bạn cần một Worker Class riêng cho nó.
        // Ví dụ: OneTimeWorkRequest.Builder(MyWorkerExcelExport.class)
        // Hiện tại, bạn đang gọi MyWorkerCrawler.class, điều này có thể không đúng mục đích.
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(MyWorkerCrawler.class) // Lỗi tiềm ẩn nếu đây không phải Worker cho Excel
                .addTag("WorkerCreatExcel")
                .setInputData(data)
                .setConstraints(constraints)
                .build();

        workManager.enqueueUniqueWork(
                "sendLogsCreatExcel",
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest);
    }

    // CHỈNH SỬA: Thêm phương thức exportToExcel
    public void exportToExcel(SQLiteDatabase db, String tableName, String filePath, ExcelExporterOptimized_goc.ExportCallbacks callbacks) {
        // Chạy tác vụ xuất Excel trên luồng nền
        excelExportExecutor.execute(() -> {
            try {
                // CHỈNH SỬA: Gọi phương thức static của ExcelExporterOptimized
                // Đây là nơi logic xuất Excel thực sự diễn ra
                File file = new File(filePath);
                excelExporterOptimized.exportDataToExcelXlsx(getApplication().getApplicationContext(), db, tableName, file, callbacks);
            } catch (Exception e) {
                Log.e(TAG, "Error exporting Excel from ViewModel: " + e.getMessage(), e);
                if (callbacks != null) {
                    //callbacks.onExportFailure("Lỗi khi xuất Excel từ ViewModel: " + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // CHỈNH SỬA: Đảm bảo tắt ExecutorService khi ViewModel bị hủy
        if (excelExportExecutor != null && !excelExportExecutor.isShutdown()) {
            excelExportExecutor.shutdownNow();
        }
    }
}