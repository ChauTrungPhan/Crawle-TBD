package com.example.crawlertbdgemini2modibasicview;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.crawlertbdgemini2modibasicview.utils.AppKeys;
import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MyViewModelCrawler_Gemini extends AndroidViewModel {
    private static final String TAG = "MyViewModelCrawler_Gemini";

    ///////
    // Của Workmanager
    private final WorkManager workManager;
    /// /
    //private final AppDatabase appDatabase;
    //private final SettingsRepository settingsRepository;
    //private final CrawlType selectedCrawlType;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(); // Executor cho các thao tác Room DB
    // LiveData for WorkManager WorkInfo, dynamically updated via switchMap
    //private final LiveData<WorkInfo> workInfoLiveData;

    // LiveData to hold the current WorkRequest ID (persisted in Room DB)
    //private final MutableLiveData<String> currentWorkRequestIdLiveData = new MutableLiveData<>();
    // LiveData for UI updates related to export progress
    //private final MutableLiveData<Integer> exportProgress = new MutableLiveData<>(0);
    //private final MutableLiveData<String> exportStatusText = new MutableLiveData<>("");
    //private final MutableLiveData<Boolean> isExporting = new MutableLiveData<>(false);
    //private final MutableLiveData<Long> totalRecordsCount = new MutableLiveData<>();
    // Observer for workInfoLiveData to handle state changes within ViewModel
    private Observer<WorkInfo> internalWorkInfoObserver;

    // Getters for LiveData to be observed by UI components
    //public LiveData<WorkInfo> getWorkInfoLiveData() { return workInfoLiveData; }
    //public LiveData<Integer> getExportProgress() { return exportProgress; }
    //public LiveData<String> getExportStatusText() { return exportStatusText; }
    //public LiveData<Boolean> getIsExporting() { return isExporting; }
    //public LiveData<String> getCurrentWorkRequestIdLiveData() { return currentWorkRequestIdLiveData; }
    //public LiveData<Long> getTotalRecordsCount() { return totalRecordsCount; }
    public MyViewModelCrawler_Gemini(@NonNull Application application) {
        super(application);
        // Mới
       this.workManager = WorkManager.getInstance(application);
        //
        //this.settingsRepository = new SettingsRepository(application);
        //this.selectedCrawlType = MainActivity.getSelectedCrawlType();
        // Khởi tạo Room Database và DAO
//        appDatabase = Room.databaseBuilder(application.getApplicationContext(),
//                        AppDatabase.class, "crawler_tbd_db_room")
//                .fallbackToDestructiveMigration() // Sử dụng migration phá hủy cho dev
//                .build();

        // Load workRequestId (đã lưu (nếu có)) khi ViewModel được tạo (để khôi phục trạng thái)
        //loadWorkRequestId();

        // Quan sát WorkInfo bằng ID đã lưu
        // Sử dụng Transformations.switchMap để theo dõi(to observe WorkInfo based on the currentWorkRequestIdLiveData)
//        workInfoLiveData = Transformations.switchMap(currentWorkRequestIdLiveData, workRequestIdString -> {
//            if (workRequestIdString != null && !workRequestIdString.isEmpty()) {
//                Log.d(TAG, "MyViewModelCrawler_Gemini: WorkManage Tự chạy lại: TRUE ") ;
//                try {
//                    UUID workId = UUID.fromString(workRequestIdString);
//                    Log.d(TAG, "switchMap: Observing workInfo for ID = " + workId);
//                    return workManager.getWorkInfoByIdLiveData(workId);
//                } catch (IllegalArgumentException e) {
//                    Log.e(TAG, "switchMap: Invalid UUID string: " + workRequestIdString, e);
//                    return new MutableLiveData<>(null);
//                }
//            } else {
//                Log.d(TAG, "MyViewModelCrawler_Gemini: WorkManage  KHÔNG Tự chạy lại, PHIÊN TRƯỚC ĐÃ SUCCEDED! ") ;
//                return new MutableLiveData<>(null);
//            }
//        });

        // Set up an internal observer for workInfoLiveData to react to state changes
//        internalWorkInfoObserver = workInfo -> {
//            if (workInfo != null) {
//                Log.d(TAG, "WorkInfo changed: State = " + workInfo.getState() + ", Progress = " + workInfo.getProgress().getInt(AppConstants.WORK_PROGRESS_PERCENT, 0));
//
//                if (workInfo.getState() == WorkInfo.State.RUNNING || workInfo.getState() == WorkInfo.State.ENQUEUED) {
//                    int progress = workInfo.getProgress().getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
//                    exportProgress.postValue(progress);
//                    String statusMessage = workInfo.getProgress().getString(AppConstants.WORK_STATUS_MESSAGE);
//                    if (statusMessage != null && !statusMessage.isEmpty()) {
//                        exportStatusText.postValue(statusMessage);
//                    } else {
//                        exportStatusText.postValue("Đang crawl: " + progress + "%");
//                    }
//                    isExporting.postValue(true);
//                } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                    // xEM TẠI SAO MAINACTIVITY CÓ WorkInfo.state = FAIL
//                    int stop = 0;
//                    ///
//                    exportProgress.postValue(100);
//                    exportStatusText.postValue("Crawl hoàn tất!");
//                    isExporting.postValue(false);
//                    // Reset counters after completion. KHÔNG RESET ĐỂ STARTCRAWL CHẠY PHIN MỚI HOÀN TOÀN MỚI RESET
//                    clearWorkRequestId(); // Clear workRequestId when succeeded
//
//                    //CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();
//                    settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(), 0);
//                    settingsRepository.saveTotalUrls(selectedCrawlType.getTotalUrlsPrefKey(), 0);
//                    settingsRepository.saveElapsedTime(0);
//                    loadTotalRecordsCount(); // Reload total records after successful crawl
//                } else if (workInfo.getState() == WorkInfo.State.FAILED || workInfo.getState() == WorkInfo.State.CANCELLED) {
//                    String errorMessage = workInfo.getOutputData().getString("message"); // Assuming worker sends error message
//                    if (errorMessage != null && !errorMessage.isEmpty()) {
//                        exportStatusText.postValue("Crawl bị lỗi: " + errorMessage);
//                    } else {
//                        exportStatusText.postValue("Crawl bị lỗi hoặc đã hủy.");
//                    }
//                    isExporting.postValue(false);
//                    exportProgress.postValue(0);
//                    // Reset counters. KHÔNG RESET ĐỂ STARTCRAWL CHẠY PHIN MỚI HOÀN TOÀN MỚI RESET
//                    clearWorkRequestId(); // Clear workRequestId when failed or cancelled
//
//                    //CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();
//                    settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(), 0);
//                    settingsRepository.saveTotalUrls(selectedCrawlType.getTotalUrlsPrefKey(), 0);
//                    settingsRepository.saveElapsedTime(0);
//                }
//            } else {
//                // WorkInfo is null, perhaps work was never enqueued or already cleared
//                exportStatusText.postValue("Chưa có crawl nào được bắt đầu hoặc đã hoàn tất.");
//                isExporting.postValue(false);
//                exportProgress.postValue(0);
//            }
//        };
//
//        // Attach the internal observer
//        workInfoLiveData.observeForever(internalWorkInfoObserver);
        //

        // Load crawl configuration (and initial total records count)
        //loadCrawlConfiguration();

    }   // End constructor

    // Quan sát trạng thái worker
    public LiveData<List<WorkInfo>> getWorkInfoLiveData() {
        return workManager.getWorkInfosForUniqueWorkLiveData(AppConstants.UNIQUE_WORK_NAME);
    }


    // Mới
    // Mới Gemini
    public void startCrawler(boolean tiepTuc, String customCrawlString) {
        // Hủy bỏ bất kỳ công việc đang chạy nào trước khi bắt đầu công việc mới
        //stopCrawler();
        // Hoặc code sau: Hủy bỏ bất kỳ công việc nào đang chạy trước đó
        //workManager.cancelUniqueWork(UNIQUE_WORK_NAME);
        //settingsRepository.clearWorkRequestId(selectedCrawlType.getWorkIdPrefKey()); // Xóa ID công việc cũ


        // Lấy CrawlType từ tên
        //CrawlType selectedCrawlType = CrawlType.valueOf(crawlTypeName);

        // Chuẩn bị InputData cho Worker
        Data inputData = new Data.Builder()
                .putBoolean("TIEP_TUC", tiepTuc)
                //.putString(AppConstants.CRAWL_TYPE, selectedCrawlType.name())
                .putString(AppConstants.CUSTOM_CRAWL_STRING, customCrawlString)
                .build();

        // Tạo WorkRequest
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                //.setRequiresCharging(false) // Tùy chọn: chỉ chạy khi sạc
                //.setRequiresDeviceIdle(false) // Tùy chọn: chỉ chạy khi thiết bị rảnh
                .build();

        OneTimeWorkRequest crawlRequest = new OneTimeWorkRequest.Builder(MyWorkerCrawler.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // ưu tiên
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag("CRAWL_WORK") // Thêm tag để dễ dàng quản lý
                //text = "Thử lại sau ~30 giây": NẾU ĐỨT MẠNG
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();

        // Gửi WorkRequest đến WorkManager và lưu ID: Enqueue the unique work and save its ID
        workManager.enqueueUniqueWork(
                AppConstants.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                crawlRequest);
        // Lưu lại crawlRequestId: hiện tại đang chạy
        //saveWorkRequestId(crawlRequest.getId());
        Log.d("MyViewModelCrawler_Gemini", "Crawl request enqueued with ID: " + crawlRequest.getId());

        // Update WorkState in Room DB: Lưu trạng thái vào Room Database
//        ioExecutor.execute(() -> {
//            WorkState workState = new WorkState();
//            workState.workIdPrefKey = selectedCrawlType.getWorkIdPrefKey();
//            workState.workId = crawlRequest.getId();
//            workState.state = WorkInfo.State.ENQUEUED.name();   // Initial state when saved
//            workState.progress = 0;
//
//            workState.message = "Crawl started";
//            workState.startTime = System.currentTimeMillis();
//            appDatabase.workStateDao().insert(workState);
//        });
    }

    public void stopCrawler() {
        try {
            workManager.cancelUniqueWork(AppConstants.UNIQUE_WORK_NAME);
        } catch (IllegalArgumentException e){
            Log.d(TAG, "No active work to stop!:" + e.getMessage());
        }

                //
//        String currentIdString = currentWorkRequestIdLiveData.getValue();   // Chú ý null
//        if (currentIdString != null && !currentIdString.isEmpty()) {
//            try {
//                UUID currentId = UUID.fromString(currentIdString);
//                workManager.cancelWorkById(currentId);
//                // Trong Trường hơp này có thẻ hủy hết vì chỉ có 1 Manager chạy
////                workManager.cancelAllWork();
//                Log.d(TAG, "Cancelled work with ID: " + currentId);
//                // DO clearWorkRequestId NÀY MÀ KHI APP KHỎI ĐỘNG LẠI KHÔNG LẤY ĐƯỢC currentWorkRequestIdLiveData?
//                //clearWorkRequestId(); // Clear ID after cancellation
//            } catch (IllegalArgumentException e) {
//                Log.e(TAG, "Error parsing UUID for stopping crawler: " + currentIdString, e);
//            }
//        } else {
//            Log.d(TAG, "No active work to stop.");
//        }

    }

}   // end của Workmanager