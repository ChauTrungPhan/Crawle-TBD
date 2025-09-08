package com.example.crawlertbdgemini2modibasicview;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Lớp đại diện cho một đối tượng Thuốc, được sử dụng làm Room Entity
 * để lưu trữ dữ liệu thuốc trong cơ sở dữ liệu.
 * Đã gộp 3 loại thuốc (2KT, 3KT, Custom) vào một bảng duy nhất
 * và sử dụng cột 'loai_thuoc' để phân biệt.
 */
@Entity(tableName = "thuoc_info_room", // Tên bảng chung cho tất cả các loại thuốc
        indices = {
                @Index(value = {"ma_thuoc"}, unique = true), // Đảm bảo mã thuốc là duy nhất
                @Index(value = {"loai_thuoc", "ma_thuoc"}) // Chỉ mục kết hợp để tìm kiếm hiệu quả theo loại và mã
        })

public class ThuocRoom {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Thêm cột để phân loại thuốc (2KT, 3KT, CUSTOM)
    @ColumnInfo(name = "loai_thuoc")
    @NonNull
    public String loai_thuoc = ""; // Ví dụ: "2KT", "3KT", "CUSTOM"

    // Thuộc tính liên quan đến nguồn gốc URL của thuốc
    @ColumnInfo(name = "crawled_from_url")
    @NonNull
    public String url = "";

    @ColumnInfo(name = "parent_id_of_crawled_url")
    public long parent_id_of_crawled_url;

    @ColumnInfo(name = "level_of_crawled_url")
    public int level_of_crawled_url;

    @ColumnInfo(name = "ky_tu_search")
    @NonNull
    public String ky_tu_search = "";

    // Thuộc tính dữ liệu thuốc
    @ColumnInfo(name = "ma_thuoc")
    @NonNull
    public String ma_thuoc = "";

    @ColumnInfo(name = "ten_thuoc")
    @NonNull
    public String ten_thuoc = "";

    @ColumnInfo(name = "thanh_phan")
    @NonNull
    public String thanh_phan = "";

    @ColumnInfo(name = "nhom_thuoc")
    @NonNull
    public String nhom_thuoc = "";

    @ColumnInfo(name = "dang_thuoc")
    @NonNull
    public String dang_thuoc = "";

    @ColumnInfo(name = "san_xuat")
    @NonNull
    public String san_xuat = "";

    @ColumnInfo(name = "dang_ky")
    @NonNull
    public String dang_ky = "";

    @ColumnInfo(name = "phan_phoi")
    @NonNull
    public String phan_phoi = "";

    @ColumnInfo(name = "sdk")
    @NonNull
    public String sdk = "";

    @ColumnInfo(name = "cac_thuoc")
    @NonNull
    public String cac_thuoc = "";

    @ColumnInfo(name = "ghi_chu")
    @NonNull
    public String ghi_chu = "";

    @ColumnInfo(name = "index_color")
    public int index_color;

    // Các link liên quan đến dữ liệu thuốc
    @ColumnInfo(name = "link_ky_tu_search")
    @NonNull
    public String link_ky_tu_search = "";

    @ColumnInfo(name = "link_ma_thuoc")
    @NonNull
    public String link_ma_thuoc = "";

    @ColumnInfo(name = "thanh_phan_link")
    @NonNull
    public String thanh_phan_link = "";

    @ColumnInfo(name = "NHOM_THUOC_LINK")
    @NonNull
    public String NHOM_THUOC_LINK = "";

    @ColumnInfo(name = "DANG_THUOC_LINK")
    @NonNull
    public String DANG_THUOC_LINK = "";

    @ColumnInfo(name = "SAN_XUAT_LINK")
    @NonNull
    public String SAN_XUAT_LINK = "";

    @ColumnInfo(name = "DANG_KY_LINK")
    @NonNull
    public String DANG_KY_LINK = "";

    @ColumnInfo(name = "PHAN_PHOI_LINK")
    @NonNull
    public String PHAN_PHOI_LINK = "";

