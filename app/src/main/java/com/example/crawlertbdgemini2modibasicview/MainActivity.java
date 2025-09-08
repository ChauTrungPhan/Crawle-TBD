package com.example.crawlertbdgemini2modibasicview;
//CỦA EXCEL PP MỚI
import android.Manifest;
import android.app.AlertDialog;
//
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.WorkInfo;


import com.example.crawlertbdgemini2modibasicview.databinding.ActivityMainBinding;
import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.PrefKey;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;
import com.example.crawlertbdgemini2modibasicview.utils.SharedPreferencesUtils;
import com.example.crawlertbdgemini2modibasicview.utils.Utils;
import com.google.android.material.appbar.MaterialToolbar;

import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STHexColorRGB;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
// Hỏi thêm ChatGPT

//public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener,
//        SettingsDialogListener, ExcelExporterOptimized.ExportCallbacks {    // Cũ

//public class MainActivity extends AppCompatActivity implements ExcelExporterOptimized.ExportCallbacks {   // Mới
public class MainActivity extends AppCompatActivity {   // Mới
    private static final String TAG = "MainActivity";
    /** CÁC BIẾN PHỤ: CÓ THỂ BỎ
     *Cho phép bật/tắt disable nút khi ENQUEUED
     **/
    // Của Crawl
    private WriterEngine writerEngine;  // ✅ khai báo ở đây
    private boolean cancelledWorker;

    private static final boolean DISABLE_BUTTONS_WHEN_ENQUEUED = true;
    private long crawlerStartTimeMillis = -1;
    // Khai báo biến trạng thái
    private boolean isUIRunningStateSet = false;
    // Của 1 nút btnCrawlData: có 2 chức năng: CHẠY+HỦY
    private boolean isCrawling = false;      // crawler đang chạy
    private boolean isStartingOrStopping = false; // để chặn double click khi đang đổi trạng thái

    private WorkInfo.State lastState = null;    // Mục đích chỉ Log để theo dõi trạng thái worker

    // Màu ANSI cho Logcat: MỤC ĐÍCH PHÂN BIỆT LOGCAT BÁO. LÀ BIẾN PHỤ CÓ THẺ BỎ.
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    //
    private final List<File> excelFileList = new ArrayList<>();
    //Của excel
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    //private static final Logger log = LogManager.getLogger(MainActivity.class);
//    private static String FILE_NAME; // = "ThuocBietDuoc_" + System.currentTimeMillis(); // Tên file động
    private Menu optionsMenu; // Biến để giữ tham chiếu đến Menu
//    private boolean isSettingsItemDisabled = false; // Biến cờ để kiểm soát trạng thái
    private MenuItem settingsMenuItem; // Biến để lưu trữ, tham chiếu đến menu item cài đặt(item menu cần disable)

    // Khai báo flag
    private boolean isInitialLoadSettings = true;
    public static final String KEY_IS_FIRST_RUN_FOR_WORK = "is_first_run_for_work"; // Cờ này luôn là true khi WorkRequest mới được tạo
    //public static final String KEY_LIST_ROW_URLS_JSON = "list_row_urls_json"; // Key cho JSON string

    private String tableThuoc;
    private final String ELAPSED_TIME = "elapsed_time"; // Thời gian đã trôi qua kể từ khi bắt đầu crawl elapsedTimes
    //public static final String ELAPSED_TIME = "elapsed_time"; // Thời gian đã trôi qua kể từ khi bắt đầu crawl elapsedTimes

    //private static File directory = FileUtils.getDir(Environment.DIRECTORY_DOWNLOADS);
    //static String excelFileName = "ThuocBietDuoc.xlsx";  //filename
    private static CrawlType selectedCrawlType = null;
    private int k0, k1;
    private int totalRecords; //phu thuoc k0, k1 của phiên làm việc:KHÔNG ĐỔI

    private ActivityResultLauncher<Intent> settingsActivityResultLauncher;
    //private ActivityResultLauncher<String> requestPermissionLauncher;
    // Khai báo ActivityResultLauncher

    private long startTime;
    //long elapsedTimesPhienTruoc;
    //private Boolean chọnCrawlerTheoURL = true;  //Mặt định là true

    private Spinner btnSpinner;
    private MyViewModelCrawler_Gemini viewModelCrawlerGemini;
    private MyViewModelCreateExcel_Gemini viewModelCreateExcel_gemini;  // ĐƯỢC THAY THẾ BỞI ExportViewModel

    private final ArrayList<Integer> indexKyTuList = new ArrayList<>();
    //    String[] kytuArray = {"Monday", "Tuesday","Wednesday"
//    ,"Thursday", "Friday", "Saturday", "Sunday"};
    //String[] kytuArray = GetArray3KyTu.getArray_3KyTuKhongLap(this);    //Làm gì

    //    private EditText editNumFromK0, editNumToK1;

    /************************************/
    //String selectedIdName;
    /**
     * Của excel theo pp mới: Quá trình diễn tiến THÔNG QUA ExportProgressActivity
     * Nếu dùng bind thì không cần khai báo
     */

    // KHAI BÁO VÀ KHỞI TẠO excelExecutor MỘT LẦN DUY NHẤT Ở ĐÂY: Để quản lý luồng nền
    private final ExecutorService excelExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler; // Để cập nhật UI từ luồng nền
    private long exportStartTime; // Để tính toán thời gian trôi qua
    //private ExcelExporterOptimized_goc excelExporterOptimized;

    /**
     * Của Dialog: Xuất Excel: Quá trình diễn tiến theo Hộp Dialog:
     * Nếu dùng bind thì không cần khai báo
     */
    private AlertDialog dialogAlertExcel;
    private ProgressBar dialogExcelProgressBar;
    private TextView dialogExcelProgressText;
    private TextView dialogExcelElapsedTimeText;
    private TextView dialogExcelRemainingTimeText;
    //private Handler dialogMainHandler; // Để cập nhật UI từ luồng nền

    ///  Mới toanh
    private ActivityMainBinding binding; // View Binding instance
    //private MyViewModelCrawler_Gemini viewModelCrawlerGemini;
    private SettingsRepository settingsRepository;  //Thêm SettingsRepository
    private AppDatabase appDatabase;
    private WorkStateDao workStateDao;

    private DBHelperThuoc dbHelperThuoc;
    //private SQLiteDatabase dbThuoc;
    private ExportViewModel exportViewModel; // Khai báo ExportViewModel
    ///

    // Khai báo các ActivityResultLauncher ở CẤP ĐỘ LỚP
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> manageAllFilesLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher; // Thêm launcher này nếu bạn dùng cho việc chọn file
    //private ActivityResultLauncher<Intent> createDocumentLauncher; // Thêm launcher này nếu bạn dùng cho việc xuất file (SAF)
    // Biến để lưu trạng thái isXml tạm thời
    private final boolean isExportingXml = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // Initialize View Binding
        setContentView(binding.getRoot());  // Set the root view
        ///
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false); // Cho phép nội dung kéo dài
        window.setStatusBarColor(ContextCompat.getColor(this.getApplicationContext(), android.R.color.transparent)); // Thanh trạng thái trong suốt
        // Xóa giá trị cũ để đảm bảo nó được lưu lại dưới dạng long: RẤT VÔ LÝ! KHÔNG BIẾT TẠI SAO
        //SharedPreferencesUtils.remove(this, PrefKey.CRAWLED_URLSTART1_2KT_COUNT);   // Tại sao có lệnh này?
        //
        setSupportActionBar(binding.toolbar);   // Set Toolbar
        if (getSupportActionBar() != null) {
            // Thêm nút back (nếu cần)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Tạo nút Back (trở về)
            getSupportActionBar().setTitle(R.string.app_name);
        }

/////////////////////////////
        // ... (ánh xạ các nút, ProgressBar, TextView status/progress khác): initViews()
        //initViews();    //trong onCreate()
        // 1. KHỞI TẠO CÁC ĐỐI TƯỢNG CẦN THIẾT
        //ĐẦU TIÊN settingsRepository PHẢI CÓ ĐỂ TÍNH CÁC THÔNG SỐ KHÁC, SẼ LẤY ĐƯỢC CRAWLTYPE
        settingsRepository = new SettingsRepository(this);
        //PHẢI CÓ ĐỂ selectedCrawlType KHÔNG null
        selectedCrawlType = settingsRepository.getSelectedCrawlType();  // getSelectedCrawlType() đã có Mặc định là TWO_CHAR
        appDatabase = AppDatabase.getInstance(this.getApplicationContext()); // Lấy thể hiện của Room Database
        workStateDao = appDatabase.workStateDao(); // Lấy DAO

        dbHelperThuoc = DBHelperThuoc.getInstance(this.getApplicationContext());
        //if (dbHelperThuoc.getCountErrorUrls(selectedCrawlType.getUrlQueueTableName()) > 0) {
        if (dbHelperThuoc.hasErrorUrls(selectedCrawlType)) {
            binding.tvSoUrlErrors.setVisibility(View.VISIBLE);
            binding.btnViewErrors.setVisibility(View.VISIBLE);
        }

        tableThuoc = selectedCrawlType.getTableThuocName();
        // Khởi tạo ViewModel
        viewModelCrawlerGemini = new ViewModelProvider(this, new CrawlerViewModelFactory(getApplication())).get(MyViewModelCrawler_Gemini.class);

        // Khởi tạo ExportViewModel: Cho xuất tạo file excel từ dbThuoc
        exportViewModel = new ViewModelProvider(this, new ExportViewModelFactory(getApplication())).get(ExportViewModel.class);
        //exportViewModel = new ViewModelProvider(this).get(ExportViewModel.class);

        //2. KHAI BÁO CÁC ĐỐI TƯỢNG UI: TEXTVIEW, BUTTON, spinner, Listener các nút btn, và k0, k1 (TextWatcher)...
        // --- RẤT QUAN TRỌNG: Gọi phương thức này ngay trong onCreate ---
        setupUI();  // Cho spinner, Listener các nút btn và k0, k1 (TextWatcher). Đang có updateUIState(false);

        //3. RESTORE: DÙNG setupUI(): LẤY LẠI CÁC THÔNG SỐ CŨ ĐÃ LƯU, ĐỂ SET LẠI UI
        // loadSettings: Đặt trong onResume()
        loadSettings(); // Load cài đặt K0, K1, CrawlType, cancelledWorker ĐÃ LƯU khi khởi động app

        //4. QUAN SÁT TRẠNG THÁI CỦA WORKER MANAGER: 1 LẦN TRONG ONCREATE()
        /** Phương thức checkAndObserveWorkerState có chức năng khác observeViewModel
         * checkAndObserveWorkerState: Chỉ xem xét trang thái của worker 1 lần khi bắt đầu onCreate() của MainActivity
         *
         * observeViewModel: LUÔN LUÔN QUAN SÁT TRẠNG THÁI CỦA WORKER MANAGER
         */
        observeViewModel(); // Khởi tạo và quan sát ViewModel, quan sát trạng thái Worker qua workInfoLiveData

        //5. KHỞI TẠO CÁC ActivityResultLauncher:
        // Khởi tạo ActivityResultLauncher
        chooseFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            Log.d(TAG, "File được chọn: " + selectedFileUri.toString());
                            Toast.makeText(this, "Đã chọn file: " + selectedFileUri.getLastPathSegment(), Toast.LENGTH_LONG).show();
                            // Tại đây bạn có thể xử lý Uri của file đã chọn
                            // Ví dụ: đọc nội dung file, hiển thị tên file, v.v.
                            // TODO: Xử lý Uri của file đã chọn tại đây (ví dụ: đọc nội dung)
                        }
                    } else {
                        Toast.makeText(this, "Không có file nào được chọn.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        /// ////////

        // 5A. Khai báo ActivityResultLauncher của bạn: (đây là nơi xử lý kết quả các quyền)
        // Launchers for permissions and settings
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissionsGranted -> {

                    // permissionsGranted là một Map<String, Boolean> chứa kết quả của từng quyền
                    boolean allStoragePermissionsGranted = true;
                    // Kiểm tra quyền READ/WRITE_EXTERNAL_STORAGE (chỉ cần cho API <= 29)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        if (Boolean.FALSE.equals(permissionsGranted.get(Manifest.permission.READ_EXTERNAL_STORAGE)) ||
                                Boolean.FALSE.equals(permissionsGranted.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                            allStoragePermissionsGranted = false;
                        }
                    }

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 và thấp hơn
                        if (permissionsGranted.containsKey(Manifest.permission.WRITE_EXTERNAL_STORAGE) && !permissionsGranted.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            allStoragePermissionsGranted = false;
                        }
                        if (permissionsGranted.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE) && !permissionsGranted.get(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            allStoragePermissionsGranted = false;
                        }
                    }

                    // Kiểm tra quyền POST_NOTIFICATIONS (cho API >= 33)
                    // Đối với Android 10+, quyền WRITE_EXTERNAL_STORAGE không còn cần thiết cho thư mục ứng dụng
                    // Đối với POST_NOTIFICATIONS (Android 13+)

                    boolean notificationPermissionGranted = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (permissionsGranted.containsKey(Manifest.permission.POST_NOTIFICATIONS) && !permissionsGranted.get(Manifest.permission.POST_NOTIFICATIONS)) {
                            allStoragePermissionsGranted = false;
                        }
                        if (Boolean.FALSE.equals(permissionsGranted.get(Manifest.permission.POST_NOTIFICATIONS))) {
                            notificationPermissionGranted = false;
                        }

                    }

                    if (allStoragePermissionsGranted && notificationPermissionGranted) {
                        Toast.makeText(this, "Tất cả quyền cần thiết đã được cấp!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "All permissions granted. Proceeding with export.");

                        // <--- ĐIỂM QUAN TRỌNG: Kích hoạt quá trình xuất Excel tại đây! --->
                        // Bạn cần một cách để biết isXml là true hay false từ trước đó
                        // Có thể lưu một biến tạm thời hoặc truyền nó vào launcher (hơi phức tạp hơn)
                        // Hoặc đơn giản là gọi lại một phương thức `proceedWithExport()`
                        // Nếu bạn có một biến boolean `isExportingXml` ở cấp độ lớp:
                        proceedWithExport(isExportingXml); // Gọi lại logic xuất với trạng thái isXml đã lưu
                    } else {
                        Toast.makeText(this, "Một hoặc nhiều quyền bị từ chối. Không thể thực hiện xuất Excel.", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Permissions denied. Cannot proceed with export.");
                    }

                });

        manageAllFilesLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "Quyền quản lý tất cả tệp đã được cấp.", Toast.LENGTH_SHORT).show();
                            // Thực hiện hành động nếu quyền được cấp
                        } else {
                            Toast.makeText(this, "Quyền quản lý tất cả tệp bị từ chối.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        //////////
        //5B. XỬ LÝ KHI THAY ĐỔI CÁC TÙY CHỌN: 2KT, 3KT, CUSTOM (CRAWLTPYE), SẼ LÀM THAY ĐỔI GIAO DIỆN UI
        // Đang dùng cơ chế onResume () thay thế cho registerForActivityResult, nên bỏ registerForActivityResult
        // Khởi tạo ActivityResultLauncher Đăng ký ActivityResultLauncher cho SettingsActivity
        settingsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // CHỈNH SỬA: Chỉ cần gọi restoreSettingsAndUI() sau khi SettingsActivity đóng
                        // Cài đặt đã được lưu, cập nhật UI của MainActivity
                        updateCrawlTypeDisplay();
                        // Bạn có thể cần cập nhật các UI khác liên quan đến cài đặt ở đây nếu có
                        //restoreSettingsAndUI(); // TỰ THÊM XEM CÓ ĐÚNG KHÔNG
                        Toast.makeText(this, "Cài đặt đã được lưu và cập nhật!", Toast.LENGTH_SHORT).show();
                    }
                });

        // Gọi phương thức để cập nhật UI lần đầu khi Activity được tạo
        updateCrawlTypeDisplay();   // Dùng để set tvCurrentCrawlType

        // ... (thiết lập các Listener cho nút, Observer cho ViewModel nếu có)
        // Đã có trong setupUI(). Click Listeners
