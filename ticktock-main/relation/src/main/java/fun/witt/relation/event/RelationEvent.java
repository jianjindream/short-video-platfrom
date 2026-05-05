package fun.witt.relation.event;

public class RelationEvent {
    private String eventId;
    private String type;
    private Long fromUserId;
    private Long toUserId;
    private Long timestamp;

    public RelationEvent() {
    }

    public RelationEvent(String eventId, String type, Long fromUserId, Long toUserId, Long timestamp) {
        this.eventId = eventId;
        this.type = type;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
