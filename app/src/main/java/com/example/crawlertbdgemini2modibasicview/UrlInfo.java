// Ví dụ: Tạo một file mới UrlInfo.java trong thư mục utils hoặc models
package com.example.crawlertbdgemini2modibasicview; // Đặt package phù hợp

import org.w3c.dom.Text;

import java.util.Date;

public class UrlInfo {
    private final String url;
    private final long parentId; // ID của URL cha trong bảng URL_QUEUE
    private final int level;
    //private final String kyTuSearch;
    private String maThuoc_P;
    //private final String maThuocLink_P;

    private int status;
    private String ghiChu;  // Ghi chu = errMessage
    private int indexColor;
    // checkpoint: lưu record index cuối cùng đã xử lý
    private int lastRecordIndex;
    private long last_attempt_at;

    // Constructor đầy đủ
    public UrlInfo(String url, long parentId, int level, String maThuocP, int status, String ghiChu, int indexColor, int lastRecordIndex, long lastAttemptAt) {
        this.url = url; // Tương đương: maThuocLinkP
        this.parentId = parentId;
        this.level = level;
        //this.kyTuSearch = kyTuSearch != null?kyTuSearch:""; // Sẽ lấy từ url
        this.maThuoc_P = maThuocP;
        //this.maThuocLink_P = maThuocLinkP;   // Là url

        this.status = status;
        this.ghiChu = ghiChu;   // Là ghi chú = errMessage
        this.indexColor = indexColor;
        this.lastRecordIndex = lastRecordIndex;
        this.last_attempt_at = lastAttemptAt;
    }

    // Getters and Setter
    public int getLastRecordIndex() { return lastRecordIndex; }
    public void setLastRecordIndex(int lastRecordIndex) { this.lastRecordIndex = lastRecordIndex; }

    public String getUrl() { return url; }

    public long getParentId() { return parentId; }
    public int getLevel() { return level; }
    //public String getKyTuSearch(){ return kyTuSearch; }
    public String getMaThuoc_P(){ return maThuoc_P; }
    public void setMaThuoc_P(String maThuocP){ this.maThuoc_P = maThuocP; }
//    public String getMaThuocLink_P() { return maThuocLink_P; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String errMessage) { this.ghiChu = errMessage; }
    public int getIndexColor() {
        return indexColor;
    }
    public long getLast_attempt_at() {
        return last_attempt_at;
    }

    public void setIndexColor(int indexColor) {
        this.indexColor = indexColor;
    }


    // Bạn có thể thêm setters nếu cần, nhưng thường thì các đối tượng dữ liệu như này nên là immutable.
    // Hoặc thêm phương thức equals() và hashCode() nếu bạn muốn sử dụng UrlInfo trong Set
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlInfo urlInfo = (UrlInfo) o;
        return url.equals(urlInfo.url); // Chỉ so sánh dựa trên URL để tránh trùng lặp
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}