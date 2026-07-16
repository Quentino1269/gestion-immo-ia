package com.immo.gestion.shared.adapter.persistence.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne append-only de l'event store. Cf. MISSION.md §5 (Event Sourcing strict).
 * La contrainte unique (stream_id, version) est le mécanisme de concurrence optimiste.
 */
@Entity
@Table(
        name = "event_store",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_store_stream_version", columnNames = {"stream_id", "version"})
)
public class EventStoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_no")
    private Long sequenceNo;

    @Column(name = "stream_id", nullable = false, updatable = false)
    private UUID streamId;

    @Column(name = "stream_type", nullable = false, length = 40, updatable = false)
    private String streamType;

    @Column(name = "version", nullable = false, updatable = false)
    private long version;

    @Column(name = "event_type", nullable = false, length = 200, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected EventStoreEntry() {
        // JPA
    }

    public EventStoreEntry(UUID streamId, String streamType, long version, String eventType,
                            String payload, Instant occurredAt, Instant recordedAt) {
        this.streamId = streamId;
        this.streamType = streamType;
        this.version = version;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
    }

    public Long getSequenceNo() { return sequenceNo; }
    public UUID getStreamId() { return streamId; }
    public String getStreamType() { return streamType; }
    public long getVersion() { return version; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
}
