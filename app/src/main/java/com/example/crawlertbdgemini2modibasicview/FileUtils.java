package com.example.crawlertbdgemini2modibasicview;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    private static final String TAG = "FileUtils"; // Đổi tên TAG cho nhất quán
    private static final String FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"; // Hậu tố cho authority của FileProvider

    /**
     * Phương thức chính để lấy Uri của một file từ đường dẫn tuyệt đối.
     * Ưu tiên sử dụng MediaStore cho các file phương tiện (ảnh, video, âm thanh),
     * sau đó FileProvider cho các file khác nếu cần chia sẻ an toàn.
     *
     * @param context   Context của ứng dụng.
     * @param filePath  Đường dẫn tuyệt đối của file.
     * @return Uri của file, hoặc null nếu không tìm thấy hoặc xảy ra lỗi.
     */
    public static Uri getUriFromFilePath(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File not found at path: " + filePath);
            return null;
        }

        String mimeType = getMimeTypeFromFileName(file.getName());
        Uri uri = null;

        // 1. Thử lấy Uri từ MediaStore cho các loại file phương tiện phổ biến
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                uri = getImageContentUri(context, file);
            } else if (mimeType.startsWith("video/")) {
                uri = getVideoContentUri(context, file);
            } else if (mimeType.startsWith("audio/")) {
                uri = getAudioContentUri(context, file);
            }
        }

        // 2. Nếu không phải là file phương tiện hoặc không tìm thấy trong MediaStore,
        //    thử tìm trong MediaStore.Files hoặc sử dụng FileProvider (nếu là file riêng tư)
        if (uri == null) {
            // Thử truy vấn MediaStore chung cho các loại file khác
            uri = getGenericContentUriFromPath(context, filePath);
        }

        // 3. Nếu vẫn không có Uri và file nằm trong thư mục riêng của ứng dụng, sử dụng FileProvider
        //    (Điều này chỉ thích hợp khi bạn cần chia sẻ file riêng tư với ứng dụng khác)
        if (uri == null && filePath.startsWith(context.getFilesDir().getAbsolutePath())) {
            try {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + FILE_PROVIDER_AUTHORITY_SUFFIX, file);
                Log.d(TAG, "Uri from FileProvider: " + uri);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error getting Uri from FileProvider: " + e.getMessage());
            }
        }

        if (uri == null) {
            Log.e(TAG, "Failed to get Uri for path: " + filePath + " with any method.");
        }
        return uri;
    }

    /**
     * Lấy Uri chung từ MediaStore.Files cho bất kỳ loại file nào.
     * Sử dụng khi bạn không chắc chắn về loại file hoặc file không phải là ảnh/video/âm thanh.
     *
     * @param context  Context của ứng dụng.
     * @param filePath Đường dẫn tuyệt đối của file.
     * @return Uri của file, hoặc null nếu không tìm thấy.
     */
    private static Uri getGenericContentUriFromPath(Context context, String filePath) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        Uri uri = null;
        try {
            cursor = contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    new String[]{MediaStore.Files.FileColumns._ID},
                    MediaStore.Files.FileColumns.DATA + " = ?",
                    new String[]{filePath},
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                Log.d(TAG, "Uri from generic MediaStore query: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting generic Uri from MediaStore: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return uri;
    }

    /**
     * Lấy URI nội dung cho file ảnh từ MediaStore.
     * Nếu file chưa có trong MediaStore, nó sẽ được thêm vào.
     *
     * @param context   Context của ứng dụng.
     * @param imageFile Đối tượng File của ảnh.
     * @return Uri của ảnh, hoặc null nếu không thành công.
     */
    private static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return getExistingOrCreateContentUri(context, contentUri, filePath, imageFile, "image");
    }

    /**
     * Lấy URI nội dung cho file video từ MediaStore.
     * Nếu file chưa có trong MediaStore, nó sẽ được thêm vào.
     *
     * @param context   Context của ứng dụng.
     * @param videoFile Đối tượng File của video.
     * @return Uri của video, hoặc null nếu không thành công.
     */
    private static Uri getVideoContentUri(Context context, File videoFile) {
        String filePath = videoFile.getAbsolutePath();
        Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        return getExistingOrCreateContentUri(context, contentUri, filePath, videoFile, "video");
    }

    /**
     * Lấy URI nội dung cho file âm thanh từ MediaStore.
     * Nếu file chưa có trong MediaStore, nó sẽ được thêm vào.
     *
     * @param context   Context của ứng dụng.
     * @param audioFile Đối tượng File của âm thanh.
     * @return Uri của âm thanh, hoặc null nếu không thành công.
     */
    private static Uri getAudioContentUri(Context context, File audioFile) {
        String filePath = audioFile.getAbsolutePath();
        Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        return getExistingOrCreateContentUri(context, contentUri, filePath, audioFile, "audio");
    }

    /**
     * Phương thức chung để lấy Uri từ MediaStore, hoặc chèn mới nếu chưa tồn tại.
     *
     * @param context    Context của ứng dụng.
     * @param mediaUri   Uri của MediaStore (e.g., MediaStore.Images.Media.EXTERNAL_CONTENT_URI).
     * @param filePath   Đường dẫn tuyệt đối của file.
     * @param file       Đối tượng File.
     * @param mediaType  Loại phương tiện (dùng cho Log).
     * @return Uri của file, hoặc null.
     */
    private static Uri getExistingOrCreateContentUri(Context context, Uri mediaUri, String filePath, File file, String mediaType) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    mediaUri,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + "=? ",
                    new String[]{filePath}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                long id = cursor.getLong(idIndex);
                Log.d(TAG, "Found existing " + mediaType + " Uri for path: " + filePath);
                return Uri.withAppendedPath(mediaUri, String.valueOf(id));
            } else {
                if (file.exists()) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, filePath);
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    String mimeType = getMimeTypeFromFileName(file.getName());
                    if (mimeType != null) {
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                    }
                    Uri insertedUri = context.getContentResolver().insert(mediaUri, values);
                    if (insertedUri != null) {
                        Log.d(TAG, "Inserted new " + mediaType + " into MediaStore. Uri: " + insertedUri);
                    } else {
                        Log.e(TAG, "Failed to insert " + mediaType + " into MediaStore for path: " + filePath);
                    }
                    return insertedUri;
                } else {
                    Log.e(TAG, "File does not exist to get or create " + mediaType + " Uri: " + filePath);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting or creating " + mediaType + " Uri for path: " + filePath + ". " + e.getMessage(), e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Trả về phần mở rộng của file từ tên file.
     *
     * @param fileName Tên file đầy đủ.
     * @return Phần mở rộng (ví dụ: "txt", "jpg"), hoặc chuỗi rỗng nếu không có.
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase(); // Chuyển sang chữ thường
        }
        return "";
    }

    /**
     * Trả về MIME type của file dựa vào phần mở rộng.
     * Cần thêm các MIME type khác nếu cần.
     *
     * @param fileName Tên file.
     * @return MIME type (ví dụ: "image/jpeg", "application/pdf"), hoặc null nếu không xác định được.
     */
    public static String getMimeTypeFromFileName(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "3gp":
                return "video/3gpp";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            default:
                return null; // Hoặc "application/octet-stream" cho các loại không xác định
        }
    }

    // --- Quản lý thư mục và file cơ bản ---

    /**
     * Kiểm tra xem một file hoặc thư mục có tồn tại hay không.
     *
     * @param path Đường dẫn tuyệt đối của file/thư mục.
     * @return true nếu tồn tại, false nếu không.
     */
    public static boolean doesFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * Tạo một thư mục mới.
     *
     * @param directoryPath Đường dẫn của thư mục muốn tạo.
     * @return true nếu thư mục được tạo thành công hoặc đã tồn tại, false nếu lỗi.
     */
    public static boolean createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists()) {
            if (directory.isDirectory()) {
                Log.d(TAG, "Directory already exists: " + directoryPath);
                return true;
            } else {
                Log.e(TAG, "Path exists but is not a directory: " + directoryPath);
                return false;
            }
        }
        if (directory.mkdirs()) {
            Log.d(TAG, "Directory created: " + directoryPath);
            return true;
        } else {
            Log.e(TAG, "Failed to create directory: " + directoryPath);
            return false;
        }
    }

    /**
     * Xóa một file hoặc thư mục (bao gồm cả nội dung của thư mục).
     *
     * @param fileOrDirectory Đối tượng File của file hoặc thư mục cần xóa.
     * @return true nếu xóa thành công, false nếu lỗi.
     */
    public static boolean deleteFileOrDirectory(File fileOrDirectory) {
        if (!fileOrDirectory.exists()) {
            Log.w(TAG, "File or directory does not exist: " + fileOrDirectory.getAbsolutePath());
            return true; // Coi như đã xóa thành công nếu nó không tồn tại
        }
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteFileOrDirectory(child)) {
                        Log.e(TAG, "Failed to delete child: " + child.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        boolean deleted = fileOrDirectory.delete();
        if (deleted) {
            Log.d(TAG, "Deleted: " + fileOrDirectory.getAbsolutePath());
        } else {
            Log.e(TAG, "Failed to delete: " + fileOrDirectory.getAbsolutePath());
        }
        return deleted;
    }

    /**
     * Sao chép một file từ nguồn đến đích.
     *
     * @param sourceFile File nguồn.
     * @param destFile   File đích.
     * @return true nếu sao chép thành công, false nếu lỗi.
     */
    public static boolean copyFile(File sourceFile, File destFile) {
        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file does not exist: " + sourceFile.getAbsolutePath());
            return false;
        }
        if (destFile.isDirectory()) {
            Log.e(TAG, "Destination is a directory, not a file: " + destFile.getAbsolutePath());
            return false;
        }
        if (destFile.exists()) {
            Log.w(TAG, "Destination file already exists, overwriting: " + destFile.getAbsolutePath());
            // Có thể hỏi người dùng hoặc xử lý khác ở đây
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Log.d(TAG, "File copied from " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Di chuyển một file từ nguồn đến đích.
     *
     * @param sourceFile File nguồn.
     * @param destFile   File đích.
     * @return true nếu di chuyển thành công, false nếu lỗi.
     */
    public static boolean moveFile(File sourceFile, File destFile) {
        if (copyFile(sourceFile, destFile)) {
            if (deleteFileOrDirectory(sourceFile)) {
                Log.d(TAG, "File moved from " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                return true;
            } else {
                Log.e(TAG, "Failed to delete source file after copy: " + sourceFile.getAbsolutePath());
                return false;
            }
        }
        return false;
    }

    /**
     * Đọc nội dung file văn bản.
     *
     * @param file File cần đọc.
     * @return Nội dung file dưới dạng String, hoặc null nếu lỗi.
     */
    public static String readFileAsString(File file) {
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "File does not exist or cannot be read: " + file.getAbsolutePath());
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Ghi nội dung String vào file.
     *
     * @param file    File cần ghi.
     * @param content Nội dung String.
     * @param append  true để ghi tiếp vào cuối file, false để ghi đè.
     * @return true nếu ghi thành công, false nếu lỗi.
     */
    public static boolean writeStringToFile(File file, String content, boolean append) {
        try (FileOutputStream fos = new FileOutputStream(file, append)) {
            fos.write(content.getBytes());
            Log.d(TAG, "Content written to file: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy kích thước của một file theo byte.
     *
     * @param file Đối tượng File.
     * @return Kích thước file theo byte, hoặc -1 nếu file không tồn tại.
     */
    public static long getFileSize(File file) {
        if (file.exists()) {
            return file.length();
        }
        return -1;
    }

    /**
     * Lấy danh sách tất cả các file trong một thư mục.
     *
     * @param directoryPath Đường dẫn của thư mục.
     * @param filter        FilenameFilter để lọc file (có thể là null để lấy tất cả).
     * @return Danh sách các đối tượng File, hoặc danh sách rỗng nếu không tìm thấy hoặc lỗi.
     */
    public static List<File> getFilesInDirectory(String directoryPath, FilenameFilter filter) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Directory does not exist or is not a directory: " + directoryPath);
            return Collections.emptyList();
        }

        File[] files = (filter != null) ? directory.listFiles(filter) : directory.listFiles();
        if (files == null) {
            Log.e(TAG, "Failed to list files in directory: " + directoryPath);
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    // --- Các phương thức lưu file Excel vào thư mục công cộng (có cập nhật MediaStore) ---

    /**
     * Lưu Workbook Excel vào thư mục Downloads công cộng.
     * Quan trọng: Với Android 10 trở lên, sử dụng MediaStore API để đảm bảo file hiển thị.
     *
     * @param context  Context của ứng dụng.
     * @param wB       Workbook Excel cần lưu.
     * @param fileName Tên file (ví dụ: "my_excel_report.xlsx").
     * @return true nếu lưu thành công, false nếu lỗi.
     */
    public static boolean saveExcelToDownloads(Context context, Workbook wB, String fileName) {
        return saveExcelToPublicDirectory(context, wB, fileName, Environment.DIRECTORY_DOWNLOADS);
    }

    /**
     * Lưu Workbook Excel vào thư mục Documents công cộng.
     * Quan trọng: Với Android 10 trở lên, sử dụng MediaStore API để đảm bảo file hiển thị.
     *
     * @param context  Context của ứng dụng.
     * @param wB       Workbook Excel cần lưu.
     * @param fileName Tên file (ví dụ: "my_document.xlsx").
     * @return true nếu lưu thành công, false nếu lỗi.
     */
    public static boolean saveExcelToDocuments(Context context, Workbook wB, String fileName) {
        return saveExcelToPublicDirectory(context, wB, fileName, Environment.DIRECTORY_DOCUMENTS);
    }

    /**
     * Phương thức chung để lưu Workbook Excel vào thư mục công cộng được chỉ định.
     * Xử lý lưu trữ bằng FileOutputStream và cập nhật MediaStore.
     *
     * @param context         Context của ứng dụng.
     * @param wB              Workbook Excel cần lưu.
     * @param fileName        Tên file (ví dụ: "report.xlsx").
     * @param publicDirectory Hằng số thư mục công cộng (e.g., Environment.DIRECTORY_DOWNLOADS).
     * @return true nếu lưu thành công, false nếu lỗi.
     */
    private static boolean saveExcelToPublicDirectory(Context context, Workbook wB, String fileName, String publicDirectory) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        OutputStream fos = null;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel"); // Hoặc "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" cho .xlsx
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, publicDirectory);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Với Android 10 (Q) trở lên, sử dụng MediaStore để ghi trực tiếp
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Log.e(TAG, "Failed to create new MediaStore entry for " + fileName + " in " + publicDirectory);
                    return false;
                }
                fos = resolver.openOutputStream(uri);
            } else {
                // Với Android dưới 10, sử dụng FileOutputStream truyền thống
                File downloadsDir = Environment.getExternalStoragePublicDirectory(publicDirectory);
                if (!createDirectory(downloadsDir.getAbsolutePath())) { // Đảm bảo thư mục tồn tại
                    return false;
                }
                File file = new File(downloadsDir, fileName);
                fos = new FileOutputStream(file);
                // Sau khi ghi xong, cần quét media để file xuất hiện (không bắt buộc với MediaStore API trên Q+)
                // MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }

            if (fos != null) {
                wB.write(fos);
                Log.d(TAG, "Excel file saved to " + publicDirectory + ": " + fileName);
                return true;
            } else {
                Log.e(TAG, "OutputStream was null, cannot save file.");
                return false;
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving Excel file to " + publicDirectory + ": " + e.getMessage(), e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing OutputStream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Lấy mảng các file Excel (.xls hoặc .xlsx) trong một thư mục công cộng, sắp xếp giảm dần theo thời gian sửa đổi.
     *
     * @param publicDirectory Hằng số thư mục công cộng (e.g., Environment.DIRECTORY_DOWNLOADS).
     * @return Mảng các đối tượng File Excel, hoặc mảng rỗng nếu không tìm thấy hoặc lỗi.
     */
    public static File[] getExcelFilesInPublicDirectoryDescending(String publicDirectory) {
        File directory = Environment.getExternalStoragePublicDirectory(publicDirectory);
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Directory does not exist or is not a directory: " + publicDirectory);
            return new File[0];
        }

        File[] excelFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase();
                return lowerCaseName.endsWith(".xls") || lowerCaseName.endsWith(".xlsx");
            }
        });

        if (excelFiles == null || excelFiles.length == 0) {
            Log.d(TAG, "No Excel files found in " + publicDirectory);
            return new File[0];
        }

        Arrays.sort(excelFiles, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                // Sắp xếp giảm dần (file mới hơn lên trước)
                return Long.compare(file2.lastModified(), file1.lastModified());
            }
        });
        return excelFiles;
    }

    // --- Các phương thức hỗ trợ FileProvider ---

    /**
     * Trả về Uri từ FileProvider cho một file cụ thể.
     * Được sử dụng để chia sẻ file an toàn với các ứng dụng khác.
     *
     * @param context Context của ứng dụng.
     * @param file    File cần lấy Uri. File này thường nằm trong thư mục riêng của ứng dụng
     * (e.g., context.getFilesDir() hoặc context.getExternalFilesDir()).
     * @return Uri nội dung (content Uri) của file.
     * @throws IllegalArgumentException nếu FileProvider không được cấu hình đúng hoặc file không nằm trong đường dẫn được cho phép.
     */
    public static Uri getUriForFileWithProvider(Context context, File file) throws IllegalArgumentException {
        // "com.example.myapp.fileprovider" phải khớp với authority trong AndroidManifest.xml
        // Ví dụ: <provider android:name="androidx.core.content.FileProvider" android:authorities="com.example.myapp.fileprovider" ... />
        String authority = context.getPackageName() + FILE_PROVIDER_AUTHORITY_SUFFIX;
        Log.d(TAG, "Getting FileProvider Uri for: " + file.getAbsolutePath() + " with authority: " + authority);
        return FileProvider.getUriForFile(context, authority, file);
    }
}