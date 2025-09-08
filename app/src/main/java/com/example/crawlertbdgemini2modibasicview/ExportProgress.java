package com.example.crawlertbdgemini2modibasicview;

public class ExportProgress {
    public final int percent;
    public final String message; // Ví dụ: "Đã xử lý X/Y bản ghi"
    public final long elapsedTime;
    public final long remainingTime;

    public ExportProgress(int percent, String message, long elapsedTime, long remainingTime) {
        this.percent = percent;
        this.message = message;
        this.elapsedTime = elapsedTime;
        this.remainingTime = remainingTime;
    }
}