package fun.witt.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Table(name = "t_outbox")
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "event_id")
    private String eventId;

    private String topic;

    @Column(name = "event_key")
    private String eventKey;

    private String payload;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "processed")
    private Boolean processed;

    private String status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Date nextRetryAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_at")
    private Date processedAt;
}