//        binding.btnCrawlData.setOnClickListener(v -> startCrawl()); // Renamed from btnEnableCrawler
//        binding.btnCancelCrawler.setOnClickListener(v -> stopCrawler());
//        binding.btnExportExcel.setOnClickListener(v -> checkAndExportExcel(false)); // False for XLSX
//        //binding.btnExportXml.setOnClickListener(v -> checkAndExportExcel(true)); // True for XML
//        binding.btnOpenFolder.setOnClickListener(v -> openExportFolder());
//
//        // Assuming btnDeleteTables and btnSqlDelete are also in activity_main.xml
//        binding.btnSqlDelete.setOnClickListener(v -> showDeleteTablesConfirmationDialog());
        //binding.btnDeleteDuplicateRecords.setOnClickListener(v -> showDeleteDuplicateRecordsConfirmationDialog());
        ////////// End Đã có trong setupUI()
        // Initial UI state (buttons enabled/disabled)
        // This will be called again in checkAndObserveWorkerState for proper initial state.
        updateUIState(false);   // Đã có trong checkAndObserveWorkerState
        //
    }   // END onCreate: CHỈ CHẠY 1 LẦN KHI KHỞI TẠO,

    private void setupUI() {
        // KHAI BÁO CÁC PHƯƠNG THỨC Listener CHO CÁC TEXTVIEW, EDITTEXT, BUTTON, SPINNER...
        // 1/ CÁC BUTTON: Click Listeners
        binding.btnCrawlData.setOnClickListener(v -> {
            // DÀNH CHO CÓ 2 CHỨC NĂNG
            if (isStartingOrStopping) { //isStartingOrStopping: chống double-click khi WorkManager chưa báo state mới.
                Log.d(TAG, "Đang chuyển trạng thái, bỏ qua click");
                return;
            }

            if (!isCrawling) {  // Dùng 1 nút btnCrawlData cho 2 chức năng : CRAWL+HỦY CRAWL
                // Bấm Start
                isCrawling = true;  // Đã có trong startCrawl
                isStartingOrStopping = true;
                binding.btnCrawlData.setText("Đang khởi động...");
                binding.btnCrawlData.setEnabled(false); // TẠM THỜI

                startCrawl();   // enqueue WorkManager

            } else {
                // Bấm Dừng
                isStartingOrStopping = true;
                binding.btnCrawlData.setText("Đang dừng...");
                binding.btnCrawlData.setEnabled(false); // TẠM THỜI

                stopCrawler();  // cancel WorkManager

            }
        }); // Renamed from btnEnableCrawler

        //binding.btnCancelCrawler.setOnClickListener(v -> stopCrawler());
        //binding.btnExportExcel.setOnClickListener(v -> checkAndExportExcel(true)); // true for XML,False for XLSX
        binding.btnExportExcel.setOnClickListener(v -> checkAndExportExcel(false)); // False for XLSX
        //binding.btnExportXml.setOnClickListener(v -> checkAndExportExcel(true)); // True for XML
        //binding.btnOpenFolder.setOnClickListener(v -> openExportFolder());    // gốc cũ
        binding.btnOpenFolder.setOnClickListener(v -> openFolderToChooseFile());    //HÌNH NHƯ HOẠT ĐỘNG KHÔNG ĐÚNG
        binding.btnOpenAndShareFile.setOnClickListener(v -> {
            int position = binding.spinnerSelectExcelFile.getSelectedItemPosition();
            if (excelFileList.isEmpty() || position < 0 || position >= excelFileList.size()) {
                Toast.makeText(this, "Bạn chưa chọn file!", Toast.LENGTH_SHORT).show();
                return;
            }

            File selectedFile = excelFileList.get(position);
            openOrShareFile(selectedFile);

        });

        // Assuming btnDeleteTables and btnSqlDelete are also in activity_main.xml
        binding.btnSqlDelete.setOnClickListener(v -> showDeleteConfirmationDialog());   // Như là reset lại Data
        //binding.btnSqlDelete.setOnClickListener(v -> showDeleteTablesConfirmationDialog());     //showDeleteRecordsConfirmationDialog());

        //2. SETUP SPINNER for Excel Files (Your original spinner)
        //loadExcelFilesIntoSpinner();
        loadExcelFileListSpinner(); // lOAD FILE EXCEL VÀO SPINNER(NẾU CÓ)

        binding.spinnerSelectExcelFile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFileName = (String) parent.getItemAtPosition(position);

                if (selectedFileName.equals("Chưa có file Excel")) {
                    Toast.makeText(MainActivity.this, "Không có file để mở.", Toast.LENGTH_SHORT).show();
                } else {
                    // Bạn có thể lưu tên file này hoặc thực hiện hành động gì đó
                    Log.d(TAG, "Selected Excel file: " + selectedFileName);
                    Toast.makeText(MainActivity.this, "Selected Excel file: " + selectedFileName, Toast.LENGTH_SHORT).show();
                    //Xem
                    //Toast.makeText(parent.getContext(), "Spinner item:" + position + 1, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Thêm long click để xóa file
        binding.spinnerSelectExcelFile.setOnLongClickListener(v -> {
            int position = binding.spinnerSelectExcelFile.getSelectedItemPosition();
            if (position >= 0 && !excelFileList.isEmpty()) {
                File selectedFile = excelFileList.get(position);
                confirmDeleteFile(selectedFile);
            }
            return true;
        });

        binding.btnViewErrors.setOnClickListener(v -> {
            Intent intent = new Intent(this, ErrorListActivity.class);
            startActivity(intent);
        });



        //3. SETUP TextWatcher, Listeners for K0, K1 EditText
        binding.editNumFromK0.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { /* Not used */ }

            @Override
            public void afterTextChanged(Editable s) {
                // Save K0 immediately to SharedPreferences for persistence
                try {
                    // Nên kiểm tra k0 có hợp lệ
//                    int k0 = Integer.parseInt(s.toString());
//                    int k1 = Integer.parseInt(binding.tvNumTongUrls.getText().toString());
                    k0 = Integer.parseInt(s.toString());    // k0" Globle
                    //settingsRepository.saveK0(selectedCrawlType.getK0PrefKey(), k0);    // KHÔNG LƯU, KHI QUYẾT ĐINH STARCRAWL MỚI LƯU
                    binding.tvChenhLechK1K0.setText("Số Urls thực hiện: " + (k1 - k0));
                } catch (NumberFormatException e) {
                    // Handle empty or invalid number, perhaps show a temporary hint
                    // Do not show Toast repeatedly here.
                    //Toast.makeText(MainActivity.this, "Invalid K0", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.editNumToK1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { /* Not used */ }

            @Override
            public void afterTextChanged(Editable s) {
                // Save K1 immediately to SharedPreferences for persistence
                try {
                    // Nên kiểm tra k1 có hợp lệ
                    // Nếu k0, k1 khai báo local thì phải lấy lại giá trị của cả 2
//                    int k1 = Integer.parseInt(s.toString());
//                    int k0 = Integer.parseInt(binding.editNumFromK0.getText().toString());
                    k1 = Integer.parseInt(s.toString());    // k0" Globle
                    //settingsRepository.saveK1(selectedCrawlType.getK1PrefKey(), k1);    // KHÔNG LƯU, KHI QUYẾT ĐINH STARCRAWL MỚI LƯU
                    binding.tvChenhLechK1K0.setText("Số Urls thực hiện: " + (k1 - k0));
                } catch (NumberFormatException e) {
                    // Handle empty or invalid number
                }
            }
        });

        // Xem Lại: setOnItemSelectedListener CÓ CẦN KHÔNG?
        binding.spinnerSelectExcelFile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // THÊM KIỂM TRA NÀY
                if (excelFileList != null && !excelFileList.isEmpty()) {
                    // Đảm bảo position hợp lệ trong trường hợp danh sách bị thay đổi đột ngột
                    if (position >= 0 && position < excelFileList.size()) {
                        File selectedFile = excelFileList.get(position);
                        // Dùng để hiển thị thêm thông tin ngày cập nhật cuối cùng của file, nếu không thì chẳng có công dụng gì
                        //Toast.makeText(MainActivity.this, "Đã chọn file: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();

                        // Ví dụ mở hoặc share file luôn
                        //openOrShareFile(selectedFile);

                        // Bạn có thể muốn cập nhật một TextView hoặc UI nào đó tại đây
                        // Ví dụ: binding.txtSelectedExcelFile.setText("File đã chọn: " + selectedFile.getName());
                    } else {
                        // Trường hợp position không hợp lệ (nên không xảy ra nếu spinner được cập nhật đúng)
                        Log.w(TAG, "Selected position " + position + " is out of bounds for excelFileList (size: " + excelFileList.size() + ")");
                    }
                } else {
                    // Xử lý trường hợp excelFileList rỗng (ví dụ: hiển thị thông báo, đặt text mặc định)
                    Log.w(TAG, "excelFileList is empty when onItemSelected for spinnerSelectExcelFile is called.");
                    // Ví dụ: binding.txtSelectedExcelFile.setText("Chưa có file Excel nào.");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Không chọn gì cả
            }
        });

        /// /

        // Initial UI state (buttons enabled/disabled)
        // This will be called again in checkAndObserveWorkerState for proper initial state.
        //updateUIState(false);
    }


    //Ví dụ: đặt sau onCreate() là ngon:

    /**
     * Mở màn hình ExportProgressActivity và truyền phương thức export
     *
     * @param exportType "XML" hoặc "XLSX"
     */

    private void startExportProgressActivity(String exportType) {
        Intent intent = new Intent(MainActivity.this, ExportProgressActivity.class);
        intent.putExtra(AppConstants.EXPORT_TYPE, exportType);
        intent.putExtra(AppConstants.TABLE_THUOC, selectedCrawlType.getTableThuocName());
        //Đã có thay đôi tên file theo thời gian hiện tại trong ExportProgressActivity
        intent.putExtra(AppConstants.NAME_FILE_EXCEL, selectedCrawlType.getExcelFileName());
        startActivity(intent);
    }


    // VỊ TRÍ CÁC HÀM NGOÀI onCreate() (và nằm TRONG CLASS MAINACTIVITY)
    //*** Các giai đoạn trạng thái của mainActivity:phản ưng *** //


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //this.optionsMenu = menu; // Lưu tham chiếu đến menu
        settingsMenuItem = menu.findItem(R.id.action_settings); // Save reference
        // Lấy tham chiếu
        settingsMenuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this.getApplicationContext(), R.color.white)));
        // Cập nhật trạng thái ban đầu của menu item
        // Sau khi menu được tạo, cần kiểm tra trạng thái hiện tại của crawler
        if (viewModelCrawlerGemini != null && viewModelCrawlerGemini.getWorkInfoLiveData().getValue() != null) {
            boolean isRunning = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                isRunning = viewModelCrawlerGemini.getWorkInfoLiveData().getValue().getFirst().getState() == WorkInfo.State.RUNNING ||
                        viewModelCrawlerGemini.getWorkInfoLiveData().getValue().getFirst().getState() == WorkInfo.State.ENQUEUED;
            } else {
                isRunning = viewModelCrawlerGemini.getWorkInfoLiveData().getValue().get(0).getState() == WorkInfo.State.RUNNING ||
                        viewModelCrawlerGemini.getWorkInfoLiveData().getValue().get(0).getState() == WorkInfo.State.ENQUEUED;
            }
            toggleSettingsMenuItem(isRunning);
        }

        return true;
    } // END onCreateOptionsMenu()


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Ghi log để kiểm tra
        Log.d("MENU_CLICK", "Item selected: " + item.getTitle() + ", Enabled: " + item.isEnabled());
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            // Bỏ: ĐÃ LÀM menu Settings DISABLE GIẢ
//            if (!item.isEnabled()) {
//                // Nếu item bị disabled mà vẫn vào đây, thì đây là vấn đề
//                Log.w("MENU_CLICK", "action_settings was clicked but it IS DISABLED!");
//                return true; // Quan trọng: return true để báo rằng bạn đã xử lý sự kiện này
//                // và không thực hiện hành động nào cả.
//            }
            ///
            if (isCrawling) {
                Toast.makeText(this,
                        "Đang thu thập dữ liệu, không thể mở cài đặt!",
                        Toast.LENGTH_SHORT).show();

            } else {
                // ... code xử lý khi item được click và enabled ...
                openSettingsActivity();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }   // END onOptionsItemSelected()

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Nếu bạn đã lấy settingsMenuItem trong onCreateOptionsMenu
        if (settingsMenuItem != null) {
            if (isCrawling) {
                settingsMenuItem.setEnabled(true);  // Sẽ làm disable GIẢ
                // THAY ĐOẠN NÀY
//                if (isSettingsItemDisabled) {
//                    settingsMenuItem.setEnabled(false);
//                    // Đối với icon, bạn có thể cần phải tự thay đổi alpha để nó trông "mờ" đi
//                    if (settingsMenuItem.getIcon() != null) {
//                        settingsMenuItem.getIcon().setAlpha(130); // 130: LÀM MỜ.Giá trị từ 0 (trong suốt) đến 255 (rõ nét)
//                        // 130 là khoảng 50% mờ
//                    }
//                } else {
//                    settingsMenuItem.setEnabled(true);
//                    if (settingsMenuItem.getIcon() != null) {
//                        settingsMenuItem.getIcon().setAlpha(255); // Rõ nét hoàn toàn
//                    }
//                }
                // END THAY ĐOẠN NÀY
                // BẰNG ĐOẠN DISABLE GIẢ: CHO PHÉP PHẢN HỒI KHI NHẤN
                settingsMenuItem.setOnMenuItemClickListener(item -> {
                    Toast.makeText(this,
                            "Đang thu thập dữ liệu, không thể mở cài đặt!",
                            Toast.LENGTH_SHORT).show();
                    return true;
                });


            } else {    // WORKER CHƯA CHẠY
                settingsMenuItem.setEnabled(true);
                if (settingsMenuItem.getIcon() != null) {
                    settingsMenuItem.getIcon().setAlpha(255);
                }
                settingsMenuItem.setOnMenuItemClickListener(null);
            }
        }
        return super.onPrepareOptionsMenu(menu);    // HOẶC TRUE
    }

    // Hàm của bạn để disable/enable item
//    public void disableMySettingsItem(boolean disable) {
//        isSettingsItemDisabled = disable;
//        invalidateOptionsMenu(); // Yêu cầu hệ thống gọi lại onPrepareOptionsMenu
//    }

    // Thêm disable hay enable nút Settings
// Nếu bạn muốn thay đổi trạng thái của menu item từ một nơi khác trong Activity
// (ví dụ: sau một hành động của người dùng)
    public void disableSettingsMenu(boolean isTrue) {
        if (settingsMenuItem != null) {
            settingsMenuItem.setEnabled(!isTrue);         //(false);
            // Đối với icon, bạn có thể cần phải tự thay đổi alpha để nó trông "mờ" đi
            if (settingsMenuItem.getIcon() != null) {
                settingsMenuItem.getIcon().setAlpha(130); // Giá trị từ 0 (trong suốt) đến 255 (rõ nét)
                // 130 là khoảng 50% mờ
            }
        }
        // Sau khi thay đổi trạng thái của một item, bạn cần gọi invalidateOptionsMenu()
        // để hệ thống gọi lại onPrepareOptionsMenu() và cập nhật giao diện menu.
        invalidateOptionsMenu();
    }

    public void enableSettingsMenu() {
        if (settingsMenuItem != null) {
            settingsMenuItem.setEnabled(true);
            // Đối với icon, bạn có thể cần phải tự thay đổi alpha để nó trông "mờ" đi
            if (settingsMenuItem.getIcon() != null) {
                settingsMenuItem.getIcon().setAlpha(255); // Rõ nét hoàn toàn
                // 130 là khoảng 50% mờ
            }
        }
        invalidateOptionsMenu();
    }

    private void initViews() {
        binding.btnCrawlData.setOnClickListener((View.OnClickListener) this);
        binding.btnExportExcel.setOnClickListener((View.OnClickListener) this);
        binding.btnOpenAndShareFile.setOnClickListener((View.OnClickListener) this);
        //binding.btnShareFile.setOnClickListener((View.OnClickListener) this);
        binding.btnCancelCrawler.setOnClickListener((View.OnClickListener) this);
        binding.btnEnableCrawler.setOnClickListener((View.OnClickListener) this);
        binding.btnSqlDelete.setOnClickListener((View.OnClickListener) this);
        binding.btnOpenFolder.setOnClickListener((View.OnClickListener) this);
        //spinnerSelectExcelFile : KHÔNG onClick() vì hành vi nó khác
        binding.spinnerSelectExcelFile .setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);   //Lỗi

    }   // END initViews()

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences(MyWorkerCrawler.PREFS_SETTINGS, MODE_PRIVATE);
        boolean isFirst = prefs.getBoolean("FIRST_RUN", true);
        if (isFirst) {
            prefs.edit().putBoolean("FIRST_RUN", false).apply();
        }
        return isFirst;
    } // END isFirstRun

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Ứng dụng cần quyền tự khởi động và thông báo để hoạt động.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    // Hiển thị nút bấm
                    // Gọi openAutoStartSettings và openNotificationSettings sau khi người dùng đồng ý
                    openAutoStartSettings();
                    //openNotificationSettings();

                })
                .setNegativeButton("Từ chối", null)
                .show();
    } //END showPermissionDialog

    private void openAutoStartSettings() {
        //Mở này tùy theo nhà sản suất điện thoại
        try {
            //Nếu bạn muốn mở trực tiếp trang cài đặt thông tin ứng dụng của chính ứng dụng đang chạy,
            // bạn có thể dùng đoạn code sau:
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi nếu không mở được cài đặt
            //NẾU CÓ LỖI THÌ MỞ TRANG SETTINGS DƯỢC KHÔNG? THỬ XEM
            // THÊM VÀO XỦ LÝ LỖI CHỈ Ở ĐẾN TRANG SETTINGS
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
                // Hiển thị hướng dẫn cho người dùng
                // Ví dụ: Toast.makeText(this, "Vui lòng tìm đến mục 'Ứng dụng'...", Toast.LENGTH_SHORT).show();
            } catch (Exception e2) {
                e2.printStackTrace();
                // Xử lý lỗi
            }
            //END THÊM VÀO
        }
    }  // END openAutoStartSettings

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi nếu không mở được cài đặt
        }
    } // END openNotificationSettings

    private void checkPermissionsAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            startExportProcess();
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startExportProcess();
//            } else {
//                Toast.makeText(this, "Quyền ghi bộ nhớ bị từ chối. Không thể xuất Excel.", Toast.LENGTH_SHORT).show();
//                // Optionally, guide the user to app settings
//                new AlertDialog.Builder(this)
//                        .setTitle("Quyền bị từ chối")
//                        .setMessage("Để xuất file Excel, bạn cần cấp quyền ghi bộ nhớ. Bạn có muốn đi tới cài đặt ứng dụng để cấp quyền không?")
//                        .setPositiveButton("Đi tới cài đặt", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package", getPackageName(), null);
//                                intent.setData(uri);
//                                startActivity(intent);
//                            }
//                        })
//                        .setNegativeButton("Hủy", null)
//                        .show();
//            }
//        }
//    }

    private void startExportProcess() {
        // Show progress dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.excel_export_progress_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false); // Prevent user from dismissing by tapping outside

        dialogExcelProgressBar = dialogView.findViewById(R.id.progressBar);
        dialogExcelProgressText = dialogView.findViewById(R.id.progressText);
        dialogExcelElapsedTimeText = dialogView.findViewById(R.id.elapsedTimeText);
        dialogExcelRemainingTimeText = dialogView.findViewById(R.id.remainingTimeText); // Initialize remaining time TextView

        dialogAlertExcel = dialogBuilder.create();
        dialogAlertExcel.show();

        // Start the export task on a background thread
        exportStartTime = System.currentTimeMillis();
    }


