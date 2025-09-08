package com.example.crawlertbdgemini2modibasicview;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.crawlertbdgemini2modibasicview.databinding.ActivityExportProgressBinding;
import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;

import java.util.Locale;

public class ExportProgressActivity extends AppCompatActivity {
    private ActivityExportProgressBinding binding;
    private ExportViewModel exportViewModel;
    private long exportStartTime;
    private boolean dangXuatFile = true;
    private long toTalRecords;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExportProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(binding.toolBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //Hiện nút BACK
        }

        CrawlType crawlType = new  SettingsRepository(this.getApplicationContext()).getSelectedCrawlType();
        SQLiteDatabase db = DBHelperThuoc.getInstance(this.getApplicationContext()).getReadableDatabase();
        //Cursor cursor = db.query(crawlType.getOldTableThuocName(), String[] {count(*)],)
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + crawlType.getTableThuocName(), null);
        // Di chuyển con trỏ đến hàng đầu tiên chứa kết quả
        cursor.moveToFirst();
        toTalRecords = cursor.getLong(0);
        exportViewModel = new ViewModelProvider(this).get(ExportViewModel.class);
        // Lấy dữ liệu intent truyền qua
        Intent intent = getIntent();
        String tableName = intent.getStringExtra(AppConstants.TABLE_THUOC);
        String nameFileExcel = intent.getStringExtra(AppConstants.NAME_FILE_EXCEL);
        boolean isXmlExport = intent.getBooleanExtra(AppConstants.EXPORT_TYPE, false);

        // Bắt đầu export
        exportViewModel.exportToExcel(isXmlExport, DBHelperThuoc.getInstance(this.getApplicationContext()),
                tableName, nameFileExcel);

        exportStartTime = System.currentTimeMillis();

        observeViewModel();
        // Vị Trí: cuối onCreate() (nằm trong onCreate() )
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (dangXuatFile) {
                    new AlertDialog.Builder(ExportProgressActivity.this)
                            .setTitle("Dừng tiến trình?")
                            .setMessage("Tiến trình xuất/crawl đang chạy. Bạn có chắc muốn thoát không?")
                            .setNegativeButton("Dừng và thoát", (dialog, which) -> {

                                finish();
                            })
                            .setPositiveButton("Hủy", null)
                            .show();
                } else {
                    finish();
                }
            }
        });

    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void observeViewModel() {
        // Quan sát tiến trình phần trăm
        exportViewModel.getProgressPercent().observe(this, percent -> {
            binding.progressBar.setProgress(percent);
            binding.txtPercent.setText(percent + " %");

            long elapsedMillis = System.currentTimeMillis() - exportStartTime;
            binding.txtTimePassed.setText("Đã chạy: " + formatMillisToHMS(elapsedMillis));

            if (percent > 0) {
                long estimatedTotalTime = (elapsedMillis * 100) / percent;
                long remainingTime = estimatedTotalTime - elapsedMillis;
                binding.txtTimeRemaining.setText("Còn lại: " + formatMillisToHMS(remainingTime));
            } else {
                binding.txtTimeRemaining.setText("Còn lại: --:--:--");
            }
        });

        // Quan sát log message
        exportViewModel.getMessageLog().observe(this, message -> {
            if (message.contains("Toast")) {
                Toast.makeText(getApplicationContext(), "Không có dữ liệu để xuất ra Excel!",
                        Toast.LENGTH_LONG).show();
            } else {
                binding.txtProgress.setText(message);
            }
        });
        // Quan sát thêm số record/records và mã thuốc
        exportViewModel.getRecordOnRecods().observe (this,  recordsOnTotalRecords -> {
            binding.recordTrenRecods.setText(recordsOnTotalRecords);

        });
        exportViewModel.getMaThuoc().observe (this,   mThuoc -> {
            binding.maThuoc.setText(mThuoc);
        });


        // Quan sát trạng thái hoàn tất
        exportViewModel.getIsExporting().observe(this, isExporting -> {
            if (Boolean.TRUE.equals(isExporting)) {
                binding.txtProgress.setText("🔄 Đang thực hiện...");
            } else {
                String status = exportViewModel.getExportStatus().getValue();
                if ("SUCCESS".equals(status)) {
                    dangXuatFile = false;
                    binding.txtPercent.setText("100%"); // ép 100%: do tính gần đúng
                    binding.txtProgress.setText("✅ Xuất file thành công!");
                } else if ("FAIL".equals(status)) {
                    binding.txtProgress.setText("❌ Xuất file thất bại!");
                } else {    // Các trường hợp khác
                    binding.txtProgress.setText("Đã kết thúc.");
                }
            }
        });
    }

    private String formatMillisToHMS(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }
}
