package com.example.crawlertbdgemini2modibasicview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText; // Import EditText
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crawlertbdgemini2modibasicview.databinding.ActivitySettingsBinding;
import com.example.crawlertbdgemini2modibasicview.utils.CrawlType; // Import CrawlType
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository; // Import SettingsRepository

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding; // Khai báo binding
    private SettingsRepository settingsRepository;
    ///
    //public static String SELECTED_ID_NAME = "SELECTED_ID_NAME"; // Key để lưu/đọc
    //public static String RADIO_MACDINH_ID_2KT = "radioUrl2kt";
    // Các hằng số cho GIÁ TRỊ CỦA CÁC LOẠI CRAWL
    public static final String RADIO_VALUE_2KT = "radioUrl2kt";
    public static final String RADIO_VALUE_3KT = "radioUrl3kt";
    public static final String RADIO_VALUE_TUY_CHINH = "radioUrlTuyChinh"; // Đặt tên phù hợp với bạn
    // public static final String RADIO_MACDINH_ID_2KT; // Bạn đã có cái này rồi, hãy đảm bảo nó khớp với RADIO_VALUE_2KT
    // Ví dụ: public static final String RADIO_MACDINH_ID_2KT = RADIO_VALUE_2KT;

    private RadioGroup radioGroupCrawlType; // Khai báo RadioGroup
    //
    public static String K0 = "fromK0";
    public static String K1 = "toK1";

    //Cập nhật SettingsActivity
    //SettingsActivity.java (Sử dụng SettingsRepository và liên kết với CrawlType)
    //private EditText etCustomCrawlString; // Thêm EditText cho chuỗi tùy chỉnh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater()); // Khởi tạo binding
        setContentView(binding.getRoot()); // Đặt root view từ binding, Đảm bảo layout này chứa RadioGroup của bạn
        // Khởi tạo SettingsRepository
        settingsRepository = new SettingsRepository(this);
        /** 1. Tải giá trị CrawlType đã lưu khi Activity được tạo:
         * Khôi phục cài đặt đã chọn khi Activity được tạo, Tải cài đặt và cập nhật UI
         */
        loadSettings();

        // Thiết lập Listener cho RadioGroup để xử lý hiển thị EditText
        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            CrawlType selectedCrawlType;
            if (checkedId == binding.radioUrl2kt.getId()) {
                selectedCrawlType = CrawlType.TWO_CHAR;
                binding.etCustomCrawlString.setVisibility(View.GONE);
            } else if (checkedId == binding.radioUrl3kt.getId()) {
                selectedCrawlType = CrawlType.THREE_CHAR;
                binding.etCustomCrawlString.setVisibility(View.GONE);
            } else if (checkedId == binding.radioTuyChonKyTu.getId()) {
                selectedCrawlType = CrawlType.CUSTOM_STRING;
                binding.etCustomCrawlString.setVisibility(View.VISIBLE);
            } else {
                selectedCrawlType = CrawlType.TWO_CHAR; // Giá trị mặc định hoặc xử lý lỗi
                binding.etCustomCrawlString.setVisibility(View.GONE);
            }
            // Lưu giá trị dbType của CrawlType vào SharedPreferences
            //SharedPreferencesUtils.saveString(SettingsActivity.this, SharedPreferencesUtils.KEY_CRAWL_TYPE, selectedCrawlType.getDbType());
            settingsRepository.saveCrawlType(selectedCrawlType);
            ///
        });


        // Ánh xạ EditText
        //etCustomCrawlString = binding.etCustomCrawlString; // Đảm bảo ID này có trong activity_settings.xml


        // Xử lý sự kiện thay đổi RadioGroup
        // Thiết lập Listener cho RadioGroup để xử lý hiển thị EditText
        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Ẩn/hiện EditText dựa trên lựa chọn
            if (checkedId == binding.radioTuyChonKyTu.getId()) {
                binding.etCustomCrawlString.setVisibility(View.VISIBLE);
            } else {
                binding.etCustomCrawlString.setVisibility(View.GONE);
            }
        });

        // Xử lý sự kiện nút Save, Thiết lập Listener cho nút Save
        binding.btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        //CrawlType currentCrawlType = settingsRepository.getSelectedCrawlType();
        CrawlType currentCrawlType = settingsRepository.getSelectedCrawlType();
        Log.d("SettingsActivity", "Loaded crawl type: " + currentCrawlType.getSettingIdName());
        // Đặt RadioButton tương ứng
        int radioId = getResources().getIdentifier(currentCrawlType.getSettingIdName(), "id", getPackageName());
        if (radioId != 0) {
            binding.radioGroup.check(radioId);
        }

        // Hiển thị chuỗi tùy chỉnh nếu cần
        switch (currentCrawlType) {
            case TWO_CHAR:
                binding.radioUrl2kt.setChecked(true);

                binding.etCustomCrawlString.setVisibility(View.GONE);
                break;
            case THREE_CHAR:
                binding.radioUrl3kt.setChecked(true);

                binding.etCustomCrawlString.setVisibility(View.GONE);
                break;
            case CUSTOM_STRING:
                binding.etCustomCrawlString.setText(settingsRepository.getCustomCrawlString());
                binding.etCustomCrawlString.setVisibility(View.VISIBLE);

                binding.radioTuyChonKyTu.setChecked(true);
                break;
        }

        // Tải K0, K1 nếu có các EditText tương ứng
        // binding.etK0.setText(String.valueOf(settingsRepository.getK0()));
        // binding.etK1.setText(String.valueOf(settingsRepository.getK1()));

        // Hiển thị EditText cho chuỗi tùy chỉnh nếu CUSTOM_STRING được chọn
        if (currentCrawlType == CrawlType.CUSTOM_STRING) {
            binding.etCustomCrawlString.setVisibility(View.VISIBLE);    // <--- LỖI TẠI ĐÂY: TRUY CẬP TRỰC TIẾP etCustomCrawlString
            binding.etCustomCrawlString.setText(settingsRepository.getCustomCrawlString()); // <--- LỖI TẠI ĐÂY
        } else {
            binding.etCustomCrawlString.setVisibility(View.GONE);   // <--- LỖI TẠI ĐÂY
        }

        // Load K0 và K1 (nếu có EditText cho K0, K1 trong layout)
        // Ví dụ: binding.etK0.setText(String.valueOf(settingsRepository.getK0()));
        // binding.etK1.setText(String.valueOf(settingsRepository.getK1()));
    }

    private void saveSettings() {
        int checkedRadioId = binding.radioGroup.getCheckedRadioButtonId();
        if (checkedRadioId == -1) {
            Toast.makeText(this, "Vui lòng chọn một phương pháp crawl.", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(checkedRadioId);
        String selectedIdName = getResources().getResourceEntryName(selectedRadioButton.getId());
        CrawlType selectedCrawlType = CrawlType.fromSettingIdName(selectedIdName);
        // Hoặc dùng code sau để lấy selectedCrawlType
//        if (checkedRadioId == R.id.radioUrl2kt) {
//            selectedCrawlType = CrawlType.TWO_CHAR;
//        } else if (checkedRadioId == R.id.radioUrl3kt) {
//            selectedCrawlType = CrawlType.THREE_CHAR;
//        } else if (checkedRadioId == R.id.radioTuyChonKyTu) {
//            selectedCrawlType = CrawlType.CUSTOM_STRING;
//        } else {
//            // Mặc định hoặc xử lý lỗi nếu không có radio button nào được chọn
//            Toast.makeText(this, "Vui lòng chọn một loại Crawler.", Toast.LENGTH_SHORT).show();
//            return;
//        }
        // end Hoặc dùng code sau để lấy selectedCrawlType

        settingsRepository.saveCrawlType(selectedCrawlType);

        if (selectedCrawlType == CrawlType.CUSTOM_STRING) {
            String customString = binding.etCustomCrawlString.getText().toString().trim();
            if (customString.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập chuỗi ký tự tùy chỉnh.", Toast.LENGTH_SHORT).show();
                return;
            }
            settingsRepository.saveCustomCrawlString(customString);
        }
        // Chuyển data về MainActivity: KHÔNG CẦN NỮA, VÌ DÙNG SharePreferences
//        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
//                    intent.putExtra("SELECTED_CHECKEDID", selectedId);
//                    //TẠI SAO selectedIdName CÓ GIÁ TRỊ KHÁC GIÁ TRỊ TRONG setOnCheckedChangeListener
//                    intent.putExtra(SELECTED_ID_NAME, selectedIdName);    //PHẢI TÌM LẠI selectedIdName
//        setResult(Activity.RESULT_OK, intent);
        //CHỈ finish();
        finish();
    }

        // Lưu K0 và K1 (nếu có các EditText tương ứng)
        // try {
        //     int k0 = Integer.parseInt(binding.etK0.getText().toString());
        //     int k1 = Integer.parseInt(binding.etK1.getText().toString());
        //     settingsRepository.saveK0(k0);
        //     settingsRepository.saveK1(k1);
        // } catch (NumberFormatException e) {
        //     Toast.makeText(this, "K0 và K1 phải là số nguyên.", Toast.LENGTH_SHORT).show();
        //     return;
        // }
}