private void confirmDeleteFile(File file) {
    new AlertDialog.Builder(this)
            .setTitle("Xóa file")
            .setMessage("Bạn có chắc muốn xóa file:\n" + file.getName() + " ?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                if (file.delete()) {
                    Toast.makeText(this, "Đã xóa file: " + file.getName(), Toast.LENGTH_SHORT).show();
                    loadExcelFileListSpinner(); // Cập nhật lại spinner sau khi xóa
                } else {
                    Toast.makeText(this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
}

    private void openOrShareFile(File file) {
        String[] options = {"Mở file", "Chia sẻ file"};

        new AlertDialog.Builder(this)
                .setTitle("Chọn thao tác với " + file.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openFile(file);
                    } else if (which == 1) {
                        shareFile(file);
                    }
                })
                .show();
    }

    private void shareFile(File file) {
        // Lấy file ở đây cho dễ tìm
        int position = binding.spinnerSelectExcelFile.getSelectedItemPosition();
        if (excelFileList.isEmpty() || position < 0 || position >= excelFileList.size()) {
            Toast.makeText(this, "Chưa chọn file để chia sẻ!", Toast.LENGTH_SHORT).show();
            return;
        }
        File selectedFile = excelFileList.get(position);

        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            // Xác định mime-type
            String mimeType = getMimeType(file.getName());
            if (file.getName().endsWith(".xml")) mimeType = "text/xml";

            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);    // Chạy riêng, KHÔNG LIÊN QUAN ĐẾN APP CHÍNH MỞ NÓ

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ file"));

        } catch (Exception e) {
            Toast.makeText(this, "Không thể share file!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            String mimeType = getMimeType(file.getName());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> apps = getPackageManager().queryIntentActivities(intent, 0);
            if (!apps.isEmpty()) {
                startActivity(Intent.createChooser(intent, "Mở bằng..."));
            } else {
                showNoAppDialog(file, uri, mimeType);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở file!", Toast.LENGTH_SHORT).show();
        }

    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".xml")) {
            //return "text/xml";  // Tạm bỏ
            // Thử mởi trong excel
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; // Thử mởi trong excel
        } else if (fileName.endsWith(".xlsx")||fileName.endsWith(".xls")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return "*/*";
    }


    private void openExcelFile(String filePath) {   //Cũ của mình
        if (filePath == null) {
            Toast.makeText(this, "Không tìm thấy đường dẫn file.", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "File không tồn tại: " + filePath, Toast.LENGTH_LONG).show();
            return;
        }

        // Use FileProvider for opening files (required for Android N and above)
        // You need to configure a FileProvider in your AndroidManifest.xml and xml/file_paths.xml
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider", // Make sure this matches your manifest authority
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // MIME type for .xlsx
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary permission to the receiving app

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không có ứng dụng nào để mở file Excel.", Toast.LENGTH_LONG).show();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "FileProvider error: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi tạo URI file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi mở file Excel: " + e.getMessage());
            Toast.makeText(this, "Không thể mở file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to format milliseconds into HH:mm:ss
    private String formatDuration(long millis) {
        return DateUtils.formatElapsedTime(millis / 1000);
    }

    private void openExcelFile(File file) { //Mới
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Mở file Excel với..."));
    }

    private void showNoAppDialog(File file, Uri uri, String mimeType ) {

        new AlertDialog.Builder(this)
                .setTitle("Không có ứng dụng mở file")
                .setMessage("Thiết bị của bạn chưa có ứng dụng đọc file phù hợp.\nBạn có muốn?")   //Bạn muốn làm gì tiếp
                .setPositiveButton("Cài Google Sheets", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.docs.editors.sheets"));
                    startActivity(intent);
                })
                .setNeutralButton("Cài MS Excel", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.microsoft.office.excel"));
                    startActivity(intent);
                })
                .setNegativeButton("Chia sẻ qua Cloud", (dialog, which) -> {
                    shareFileToCloud(uri, mimeType);
                })
                .setCancelable(true)
                .show();
    }

    private void shareFileToCloud(Uri uri, String mimeType) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // 👈 Fix crash sau Gmail

            List<ResolveInfo> apps = getPackageManager().queryIntentActivities(shareIntent, 0);
            if (!apps.isEmpty()) {
                startActivity(Intent.createChooser(shareIntent, "Chia sẻ file qua..."));
            } else {
                Toast.makeText(this, "Không tìm thấy ứng dụng chia sẻ file!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Không thể chia sẻ file!", Toast.LENGTH_SHORT).show();
        }
    }



//    Nếu muốn chọn bằng RadioGroup trong Dialog đẹp → dùng Cách 1
//    Nếu thích nhẹ nhàng nhanh gọn → dùng Cách 2

    //Cách 1: Theo pp Nút chọn
    private void showExportMethodDialog() {
        // Cách mới
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Chọn kiểu xuất Excel")
//                .setItems(new CharSequence[]{"Excel XML 2003", "Excel .xlsx (POI SXSSF)"}, (dialog, which) -> {
//                    if (which == 0) {
//                        exportExcelXml();
//                    } else {
//                        exportExcelXlsx();
//                    }
//                })
//                .show();
        // end Cách mới
        //Cách cũ
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn phương thức xuất Excel");

        String[] options = {"Excel XML (2003)", "Excel XLSX (Apache POI)"};

        builder.setSingleChoiceItems(options, -1, (dialog, which) -> {
            String selectedType = (which == 0) ? "XML" : "XLSX";
            dialog.dismiss();
            startExportProgressActivity(selectedType);
        });

        builder.setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss());
        builder.show();
        // end Cách cũ
    }



    //Cách 2: AlertDialog Items dạng list đơn giản
    private void showExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn phương pháp export Excel")
                .setItems(new CharSequence[]{"Export XLSX (POI)", "Export Excel XML"}, (dialog, which) -> {
                    if (which == 0) {   // Chọn Excel
                        startExportProgressActivity("xml");
                    } else {
                        startExportProgressActivity("xlsx");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    // Phương thức cập nhật hiển thị loại crawl
    private void updateCrawlTypeDisplay() {
        CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();
        String displayText = selectedCrawlType.getDescription();
        if (selectedCrawlType == CrawlType.CUSTOM_STRING) {
            String customString = settingsRepository.getCustomCrawlString();
            displayText += " (" + (customString.isEmpty() ? "Chưa cài đặt" : customString) + ")";
        }
        binding.tvCurrentCrawlType.setText(displayText);
        //binding.tvChenhLechK1K0.setText(k1 - k0 + "");
        Log.d("MainActivity", "Crawl type display updated: " + displayText);
    }


    // Mới
    private void checkAndRequestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissions = new String[]{Manifest.permission.POST_NOTIFICATIONS};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10-12
            // Không cần WRITE_EXTERNAL_STORAGE cho thư mục ứng dụng
            permissions = new String[]{};
        } else { // Android 9 và thấp hơn
            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        if (permissions.length > 0) {
            boolean allGranted = true;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                requestPermissionLauncher.launch(permissions);
            }
        }
    }

    private void startCrawl() {
        startTime = System.currentTimeMillis();
//        if (!viewModelCrawlerGemini.isWorkRunning()) {  // NẾU KHÔNG ĐANG CHẠY: CHỈ XAY RA KHI NHẤN LẦN 2,3N=.. LIÊN TIẾP

            // 2. Lưu giá trị k0, k1 khi nút Start Crawl được nhấn
            // Trong MainActivity.java, trong listener của btnCrawler
//            int k0 = AppConstants.DEFAULT_K0_SETTING;
//            int k1 = AppConstants.DEFAULT_K1_SETTING; // Giá trị mặc định cho "đến cuối mảng"

            // XÊT CAC k0, k1: có howkp lệ
            String k0Text = binding.editNumFromK0.getText().toString().trim();
            String k1Text = binding.editNumToK1.getText().toString().trim();

            try {
                if (!k0Text.isEmpty()) { //ĐK: k0Text > 0 và <= arrayAZ.lenght
                    k0 = Integer.parseInt(k0Text);
                    if (k0 < 0) {
                        Toast.makeText(this, "K0 phải là số không âm.", Toast.LENGTH_SHORT).show();
                        return; // Ngừng nếu K0 không hợp lệ
                    }
                }

                if (!k1Text.isEmpty()) {    //ĐK: k1Text > k0Text và <= arrayAZ.lenght
                    k1 = Integer.parseInt(k1Text);
                    // K1 có thể nhỏ hơn K0 nếu người dùng muốn crawl 1 phần tử hoặc K0=K1
                    // MyWorkerCrawler sẽ xử lý trường hợp K1 < K0 bằng cách đặt endIndex = startIndex
                    // Nhưng ở đây bạn có thể đưa ra cảnh báo hoặc sửa tự động
                    if (k1 < k0 && !k1Text.isEmpty()) { // Nếu K1 được nhập và nhỏ hơn K0
                        Toast.makeText(this, "K1 không thể nhỏ hơn K0. Đã điều chỉnh K1 bằng K0.", Toast.LENGTH_LONG).show();
                        k1 = k0+1; // Tự động điều chỉnh K1
                        binding.editNumToK1.setText(String.valueOf(k1)); // Cập nhật lại UI
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "K0 và K1 phải là số nguyên hợp lệ.", Toast.LENGTH_LONG).show();
                return; // Ngừng nếu định dạng số không hợp lệ
            }

            // Kiểm tra trước
            // Lấy danh sách các URL đang chờ xử lý từ DB (STATUS = 0)
            List<UrlInfo> pendingUrlInfos = dbHelperThuoc
                    .getDetailedUrlInfoByStatus(selectedCrawlType.getUrlQueueTableName(), 0);

            if (!pendingUrlInfos.isEmpty()) {    // Phiên trước kết thúc NHƯNG CÓ THỂ BỊ LỖI: FAILED, 1 phần nên còn
                if (cancelledWorker) {
                    // Đổi tên nút: 1 nút 2 chức năng
                    isCrawling = true;
                    binding.btnCrawlData.setText("Dừng Crawl");
                    viewModelCrawlerGemini.startCrawler(true, ""); // customCrawlString="": Truyền tên enum và chuỗi tùy chỉnh
                    cancelledWorker = false;    // reset
                    settingsRepository.saveCancelledWorker(selectedCrawlType.getCancelledWorkerPrefKey(),false); // reset
                } else {

                    // Nếu tiếp tục thì lấy lại các tông số của phiên trước: k0, k1, crawlType...
                    // Load K0, K1 from SettingsRepository: có liên quan đến loại crawlType
                    k0 = settingsRepository.getK0(selectedCrawlType.getK0PrefKey());
                    k1 = settingsRepository.getK1(selectedCrawlType.getK1PrefKey(), "");
                    if (k0 != Integer.parseInt(k0Text) || k1 != Integer.parseInt(k1Text)) {
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("Chọn cào dữ liệu")
                                    .setMessage("Phiên cào dữ liệu trước chưa hoàn thành! Bạn có muốn tiếp tục, hay cào phiên mới?")
                                    .setPositiveButton("Tiếp tục", (dialog, which) -> {
                                        SharedPreferencesUtils.setHasCrawlStarted(getApplicationContext(), true);  // set true
                                        Toast.makeText(this, "Bắt đầu crawl...", Toast.LENGTH_SHORT).show();

                                        //binding.btnCrawlData.setText(R.string.stop_crawl);    // NẾU THIẾT KẾ 1 NÚT CÓ 2 CHỨC NĂNG: CRAWL & HỦY

                                        // Nếu tiếp tục thì lấy lại các tông số của phiên trước: k0, k1, crawlType...
                                        // Load K0, K1 from SettingsRepository: có liên quan đến loại crawlType
                                        k0 = settingsRepository.getK0(selectedCrawlType.getK0PrefKey());
                                        k1 = settingsRepository.getK1(selectedCrawlType.getK1PrefKey(), "");

                                        long currentProcessedCount = settingsRepository.getSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey());
                                        long ProcessedUrlStar1Count = settingsRepository.getCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey());
                                        long totalRecords = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());    // Lấy cách này: THỐNG NHẤT ĐỒNG BỘ, TRÁNH SAI
                                        totalRecords = k1 - k0;
                                        int percent = (int) (ProcessedUrlStar1Count * 100.0 / totalRecords);

                                        // Hoặc
                                        //long totalRecords = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
                                        // Hoaặc
                                        //totalRecords = selectedCrawlType.getKyTuArray(settingsRepository).length;
                                        //long totalRecords = progress.getLong(AppConstants.WORK_TOTAL_RECORDS, 0);

                                        //String urlCurrent = progress.getString(AppConstants.URL_CURRENT);
                                        // Thời gian có thể không cần truyền, MÀ LẤY TRỰC TIẾP TRÊN MAINACTIVITY
                                        //long elapsedTime = System.currentTimeMillis() - startTime;

                                        // Hiện tại: elapsedTime được cộng dồn từ phiên bản trước, KHÔNG PHẢI LÀ THỜI GIAN HIỆN TẠI!
                                        long elapsedTime = settingsRepository.getLongElapsedTime(selectedCrawlType);   // Lấy từ progress
                                        //long remainingTime = settingsRepository.getLong(AppConstants.WORK_REMAINING_TIME, 0); // Lấy từ progress


                                        // Thread monitoring data
//                            int threadsWaiting = progress.getInt(AppConstants.THREADS_WAITING, 0);
//                            int queuedTasks = progress.getInt(AppConstants.QUEUED_TASKS, 0);
//                            int activeThreads = progress.getInt(AppConstants.ACTIVE_THREADS, 0);
//                            int parallelism = progress.getInt(AppConstants.PARALLELISM, 0); //Tổng luoogng tối đa
//                            int threadsCompleted = progress.getInt(AppConstants.THREADS_COMPLETED, 0);  // KHÔNG CHÍNH XÁC
//                            int completedTasks = progress.getInt("completedTasks", 0);
//                            String statusMessage = progress.getString(AppConstants.WORK_STATUS_MESSAGE);

                                        /** // Cập nhật UI elements CHUNG CHO TRẠNG THÁI ĐANG CHẠY
                                         * Cập nhật ProgressBar và TextView trên Main Thread (LiveData tự động)
                                         * Đảm bảo yourProgressBar và yourTextView đã được khởi tạo và tham chiếu đúng
                                         * Update UI elements
                                         */
                                        binding.editNumFromK0.setText(k0 + "");
                                        binding.editNumToK1.setText(k1 + "");
                                        binding.tvChenhLechK1K0.setText(String.format("%d", k1 - k0));

                                        binding.progressPercentText.setText(percent + "%");
                                        binding.pbCrawlerProgress.setProgress(percent);
                                        //binding.tvCrawlerStatus.setText(statusMessage != null ? statusMessage : "Đang xử lý...");
                                        binding.tvSumProcessedUrls.setText(String.format("Tổng đã xử lý: %d", currentProcessedCount));
                                        //Utils.debugSetText(binding.tvSumProcessedUrls, String.format("Tổng đã xử lý: %d", currentProcessedCount)); // THEO DÕI
                                        binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", ProcessedUrlStar1Count, totalRecords));
                                        //binding.tvInfoUrl.setText(urlCurrent);
                                        binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(elapsedTime / 1000)));
//                            binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", formatTime(remainingTime)));
                                        // Update thread monitoring
//                            binding.tvThreadsWaiting.setText(String.format("Chờ: %d", threadsWaiting));
//                            binding.tvNumThreadsActiveThreads.setText(String.format("Đang chạy: %d", activeThreads));
                                        //binding.tvNumThreadParallelism.setText(String.format("Số luồng tối đa: %d", threadsCompleted));
//                            binding.tvNumThreadParallelism.setText(String.format("Số tác vụ hoàn thành: %d", completedTasks)); //Số tác vụ hoàn thành

                                        // Đổi tên nút: 1 nút 2 chức năng
                                        isCrawling = true;
                                        binding.btnCrawlData.setText("Dừng Crawl");
                                        //Thanm số: customCrawlString: CÓ THỂ KHÔNG CẦN, VÌ TRONG MyCrawlerWorker sẽ tính
                                        viewModelCrawlerGemini.startCrawler(true, ""); // customCrawlString="": Truyền tên enum và chuỗi tùy chỉnh

                                        // PHẢI LOAD LẠI CÁC UI PROGRESS
                                        //updateProgressUI();
                                    })
                                    .setNeutralButton("Phiên cào mới", (dialog, which) -> {
                                        /** Sau khi kiểm tra và lấy được k0, k1 hợp lệ: THÌ TIẾN HÀNH LƯU K0, K1
                                         * CÓ THỂ SO SÁNH ko, k1 đã lưu trong prefs trước và k0, k1 mới gõ (nếu có)
                                         * NẾU k0 cũ # k0 MỚI GÕ hoặc k1 cũ # k1 MỚI GÕ THÌ MÓI TÍNH TOÁN LẠI CHO NHANH
                                         */

                                        //settingsRepository.saveK0(selectedCrawlType.getK0PrefKey(), Integer.parseInt(k0Text));    // QUYẾT ĐỊNH LƯU Ở ĐÂY
                                        //settingsRepository.saveK1(selectedCrawlType.getK1PrefKey(), Integer.parseInt(k1Text));    // QUYẾT ĐỊNH LƯU Ở ĐÂY
                                        // xem giá trị KyTuArray TRƯỚC set
                                        Log.d(TAG, "TRƯỚC set, giá trị KyTuArray=" + Arrays.asList(selectedCrawlType.getKyTuArrayK0K1(settingsRepository)));
                                        //selectedCrawlType.setKyTuArray(selectedCrawlType, k0, k1);
                                        // xem giá trị KyTuArray SAU set
                                        Log.d(TAG, "SAU set, giá trị KyTuArray=" + Arrays.asList(selectedCrawlType.getKyTuArrayK0K1(settingsRepository)));

                                        // Truyền loại crawl được chọn vào worker
                                        //selectedCrawlType = settingsRepository.getSelectedCrawlType();
                                        String customCrawlString;
                                        if (selectedCrawlType == CrawlType.CUSTOM_STRING) {
                                            customCrawlString = settingsRepository.getCustomCrawlString();
                                            if (customCrawlString.isEmpty()) {
                                                Toast.makeText(this, "Vui lòng nhập chuỗi ký tự tùy chỉnh trong cài đặt.", Toast.LENGTH_LONG).show();
                                                binding.btnCrawlData.setText(R.string.crawl_now);
                                                binding.btnCancelCrawler.setEnabled(false);
                                                binding.pbCrawlerProgress.setVisibility(View.GONE);
                                                return;
                                            }
                                        } else {    //Chưa viết
                                            customCrawlString = "";
                                        }
                                        startCrawlTuDau();
                                        // Đổi tên nút: 1 nút 2 chức năng
                                        isCrawling = true;
                                        //binding.btnCrawlData.setText("Dừng Crawl");
                                        binding.btnCrawlData.setText("Đang Crawl");
//                            // RESET: // Code sau chính là startCrawlTuDau()
//                            // Đặt lại cờ HAS_CRAWL_STARTED về false khi bắt đầu một phiên mới hoàn toàn
//                            SharedPreferencesUtils.setHasCrawlStarted(getApplicationContext(), false);  // set false
//
//                            //Thanm số: customCrawlString: CÓ THỂ KHÔNG CẦN, VÌ TRONG MyCrawlerWorker sẽ tính
//                            viewModelCrawlerGemini.startCrawler(selectedCrawlType, false, customCrawlString); // Truyền tên enum và chuỗi tùy chỉnh
                                    })

                                    .setNegativeButton("Hủy", (dialog, which) -> {
                                        updateUIState(false); // Enable all buttons when finished
                                        dialog.dismiss();
                                        //return;
                                    })
                                    .show();
                            // PHẢI ĐẶT SAU CÙNG VÌ CÓ DISABLE CÁC NÚT, NÊN KHI GÁN LẠI GIÁ TRỊ SẼ LỖI
                            updateUIState(true); // Disable buttons when running
                        });
                    } else { //k0, k1 không thay đổi thì chạy tiếp tục luôn
                        isCrawling = true;
                        binding.btnCrawlData.setText("Dừng Crawl");
                        //Thanm số: customCrawlString: CÓ THỂ KHÔNG CẦN, VÌ TRONG MyCrawlerWorker sẽ tính
                        viewModelCrawlerGemini.startCrawler(true, ""); // customCrawlString="": Truyền tên enum và chuỗi tùy chỉnh
                    }
                }

            } else {    // CHẠY MỚI HOÀN TOÀN
                isCrawling = true;
//                binding.btnCrawlData.setText("Dừng Crawl");
                binding.btnCrawlData.setText("Đang Crawl");
                startCrawlTuDau();
                // Code sau chính là startCrawlTuDau()
//                // Đặt lại cờ HAS_CRAWL_STARTED về false khi bắt đầu một phiên mới hoàn toàn
//                SharedPreferencesUtils.setHasCrawlStarted(getApplicationContext(), false);  // set false
//                // reset Thanh diễn tiến = 0
//                binding.progressPercentText.setText(0 + "%");
//                binding.pbCrawlerProgress.setProgress(0);
//                binding.tvCrawlerStatus.setText("Bắt đầu crawl...");
//                binding.tvSumProcessedUrls.setText(String.format("Số URL đã xử lý: %d", 0));
//                binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", 0, 0));
//                binding.tvInfoUrl.setText("");
//                binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(0)));
//                binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", DateUtils.formatElapsedTime(0)));
//                // RESET CÁC THÔNG SỐ TRONG PREFS
//                settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(),0L);
//                settingsRepository.saveCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey(),0L);     //crawledUrlStar1Count
//                // Reset lại time đã lưu
//                settingsRepository.saveLongElapsedTime(selectedCrawlType, 0L);
//
//                //Thanm số: customCrawlString: CÓ THỂ KHÔNG CẦN, VÌ TRONG MyCrawlerWorker sẽ tính
//                viewModelCrawlerGemini.startCrawler(selectedCrawlType, false, customCrawlString); // Truyền tên enum và chuỗi tùy chỉnh

            }

