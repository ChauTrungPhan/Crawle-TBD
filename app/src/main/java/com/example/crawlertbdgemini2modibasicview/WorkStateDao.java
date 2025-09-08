package com.example.crawlertbdgemini2modibasicview;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.crawlertbdgemini2modibasicview.utils.PrefKey;

import java.util.UUID;

/**
 * Data Access Object (DAO) cho Room entity {@link WorkState}.
 * Cung cấp các phương thức để tương tác với bảng work_states trong cơ sở dữ liệu.
 */
@Dao
public interface WorkStateDao {

    /**
     * Chèn một đối tượng WorkState mới vào cơ sở dữ liệu.
     * Nếu đã tồn tại WorkState với cùng workId, nó sẽ được thay thế.
     *
     * @param workState Đối tượng WorkState cần chèn hoặc cập nhật.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkState workState);

    /**
     * Cập nhật một đối tượng WorkState hiện có trong cơ sở dữ liệu.
     *
     * @param workState Đối tượng WorkState cần cập nhật.
     */
    @Update
    void update(WorkState workState);

    /**
     * Truy vấn và trả về WorkState đầu tiên tìm thấy trong cơ sở dữ liệu.
     * Phương thức này thích hợp nếu ứng dụng chỉ theo dõi một WorkRequest tại một thời điểm.
     *
     * @return Đối tượng WorkState đầu tiên, hoặc null nếu không có.
     */
    @Query("SELECT * FROM work_states WHERE workIdPrefKey = :workIdPrefKey LIMIT 1") // SỬA: Dùng tên bảng đã định nghĩa
    WorkState getWorkState(PrefKey workIdPrefKey);
    //LiveData<WorkState> getLiveWorkState(); // Thêm phương thức này

    /**
     * Xóa một WorkState khỏi cơ sở dữ liệu dựa trên ID của WorkRequest.
     *
     * @param workIdPrefKey Là ID của bản ghi chứa WorkRequest cần xóa.
     */
//    @Query("DELETE FROM work_states WHERE requestId = :requestId AND workId = : workId") // SỬA: Dùng tên bảng đã định nghĩa
    @Query("DELETE FROM work_states WHERE workIdPrefKey = :workIdPrefKey AND workId = :workId")
    void deleteByWorkId(PrefKey workIdPrefKey, UUID workId);                                //void deleteByWorkId(UUID workId);

//    @Query("DELETE FROM work_states WHERE typeCrawl = :typeCrawl") // SỬA: Dùng tên bảng đã định nghĩa
//    void deleteByTypeCrawl(String typeCrawl);

    /**
     * Xóa tất cả các WorkState khỏi cơ sở dữ liệu.
     * Có thể hữu ích để reset trạng thái.
     */
    @Query("DELETE FROM work_states")
    void deleteAllWorkStates(); // SỬA: Thêm phương thức để xóa tất cả
}