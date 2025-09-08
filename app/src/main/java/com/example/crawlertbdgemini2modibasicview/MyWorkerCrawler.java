package com.example.crawlertbdgemini2modibasicview;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.navigation.internal.AtomicInt;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.crawlertbdgemini2modibasicview.utils.SharedPreferencesUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.example.crawlertbdgemini2modibasicview.utils.CrawlType; // Import CrawlType
import com.example.crawlertbdgemini2modibasicview.utils.PrefKey; // Import PrefKey
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository; // Import SettingsRepository

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MyWorkerCrawler extends Worker {
    private static final String TAG = "MyWorkerCrawler";
    // XEM
    AtomicInteger numPacket = new AtomicInteger(0);
    //END XEM
    private static final Logger log = LogManager.getLogger(MyWorkerCrawler.class);
    private final DBHelperThuoc dbHelperThuoc; // Giữ tham chiếu đến Singleton DBHelperThuoc
    private static final int NOTIFICATION_ID = 1001;
    private final ConcurrentSkipListSet<String> visitedUrls; // Sử dụng ConcurrentSkipListSet:  URLs đã ghé thăm (bao gồm cả đang xử lý và đã hoàn thành)
    private final ConcurrentSkipListSet<String> completedUrlsChild; // URLs con đã hoàn thành
    private final SettingsRepository settingsRepository;
    private long totalUrlsToCrawl = 0; // Tổng số URL cần cào
    private CrawlType selectedCrawlType;
    private String currentTableThuocName;
    private String customCrawlString; // Chuỗi crawl tùy chỉnh (nếu có)
    private final AtomicLong lastUpdateTime = new AtomicLong(0);      // Biến thời gian nguyên tử
    private static final long UI_UPDATE_INTERVAL_MS = 100; // Cập nhật mỗi 100ms-500ms
    private static int THRESHOLD = 40; // hoặc 50; hoặc 40 (Ngưỡng chia nhỏ listUrls cho ForkJoinPool)
    private final ReentrantLock lock = new ReentrantLock();
    ///demProgress: PHẢI GLOBAL
    private final Context context;    //context là Application
    public static final String PREFS_SETTINGS = "prefs_settings"; // Tên SharedPreferences để lưu trữ thông tin crawl
    public static final String KEY_LIST_ROW_URLS_JSON = "list_row_urls_json"; // Key cho JSON string
    public static final String PARALLELISM = "parallelism"; // Key cho tổng số URL đang
    private final String initUrlsTable="";
    private String tableThuoc="";
    private AtomicLong crawledUrlStar1Count; // Bộ đếm an toàn cho số URL CÓ "start=1" đã xử lý
    private AtomicLong crawledUrlCount; // Bộ đếm an toàn bộ cho số URL đã xử lý crawl (bao gồm url cha, url con);
    private static final ConcurrentSkipListSet<String> urlLast = new ConcurrentSkipListSet<>();
    private static final AtomicInteger completedTasks = new AtomicInteger(0);   //Dể xem
    private final ConcurrentHashMap<String, Boolean> maThuocDaLay = new ConcurrentHashMap<>();    //Mã thuốc trùng
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    String lastCurrentUrl;
    int percent;    // Phải toàn cầu
    long currentCumulativeElapsedTime;
    long estimatedRemainingTime;
    // Khai báo biến này ở cấp class trong Worker của bạn (hoặc bên ngoài doWork() nhưng trong class)
    private long currentSessionStartTime;
    private long lastSavedCumulativeElapsedTime;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    //
    public MyWorkerCrawler(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        this.context = context.getApplicationContext();
        lastUpdateTime.set(0);      // Tránh bị lưu giũ giá trị của phiên trước: CẦN THIẾT KHÔNG?
        // Cập nhật MyWorkerCrawler (Sử dụng CrawlType và SettingsRepository)
        // Mới
        // Lấy thể hiện Singleton của DBHelperThuoc
        this.dbHelperThuoc = DBHelperThuoc.getInstance(context);
        // XÓA DÒNG NÀY: Không cần lấy dbThuoc ở đây và giữ tham chiếu lâu dài
        // this.dbThuoc = dbHelperThuoc.getWritableDatabase();  // LOẠI BỎ DÒNG NÀY!

        // ... (khởi tạo các biến khác)

        settingsRepository = new SettingsRepository(context);

        visitedUrls = new ConcurrentSkipListSet<>();
        completedUrlsChild = new ConcurrentSkipListSet<>();
        //urlInfoQueueList = new ConcurrentLinkedQueue<>();
        // KHỞI TẠO selectedCrawlType Ở ĐÂY!
        //this.selectedCrawlType = MainActivity.getSelectedCrawlType();        // HOẶC settingsRepository.getSelectedCrawlType(); // <-- THÊM DÒNG NÀY
        // tránh phụ thuộc vào trạng thái tĩnh của MainActivity, nên dùng
        //this.selectedCrawlType = settingsRepository.getSelectedCrawlType(); // PHẢI CÓ ĐỂ selectedCrawlType KHÔNG null
        this.selectedCrawlType = settingsRepository.getSelectedCrawlType();
        if (this.selectedCrawlType == null) {
            this.selectedCrawlType = CrawlType.TWO_CHAR; // Mặc định
        }
        tableThuoc = this.selectedCrawlType.getTableThuocName();
        //urlInfoQueueList: có thể là List <String[]> : List<String[]> hoặc ArrayList
        //private final ConcurrentLinkedQueue<String> urlInfoQueueList; // Hàng đợi URL cần xử lý
        WorkStateDao workStateDao = AppDatabase.getInstance(context).workStateDao(); // Khởi tạo WorkStateDao
        crawledUrlCount = new AtomicLong(0);    //crawledUrlCount=whichProcessedUrlsStart1_count
        /// ///
        // Tải URL đã hoàn thành từ DB khi Worker khởi tạo
        // THAY THẾ DÒNG NÀY:
        //loadCompletedUrls(visitedUrls, dbHelperThuoc.getWritableDatabase());
        // BẰNG DÒNG NÀY:
        //loadCompletedUrls(visitedUrls); // Gọi phương thức đã sửa đổi

        Log.d(TAG, "Loaded " + visitedUrls.size() + " completed URLs.");

        // Trong constructor:
        // ...
        // Thay thế dòng này:
        // private static final String CHANNEL_ID = "crawler_channel";
        // Bằng cách truy cập CHANNEL_ID từ MyApplication:
        // CHANNEL_ID được sử dụng trong constructor của MyWorkerCrawler không còn cần thiết nếu bạn đã sử dụng nó từ MyApplication
        // Đảm bảo notificationBuilder sử dụng ID nhất quán
        notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context, MyApplication.CRAWLER_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_build_24) // Xem lại: ĐÃ KHỞI CHẠY TRONG MyApplication
                .setContentTitle("Important background job");


        // end Cũ

    } // End Constructor
    ////////////////////
    /* https://www.simplifiedcoding.net/android-workmanager-tutorial/
     * This method is responsible for doing the work
     * so whatever work that is needed to be performed
     * we will put it here
     *
     * For example, here I am calling the method displayNotification()
     * It will display a notification
     * So that we will understand the work is executed
     * */
// Helper method để lấy List<String[]> từ inputData
    private List<String[]> getUrlsFromInputData() {
        String urlsJson = getInputData().getString(KEY_LIST_ROW_URLS_JSON);
        if (urlsJson == null || urlsJson.isEmpty()) {
            Log.e("MyWorkerCrawler", "No URL data found in inputData.");
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        // Đây là cách đúng để deserialize một List của một kiểu phức tạp (như String[])
        Type type = new TypeToken<List<String[]>>() {}.getType();
        return gson.fromJson(urlsJson, type);
    }

    // Trong lớp CrawlWorker của bạn

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: MyWorkerCrawler started. Initial lastUpdateTime: " + lastUpdateTime.get());

        totalUrlsToCrawl = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey()); // HOẶC = k1-k0
        //totalUrlsToCrawl = urlInfoQueueList.size();
        // hoặc : PHAI LẤY TỪ LƯU VÌ KHI THOÁT ĐỘT NGỘT: TẠI SAO =0? AI SET = 0?:
        // XẢY RA TR0NG internalWorkInfoObserver của MyViewModelCrawler_Gemini

        // lẤY TỪ k0, k1 đã lưu trong prefs
//        totalUrlsToCrawl = settingsRepository.getK1(selectedCrawlType.getK1PrefKey(),"")
//                - settingsRepository.getK0(selectedCrawlType.getK0PrefKey());

        // 1️⃣ Bật foreground service NGAY TỪ ĐẦU: setForegroundAsync: Chạy trong doWork càng sớm càng tốt
        //createNotificationChannel();    // Đã được khai báo và khởi động ở MyApplication
        setForegroundAsync(createForegroundInfo(0, totalUrlsToCrawl, null, "")); // "Đang khởi tạo..."
        //setForegroundAsync(createForegroundInfo(0, "Đang chuẩn bị crawl..."));
        // 2️⃣ Bỏ qua tối ưu pin nếu chưa bật
        requestIgnoreBatteryOptimizations();

        // Luôn khởi tạo startTime ở đầu mỗi lần doWork() được gọi
        // Điều này quan trọng nếu Worker được retry
        //startTime = System.currentTimeMillis();
        // 1. Lấy tổng thời gian đã trôi qua từ phiên trước đó (nếu có)
        // Giả sử settingsRepository của bạn có phương thức getLong cho SharedPreferences
        lastSavedCumulativeElapsedTime = settingsRepository.getLongElapsedTime(selectedCrawlType);

        // 2. Lưu thời gian bắt đầu của phiên Worker HIỆN TẠI
        currentSessionStartTime = System.currentTimeMillis();


        //try {   // Có thể không cần
            Log.d(TAG, "doWork: started.");
            // Gọi mạng....

            // 1. Kiểm tra trạng thái cờ HAS_CRAWL_STARTED
            boolean hasCrawlStartedBefore = SharedPreferencesUtils.getHasCrawlStarted(context);
            Log.d(TAG, "HAS_CRAWL_STARTED flag: " + hasCrawlStartedBefore);

            // ... (Logic khởi tạo và kiểm tra ban đầu)
            // Khởi tạo loại crawl từ SettingsRepository
            currentTableThuocName = selectedCrawlType.getTableThuocName();
        //getUrlQueueTableName. THAY THẾ CHO TÊN: initUrlsTable
        //String urlQueueTableName = selectedCrawlType.getUrlQueueTableName();

            // 2. Lấy danh sách các URL đang chờ xử lý từ DB (STATUS = 0) và biến urlInfoQueueList
            List<UrlInfo> urlInfoQueueList;
            boolean tiepTuc = getInputData().getBoolean("TIEP_TUC", false);

            if (!hasCrawlStartedBefore) {   // Đây là lần đầu tiên chạy hoặc sau khi reset hoàn toàn (chưa có URL nào trong DB)
                // Đây là lần đầu tiên chạy hoặc sau khi reset hoàn toàn (chưa có URL nào trong DB)
                Log.d(TAG, "Initial crawl: Database is empty. Seeding initial URLs.");
                // Cần lấy danh sách URL ban đầu (allInitialUrls) từ đâu đó (ví dụ: MainActivity)
                // Hoặc nó được lưu trong một Preference khác hoặc một file assets
                // 1. Khôi phục visitedUrls (tất cả các URL đã hoàn thành từ các phiên trước)
                // Đây là cách bạn tránh cào lại các URL đã hoàn thành, ngay cả sau khi app bị tắt.
                if (!tiepTuc) { // KHÔNG TIẾP TUC, CHẠY PHIÊN MỚI HOÀN TOÀN
                    // Lấy lại từ đầu theo k0,k1 (Phải delete All records bảng Queue)
                    urlInfoQueueList = dbHelperThuoc.getListQueueUrls(settingsRepository);    //getListQueueUrlFromCrawlType
                    // Cập nhật tổng số URL ban đầu: KHI LẤY MỚI DỰA THEO k0, k1. CẦN KHÔNG: VÌ totalURL = k1 - k0
                    settingsRepository.saveTotalUrls(selectedCrawlType.getTotalUrlsPrefKey(), urlInfoQueueList.size());
                    // Các thông số khác xem như =0, hoặc null

                    // Đặt cờ là đã bắt đầu crawl
                    SharedPreferencesUtils.setHasCrawlStarted(context, true);
                } else {    //true
                    //Lấy lại thông số cũ
                    List<UrlInfo> completedUrlInfos = dbHelperThuoc.getDetailedUrlInfoByStatus(selectedCrawlType.getUrlQueueTableName(), 1);
                    for (UrlInfo urlInfo : completedUrlInfos) {
                        visitedUrls.add(urlInfo.getUrl());
                    }
                    Log.d(TAG, "Restored " + completedUrlInfos.size() + " completed URLs to visited set.");

                    urlInfoQueueList = dbHelperThuoc.getDetailedUrlInfoByStatus(selectedCrawlType.getUrlQueueTableName(), 0);

                }

            } else { // Worker tự động chạy khi app khởi động lại (app bị thoát đột ngột, bị hệ thống kill
                // Lấy lại các thông số cũ
                List<UrlInfo> completedUrlInfos = dbHelperThuoc.getDetailedUrlInfoByStatus(selectedCrawlType.getUrlQueueTableName(), 1);
                for (UrlInfo urlInfo : completedUrlInfos) {
                    visitedUrls.add(urlInfo.getUrl());
                }
                Log.d(TAG, "Restored " + completedUrlInfos.size() + " completed URLs to visited set.");

                urlInfoQueueList = dbHelperThuoc.getDetailedUrlInfoByStatus(selectedCrawlType.getUrlQueueTableName(), 0);
                Log.d(TAG, "Found " + urlInfoQueueList.size() + " pending URLs from DB.");
                if (urlInfoQueueList.isEmpty()) {
                    Log.d(TAG, "No pending URLs in DB. Crawl might be complete or needs initial seeding.");
                    // Logic cho lần chạy đầu tiên hoặc khi hoàn thành (SUCCEED):
                    // Bạn cần kiểm tra một cờ trong SharedPreferences (ví dụ: HAS_CRAWL_STARTED)
                    // Nếu chưa từng start, thì đây là lúc nạp các URL ban đầu vào DB và khởi tạo pendingUrlInfos
                    // (Như đã thảo luận ở phần "StartCrawl" của người dùng)
                    // ... (Logic kiểm tra và chèn URL ban đầu nếu cần) ...
//                if (urlInfoQueueList.isEmpty()) { // Nếu vẫn rỗng sau khi thử seed
//                    Log.d(TAG, "Crawl finished successfully or no initial URLs to process.");
                    return Result.success();
//                }

                }

            }