//        } else {    // NẾU ĐANG CHẠY:  CHỈ XAY RA KHI NHẤN LẦN 2,3N=.. LIÊN TIẾP NHƯNG NÚT NÀY ĐÃ BỊ DISABLE
//            // KHÔNG XẢY RA VÌ ĐÃ BỊ DISABLE
//            Toast.makeText(this, "Crawler đang chạy. Dừng lại...", Toast.LENGTH_SHORT).show();
//            stopCrawler();
//        }
    }

    private void startCrawlTuDau() {
        // Đặt lại cờ HAS_CRAWL_STARTED về false khi bắt đầu một phiên mới hoàn toàn
        SharedPreferencesUtils.setHasCrawlStarted(getApplicationContext(), false);  // set false
        // reset Thanh diễn tiến = 0
        binding.progressPercentText.setText(0 + "%");
        binding.pbCrawlerProgress.setProgress(0);
        binding.tvCrawlerStatus.setText("Bắt đầu crawl...");
        //binding.tvSumProcessedUrls.setText(String.format("Số URL đã xử lý: %d", 0));
        Utils.debugSetText(binding.tvSumProcessedUrls, String.format("Tổng đã xử lý: %d", 0)); // THEO DÕI
        binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", 0, k1 - k0));
        binding.tvInfoUrl.setText("");
        binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(0)));
        binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", DateUtils.formatElapsedTime(0)));
        // RESET CÁC THÔNG SỐ TRONG PREFS
        handleWorkerSuccess(selectedCrawlType); // Reset
        // Lưu k0, k1, và tổng Urls theo k1, k0
        settingsRepository.saveK0(selectedCrawlType.getK0PrefKey(),k0);
        settingsRepository.saveK1(selectedCrawlType.getK1PrefKey(),k1);
        settingsRepository.saveTotalUrls(selectedCrawlType.getTotalUrlsPrefKey(),k1-k0);
            // handleWorkerSuccess thay cho đoạn sau
//        settingsRepository.saveSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey(),0L);
//        settingsRepository.saveCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey(),0L);     //crawledUrlStar1Count
//        // Reset lại time đã lưu
//        settingsRepository.saveLongElapsedTime(selectedCrawlType, 0L);
                // End đoạn sau

        //Thanm số: customCrawlString: CÓ THỂ KHÔNG CẦN, VÌ TRONG MyCrawlerWorker sẽ tính.
        String customCrawlString ="";   //Tạm gán để chạy
        viewModelCrawlerGemini.startCrawler(false, customCrawlString); // Truyền tên enum và chuỗi tùy chỉnh
    }

    private void updateUIForWorkInfo(WorkInfo workInfo) {
        WorkInfo.State state = workInfo.getState();
        Data progress = workInfo.getProgress();
        int percent = progress.getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
        String statusMessage = progress.getString(AppConstants.WORK_STATUS_MESSAGE);

        binding.pbCrawlerProgress.setVisibility(View.VISIBLE);
        binding.btnCancelCrawler.setEnabled(true);

        if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
            binding.btnCrawlData.setText(R.string.stop_crawl);
            binding.pbCrawlerProgress.setProgress(percent);
            //binding.tvCrawlStatus.setText(statusMessage != null ? statusMessage : getString(R.string.crawl_status_crawling));
        } else if (state == WorkInfo.State.SUCCEEDED) {
            binding.btnCrawlData.setText(R.string.crawl_now);
            binding.btnCancelCrawler.setEnabled(false);
            binding.pbCrawlerProgress.setVisibility(View.GONE);
            //binding.tvCrawlStatus.setText(R.string.crawl_status_finished);
        } else if (state == WorkInfo.State.FAILED) {
            binding.btnCrawlData.setText(R.string.crawl_now);
            binding.btnCancelCrawler.setEnabled(false);
            binding.pbCrawlerProgress.setVisibility(View.GONE);
            //binding.tvCrawlStatus.setText(getString(R.string.crawl_status_failed, statusMessage));
        } else if (state == WorkInfo.State.CANCELLED) {
            binding.btnCrawlData.setText(R.string.crawl_now);
            binding.btnCancelCrawler.setEnabled(false);
            binding.pbCrawlerProgress.setVisibility(View.GONE);
            binding.tvCrawlerStatus.setText(R.string.crawl_status_paused);
        } else {
            binding.btnCrawlData.setText(R.string.crawl_now);
            binding.btnCrawlData.setEnabled(false);
            binding.pbCrawlerProgress.setVisibility(View.GONE);
            binding.tvCrawlerStatus.setText("Sẵn sàng.");
        }
    }

    private void updateRecordCount() {
        // Lấy CrawlType hiện tại để đếm bản ghi của bảng tương ứng
        CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();
        String tableName = selectedCrawlType.getTableThuocName();   //Table: 2Kt, 3kt, Custom
        //long count = dbHelperThuoc.getCountRecord(tableName);
        long count = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
        binding.tvTotalRecords.setText(getString(R.string.tvTotalRecords) + count);
    }

