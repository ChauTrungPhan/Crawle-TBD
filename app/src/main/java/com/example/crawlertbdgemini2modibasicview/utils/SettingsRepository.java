package com.example.crawlertbdgemini2modibasicview.utils;

import android.content.Context;
import android.util.Log;
// Không cần import SharedPreferences trực tiếp ở đây nếu dùng SharedPreferencesUtils
//import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crawlertbdgemini2modibasicview.AppConstants; // Chắc chắn AppConstants tồn tại và chứa DEFAULT_K0, DEFAULT_K1
import com.example.crawlertbdgemini2modibasicview.AppDatabase;
import com.example.crawlertbdgemini2modibasicview.GetKyTuAZ;
import com.example.crawlertbdgemini2modibasicview.MainActivity;

import java.util.UUID;

//import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
//import com.example.crawlertbdgemini2modibasicview.utils.PrefKey; // Import PrefKey
//import com.example.crawlertbdgemini2modibasicview.utils.SharedPreferencesUtils; // Import SharedPreferencesUtils

public class SettingsRepository {

    private final Context appContext; // Sử dụng ApplicationContext để tránh rò rỉ bộ nhớ

    //(thêm)
    public static CrawlType selectedCrawlType = null;
    private final MutableLiveData<String> workRequestIdLiveData = new MutableLiveData<>();    //(thêm)

    /**
     *
     * @param context
     * selectedCrawlType: MẶC ĐỊNH LÀ "TWO_CHAR": LUÔN LUÔN CÓ, KHÔNG BAO GIỜ NULL
     */
    public SettingsRepository(Context context) {
        this.appContext = context.getApplicationContext(); // Luôn sử dụng ApplicationContext

        //this.selectedCrawlType = MainActivity.getSelectedCrawlType();
        //this.selectedCrawlType = this.getSelectedCrawlType();

        // selectedCrawlType: MẶC ĐỊNH LÀ LOẠI TYPE: "TWO_CHAR"
//        selectedCrawlType = CrawlType.fromSettingIdName(SharedPreferencesUtils.getString(appContext,
//                PrefKey.SELECTED_CRAWL_TYPE_ID_NAME , CrawlType.TWO_CHAR.getSettingIdName()));
    }

    // --- Quản lý Loại Crawl đã chọn (CrawlType) ---
    public CrawlType getSelectedCrawlType() {   // NÊN DÙNG HÀM NÀY TRÁNH biến static selectedCrawlType
//        // Lấy chuỗi ID từ SharedPreferences, nếu không có, mặc định là ID của TWO_CHAR
        String idName = SharedPreferencesUtils.getString(appContext, PrefKey.SELECTED_CRAWL_TYPE_ID_NAME , CrawlType.TWO_CHAR.getSettingIdName());
//        // Chuyển đổi chuỗi ID thành đối tượng CrawlType: NẾU idName == null thì fromSettingIdName trả về TWOCHAR
        return CrawlType.fromSettingIdName(idName);
//        return selectedCrawlType;
    }

    public void saveCrawlType(CrawlType type) { //Chỉ cần 1 loại SELECTED_CRAWL_TYPE_ID_NAME
        // Lưu chuỗi ID của CrawlType vào SharedPreferences
        SharedPreferencesUtils.saveString(appContext, PrefKey.SELECTED_CRAWL_TYPE_ID_NAME, type.getSettingIdName());
    }

    public boolean isFirstRun() {
        return SharedPreferencesUtils.getBoolean(appContext, PrefKey.FIRST_RUN, true);
    }

    public void setFirstRun(boolean isFirstRun) {
        SharedPreferencesUtils.saveBoolean(appContext, PrefKey.FIRST_RUN, isFirstRun);
    }



    // --- Quản lý giá trị K0 ---
//    public int getK0() {
//        // Lấy giá trị K0 từ SharedPreferences, mặc định là DEFAULT_K0 từ AppConstants
//        // Không cần DEFAULT_K0_SETTING
//        return SharedPreferencesUtils.getInt(appContext, PrefKey.K0_SETTING, AppConstants.DEFAULT_K0_SETTING);
//    }