//            startTime = System.currentTimeMillis(); // đẠT CHỖ NÀY?
//            Log.d(TAG, "doWork started for " + selectedCrawlType.name());
            // Mới
            // Khởi tạo số lượng lấy từ prefs đã lưu
                    // a/ URL (start=1) đã cào thành công từ SharedPreferences của phiên trước
            crawledUrlStar1Count = new AtomicLong(settingsRepository.getCrawledUrlsStart1Count(
                    selectedCrawlType.getCrawledUrlStart1sCountPrefKey()));
            // XEM: KIỂM TRA SỐ crawledUrlStar1Count CCOS ĐÚNG KHÔNG? TAI SAO TỔNG crawledUrlStar1Count <> tOtAL
                    // b/ Tổng số url chung đã xử lý (cào) được
            crawledUrlCount = new AtomicLong(settingsRepository.getSumCrawledUrlsCount(
                    selectedCrawlType.getCrawledUrlsCountPrefKey()
            ));
            // Tính lại từ đầu percent
            percent = (int) (crawledUrlStar1Count.get() * 100.0 / (totalUrlsToCrawl==0?1:totalUrlsToCrawl));
            long finalCumulativeElapsedTime = 0; // Thay thế finalElapsedTime bằng finalCumulativeElapsedTime

        try {
            // Chạy trước để ghi data vào sqlite: THẤT BẠI DO 2 LỆNH NÀY
            CrawlRuntime runtime = new CrawlRuntime(getApplicationContext());
            runtime.start();

            // 3. Khởi tạo ForkJoinPool
            Log.d(TAG, "doWork: Total URLs to process (from initUrlsTable excluding completed): " + urlInfoQueueList.size());
            Log.d(TAG, "doWork: Starting ForkJoinPool with " + Runtime.getRuntime().availableProcessors() + " processors.");
            ForkJoinPool forkJoinPool = new ForkJoinPool();

            //try {
            if (!urlInfoQueueList.isEmpty()) {    // Đã kiểm soat ở trên
                // Trước khi chạy forkJoinPool.submit: CẦN KHÔNG?
                if (isStopped()) {
                    isCancelled.set(true);
                    Log.d("MyWorkerCrawler", "Work cancelled before execution: " + getId());
                    return Result.failure();
                }

                // 1/ Cũ
                //forkJoinPool.invoke(new UrlCrawlRecursiveAction(urlInfoQueueList));// cỦA GEMINI

                        forkJoinPool.invoke(new CrawlRecursiveAction(urlInfoQueueList, 0, urlInfoQueueList.size(),
                                crawledUrlStar1Count, runtime, isCancelled)); // truyền dbHelperThuoc thay vì dbThuoc

                forkJoinPool.shutdown();
                //forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                runtime.stop();
                Log.d(TAG, "doWork: ForkJoinPool shut down.");

                //2. Mới; ĐỂ NHẬN LỖI THROW RA
                // Thay invoke = submit
                // CHÚ Ý: KHÔNG LẤY END (LẤY TỪ START ĐẾN < END)
//                        Future<?> future = forkJoinPool.submit(new CrawlRecursiveAction(urlInfoQueueList, 0, urlInfoQueueList.size(),
//                                crawledUrlStar1Count,
//                                dbHelperThuoc, selectedCrawlType, isCancelled )); // truyền dbHelperThuoc thay vì dbThuoc

                /// /////////
//                ForkJoinTask<?> task = forkJoinPool.submit(new CrawlRecursiveAction(urlInfoQueueList, 0, urlInfoQueueList.size(),
//                        crawledUrlStar1Count, runtime, isCancelled)); // truyền dbHelperThuoc thay vì dbThuoc
//                try {   // Cần không?
//                    // Giám sát: cancel nếu WorkManager yêu cầu dừng
//                    while (!task.isDone()) {
//                        if (isStopped()) {
//                            // Giám sát: cancel nếu WorkManager yêu cầu dừng
//                            isCancelled.set(true);
//                            task.cancel(true);  // gửi cancel tới task
//                            forkJoinPool.shutdownNow(); // Cố gắng DỪNG NGAY LẶP TỨC: Gửi interrupt tới các thread pool
//                            Log.d("MyWorkerCrawler", "Work cancelled during execution: " + getId());
//                            // Lưu trạng thái tổng quan nếu muốn
//                            return Result.failure();
//                        }
//                        Thread.sleep(150);  // Nên nhẹ nhàng, 100-300ms hợp lý
//                    }
//
//                    // Đã done → join để ném exception nếu có
//                    try { // Cần . try-CATCH: CHỦ YẾU Ở ĐÂY ĐỂ BẮT LỖI
//                        // chờ toàn bộ task crawl hoàn tất: task.join() hay task.get)
//                        //future.get();
//                        //task.join(); // ném RuntimeException nếu compute() throw. TẠI SAO VẪN CÒN THRED CHẠY?
//                        task.get();
//                        //runtime.stop();   // ✅ an toàn vì toàn bộ RecursiveAction đã kết thúc
//                        Log.d(TAG, "Work completed: " + getId());
//                        return Result.success();
//                    } catch (RuntimeException re) {
//                        Log.e("MyWorker", "Error in task: ", re);
//                        Throwable cause = re.getCause() != null ? re.getCause() : re;
//                        // tốt nhất: duyệt cả chuỗi cause để robust (nếu bị wrap nhiều lớp)
//                        if (containsCause(cause, UnknownHostException.class)) {
//                            // Thường xảy ra SAU SocketTimeoutException
//                            // Mạng đứt / DNS fail
//                            setProgressAsync(new Data.Builder().putString("status", "RETRY_NETWORK").build());
//                            return Result.retry();
//                        } else if (containsCause(cause, SocketTimeoutException.class)) {
//                            // Thường xảy ra trước UnknownHostException: ỪNG NÉM LỖI NÀY RA
//                            // Timeout (có thể mạng yếu hoặc server chậm)
//                            Log.d(TAG, "doWork: url TIMEOUT" + re.getCause());
//                            //setProgressAsync(new Data.Builder().putString("status","RETRY_NETWORK").build());
//                            //return Result.retry(); TIẾP TỤC CHẠY URL KẾ TIẾP
//                        } else if (containsCause(cause, HttpStatusException.class)) {
//                            // HTTP error (không phải mất mạng)
//                            Log.d(TAG, "doWork: HttpStatusException=" + re.getCause());
//                            setProgressAsync(new Data.Builder().putString("status", "RETRY_NETWORK").build());
//                            return Result.retry();
//
//                        } else if (containsCause(cause, InterruptedIOException.class) || containsCause(cause, InterruptedException.class)) {
//                            // interrupt / cancel
//                            // Bị interrupt -> coi là cancel / failure hoặc retry tùy logic
//                            return Result.failure();
//                            //} else if (containsCause(cause, IOException.class)) {   // Cuối cùng hoặc
//                        } else {
//                            // Lỗi khác
//                            return Result.failure(); // IO khác -> fail
//                        }
//                    }
//                    /// //
//
//                    if (isStopped()) {
//                        Log.d("MyWorkerCrawler", "Work cancelled after execution: " + getId());
//                        return Result.failure();
//                    }
//
//
//                } catch (InterruptedException ie) {
//                    Log.e("MyWorker", "Error in doWork: ", ie);
//                    Thread.currentThread().interrupt();
//                    return Result.failure();
//                } catch (Exception e) {
//                    Log.e(TAG, "Lỗi trong ForkJoinPool: " + e.getMessage(), e);
//                    // Cập nhật trạng thái lỗi nếu cần
//                    return Result.failure(new Data.Builder().putString("error", e.getMessage()).build());
//                } finally {
//                    /**
//                     * Lệnh forkJoinPool.shutdown(); là rất quan trọng. Nó bắt đầu quá trình tắt của ForkJoinPool,
//                     * ngăn không cho các tác vụ mới được gửi đi và cho phép các tác vụ đang chạy hoàn thành trước khi các luồng trong pool kết thúc.
//                     * Việc không tắt pool có thể dẫn đến rò rỉ tài nguyên (các luồng vẫn hoạt động trong nền
//                     * ngay cả khi công việc của chúng đã xong) và ảnh hưởng đến hiệu suất hệ thống.
//                     * Ý nghĩa: Khối finally đảm bảo rằng ForkJoinPool luôn được tắt một cách gọn gàng
//                     * sau khi MyWorkerCrawler hoàn thành công việc của nó
//                     * (dù thành công hay thất bại), giúp giải phóng các luồng và tài nguyên hệ thống
//                     * đã được sử dụng.
//                     */
//                    runtime.stop();
//                    forkJoinPool.shutdownNow();
//                    Log.d(TAG, "doWork: ForkJoinPool shut down.");
//                }
                ///////////////

            } else {
                Log.d(TAG, "doWork: No new URLs to process in this batch.");
            }


            // Cập nhật trạng thái cuối cùng sau khi hoàn thành hoặc có lỗi

            if (crawledUrlStar1Count.get() >= totalUrlsToCrawl) {
                Log.d(TAG, "doWork: Crawler completed all URLs successfully." +
                        "\n* crawledUrlStar1Count.get()=" + crawledUrlStar1Count.get() +
                        "\n* totalUrlsToCrawl=" + totalUrlsToCrawl +
                        "\n* Chênh lệch totalUrlsToCrawl - crawledUrlStar1Count.get() =" + (totalUrlsToCrawl - crawledUrlStar1Count.get()));

                //
                // --- Sau khi tất cả công việc đã hoàn thành ---
                // Thời gian cuối cùng đã trôi qua sẽ là tổng thời gian cộng dồn
                finalCumulativeElapsedTime = lastSavedCumulativeElapsedTime + (System.currentTimeMillis() - currentSessionStartTime);

                //long finalElapsedTime = System.currentTimeMillis() - startTime; // Thời gian tổng cộng đã trôi qua
                Data progressData = new Data.Builder()
                        //Xem
                        .putInt(AppConstants.WORK_PROGRESS_PERCENT, 100)   //percent
                        //
                        .putLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), crawledUrlCount.get())   //iSumProcessedUrlsCount
                        .putLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), crawledUrlStar1Count.get())
                        .putLong(selectedCrawlType.getTotalUrlsPrefKey().name(), totalUrlsToCrawl)

                        .putString(AppConstants.URL_CURRENT, lastCurrentUrl)
                        .putLong(AppConstants.WORK_ELAPSED_TIME, finalCumulativeElapsedTime) // <-- Đảm bảo thời gian tổng cộng được đưa vào outputData
                        // Thông tin Threads
                        .putInt("completedTasks", completedTasks.get())
                        .putString("message", "Chau Crawl thành công")
                        .build();
                //setProgressAsync(new Data.Builder().putInt(AppConstants.WORK_PROGRESS_PERCENT, 100).build());
                //setProgressAsync(progressData);
                //KHÔNG CÓ TÁC DỤNG: VÌ CHẬM HƠN SUCCESS
                //setProgressAsync(progressData).get(); // Sử dụng .get() để đảm bảo nó hoàn thành trước khi return

                // Nếu thành công: reset lại các thông số
                // KHÔNG NÊN RESET Ở ĐÂY, SẼ LÀM THAY ĐỔI UI KHI RESUME(ĐÃ HOÀN THÀNH)
                // KHI CHẠY CRAWL MỚI THÌ RESET
                //                settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(),0L);
                //                settingsRepository.saveCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey(),0L);     //crawledUrlStar1Count
                //                // Reset lại time đã lưu
                //                settingsRepository.saveLongElapsedTime(selectedCrawlType, 0L);
                // CHỈ RESET WorkRequestId
                settingsRepository.clearWorkRequestId(selectedCrawlType.getWorkIdPrefKey());    // Trong SQLITE
                //Trong Room
                //                AppDatabase appDatabase = AppDatabase.getInstance(context); // Lấy thể hiện của Room Database
                //                workStateDao = appDatabase.workStateDao(); // Lấy DAO
                //                workStateDao.deleteByWorkId(selectedCrawlType.getWorkIdPrefKey(), re);   // Đảm bảo workStateDao được khởi tạo và có sẵn

                // Có thể thêm một Thread.sleep ngắn ở đây (ví dụ 100-200ms) nếu Worker kết thúc QUÁ NHANH,
                // nhưng .get() thường là đủ để chờ setProgressAsync hoàn tất việc gửi dữ liệu.
                // Nếu bạn không muốn block, có thể bỏ .get() nhưng chấp nhận rủi ro nhỏ
                // là UI không kịp nhận 100% nếu WorkManager quá tải hoặc UI không kịp xử lý.
                //Thread.sleep(2000);   // Mới có tác dụng


                //                return Result.success(createOutputData(true, "Worker hoàn thành thành công!",
                //                        crawledUrlStar1Count.get(), crawledUrlCount.get(), totalUrlsToCrawl, finalCumulativeElapsedTime));
                return Result.success(progressData);
            } else {
                Log.d(TAG, "doWork: KẾT THÚC: Crawler finished, but some URLs might remain or failed." +
                        "\n* crawledUrlStar1Count.get()=" + crawledUrlStar1Count.get() +
                        "\n* totalUrlsToCrawl=" + totalUrlsToCrawl +
                        "\n* Chênh lệch totalUrlsToCrawl - crawledUrlStar1Count.get() =" + (totalUrlsToCrawl - crawledUrlStar1Count.get()));

                //Thread.sleep(2000);   // Mới có tác dụng
                //return Result.success(); // Hoặc Result.retry() nếu muốn thử lại
                // Nếu mọi thứ thành công
                return Result.success(createOutputData(true, "Vẫn cồ thiếu một số. Worker hoàn thành thành công!"));
            }

        } catch (Exception e) {
            Log.e("Worker", "Lỗi trong doWork()", e);
            return Result.failure();
        }


        // End Mới

                // CŨ \\\\\\\\\\\/////////\\\\\\\\///////
                // Cờ này LUÔN LUÔN là true nếu WorkRequest này được tạo ban đầu với cờ đó
    //            boolean isFirstRunForThisWorkRequest = getInputData().getBoolean(AppConstants.KEY_IS_FIRST_RUN, false);
    //            // Kiểm tra trạng thái lưu trữ bền vững để xác định đây có phải là lần tiếp tục không
    //            boolean hasBeenStartedBefore = prefs.getBoolean(KEY_HAS_BEEN_STARTED_BEFORE, false);
