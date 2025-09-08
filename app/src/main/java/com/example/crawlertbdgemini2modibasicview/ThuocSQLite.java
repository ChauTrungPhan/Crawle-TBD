package com.example.crawlertbdgemini2modibasicview;

public class ThuocSQLite {
    // Thuộc tính liên quan đến nguồn gốc URL của thuốc
    public Integer url_id;              // (1) Đây là KHÓA NGOẠI THAM CHIẾU DẾN id của TABLE CHA
    //public long parent_id;              // (2) PARENT_ID_OF_CRAWLED_URL trong DB
    //public int level;                   // (3) LEVEL_OF_CRAWLED_URL trong DB
    //public String ky_tu_search;         // (4)  KY_TU_SEARCH. BẢNG CON ĐÃ BỎ
    // Thuộc tính dữ liệu thuốc
    public String ma_thuoc;             // (5)
    public String ma_thuoc_link;        // (6)
    public String ten_thuoc;            // (7)
    //public String ten_thuoc;_link
    public String thanh_phan;           // (8)
    public String thanh_phan_link;      // (9)
    public String nhom_thuoc;           // (10)
    public String nhom_thuoc_link;      // (11)
    public String dang_thuoc;           // (12)
    public String dang_thuoc_link;      // (13)
    public String san_xuat;             // (14)
    public String san_xuat_link;        // (15)
    public String dang_ky;              // (16)
    public String dang_ky_link;         // (17)
    public String phan_phoi;            // (18)
    public String phan_phoi_link;       // (19)
    public String sdk;                  // (20)
    public String sdk_link;             // (21)
    public String cac_thuoc;            // (22)
    public String cac_thuoc_link;       // (23)
//    public String ghi_chu;              // (24): BẢNG CON ĐÃ BỎ
//    public int index_color = 0;         // (25) : BẢNG CON ĐÃ BỎ
    public long updatedAt;              // (26): BẮT ĐẦU TỪ 1. Là timestamp


    public ThuocSQLite(){
        //index_color = -1;
    }


    // Constructor và getter/setter (nếu cần)
    // ...

}
