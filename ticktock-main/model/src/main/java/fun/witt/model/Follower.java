package fun.witt.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Table(name = "t_follower")
public class Follower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "created_at")
    private Date createdAt;
}