//        } catch (RuntimeException e) {
//            if (e.getCause() instanceof IOException) {
//                // Đứt mạng → retry
//                setProgressAsync(new Data.Builder()
//                        .putString("status", "RETRY_NETWORK")
//                        .build());
//                return Result.retry();
//            } else {
//                return Result.failure();
//            }
//
//        } catch (Exception e) {
////            Log.e(TAB, "Error in doWork: " + e.getMessage());
////            try {
////                Thread.sleep(2000);   // Mới có tác dụng
////            } catch (InterruptedException ex) {
////                throw new RuntimeException(ex);
////            }
////            return Result.failure();
//            // Khi có lỗi xảy ra
//            Log.e(TAG, "Worker failed: " + e.getMessage(), e);
//            // Gửi thông điệp lỗi qua outputData
//            return Result.failure(createOutputData(false, "Lỗi trong quá trình xử lý: " + e.getMessage()));
//        }

    } // End doWork()



    //Chú ý:
    // Thậm chí, trong một số trường hợp, ForkJoinPool có thể thích các lớp static hơn để tránh overhead
    // liên quan đến tham chiếu ngầm của inner class, nhưng điều này thường không phải là vấn đề hiệu suất lớn
    // trừ khi bạn đang làm việc với một lượng lớn các tác vụ nhỏ.
    //
    //Vì vậy, cách bạn truyền AtomicInteger vào hàm tạo của RecursiveAction là đúng
    // và hiệu quả để chia sẻ trạng thái giữa các tác vụ trong ForkJoinPool,
    // bất kể UrlCounterAction là static hay non-static inner class
    // (mặc dù static có thể là lựa chọn hơi tốt hơn về mặt kiến trúc
    // nếu bạn không cần truy cập các thành viên không tĩnh của MyWorker).

    // Lớp nội bộ CrawlRecursiveAction
    class CrawlRecursiveAction extends RecursiveAction {   //public static  cũng được
        // ĐẶT Ở ĐÂY ĐỂ TRUYỀN LẠI CHO AN TOÀN VỀ LUỒNG
        private final AtomicLong crawledUrlStar1Count; // Bộ đếm an toàn cho số URL CÓ "start=1" đã xử lý
        //private Set<String> crawledUrlChilds = ConcurrentHashMap.newKeySet();
        private final ConcurrentSkipListSet<String> crawledUrlChilds = new ConcurrentSkipListSet<>();

        //private final int STT=0;
        //private int demProgress=0;  //Khác iD
        private final List<UrlInfo> urlInfoQueueList;
        private final int start;
        private final int end;
        private final CrawlRuntime runtime; // ✅ TIÊM RUNTIME VÀO ĐÂY
        //private AtomicInteger parentId = new AtomicInteger(0);  //local. Cũng là số iD của link nhưng không kèm theo các link#start=1/
        //Độ sâu crawl kể từ url đầu tiên bắt đầu (start=1), khi gặp url có "start=1" thì reset lại level=0
        //level: nếu xem là số pages (title) thì level bắt đầu là 1 (các trang sau: đk level > 1)
        // lúc đó sẽ lấy số "title" thay cho level, bằng cách dùng:
        // String title = link.attr("title"); rồi lấy int childLevel = Integer.parseInt(title);
        //private final int level = 1;    //Độ sâu crawl kể từ url đầu tiên bắt đầu (start=1), khi gặp
        //private Map<String, XSSFCellStyle> styles;
        //private TextView mTextNumber;
        /***/
        /**
         * Used for statistics
         */
        private final long t0 = System.nanoTime();
        private final AtomicBoolean isCancelled;
        //private static Logger logger = Logger.getAnonymousLogger();

        //CÓ THỂ KHÔNG CHIA, MÀ CHẠY VÒNG LẶP
         public CrawlRecursiveAction(List<UrlInfo> urlInfoQueueList, int start, int end,
                                     AtomicLong crawledUrlStar1Count, CrawlRuntime runtime, AtomicBoolean isCancelled) {

            // PP CŨ của tôi
            this.crawledUrlStar1Count = crawledUrlStar1Count;
            //KHỞI TẠO BIẾN BAN ĐẦU:
            this.urlInfoQueueList = urlInfoQueueList;
            this.start = start;
            this.end = end;
            this.runtime = runtime;
//            this.parentId = parentId;
//            this.level=level;
            // this.db = db; // LOẠI BỎ DÒNG NÀY!
             // private SQLiteDatabase dbThuoc; // LOẠI BỎ BIẾN NÀY!
             // Thay thế bằng DBHelperThuoc

             //this.crawledUrlChilds = crawledUrlChilds;   //Chỉ dùng visitedUrls là đủ, không cần dùng crawledUrlChilds khi tìm các trang con
             this.isCancelled = isCancelled;
         }
        @Override
        public void compute() {
            Log.d(TAG, "compute: Bắt đầu chia công việc");
            if (isCancelled.get()) {
                return;
            }

            runtime.activeCrawlerCount.incrementAndGet();
            try {
                // ✅ Kiểm tra invariant cho chắc
                if (start < 0 || end < start || end > urlInfoQueueList.size()) {
                    throw new IllegalArgumentException("Invalid range: start=" + start +
                            " end=" + end +
                            " listSize=" + urlInfoQueueList.size());
                }

                Log.d(TAG, "compute(): range=[" + start + "," + end + ") total=" + urlInfoQueueList.size());
                //Chia công việc nhỏ hơn
                if (end - start <= THRESHOLD) {
                    //isCancelled → kiểm soát mức Worker/ForkJoinPool.

                    //Thread.interrupted() → bắt các trạng thái interrupt ở tầng thread.
                    //Như vậy, đệ tử vừa dừng sớm được, vừa tránh bỏ sót tình huống blocking.
                    // 3️⃣ Chạy crawl dài
                    // Trường hợp nhỏ, xử lý trực tiếp
                    for (int i = start; i < end && !isCancelled.get(); i++) {  // XEM LẠI DẤU i<=end hay i<end?
                        //Vẫn nên kết hợp với Thread.interrupted() bên trong crawlUrl()
                        // nếu crawl mất nhiều thời gian → để dừng ngay trong khi crawl.

                        //Nếu crawlUrl() gọi Jsoup hoặc I/O blocking, chỉ isCancelled không đủ,
                        // cần interrupt để thoát blocking.
                        if (Thread.currentThread().isInterrupted()) {
                            // Lưu tiến trình trước khi dừng
                            //saveProgress(urls.get(i));
                            return;
                        }

                        //urlInfo = urlInfoQueueList.get(i);
                        // Chú Ý thứ tự do: Cursor cursor = dbThuoc.query(initUrlsTable, new String[]{URL, PARENT_ID, LEVEL}, STATUS + "=?", new String[]{String.valueOf(0)}, null, null, null, null);
                        UrlInfo urlInfo = urlInfoQueueList.get(i);
                        String currentUrl = urlInfo.getUrl();  // url. urlInfoQueueList.get(i) là UrlInfo
                        lastCurrentUrl = currentUrl;
                        long parentId = urlInfo.getParentId(); //
                        int level = urlInfo.getLevel();
                        // end xem
                        Log.d(TAG, "compute: TRƯỚC lọc các url trùng, start=" + start + " end=" + end +
                                "; currentUrl=" + currentUrl + "\n==*******========********==========\"");
                        //Log.d(TAG, "compute: TRƯỚC lọc các url trùng ==*******========********==========");
                        //Xem Kiểm tra
//                        if (currentUrl.contains("A.&opt=TT&start=1")) {
//                            int iStop = 0;
//                        }

                        // Chỉ dùng visitedUrls là đủ, không cần dùng crawledUrlChilds khi tìm các trang con
                        //HIỆU QUẢ HƠN synchronized (vì nó cung cấp hiệu suất cao hơn và đơn giản hóa code.)
                        if (currentUrl.endsWith("261")||currentUrl.endsWith("281")){
                            int stop = 0;
                        }
                        if (visitedUrls.contains(currentUrl)) { // Sử dụng contains, Kiểm tra xem URL đã crawl chưa
                            //Có cần delete url đã crawl hoàn thành khỏi bảng initUrlsTable (do còn sót? tại sao sót)?
                            //dbThuoc.delete(initUrlsTable, "url = ?", new String[]{currentUrl});
//                        return;   // Bỏ qua nếu đã crawl
                            continue;
                        }
                        // Thêm vào luôn. Nếu crawlUrl mà bị lỗi thì crawl lại sau phiên sau
                        visitedUrls.add(currentUrl);    // QUAN TRỌNG: KHÔNG THỂ THIẾU
                        // Tổng url đã xử lý: kể cả bị lỗi: crawledUrlCount sẽ tăng 1
                        settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(), crawledUrlCount.incrementAndGet());

                        // [SỬA ĐỔI] Gọi crawlUrl cho URL hiện tại
                        // Cào chính sau xảy ra TRƯỚC tiến trình progress
                        Log.d(TAG, "compute: Bắt đầu gọi phương thức crawlUrl");
                        try {
                            // XEM
                            Log.d(TAG, "compute crawlChildPages: UrlInfo đã nạp, TRƯỚC CHẠY crawlUrlParentChild currentUrl= " + urlInfo.getUrl());  //currentUrl);

                            Packet packet = crawlUrlParentChild(urlInfo, parentId, level, crawledUrlChilds,
                                    crawledUrlCount, crawledUrlStar1Count, dbHelperThuoc);
                            Log.d(TAG, "compute: Sau khi gọi phương thức crawlUrl. URL= " + currentUrl);

                            // XEM
//                            Log.d(TAG, "WriterEngineChunked: compute: packet != null = " + (packet != null));
//                            //if (packet.getThuocList()!= null){
//                                Log.d(TAG, "WriterEngineChunked compute: ThuocSQLite.size()=" + packet.getThuocList().size());
//                                Log.d(TAG, "WriterEngineChunked compute: urlInfo.getUrl()=" + packet.urlInfo.getUrl());
//                                for (ThuocSQLite t : packet.getThuocList()) {
//                                    //url_id ĐƯC GHI THRONG PHƯƠNG THỨC: bindThuocAndExecute(urlId, t);
//                                    Log.d(TAG, "WriterEngineChunked compute:" +
//                                            "\nt.ma_thuoc=" + t.ma_thuoc +
//                                            "\nt.ten_thuoc" + t.ten_thuoc);
//                                }
//                            //}
                            //End Xem

                            if (packet != null) {
                                // XEM
                                Log.d(TAG, "WriterEngineChunked compute : numPacket=" + numPacket.incrementAndGet());
                                Log.d(TAG, "WriterEngineChunked compute : packet LẤY ĐƯỢC, có 1 UrlInfo chứa Url ="+ packet.urlInfo.getUrl() +
                                        "\npacket có Số records/packet: " + packet.records.size());  //currentUrl);
                                // TẠM BỎ KHÔNG CHẠY runtime.submit(packet): XEM url có 281 được chạy không?
                                runtime.submit(packet); // CrawlRuntime = runtime
                                // end tạm
                            }

                        } catch (IOException e) {
                            // đã lưu lỗi trong crawlUrl; bọc để propagate lên join()
                            Log.d(TAG, "compute: Lỗi IOException trong crawlUrl ném cho compute : " + e.getMessage());
                            throw new RuntimeException(e); // Cho Worker biết để retry
                        }
                    }

                } else {
                    //// Chia đôi và đệ quy: "end" exclusive
                    int mid = start + (end - start) / 2;
                    Log.d(TAG, "compute(): split -> left=[" + start + "," + mid + "), right=[" + mid + "," + end + ")");

                    CrawlRecursiveAction left = new CrawlRecursiveAction(urlInfoQueueList, start, mid, crawledUrlStar1Count, runtime, isCancelled);
                    CrawlRecursiveAction right = new CrawlRecursiveAction(urlInfoQueueList, mid, end, crawledUrlStar1Count, runtime, isCancelled); //end exclusive: mid "KHÔNG" + 1
                    invokeAll(left, right);

                }
            } catch (Exception e) {
                if (!(e instanceof SocketTimeoutException)) {
                    //e.printStackTrace();
                    Log.e(TAG, "compute: Error in compute: " + e.getMessage());
                    throw e;
                }
            } //finally {
        }
        //Tạm bỏ

        public List<ThuocSQLite>  crawlUrl(String url, long parentId, int level, ConcurrentSkipListSet<String> crawledUrlChilds,
                                           AtomicLong crawledUrlCount, AtomicLong crawledUrlStar1Count, DBHelperThuoc dbHelperThuoc) throws IOException {
            /* Mục đích:
                * 1/ Cào dữ liệu thuốc từ url
                * 2/ Đãnh dấu url đã cào vào vào bảng URL_QUEUE với STATUS=1
                * 3/ Lưu các Url con lấy được vào bảng URL_QUEUE với STATUS=0
             */
            // Lưu trữ thông tin về URL vào bảng URL_QUEUE với Ttrang thái chưa xử lý (STATUS = 0)
            //long idUrrl = dbHelperThuoc.addUrlToQueue(selectedCrawlType.getUrlQueueTableName(), url, parentId, level, extractKyTuSearchFromUrl(url));
            //

            // Đặt setProgress ở đây thử: TRƯỚC KHI GET DOCUMENT
            //if (url.endsWith("start=1")) {
                setProgressStartBang1(parentId, url);
            //}
            String err_message = "";
            int indexColor = 0;
            StringBuilder sbMaTrung = new StringBuilder();
            int soMaThuocDaLay = 0;
            boolean trungTatCaMaThuoc = true;   //Chỉ cần 1 maThuoc không trùng là false
//            if (isUrlProcessed(url)) {  //Xem lại : chức năng giống markUrlAsProcessing
//                Log.d(TAG, "crawlUrl: URL đã được đánh dấu là đang xử lý: " + url);
//                return;
//            }

            //dbHelperThuoc.markUrlAsProcessing(dbThuoc, selectedCrawlType.getUrlQueueTableName(), url, level);   // Đánh dấu url là đang xử lý

            //Tạo một số đối tượng Thuoc: 2 phương pháp khác nhau: chỉ chọn 1
                // 1/ Dành cho Room: Tạo một số đối tượng Thuoc
            List<ThuocRoom> thuocToInsert = new ArrayList<>();
            // Thêm url đầu tiên vào thuoc của ROOM: 2kytu hoặc 3kyTu, trước khi lấy document
            //  Nếu kết nối đến trang web thất bại, thì vẫn có listThuoc chứa url đầu tiên sẽ thêm url vào table ThuocPrefs
            ThuocRoom thuocRoom = new ThuocRoom();  //thuoc là thuoc của Room
            // Gán loai_thuoc từ enum
            thuocRoom.loai_thuoc = selectedCrawlType.getDbType(); // <-- Quan trọng!
            thuocRoom.url = url;
            thuocRoom.parent_id_of_crawled_url = parentId;
            thuocRoom.level_of_crawled_url = level;
            //Hàng đầu: cho thông tin tóm tắc về url có: thuocPrefs.ky_tu_search và thuocPrefs.ma_thuoc giống nhau
            //thuocRoom.ky_tu_search = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn
            //thuocRoom.ma_thuoc = "key=" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            thuocRoom.ky_tu_search = "key=" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            thuocRoom.ma_thuoc = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn
            thuocRoom.ten_thuoc = "#HangDau";
            // ... điền các trường khác
            thuocToInsert.add(thuocRoom);   //Phần tử đầu là Hàng đầu

                // 2/ Dành cho Prefs: Tạo một số đối tượng Thuoc
            List <ThuocSQLite> listThuoc = new ArrayList<>();

            //  Nếu kết nối đến trang web thất bại, thì vẫn có listThuoc chứa url đầu tiên sẽ thêm url vào table ThuocPrefs
            ThuocSQLite thuocSQLite = new ThuocSQLite();
            //thuocSQLite.url = url;
            //thuocSQLite.parent_id = parentId;
            //thuocSQLite.level = level;
            //Hàng đầu: cho thông tin tóm tắc về url có: thuocPrefs.ky_tu_search và thuocPrefs.ma_thuoc giống nhau
//            thuocSQLite.ky_tu_search = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn
//            thuocSQLite.ma_thuoc = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            //thuocSQLite.ky_tu_search = "key" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            thuocSQLite.ma_thuoc = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn

            thuocSQLite.ten_thuoc = "#HangDau";
            thuocSQLite.ma_thuoc_link = url;
            listThuoc.add(thuocSQLite);   //Phần tử đầu là Hàng đầu
            //long idUrlHangDau = dbHelperThuoc.insertThuoc(tableThuoc, ThuocPrefs);   //Luu vị trí, sau này thêm ghi chú

            //Màu nền của excel: patternFill patternType theo Indexed Colors
            //Xám: Indexed Colors=10, màu RGB(Hex): FF808080    .font: RGB: FFFFFFFF (Trắng, xanh nhạt)
            //Đỏ: Indexed Colors=3, màu RGB(Hex): FFFF0000  . font: rgb="FFFFFFFF"/> <!-- Font trắng --> (Trắng, vàng nhạt)
            //Vàng: Indexed Colors=6, màu RGB(Hex): FFFFFF00    . font: RGB: FF000000 (Đen, xanh đậm)
            //Cam: Indexed Colors=45, màu RGB(Hex): FFFFA500    . Font: RGB: FF000000 (Đen, xanh đậm)
            //Xanh Lam: Indexed Colors=8, màu RGB(Hex): FF0000FF    . font: RGB: FFFFFFFF (Trắng, vàng nhạt)
            //Xanh lá cây: Indexed Colors=4, màu RGB(Hex): FF008000 . font: RGB FFFFFFFF (Trắng, vàng nhạt)

            try {
                Log.d(TAG, "crawlUrl: Bắt đầu get document, lấy dữ liệu cào cho URL: " + url);
                Document document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        //.timeout(10000) // Tăng timeout nếu cần
                        .ignoreHttpErrors(true) // tránh crash khi 404
                        .get();
                Log.d(TAG, "crawlUrl: Đã lấy được document từ: " + url);

                //Đặt trước khi cào d liệu: để xem tình trạng các luồng. Tìm url con và lưu vào bảng initUrlsTable và đưa vào luồng mới để crawl
                Log.d(TAG, "crawlUrl: Bắt đầu thực hiện phương thức crawlChildPages. Đặt trước khi cào d liệu (tìm urrl con): để xem tình trạng các luồng");
                //crawlChildPages(url, document, parentId, level, crawledUrlChilds, crawledUrlStar1Count, dbHelperThuoc); // Bỏ qua crawl trang con để đơn giản hóa debug ban đầu
                Log.d(TAG, "crawlUrl: Sau khi thực hiện phương thức crawlChildPages");

                // Nếu HTTP status != 200 → trang ảo
                if (document.connection().response().statusCode() != 200) {
                    return listThuoc;
                }

                // Kiểm tra nội dung thực tế (ví dụ: có table, danh sách thuốc...)
//                Element content = document.selectFirst("#dlstThuoc, table, .list-item");
//                if (content == null || content.text().trim().isEmpty()) {
//                    return listThuoc; // Không có dữ liệu thật
//                }

                Element dlstThuoc  = document.getElementById("dlstThuoc");
                if (dlstThuoc  == null) return listThuoc;
                // Mỗi table: "table[border='0']" là 1 record
                Elements records = dlstThuoc .select("table[border='0']");   // Mỗi "table[border='0']" là 1 record
                if (records.isEmpty()) { // Không có bảng nào được tìm thấy
                    Log.d(TAG, "Không tìm thấy bảng nào trong URL: " + url);
                    //listThuoc.get(0).ghi_chu = "TRANG NÀY KHÔNG CÓ THUỐC";
                    listThuoc.get(0).ten_thuoc = "TRANG NÀY KHÔNG CÓ THUỐC";
                    //listThuoc.get(0).index_color = 2;   // Nền Đỏ (Red)
                    // Thay vì:
                    //nếu thành công trả về true, ngược lại false
                    // dbHelperThuoc.insertMultipleThuoc(db, table, listThuoc); // Sai, db không nên là tham số
                    // Bị lỗi: thay
                    //dbHelperThuoc.insertMultipleThuoc(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                    // Bằng:
                    dbHelperThuoc.insertMultipleThuocNotransaction(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                    // KHÔNG CÓ THUỐC, CŨNG ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                    dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

                    //DBHelperThuoc.updateGhiChu(dbThuoc, tableThuoc, idUrlHangDau, "TRANG NÀY KHÔNG CÓ THUỐC", "colorRedChuTrang");
                    return listThuoc; // Không có bảng nào được tìm thấy, thoát khỏi hàm (không cần xử lý tiếp)
                }
                for (Element record : records) {
                    thuocSQLite = new ThuocSQLite();
                    //thuocSQLite.url = url;  // url (key ngoại) nguồn để lấy thuốc
                    //thuocSQLite.parent_id = parentId;
                    //thuocSQLite.level = level;
                    //thuocSQLite.ky_tu_search = "key" + url.split("key")[1];   //= keyS. Có nhanh hơn
                    // hoặc dùng ma_thuoc_link = url; thay vì ky_tu_search
                    //thuocSQLite.ma_thuoc_link = url;
                    //thuocPrefs.link_ky_tu_search = url; // chính là url. CÓ THỂ TRƯỜNG url là thừa, vì ma_thuoc đã có link url
                    //Tách riêng: nên
                    // 1/ Lấy <h2><a>
//                    Element h2a  = table.selectFirst("h2 a");
                    Element h2a = record.selectFirst(" h2 a[href*='thuoc-']");
                    if (h2a  != null) {
                        String  linkThuoc = h2a.absUrl("href"); //Xem có =
                        //String href = h2a.absUrl("href");   // linkThuoc
                        String href = h2a.attr("href");
                        int idx = href.indexOf("thuoc-");
                        if (idx != -1) {
                            // lẤY mã thuốc có dạng "thuoc-số"
                            //thuocSQLite.ma_thuoc = href.substring(idx).split("/")[0];
                            // lẤY mã thuốc có dạng  "số"
                            thuocSQLite.ma_thuoc = href.substring(idx).split("/")[0].replace("thuoc-", "");
                            thuocSQLite.ma_thuoc_link = linkThuoc;   //href;
                        }

                        // Dùng regex nhiều sẽ chậm
//                        Matcher matcher = Pattern.compile("thuoc-(\\d+)").matcher(href);    // Dùng regex nhiều sẽ chậm
//                        if (matcher.find()) {
//                            thuocSQLite.ma_thuoc = matcher.group(1); //Kết quả là chuỗi số: "1234"
//                            //thuocPrefs.ma_thuoc = matcher.group(0);   //Kết quả: "thuoc-1234"
//                            thuocSQLite.ma_thuoc_link = linkThuoc;   //href;
//                        }

                        thuocSQLite.ten_thuoc = h2a.text();
                        //thuocPrefs.link_ten_thuoc = href;
                    }

                    // 2/ Lấy các td có bất kỳ class nào đó theo điều kiện
                    Elements tds = records.select("td[class]:not(:has(img)):not(.tddrg_lst)");
                    int colIndex = 0;
                    for (int i = 0; i < tds.size(); i++) {
                        // Nếu duyệt
                        if (tds.get(i).selectFirst("h2 a") != null ||
                                (i + 1 < tds.size() && tds.get(i + 1).selectFirst("h2 a") != null)) {
                            continue;
                        }
                        String textValue = "", linkValue = null;

                        Element td = tds.get(i);
                        if (td.text().trim().isEmpty() && td.select("img").size() > 0) {
                            colIndex++;
                            continue;
                        }

                        Element aTag = td.selectFirst("a[href]");
                        if (aTag != null) {
                            textValue = aTag.text().trim();
                            linkValue = aTag.absUrl("href");
                            linkValue = linkValue.endsWith("compid=0") ? null : linkValue;
                        } else {
                            textValue = td.text().trim();
                            if (textValue.startsWith("Thành phần:")) {
                                textValue = textValue.replaceFirst("^Thành phần:\\s*", "");
                            }
                        }

                        if (textValue.isEmpty()) {
                            colIndex++;
                            continue;
                        }


                        //String href = aTag != null ? aTag.absUrl("href") : "";
                        // Xét thêm ĐK href(link = "https://www.thuocbietduoc.com.vn/home/clickAd.aspx?comptyp=2&compid=0"   // lỗi khi set)
                        //href = href.endsWith("compid=0") ? "" : href;

                        switch (i) {
                            //Hoặc: text.replaceAll("(?i)^thành\\s*phần.*:\\s*","")
                            case 0:
                                thuocSQLite.thanh_phan = textValue;
                                thuocSQLite.thanh_phan_link = linkValue;
                                break;
                            case 1:
                                thuocSQLite.nhom_thuoc = textValue;
                                thuocSQLite.nhom_thuoc_link = linkValue;
                                break;
                            case 2:
                                thuocSQLite.dang_thuoc = textValue;
                                thuocSQLite.dang_thuoc_link = linkValue;
                                break;
                            case 3:
                                thuocSQLite.san_xuat = textValue;
                                thuocSQLite.san_xuat_link = linkValue;
                                break;
                            case 4:
                                thuocSQLite.dang_ky = textValue;
                                thuocSQLite.dang_thuoc_link = linkValue;
                                break;
                            case 5:
                                thuocSQLite.phan_phoi = textValue;
                                thuocSQLite.phan_phoi_link = linkValue;
                                break;
                            case 6:
                                thuocSQLite.sdk = textValue;
                                thuocSQLite.sdk_link = linkValue;
                                break;
                        }
                    }   // End for (int i = 1; i <= 7 && i < tds.size(); i++): lấy các trường của 1 thuốc

                    // Lưu vào DB: Chưa dùng Chưa hiểu
                    lock.lock();    //KHÔNG CHO THẰNG KHÁC CHÈN
                    try {
                        if (!maThuocDaLay.containsKey(thuocSQLite.ma_thuoc)) { //Hoặc dùng isMaThuocExists cũng nhanh do maThuoc UNIQUE INDEX
                            // Các khác
                        //if (!dbHelperThuoc.isMaThuocExists(selectedCrawlType.getTableThuocName(),thuocSQLite.ma_thuoc, url)) {
                            trungTatCaMaThuoc = false;  //Chỉ cần 1 lần false

                            //long id = dbHelperThuoc.insertThuoc(...); // insert với maThuoc, tenThuoc,...
                            //long id  = dbHelperThuoc.insertThuoc(tableThuoc, thuocPrefs);    //nếu thành công trả về true, ngược lại false
                            listThuoc.add(thuocSQLite);
                            //if (id != -1) {
                            maThuocDaLay.put(thuocSQLite.ma_thuoc, true); // đánh dấu đã thêm
                            //}
                        } else {
                            if (sbMaTrung.length() > 0) {
                                sbMaTrung.append(";");
                            }
                            sbMaTrung.append(thuocSQLite.ma_thuoc);
                            soMaThuocDaLay++;
                            trungTatCaMaThuoc = false;
                        }
                    } finally {
                        lock.unlock();
                    }
                } // End for (Element table : tables): đã lấy hết các thuốc trong url này

                completedUrlsChild.add(url); // Thêm URL con vào map

                if (!trungTatCaMaThuoc) {   //KHÔNG trùng tất cả
                    if (soMaThuocDaLay > 0) {
                        //listThuoc.get(0).ghi_chu = "TRÙNG 1 PHẦN: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung;
                        //Đổi sang cột tên thuốc: listThuoc.get(0): là hàng đầu tiên
                        listThuoc.get(0).ten_thuoc = "TRÙNG 1 PHẦN: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung;
                        //listThuoc.get(0).index_color = 1; // Nền vàng
                    }

                } else {    // Trùng tất cả
                    //listThuoc.get(0).ghi_chu = "TRÙNG HẾT: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung.toString();
                    //Đổi sang cột tên thuốc: listThuoc.get(0): là hàng đầu tiên
                    listThuoc.get(0).ten_thuoc = "TRÙNG TẤT CẢ: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung;
                    //listThuoc.get(0).index_color = 3; // Nền Green (Nền: Cam nhạt (#FFB266))

                }

                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ KHÔNG CÓ LỖI) THÌ ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

                // Trước khi tìm url con, phải lưu url cha vào crawledUrlChilds để tránh tìm lại url cha
                //crawledUrlChilds.put(url, true); //Có thể không cần, Chỉ cần xét Đk "start>1"
                //Sau khi cào d liệu: Tìm url con và lưu vào bảng initUrlsTable và đưa vào luồng mới để crawl
                crawlChildPages(url, document, parentId, level, crawledUrlChilds, crawledUrlStar1Count, dbHelperThuoc, runtime); // Bỏ qua crawl trang con để đơn giản hóa debug ban đầu
            } catch (UnknownHostException uhe) {   //SocketTimeoutException là sub class của IOException
                Log.e(TAG, "Lỗi SocketTimeoutException cho URL " + url + ": " + uhe.getMessage(), uhe);
                // Nếu lỗi timeout hoặc lỗi kết nối, đánh dấu URL là chưa hoàn thành để thử lại sau
//                if (e instanceof SocketTimeoutException || e instanceof ConnectException) {
//                    Log.e(TAG, "Lỗi kết nối hoặc timeout cho URL " + url + ". Đánh dấu URL là chưa hoàn thành để thử lại sau.");
//                }
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url );
                // Ghi chú lỗi tại url này trong bảng tableThuoc
                //listThuoc.get(0).ghi_chu = "Lỗi mạng: " + uhe.getMessage();
                err_message = "Lỗi mạng: " + uhe.getMessage();
                //listThuoc.get(0).index_color = 6;   // 6-Nền Cam
                indexColor = 3;

                // Phai lưu vào Table_thuoc

                // Bị lỗi: thay
                dbHelperThuoc.insertMultipleThuoc(tableThuoc, listThuoc);    //nếu thành công trả về true, ngược lại false
                // Bằng:
//                dbHelperThuoc.insertMultipleThuocNotransaction(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                //TableQueue
                dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(), url, 0, "Lỗi mạng: " + uhe.getMessage());
                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ CÓ LỖI) THÌ CŨNG ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Dù truyền context vào đây, nhưng vì đang ở luồng nền,
                        // VẪN CẦN Handler để hiển thị Toast
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Lỗi kết nối hoặc timeout: " + uhe.getMessage(), Toast.LENGTH_LONG).show();
                                //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
                throw uhe; // Rethrow lại để propagate: NÉM LẦN NỮA ĐỄ Ở TRY NGOÀI BẮT, để retry
