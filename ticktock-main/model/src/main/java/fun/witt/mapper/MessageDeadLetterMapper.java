package fun.witt.mapper;

import org.apache.ibatis.annotations.Param;

public interface MessageDeadLetterMapper {

    int insertDeadLetter(@Param("eventId") String eventId,
                         @Param("topic") String topic,
                         @Param("consumerGroup") String consumerGroup,
                         @Param("payload") String payload,
                         @Param("errorMessage") String errorMessage,
                         @Param("retryCount") Integer retryCount);
}
