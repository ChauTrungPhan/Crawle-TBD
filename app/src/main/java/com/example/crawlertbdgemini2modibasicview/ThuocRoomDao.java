package com.example.crawlertbdgemini2modibasicview;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction; // Cần thiết cho các thao tác phức tạp

import java.util.List;

@Dao
public interface ThuocRoomDao {

    // Phương thức chèn hoặc cập nhật một đối tượng Thuoc
    // Sử dụng OnConflictStrategy.REPLACE sẽ thay thế bản ghi nếu có MA_THUOC trùng lặp
    // (do MA_THUOC đã được đánh dấu UNIQUE trong Entity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(ThuocRoom thuoc);

    // Phương thức chèn nhiều đối tượng Thuoc
    // Sử dụng OnConflictStrategy.REPLACE sẽ tự động cập nhật nếu MA_THUOC trùng
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMultiple(List<ThuocRoom> thuocList);

    // Nếu bạn muốn kiểm soát chi tiết hơn việc cập nhật (ví dụ: không thay đổi created_at khi update)
    // thì vẫn cần transaction và logic thủ công hơn.
    // Ví dụ:
    @Transaction
    default void insertOrUpdateMultipleWithSpecificUpdate(List<ThuocRoom> thuocList) {
        for (ThuocRoom thuoc : thuocList) {
            // Lấy bản ghi hiện có dựa trên ma_thuoc (nếu tồn tại)
            ThuocRoom existingThuoc = getThuocByMaThuocAndLoai(thuoc.ma_thuoc, thuoc.loai_thuoc);

            if (existingThuoc != null) {
                // Cập nhật các trường mong muốn của bản ghi hiện có từ bản ghi mới
                existingThuoc.url = thuoc.url;
                existingThuoc.parent_id_of_crawled_url = thuoc.parent_id_of_crawled_url;
                existingThuoc.level_of_crawled_url = thuoc.level_of_crawled_url;
                existingThuoc.ky_tu_search = thuoc.ky_tu_search;
                existingThuoc.ten_thuoc = thuoc.ten_thuoc;
                existingThuoc.thanh_phan = thuoc.thanh_phan;
                existingThuoc.nhom_thuoc = thuoc.nhom_thuoc;
                existingThuoc.dang_thuoc = thuoc.dang_thuoc;
                existingThuoc.san_xuat = thuoc.san_xuat;
                existingThuoc.dang_ky = thuoc.dang_ky;
                existingThuoc.phan_phoi = thuoc.phan_phoi;
                existingThuoc.sdk = thuoc.sdk;
                existingThuoc.cac_thuoc = thuoc.cac_thuoc;
                existingThuoc.ghi_chu = thuoc.ghi_chu;
                existingThuoc.index_color = thuoc.index_color;
                existingThuoc.link_ky_tu_search = thuoc.link_ky_tu_search;
                existingThuoc.link_ma_thuoc = thuoc.link_ma_thuoc;
                existingThuoc.thanh_phan_link = thuoc.thanh_phan_link;
                existingThuoc.NHOM_THUOC_LINK = thuoc.NHOM_THUOC_LINK;
                existingThuoc.DANG_THUOC_LINK = thuoc.DANG_THUOC_LINK;
                existingThuoc.SAN_XUAT_LINK = thuoc.SAN_XUAT_LINK;
                existingThuoc.DANG_KY_LINK = thuoc.DANG_KY_LINK;
                existingThuoc.PHAN_PHOI_LINK = thuoc.PHAN_PHOI_LINK;
                existingThuoc.SDK_LINK = thuoc.SDK_LINK;
                existingThuoc.CAC_THUOC_LINK = thuoc.CAC_THUOC_LINK;
                existingThuoc.link_ghi_chu = thuoc.link_ghi_chu;
                existingThuoc.is_crawled = thuoc.is_crawled;
                existingThuoc.updated_at = thuoc.updated_at; // Cập nhật thời gian cập nhật

                update(existingThuoc); // Thực hiện cập nhật
            } else {
                insertOrUpdate(thuoc); // Chèn mới nếu không tồn tại
            }
        }
    }

    // Phương thức hỗ trợ cho việc cập nhật tùy chỉnh (sử dụng ID của bản ghi hiện có)
    @Update
    int update(ThuocRoom thuoc);

    // Phương thức truy vấn một thuốc dựa vào mã thuốc và loại thuốc
    @Query("SELECT * FROM thuoc_info_room WHERE ma_thuoc = :maThuoc AND loai_thuoc = :loaiThuoc LIMIT 1")
    ThuocRoom getThuocByMaThuocAndLoai(String maThuoc, String loaiThuoc);


    // Phương thức lấy số lượng bản ghi tổng (tương đương getThuocCount cho toàn bộ bảng)
    @Query("SELECT COUNT(*) FROM thuoc_info_room")
    long getTotalThuocCount();

    // Phương thức lấy số lượng bản ghi theo loại (tương đương getThuocCount cho từng bảng cũ)
    @Query("SELECT COUNT(*) FROM thuoc_info_room WHERE loai_thuoc = :loaiThuoc")
    long getThuocCountByType(String loaiThuoc);


    // Phương thức lấy ngày tạo sớm nhất hoặc muộn nhất
    @Query("SELECT MIN(created_at) FROM thuoc_info_room")
    String getMinCreatedAt();

    @Query("SELECT MAX(created_at) FROM thuoc_info_room")
    String getMaxCreatedAt();

    // Các phương thức truy vấn khác
    @Query("SELECT * FROM thuoc_info_room WHERE loai_thuoc = :loaiThuoc ORDER BY id ASC LIMIT :limit OFFSET :offset")
    List<ThuocRoom> getThuocByType(String loaiThuoc, int limit, int offset);

    @Query("SELECT * FROM thuoc_info_room WHERE ten_thuoc LIKE :searchQuery OR ma_thuoc LIKE :searchQuery")
    List<ThuocRoom> searchThuoc(String searchQuery);

    // Xóa tất cả bản ghi
    @Query("DELETE FROM thuoc_info_room")
    int deleteAllThuoc();

    // Xóa bản ghi theo loại
    @Query("DELETE FROM thuoc_info_room WHERE loai_thuoc = :loaiThuoc")
    int deleteThuocByType(String loaiThuoc);
}