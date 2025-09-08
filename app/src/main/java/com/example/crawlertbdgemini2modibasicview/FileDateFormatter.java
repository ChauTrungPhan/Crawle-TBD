package com.example.crawlertbdgemini2modibasicview;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FileDateFormatter {

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
        String formattedCreationDate = formatDate(creationDate);
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

    private static String formatDate(Date date) {
        // Lấy Locale mặc định của hệ thống
        Locale defaultLocale = Locale.getDefault();
        // Định dạng ngày tháng theo Locale mặc định
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", defaultLocale);
        //SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        return formatter.format(date);
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

}