//            } catch (SocketTimeoutException e) {
//                // Timeout (có thể mạng yếu hoặc server chậm)
//                //logError(url, "TIMEOUT", e);
//                throw e;

            } catch (IOException ioe) {
                // Các lỗi khác không retry
                if(ioe instanceof SocketTimeoutException) {
                    Log.e(TAG, "Lỗi SocketTimeoutException khi crawlUrl, URL=" + url + ": " + ioe.getMessage(), ioe);
                    //listThuoc.get(0).ghi_chu = "Lỗi SocketTimeoutException khi crawlUrl: " + ioe.getMessage();
                    err_message = "Lỗi SocketTimeoutException khi crawlUrl: " + ioe.getMessage();
                    dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, "Lỗi SocketTimeoutException:" + ioe.getMessage());
                } else {
                    Log.e(TAG, "Lỗi IOException không mong muốn trong crawlUrl, url= " + url + ": " + ioe.getMessage(), ioe);
                    //listThuoc.get(0).ghi_chu = "Lỗi IOException khi crawlUrl: " + url + ioe.getMessage();
                    err_message = "Lỗi IOException khi crawlUrl: " + url + ioe.getMessage();
                    dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, "Lỗi IOException: " + ioe.getMessage());
                }
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url);
                // Ghi chú lỗi tại url này trong bảng tableThuoc

                //listThuoc.get(0).index_color = 6;   // 6-Nền Cam
                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ CÓ LỖI) THÌ CŨNG ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

            } catch (Exception ex) {
                // parse error: lưu, nhưng không rethrow as IO
                // Các lỗi khác không retry
                Log.e(TAG, "Lỗi Exception không mong muốn trong crawlUrl cho URL " + url + ": " + ex.getMessage(), ex);
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url);
                // Ghi chú lỗi tại url này trong bảng tableThuoc
                //listThuoc.get(0).ghi_chu = "Lỗi không mong muốn: " + ex.getMessage();
                err_message = "Lỗi không mong muốn: " + ex.getMessage();
                //listThuoc.get(0).index_color = 6;   // 6-Nền Cam
                indexColor = 3;
                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ CÓ LỖI) THÌ CŨNG ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable
                dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, "Lỗi không mong muốn: " + ex.getMessage());
            } finally {
                // DÙ CÓ BẤT CỨ LỖI GÌ, CŨNG ĐÁNH DẤU ĐÃ XỬ LÝ (TRỪ TRƯỜNG HỢP MUỐN CRAW LẠI ĐỂ XỬ LÝ)
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable
                Log.d(TAG, "crawlUrl() kết thúc cào dữ liệu cho URL: " + url);
            }
            //1/ THÊM URL (HÀNG ĐẦU) VÀO BẢNG CHA (PARENT), LÀ NGUỒN GỐC LẤY ĐƯỢC DỮ LIỆU THUỐC
            //dbHelperThuoc.insertParentTable(selectedCrawlType.getParentTableUrls(), listThuoc); //Sau đó mói ghi table con

            // Hoặc
