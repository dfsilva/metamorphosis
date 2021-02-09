CREATE TABLE metamorphosis.event_journal
(
    "ordering"         bigserial    NOT NULL,
    persistence_id     varchar(255) NOT NULL,
    sequence_number    int8         NOT NULL,
    deleted            bool         NOT NULL DEFAULT false,
    writer             varchar(255) NOT NULL,
    write_timestamp    int8         NULL,
    adapter_manifest   varchar(255) NULL,
    event_ser_id       int4         NOT NULL,
    event_ser_manifest varchar(255) NOT NULL,
    event_payload      bytea        NOT NULL,
    meta_ser_id        int4         NULL,
    meta_ser_manifest  varchar(255) NULL,
    meta_payload       bytea        NULL,
    CONSTRAINT event_journal_pkey PRIMARY KEY (persistence_id, sequence_number)
);
CREATE UNIQUE INDEX event_journal_ordering_idx ON metamorphosis.event_journal USING btree (ordering);


CREATE TABLE metamorphosis.snapshot
(
    persistence_id        varchar(255) NOT NULL,
    sequence_number       int8         NOT NULL,
    created               int8         NOT NULL,
    snapshot_ser_id       int4         NOT NULL,
    snapshot_ser_manifest varchar(255) NOT NULL,
    snapshot_payload      bytea        NOT NULL,
    meta_ser_id           int4         NULL,
    meta_ser_manifest     varchar(255) NULL,
    meta_payload          bytea        NULL,
    CONSTRAINT snapshot_pkey PRIMARY KEY (persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS metamorphosis.delivered_messages
(
    id          VARCHAR(255),
    content     text,
    if_result    text,
    result      text,
    created     BIGINT,
    processed   BIGINT,
    delivered_to VARCHAR(255),
    from_queue   VARCHAR(255),
    agent       VARCHAR(255),
    PRIMARY KEY (id, created, processed)
);