    @ColumnInfo(name = "SDK_LINK")
    @NonNull
    public String SDK_LINK = "";

    @ColumnInfo(name = "CAC_THUOC_LINK")
    @NonNull
    public String CAC_THUOC_LINK = "";

    @ColumnInfo(name = "link_ghi_chu")
    @NonNull
    public String link_ghi_chu = "";

    // Các thuộc tính bổ sung để lưu trữ trạng thái crawl
    @ColumnInfo(name = "is_crawled", defaultValue = "0")
    public int is_crawled; // 0: chưa crawl, 1: đã crawl

    @ColumnInfo(name = "created_at")
    @NonNull
    public String created_at = ""; // Thời gian tạo bản ghi (dùng String để lưu timestamp/datetime)

    @ColumnInfo(name = "updated_at")
    @NonNull
    public String updated_at = ""; // Thời gian cập nhật bản ghi (nếu có)

    // Constructor mặc định (Room yêu cầu)
    public ThuocRoom() {
    }

    // Constructor đầy đủ
    public ThuocRoom(@NonNull String loai_thuoc, @NonNull String url, long parent_id_of_crawled_url, int level_of_crawled_url,
                     @NonNull String ky_tu_search, @NonNull String ma_thuoc, @NonNull String ten_thuoc,
                     @NonNull String thanh_phan, @NonNull String nhom_thuoc, @NonNull String dang_thuoc,
                     @NonNull String san_xuat, @NonNull String dang_ky, @NonNull String phan_phoi,
                     @NonNull String sdk, @NonNull String cac_thuoc, @NonNull String ghi_chu, int index_color,
                     @NonNull String link_ky_tu_search, @NonNull String link_ma_thuoc, @NonNull String thanh_phan_link,
                     @NonNull String NHOM_THUOC_LINK, @NonNull String DANG_THUOC_LINK, @NonNull String SAN_XUAT_LINK,
                     @NonNull String DANG_KY_LINK, @NonNull String PHAN_PHOI_LINK, @NonNull String SDK_LINK,
                     @NonNull String CAC_THUOC_LINK, @NonNull String link_ghi_chu, int is_crawled,
                     @NonNull String created_at, @NonNull String updated_at) {
        this.loai_thuoc = loai_thuoc;
        this.url = url;
        this.parent_id_of_crawled_url = parent_id_of_crawled_url;
        this.level_of_crawled_url = level_of_crawled_url;
        this.ky_tu_search = ky_tu_search;
        this.ma_thuoc = ma_thuoc;
        this.ten_thuoc = ten_thuoc;
        this.thanh_phan = thanh_phan;
        this.nhom_thuoc = nhom_thuoc;
        this.dang_thuoc = dang_thuoc;
        this.san_xuat = san_xuat;
        this.dang_ky = dang_ky;
        this.phan_phoi = phan_phoi;
        this.sdk = sdk;
        this.cac_thuoc = cac_thuoc;
        this.ghi_chu = ghi_chu;
        this.index_color = index_color;
        this.link_ky_tu_search = link_ky_tu_search;
        this.link_ma_thuoc = link_ma_thuoc;
        this.thanh_phan_link = thanh_phan_link;
        this.NHOM_THUOC_LINK = NHOM_THUOC_LINK;
        this.DANG_THUOC_LINK = DANG_THUOC_LINK;
        this.SAN_XUAT_LINK = SAN_XUAT_LINK;
        this.DANG_KY_LINK = DANG_KY_LINK;
        this.PHAN_PHOI_LINK = PHAN_PHOI_LINK;
        this.SDK_LINK = SDK_LINK;
        this.CAC_THUOC_LINK = CAC_THUOC_LINK;
        this.link_ghi_chu = link_ghi_chu;
        this.is_crawled = is_crawled;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    // Getters and Setters for all fields (Recommended for good practice)
    // (Omitted for brevity)
}