//            dbHelperThuoc.insertParent(dbHelperThuoc.getDb(), selectedCrawlType.getParentTableUrls(),String.valueOf(parentId), String.valueOf(level), url,
//                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), err_message, indexColor,"");
            return listThuoc;   // Trả về để ghi table con
        }

        // Theo pp: CHA-CON(Parent-child)
        public Packet crawlUrlParentChild(UrlInfo urlInfo, long parentId, int level, ConcurrentSkipListSet<String> crawledUrlChilds,
                                                     AtomicLong crawledUrlCount, AtomicLong crawledUrlStar1Count, DBHelperThuoc dbHelperThuoc) throws UnknownHostException { //throws IOException
            /* Mục đích:
             * 1/ Cào dữ liệu thuốc từ url
             * 2/ Đãnh dấu url đã cào vào vào bảng URL_QUEUE với STATUS=1
             * 3/ Lưu các Url con lấy được vào bảng URL_QUEUE với STATUS=0
             */
            // Lưu trữ thông tin về URL vào bảng URL_QUEUE với Ttrang thái chưa xử lý (STATUS = 0)
            //long idUrrl = dbHelperThuoc.addUrlToQueue(selectedCrawlType.getUrlQueueTableName(), url, parentId, level, extractKyTuSearchFromUrl(url));
            //

            String url = urlInfo.getUrl();  // Trước khi truyền vào crawlUrlParentChild, đã set url cho urlInfo

            // Đặt setProgress ở đây thử: TRƯỚC KHI GET DOCUMENT
//            // XEM
//            if (url.endsWith("281")||level==15){
//                int stop=0;
//            }

            setProgressStartBang1(parentId, url);
            //String err_message = "";
            //int indexColor = 0;
            StringBuilder sbMaTrung = new StringBuilder();
            int soMaThuocDaLay = 0;
            boolean trungTatCaMaThuoc = true;   //Chỉ cần 1 maThuoc không trùng là false
            //dbHelperThuoc.markUrlAsProcessing(dbThuoc, selectedCrawlType.getUrlQueueTableName(), url, level);   // Đánh dấu url là đang xử lý

            //Tạo một số đối tượng Thuoc: 2 phương pháp khác nhau: chỉ chọn 1
            // 1/ Dành cho Room: Tạo một số đối tượng Thuoc
            List<ThuocRoom> thuocToInsert = new ArrayList<>();
            // Thêm url đầu tiên vào thuoc của ROOM: 2kytu hoặc 3kyTu, trước khi lấy document
            //  Nếu kết nối đến trang web thất bại, thì vẫn có listThuoc chứa url đầu tiên sẽ thêm url vào table ThuocPrefs
            ThuocRoom thuocRoom = new ThuocRoom();  //thuoc là thuoc của Room
            // Gán loai_thuoc từ enum
            thuocRoom.loai_thuoc = selectedCrawlType.getDbType(); // <-- Quan trọng!
            thuocRoom.url = url;
            //thuocRoom.parent_id_of_crawled_url = parentId;
            //thuocRoom.level_of_crawled_url = level;
            //Hàng đầu: cho thông tin tóm tắc về url có: thuocPrefs.ky_tu_search và thuocPrefs.ma_thuoc giống nhau
            //thuocRoom.ky_tu_search = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn
            //thuocRoom.ma_thuoc = "key=" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            //thuocRoom.ky_tu_search = "key=" + url.split("key")[1];    // là UNIQUE, tránh mã thuốc bị null url Hoặc: "key" + url.split("key")[1]: Xem như duy nhất
            thuocRoom.ma_thuoc = "https://www..." +"key" + url.split("key")[1];   //= keyS. Có nhanh hơn
            thuocRoom.ten_thuoc = "#HangDau";
            // ... điền các trường khác
            thuocToInsert.add(thuocRoom);   //Phần tử đầu là Hàng đầu

            // 2/ Dành cho Prefs: Tạo một số đối tượng Thuoc
            //List <ThuocSQLite> listThuoc = new ArrayList<>();
            ThuocSQLite thuocSQLite = new ThuocSQLite();

            //Màu nền của excel: patternFill patternType theo Indexed Colors
            //Xám: Indexed Colors=10, màu RGB(Hex): FF808080    .font: RGB: FFFFFFFF (Trắng, xanh nhạt)
            //Đỏ: Indexed Colors=3, màu RGB(Hex): FFFF0000  . font: rgb="FFFFFFFF"/> <!-- Font trắng --> (Trắng, vàng nhạt)
            //Vàng: Indexed Colors=6, màu RGB(Hex): FFFFFF00    . font: RGB: FF000000 (Đen, xanh đậm)
            //Cam: Indexed Colors=45, màu RGB(Hex): FFFFA500    . Font: RGB: FF000000 (Đen, xanh đậm)
            //Xanh Lam: Indexed Colors=8, màu RGB(Hex): FF0000FF    . font: RGB: FFFFFFFF (Trắng, vàng nhạt)
            //Xanh lá cây: Indexed Colors=4, màu RGB(Hex): FF008000 . font: RGB FFFFFFFF (Trắng, vàng nhạt)
            //Map<UrlInfo, List<ThuocSQLite>> data = new HashMap<>();
            List<ThuocSQLite> thuocList = new ArrayList<>();  // Xem xét ConcurrentSkipListSet  thuocList
            //ConcurrentSkipListSet<ThuocSQLite> thuocList = new ConcurrentSkipListSet<>();
            //2.2. LẤY DỮ LIỆU THUỐC TỪ URL
            //Xem
            //setProgressAsync(progressData).get(); // Sử dụng .get() để đảm bảo nó hoàn thành trước khi return
//            if (
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=-A&opt=TT&start=1") ||
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=A-&opt=TT&start=1") ||
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=A -&opt=TT&start=1") ||
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key= A&opt=TT&start=1") ||
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=A &opt=TT&start=1") ||
//                url.equals("https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=A.&opt=TT&start=81")) {
//                int iStop=0;
//            }
            try {
                Log.d(TAG, "crawlUrl: Bắt đầu get document, lấy dữ liệu cào cho URL: " + url);
                Document document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        //.timeout(10000) // Tăng timeout nếu cần
                        .ignoreHttpErrors(true) // tránh crash khi 404
                        .get();

                Log.d(TAG, "crawlUrl: Đã lấy được document từ: " + url);

                //Đặt trước khi cào d liệu: để xem tình trạng các luồng. Tìm url con và lưu vào bảng initUrlsTable và đưa vào luồng mới để crawl
                Log.d(TAG, "crawlUrl: Bắt đầu thực hiện phương thức crawlChildPages. Đặt trước khi cào d liệu (tìm urrl con): để xem tình trạng các luồng");
                //crawlChildPages(url, document, parentId, level, crawledUrlChilds, crawledUrlStar1Count, dbHelperThuoc); // Bỏ qua crawl trang con để đơn giản hóa debug ban đầu
                Log.d(TAG, "crawlUrl: Sau khi thực hiện phương thức crawlChildPages");

                // Nếu HTTP status != 200 → trang ảo
                if (document.connection().response().statusCode() != 200) {
                    urlInfo.setGhiChu("document.connection().response().statusCode() = " +
                            document.connection().response().statusCode() + " , KHÁC 200");
                    // XEM
//                    Packet packet = new Packet(urlInfo, thuocList);
//                    if (packet == null) {
//                        // XEM
//                        Log.d(TAG, "crawlChildPages: TẠI SAO NULL,  packet="+ packet);  //currentUrl);
//                    }
                    // END XEM

                    return new Packet(urlInfo, thuocList);
                }
                // Đặt trước khi lấy dữ liệu
                //crawlChildPages(url, document, parentId, level, crawledUrlChilds, crawledUrlStar1Count, dbHelperThuoc, runtime); // Bỏ qua crawl trang con để đơn giản hóa debug ban đầu
                //
                Element dlstThuoc  = document.getElementById("dlstThuoc");
                if (dlstThuoc  == null) {   // PHẢI CÓ
                    urlInfo.setGhiChu("Không có Element dlstThuoc");
                    // XEM
//                    Packet packet = new Packet(urlInfo, thuocList);
//                    if (packet == null) {
//                        // XEM
//                        Log.d(TAG, "crawlChildPages: TẠI SAO NULL,  packet="+ packet);  //currentUrl);
//                    }
                    // END XEM

                    return new Packet(urlInfo, thuocList);
                }
                // Mỗi table: "table[border='0']" là 1 record
                Elements records = dlstThuoc .select("table[border='0']");   // Mỗi "table[border='0']" là 1 record
                if (records.isEmpty()) { // Không có bảng nào được tìm thấy
                    Log.d(TAG, "Không tìm thấy bảng nào trong URL: " + url);
                    //listThuoc.get(0).ghi_chu = "TRANG NÀY KHÔNG CÓ THUỐC";
                    urlInfo.setStatus(1);   // KHÔNG CÓ THUỐC, CŨNG ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                    urlInfo.setGhiChu("TRANG NÀY KHÔNG CÓ THUỐC");
                    
                    urlInfo.setIndexColor(2);   // 2- Nền Đỏ (Red)
                    //data.put(urlInfo, thuocList);
                    
                    // Thay vì:
                    //nếu thành công trả về true, ngược lại false
                    // dbHelperThuoc.insertMultipleThuoc(db, table, listThuoc); // Sai, db không nên là tham số
                    // Bị lỗi: thay
                    //dbHelperThuoc.insertMultipleThuoc(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                    // Bằng:
                    //dbHelperThuoc.insertMultipleThuocNotransaction(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                    // KHÔNG CÓ THUỐC, CŨNG ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                    dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

                    //DBHelperThuoc.updateGhiChu(dbThuoc, tableThuoc, idUrlHangDau, "TRANG NÀY KHÔNG CÓ THUỐC", "colorRedChuTrang");
                    // XEM
//                    Packet packet = new Packet(urlInfo, thuocList);
//                    if (packet == null) {
//                        // XEM
//                        Log.d(TAG, "crawlChildPages: TẠI SAO NULL,  packet="+ packet);  //currentUrl);
//                    }
                    // END XEM

                    return new Packet(urlInfo, thuocList); // Không có bảng nào được tìm thấy, thoát khỏi hàm (không cần xử lý tiếp)
                }
                for (Element record : records) {
                    //Map<String, Object> thuoc = new HashMap<>();
                    thuocSQLite = new ThuocSQLite();
                    //thuocSQLite.url = url;  // url (key ngoại) nguồn để lấy thuốc
                    //thuocSQLite.parent_id = parentId;
                    //thuocSQLite.level = level;
                    //thuocSQLite.ky_tu_search = "key" + url.split("key")[1];   //= keyS. Có nhanh hơn

                    // hoặc dùng ma_thuoc_link = url; thay vì ky_tu_search
                    //thuocSQLite.ma_thuoc_link = url;
                    //thuocPrefs.link_ky_tu_search = url; // chính là url. CÓ THỂ TRƯỜNG url là thừa, vì ma_thuoc đã có link url
                    //Tách riêng: nên
                    // 1/ Lấy <h2><a>
//                    Element h2a  = table.selectFirst("h2 a");
                    Element h2a = record.selectFirst(" h2 a[href*='thuoc-']");
                    if (h2a  != null) {
                        String  linkThuoc = h2a.absUrl("href"); //Xem có =
                        //String href = h2a.absUrl("href");   // linkThuoc
                        String href = h2a.attr("href");
                        int idx = href.indexOf("thuoc-");
                        if (idx != -1) {
                            // lẤY mã thuốc có dạng "thuoc-số"
                            //thuocSQLite.ma_thuoc = href.substring(idx).split("/")[0];
                            // lẤY mã thuốc có dạng  "số"
                            thuocSQLite.ma_thuoc = href.substring(idx).split("/")[0].replace("thuoc-", "");
                            thuocSQLite.ma_thuoc_link = linkThuoc;   //href;
                        }

                        // Dùng regex nhiều sẽ chậm
//                        Matcher matcher = Pattern.compile("thuoc-(\\d+)").matcher(href);    // Dùng regex nhiều sẽ chậm
//                        if (matcher.find()) {
//                            thuocSQLite.ma_thuoc = matcher.group(1); //Kết quả là chuỗi số: "1234"
//                            //thuocPrefs.ma_thuoc = matcher.group(0);   //Kết quả: "thuoc-1234"
//                            thuocSQLite.ma_thuoc_link = linkThuoc;   //href;
//                        }

                        thuocSQLite.ten_thuoc = h2a.text();
                        //thuocPrefs.link_ten_thuoc = href;
                    }

                    // 2/ Lấy các td có bất kỳ class nào đó theo điều kiện
                    Elements tds = record.select("td[class]:not(:has(img)):not(.tddrg_lst)");
                    //int colIndex = 0;
                    for (int i = 0; i < tds.size(); i++) {
                        // Nếu duyệt
                        if (tds.get(i).selectFirst("h2 a") != null ||
                                (i + 1 < tds.size() && tds.get(i + 1).selectFirst("h2 a") != null)) {
                            continue;
                        }
                        String textValue = "", linkValue = null;

                        Element td = tds.get(i);
                        if (td.text().trim().isEmpty() && td.select("img").size() > 0) {
                            //colIndex++;
                            continue;
                        }

                        Element aTag = td.selectFirst("a[href]");
                        if (aTag != null) {
                            textValue = aTag.text().trim();
                            linkValue = aTag.absUrl("href");
                            linkValue = linkValue.endsWith("compid=0") ? null : linkValue;
                        } else {
                            textValue = td.text().trim();
                            if (textValue.startsWith("Thành phần:")) {
                                textValue = textValue.replaceFirst("^Thành phần:\\s*", "");
                            }
                        }

                        if (textValue.isEmpty()) {
                            //colIndex++;
                            continue;
                        }


                        //String href = aTag != null ? aTag.absUrl("href") : "";
                        // Xét thêm ĐK href(link = "https://www.thuocbietduoc.com.vn/home/clickAd.aspx?comptyp=2&compid=0"   // lỗi khi set)
                        //href = href.endsWith("compid=0") ? "" : href;

                        switch (i) {
                            //Hoặc: text.replaceAll("(?i)^thành\\s*phần.*:\\s*","")
                            case 0:
                                thuocSQLite.thanh_phan = textValue;
                                thuocSQLite.thanh_phan_link = linkValue != null?linkValue:null; // THỰC TẾ thanh_phan KHÔNG CÓ LINK
                                break;
                            case 1:
                                thuocSQLite.nhom_thuoc = textValue; // THỰC TẾ nhom_thuoc KHÔNG CÓ LINK
                                thuocSQLite.nhom_thuoc_link = linkValue != null?linkValue:null;
                                break;
                            case 2:
                                thuocSQLite.dang_thuoc = textValue; // THỰC TẾ dang_thuoc KHÔNG CÓ LINK
                                thuocSQLite.dang_thuoc_link = linkValue != null?linkValue:null;
                                break;
                            case 3:
                                thuocSQLite.san_xuat = textValue;
                                thuocSQLite.san_xuat_link = linkValue != null?linkValue:null;
                                break;
                            case 4:
                                thuocSQLite.dang_ky = textValue;    // Đơn vị dang_ky
                                thuocSQLite.dang_ky_link = linkValue != null?linkValue:null;
                                break;
                            case 5:
                                thuocSQLite.phan_phoi = textValue;
                                thuocSQLite.phan_phoi_link = linkValue != null?linkValue:null;
                                break;
                            case 6:
                                thuocSQLite.sdk = textValue;    // THỰC TẾ sdk KHÔNG CÓ LINK
                                thuocSQLite.sdk_link = linkValue != null?linkValue:null;
                                break;
                        }
                    }   // End for (int i = 1; i <= 7 && i < tds.size(); i++): lấy các trường của 1 thuốc

                    // Lưu vào DB: Chưa dùng Chưa hiểu
                    lock.lock();    //KHÔNG CHO THẰNG KHÁC CHÈN
                    try {
                        //if (!maThuocDaLay.containsKey(thuocSQLite.ma_thuoc)) { //Hoặc dùng isMaThuocExists cũng nhanh do maThuoc UNIQUE INDEX
                        // Các khác
                        if (!dbHelperThuoc.isMaThuocExists(selectedCrawlType.getTableThuocName(),thuocSQLite.ma_thuoc, url)) {
                            trungTatCaMaThuoc = false;  //Chỉ cần 1 lần false

                            //long id = dbHelperThuoc.insertThuoc(...); // insert với maThuoc, tenThuoc,...
                            //long id  = dbHelperThuoc.insertThuoc(tableThuoc, thuocPrefs);    //nếu thành công trả về true, ngược lại false
                            thuocList.add(thuocSQLite);
                            //if (id != -1) {
                            maThuocDaLay.put(thuocSQLite.ma_thuoc, true); // đánh dấu đã thêm
                            //}
                        } else {
                            if (sbMaTrung.length() > 0) {
                                sbMaTrung.append(";");
                            }
                            sbMaTrung.append(thuocSQLite.ma_thuoc);
                            soMaThuocDaLay++;
                            trungTatCaMaThuoc = false;
                        }
                    } finally {
                        lock.unlock();
                    }
                } // End for (Element record : records): đã lấy hết các thuốc trong url này

                // SAU KHI LẤY HẾT DỮ LIỆU THUỐC CHO 1 urlInfo: Thêm vào data
                completedUrlsChild.add(url); // Thêm URL con vào map
            
                if (!trungTatCaMaThuoc) {   //KHÔNG trùng tất cả
                    if (soMaThuocDaLay > 0) {
                        //urlInfo.setStatus(1);   // ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                        urlInfo.setGhiChu("TRÙNG 1 PHẦN: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung);
                        urlInfo.setIndexColor(1); // Nền vàng
                    }

                } else {    // Trùng tất cả
                    //urlInfo.setStatus(1);   // ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                    urlInfo.setGhiChu("TRÙNG TẤT CẢ: " + soMaThuocDaLay + " thuốc. Mã thuốc trùng: " + sbMaTrung);
                    urlInfo.setIndexColor(3); // Nền Green (Nền: Cam nhạt (#FFB266))
                }
                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ KHÔNG CÓ LỖI) THÌ ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                urlInfo.setStatus(1);   // ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable

                // Trước khi tìm url con, phải lưu url cha vào crawledUrlChilds để tránh tìm lại url cha
                //crawledUrlChilds.put(url, true); //Có thể không cần, Chỉ cần xét Đk "start>1"
                //Sau khi cào d liệu: Tìm url con và lưu vào bảng initUrlsTable và đưa vào luồng mới để crawl
                // XEM
                if (level==14||level==15) {
                    int stop =0;
                }
                // LẤY PHÂN TRANG (NẾU CÓ) Đặt SAU khi lấy dữ liệu
                crawlChildPages(url, document, parentId, level, crawledUrlChilds, crawledUrlStar1Count, dbHelperThuoc, runtime); // Bỏ qua crawl trang con để đơn giản hóa debug ban đầu
            } catch (UnknownHostException uhe) {   //SocketTimeoutException là sub class của IOException
                // Thường là do đứt kết nối internet
                Log.e(TAG, "Lỗi UnknownHostException cho URL " + url + ": " + uhe.getMessage(), uhe);
                // Nếu lỗi timeout hoặc lỗi kết nối, đánh dấu URL là chưa hoàn thành để thử lại sau
//                if (e instanceof SocketTimeoutException || e instanceof ConnectException) {
//                    Log.e(TAG, "Lỗi kết nối hoặc timeout cho URL " + url + ". Đánh dấu URL là chưa hoàn thành để thử lại sau.");
//                }
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url );
                // Ghi chú lỗi tại url này trong bảng tableThuoc
                urlInfo.setStatus(0);   // (set = 0 Nếu muốn cào lại: retry). ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)
                urlInfo.setGhiChu("Lỗi mạng, UnknownHostException: " + uhe.getMessage());
                urlInfo.setIndexColor(6); // 6-Nền Cam Err
                dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, urlInfo.getGhiChu());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Dù truyền context vào đây, nhưng vì đang ở luồng nền,
                        // VẪN CẦN Handler để hiển thị Toast
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Lỗi kết nối UnknownHostException hoặc timeout: " + uhe.getMessage(), Toast.LENGTH_LONG).show();
                                //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
                throw uhe; // Rethrow lại để propagate: NÉM LẦN NỮA ĐỄ Ở TRY NGOÀI BẮT, để retry