//    private void showExportDialog() {
//        String[] options = {getString(R.string.export_excel_xml), getString(R.string.export_xlsx_poi)};
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(R.string.export_select_format);
//        builder.setItems(options, (dialog, which) -> {
//            boolean isXmlExport = (which == 0); // 0 là XML, 1 là XLSX
//            startExportProgressActivity(isXmlExport);
//        });
//        builder.show();
//    }
    // Phương thức để mở SettingsActivity
    private void openSettingsActivity() {   // Dành cho SettingsActivity
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        settingsActivityResultLauncher.launch(intent);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                //.setTitle(R.string.delete_confirm_title)
                .setTitle("Xác nhận delete data")
                //.setMessage(R.string.delete_confirm_message)
                .setMessage("Bạn có muốn xóa hết dữ liệu vĩnh viễn?.")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    resetDatabase(selectedCrawlType);
                    clearPreferences();
                    runOnUiThread(() -> {
                        binding.editNumToK1.setText(String.valueOf(settingsRepository.getK1(selectedCrawlType.getK1PrefKey(),"")));
                        binding.tvSumProcessedUrls.setText("Số Crawled Urls: 0");
                        binding.tvSoUrlTrenTotalUrl.setText("0/0");
                        binding.progressPercentText.setText("0%");
                        binding.pbCrawlerProgress.setProgress(0);
                        binding.tvElapsedTimes.setText("00:00");
                        binding.tvTimeRemaining.setText("00:00");
                        binding.tvSoUrlErrors.setText("0");
                        binding.tvSoUrlErrors.setVisibility(View.GONE);
                        binding.btnViewErrors.setVisibility(View.GONE);


                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void resetDatabase(CrawlType crawlType) {
        DBHelperThuoc dbHelper = DBHelperThuoc.getInstance(this);
        //dbHelper.getDbPath();
        //FileUtils.deleteFileOrDirectory(new File(dbHelper.getDbPath())); //Delete file ThuocBietDuoc.db
        //SQLiteDatabase db = dbHelper.getWritableDatabase();
        SQLiteDatabase db = dbHelper.getDb();

        // Xóa sạch toàn bộ dữ liệu các bảng
        db.execSQL("DELETE FROM " + crawlType.getTableThuocName()); // Hoặc chỉ giữ lại url start=1
        db.execSQL("DELETE FROM " + crawlType.getUrlQueueTableName());
        // Nếu có thêm bảng nào nữa thì add ở đây

        //db.close();   // Không đóng: VÌ LÀ Singleton
    }

    private void clearPreferences() {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Nếu dùng SharedPreferences với tên riêng
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_SETTINGS, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
// end mới
    // Phương thức để bật/tắt MenuItem cài đặt
    private void toggleSettingsMenuItem(boolean enable) {
        if (optionsMenu != null) {
            // Lấy tham chiếu
            MenuItem settingsItem = optionsMenu.findItem(R.id.action_settings);
            //thêm đổi màu icon thành màu trắng
            settingsItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this.getApplicationContext(), R.color.white)));

            if (settingsItem != null) {
                settingsItem.setEnabled(!enable);
                // Thay đổi màu sắc icon để trực quan hơn
                if (enable) {
                    settingsItem.getIcon().setAlpha(130); // Giảm độ trong suốt khi disabled
                } else {
                    settingsItem.getIcon().setAlpha(255); // Đầy đủ độ trong suốt khi enabled
                }
            }
        }
    }


    private void loadExcelFileListSpinner() {
//        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        //File dir = Utils.getExternalFilesDirThuocBietDuoc(MainActivity.this.getApplicationContext(), false);
        File dir = new File(getExternalFilesDir(null), "Documents");
        if (!dir.exists()) dir.mkdir();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml") || name.endsWith(".xlsx")
                || name.endsWith(".xls"));
        List<String> fileNames = new ArrayList<>();
        excelFileList.clear();
        if (files != null && files.length > 0) {
            // Sắp xếp giảm dần theo ngày sửa
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            for (File file : files) {
                //excelFileList.addAll(Arrays.asList(files));
                excelFileList.add((file));
                fileNames.add(file.getName());
            }
        } else {
            Toast.makeText(this, "Chưa có file Excel nào!", Toast.LENGTH_SHORT).show();
        }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fileNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerSelectExcelFile.setAdapter(adapter);
    }

    // Mói: có thể trùng lặp
    private void loadExcelFilesIntoSpinner() {
        //File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportDir = Utils.getExternalFilesDirThuocBietDuoc(MainActivity.this.getApplicationContext(), false);
        File[] excelFiles = exportDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xls") || name.endsWith(".xlsx");
            }
        });

        String[] fileNames;
        if (excelFiles != null && excelFiles.length > 0) {
            // Sort files by last modified date, descending
            Arrays.sort(excelFiles, Comparator.comparingLong(File::lastModified).reversed());
            fileNames = new String[excelFiles.length];
            for (int i = 0; i < excelFiles.length; i++) {
                fileNames[i] = excelFiles[i].getName();
            }
        } else {
            fileNames = new String[]{"Chưa có file Excel"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSelectExcelFile.setAdapter(adapter);
    }

    private void loadSettings() {
        if (selectedCrawlType == null) {
            selectedCrawlType = settingsRepository.getSelectedCrawlType();  // Đã có khi onCreate() trước đó
        }
        // Load và cập nhật UI theo loại CrawlType
        //loadAndDisplayCrawlType();
        binding.tvCurrentCrawlType.setText(selectedCrawlType.getDescription());

        // Load K0, K1 from SettingsRepository: có liên quan đến loại crawlType
        k0 = settingsRepository.getK0(selectedCrawlType.getK0PrefKey());
        k1 = settingsRepository.getK1(selectedCrawlType.getK1PrefKey(), "");    //Mặc dịnh là MAX array
        //k0 = selectedCrawlType.getTotalKyTuCount(settingsRepository);
        //k1 = selectedCrawlType.getTotalKyTuCount(settingsRepository);
        binding.editNumFromK0.setText(String.valueOf(k0));
        // Only set K1 if it's not the default "end of array" value (e.g., -1 or MAX_INT)
        //if (k1 != AppConstants.DEFAULT_K1_SETTING) {
            binding.editNumToK1.setText(String.valueOf(k1));
        //} else {
        //    binding.editNumToK1.setText(""); // Keep it empty for "end of array"
        //}

        if (selectedCrawlType.getSettingIdName().equals(AppConstants.RADIOURL2KT)||
                selectedCrawlType.getSettingIdName().equals(AppConstants.RADIOURL3KT)) {
            binding.tvNumTongUrls.setText(GetKyTuAZ.getArrayAZ(selectedCrawlType).length + "");
        } else {
            //Custom
        }

        cancelledWorker = settingsRepository.getCancelledWorker(selectedCrawlType.getCancelledWorkerPrefKey());
        // Kiểm tra DB xem còn dữ liệu chưa xử lý không
        List<UrlInfo> urlsToProcessQueue =
                dbHelperThuoc.getDetailedUrlInfoByStatus(
                        selectedCrawlType.getUrlQueueTableName(), 0
                );
        // hoặc
//        Cursor c1 = dbHelperThuoc.getDb().query(selectedCrawlType.getUrlQueueTableName(), new String[]{DBHelperThuoc.PARENT_ID},
//                DBHelperThuoc.STATUS + " = ?", new String[]{String.valueOf(0)},
//                null, null, null);

        // hoặc
        Cursor c = dbHelperThuoc.getDb().rawQuery("SELECT COUNT(*) FROM " + selectedCrawlType.getUrlQueueTableName() +
                " WHERE " + DBHelperThuoc.STATUS +"=?", new String[]{String.valueOf(0)});

        //c.moveToFirst();    //Phải có
        //if (urlsToProcessQueue != null && !urlsToProcessQueue.isEmpty()) {
        if (c.moveToFirst() && c.getInt(0) > 0 && cancelledWorker) {
            binding.btnCrawlData.setText("Tiếp tục Crawl");
            settingsRepository.saveCancelledWorker(selectedCrawlType.getCancelledWorkerPrefKey(), false);
        } else {
            //binding.btnCrawlData.setText("Start Crawl");
        }

    }


    private void observeViewModel() {
        // Vì dùng enqueueUniqueWork, nên KHÔNG CẦN QUAN TÂM workID khi khởi động app và worker tự chạy lại VẪN QUAN SÁT ĐƯỢC
        viewModelCrawlerGemini.getWorkInfoLiveData().observe(this, workInfos -> {
            if (workInfos != null && !workInfos.isEmpty()) {
                WorkInfo workInfo = workInfos.get(0);
                Data progress = workInfo.getProgress();
                // kIỂM TRA progress.getKeyValueMap():RỖNG THÌ KHÔNG LÀM:
                // NÊN XEM LẠI, DÙ progress.getKeyValueMap() LÀ RỖNG, NGHIÃ LÀ CHƯA CÓ setProgressAsync,
                // NHƯNG VẪN CHỨNG TỎ WORKER ĐANG HOẠT ĐỘNG: SỬ DỤNG ĐỂ SET CÁC NÚT: ENABLE HAY DISABLE
//                if(progress.getKeyValueMap().isEmpty()) {
//                    return;
//                }

                WorkInfo.State state = workInfo.getState();
                Log.d(TAG, "STATE CHANGE: " + state);

                String status = workInfo.getProgress().getString("status");

                // Log khi state thay đổi
                if (lastState == null || !lastState.equals(state)) {
                    Log.d("STATE_TRACKER", "WorkInfo state changed: " + state.name());
                    // Log toàn bộ thông tin WorkInfo
                    Log.d(TAG, "🛠 WorkInfo: " + workInfo + " | thread=" + Thread.currentThread().getName());
                    lastState = state;
                }

                // --- Xử lý UI ---
                // Log vòng đời đầy đủ
                Log.d(TAG, "STATE CHANGE: " + state
                        + " | finished=" + state.isFinished()
                        + " | progressStatus=" + status);

                switch (state) {
                    case ENQUEUED:
                        if ("RETRY_NETWORK".equals(status)) {
                            binding.tvCrawlerStatus.setText("Mạng bị mất. Đang thử lại...");
                            // Tương thích tất cả API>=1
                            binding.tvCrawlerStatus.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.md_theme_error));
                        } else {
                            binding.tvCrawlerStatus.setText("Đang đợi có mạng...");
                            //binding.tvCrawlerStatus.setTextColor(getResources().getColor(R.color.md_theme_error));
                            //binding.tvCrawlerStatus.setTextColor(getColor(R.color.md_theme_error));
                            // Tương thích tất cả API>=1
                            binding.tvCrawlerStatus.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.md_theme_error));

                        }
                        // Cho phép hủy khi ENQUEUED
                        isCrawling = false;
                        isStartingOrStopping = false;
                        binding.btnCrawlData.setText("Dừng Crawl");   //Hủy chuẩn bị
                        binding.btnCrawlData.setEnabled(true);
                        break;
                    case RUNNING:
                        // SETUP THANH PROGRESS BAR: LUÔN LUÔN
                        // VÌ URL có "start=1" c thể chạy trước các url phân trang, nên có thể đạt 100%
                        // ngưng các thread chạy các url phân trang chư xong!
                        // GIẢI PHÁP CHO MƯỢT: CH CHO ĐẠT 99%, CHỜ SUCCEEDED THÌ GÁN 100%
                        int percent = progress.getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
                        percent = Math.min(percent, 99);
                        long currentProcessedCount = progress.getLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), 0);
                        long ProcessedUrlStar1Count = progress.getLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), 0);
                        // HOẶC LẤY totalRecords = k1 - k0, VÌ k1, k0 là của phiên làm việc (của loại 2KT, hay 3KT, hay CUSTOM), không thay đổi
                        int totalRecords = k1 - k0;  // Gán Biến toàn câu cho phiên làm việc. Đối với Java: kích thước int = long
                        //long totalRecords = progress.getLong(selectedCrawlType.getTotalUrlsPrefKey().name(), 0);    // Lấy cách này: THỐNG NHẤT ĐỒNG BỘ, TRÁNH SAI

                        // Hoặc
                        //long totalRecords = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
                        // Hoaặc

                        String urlCurrent = progress.getString(AppConstants.URL_CURRENT);
                        // Thời gian có thể không cần truyền, MÀ LẤY TRỰC TIẾP TRÊN MAINACTIVITY
                        //long elapsedTime = System.currentTimeMillis() - startTime;

                        long elapsedTime = progress.getLong(AppConstants.WORK_ELAPSED_TIME, 0);   //milis Lấy từ progress
                        long remainingTime = progress.getLong(AppConstants.WORK_REMAINING_TIME, 0); // milis Lấy từ progress

                        // RẤT QUAN TRỌNG: Hãy đảm bảo bạn thấy log này với các giá trị đúng!
                        Log.d("UI_OBSERVER", "Progress Data: Percent=" + percent + "%," +
                                " Star1 Count=" + ProcessedUrlStar1Count + ", Total=" + totalRecords);
                        Log.d(TAG, "updateProgressUI: percent=" + percent);
                        Log.d(TAG, "updateProgressUI: TAO DAY=" + progress.getString("TaoDay"));

                        /** // Cập nhật UI elements CHUNG CHO TRẠNG THÁI ĐANG CHẠY
                         * Cập nhật ProgressBar và TextView trên Main Thread (LiveData tự động)
                         * Đảm bảo yourProgressBar và yourTextView đã được khởi tạo và tham chiếu đúng
                         * Update UI elements
                         */

                        // Thêm 1
                        binding.tvSoUrlErrors.setText(String.valueOf(dbHelperThuoc.getCountErrorUrls(selectedCrawlType.getUrlQueueTableName())));
                        //
                        binding.progressPercentText.setText(percent + "%");
                        binding.pbCrawlerProgress.setProgress(percent);
                        //binding.tvCrawlerStatus.setText("Đang xử lý...");
                        binding.tvSumProcessedUrls.setText(String.format("Tổng đã xử lý: %d", currentProcessedCount));
                        binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", ProcessedUrlStar1Count, totalRecords));
                        binding.tvInfoUrl.setText(urlCurrent);
                        binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(elapsedTime/1000)));
                        binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", DateUtils.formatElapsedTime(remainingTime/1000)));
                        // Update thread monitoring
                        //binding.tvThreadsWaiting.setText(String.format("Chờ: %d", threadsWaiting));
                        //binding.tvNumThreadsActiveThreads.setText(String.format("Đang chạy: %d", activeThreads));
                        //binding.tvNumThreadParallelism.setText(String.format("Số luồng tối đa: %d", threadsCompleted));
                        //binding.tvNumThreadParallelism.setText(String.format("Số tác vụ hoàn thành: %d", completedTasks)); //Số tác vụ hoàn thành

                        // threadsCompleted thường không chính xác cho việc hiển thị số luồng đang chạy
                        // Bạn có thể không hiển thị threadsCompleted nếu nó không có ý nghĩa rõ ràng


                        // 2 CHỨC NĂNG
                        if (!isCrawling) { // Chỉ set một lần
                            isCrawling = true;
                            binding.tvCrawlerStatus.setText("Đang thu thập dữ liệu...");
                            binding.tvCrawlerStatus.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.black));

                            isStartingOrStopping = false;
                            binding.btnCrawlData.setText("Dừng Crawl");
                            binding.btnCrawlData.setEnabled(true);
                            Log.d("STATE_TRACKER", "DEBUG_FLAG: RUNNING → updateUIState(true) (first time)");
                            updateUIState(true); // disable các nút khác nếu cần
                        } else {    //XEM
                            Log.d("STATE_TRACKER", "DEBUG_FLAG: RUNNING → updateUIState(true) skipped (already set)");
                        }
                        // Bắt đầu đo thời gian nếu chưa
                        if (crawlerStartTimeMillis == -1) {
                            crawlerStartTimeMillis = System.currentTimeMillis();
                            Log.d("STATE_TRACKER", "⏱ Crawler bắt đầu lúc: " + crawlerStartTimeMillis);
                        }
                        break;
                    case SUCCEEDED:
                        isCrawling = false;
                        binding.tvCrawlerStatus.setText("Thu thập hoàn tất.");

                        // Thêm: TẠI SAO urlsToProcessQueue STATUS = 0 VẪN CÒN?
                        // Kiểm tra DB xem còn dữ liệu chưa xử lý không
                        // Sau khi Worker kết thúc
                        if (dbHelperThuoc.hasErrorUrls(selectedCrawlType)) {
                            // Hiện nút "Xem lỗi"
                            runOnUiThread(() -> {
                                binding.btnViewErrors.setVisibility(View.VISIBLE);
                                binding.tvSoUrlErrors.setText("Số View Errors: " + dbHelperThuoc.getCountErrorUrls(selectedCrawlType.getUrlQueueTableName()));
                                binding.tvSoUrlErrors.setVisibility(View.VISIBLE);
                                binding.btnCrawlData.setText("Tiếp tục Crawl");
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.progressPercentText.setText("100%");
                                    binding.pbCrawlerProgress.setProgress(100);

                                    binding.btnViewErrors.setVisibility(View.GONE);
                                    binding.tvSoUrlErrors.setVisibility(View.GONE);
                                    binding.btnCrawlData.setText("Start Crawl");
                                }
                            });
                        }
                        // Cách cũ
//                        List<UrlInfo> urlsToProcessQueue =
//                                dbHelperThuoc.getDetailedUrlInfoByStatus(
//                                        selectedCrawlType.getUrlQueueTableName(), 0
//                                );
//
//                        if (urlsToProcessQueue != null && !urlsToProcessQueue.isEmpty()) {
//                            binding.btnCrawlData.setText("Tiếp tục Crawl");
//                        } else {
//                            binding.btnCrawlData.setText("Start Crawl");
//                        }

                        Toast.makeText(this, "Crawler đã hoàn thành!", Toast.LENGTH_SHORT).show();
                        resetAfterFinish();
                        break;
                    case FAILED:
                        isCrawling = false;
                        binding.tvCrawlerStatus.setText("Crawler thất bại!");
                        binding.btnCrawlData.setText("Tiếp tục Crawl");
                        Toast.makeText(this, "Crawler thất bại!", Toast.LENGTH_SHORT).show();
                        resetAfterFinish();
                        break;

                    case CANCELLED:
                        isCrawling = false;
                        cancelledWorker = true;
                        settingsRepository.saveCancelledWorker(selectedCrawlType.getCancelledWorkerPrefKey(),true); // reset

                        // Kiểm tra DB xem còn dữ liệu chưa xử lý không
                        //Mới
                        if (dbHelperThuoc.hasErrorUrls(selectedCrawlType)) {
                            // Hiện nút "Xem lỗi"
                            runOnUiThread(() -> {
                                binding.btnViewErrors.setVisibility(View.VISIBLE);
                                binding.tvSoUrlErrors.setText("Số View Errors: " + dbHelperThuoc.getCountErrorUrls(selectedCrawlType.getUrlQueueTableName()));
                                binding.tvSoUrlErrors.setVisibility(View.VISIBLE);
                                binding.btnCrawlData.setText("Tiếp tục Crawl");
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.btnViewErrors.setVisibility(View.GONE);
                                    binding.tvSoUrlErrors.setVisibility(View.GONE);
                                    binding.btnCrawlData.setText("Start Crawl");
                                }
                            });
                        }
                        // Cũ
//                        urlsToProcessQueue =
//                                dbHelperThuoc.getDetailedUrlInfoByStatus(
//                                        selectedCrawlType.getUrlQueueTableName(), 0
//                                );
//
//                        if (urlsToProcessQueue != null && !urlsToProcessQueue.isEmpty()) {
//                            binding.btnCrawlData.setText("Tiếp tục Crawl");
//                        } else {
//                            binding.btnCrawlData.setText("Start Crawl");
//                        }

                        binding.tvCrawlerStatus.setText("Crawl đã hủy!");
                        Toast.makeText(this, "Crawler đã hủy!", Toast.LENGTH_SHORT).show();
                        resetAfterFinish(); // Bỏ: VẪN CHE CÁC NÚT KHÁC, KHÔNG CHO CHỈ
                        break;
                } // END SWITCH

                /// tẠM bỎ dÙNG ĐOẠN TRÊN tƯƠNG ĐƯƠNG
