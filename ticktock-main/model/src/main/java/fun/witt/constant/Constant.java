package fun.witt.constant;

public class Constant {
    public static final String FAVORITE_LIKE = "1";
    public static final String FAVORITE_UNLIKE = "2";
    public static final String COLLECT_COLLECT = "1";
    public static final String COLLECT_UNCOLLECT = "2";

    public static final String COMMENT_PUBLISH = "1";
    public static final String COMMENT_REMOVE = "2";

    public static final String RELATION_FOLLOW = "1";
    public static final String RELATION_UNFOLLOW = "2";

    // Outbox aggregate types
    public static final String AGG_TYPE_FOLLOW = "FOLLOW";
    public static final String AGG_TYPE_UNFOLLOW = "UNFOLLOW";
    public static final String AGG_TYPE_LIKE_EVENT = "LIKE_EVENT";
    public static final String AGG_TYPE_COLLECT_EVENT = "COLLECT_EVENT";

    // Kafka topics
    public static final String TOPIC_CANAL_OUTBOX = "canal-outbox";
    public static final String TOPIC_LIKE_EVENTS = "like-events";
    public static final String TOPIC_COLLECT_EVENTS = "collect-events";

    // Redis key patterns
    public static final String REDIS_VIDEO_LIKE_BITMAP = "bm:like:video:%d:%d";
    public static final String REDIS_VIDEO_COLLECT_BITMAP = "bm:collect:video:%d:%d";
    public static final String REDIS_VIDEO_COUNTER = "cnt:v1:video:%d";
    public static final String REDIS_AGG_VIDEO_LIKE = "agg:v1:video:%d:%d";
    public static final String REDIS_AGG_VIDEO_COLLECT = "agg:v1:video:collect:%d:%d";
    public static final String REDIS_AGG_USER_COUNT = "agg:ucount:%d:%d";
    public static final String REDIS_ACTIVE_USER_SET = "active:ucount:%d";
    public static final String REDIS_ACTIVE_VIDEO_SET = "active:video:%d";
    public static final String REDIS_USER_COUNTER = "ucounter:%d";
    public static final String REDIS_RELATION_FOLLOWING = "rel:following:%d";
    public static final String REDIS_RELATION_FOLLOWER = "rel:follower:%d";
    public static final String REDIS_HOT_FEED_ZSET = "feed:v1:hot";

    // User counter offsets in bits. Each field is an unsigned 32-bit big-endian segment.
    public static final int OFFSET_FOLLOW_COUNT = 0;
    public static final int OFFSET_FOLLOWER_COUNT = 32;
    public static final int OFFSET_FAVORITED_COUNT = 64;
    public static final int OFFSET_FAVORITE_COUNT = 96;
    public static final int USER_COUNTER_FIELDS = 4;
    public static final int COUNTER_FIELD_SIZE = 4;
    public static final int VIDEO_COUNTER_SCHEMA_LEN = 5;
    public static final int VIDEO_COUNTER_IDX_LIKE = 1;
    public static final int VIDEO_COUNTER_IDX_COLLECT = 2;
    public static final int BITMAP_CHUNK_SIZE = 32768;

    public static final String RELATION_EVENT_FOLLOW_CREATED = "FollowCreated";
    public static final String RELATION_EVENT_FOLLOW_CANCELED = "FollowCanceled";
}