    // --- Quản lý giá trị K1 ---
//    public int getK1() {
//        // Lấy giá trị mặc định dựa trên loại crawler hiện tại nếu chưa có
//        // Lấy giá trị K1 từ SharedPreferences.
//        // Giá trị mặc định ban đầu cần được tính toán cẩn thận.
//        // Sử dụng một giá trị mặc định chung trước, sau đó tính toán lại dựa trên CrawlType
//        // nếu giá trị lưu trữ chưa có hoặc không hợp lệ.
//
//        CrawlType currentType = getSelectedCrawlType();
//        int defaultK1 = currentType.getTotalKyTuCount(); // Sử dụng tổng số ký tự của loại crawler
//        // Nếu = 0 thì lấy hết giá tr chiều dài arrayKyTy
//        if (defaultK1 == 0) defaultK1 = currentType.getTotalKyTuCount();
//        //if (defaultK1 == 0) defaultK1 = AppConstants.DEFAULT_K1_SETTING; // Đảm bảo có giá trị mặc định tối thiểu
//
//        return SharedPreferencesUtils.getInt(appContext, PrefKey.K1_SETTING, defaultK1);
//    }

    public String getK0CustomString() {
        // CHƯA VIẾT

        //return SharedPreferencesUtils.getString(appContext, PrefKey.K0_CUSTOM_STRING, "");
        return "CHUA VIET XONG";
    }

//    public void saveK1(int k1) {
//        SharedPreferencesUtils.saveInt(appContext, PrefKey.K1_SETTING, k1);
//    }

    public boolean isCrawlByUrlSelected() {
        // Tên hằng số cần được cập nhật trong PrefKey nếu KEY_CRAWL_BY_URL không phải là PrefKey
        return SharedPreferencesUtils.getBoolean(appContext, PrefKey.CRAWL_BY_URL_SELECTED, true); // Mặc định là true
    }

    public void saveCrawlByUrlSelected(boolean selected) {
        SharedPreferencesUtils.saveBoolean(appContext, PrefKey.CRAWL_BY_URL_SELECTED, selected);
    }

    public long getElapsedTime() {
        return SharedPreferencesUtils.getLong(appContext, PrefKey.ELAPSED_TIME, 0);
    }

    public void saveElapsedTime(long elapsedTime) {
        SharedPreferencesUtils.saveLong(appContext, PrefKey.ELAPSED_TIME, elapsedTime);
    }
    public void saveLongElapsedTime(CrawlType crawlType, long milis) { // SỬA: Thay String key bằng PrefKey key
        if (crawlType.getDbType().equalsIgnoreCase("2KT")) {
            SharedPreferencesUtils.saveLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_2KT, milis);
        } else if (crawlType.getDbType().equalsIgnoreCase("3KT")) {
            SharedPreferencesUtils.saveLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_2KT, milis);
        } else {
            SharedPreferencesUtils.saveLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_CUSTOM, milis);
        }
    }
    public long getLongElapsedTime(CrawlType crawlType) { // SỬA: Thay String key bằng PrefKey key
        if (crawlType.getDbType().equalsIgnoreCase("2KT")) {
            return SharedPreferencesUtils.getLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_2KT, 0);
        } else if (crawlType.getDbType().equalsIgnoreCase("3KT")) {
            return SharedPreferencesUtils.getLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_2KT, 0);
        } else {
            return SharedPreferencesUtils.getLong(appContext, AppConstants.CUMULATIVE_ELAPSED_TIME_PREF_KEY_CUSTOM, 0);
        }
    }

    public void saveCrawledUrlsStart1Count(PrefKey key, long count) { // SỬA: Thay String key bằng PrefKey key
        SharedPreferencesUtils.saveLong(appContext, key, count); //PROCESSED_URLSTART1_2KT_COUNT
    }

    public long getCrawledUrlsStart1Count(PrefKey key) { // SỬA: Thay String key bằng PrefKey key
        long value = SharedPreferencesUtils.getLong(appContext, key, 0L);
        Log.d("SettingsRepository", "Key: " + key.name() + ", Value read: " + value + ", Type: long");
        //return SharedPreferencesUtils.getLong(appContext, key, 0L);
        return value;
    }

    public void saveSumCrawledUrlsCount(PrefKey key, long count) { // SỬA: Thay String key bằng PrefKey key
        SharedPreferencesUtils.saveLong(appContext, key, count);
    }

    public long getSumCrawledUrlsCount(PrefKey key) { // SỬA: Thay String key bằng PrefKey key
        return SharedPreferencesUtils.getLong(appContext, key, 0L);   //SUM_PROCESSED_URLS_2KT_COUNT
    }

    public void saveTotalUrls(PrefKey key, long totalUrls) { // SỬA: Thay String key bằng PrefKey key
        SharedPreferencesUtils.saveLong(appContext, key, totalUrls);
    }

    public long getTotalUrls(PrefKey key) { // SỬA: Thay String key bằng PrefKey key
        return SharedPreferencesUtils.getLong(appContext, key, 0L);
    }

    public void saveCancelledWorker( PrefKey key, boolean value) {
        SharedPreferencesUtils.saveBoolean(appContext, key, value);
    }

    public boolean getCancelledWorker(PrefKey key) { // SỬA: Thay String key bằng PrefKey key
        return SharedPreferencesUtils.getBoolean(appContext, key, false);
    }

    // Mới Sửa CÓ THỂ CÓ HÀM TRÙNG CHỨC NĂNG
