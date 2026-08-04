package fun.witt.mapper;

import fun.witt.model.Outbox;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface OutboxMapper extends MyMapper<Outbox> {

    List<Outbox> selectUnprocessedBatch(@Param("limit") int limit);

    List<Outbox> selectPublishableByTopics(@Param("topics") List<String> topics, @Param("limit") int limit);

    int markAsProcessed(@Param("ids") List<Long> ids);

    int markOneAsProcessed(@Param("id") Long id);

    int markAsPublishing(@Param("id") Long id, @Param("nextRetryAt") Date nextRetryAt);

    int markForRetry(@Param("id") Long id,
                     @Param("retryCount") Integer retryCount,
                     @Param("nextRetryAt") Date nextRetryAt,
                     @Param("lastError") String lastError);

    int markAsDead(@Param("id") Long id, @Param("lastError") String lastError);
}