//            } catch (SocketTimeoutException e) {
//                // Timeout (có thể mạng yếu hoặc server chậm)
//                //logError(url, "TIMEOUT", e);
//                throw e;

            } catch (IOException ioe) {
                // Các lỗi khác không retry
                if(ioe instanceof SocketTimeoutException) {
                    Log.e(TAG, "Lỗi SocketTimeoutException khi crawlUrl, URL=" + url + ": " + ioe.getMessage(), ioe);
                    urlInfo.setStatus(1);   // (set = 0 Nếu muốn cào lại: retry). ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)
                    urlInfo.setGhiChu("Lỗi SocketTimeoutException khi crawlUrl: " + ioe.getMessage());
                } else {
                    Log.e(TAG, "Lỗi IOException không mong muốn trong crawlUrl, url= " + url + ": " + ioe.getMessage(), ioe);
                    //urlInfo.setStatus(0);   // (set = 0 Nếu muốn cào lại: retry). ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)
                    urlInfo.setGhiChu("Lỗi IOException khi crawlUrl: " + url + ioe.getMessage());
                }
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url);
                // Ghi chú lỗi tại url này trong bảng tableThuoc
                // Chung
                urlInfo.setIndexColor(6); // 6-Nền Cam Err
//                dbHelperThuoc.insertMultipleThuocNotransaction(tableThuoc, listThuoc); // Đúng, dbHelperThuoc tự lấy DB
                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ CÓ LỖI) THÌ CŨNG ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable
                dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, urlInfo.getGhiChu());
            } catch (Exception ex) {
                // parse error: lưu, nhưng không rethrow as IO
                // Các lỗi khác không retry
                Log.e(TAG, "Lỗi Exception không mong muốn trong crawlUrl cho URL " + url + ": " + ex.getMessage(), ex);
                // Đánh dấu URL là chưa hoàn thành để thử lại sau
                //dbHelperThuoc.markUrlAsIncomplete(dbThuoc, initUrlsTable, url);
                // Ghi chú lỗi tại url này trong bảng tableThuoc
                //urlInfo.setStatus(0);   // (set = 0 Nếu muốn cào lại: retry). ĐÁNH DẤU ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)
                urlInfo.setGhiChu("Lỗi Exception không mong muốn: " + ex.getMessage());
                urlInfo.setIndexColor(6); // 6-Nền Cam Err

                // Sau khi lấy xong dữ liệu thuốc của url này (MÀ CÓ LỖI) THÌ CŨNG ĐÁNH DẤU URL ĐÃ XỬ LÝ HOÀN THÀNH (STATUS = 1)/
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable
                dbHelperThuoc.updateErrorMessageException(selectedCrawlType.getUrlQueueTableName(),url, 0, urlInfo.getGhiChu());
            } finally {
                // DÙ CÓ BẤT CỨ LỖI GÌ, CŨNG ĐÁNH DẤU ĐÃ XỬ LÝ (TRỪ TRƯỜNG HỢP MUỐN CRAW LẠI ĐỂ XỬ LÝ)
                //dbHelperThuoc.markUrlAsProcessedInQueue(selectedCrawlType.getUrlQueueTableName(), url);    //getOldTableName tương đương initUrlTable
                Log.d(TAG, "crawlUrl() kết thúc cào dữ liệu cho URL: " + url);
            }
            // XEM