//    public void saveWorkRequestId(String workId) {
//        SharedPreferencesUtils.saveString(appContext, PrefKey.WORK_REQUEST_ID, workId);
//        workRequestIdLiveData.postValue(workId); // Cập nhật LiveData
//    }

    /**
     * Tùy theo typyCrawl
     * @param workId
     */

    public void saveWorkRequestId(PrefKey key, String workId) {
        SharedPreferencesUtils.saveString(appContext, key, workId);
        workRequestIdLiveData.postValue(workId); // Cập nhật LiveData
    }

    public String getWorkRequestId(PrefKey key) {
        String id = SharedPreferencesUtils.getString(appContext, key, null); //typeCrawl thay co PrefKey.WORK_REQUEST_ID
        workRequestIdLiveData.postValue(id); // Cập nhật LiveData khi khởi tạo/load
        return id;
    }

    public void clearWorkRequestId(PrefKey key) {
        // NỚI
        // Clear WorkRequestId trong prefs
        SharedPreferencesUtils.remove(appContext, key.name()); // Cần thêm phương thức remove vào SharedPreferencesUtils
        // Clear WorkRequestId trong Dao
        AppDatabase database = AppDatabase.getInstance(appContext);
        String id = SharedPreferencesUtils.getString(appContext, key, null);
        if (id != null) {
            UUID idUUID = UUID.fromString(id);
            database.workStateDao().deleteByWorkId(selectedCrawlType.getWorkIdPrefKey(), idUUID); // Cũ : không phân biệt được của ai
            //database.workStateDao().deleteByTypeCrawl(key.name());
        }

        /************* CŨ KHÔNG PHÂN BIỆT ĐƯỢC ********/
        // Clear WorkRequestId trong prefs
//        SharedPreferencesUtils.remove(appContext, PrefKey.WORK_REQUEST_ID); // Cần thêm phương thức remove vào SharedPreferencesUtils
//        // Clear WorkRequestId trong Dao
//        AppDatabase database = AppDatabase.getInstance(appContext);
//        String id = SharedPreferencesUtils.getString(appContext, selectedCrawlType.getDbType(), null);
//        if (id != null) {
//            UUID idUUID = UUID.fromString(id);
//            //database.workStateDao().deleteByWorkId(idUUID); // Cũ : không phân biệt được của ai
//            database.workStateDao().deleteByTypeCrawl(idUUID);
//        }

        //
        workRequestIdLiveData.postValue(null); // Cập nhật LiveData
    }

    // Lưu K0 cho loại crawl cụ thể
    public void saveK0(PrefKey k0PrefKey, int k0) { // Sửa đổi để nhận PrefKey
        // Nếu là tùy chỉnh: radioUrlTuyChinh: vẫn save
        SharedPreferencesUtils.saveInt(appContext, k0PrefKey, k0);
    }

    // Lấy K0 cho loại crawl cụ thể
    public int getK0(PrefKey k0PrefKey) { // Sửa đổi để nhận PrefKey
        // Cung cấp giá trị mặc định nếu PrefKey không tồn tại
        if (k0PrefKey != PrefKey.K0_CUSTOM) {
            //Lấy nếu: k0PrefKey == PrefKey.K0_2KT || k0PrefKey == PrefKey.K0_3KT
            return SharedPreferencesUtils.getInt(appContext, k0PrefKey, 0); // Mặc định K0 cho 2KT là 0
//        } else  {   //là PrefKey.K0_CUSTOM: chưa viết
//            return SharedPreferencesUtils.getInt(appContext, k0PrefKey, 0); // Mặc định K0 cho 3KT 0
        }
        return 0; // Giá trị mặc định chung nếu PrefKey không khớp
    }

    // Lưu K1 cho loại crawl cụ thể
    public void saveK1(PrefKey k1PrefKey, int k1) { // Sửa đổi để nhận PrefKey
        // Lấy giá trị mặc định dựa trên loại crawler hiện tại nếu chưa có
        // Lấy giá trị K1 từ SharedPreferences.
        // Giá trị mặc định ban đầu cần được tính toán cẩn thận.
        // Sử dụng một giá trị mặc định chung trước, sau đó tính toán lại dựa trên CrawlType
        // nếu giá trị lưu trữ chưa có hoặc không hợp lệ.

            // Không cần DEFAULT_K1_SETTING
            //int defaultK1 = crawlType.getTotalKyTuCount(); // Sử dụng tổng số ký tự của loại crawler
            //if (defaultK1 == 0)
            //    defaultK1 = AppConstants.DEFAULT_K1_SETTING; // Đảm bảo có giá trị mặc định tối thiểu

            SharedPreferencesUtils.saveInt(appContext, k1PrefKey, k1);

    }

    // Lấy K1 cho loại crawl cụ thể
    public int getK1(PrefKey k1PrefKey, String custom) { // Sửa đổi để nhận PrefKey
        if (selectedCrawlType != CrawlType.CUSTOM_STRING) {
            return SharedPreferencesUtils.getInt(appContext, k1PrefKey, GetKyTuAZ.getLengthKyTuAZ(k1PrefKey)); // Mặc định K1 cho 2KT là max index của mảng
        } else if (!custom.isEmpty()){
            return SharedPreferencesUtils.getInt(appContext, k1PrefKey, custom.split(",").length); // Mặc định K1 cho 2KT là max index của mảng
        }

        // Cung cấp giá trị mặc định tùy thuộc vào PrefKey
//        if (k1PrefKey == PrefKey.K1_2KT || k1PrefKey == PrefKey.K1_3KT) {
//            return SharedPreferencesUtils.getInt(appContext, k1PrefKey, GetKyTuAZ.getLengthKyTuAZ(k1PrefKey)); // Mặc định K1 cho 2KT là max index của mảng
//        } else if (!custom.isEmpty()){
//            return SharedPreferencesUtils.getInt(appContext,k1PrefKey, custom.split(",").length); // Mặc định K1 cho 3KT là max index của mảng
//        }
        return 0; // Giá trị mặc định chung


        // Lấy giá trị mặc định dựa trên loại crawler hiện tại nếu chưa có
        // Lấy giá trị K1 từ SharedPreferences.
        // Giá trị mặc định ban đầu cần được tính toán cẩn thận.
        // Sử dụng một giá trị mặc định chung trước, sau đó tính toán lại dựa trên CrawlType
        // nếu giá trị lưu trữ chưa có hoặc không hợp lệ.

        // Cung cấp giá trị mặc định nếu chưa có
        // Nếu là tùy chỉnh: radioUrlTuyChinh: vẫn lấy
        //return SharedPreferencesUtils.getInt(appContext, crawlType.getK1PrefKey(), crawlType.getTotalKyTuCount());
        //return crawlType.getTotalKyTuCount();
        // Lấy giá trị K1 từ SharedPreferences: NẾU CÓ LƯU TRONG PREFS
        //return SharedPreferencesUtils.getInt(appContext, crawlType.getK1PrefKey(), 0);
        // Không cần DEFAULT_K1_SETTING
        //return SharedPreferencesUtils.getInt(appContext, crawlType.getK1PrefKey(), AppConstants.DEFAULT_K1_SETTING);    // Không cần DEFAULT_K1_SETTING
    }

    // Phương thức để lưu và lấy chuỗi tùy chỉnh
    public void saveCustomCrawlString(String customString) {
//        SharedPreferencesUtils.getPrefs(appContext).edit()
//                .putString(AppConstants.KEY_CUSTOM_CRAWL_STRING, customString).apply();
        //prefs.edit().putString(AppConstants.KEY_CUSTOM_CRAWL_STRING, customString).apply();
        //SharedPreferencesUtils.saveString(appContext, , customString).apply();
        SharedPreferencesUtils.saveString(appContext, PrefKey.CUSTOM_CRAWL_STRING, customString);
    }

    public String getCustomCrawlString() {  // Bỏ chn k0, k1
        //return prefs.getString(AppConstants.KEY_CUSTOM_CRAWL_STRING, "");
        return SharedPreferencesUtils.getString(appContext, PrefKey.CUSTOM_CRAWL_STRING,""); // Mặc định K1 cho 3KT là max index của mảng
        //return SharedPreferencesUtils.getString(appContext, selectedCrawlType.getDbType(),""); // Mặc định K1 cho 3KT là max index của mảng
    }



    // ... (các phương thức khác)

    //Mói
//    public void saveProcessedUrlsStart1Count(CrawlType crawlType, int k1) {
//        SharedPreferencesUtils.saveInt(appContext, crawlType.getProcessedUrlsStart1CountPrefKey(), k1);
//    }

//    public long getProcessedUrlsStart1Count (CrawlType crawlType){
//        return SharedPreferencesUtils.getInt(appContext, crawlType.getProcessedUrlsStart1CountPrefKey(), 0);
//    }
//    public int getSumProcessedUrlsCount(CrawlType crawlType) {
//        return SharedPreferencesUtils.getInt(appContext, crawlType.getSumProcessedUrlsCountPrefKey(), 0);
//    }


}