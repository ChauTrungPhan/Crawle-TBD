package com.example.crawlertbdgemini2modibasicview;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalState {

    // Stop toàn hệ thống
    public static final AtomicBoolean STOP =
            new AtomicBoolean(false);

    // Đếm page rỗng liên tiếp
    public static final AtomicInteger EMPTY_PAGE_COUNT =
            new AtomicInteger(0);

    // Đếm page không có thuốc mới
    public static final AtomicInteger DUPLICATE_PAGE_COUNT =
            new AtomicInteger(0);

    // Chống trùng thuốc
    public static final ConcurrentHashMap<String, Boolean> DRUG_MAP =
            new ConcurrentHashMap<>();
}