//            Packet packet = new Packet(urlInfo, thuocList);
//            if (packet == null) {
//                // XEM
//                Log.d(TAG, "crawlChildPages: TẠI SAO NULL,  packet="+ packet);  //currentUrl);
//            }
            // END XEM

            return new Packet(urlInfo, thuocList);  // Trả về Packet
        }

    } //end class CrawRecursiveAction  //Vị trí cũ

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        // Create a Notification channel
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String name = "snap map fake location ";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel(MyApplication.CRAWLER_CHANNEL_ID, name, importance);
        //mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(mChannel);
        } else {
            //stopSelf();
        }
        //return "snap map channel";

    }
    ////////////////////////////////////////

    //Gốc: File tmpfile
    //Đổi lại aray File: File[] tmpfile
    //private void substitute(File zipfile, File tmpfile, String entry, OutputStream out) throws IOException { //gốc
    public static void substitute(File zipfile, File[] tmpfile, String[] entry, OutputStream out) throws IOException {
        ZipFile zip = new ZipFile(zipfile);
        ZipOutputStream zos = new ZipOutputStream(out);
        //CHUNG CHO WORKBOOK: COPY TÍNH CHẤT CỦA FILE EXCEL MẪU TRUNGGIAN
        // (SẼ ĐÚNG PHIÊN BẢN EXCEL CỦA MÁY ĐANG CÀI) CHO ThuocBietDuoc excel
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> en = (Enumeration<ZipEntry>) zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry ze = en.nextElement();
            //if(!ze.getName().equals(entry)){    //gốc:
            //Tránh biến entry la Array
            if(!ze.getName().toLowerCase().contains("xl/worksheets/sheet")){    //Cụ thể:xl/worksheets/sheet1.xml
                zos.putNextEntry(new ZipEntry(ze.getName()));
                InputStream is = zip.getInputStream(ze);
                copyStream(is, zos);
                is.close();
            }
        }

        //zos.putNextEntry(new ZipEntry(entry));  //Gốc chỉ có 1 entry
        //Biến Mảng: entry
        for (int c =0 ; c<entry.length; c++){
            zos.putNextEntry(new ZipEntry(entry[c]));  //  entry: Array
            //Chép file tmp vào zop. VÌ LÀ ARRAY FILE NÊN DÙNG VÒNG LẶP
            //InputStream is = new FileInputStream(tmpfile);    //gốc KHI KHÔNG FOR..LOOP
            InputStream is = new FileInputStream(tmpfile[c]);
            copyStream(is, zos);    //PHAI copyStream SAU putNextEntry (CÙNG 1 LẦN DUYET)
            is.close();
        }
        zos.close();
    }
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        //Xem
//        List<String> doc =
//                new BufferedReader(new InputStreamReader(in,
//                        StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
//        for(String d:doc){
//            Log.d(TAG, "copyStream: d=" + d.toString());
//        }
        ///
        byte[] chunk = new byte[1024];
        int count;
        while ((count = in.read(chunk)) >=0 ) {
            out.write(chunk,0,count);
        }

    }

    private void resetProcessingUrlsStatus(SQLiteDatabase dbThuoc, List<String> urls) {
        ContentValues values = new ContentValues();
        values.put("status", 0);
        // RESET trạng thái của các URL đang xử lý về chưa xử lý (status = 0)
        for (String url : urls) {
            int rowsAffected = dbThuoc.update(initUrlsTable, values, "url = ?", new String[]{url});
            // Nếu rowsAffected == 0, có thể có vấn đề với dữ liệu hoặc bảng
            if (rowsAffected == 0) {
                Log.w(TAG, "resetProcessingUrlsStatus: Không thể cập nhật trạng thái cho URL: " + url);
            } else {
                Log.d(TAG, "resetProcessingUrlsStatus: Đã cập nhật trạng thái cho URL: " + url);
            }
        }
    }

    private List<String> getProcessingUrls(SQLiteDatabase dbThuoc) {
        List<String> processingUrls = new ArrayList<>();

        Cursor cursor = dbThuoc.query(initUrlsTable, new String[]{"url"}, "status = 1", null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                processingUrls.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return processingUrls;
    }

    private List<String[]> loadUrlsFromAsset(SQLiteDatabase dbThuoc, int k0, int k1) { //KHÔNG gồm k1
        //tableThuoc: Phải gán giá trị trong constructor, là biến thành viên: có tác dụng toàn class
        //Tổng url sẽ là hàng số: luôn tính trên 1 lần chạy, kể cả khi thoát và app tự chạy lại
        //List<String> urls = new ArrayList<>();
        List<String> listKyTu = GetKyTuAZ.getList_AZ_2KThay3KT(tableThuoc, k0, k1); //KHÔNG gồm k1
        List<String[]> listRowUrls = new ArrayList<>();

        int parentId = 0;
        // Ghi URL vào bảng initUrlsTable
        String url0 = "https://www.thuocbietduoc.com.vn/defaults/drgsearch?act=DrugSearch&key=";
        String url1 = "&opt=TT&start=1";
        assert listKyTu != null;
        for (String kytu : listKyTu) {
            if (kytu.trim().isEmpty()) continue; //Chỉ kiểm tra url rỗng (hoặc chỉ chứa các khoảng trắng)
            // Vẫn lấy kytu có khoảng trắng ở đầu và cuối
            String[] row = new String[3];
            row[0] = String.valueOf(parentId);  //parentId
            row[1] = String.valueOf(1);     //Cũ: level = 0; Mới: level (thành page) = 1
            row[2] = (url0 + kytu + url1).trim();   // url

            // Xem: tìm lỗi thiếu đếm crawledUrlStar1Count thiếu 1
            Log.d(TAG, "loadUrlsFromAsset: crawledUrlStar1Count, url (start=1) = " + row[2]+ ";parentId=" + row[0]);
            //
            listRowUrls.add(row);   // Hoặc chỉ 1 lệnh: listRowUrls.add(new String[]{String.valueOf(parentId), String.valueOf(parentId), (url0 + kytu + url1).trim()});

//            kytu = url0 + kytu + url1;
//            String trimmedUrl = kytu.trim(); //Cắt khoảng trắng ở đầu và cuối
//            urls.add(trimmedUrl);

            // Ghi URL vào bảng initUrlsTable
            ContentValues values = new ContentValues();
            values.put(DBHelperThuoc.PARENT_ID, parentId);
            values.put(DBHelperThuoc.LEVEL, 1); //Cũ: Của url cha level = 0 (Mặc định); Mới: level (thành page) = 1 (Mặc định)
            values.put("url", (url0 + kytu + url1).trim());
            values.put("status", 0); // 0: chưa xử lý, có thể thêm cột 'completed' nếu cần. Mặc định completed = 0 (uncompleted)
            dbThuoc.insertWithOnConflict( initUrlsTable, null, values, SQLiteDatabase.CONFLICT_IGNORE); //initUrlsTable. tableNameThuoc_Nao phải có
            parentId++;

        }
        // Lưu tổng số URL vào SharedPreferences
        saveTotalUrls2SharePrefs(tableThuoc, listRowUrls.size());
        return listRowUrls;
    }

    private void saveTotalUrls2SharePrefs(String tableThuoc, int totalUrlsToCrawl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        //String TOTAL_URLS_KEY = tableThuoc.contains("2kt")? DBHelperThuoc.TOTAL_URLS_2KT:DBHelperThuoc.TOTAL_URLS_3KT;
        String TOTAL_URLS_KEY = selectedCrawlType.getTotalUrlsPrefKey().name();
        prefs.edit().putInt(TOTAL_URLS_KEY, totalUrlsToCrawl).apply();
        Log.d("CrawlWorker", "saveTotalUrls2SharePrefs: Đã lưu totalUrlsToCrawl = " + totalUrlsToCrawl);
    }

    // Các phương thức crawlChildPages vẫn giữ nguyên, có thể cần xem xét lại logic
    // tùy thuộc vào yêu cầu cụ thể về việc crawl trang con.
    private void crawlChildPagesSingle(String urlCha, Document doc, int parentId, int level, ConcurrentSkipListSet<String> crawledUrlChilds,
                                 AtomicLong crawledUrlStar1Count, DBHelperThuoc dbHelperThuoc, CrawlRuntime runtime) {
        if (doc == null) {
            return;        //return Collections.emptyList();
        }
        List<CrawlRecursiveAction> listCrawlChild = new ArrayList<>();

        try {
            Elements links = doc.select("div a[href*='DrugSearch'][title~=\\d+]");
            for (Element link : links) {
                if (!link.attr("title").contains("Next page")
                        && !link.attr("title").contains("Previous")) {  // Không phải link chứa "Next page" và "Previous"

                    String absUrlChild = link.absUrl("href");      //absUrlChild
                    if (absUrlChild.isEmpty()) continue;
                    //visitedUrls.putIfAbsent(absUrl,true);
                    if(crawledUrlChilds.contains(absUrlChild)
                            ||absUrlChild.equals(urlCha)) continue; // Kiểm tra xem URL con  đã được thêm vào luồng mới để crawl chưa


                    //tạo một List chỉ chứa một phần tử duy nhất là absUrlChild.
//                    listCrawlChild.add(new CrawlRecursiveAction(Collections.singletonList(absUrlChild), 0, 0, parentId, level + 1,crawledUrlChilds)); // Cách (1)
                    //Mới:
                    //String[] arrayUrlchild = new String[]{String.valueOf(parentId), String.valueOf(++level), absUrlChild}; //Đưa luôn parentId, level vào từng url con

                    UrlInfo urlInfo = new UrlInfo(absUrlChild, parentId, ++level, null,0, null, 0,
                            -1, System.currentTimeMillis()/1000);
                    listCrawlChild.add(new CrawlRecursiveAction(Collections.singletonList(urlInfo), 0, 0,
                            crawledUrlStar1Count, runtime, isCancelled)); // Cách (1)
                }
            }
            // Hoặc tạo các luồng task, mỗi task chứa hàng 1 url (cách 1)
            ForkJoinTask.invokeAll(listCrawlChild);  // Thực thi tất cả các task trong danh sách

        } catch (Exception e) {
            e.printStackTrace();
            //return Collections.emptyList();
        }
    } // End crawlChildPagesSingle

    private void crawlChildPages(String urlCha,
                                 Document doc,
                                 long parentId,
                                 int level,
                                 ConcurrentSkipListSet<String> crawledUrlChilds,
                                 AtomicLong crawledUrlStar1Count,
                                 DBHelperThuoc dbHelperThuoc,
                                 CrawlRuntime runtime) {

        Log.d(TAG, "crawlChildPages: urlCha=" + urlCha + " level=" + level);

        if (doc == null) {
            Log.w(TAG, "crawlChildPages: doc=null, bỏ qua " + urlCha);
            return;
        }

        try {
            List<CrawlRecursiveAction> listCrawlChild = new ArrayList<>();

            // Nếu không có "Next" => đây là trang cuối
            if (doc.select("div a[href*='DrugSearch'][title*='Next']").isEmpty()) {
                urlLast.add(urlCha);
                Log.d(TAG, "crawlChildPages: Đây là trang cuối -> " + urlCha);
            }

            Elements links = doc.select("div a[href*='DrugSearch'][title~=\\d+]");
            List<UrlInfo> listUrlInfosChild = new ArrayList<>();

            for (Element link : links) {
                String title = link.attr("title");
                if (title.contains("Next page") || title.contains("Previous")) {
                    continue; // bỏ qua nút Next/Prev
                }

                int iPage = Integer.parseInt(title);

                // ✅ Bỏ qua các trang đã crawl hoặc trùng với level hiện tại
                if (iPage <= level) {
                    Log.d(TAG, "crawlChildPages: Bỏ qua page=" + iPage +
                            " vì <= level hiện tại=" + level);
                    continue;
                }

                String absUrlChild = link.absUrl("href");
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //URLDecoder.decode(String, Charset) chỉ có từ API 33 (TIRAMISU).
                        //Trên API 33+, dùng overload mới với Charset để tránh UnsupportedEncodingException.
                        absUrlChild = URLDecoder.decode(absUrlChild, StandardCharsets.UTF_8);
                    } else {
                        //Trên API < 33, URLDecoder.decode(String, String) vẫn an toàn, miễn truyền "UTF-8".
                        absUrlChild = URLDecoder.decode(absUrlChild, "UTF-8");
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "crawlChildPages: UnsupportedEncodingException khi decode URL", e);
                }


                // ✅ Check trùng trong set
                if (!crawledUrlChilds.add(absUrlChild)) {
                    Log.d(TAG, "crawlChildPages: Bỏ qua url trùng -> " + absUrlChild);
                    continue;
                }

                Log.d(TAG, "crawlChildPages: Nạp url con (page=" + iPage + ") -> " + absUrlChild);

                // Lưu vào DB hàng đợi
                dbHelperThuoc.saveChildUrlToUrlQueueTable(
                        selectedCrawlType.getUrlQueueTableName(),
                        absUrlChild, parentId, iPage);

                // Tạo UrlInfo
                String maThuocP = "https://www..." + "key" + absUrlChild.split("key")[1];
                listUrlInfosChild.add(new UrlInfo(absUrlChild, parentId, iPage, maThuocP,
                        0, null, 0, -1, System.currentTimeMillis() / 1000));

                // Khi đủ THRESHOLD thì tạo task con
                if (listUrlInfosChild.size() >= THRESHOLD) {
                    List<UrlInfo> batch = new ArrayList<>(listUrlInfosChild);
                    listCrawlChild.add(new CrawlRecursiveAction(
                            batch, 0, batch.size(), crawledUrlStar1Count, runtime, isCancelled));
                    Log.d(TAG, "crawlChildPages: tạo task con batch size=" + batch.size());
                    listUrlInfosChild.clear();
                }
            }

            // Batch còn lại (< THRESHOLD)
            if (!listUrlInfosChild.isEmpty()) {
                List<UrlInfo> batch = new ArrayList<>(listUrlInfosChild);
                listCrawlChild.add(new CrawlRecursiveAction(
                        batch, 0, batch.size(), crawledUrlStar1Count, runtime, isCancelled));
                Log.d(TAG, "crawlChildPages: tạo task con cuối cùng size=" + batch.size());
            }

            // Gọi invokeAll
            if (!listCrawlChild.isEmpty()) {
                Log.d(TAG, "crawlChildPages: invokeAll với " + listCrawlChild.size() + " tasks");
                ForkJoinTask.invokeAll(listCrawlChild);
            }

        } catch (Exception e) {
            Log.e(TAG, "crawlChildPages: Lỗi khi xử lý urlCha=" + urlCha, e);
        }
    }

    @Override
    @NonNull
    public ForegroundInfo getForegroundInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return createForegroundInfo(0, 100,"","Bắt đầu crawl...");
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    private ForegroundInfo createForegroundInfo( int currentProgress, long maxProgress, String currentUrl, String message) {
        //Nếu không addAction THÌ KHÔNG CẦN Intent
//        Intent intent = new Intent(context, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
//        String cancel = context.getString(R.string.cancel_download);
        //// TẠO KÊNH CHANEL: createNotificationChannel
        //createNotificationChannel();    //Chỉ tạo 1 lần. Trong MyApplication đã tạo rồi
        //

        String contentText = "Đã xử lý: " + currentProgress + "/" + maxProgress + " URL(start=1).";
        if (currentUrl != null && !currentUrl.isEmpty()) {
            //contentText += " Đang cào: " + currentUrl;  // Gốc
            String ky_Tu_Search = "key=" + currentUrl.split("&key=")[1];
            contentText = "Đã xử lý: " + currentProgress + "/" + maxProgress + ". Đang cào: " + ky_Tu_Search;
        }
        // Đã có chanel: MyApplication.CRAWLER_CHANNEL_ID)
        //NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Xây dựng lại notification với thông tin mới
        Notification updatedNotification = new NotificationCompat.Builder(context, MyApplication.CRAWLER_CHANNEL_ID)
                //.setSmallIcon(R.drawable.baseline_build_24) // Đảm bảo icon baseline_build_24 này tồn tại
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Crawl thuốc biệt dược")   //("Đang Crawl Dữ Liệu")
                .setContentText(contentText)
                //.setContentText(currentProgress + "%")
                //.setProgress((int) maxProgress, currentProgress, false)  // `false` cho tiến độ xác định
                .setProgress(100, currentProgress, false)
                .setOnlyAlertOnce(true) // Tránh thông báo kêu/rung liên tục khi cập nhật
                .setOngoing(true) // 🔹 Không cho tắt
                .setPriority(NotificationCompat.PRIORITY_LOW) // Giữ độ ưu tiên thấp
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                //.addAction(android.R.drawable.ic_delete, cancel, pendingIntent)
                .build();

        // Nên sử dụng ServiceInfo Khi bạn muốn tuân thủ các quy tắc Android 12+ (API 31+) và chỉ định rõ ràng mục đích của dịch vụ tiền cảnh.
        return new ForegroundInfo(NOTIFICATION_ID, updatedNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC); // Nên sử dụng
        //Các trường hợp cơ bản, hoặc khi bạn không cần chỉ định loại cụ thể hoặc nhắm mục tiêu API < 29.
        //return new ForegroundInfo(NOTIFICATION_ID, updatedNotification)
    }

    private Data createOutputData(boolean success, String message) {
        return new Data.Builder()
                .putBoolean("success", success)
                .putString("message", message)
                .putString("work_request_id", getId().toString()) // Trả lại ID của WorkRequest
                .build();
    }

    private static boolean containsCause(Throwable t, Class<?> cls) {
        while (t != null) {
            if (cls.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }
    //////// Mới
    public class CrawlRuntime {
        private final BlockingQueue<Packet> queue;
        private final WriterEngineChunked writerEngine;
        private final CheckpointWorker cp; // WAL checkpoint monitor
        private final SQLiteDatabase db;
        private final AtomicBoolean writerBusy;
        private final AtomicBoolean running = new AtomicBoolean(true);
        // ✅ Đếm số nhánh crawler đang hoạt động
        public final AtomicInteger activeCrawlerCount = new AtomicInteger(0);

        private final Thread monitorThread;
        //private final Thread writerThread;
        public CrawlRuntime(Context ctx) {

            DBHelperThuoc helper = DBHelperThuoc.getInstance(ctx);
            db = helper.getWritableDatabase();    //Gốc
//            db = helper.getDb();  //Singleton
            // Cũ
//            queue = new LinkedBlockingQueue<>(500);
//            writer = new WriterEngine(db, queue, selectedCrawlType.getTableThuocName());
            // Mới
            queue = new LinkedBlockingQueue<>(2000);
            writerEngine = new WriterEngineChunked(selectedCrawlType, db, queue, 100); // chunk = 100
            //
            writerBusy = writerEngine.writerBusy;
            // CheckpointWorker đã tích hợp getDbPath() từ DBHelperThuoc
            cp = new CheckpointWorker(
                    context,
//                    () -> !writerBusy.get() && queue.isEmpty(),
                    () -> queue.isEmpty() && !writerEngine.writerBusy.get(),
                    10 * 1024 * 1024,   // 10MB WAL limit
                    2000                            // check mỗi 2s
            );

            monitorThread = new Thread(() -> {
                Log.i(TAG, "Monitor thread started");
                try {
                    //isStopped(): CỦA CrawlRuntime runTime
                    while (!isStopped()) {  //Cách này tự nhiên hơn, Hoặc while (running.get()) {
                        Thread.sleep(2000);
                        //Có thể kiểm tra thêm !isStopped(): CỦA CrawlRuntime runTime để chắc chắn chưa dừng:
                        if (!isStopped() && activeCrawlerCount.get() == 0 &&
                                queue.isEmpty() &&
                                !writerBusy.get()) {
                            stop();
                            break;
                        }
                    }
                } catch (InterruptedException ignored) {}
            }, "RuntimeMonitor");
        }

        public void start() {
            writerEngine.start();
            cp.start();
            monitorThread.start();
        }

        /** Gửi tín hiệu dừng writer, không block */
        public void stopAsync() {
            Log.i("CrawlRuntime", "Stopping (async)...");
            try {
                writerEngine.shutdown();
                running.set(false); // ✅ update flag
                Log.i("CrawlRuntime", "Stop signal sent (writer still shutting down)...");
            } catch (Exception ex) {
                Log.e("CrawlRuntime", "Stop async failed", ex);
            }
        }

        /** Block chờ writerThread dừng hẳn */
        public void awaitStopped() {

            // 1. Chờ executor: KHÔNG DÙNG.
//            if (executor != null) {
//                try {
//                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
//                        Log.w(TAG, "Executor chưa dừng, buộc shutdownNow()");
//                        executor.shutdownNow();
//                    }
//                } catch (InterruptedException e) {
//                    Log.w(TAG, "awaitStopped() bị interrupt", e);
//                    Thread.currentThread().interrupt();
//                }
//            }

            // 2. Chờ monitorThread
            // Nếu monitorThread còn chạy thì: Ngắt monitorThread để thoát sleep nhanh
            if (monitorThread != null && monitorThread.isAlive()) {
                try {
                    monitorThread.join(2000); // chờ tối đa 2 giây
                    //monitorThread.join();        // ✅ chờ writerEngine dừng hẳn. writerThread = monitorThread
                    if (monitorThread.isAlive()) {
                        Log.w("CrawlRuntime", "MonitorThread chưa dừng hẳn sau join");
                    }
                    Log.i("CrawlRuntime", "Writer stopped completely");
                } catch (InterruptedException e) {
                    Log.w("CrawlRuntime", "awaitStopped() join monitorThread bị interrupt", e);
                    Thread.currentThread().interrupt();
                    //Log.e("CrawlRuntime", "awaitStopped interrupted", e);
                }
            }
            // 3. Log trạng thái isStopped()
            Log.d(TAG, "awaitStopped() finished, isStopped=" + isStopped());

        }

        /** Wrapper gọn gàng: vừa stopAsync vừa chờ join */
        public void stop() {
            /* Cách này đảm bảo:
             * stop() gọi xong thì executor + monitor thread đều dừng hoàn toàn.
             * Không còn delay 2 giây vô ích.
             * Log rõ ràng để debug.
             */
            Log.d("CrawlRuntime", "Stopping (sync wrapper)...");
            // Mới
            // B1. Gửi tín hiệu dừng (writerEngine + flag running): Đặt cờ và shutdown executor
            //stopAsync(): (HÌNH NHƯ CHỈ DÀNH CHO UI?) chỉ gửi tín hiệu, không join) để giữ cho UI thread không bị block khi người dùng bấm "Stop"
            stopAsync();    //sẽ set stopped = true + executor.shutdown().
            // B2. Ngắt monitorThread nếu còn sống (đang sleep)
            if (monitorThread != null && monitorThread.isAlive()) {
                monitorThread.interrupt();  //phá ngay Thread.sleep(2000), làm nó thoát tức thì thay vì chờ.
            }

            // B3. Chờ writerEngine (thread riêng) thoát (block chờ writer dừng hẳn)
            try {
                if (writerEngine != null && writerEngine.isAlive()) {
                    Log.i(TAG, "Waiting writerEngine to stop...");
                    writerEngine.join();   // ✅ chờ writer thread kết thúc(block cho đến khi thread kết thúc)
                    Log.i(TAG, "WriterEngine stopped");
                }
            } catch (Exception e) {
                Log.w("CrawlRuntime", "Interrupted while waiting writerEngine", e);
                Thread.currentThread().interrupt();
            }

            // B4. Chờ CheckpointWorker (thread riêng) thoát
            try {
                if (cp != null && cp.isAlive()) {
                    cp.stopWorker();   // gửi tín hiệu dừng
                    Log.d(TAG, "Waiting checkpointWorker to stop...");
                    cp.join();         // chờ thread kết thúc
                    Log.d(TAG, "CheckpointWorker stopped");
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting checkpointWorker", e);
                Thread.currentThread().interrupt();
            }

            // B5. Chờ monitorThread thoát
            try {
                if (monitorThread != null && monitorThread.isAlive()) {
                    Log.i(TAG, "Waiting monitorThread to stop...");
                    monitorThread.join(2000);  // timeout để tránh block vô hạn
                    if (monitorThread.isAlive()) {
                        Log.w(TAG, "MonitorThread chưa dừng hẳn sau join");
                    } else {
                        Log.i(TAG, "MonitorThread stopped");
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting monitorThread", e);
                Thread.currentThread().interrupt();
            }

            //B3. Chờ executor kết thúc: Chờ mọi thứ dừng hẳn
            // 4. Chờ monitorThread dừng hẳn: BỎ
            //awaitStopped(); //awaitStopped() sẽ awaitTermination hoặc join() đảm bảo executor thật sự kết thúc.
            //Log.i("CrawlRuntime", "Stopped OK");


            // Cũ
//            if (!running.getAndSet(false)) return;
//            try {
//                writerEngine.shutdown();
//                // ChatGPT đề nghị bổ sung
//                writerEngine.join(); // ✅ chờ writer dừng hẳn
//                //
//            } catch (Exception ignore) {}
//
//            try { cp.stopWorker(); } catch (Exception ignore) {}

            // End Cũ

            // SLEEP cho worker còn lại kịp dừng

            // WAL checkpoint cuối (tùy chọn)
            // Dùng pp khác : KHÔNG CẦN sleep(300)
//            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            // checkpoint cuối cùng + đóng DB được xử lý trong DBHelper/Writer tuỳ thiết kế của đệ tử
            //Vì stop() là lúc dừng worker, đệ tử chỉ cần checkpoint
            // và không quan tâm kết quả thì vẫn phải gọi rawQuery() nhưng không cần log:
            //1. KHÔNG CẦN KẾT QUẢ: CHỈ CẦN 2 LỆNH
//            Cursor c = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null);    //Hãy ép SQLite ghi dữ liệu từ WAL vào DB chính, xóa file WAL, và tôi không quan tâm thống kê kết quả.”
//            c.close();
//            // RỒI
//            db.close();
            // END cũ.

            //2. MUỐN LẤY KẾT QUẢ(KHÔNG CẦN) THÌ LÀM NHƯ SAU:

            // B6. WAL checkpoint cuối cùng (tùy chọn)
            if (db != null && db.isOpen()) {
                try (Cursor c = db.rawQuery("PRAGMA wal_checkpoint(PASSIVE)", null)) {
                    // Ngắn gọn: chỉ cần Log sau:
                    Log.d(TAG, "Final checkpoint executed");

                    // Xem Tham khảo
                    // ✅ Kiểm tra số logFrames trong WAL hiện tại
                    if (c.moveToFirst() && c.getInt(1) > 0) {
                        int busy = c.getInt(0);
                        int logFrames = c.getInt(1);
                        int checkpointedFrames = c.getInt(2);

                        Log.d("CrawlRuntime", String.format(
                                "[Check WAL] busy=%d, logFrames=%d, checkpointed=%d",
                                busy, logFrames, checkpointedFrames
                        ));

                        // Nếu WAL còn dữ liệu, mới chạy checkpoint TRUNCATE
                        if (logFrames > 0) {
                            // CHỦ YẾU ĐOẠN NÀY LÀ TRUNCATE
                            Log.d("CrawlRuntime", "WAL has data → Running TRUNCATE checkpoint...");
                            try (Cursor c2 = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
                                if (c2.moveToFirst()) {
                                    Log.d(TAG, String.format(
                                            "TRUNCATE result → busy=%d, logFrames=%d, checkpointed=%d",
                                            c2.getInt(0), c2.getInt(1), c2.getInt(2)
                                    ));
                                }
                            }
                        } else {
                            Log.d("CrawlRuntime", "WAL is empty → Skip checkpoint");
                        }
                    }

                    // END Xem Tham khảo
                } catch (Exception e) {
                    Log.e(TAG, "CrawlRuntime: Error checking/cleaning WAL", e);
                }

                // B7. ✅ Đóng DB
                try {
                    // Vì dùng DBHelperThuoc là singleton: Nghĩa là trong suốt vòng đời app,
                    // toàn bộ code dùng một DBHelper, và DBHelper thường tự quản lý DB connection.
                    // Nếu app còn sống, không cần gọi db.close() thủ công mỗi lần xong việc.
                    //
                    //Thực tế Android team cũng khuyến nghị: chỉ đóng DB khi app thực sự thoát (ví dụ trong Application.onTerminate() hoặc khi chắc chắn không crawl thêm gì nữa).
                    db.close(); // ✅ giờ mới an toàn(Có nên đóng db?)
                    Log.d(TAG, "Database closed after optional checkpoint");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing database", e);
                }
            }

            Log.i(TAG, "Stopped OK");
        }


        /** ✅ Check runtime đã dừng hay chưa
         * Trả về true nếu executor đã shutdown & monitorThread đã chết.
         */
        public boolean isStopped() {
            //isStopped() dùng để check cuối cùng hoặc trong debug UI.

            //1. Cách cũ
            //running vẫn giữ vai trò “nội bộ” (internal state).
            //
            //isStopped() chỉ là API thân thiện, giúp code đọc như tiếng Việt/Anh tự nhiên:
            //return !running.get();  //running vẫn giữ vai trò “nội bộ” (internal state).

            //2. CÁCH MớI
            //boolean executorTerminated = (executor == null || executor.isTerminated());   // KHÔNG DÙNG
            //boolean monitorDead = (monitorThread == null || !monitorThread.isAlive());
            //return stopped && executorTerminated && monitorDead;
            return (monitorThread == null || !monitorThread.isAlive());

        }

        public void submit(Packet p) {
            // Mới
            try {
                if (p != null) queue.put(p);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }
    // ⚡ Yêu cầu bỏ qua battery optimization
    private void requestIgnoreBatteryOptimizations() {
        Context context = getApplicationContext();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    private void setProgressStartBang1 (long parentId, String currentUrl) {
        if (currentUrl.endsWith("start=1")) { // Luôn luôn đếm trước, nằm ngoài ĐK time
            Log.d(TAG, "compute: TRƯỚC tăng crawledUrlStar1Count,  Số url (có start=1) =" + crawledUrlStar1Count.get());
            // Tăng bộ đếm an toàn và lưu vào SharedPreferences
            /**crawledUrlStar1Count.incrementAndGet(); // Tăng bộ đếm an toàn.
             * LỆNH SAU LÀM crawledUrlStar1Count TĂNG LÊN 1: settingsRepository.saveProcessedUrlsStart1Count
             */
            settingsRepository.saveCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey(), crawledUrlStar1Count.incrementAndGet());
            Log.d(TAG, "compute: SAU tăng crawledUrlStar1Count, Số url (có start=1) =" + crawledUrlStar1Count.get());
            Log.d(TAG, "compute: SAU tăng crawledUrlStar1Count, currentUrl =" + currentUrl + ";parentId=" + parentId);

            percent = (int) (crawledUrlStar1Count.get() * 100.0 / totalUrlsToCrawl);
            // Tính toán thời gian đã trôi qua hiện tại
            //long currentElapsedTime = System.currentTimeMillis() - startTime;

        }// đổi chỗ = 1

        // Tính toán thời gian đã trôi qua CỘNG DỒN
        currentCumulativeElapsedTime = lastSavedCumulativeElapsedTime + (System.currentTimeMillis() - currentSessionStartTime);

        /** Ước tính thời gian còn lại (dựa trên thời gian CỘNG DỒN):
         * PHẢI NĂM TRONG ĐK START = 1, VÌ PHỤ THUỘC crawledUrlStar1Count
         */
        estimatedRemainingTime = 0;
        if (crawledUrlStar1Count.intValue() > 0 && totalUrlsToCrawl > 0) {
            // Tỷ lệ hoàn thành (0.0 đến 1.0)
            double completionRatio = (double) crawledUrlStar1Count.intValue() / totalUrlsToCrawl;
            if (completionRatio > 0) { // Tránh chia cho 0
                // Ước tính tổng thời gian = Thời gian đã trôi qua / Tỷ lệ hoàn thành
                long estimatedTotalTime = (long) (currentCumulativeElapsedTime  / completionRatio);
                estimatedRemainingTime = estimatedTotalTime - currentCumulativeElapsedTime;
            }
        }
        // Đảm bảo thời gian còn lại không âm
        estimatedRemainingTime = Math.max(0, estimatedRemainingTime);
        // Cập nhật dựa trên thời gian
        // Chỉ cập nhật UI nếu đã quá UI_UPDATE_INTERVAL_MS
        // và cố gắng so sánh-và-trao đổi (CAS) giá trị lastUpdateTime
        // Chỉ cập nhật UI nếu đã quá UI_UPDATE_INTERVAL_MS
        // và cố gắng so sánh-và-trao đổi (CAS) giá trị lastUpdateTime
        long currentTime = System.currentTimeMillis();
        // Lấy giá trị hiện tại của lastUpdateTime, sau đó cố gắng cập nhật
        long currentLastUpdateTime = lastUpdateTime.get();  // THỜI ĐIỂM NÀY
        // DÒNG LOG MỚI ĐỂ XEM TRẠNG THÁI CỤ THỂ TRƯỚC COMPAREANDSET
        Log.d(TAG, "PRE_CAS_CHECK: URL=" + currentUrl +
                ", ThreadID=" + Thread.currentThread().getId() +
                ", currentTime=" + currentTime +
                ", currentLastUpdateTime (đọc)=" + currentLastUpdateTime +
                ", lastUpdateTime.get() (thực tế)=" + lastUpdateTime.get() + // Đây là giá trị quan trọng
                ", TimeDiff (thực tế)=" + (currentTime - currentLastUpdateTime) +
                ", IntervalMet=" + (currentTime - currentLastUpdateTime > UI_UPDATE_INTERVAL_MS));
        if (currentTime - currentLastUpdateTime  > UI_UPDATE_INTERVAL_MS) { // Cập nhật mỗi 200ms là tối ưu
            // Cố gắng cập nhật lastUpdateTime. Nếu thành công, có nghĩa là luồng này là luồng đầu tiên đạt ngưỡng.
            // XEM GIÁ TRỊ lastUpdateTime.get ở thời điểm này có khác currentLastUpdateTime do 1 luồng nào đó đã CẬP NHẬT THÀNH CÔNG
            Log.d(TAG, "compute: COMPAREANDSET. XEM LẠI GIÁ TRỊ lastUpdateTime. CHÊNH LỆCH= " +
                    (lastUpdateTime.get() - currentLastUpdateTime) +
                    "; " + ((lastUpdateTime.get() - currentLastUpdateTime)==0) );
            //if (lastUpdateTime.compareAndSet(currentLastUpdateTime, currentTime)
            //        || crawledUrlStar1Count.get() == totalUrlsToCrawl) {
            if (lastUpdateTime.compareAndSet(currentLastUpdateTime, currentTime)) {
                // Đây là khối code chỉ chạy khi thành công
                Log.d(TAG, "COMPAREANDSET THÀNH CÔNG! lastUpdateTime mới: " + currentTime +
                        " (Chênh lệch: " + (currentTime - currentLastUpdateTime) + "ms)" +
                        "\n*crawledUrlStar1Count.get()=" + crawledUrlStar1Count.get() +
                        "\n*totalUrlsToCrawl=" + totalUrlsToCrawl +
                        "\n*PERCENT = " + (int) (crawledUrlStar1Count.get() * 100.0 / totalUrlsToCrawl));

                Log.d(TAG, "compute: 2/ Khoảng thời gian SAU ĐK  = " + (currentTime - currentLastUpdateTime) + "ms" +
                        "\nSố lượng url đã crawl=" + crawledUrlCount.get() +
                        "\ncrawledUrlStar1Count = " + crawledUrlStar1Count.get() +
                        "\ntotalUrlsToCrawl=" + totalUrlsToCrawl +
                        "\nlastUpdateTime.compareAndSet(currentLastUpdateTime, currentTime)=" + (lastUpdateTime.compareAndSet(currentLastUpdateTime, currentTime)) +                                    "\ncrawledUrlStar1Count.get() == totalUrlsToCrawl:" + (crawledUrlStar1Count.get() == totalUrlsToCrawl) +
                        "\n(currentTime - currentLastUpdateTime  > UI_UPDATE_INTERVAL_MS || crawledUrlStar1Count.get() == totalUrlsToCrawl) =" + ((currentTime - currentLastUpdateTime  > UI_UPDATE_INTERVAL_MS || crawledUrlStar1Count.get() == totalUrlsToCrawl )) +
                        "\nPERCENT = " + percent +
                        "\ncurrentUrl=" + currentUrl + "\n==========================");
//                                  Log.d(TAG, "compute percent: SAU TÍNH PERCENT, crawledUrlStar1Count=" + crawledUrlStar1Count.get() +
//                                        "; totalUrlsToCrawl =" + totalUrlsToCrawl + "\n; currentUrl=" + currentUrl);
                Log.d(TAG, "compute percent: ĐÃ TÍNH PERCENT =" + percent + "%" +
                        "; crawledUrlStar1Count=" + crawledUrlStar1Count.get() +
                        "; totalUrlsToCrawl =" + totalUrlsToCrawl + "\n; currentUrl=" + currentUrl);
                Data progressData = new Data.Builder()
                        .putInt(AppConstants.WORK_PROGRESS_PERCENT, percent)
                        //
                        .putLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), crawledUrlCount.get())   //iSumProcessedUrlsCount
                        .putLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), crawledUrlStar1Count.get())
                        .putLong(selectedCrawlType.getTotalUrlsPrefKey().name(), totalUrlsToCrawl)
                        .putString(AppConstants.URL_CURRENT, currentUrl)
                        .putLong(AppConstants.WORK_ELAPSED_TIME, currentCumulativeElapsedTime) // <-- Thêm vào progress data
                        .putLong(AppConstants.WORK_REMAINING_TIME, estimatedRemainingTime) // <-- Thêm vào progress data
                        // Thông tin Threads
                        .build();

                //Log.d("CrawlWorker", "compute:Đến TRƯỚC ĐẦU setProgressAsync(): time =" + (double) (System.currentTimeMillis() - t0StartWorkButton) / 1000 + "(s)");
                setProgressAsync(progressData);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Phải có tránh Kill
                    setForegroundAsync(createForegroundInfo(0, totalUrlsToCrawl, null, "")); // "Đang khởi tạo..."
                }
                //updateNotificationProgress(crawledUrlStar1Count.intValue(), totalUrlsToCrawl);
                // *** LƯU TRỮ currentCumulativeElapsedTime vào SharedPreferences MỖI KHI CẬP NHẬT PROGRESS ***
                settingsRepository.saveLongElapsedTime(selectedCrawlType, currentCumulativeElapsedTime);

                Log.d("CrawlWorker", "compute urlLast=" + currentUrl);
                Log.d("CrawlWorker", "compute urlLast: setProgressAsync progressData=" + progressData);
            } else { //Xem log last time
                // Đây là khối code chỉ chạy khi thất bại
                Log.d(TAG, "COMPAREANDSET THẤT BẠI. ThreadID=" + Thread.currentThread().getId() +
                        ", currentLastUpdateTime (mong muốn): " + currentLastUpdateTime +
                        ", lastUpdateTime.get() (thực tế tại thời điểm thất bại): " + lastUpdateTime.get() +
                        ", LÝ DO THẤT BẠI: giá trị thực tế khác giá trị mong muốn. Chênh lệch: " + (lastUpdateTime.get() - currentLastUpdateTime));
            }
        }   // >

    }


    ////////////////////////////////////////
} // End class MyWorkerCrawler