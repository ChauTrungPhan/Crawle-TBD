package com.example.crawlertbdgemini2modibasicview;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class MyApplication extends Application {
    /**
     * Tôi đã đổi NOTIFICATION_CHANNEL_ID trong ví dụ thành CRAWLER_CHANNEL_ID và
     * đặt nó là public static final trong MyApplication. Điều này giúp bạn truy
     * cập ID kênh một cách nhất quán từ bất kỳ đâu trong ứng dụng của mình (ví dụ:
     * MyApplication.CRAWLER_CHANNEL_ID).
     */
    public static final String CRAWLER_CHANNEL_ID = "my_worker_crawler_channel"; // Đổi tên để rõ ràng và nhất quán

    @Override
    public void onCreate() {
        super.onCreate();
        /** Khi nào MyApplication.onCreate() được gọi?
         *
         * Phương thức onCreate() của lớp MyApplication của bạn sẽ được gọi duy nhất một lần
         * khi quá trình ứng dụng của bạn lần đầu tiên được tạo. Điều này xảy ra trước khi
         * bất kỳ Activity, Service, BroadcastReceiver hoặc ContentProvider nào của ứng dụng được khởi tạo.
         *
         * Nói cách khác:
         *      1/ Khi người dùng chạm vào icon ứng dụng để mở MainActivity.
         *      2/ Khi một Worker của bạn được kích hoạt và chạy lần đầu tiên trong một tiến trình mới.
         *      3/ Khi một BroadcastReceiver của bạn nhận được một broadcast và tiến trình ứng dụng
         *      của bạn chưa chạy.
         *
         * Đây là lý do tại sao nó là nơi lý tưởng để tạo NotificationChannel, vì bạn đảm bảo
         * rằng kênh sẽ được tạo và sẵn sàng trước khi bất kỳ thành phần nào của ứng dụng
         * cần sử dụng nó để hiển thị thông báo, đặc biệt là các Worker chạy nền.
         */
        Log.d("MyApplication", "Application onCreate called.");
        createNotificationChannel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Crawler Progress Notifications";
            String description = "Shows notifications for the web crawling process.";
            int importance = NotificationManager.IMPORTANCE_LOW; // Hoặc IMPORTANCE_DEFAULT

            NotificationChannel channel = new NotificationChannel(
                    CRAWLER_CHANNEL_ID,
                    name,
                    importance
            );
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("MyApplication", "Notification Channel '" + CRAWLER_CHANNEL_ID + "' created.");
            } else {
                Log.e("MyApplication", "NotificationManager is null, cannot create channel.");
            }
        }
    }
}
