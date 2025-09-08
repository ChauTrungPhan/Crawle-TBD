package com.example.crawlertbdgemini2modibasicview;

import java.util.Collections;
import java.util.List;

// --------------------- Packet ---------------------
// Gói công việc: 1 URL cha + list thuốc con
public final class Packet {
    public final UrlInfo urlInfo;                  // chỉ metadata cha (url, status, errMessage, v.v.)
    public final List<ThuocSQLite> records;   // danh sách con

    public Packet(UrlInfo urlInfo, List<ThuocSQLite> records) {
        this.urlInfo = urlInfo;
        this.records = (records == null) ? Collections.emptyList() : records;
    }

    public UrlInfo getUrlInfo() {
        return urlInfo;
    }

    public List<ThuocSQLite> getThuocList() {
        return records;
    }
}