//                if ("RETRY_NETWORK".equals(status)) {
//                    binding.tvCrawlerStatus.setText("Mạng bị mất. Đang thử lại...");
//                    //binding.tvCrawlerStatus.setTextColor(getResources().getColor(R.color.md_theme_error));
//                    binding.tvCrawlerStatus.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.md_theme_error));
//
//                // ENQUEUED
//                } else if (state == WorkInfo.State.ENQUEUED) {
//                    // Đang chờ constraint (ví dụ mạng)
//                    isCrawling = false; // chưa thực sự chạy
//                    isStartingOrStopping = false;
//                    isUIRunningStateSet = false; // reset flag
//                    Log.d("STATE_TRACKER", "DEBUG_FLAG: Reset RUNNING flag (ENQUEUED)");
//                    binding.btnCrawlData.setText("Hủy chuẩn bị");
//                    binding.btnCrawlData.setEnabled(true);
//                    // Tùy chọn: updateUIState(true);
//
//                    binding.tvCrawlerStatus.setText("Đang đợi có mạng...");
//                    //binding.tvCrawlerStatus.setTextColor(getResources().getColor(R.color.md_theme_error));
//                      binding.tvCrawlerStatus.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.md_theme_error));
//                    if (DISABLE_BUTTONS_WHEN_ENQUEUED) {
//                        updateUIState(true);    // Tùy chọn: updateUIState(true);
//                    }
//
//
//
//                // RUNNING
//                } else if (state == WorkInfo.State.RUNNING) { // ====== RUNNING: cập nhật tiến độ ======
//                    binding.tvCrawlerStatus.setText("Đang thu thập dữ liệu...");
//                    binding.tvCrawlerStatus.setTextColor(getResources().getColor(R.color.black));
//                    //binding.tvCrawlerStatus.setTextColor(getColor(ColorRes, 1));
//                    // Bắt đầu đo thời gian nếu chưa
//                    if (crawlerStartTimeMillis == -1) {
//                        crawlerStartTimeMillis = System.currentTimeMillis();
//                        Log.d("STATE_TRACKER", "⏱ Crawler bắt đầu lúc: " + crawlerStartTimeMillis);
//                    }
//                    // 2 CHỨC NĂNG
//                    isCrawling = true;
//                    isStartingOrStopping = false;   //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
//                    binding.btnCrawlData.setText("Dừng Crawl");
//                    binding.btnCrawlData.setEnabled(true);
//                    //
//                    // Chỉ update UI disable nút 1 lần
//                    if (!isUIRunningStateSet) { //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
//                        Log.d("STATE_TRACKER", "DEBUG_FLAG: RUNNING → updateUIState(true) (first time)");
//                        updateUIState(true); // disable các nút khác nếu cần
//                        isUIRunningStateSet = true;
//                    } else {
//                        Log.d("STATE_TRACKER", "DEBUG_FLAG: RUNNING → updateUIState(true) skipped (already set)");
//                    }
//
//                } else if (state.isFinished()) {
//                    // 2 CHỨC NĂNG
//                    isCrawling = false;
//                    //isStartingOrStopping chống double-click khi WorkManager chưa báo state mới.
//                    isStartingOrStopping = false;   //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
//                    binding.btnCrawlData.setText("Start Crawl");
//                    binding.btnCrawlData.setEnabled(true);
//                    updateUIState(false);
//                    //
//                    // Reset flag
//                    isUIRunningStateSet = false;    //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
//                    Log.d("STATE_TRACKER", "DEBUG_FLAG: Reset RUNNING flag (FINISHED)");
//                    // Tính thời gian chạy
//                    if (crawlerStartTimeMillis != -1) {
//                        long durationMillis = System.currentTimeMillis() - crawlerStartTimeMillis;
//                        double durationSeconds = durationMillis / 1000.0;
//                        Log.d("STATE_TRACKER", "⏱ Crawler hoàn tất. Thời gian chạy: " + durationSeconds + " giây");
//                        binding.tvCrawlerStatus.append("\nThời gian: " + durationSeconds + " giây");
//                        crawlerStartTimeMillis = -1;
//                    }
//
//                    // Các trạng thái kết thúc
//                    if (state == WorkInfo.State.SUCCEEDED) {
//                        binding.tvCrawlerStatus.setText("Thu thập hoàn tất.");
//                        binding.btnCrawlData.setText("Start Crawl");
//                        isCrawling = false;
//                        Toast.makeText(this, "Crawler đã hoàn thành!", Toast.LENGTH_SHORT).show();
//
//                    } else if (state == WorkInfo.State.CANCELLED) {
//                        binding.tvCrawlerStatus.setText("Crawl đã hủy!");
//                        binding.btnCrawlData.setText("Tiếp tục Crawl");
//                        isCrawling = false;
//                        Toast.makeText(this, "Crawler đã hủy!", Toast.LENGTH_SHORT).show();
//
//                    } else { // FAILED
//                        binding.tvCrawlerStatus.setText("Crawler thất bại!");
//                        binding.btnCrawlData.setText("Start Crawl");
//                        String message = workInfo.getOutputData().getString("message");
//                        Toast.makeText(this, "Crawler thất bại: " + message, Toast.LENGTH_SHORT).show();
//                    }
//
//                    // Enable buttons sau khi xong
//                    updateUIState(false);
//                }

            } else {
                // Không có công việc nào
                // 2 CHỨC NĂNG
                isCrawling = false;
                isStartingOrStopping = false;
                binding.btnCrawlData.setText("Start Crawl");
                binding.btnCrawlData.setEnabled(true);
                //
                binding.tvCrawlerStatus.setText("Sẵn sàng bắt đầu thu thập.");
                updateUIState(false);
            }
        });
    }



    private void checkAndObserveWorkerState() {
        //// TẠM BỎ
        //viewModelCrawlerGemini.loadLastWorkState(); // Try to load last state from DB
        // The observer for getLiveWorkState will then handle UI updates

        // 1. Lấy Work Request ID từ SharedPreferences (hoặc Room DB nếu bạn lưu ở đó)
        // Lưu ý: Dữ liệu này cần được lưu trữ bền vững (persistent)
        // Tôi đang dùng cách bạn đã sử dụng trong MyViewModelCrawler_Gemini để load từ Room DB
        // Bạn nên gọi một phương thức từ ViewModel để lấy Work ID đã lưu.
        // Ví dụ, ViewModel đã có logic này trong loadWorkRequestId()
        // và cập nhật currentWorkRequestIdLiveData.

        // Do MainActivity đã observe viewModelCrawlerGemini.getWorkInfoLiveData()
        // (mà LiveData này lại dựa vào currentWorkRequestIdLiveData),
        // thì khi ViewModel được khởi tạo, nó sẽ tự động load ID và
        // kích hoạt observer.

        // Tuy nhiên, để đảm bảo UI được cập nhật ngay lập tức
        // dựa trên trạng thái hiện tại của worker (nếu có),
        // chúng ta cần kiểm tra workInfoLiveData.getValue() ngay:
        List<WorkInfo> currentWorkInfos = viewModelCrawlerGemini.getWorkInfoLiveData().getValue();
        //WorkInfo currentWorkInfo = Objects.requireNonNull(viewModelCrawlerGemini.getWorkInfoLiveData().getValue()).get(0);

        if (currentWorkInfos != null && currentWorkInfos.isEmpty()) {
            WorkInfo currentWorkInfo = viewModelCrawlerGemini.getWorkInfoLiveData().getValue().get(0);
            // Nếu có thông tin workInfo (tức là có worker đang được theo dõi)
            WorkInfo.State state = currentWorkInfos.get(0).getState();
            Log.d(TAG, "checkAndObserveWorkerState: Initial check: Worker state is " + state);

            // Cập nhật UI dựa trên trạng thái ban đầu của worker
            // true nếu đang chạy hoặc đang trong hàng đợi, false nếu đã kết thúc
            updateUIState(state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED);

            // Nếu worker đã hoàn thành (thành công/thất bại/hủy)
            if (state.isFinished()) {
                // Có thể hiển thị thông báo cuối cùng nếu cần
                boolean success = currentWorkInfo.getOutputData().getBoolean("success", false);
                String message = currentWorkInfo.getOutputData().getString("message");
                if (message != null && !message.isEmpty()) {
                    // Chỉ hiển thị toast nếu có message để tránh toast rỗng
                    Toast.makeText(this, "Crawler " + (success ? "hoàn thành!" : "kết thúc: " + message), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "checkAndObserveWorkerState: Crawler " + (success ? "hoàn thành!" : "kết thúc: " + message));
                } else {
                    Toast.makeText(this, "Crawler đã kết thúc.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "checkAndObserveWorkerState: Crawler đã kết thúc.");
                }

                // Thêm xem thử có KHÁC PHỤC THIRNG THOẢNG ĐÃ isFinished NHƯNG CÁC NÚT KHÔNG anable
                updateUIState(false); // Bật tất cả các nút (trạng thái "sẵn sàng bắt đầu")
                Log.d(TAG, "checkAndObserveWorkerState: state.isFinished() = " +state.isFinished() +
                        " . Bật anable tất cả các nút!");
            }
        } else {
            // Không có worker nào đang chạy hoặc được theo dõi, đảm bảo UI ở trạng thái ban đầu
            Log.d(TAG, "Initial check: No active worker found. Setting UI to default.");
            if (!isCrawling) { // KHÔNG CHẠY
                updateUIState(false); // Bật tất cả các nút (trạng thái "sẵn sàng bắt đầu")
            }
        }

        /** Phương thức checkAndObserveWorkerState có chức năng khác observeViewModel
         * checkAndObserveWorkerState: Chỉ xem xét trang thái của worker 1 lần khi bắt đầu onCreate() của MainActivity
         *
         * observeViewModel: LUÔN LUÔN QUAN SÁT TRẠNG THÁI CỦA WORKER MANAGER
         */

        // Vẫn cần observer để cập nhật UI động khi trạng thái worker thay đổi sau đó
        // (ví dụ: từ ENQUEUED sang RUNNING, hoặc RUNNING sang SUCCEEDED/FAILED)
        //observeViewModel(); // Đảm bảo observer vẫn được thiết lập
    }

    private void updateProgressUI(WorkInfo workInfo) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                .format(new java.util.Date());
        Log.e("DebugUpdateUI", "🛠 " + time +
                        " | workInfo=" + workInfo.toString() +
                        " | workInfo.getOutputData=" + workInfo.getOutputData() +
                        " | thread=" + Thread.currentThread().getName(),
                new Exception("STACK TRACE")); //new Exception("STACK TRACE") giúp in stacktrace để biết ai gọi hàm này.

        // Đây là điểm nơi lỗi NullPointerException xảy ra trước đó
            // Bạn có thể chọn:
            // 1. Gán một giá trị mặc định an toàn để tiếp tục (ví dụ: selectedCrawlType = CrawlType.TWO_CHAR;)
            // 2. Bỏ qua phần cập nhật UI này
            // 3. Hiển thị thông báo lỗi cho người dùng (nếu đây là lỗi nghiêm trọng)
            // Trong hầu hết các trường hợp, bạn có thể chỉ cần gán mặc định để tránh crash.

        // Phòng selectedCrawlType null
        if (selectedCrawlType == null) {
            selectedCrawlType = settingsRepository.getSelectedCrawlType();
        }


        if (workInfo != null) {
            // RẤT QUAN TRỌNG: Hãy đảm bảo bạn thấy log này!
            Log.d("UI_OBSERVER", "WorkInfo received: State=" + workInfo.getState() + ", isFinished=" + workInfo.getState().isFinished());

            Data progress = workInfo.getProgress();
            Log.d(TAG, "updateProgressUI: !progress.getKeyValueMap().isEmpty()=" + !progress.getKeyValueMap().isEmpty());
            Log.d(TAG, "updateProgressUI: TAO DAY=" + progress.getString("TaoDay"));
            //if (progress != null && !progress.getKeyValueMap().isEmpty()) {
                // Luôn xử lý dữ liệu tiến độ khi Worker đang chạy
            if (!workInfo.getState().isFinished() && !progress.getKeyValueMap().isEmpty()) {
                int percent = progress.getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
                long currentProcessedCount = progress.getLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), 0);
                long ProcessedUrlStar1Count = progress.getLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), 0);
                long totalRecords = progress.getLong(selectedCrawlType.getTotalUrlsPrefKey().name(), 0);    // Lấy cách này: THỐNG NHẤT ĐỒNG BỘ, TRÁNH SAI

                // Hoặc
                //long totalRecords = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
                // Hoaặc
                //totalRecords = selectedCrawlType.getKyTuArray(settingsRepository).length;
                //long totalRecords = progress.getLong(AppConstants.WORK_TOTAL_RECORDS, 0);

                String urlCurrent = progress.getString(AppConstants.URL_CURRENT);
                // Thời gian có thể không cần truyền, MÀ LẤY TRỰC TIẾP TRÊN MAINACTIVITY
                //long elapsedTime = System.currentTimeMillis() - startTime;

                long elapsedTime = progress.getLong(AppConstants.WORK_ELAPSED_TIME, 0);   //milis Lấy từ progress
                long remainingTime = progress.getLong(AppConstants.WORK_REMAINING_TIME, 0); // milis Lấy từ progress

                // RẤT QUAN TRỌNG: Hãy đảm bảo bạn thấy log này với các giá trị đúng!
                Log.d("UI_OBSERVER", "Progress Data: Percent=" + percent + "%," +
                        " Star1 Count=" + ProcessedUrlStar1Count + ", Total=" + totalRecords);
                Log.d(TAG, "updateProgressUI: percent=" + percent);
                Log.d(TAG, "updateProgressUI: TAO DAY=" + progress.getString("TaoDay"));

                // Thread monitoring data
                int threadsWaiting = progress.getInt(AppConstants.THREADS_WAITING, 0);
                int queuedTasks = progress.getInt(AppConstants.QUEUED_TASKS, 0);
                int activeThreads = progress.getInt(AppConstants.ACTIVE_THREADS, 0);
                int parallelism = progress.getInt(AppConstants.PARALLELISM, 0); //Tổng luoogng tối đa
                int threadsCompleted = progress.getInt(AppConstants.THREADS_COMPLETED, 0);  // KHÔNG CHÍNH XÁC
                int completedTasks = progress.getInt("completedTasks", 0);
                String statusMessage = progress.getString(AppConstants.WORK_STATUS_MESSAGE);

                /** // Cập nhật UI elements CHUNG CHO TRẠNG THÁI ĐANG CHẠY
                 * Cập nhật ProgressBar và TextView trên Main Thread (LiveData tự động)
                 * Đảm bảo yourProgressBar và yourTextView đã được khởi tạo và tham chiếu đúng
                 * Update UI elements
                 */
                binding.progressPercentText.setText(percent + "%");
                binding.pbCrawlerProgress.setProgress(percent);
                binding.tvCrawlerStatus.setText(statusMessage != null ? statusMessage : "Đang xử lý...");
                //binding.tvSumProcessedUrls.setText(String.format("Tổng đã xử lý: %d", currentProcessedCount));
                Log.w("DebugUpdateUI", "📌 Đang set text: \"" + String.format("%d/%d", ProcessedUrlStar1Count, totalRecords) + "\" từ nhánh ...");
                Utils.debugSetText(binding.tvSumProcessedUrls, String.format("Tổng đã xử lý: %d", currentProcessedCount)); // THEO DÕI
                binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", ProcessedUrlStar1Count, totalRecords));
                binding.tvInfoUrl.setText(urlCurrent);
                binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(elapsedTime/1000)));
                binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", DateUtils.formatElapsedTime(remainingTime/1000)));
                // Update thread monitoring
                //binding.tvThreadsWaiting.setText(String.format("Chờ: %d", threadsWaiting));
                //binding.tvNumThreadsActiveThreads.setText(String.format("Đang chạy: %d", activeThreads));
                //binding.tvNumThreadParallelism.setText(String.format("Số luồng tối đa: %d", threadsCompleted));
                //binding.tvNumThreadParallelism.setText(String.format("Số tác vụ hoàn thành: %d", completedTasks)); //Số tác vụ hoàn thành

                // threadsCompleted thường không chính xác cho việc hiển thị số luồng đang chạy
                // Bạn có thể không hiển thị threadsCompleted nếu nó không có ý nghĩa rõ ràng
            } else if (workInfo.getState().isFinished()) {
                // Xử lý khi Worker kết thúc (SUCCEEDED, FAILED, CANCELLED)
                // XEM
                if (progress.getKeyValueMap().isEmpty()) {
                    Log.d(TAG, "Work is finished. 1/ progress= " + progress);
                }
                Log.d(TAG, "Work is finished. progress= " + progress);

                Log.d(TAG, "Work is finished. Final state: " + workInfo.getState());
                // Đảm bảo progress bar đạt 100% hoặc hiển thị trạng thái cuối cùng
                binding.pbCrawlerProgress.setProgress(100); // Đặt 100% là trạng thái cuối cùng trên UI
                Log.d("UI_OBSERVER", "Work is finished. Final state: " + workInfo.getState());
                // Cập nhật UI cuối cùng hoặc ẩn ProgressBar

                WorkInfo.State state = workInfo.getState();
                // Chỉ gọi updateUIState một lần ở đây cho tất cả các trạng thái kết thúc
                updateUIState(false); // Enable all buttons when finished
                Log.d(TAG, "observeViewModel: Work finished. State: " + state + ". Bật (enable) tất cả các nút!");
                boolean success = workInfo.getOutputData().getBoolean("success", false);
                String message = workInfo.getOutputData().getString("message");
                Toast.makeText(this, "Crawler " + (success ? "hoàn thành!" : "thất bại: " + message), Toast.LENGTH_LONG).show();
                Toast.makeText(this, "Crawler hoàn thành! State isFinish =" + workInfo.getState().isFinished(), Toast.LENGTH_LONG).show();
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    binding.progressPercentText.setText("100%");
                    binding.pbCrawlerProgress.setProgress(100); // Đặt percent= 100% là trạng thái cuối cùng trên UI
                    binding.tvCrawlerStatus.setText("Hoàn thành công việc!");

                    //int percent;
                    long currentProcessedCount = 0;
                    long processedUrlStar1Count = 0;
                    long totalRecords = 0;
                    String urlCurrent = "";
                    long elapsedTime = 0;
                    long remainingTime = 0 ;
                    if (progress.getKeyValueMap().isEmpty()) {
                        // Lấy từ getOutPutData
                        Log.d(TAG, "updateProgressUI: LẤY DATA TỪ getOutPutData ");
                        Data output = workInfo.getOutputData();
                        //int percent = progress.getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
                        currentProcessedCount = output.getLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), 0);
                        processedUrlStar1Count = output.getLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), 0);
                        totalRecords = output.getLong(selectedCrawlType.getTotalUrlsPrefKey().name(), 0);    // Lấy cách này: THỐNG NHẤT ĐỒNG BỘ, TRÁNH SAI
                        urlCurrent = output.getString(AppConstants.URL_CURRENT);
                        elapsedTime = progress.getLong(AppConstants.WORK_ELAPSED_TIME, 0);
                        // NẾU KẾT THÚC THÌ THỜI GIAN CÒN LẠI PHẢI = 0
                        remainingTime = progress.getLong(AppConstants.WORK_REMAINING_TIME, 0);  // NẾU KẾT THÚC THÌ THOWIG GIAN CÒN LẠI PHẢI = 0
                    } else {
                        Log.d(TAG, "updateProgressUI: LẤY DATA TỪ progress ");
                        //percent = progress.getInt(AppConstants.WORK_PROGRESS_PERCENT, 0);
                        currentProcessedCount = progress.getLong(selectedCrawlType.getCrawledUrlsCountPrefKey().name(), 0);
                        processedUrlStar1Count = progress.getLong(selectedCrawlType.getCrawledUrlStart1sCountPrefKey().name(), 0);
                        totalRecords = progress.getLong(selectedCrawlType.getTotalUrlsPrefKey().name(), 0);    // Lấy cách này: THỐNG NHẤT ĐỒNG BỘ, TRÁNH SAI

                        urlCurrent = progress.getString(AppConstants.URL_CURRENT);
                        // Thời gian có thể không cần truyền, MÀ LẤY TRỰC TIẾP TRÊN MAINACTIVITY
                        elapsedTime = progress.getLong(AppConstants.WORK_ELAPSED_TIME, 0);
                        //remainingTime = progress.getLong(AppConstants.WORK_REMAINING_TIME, 0);  // Hoàn thành thì = 0
                    }

                    //binding.tvSumProcessedUrls.setText(String.format("Tổng đã xử lý: %d", currentProcessedCount));
                    Utils.debugSetText(binding.tvSumProcessedUrls, String.format("Tổng đã xử lý: %d", currentProcessedCount)); // THEO DÕI
                    binding.tvSoUrlTrenTotalUrl.setText(processedUrlStar1Count + "/" + totalRecords);
                    binding.tvInfoUrl.setText(urlCurrent);
                    binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(elapsedTime/1000)));
                    // NẾU KẾT THÚC THÌ THỜI GIAN CÒN LẠI PHẢI = 0
                    binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", DateUtils.formatElapsedTime(0))); // Hoàn thành thì = 0

                    // Bạn có thể ẩn các thông tin thời gian, số luồng, v.v.
                    // binding.tvElapsedTimes.setVisibility(View.GONE);
                    // binding.tvTimeRemaining.setVisibility(View.GONE);
                    // Có thể thành công 1 phần, nhưng chỉ cha 1 worker, xem như thành công
                    // Lấy loại crawl hiện tại để reset đúng thông số
                    // Lấy loại crawl hiện tại để reset đúng thông số
                    //CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();

                    // >>> RESET <<<    // KHÔNG ĐỂ, CRAWL CHẠY MỚI LÀM
