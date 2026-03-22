package fun.witt.mapper;

import fun.witt.model.Outbox;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OutboxMapper extends MyMapper<Outbox> {

    List<Outbox> selectUnprocessedBatch(@Param("limit") int limit);

    int markAsProcessed(@Param("ids") List<Long> ids);
}
