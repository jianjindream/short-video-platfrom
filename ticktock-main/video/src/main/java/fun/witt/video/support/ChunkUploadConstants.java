package fun.witt.video.support;

public final class ChunkUploadConstants {
    public static final String UPLOAD_META_PREFIX = "chunk_upload:";
    public static final String UPLOAD_PARTS_PREFIX = "chunk_upload_parts:";
    public static final String CHUNK_TMP_PREFIX = "tmp/";

    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ABORTED = "ABORTED";

    public static final String META_FIELD_USER_ID = "userId";
    public static final String META_FIELD_FILE_NAME = "fileName";
    public static final String META_FIELD_FILE_SIZE = "fileSize";
    public static final String META_FIELD_OBJECT_NAME = "objectName";
    public static final String META_FIELD_TITLE = "title";
    public static final String META_FIELD_CHUNK_COUNT = "chunkCount";
    public static final String META_FIELD_CHUNK_SIZE = "chunkSize";
    public static final String META_FIELD_STATUS = "status";
    public static final String META_FIELD_CREATED_AT = "createdAt";
    public static final String META_FIELD_LAST_CHUNK_AT = "lastChunkAt";

    public static final long UPLOAD_TIMEOUT_HOURS = 24L;
    public static final long UPLOAD_META_TTL_HOURS = 25L;
    public static final long CLEANUP_FIXED_DELAY_MS = 1800000L;

    private ChunkUploadConstants() {
    }
}
