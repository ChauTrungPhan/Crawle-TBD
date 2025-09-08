package com.example.crawlertbdgemini2modibasicview;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

public class DisplayNotification {
    public static void displayNotification(Context context, String title, String task, boolean hasSound) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel;
            if (hasSound) {
                channel = new NotificationChannel("simplifiedcoding", "simplifiedcoding", NotificationManager.IMPORTANCE_DEFAULT); //có âm thanh
            }else {
                channel = new NotificationChannel("simplifiedcoding", "simplifiedcoding", NotificationManager.IMPORTANCE_LOW);//Trung bình không tạo ra âm thanh
            }
            //Thêm
            //channel.enableLights(true);
            //channel.setLightColor(Color.BLUE);
            //
            if (notificationManager != null) {  //them đk
                notificationManager.createNotificationChannel(channel);
            }else{
                //stopSelf();
            }
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, "simplifiedcoding")
                //.setNumber(2) //Số message
                .setContentTitle(title)
                .setContentText(task)
                .setSmallIcon(R.mipmap.ic_launcher);

        //notificationManager.notify(1, notification.build()); //Gốc = 1
        //private final int NOTIFICATION_INT_ID = 10;
        notificationManager.notify(10, notification.build()); //
    }
}