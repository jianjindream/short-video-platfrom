package fun.witt.relation.outbox;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class CanalKafkaBridge implements SmartLifecycle {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String destination;
    private final String username;
    private final String password;
    private final String filter;
    private final int batchSize;
    private final long intervalMs;

    private volatile boolean running;
    private CanalConnector connector;
    private Thread worker;

    public CanalKafkaBridge(KafkaTemplate<String, String> kafkaTemplate,
                            @Value("${canal.enabled:true}") boolean enabled,
                            @Value("${canal.host:canal-server}") String host,
                            @Value("${canal.port:11111}") int port,
                            @Value("${canal.destination:example}") String destination,
                            @Value("${canal.username:}") String username,
                            @Value("${canal.password:}") String password,
                            @Value("${canal.filter:camps_tiktok.t_outbox}") String filter,
                            @Value("${canal.batch-size:100}") int batchSize,
                            @Value("${canal.interval-ms:1000}") long intervalMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.destination = destination;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
    }

    @Override
    public void start() {
        if (!enabled || running) {
            return;
        }
        running = true;
        worker = new Thread(this::runLoop, "canal-outbox-bridge");
        worker.setDaemon(true);
        worker.start();
    }

    private void runLoop() {
        try {
            connector = CanalConnectors.newSingleConnector(new InetSocketAddress(host, port), destination, username, password);
            connector.connect();
            connector.subscribe(filter);
            connector.rollback();
            log.info("canal bridge started, host={}, port={}, destination={}, filter={}", host, port, destination, filter);

            while (running) {
                Message message = connector.getWithoutAck(batchSize);
                long batchId = message.getId();
                if (batchId == -1 || message.getEntries() == null || message.getEntries().isEmpty()) {
                    sleepQuietly(intervalMs);
                    continue;
                }

                try {
                    publishEntries(message);
                    connector.ack(batchId);
                } catch (Exception e) {
                    log.error("canal batch publish failed, batchId={}", batchId, e);
                    connector.rollback(batchId);
                    sleepQuietly(intervalMs);
                }
            }
        } catch (Exception e) {
            log.error("canal bridge stopped by error", e);
        } finally {
            if (connector != null) {
                try {
                    connector.disconnect();
                } catch (Exception ignored) {
                }
            }
            running = false;
        }
    }

    private void publishEntries(Message message) throws Exception {
        for (CanalEntry.Entry entry : message.getEntries()) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            CanalEntry.EventType eventType = rowChange.getEventType();
            if (eventType != CanalEntry.EventType.INSERT) {
                continue;
            }

            ArrayNode rows = objectMapper.createArrayNode();
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                ObjectNode row = objectMapper.createObjectNode();
                for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                    if ("payload".equalsIgnoreCase(column.getName())) {
                        row.put("payload", column.getValue());
                    }
                }
                if (row.has("payload")) {
                    rows.add(row);
                }
            }

            if (rows.size() == 0) {
                continue;
            }

            ObjectNode out = objectMapper.createObjectNode();
            out.put("table", entry.getHeader().getTableName());
            out.put("type", "INSERT");
            out.set("data", rows);
            kafkaTemplate.send(Constant.TOPIC_CANAL_OUTBOX, objectMapper.writeValueAsString(out)).get();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (connector != null) {
            try {
                connector.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return enabled;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
