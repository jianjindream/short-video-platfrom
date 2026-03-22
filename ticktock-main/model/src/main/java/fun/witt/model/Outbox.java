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

    private String payload;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "processed")
    private Boolean processed;
}
