package com.example.crawlertbdgemini2modibasicview;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.crawlertbdgemini2modibasicview.utils.PrefKey;

//import org.checkerframework.common.aliasing.qual.Unique;

import java.util.UUID;

/**
 * Entity Room đại diện cho trạng thái của một công việc WorkManager.
 * Được sử dụng để lưu trữ và khôi phục trạng thái công việc.
 */
@Entity(tableName = "work_states") // SỬA: Đặt tên bảng rõ ràng hơn
public class WorkState {
    @NonNull
    @PrimaryKey
    public PrefKey workIdPrefKey;    // ID Duy nhất loại crawl: "2kt", "3kt", "custom"
    public UUID workId; // (ID duy nhất của WorkRequest)
    public String state; // Trạng thái của công việc (ví dụ: RUNNING, SUCCEEDED, FAILED)
    public long progress; // Tiến độ của công việc (ví dụ: phần trăm hoàn thành hoặc số lượng đã xử lý)
    public String message; // SỬA: Thêm trường message để lưu thông báo trạng thái
    public long startTime; // SỬA: Thêm trường thời gian bắt đầu để tính toán thời gian trôi qua

    // Constructor mặc định (Room yêu cầu)
    @Ignore
    public WorkState() {
        workIdPrefKey = null;
    }

    // Constructor để khởi tạo đối tượng WorkState
    public WorkState(@NonNull PrefKey workIdPrefKey, @NonNull UUID workId, String state, long progress, String message, long startTime) {
        this.workIdPrefKey = workIdPrefKey;
        this.workId = workId;
        this.state = state;
        this.progress = progress;
        this.message = message;
        this.startTime = startTime;
    }

    // Getters và Setters (được khuyến nghị để truy cập an toàn)

    @NonNull
    public PrefKey getWorkIdPrefKey() {
        return workIdPrefKey;
    }
    @NonNull
    public UUID getWorkId() {
        return workId;
    }

    public void setWorkId(@NonNull UUID workId) {
        this.workId = workId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}