package fun.witt.constant;

public class Constant {
    public static final String FAVORITE_LIKE = "1";
    public static final String FAVORITE_UNLIKE = "2";

    public static final String COMMENT_PUBLISH = "1";
    public static final String COMMENT_REMOVE = "2";

    public static final String RELATION_FOLLOW = "1";
    public static final String RELATION_UNFOLLOW = "2";

    // Outbox aggregate types
    public static final String AGG_TYPE_FOLLOW = "FOLLOW";
    public static final String AGG_TYPE_UNFOLLOW = "UNFOLLOW";

    // Kafka topics
    public static final String TOPIC_CANAL_OUTBOX = "canal-outbox";
    public static final String TOPIC_LIKE_EVENTS = "like-events";

    // Redis key patterns
    public static final String REDIS_FEED_LIKES = "feed:%d:likes";
    public static final String REDIS_AGG_LIKE = "agg:like:feed:%d:%d";
    public static final String REDIS_ACTIVE_SET = "active:%d";
    public static final String REDIS_USER_COUNTER = "ucounter:%d";

    // BITFIELD offsets (u32 = unsigned 32-bit)
    public static final String BITFIELD_TYPE = "u32";
    public static final int OFFSET_FOLLOW_COUNT = 0;
    public static final int OFFSET_FOLLOWER_COUNT = 32;
    public static final int OFFSET_FAVORITED_COUNT = 64;
}

