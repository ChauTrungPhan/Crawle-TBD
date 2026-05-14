# TÀI LIỆU TỔNG HỢP: TỐI ƯU EXCEL EXPORTER (QUAN HỆ CHA-CON)

Tài liệu này tổng hợp các lưu ý quan trọng và mã nguồn đã tối ưu cho module xuất dữ liệu Excel trong ứng dụng Crawler TBD.

## 1. Sửa lỗi cú pháp và Cảnh báo Java cơ bản

### 1.1. Lỗi khởi tạo `ArrayList` và `NullPointerException`
*   **Vấn đề:** Khai báo `ArrayList<Integer> colCha;` mà không khởi tạo sẽ gây crash khi gọi `.add()`.
*   **Giải pháp:** Luôn khởi tạo ngay khi khai báo:
    ```java
    ArrayList<Integer> colCha = new ArrayList<>();
    ```

### 1.2. Phân biệt `Array` (Reflection) và Mảng thông thường
*   **Lưu ý:** Tránh dùng `java.lang.reflect.Array` cho logic thông thường.
*   **Khuyên dùng:** Sử dụng `ArrayList<Integer>` cho danh sách cột linh hoạt hoặc `int[]` cho hiệu năng cao.

### 1.3. Cảnh báo "Contents of collection are updated, but never queried"
*   **Giải pháp:** Đã xử lý bằng cách duyệt qua `colCha` trong hàm `writeRowCha` để lấy dữ liệu ghi vào Excel.

---

## 2. Logic xử lý Quan hệ Cha-Con trong Excel

### 2.1. Đảm bảo độ rộng hàng nhất quán (Padding)
*   **Yêu cầu:** Hàng Cha thường ít cột hơn hàng Con, nhưng file Excel cần độ dài hàng bằng nhau.
*   **Giải pháp:** 
    1. Duyệt ghi các cột của Cha từ `colCha`.
    2. Dùng vòng lặp `while` ghi thêm **Cell trống** cho đến khi đạt kích thước `EXPORT_COLUMNS.size()`.

### 2.2. Kiểm tra an toàn `dbIdx != -1`
*   **Mục đích:** `cursor.getColumnIndex()` trả về `-1` nếu cột thiếu. Kiểm tra này giúp tránh lỗi Crash ứng dụng.

---

## 3. Thống nhất chỉ số hàng (rowE) và `getCellRef`

### 3.1. Cải tiến hàm `getCellRef`
Hàm đã được sửa để nhận vào số hàng **giữ nguyên giá trị** (1-based index):
```java
private static String getCellRef(int colIndex, int rowIndex) {
    StringBuilder sb = new StringBuilder();
    int current = colIndex;
    while (current >= 0) {
        sb.insert(0, (char) ('A' + (current % 26)));
        current = (current / 26) - 1;
    }
    sb.append(rowIndex); // Đã loại bỏ việc cộng 1 bên trong
    return sb.toString();
}
```

### 3.2. Quy tắc sử dụng `rowE`
*   Headers chiếm dòng **1 đến 5**.
*   Dữ liệu bắt đầu từ `rowE = 6`.
*   Gọi `getCellRef(col, rowE)` trực tiếp, **không trừ 1**.
*   Tăng `rowE++` sau mỗi hàng.

---

## 4. Đoạn mã mẫu chuẩn cho `writeRowCha`

```java
private static void writeRowCha(StringBuilder sheetContent, Cursor cursor, int rowE,
                                ArrayList<Integer> colCha, Map<String, Integer> sharedStringsMap,
                                List<String> sharedStringList, List<String> hyperlinks,
                                List<String> hyperlinkCellRefs, int rawColor) {

    sheetContent.append("<row r=\"").append(rowE).append("\">\n");

    int totalTargetCols = DBHelperThuoc.EXPORT_COLUMNS.size();
    int excelColIndex = 0;

    // 1. Ghi các cột của Cha dựa trên colCha
    for (Integer dbIdx : colCha) {
        String value = (dbIdx != -1 && !cursor.isNull(dbIdx)) ? cursor.getString(dbIdx) : "";
        String link = (value.startsWith("http")) ? value : null;

        String cellRef = getCellRef(excelColIndex, rowE);
        writeCell(sheetContent, cellRef, value, link, sharedStringsMap, sharedStringList, 
                  hyperlinks, hyperlinkCellRefs, rawColor);
        excelColIndex++;
    }

    // 2. Bù ô trống cho bằng hàng Con (Padding)
    while (excelColIndex < totalTargetCols) {
        String cellRef = getCellRef(excelColIndex, rowE);
        writeCell(sheetContent, cellRef, "", null, sharedStringsMap, sharedStringList, 
                  hyperlinks, hyperlinkCellRefs, rawColor);
        excelColIndex++;
    }

    sheetContent.append("</row>\n");
}
```

---
*Tài liệu được tạo tự động bởi Gemini để hỗ trợ quá trình phát triển.*
