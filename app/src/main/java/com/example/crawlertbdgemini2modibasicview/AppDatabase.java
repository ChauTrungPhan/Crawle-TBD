package com.example.crawlertbdgemini2modibasicview;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


// Đảm bảo bạn đã khai báo các entity và version
@Database(entities = {WorkState.class}, version = AppConstants.DATABASE_VERSION) // Tăng version từ 1 lên 2
public abstract class AppDatabase extends RoomDatabase {
    public abstract WorkStateDao workStateDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Phương thức getInstance() để lấy thể hiện của database
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, AppConstants.DATABASE_ROOM_NAME) // Thay "your_database_name" bằng tên database của bạn
                            .fallbackToDestructiveMigration() // Tùy chọn: Xử lý nâng cấp database bằng cách xóa và tạo lại
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}