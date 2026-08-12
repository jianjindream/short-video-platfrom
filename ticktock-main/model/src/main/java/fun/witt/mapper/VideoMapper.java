package fun.witt.mapper;

import fun.witt.model.Video;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface VideoMapper extends MyMapper<Video> {
    List<Video> queryVideoOrderByLatestTime(@Param("latestTime") Date latestTime, @Param("count") int count);

    List<Video> queryVideoOrderByHotScore(@Param("latestTime") Date latestTime, @Param("count") int count);

    List<Video> selectVideoByIdList(@Param("videoIdList") List<Long> videoIdList);

    Long selectAuthorId(@Param("videoId") Long videoId);

    int increaseCommentCount(@Param("videoId") Long videoId, @Param("delta") int delta);
}