//                    // 1. Lấy giá trị chuỗi UUID từ LiveData một cách an toàn
//                    String workRequestIdString = viewModelCrawlerGemini.getCurrentWorkRequestIdLiveData().getValue();
//
//                    // 2. Kiểm tra xem chuỗi có hợp lệ không trước khi chuyển đổi
//                    if (workRequestIdString != null && !workRequestIdString.isEmpty()) {
//                        try {
//                            // 3. Chuyển đổi chuỗi thành đối tượng UUID
//                            UUID curentWorkRequestId = UUID.fromString(workRequestIdString);
//                            // ĐOẠN CODE NÀY CHỈ SỬ DỤNG KHI BẮT ĐÀU CHẠY (CRAWL) MỚI HÒA TOÀN
//                            // Gọi handleWorkerSuccess. excelExecutor đã được khởi tạo một lần.
//                            handleWorkerSuccess(curentWorkRequestId, selectedCrawlType); // Reset
//                        } catch (IllegalArgumentException e) {
//                            // Xử lý trường hợp chuỗi không phải là định dạng UUID hợp lệ
//                            Log.e(TAG, "Lỗi định dạng UUID khi xử lý thành công worker: " + workRequestIdString, e);
//                            // Tùy thuộc vào logic của bạn, có thể thông báo cho người dùng
//                            // hoặc bỏ qua xử lý với UUID này nếu nó không quan trọng
//                            // trong trường hợp lỗi này.
//                        }
//
//                    } else {
//                        // Xử lý trường hợp không có Work Request ID hoặc ID là null/rỗng
//                        Log.w(TAG, "Không có Work Request ID khi worker thành công hoặc ID là null/rỗng. Không thể xử lý handleWorkerSuccess.");
//                    }

                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                    // Lấy thông điệp lỗi nếu Worker đã trả về
                    String errorMessage = workInfo.getOutputData().getString("message");
                    binding.progressPercentText.setText("Lỗi!");
                    binding.tvCrawlerStatus.setText("Thất bại: " + (errorMessage != null ? errorMessage : "Không rõ lỗi."));
                    // Có thể hiển thị lại progress tại thời điểm thất bại nếu có sẵn trong workInfo.getProgress() cuối cùng
                } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
                    binding.progressPercentText.setText("Hủy!");
                    binding.tvCrawlerStatus.setText("Đã hủy công việc.");
                    updateUIState(false); // Enable all buttons when finished
                }

                // Ẩn/hiển thị các nút/UI liên quan đến quá trình đang chạy

            } else {
                // progress.getKeyValueMap().isEmpty() = true: là do Dữ liệu tiến độ rỗng,
                // Worker có thể vừa bắt đầu hoặc chưa công bố
                // Dữ liệu tiến độ rỗng và Worker không ở trạng thái kết thúc (ví dụ: ENQUEUED)
                //Log.d("WorkManager", "Worker đang chạy, chưa có dữ liệu tiến độ.");
                // Bạn có thể hiển thị một spinner hoặc thông báo "Đang xử lý..."
                // Có thể hiển thị một spinner hoặc thông báo "Đang khởi tạo..."
                Log.d(TAG, "Worker đang chạy, nhưng progress data rỗng hoặc chưa sẵn sàng. State: " + workInfo.getState());
            }

        }
    }

    private void stopCrawler() {
        // Hỏi trước khi dùng
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn dừng crawl?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    // Tác vụ đơn giản: hủy WorkManager
                    viewModelCrawlerGemini.stopCrawler(); // Hủy tác vụ WorkManager
                isCrawling = false; // Đã có quan sát
                    excelExecutor.execute(() -> {   //Không có tác vụ nền (bỏ excelExecutor)
                        try {
                            // Thực hiện các tác vụ nền
                            Log.d(TAG, "Crawler stopped in background thread, thread: " + Thread.currentThread().getName());

                            // Chuyển sang main thread: Cập nhật UI trên main thread
                            // "stopCrawler" được gọi từ main thread (ví dụ: từ sự kiện nhấn nút).
                            runOnUiThread(() -> { // Có thể bỏ vì: cập nhật UI và được gọi từ main thread
                                Log.d(TAG, "Updating UI on main thread: " + Thread.currentThread().getName());
                                // Vì dùng btnCrawlData có 2 chức năng nên bỏ
//                                if (!binding.btnCrawlData.isEnabled()) {
//                                    binding.btnCrawlData.setEnabled(true);
//                                }
//                                if (binding.btnCancelCrawler.isEnabled()) {
//                                    binding.btnCancelCrawler.setEnabled(false);
//                                }
                                //

                                //binding.pbCrawlerProgress.setVisibility(View.GONE);
                                binding.tvCrawlerStatus.setText("Crawler đã dừng");
                                Toast.makeText(MainActivity.this, "Crawler đã dừng", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error stopping crawler: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi khi dừng crawler: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                // KHÔNG CẦN dialog.dismiss() vì Android sẽ tự động đóng dialog sau khi xử lý xong callback,
                // trừ khi bạn override hành vi này, ví dụ như gọi dialogInterface.dismiss() bị ngăn lại, hoặc dùng custom Dialog.
                .setNegativeButton("Hủy", null)
                .show();

        //
        Log.d(TAG, "Stopping crawler...");

    }

    // --- Permission and Export Logic (giữ nguyên) ---
    private void checkAndExportExcel(boolean isXml) {  // Chạy, Hiện diễn tiến progress trên ExportProgressActivity
        //this.isExportingXml = isXml; // Lưu trạng thái isXml
        Log.d(TAG, "checkAndExportExcel called. isXml: " + isXml);

        // checkAndExportExcel: Chạy, Hiện diễn tiến progress trên ExportProgressActivity
        // Lấy CrawlType hiện tại để xác định tên bảng và tên file
        //CrawlType selectedCrawlType = SettingsRepository.getSelectedCrawlType();
        CrawlType selectedCrawlType = settingsRepository.getSelectedCrawlType();
        String tableThuoc = selectedCrawlType.getTableThuocName();
        String nameFileExcel = isXml ? selectedCrawlType.getExcelFileName().replace(".xlsx", ".xml") : selectedCrawlType.getExcelFileName();

        //File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportDir = Utils.getExternalFilesDirThuocBietDuoc(this.getApplicationContext(),false); // Đổi tên dòng này)
        //File exportedFile = new File(exportDir, nameFileExcel);

        /** Bước 1: Kiểm tra quyền lưu trữ (cho API <= 29) và quyền thông báo (cho API >= 33)
         * Kiểm tra xem ứng dụng đã có các quyền lưu trữ cơ bản (WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE) hay chưa.
         */
//        //if (!hasStoragePermissions()) { //cần được cập nhật để kiểm tra tất cả các quyền cần thiết
//        if (!hasRequiredRuntimePermissions()) { // Phương thức này cần kiểm tra cả WRITE/READ_EXTERNAL_STORAGE và POST_NOTIFICATIONS
//
//            //Nếu chưa có quyền, ứng dụng sẽ gọi phương thức này để hiển thị hộp thoại
//            // yêu cầu người dùng cấp quyền.
//            /** Quan trọng: Việc yêu cầu quyền này là một quá trình bất đồng bộ (asynchronous).
//             * Tức là, hệ thống Android sẽ hiển thị hộp thoại cho người dùng và ứng dụng của bạn
//             * sẽ không nhận được kết quả (người dùng chấp nhận hay từ chối) ngay lập tức.
//             * Kết quả sẽ được trả về sau thông qua các callback của ActivityResultLauncher
//             * mà bạn đã cấu hình (requestPermissionLauncher).
//             */
////            requestStoragePermissions();    // Yêu cầu quyền
////            return; // Dừng lại ở đây
//            /** return;: Vì ứng dụng chưa có quyền tại thời điểm này và đang chờ phản hồi từ người dùng,
//             * nên không thể tiếp tục thực hiện các tác vụ cần quyền (ví dụ: xuất Excel).
//             * Lệnh return; đảm bảo rằng phương thức checkAndExportExcel sẽ kết thúc ngay lập tức,
//             * ngăn chặn mọi dòng code tiếp theo trong phương thức này
//             * (như gọi startExportProgressActivity) được thực thi trước khi quyền được cấp.
//             *
//             * Sau khi người dùng cấp quyền, phương thức checkAndExportExcel
//             * (hoặc một phương thức khác dựa trên kết quả quyền)
//             * sẽ cần được gọi lại để tiếp tục công việc.
//             */
//            requestRequiredRuntimePermissions(); // Phương thức này sẽ gọi requestPermissionLauncher
//            return; // Dừng lại ở đây, chờ kết quả từ launcher
//        }

        /** // Bước 2: Kiểm tra quyền MANAGE_EXTERNAL_STORAGE (cho API >= 30)
         * Trường hợp 2: Quyền "Quản lý tất cả tệp" (MANAGE_EXTERNAL_STORAGE) trên Android 11+
         * !Environment.isExternalStorageManager()): Dòng này kiểm tra xem thiết bị có đang chạy Android 11
         * (API 30) trở lên không và ứng dụng đã có quyền đặc biệt "Quản lý tất cả tệp" hay chưa.
         */
//        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) { // Android 11 (API 30) trở lên
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) trở lên
//            // Nếu chưa có quyền, phương thức này sẽ hiển thị một AlertDialog giải thích cho người dùng
//            // và cung cấp tùy chọn để mở màn hình cài đặt của ứng dụng để cấp quyền MANAGE_EXTERNAL_STORAGE.
//            // Hiển thị dialog và chuyển hướng người dùng đến cài đặt
//            Log.d(TAG, "Đang kiểm tra quyền MANAGE_EXTERNAL_STORAGE (API >= 30)");
//            if (!Environment.isExternalStorageManager()) {
//                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE CHƯA được cấp. Hiển thị dialog.");
//                showManageAllFilesPermissionDialog();
//                return; // Dừng lại ở đây, chờ người dùng cấp quyền trong cài đặt
//            } else {
//                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE ĐÃ được cấp.");
//            }
//        } else {
//            Log.d(TAG, "Không cần kiểm tra MANAGE_EXTERNAL_STORAGE cho API < 30.");
//        }


//        // Nếu tất cả các kiểm tra quyền đều vượt qua, thì mới tiến hành xuất
//        Log.d(TAG, "All permissions checked and granted. Proceeding to export.");
//        proceedWithExport(isXml); // Gọi phương thức để bắt đầu xuất

        // Sau khi tự cấp quyền thì nhấn: cahyj lai xuất EXCEL
        // Pass context and filename to ExportProgressActivity
        // 1/ Chạy Xuất file Excel, xem tiến trình thông qua ExportProgressActivity

        // KHÔNG CẦN QUYỀN
        Intent intent = new Intent(this, ExportProgressActivity.class);
        intent.putExtra(AppConstants.EXPORT_TYPE, isXml);
        intent.putExtra(AppConstants.TABLE_THUOC, tableThuoc); // Truyền tên bảng
        intent.putExtra(AppConstants.NAME_FILE_EXCEL, nameFileExcel);
        // Bắt đầu chạy intent Export Excel
        Log.d(TAG, "checkAndExportExcel: Bắt đầu chạy intent Export Excel");
        startActivity(intent);
        
        // 2/ Chạy Xuất file Excel, xem tiến trình trức tiếp trên MainActivity
        // Tạo và hiển thị dialog tiến độ tùy chỉnh: thông qua Dialog: excel_export_progress_dialog.xml.
        //startExportProcess(exportedFile, false); // true cho XML


    }

    // Phương thức mới để kiểm tra tất cả các quyền runtime cần thiết
    private boolean hasRequiredRuntimePermissions() {
        boolean granted = true;

        // Kiểm tra WRITE/READ_EXTERNAL_STORAGE cho API <= Q (29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        // Đối với API 30+ (R+), các quyền này không cần thiết nếu chỉ làm việc với getExternalFilesDir()
        // Nếu bạn cần quyền truy cập rộng hơn, MANAGE_EXTERNAL_STORAGE được kiểm tra riêng.

        // Kiểm tra POST_NOTIFICATIONS cho API 33 (TIRAMISU) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        return granted;
    }

    private boolean hasStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

//    private void requestStoragePermissions() {  // Chỉ 1 chuỗi duy nhất
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }
//    }

    //@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    // Phương thức mới để yêu cầu tất cả các quyền runtime cần thiết
    private void requestRequiredRuntimePermissions() {
        // Thu thập các quyền cần yêu cầu dựa trên phiên bản SDK
        List<String> permissionsToRequest = new ArrayList<>();

        // Quyền lưu trữ cho API 23-29
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Quyền thông báo cho API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            // Chỉ yêu cầu các quyền chưa được cấp
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    // Phương thức để bắt đầu quá trình xuất Excel/XML
    private void proceedWithExport(boolean isXml) {
        // Đây là nơi bạn gọi startExportProgressActivity
        // Hoặc initiateExportProcess(isXml) nếu bạn đã chuyển sang SAF
        startExportProgressActivity(isXml ? "XML" : "XLSX"); // Hoặc initiateExportProcess(isXml);
    }

    private void requestStoragePermissions() {  // Chuỗi quyền là một mảng
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            });

            //requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            Manifest.permission.POST_NOTIFICATIONS;
//        }

    }

    private void showManageAllFilesPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền truy cập tệp")
                .setMessage("Ứng dụng cần quyền quản lý tất cả tệp để xuất dữ liệu. Vui lòng cấp quyền trong cài đặt.")
                .setPositiveButton("Đi đến cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    //manageAllFilesLauncher.launch(intent);
                    // Sai: Nếu bạn khai báo lại ở đây
                    // ActivityResultLauncher<Intent> manageAllFilesLauncher = registerForActivityResult(...);
                    // Hoặc nếu bạn không khởi tạo nó ở onCreate
                    manageAllFilesLauncher.launch(intent); // Lỗi nếu manageAllFilesLauncher là null
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    updateUIState(false); // Enable all buttons when finished
                    dialog.dismiss();
                })
                .show();
    }

    // Phương thức để mở thư mục và cho phép người dùng chọn tệp
    public void openFolderToChooseFile() {
        try {
            // 1. Xác định thư mục riêng của ứng dụng bạn muốn hiển thị
            // Đây là /storage/emulated/0/Android/data/YOUR_PACKAGE_NAME/files/Documents
            File exportDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

            if (exportDir == null) {
                Toast.makeText(this, "Không thể truy cập thư mục lưu trữ ngoài của ứng dụng.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "External files directory (Documents) is null, possibly no external storage mounted.");
                return;
            }

            // Đảm bảo thư mục tồn tại, nếu không thì tạo nó
            if (!exportDir.exists()) {
                if (!exportDir.mkdirs()) {
                    Toast.makeText(this, "Không thể tạo thư mục: " + exportDir.getName(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to create export directory: " + exportDir.getAbsolutePath());
                    return;
                } else {
                    Log.d(TAG, "Created export directory: " + exportDir.getAbsolutePath());
                }
            }

            // 2. Lấy Uri an toàn bằng FileProvider cho thư mục đó
            // AUTHORITY này PHẢI khớp chính xác với android:authorities trong AndroidManifest.xml
            Uri directoryUri = FileProvider.getUriForFile(
                    this,
                    "com.example.crawlertbdgemini2modibasicview.provider", // AUTHORITY của bạn
                    exportDir
            );
            Log.d(TAG, "FileProvider URI for directory: " + directoryUri.toString());


            // 3. Tạo Intent để mở giao diện chọn tệp
            // ACTION_OPEN_DOCUMENT được khuyến nghị cho Android 4.4 (API 19) trở lên
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); // Chỉ hiển thị các tệp có thể mở
            //intent.setType("*/*"); // Cho phép chọn bất kỳ loại tệp nào
            // (ví dụ: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" cho Excel)
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // Nếu chỉ muốn chọn file Excel

            // 4. Đặt Uri ban đầu để hướng SAF Picker đến thư mục của bạn (từ Android 8.0/API 26 trở lên)
            // Đặt Uri ban đầu để hướng dẫn người dùng đến thư mục của bạn.
            // Lưu ý: setInitialUri chỉ là một GỢI Ý. Không phải tất cả các ứng dụng quản lý tệp đều hỗ trợ
            // hoặc tôn trọng gợi ý này. SAF Picker của Google thường hỗ trợ tốt.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Oreo (>=API 26)
                // Chuyển đổi FileProvider URI thành Document Uri nếu cần, mặc dù SAF thường có thể xử lý Content URI
                // Tuy nhiên, EXTRA_INITIAL_URI hoạt động tốt nhất với document URIs.
                // Đối với thư mục của ứng dụng, FileProvider URI là đủ trong nhiều trường hợp.
                // Nếu vẫn không hoạt động, cân nhắc dùng DocumentsContract.buildDocumentUriUsingTree() hoặc DocumentsContract.buildChildDocumentsUriUsingTree()
                // nhưng điều đó phức tạp hơn nhiều.
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directoryUri);
                Log.d(TAG, "EXTRA_INITIAL_URI set to: " + directoryUri);
            } else {
                // Đối với các phiên bản cũ hơn (trước API 26), EXTRA_INITIAL_URI không có sẵn
                // và ACTION_OPEN_DOCUMENT thường không hỗ trợ đặt thư mục bắt đầu trực tiếp.
                // Người dùng sẽ phải tự điều hướng.
                Log.d(TAG, "EXTRA_INITIAL_URI not supported on API < 26.");
            }

            // 5. Cấp quyền đọc tạm thời cho Intent (rất quan trọng khi dùng FileProvider)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 6. Khởi chạy Intent để người dùng chọn tệp
            if (intent.resolveActivity(getPackageManager()) != null) {
                chooseFileLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Không tìm thấy ứng dụng quản lý tệp có khả năng chọn tài liệu.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "No activity found to handle ACTION_OPEN_DOCUMENT intent.");
            }

        } catch (IllegalArgumentException e) {
            // Lỗi này xảy ra nếu FileProvider không được cấu hình đúng trong AndroidManifest.xml
            // hoặc đường dẫn không được khai báo trong provider_paths.xml
            Log.e(TAG, "FileProvider Configuration Error: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi cấu hình ứng dụng (FileProvider). Vui lòng kiểm tra logcat.", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            // Lỗi liên quan đến quyền truy cập tệp
            Log.e(TAG, "Security Error: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi bảo mật khi truy cập thư mục. Hãy kiểm tra quyền truy cập tệp.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Xử lý các lỗi chung khác
            Log.e(TAG, "Unexpected Error: " + e.getMessage(), e);
            Toast.makeText(this, "Đã xảy ra lỗi không mong muốn: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void openExportFolder() {
        try {
        // 1. Xác định thư mục bạn muốn mở
        //File exportDir = getExternalFilesDir(null);   // Thư mục nào
        //File exportDir = getFilesDir() // KHÔNG CẦN QUYỀN: trả về đường dẫn đến thư mục của ứng dụng bên trong bộ nhớ trong của thiết bị, lưu trữ các file dữ liệu riêng tư
        //File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS); // CẦN QUYỀN
        File exportDir = Utils.getExternalFilesDirThuocBietDuoc(this.getApplicationContext(), false);
        // Đảm bảo thư mục tồn tại, nếu không thì tạo nó
        if (exportDir == null) {
            Toast.makeText(this, "Không thể truy cập thư mục lưu trữ ngoài.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "External files directory (Documents) is null.");
            return;
        }

        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) { // Tạo thư mục nếu nó chưa tồn tại
                Toast.makeText(this, "Không thể tạo thư mục xuất.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to create export directory: " + exportDir.getAbsolutePath());
                return;
            }
        }

        //if (exportDir.exists() && exportDir.isDirectory()) {
            //Uri uri = Uri.parse(exportDir.getAbsolutePath()); // cũ HÌNH NHƯ KHÔNG DÙNG NỮA
            // 2. Lấy Uri an toàn bằng FileProvider
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    //"com.example.crawlertbdgemini2modibasicview.provider", // Phải khớp với android:authorities trong manifest
                    getPackageName() + ".provider", // Sử dụng applicationId + ".provider"
                    exportDir
            );
            // 3. Tạo Intent để mở thư mục
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "resource/folder");   // Sử dụng kiểu MIME không chính thức cho thư mục
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Cấp quyền đọc tạm thời cho ứng dụng nhận Intent

            // 4. Kiểm tra xem có ứng dụng nào có thể xử lý Intent này không
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Không có ứng dụng nào xử lý được "resource/folder"
                // Bạn có thể cân nhắc mở một trình chọn tệp chung nếu không tìm thấy
                // hoặc hiển thị thông báo cho người dùng rõ ràng hơn.
                Toast.makeText(this, "Không tìm thấy ứng dụng để mở thư mục. Bạn có thể cần cài đặt một trình quản lý tệp.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "No activity found to handle folder intent for: " + exportDir.getAbsolutePath());

                // Thay thế: Cố gắng mở với ACTION_GET_CONTENT và wildcard MIME type
                // (có thể ít đáng tin cậy hơn để mở thư mục trực tiếp nhưng vẫn có thể dẫn đến trình quản lý tệp)
                 Intent defaultFileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT);
                 defaultFileBrowserIntent.setDataAndType(contentUri, "*/*");
                 defaultFileBrowserIntent.addCategory(Intent.CATEGORY_OPENABLE);
                 defaultFileBrowserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 if (defaultFileBrowserIntent.resolveActivity(getPackageManager()) != null) {
                     startActivity(Intent.createChooser(defaultFileBrowserIntent, "Mở thư mục với..."));
                 } else {
                     Toast.makeText(this, "Không tìm thấy ứng dụng quản lý tệp.", Toast.LENGTH_LONG).show();
                 }


                // Ví dụ: Mở trình quản lý tệp mặc định:
//                Intent defaultFileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                defaultFileBrowserIntent.setDataAndType(contentUri, "*/*"); // Cố gắng mở với loại chung
//                startActivity(Intent.createChooser(defaultFileBrowserIntent, "Open folder with..."));
            }


            // cũ
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(uri, "resource/folder");
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivity(Intent.createChooser(intent, "Mở thư mục bằng"));
//            } else {
//                Toast.makeText(this, "Không có ứng dụng nào để mở thư mục.", Toast.LENGTH_SHORT).show();
//            }
        //} else {
        //    Toast.makeText(this, "Thư mục xuất không tồn tại.", Toast.LENGTH_SHORT).show();
        //}
        } catch (IllegalArgumentException e) {
            // Lỗi này xảy ra nếu FileProvider không được cấu hình đúng trong AndroidManifest.xml
            // hoặc đường dẫn không được khai báo trong provider_paths.xml [cite: 2546]
            Log.e(TAG, "FileProvider Configuration Error: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi cấu hình ứng dụng (FileProvider). Vui lòng kiểm tra logcat.", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            // Xử lý các lỗi liên quan đến quyền truy cập tệp
            Log.e(TAG, "Security Error opening export folder: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi bảo mật khi mở thư mục. Hãy kiểm tra quyền truy cập tệp.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Xử lý các lỗi chung khác
            Log.e(TAG, "Unexpected Error opening export folder: " + e.getMessage(), e);
            Toast.makeText(this, "Đã xảy ra lỗi không mong muốn: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void showDeleteTablesConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa bảng")
                .setMessage("Bạn có chắc chắn muốn xóa toàn bộ dữ liệu từ tất cả các bảng (2KT và 3KT, Custom)? Thao tác này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelperThuoc.deleteAllRecords(selectedCrawlType.getTableThuocName());
                    Toast.makeText(MainActivity.this, "Đã xóa toàn bộ dữ liệu.", Toast.LENGTH_SHORT).show();
                    // Reset UI after deletion if necessary
//                    updateProgressUI(null); // Reset progress display
//                    viewModelCrawlerGemini.loadLastWorkState(); // Refresh WorkState
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDeleteDuplicateRecordsConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa bản ghi")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả các bản ghi có URL trùng lặp (Status = 1)?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int deletedRows = dbHelperThuoc.deleteDuplicateRecords(selectedCrawlType.getTableThuocName());
                    Toast.makeText(MainActivity.this, "Đã xóa " + deletedRows + " bản ghi trùng lặp.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Updates the enabled/disabled state of various UI elements based on the crawling status.
     * @param isCrawling True if a crawl is in progress, false otherwise.
     */
    private void updateUIState(boolean isCrawling) {    //BẬT CÁC BUTTON: isCrawling = fasle

        Log.d(TAG, "DEBUG_UI: updateUIState called with disableButtons = " + isCrawling);


        runOnUiThread(() -> {
            // Mã chạy trên main thread (UI thread)
            // Ví dụ: Cập nhật TextView, hiển thị Toast, v.v.
            // Buttons related to starting/stopping crawl
            binding.pbCrawlerProgress.setVisibility(View.VISIBLE);  // Nếu thiết kế ban đâu là inVisible
            // Vì dùng btnCrawlData có 2 chức năng nên bỏ SET btnCrawlData VÀ btnCancelCrawler
            //binding.btnCrawlData.setEnabled(!isCrawling);
            //Log.d(TAG, "updateUIState: btnCrawlData.setEnabled = " + !isCrawling);
            //binding.btnCancelCrawler.setEnabled(isCrawling);
            //Log.d(TAG, "updateUIState: btnCrawlData.setEnabled = " + !isCrawling);
            //
            binding.btnExportExcel.setEnabled((!isCrawling));   //Bật: isCrawling = false
            binding.btnSqlDelete.setEnabled((!isCrawling));

            // K0, K1 EditTexts
            binding.editNumFromK0.setEnabled(!isCrawling);
            binding.editNumToK1.setEnabled(!isCrawling);

            // Crawl Type selection (RadioGroup and Custom String EditText)
//        binding.radioGroupCrawlType.setEnabled(!isCrawling);
//        for (int i = 0; i < binding.radioGroupCrawlType.getChildCount(); i++) {
//            View child = binding.radioGroupCrawlType.getChildAt(i);
//            if (child instanceof RadioButton) {
//                child.setEnabled(!isCrawling);
//            }
//        }
//        binding.etCustomCrawlString.setEnabled(!isCrawling);

            // Spinner for Excel files
            //binding.spinnerSelectExcelFile.setEnabled(!isCrawling);   // Bỏ lệnh: cho chọn

            // Export and Database buttons
            binding.btnExportExcel.setEnabled(!isCrawling);
            //binding.btnExportXml.setEnabled(!isCrawling);
            //binding.btnOpenFolder.setEnabled(!isCrawling);
            //binding.btnDeleteTables.setEnabled(!isCrawling);
            binding.btnSqlDelete.setEnabled(!isCrawling);

            // Menu item for settings: SẼ LÀM disable GIẢ
//            if (settingsMenuItem != null) {
//                //SẼ LÀM disable GIẢ
//                settingsMenuItem.setEnabled(!isCrawling);   // Làm setting không phản hồi khi nhấn
//            }
        });

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        settingsMenuItem = menu.findItem(R.id.action_settings); // Save reference
//        // Update its state based on current crawling status (important for initial load)
//        if (viewModelCrawlerGemini.getLiveWorkState().getValue() != null) {
//            UUID workerId = null;
//            try {
//                workerId = UUID.fromString(viewModelCrawlerGemini.getLiveWorkState().getValue().getWorkId());
//            } catch (IllegalArgumentException e) {
//                Log.e(TAG, "Invalid UUID string in WorkState during menu creation: " + viewModelCrawlerGemini.getLiveWorkState().getValue().getWorkId());
//            }
//            if (workerId != null && viewModelCrawlerGemini.isWorkerRunning(workerId)) {
//                updateUIState(true); // If worker is running, disable menu
//            } else {
//                updateUIState(false); // Otherwise, enable menu
//            }
//        } else {
//            updateUIState(false); // Enable UI if LiveWorkState is null (no active work)
//        }
//        return true;
//    }

    @Override
    protected void onResume() {
        super.onResume();

        //updateProgressUI();
        //loadExcelFileListSpinner();
        //totalUrlsToCrawl là k1-k0: của một đợt crawl, KHÔNG PHẢI TỔNG RECORD CỦA TABBLE_2KT (2KT)
        //long totalUrlsToCrawl = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
        selectedCrawlType = settingsRepository.getSelectedCrawlType();  // phải lây lại nếu đã chọn lai trên SettingsActivity
        int totalUrlsToCrawl = k1-k0;    //k1>k0
        long crawledUrlStar1Count =settingsRepository.getCrawledUrlsStart1Count(selectedCrawlType.getCrawledUrlStart1sCountPrefKey());
        long crawledUrlsCount =settingsRepository.getSumCrawledUrlsCount(selectedCrawlType.getCrawledUrlsCountPrefKey());
        //String urlCurrent = settingsRepository.g          // chưa có viết, cần bổ sung

        // Tính percent
        //int percent = (processedUrlStar1Count*100)/ totalUrl;
        int percent = (int) (crawledUrlStar1Count * 100.0 / totalUrlsToCrawl); //totalUrlsToCrawl !=0
        binding.progressPercentText.setText(percent + "%");
        binding.pbCrawlerProgress.setProgress(percent);
        long elapsedTime = settingsRepository.getLongElapsedTime(selectedCrawlType);
        //long remainingTime = settingsRepository.getR(selectedCrawlType);  // chưa có viết, cần bổ sung

        // set lại giao diện
        //binding.tvSumProcessedUrls =
        //binding.tvSumProcessedUrls.setText(String.format("Tổng đã xử lý: %d", crawledUrlsCount));
        Utils.debugSetText(binding.tvSumProcessedUrls, String.format("Tổng đã xử lý: %d", crawledUrlsCount)); // THEO DÕI
        binding.tvSoUrlTrenTotalUrl.setText(String.format("%d/%d", crawledUrlStar1Count, totalUrlsToCrawl));
        //binding.tvInfoUrl.setText(urlCurrent);    //cần bổ sung
        binding.tvElapsedTimes.setText(String.format("Thời gian trôi qua: %s", DateUtils.formatElapsedTime(elapsedTime/1000)));
        //binding.tvTimeRemaining.setText(String.format("Thời gian còn lại: %s", formatTime(remainingTime))); // // chưa có viết, cần bổ sung,Hoàn thành thì = 0

        // Load settings again in onResume to reflect any changes from SettingsActivity or system behavior
        // Kiểm tra flag để tránh chạy lại loadAndDisplayCrawlType ngay sau onCreate
        if (isInitialLoadSettings) {
            isInitialLoadSettings = false; // Đặt lại flag sau lần tải ban đầu
        } else {
            //updateRecordCount();
            long count = settingsRepository.getTotalUrls(selectedCrawlType.getTotalUrlsPrefKey());
            //binding.tvTotalRecords.setText(getString(R.string.total_records_display, count));   //Tổng số Urls crawl
            binding.tvTotalRecords.setText(R.string.tvTotalRecords);
            // Khi không phải là lần khởi tạo ban đầu, thì luôn load để cập nhật
            loadSettings(); // Load cài đặt K0, K1, CrawlType, cancelledWorker ĐÃ LƯU khi khởi động app
            // Re-check worker state in onResume to ensure UI is consistent
            //checkAndObserveWorkerState(); // TẠM BỎ
            // Refresh Excel files list in case new files were exported
            //loadExcelFilesIntoSpinner();    // Không liên quan thông số chọn lưa từ activity_settings
            loadExcelFileListSpinner();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clear binding reference
//        if (dbHelperThuoc != null) {
//            dbHelperThuoc.close();
//        }
        // ĐẢM BẢO TẮT excelExecutor KHI ACTIVITY BỊ HỦY: Shut down the executor to prevent memory leaks
        if (excelExecutor != null) {        // && !excelExecutor.isShutdown()) {
            excelExecutor.shutdownNow(); // Dừng ExecutorService khi Activity bị hủy để tránh rò rỉ bộ nhớ
            try {
                // Chờ các tác vụ đang chạy hoàn thành trong tối đa 60 giây
                if (!excelExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    excelExecutor.shutdownNow(); // Buộc tắt nếu không hoàn thành
                }
            } catch (InterruptedException ex) {
                excelExecutor.shutdownNow();
                // Đặt lại trạng thái ngắt của luồng hiện tại nếu bị gián đoạn
                Thread.currentThread().interrupt();
            }

        }

        Log.d(TAG, "MainActivity.onDestroy() called");

        if (writerEngine != null) {
            writerEngine.stop(); // dừng loop
        }
        if (dbHelperThuoc != null) {
            SQLiteDatabase db = dbHelperThuoc.getDb();    //SQLiteDatabase db = dbHelperThuoc.getWritableDatabase();
            if (db.isOpen()) {
                db.close();
                Log.d(TAG, "Database closed safely.");
            }
        }

    }

    // CHỈNH SỬA: Thêm phương thức này để xử lý reset khi worker SUCCEEDED
    private void handleWorkerSuccess(CrawlType crawlType) {
        // KHÔNG KHỞI TẠO excelExecutor Ở ĐÂY NỮA, NÓ ĐÃ ĐƯỢC KHỞI TẠO Ở CẤP LỚP
        //excelExecutor = Executors.newSingleThreadExecutor(); // KHỞI TẠO NÓ Ở ĐÂY:SẼ BỊ KHỞI TẠO NHIỀU LẦN
        excelExecutor.execute(() -> {     //executorService = excelExecutor
            // 1. Xóa WorkState khỏi Room Database và trong PREFS: ĐỂ MyWorkerCrawler deleteByWorkId KHI SUCCEDED
//            workStateDao.deleteByWorkId(selectedCrawlType.getWorkIdPrefKey(), completedWorkId); //Đảm bảo workStateDao được khởi tạo và có sẵn
//            Log.d(TAG, "Deleted work state for SUCCEEDED worker: typeCrawl =" + crawlType.getDbType() +
//                    "\nWorkRequestId = " + completedWorkId);
//            //trong PREFS
//            settingsRepository.clearWorkRequestId(selectedCrawlType.getWorkIdPrefKey());
                ///
            // 2. Reset các thông số SharedPreferences
            // Reset số lượng URL start=1 đã xử lý về 0
            settingsRepository.saveCrawledUrlsStart1Count(crawlType.getCrawledUrlStart1sCountPrefKey(), 0L);
            Log.d(TAG, "Reset processedUrlsStart1CountPrefKey for " + crawlType.getDescription());

            // Reset tổng số URL đã xử lý được về 0 (nếu bạn muốn bắt đầu lại từ đầu)
            // Nếu bạn muốn tích lũy, hãy bỏ qua dòng này hoặc có logic khác
            settingsRepository.saveSumCrawledUrlsCount(crawlType.getCrawledUrlsCountPrefKey(), 0L);
            Log.d(TAG, "Reset sumProcessedUrlsCountPrefKey for " + crawlType.getDescription());

            // Reset thời gian đã trôi qua
            settingsRepository.saveLongElapsedTime(selectedCrawlType, 0L);
            Log.d(TAG, "Reset elapsedTime.");

            // 3. LÀM RỖNG BẢNG QueueTable LIÊN QUAN (ĐÃ ĐƯỢC DI CHUYỂN VÀO ĐÂY)
            dbHelperThuoc.deleteAllRecords(crawlType.getUrlQueueTableName());   // Đây là dòng quan trọng
            Log.d(TAG, "Cleared QueueTable for " + crawlType.getDescription());

            // 4. Xóa workRequestId hiện tại và cập nhật LiveData
//            workRequestId = null;
//            currentWorkRequestIdLiveData.postValue(null);
//            _isWorkRunning.postValue(false);
//            _workStatus.postValue("Hoàn thành quá trình crawl.");
//            Log.d(TAG, "WorkRequestId and isWorkRunning reset after success.");
        });
    }

    public static CrawlType getSelectedCrawlType() {
        return selectedCrawlType;
    }

    private void resetAfterFinish() {
        isCrawling = false;
        isStartingOrStopping = false;
        //binding.btnCrawlData.setText("Start Crawl");
        binding.btnCrawlData.setEnabled(true);
        updateUIState(false);
        // 2 CHỨC NĂNG
//        isCrawling = false;
//        //isStartingOrStopping chống double-click khi WorkManager chưa báo state mới.
//        isStartingOrStopping = false;   //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
//        binding.btnCrawlData.setText("Start Crawl");
//        binding.btnCrawlData.setEnabled(true);
//        updateUIState(false);
        //
        // Reset flag
        isUIRunningStateSet = false;    //isUIRunningStateSet cÓ VẺ GIỐNG isStartingOrStopping
        Log.d("STATE_TRACKER", "DEBUG_FLAG: Reset RUNNING flag (FINISHED)");
        // Tính thời gian chạy
        if (crawlerStartTimeMillis != -1) {
            long durationMillis = System.currentTimeMillis() - crawlerStartTimeMillis;
            double durationSeconds = durationMillis / 1000.0;
            Log.d("STATE_TRACKER", "⏱ Crawler hoàn tất. Thời gian chạy: " + durationSeconds + " giây");
            binding.tvCrawlerStatus.append("\nThời gian: " + durationSeconds + " giây");
            crawlerStartTimeMillis = -1;
        }

    }


} //END class MainActivity
