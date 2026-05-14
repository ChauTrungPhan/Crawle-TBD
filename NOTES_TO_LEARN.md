# Nhật Ký Tối Ưu Hóa & Học Tập - Crawler TBD

Tài liệu này ghi lại các lỗi quan trọng đã phát hiện và các giải pháp tối ưu hóa để cải thiện hiệu suất và độ ổn định của ứng dụng.

---

## 1. Sửa Lỗi Nghiêm Trọng: Tránh Crash bằng Parsing An Toàn
### Vấn đề:
Trước đây, chúng ta sử dụng phương pháp cắt chuỗi thủ công:
`url.split("key")[1]`
**Rủi ro:** Nếu URL không chứa chữ "key" (ví dụ: link lỗi, link quảng cáo), ứng dụng sẽ bị lỗi `ArrayIndexOutOfBoundsException` và văng (crash) ngay lập tức.

### Giải pháp (Đã áp dụng trong `Utils.java`):
Sử dụng lớp `android.net.Uri` để bóc tách tham số một cách chính thống:
```java
Uri uri = Uri.parse(url);
String key = uri.getQueryParameter("key");
// Nếu key null, trả về giá trị mặc định, không gây crash.
```
**Bài học:** Luôn giả định dữ liệu từ Internet là "không đáng tin cậy". Hãy dùng các thư viện chuẩn của Android để xử lý thay vì tự cắt chuỗi thủ công.

---

## 2. Tối Ưu Hiệu Suất: Giảm tải Ghi Đĩa (I/O)
### Vấn đề:
Trong Worker, chúng ta gọi `settingsRepository.saveSumCrawledUrlsCount(...)` sau mỗi lần cào xong 1 URL.
**Hệ quả:** Nếu cào 10.000 URL, ứng dụng sẽ thực hiện 10.000 lần ghi vào bộ nhớ Flash. Việc này làm máy nóng, tốn pin và giảm tuổi thọ bộ nhớ.

### Giải pháp (Đã áp dụng trong `MyWorkerCrawler.java`):
Sử dụng kỹ thuật "Gom lô" (Batching):
```java
long currentCount = crawledUrlCount.incrementAndGet();
if (currentCount % 10 == 0 || currentCount >= totalUrlsToCrawl) {
    settingsRepository.saveSumCrawledUrlsCount(..., currentCount);
}
```
**Bài học:** Các thao tác ghi đĩa (SharedPreferences, Database, File) rất tốn kém. Hãy gom dữ liệu lại và ghi theo đợt.

---

## 3. Hiện Đại Hóa Database: Chuyển dịch sang Room
### Vấn đề:
Dự án đang dùng song song SQLite truyền thống (`DBHelperThuoc`) và Room. Điều này khiến code bị phân mảnh, khó quản lý Transaction và dễ gây lỗi "Database is locked".

### Giải pháp (Đã thực hiện bước đầu):
1. Đã đăng ký `ThuocRoom` vào `AppDatabase`.
2. Hướng tới việc thay thế `WriterEngineChunked` (với hàng trăm dòng SQL thủ công) bằng Room DAO.

**Mẫu Room DAO tối ưu:**
```java
@Insert(onConflict = OnConflictStrategy.REPLACE)
void insertAll(List<ThuocRoom> list); 
// Một dòng này thay thế toàn bộ logic UPSERT phức tạp trong WriterEngine.
```

---

## 4. Phát Hiện Logic "Kỳ Lạ" (Cần chú ý)
Trong `MyViewModelCrawler_Gemini.java`, có đoạn code tự động reset tiến trình về 0 khi Worker hoàn thành:
`settingsRepository.saveSumCrawledUrlsCount(..., 0);`
**Lưu ý:** Nếu bạn muốn giữ lại lịch sử để xem kết quả, hãy cân nhắc việc xóa dòng này hoặc chỉ reset khi người dùng bấm nút "Bắt đầu mới".

---

## 5. Lời Khuyên Kiến Trúc (Clean Code)
1. **Single Responsibility:** File `MyWorkerCrawler.java` đang quá lớn (>2000 dòng). Nên tách logic xử lý Jsoup ra một class `CrawlerParser`.
2. **Quản lý Luồng:** `ForkJoinPool` là công cụ mạnh, nhưng hãy kiểm soát số lượng luồng (Parallelism) để tránh bị Server chặn IP do gửi quá nhiều yêu cầu cùng lúc.
3. **Mô hình Producer-Consumer:** Việc dùng `BlockingQueue` trong `CrawlRuntime` là một kỹ thuật rất tốt, giúp tách biệt việc cào dữ liệu (mạng) và ghi dữ liệu (DB). Đừng bỏ kỹ thuật này!

---
*Tài liệu này được tạo ra để hỗ trợ quá trình học tập và phát triển dự án.*
