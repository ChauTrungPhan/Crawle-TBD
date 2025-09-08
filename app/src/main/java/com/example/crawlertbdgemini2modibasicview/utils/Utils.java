package com.example.crawlertbdgemini2modibasicview.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log; // Import Log
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;

import com.example.crawlertbdgemini2modibasicview.R;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Lớp tiện ích cung cấp các hàm hỗ trợ chung cho ứng dụng,
 * bao gồm quản lý quyền, định dạng thời gian và thao tác với file.
 */
public class Utils {
    private static final String TAG = "Utils";

    // Private constructor to prevent instantiation
    private Utils() {
        // This class is not meant to be instantiated.
    }

    /// //////
    // CHỈNH SỬA: Phương thức yêu cầu quyền tự khởi chạy (có thể khác nhau tùy hãng điện thoại)
    /**
     * Yêu cầu quyền tự khởi chạy cho ứng dụng (tùy thuộc vào nhà sản xuất thiết bị).
     *
     * @param context Context của ứng dụng.
     */
    public static void askAutoStartPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+
            // Các quyền autostart thường không phải là quyền runtime mà là các cài đặt của nhà sản xuất
            // Bạn có thể mở màn hình cài đặt của ứng dụng hoặc cụ thể hơn là cài đặt autostart nếu biết intent
            try {
                Intent intent = new Intent();
                String packageName = context.getPackageName();
                // Một số hãng có intent riêng
                if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
                    intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                } else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
                    intent.setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                } else if (Build.MANUFACTURER.equalsIgnoreCase("vivo")) {
                    intent.setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AutostartActivity");
                } else {
                    // Mặc định mở cài đặt ứng dụng
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", packageName, null);
                    intent.setData(uri);
                }
                // Cách 1: Mở cài đặt tự khởi chạy: KHÔNG XÉT ĐK
                context.startActivity(intent);
                Toast.makeText(context, "Vui lòng cho phép ứng dụng tự khởi chạy trong cài đặt.", Toast.LENGTH_LONG).show();
                // Cách 2: Mở cài đặt tự khởi chạy: XÉT ĐK: TẠI SAO?
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    //Toast.makeText(context, R.string.auto_start_permission_not_available, Toast.LENGTH_LONG).show();
                    Toast.makeText(context, "No specific activity found for auto-start permission for manufacturer: " + Build.MANUFACTURER, Toast.LENGTH_LONG).show();
                    Log.w(TAG, "No specific activity found for auto-start permission for manufacturer: " + Build.MANUFACTURER);
                }
                // END Cách 2
            } catch (Exception e) {
                // Nếu không tìm thấy activity cài đặt cụ thể
                Toast.makeText(context, "Không thể mở cài đặt tự khởi chạy. Vui lòng tìm trong Cài đặt > Ứng dụng > " + context.getString(R.string.app_name) + " > Tự khởi chạy.", Toast.LENGTH_LONG).show();
                //
//                Log.e(TAG, "Error opening auto-start settings: " + e.getMessage(), e);
//                //Toast.makeText(context, R.string.error_opening_settings, Toast.LENGTH_LONG).show();
//                Toast.makeText(context, "Error opening auto-start settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // CHỈNH SỬA: Phương thức yêu cầu quyền thông báo
    public static void askNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) trở lên
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                context.startActivity(intent);
                Toast.makeText(context, "Vui lòng cho phép thông báo để nhận cập nhật tiến trình.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Phương thức để mở một file (ví dụ: file Excel)
    public static void openFile(Context context, File file) {
        if (!file.exists()) {
            Toast.makeText(context, "File không tồn tại: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Xác định loại MIME dựa trên phần mở rộng file (đơn giản hóa)
        String mimeType = getMimeType(file.getAbsolutePath());
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Cần cấp quyền đọc URI

        try {
            context.startActivity(Intent.createChooser(intent, "Mở file với"));
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở file. Vui lòng cài đặt ứng dụng phù hợp.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // CHỈNH SỬA: Phương thức chia sẻ file
    public static void shareFile(Context context, File file) {
        if (!file.exists()) {
            Toast.makeText(context, "File không tồn tại để chia sẻ: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(getMimeType(file.getAbsolutePath())); // Sử dụng MIME type phù hợp
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Cần cấp quyền đọc URI
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ file qua"));
    }

    // Helper để lấy MIME type (có thể mở rộng nếu cần)
    private static String getMimeType(String url) {
        String type = null;
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        // Mặc định cho Excel nếu không xác định được
        if (type == null && url.toLowerCase().endsWith(".xlsx")) {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (type == null) {
            type = "*/*"; // Mặc định là bất kỳ loại nào
        }
        return type;
    }

    // CHỈNH SỬA: Phương thức Dummy cho FileDateFormatter.getTimeFormatStringHHMMSS
    // Nếu bạn đã có class FileDateFormatter, hãy xóa phương thức này
    // và đảm bảo FileDateFormatter được import đúng cách.
    public static class FileDateFormatter {
        public static String getTimeFormatStringHHMMSS(long millis) {
            long seconds = (millis / 1000) % 60;
            long minutes = (millis / (1000 * 60)) % 60;
            long hours = (millis / (1000 * 60 * 60));
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
/// /////////////////////////////////////////////////////////

    /**
     * Kiểm tra xem thông báo có được bật cho ứng dụng hay không.
     *
     * @param context Context của ứng dụng.
     * @return true nếu thông báo được bật, false nếu ngược lại.
     */
    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Mở cài đặt thông báo của ứng dụng.
     *
     * @param context Context của ứng dụng.
     */
    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening notification settings: " + e.getMessage(), e);
            //Toast.makeText(context, R.string.error_opening_settings, Toast.LENGTH_LONG).show();
            Toast.makeText(context, "Error opening notification settings: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // File & Thư mục
    // Phương thức chung để lấy thư mục xuất

    /**
     * Đây là thư mục riêng tư của ứng dụng trên bộ nhớ ngoài (ví dụ: thẻ SD hoặc bộ nhớ lưu trữ chung của thiết bị).
     * Không cần bất kỳ quyền nào để đọc, ghi hoặc tạo file trong thư mục này trên Android 4.4 (API 19) trở lên. Đối với các phiên bản Android cũ hơn (API &lt; 19), vẫn có thể cần WRITE_EXTERNAL_STORAGE. Tuy nhiên, với hầu hết các ứng dụng hiện đại nhắm mục tiêu API 29+ thì quyền này không cần thiết cho thư mục này.
     * Các file trong thư mục này chỉ có thể được truy cập bởi ứng dụng của bạn. Các ứng dụng khác có thể truy cập nếu có quyền READ_EXTERNAL_STORAGE nhưng điều này không được khuyến khích và không được đảm bảo trên các phiên bản Android mới.
     * Dữ liệu trong thư mục này cũng sẽ bị xóa hoàn toàn khi ứng dụng của bạn được gỡ cài đặt.
     * Lợi ích là có thể sử dụng không gian lưu trữ lớn hơn của bộ nhớ ngoài.
     *
     * @param context
     * @return File
     */
    public static File getExternalFilesDirThuocBietDuoc(Context context, boolean folderChild) {
        // Đường dẫn ví dụ:: /sdcard/Android/data/your.package.name/files/Documents/ (trên bộ nhớ ngoài)
        // Lấy thư mục Documents dành riêng cho ứng dụng trên bộ nhớ ngoài
        File appSpecificDocumentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // Tạo thư mục con "ThuocBietDuocCrawler" bên trong đó
        File exportDir = null;
        if (folderChild) {
            exportDir = new File(appSpecificDocumentsDir, "ThuocBietDuoc");
            if (!exportDir.exists()) {
                exportDir.mkdirs(); // Tạo thư mục nếu nó chưa tồn tại
            }
            return exportDir;
        } else {
            return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }

    }

    /**
     * Mở một file Excel bằng ứng dụng mặc định.
     *
     * @param context Context của ứng dụng.
     * @param file Đối tượng File cần mở.
     * @param isXmlExport true nếu file là định dạng XML Excel, false nếu là XLSX.
     */
    public static void openFile(Context context, File file, boolean isXmlExport) { // SỬA: Thêm tham số isXmlExport
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        String mimeType = isXmlExport ? "application/xml" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        intent.setDataAndType(uri, mimeType);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // SỬA: Thêm cờ này để tránh lỗi khi gọi từ non-activity context

        try {
            //context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file_with)));
            context.startActivity(Intent.createChooser(intent, "Mở file với"));

        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            //Toast.makeText(context, context.getString(R.string.no_app_to_open_file), Toast.LENGTH_SHORT).show();
            Toast.makeText(context, "Không thể mở file. Vui lòng cài đặt ứng dụng phù hợp.", Toast.LENGTH_LONG).show();
            //e.printStackTrace();
        }
    }

    /**
     * Định dạng thời gian từ milliseconds sang định dạng HH:MM:SS.
     *
     * @param milliseconds Thời gian tính bằng milliseconds.
     * @return Chuỗi thời gian định dạng HH:MM:SS.
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            milliseconds = 0; // Đảm bảo không có giá trị âm
        }
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Lấy danh sách các file Excel trong thư mục được chỉ định, sắp xếp giảm dần theo ngày sửa đổi.
     * LƯU Ý: Environment.getExternalStoragePublicDirectory() đã deprecated từ API 29.
     * Cần cân nhắc sử dụng Context.getExternalFilesDir() hoặc MediaStore API cho Android 10+.
     *
     * @param context Context của ứng dụng (để truy cập getExternalFilesDir)
     * @param directoryType Loại thư mục (ví dụ: Environment.DIRECTORY_DOCUMENTS)
     * @return Mảng các file Excel, hoặc mảng rỗng nếu không tìm thấy hoặc có lỗi.
     */
    public static File[] GetArrayExcelFilesDescending(Context context, String directoryType) { // SỬA: Thêm Context
        File storageDir;

        // CÁCH CŨ (DEPRECATED):
        // storageDir = Environment.getExternalStoragePublicDirectory(directoryType);

        // CÁCH MỚI (Khuyến nghị cho Scoped Storage):
        // Để lưu các file riêng của ứng dụng vào Documents:
        storageDir = context.getExternalFilesDir(directoryType);

        // Nếu bạn muốn lưu vào thư mục chung (public storage) và nhắm đến Android 10+
        // bạn sẽ cần sử dụng MediaStore API. Việc này phức tạp hơn và nằm ngoài phạm vi tối ưu hóa nhanh này.
        // Đối với ví dụ này, tôi sẽ sử dụng getExternalFilesDir() như một giải pháp thay thế tạm thời.

        if (storageDir == null || !storageDir.exists() || !storageDir.isDirectory()) {
            Log.e(TAG, "Storage directory not found or not accessible: " + (storageDir != null ? storageDir.getAbsolutePath() : "null"));
            return new File[0];
        }

        File[] excelFiles = storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xls") || name.endsWith(".xlsx");
            }
        });

        if (excelFiles == null) {
            Log.w(TAG, "listFiles() returned null for directory: " + storageDir.getAbsolutePath());
            return new File[0];
        }

        Arrays.sort(excelFiles, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return Long.compare(file2.lastModified(), file1.lastModified());
            }
        });
        return excelFiles;
    }

    public static String DateTimeFormatterModern() {
            // Lấy ngày và giờ hiện tại
            LocalDateTime now = LocalDateTime.now();

            // Tạo một đối tượng DateTimeFormatter với định dạng mong muốn
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");   // Mẫu Text chuẩn của SQLITE

            // Định dạng ngày giờ thành chuỗi

        return now.format(formatter); // Ví dụ: 2025-08-26 13:05:00
    }

    public static String getFormatYYYYMMDD_HHMMSS() {
        //System.currentTimeMillis()
        // Lấy Locale mặc định của hệ thống
        //Locale defaultLocale = Locale.getDefault();
        // Định dạng ngày tháng theo Locale mặc định
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());        //Cho tên File Excel
        //strCreationTimeExcel = sdf.format(currentTimeMillis);
        return sdf.format(new Date());
    }

    // FileDateFormatter
    public static String getFileDateFormatte(File file) {
        //String filePath = "path/to/your/file.txt"; // Đường dẫn đến tệp của bạn
        //Path file = Paths.get(filePath);

        // Lấy ngày tạo
        BasicFileAttributes attributes = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                attributes = Files.readAttributes(Paths.get(file.getAbsolutePath()), BasicFileAttributes.class);
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc thuộc tính tệp: " + e.getMessage());
            // Xử lý lỗi ở đây, ví dụ:
            // - Hiển thị thông báo lỗi cho người dùng
            // - Ghi log lỗi
            // - Thoát chương trình
            return null;
        }

        Date creationDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assert attributes != null;
            creationDate = new Date(attributes.creationTime().toMillis());
        }
        String formattedCreationDate = getFormatDateDDMMYYYsuyet_HHMMSS(creationDate);
        System.out.println("Ngày tạo: " + formattedCreationDate);

        // Lấy ngày sửa đổi
        //Android
//        long lastModified = file.lastModified();
//        Date modifiedDate = new Date(lastModified);

        //Java
//        Date modifiedDate = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            modifiedDate = new Date(attributes.lastModifiedTime().toMillis());
//        }

//        String formattedModifiedDate = formatDate(modifiedDate);
//        System.out.println("Ngày sửa đổi: " + formattedModifiedDate);

        return formattedCreationDate;
    }

    private static String getFormatDateDDMMYYYsuyet_HHMMSS(Date date) {
        // Dạng: DD/MM/YYYY HH:MM:SS
        // Lấy Locale mặc định của hệ thống
        Locale defaultLocale = Locale.getDefault();
        // Định dạng ngày tháng theo Locale mặc định
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", defaultLocale);
        //SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        return formatter.format(date);
    }
    public static String getFormatDateDDMMYYYsuyet_HHMMSS(long seconds) {
        // Dạng: DD/MM/YYYY HH:MM:SS
        // Chuyển sang milliseconds
        Date date = new Date(seconds * 1000);   //milis

        // Lấy Locale mặc định của hệ thống
        //Locale defaultLocale = Locale.getDefault();
        // Định dạng ngày tháng theo Locale mặc định
        // Định dạng ngày giờ
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        //SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        return sdf.format(date);
    }
    public static String getTimeFormatStringHHMMSS(long miniSeconds){
        long seconds = miniSeconds / 1000;
        long HH = TimeUnit.SECONDS.toHours(seconds) % 24;
        long MM = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long SS = TimeUnit.SECONDS.toSeconds(seconds) % 60;
        //String strTimeInHHMMSS =
        return String.format("%02d:%02d:%02d", HH, MM, SS);
    }
    private static String formatCountdownTime(long millis) {
        if (millis < 0) millis = 0; // Đảm bảo không có giá trị âm

        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    public static void debugSetText(TextView tv, CharSequence text) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String threadName = Thread.currentThread().getName();

        // Lấy tên Activity hoặc Fragment chứa view
        //String containerName = "MainActivity";
        String containerName = "Unknown";
        Context ctx = tv.getContext();
        if (ctx != null) {
            containerName = ctx.getClass().getSimpleName();
        }

        // Lấy tên resource ID
        String viewIdName = (tv.getId() != View.NO_ID) ?
                tv.getResources().getResourceEntryName(tv.getId()) : "no_id";

        // In log camera an ninh
        Log.w("DebugSetText",
                "📸 " + time +
                        " | " + containerName +
                        " | viewId=" + viewIdName +
                        " | thread=" + threadName +
                        "\n--> setText(\"" + text + "\")" +
                        "\n" + Log.getStackTraceString(new Exception("STACK TRACE"))
        );

        tv.setText(text);
    }
    public static String getKyTuSeach(String url) {

       return "key" + url.split("key")[1].replace("+", " ");    // Phòng url có dấu "+" thay khoảng trắng
    }

    public static String getMaThuocUrlCha(String url) {
        //String maThuocP = "https://www..." +"key" + url.split("key")[1];
        return "https://www..." +"key" + url.split("key")[1];
    }
}