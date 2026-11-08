--liquibase formatted sql

--changeset esmalagutti:shared-001-event-publication
--comment: Spring Modulith event publication registry (spring-modulith-starter-jpa 2.1). Columns match Modulith's JPA entity; text instead of varchar(255) so large serialized events fit.
CREATE TABLE event_publication (
    id                     uuid        NOT NULL,
    listener_id            text        NOT NULL,
    event_type             text        NOT NULL,
    serialized_event       text        NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    last_resubmission_date timestamptz,
    completion_attempts    integer     NOT NULL DEFAULT 0,
    status                 text,
    CONSTRAINT pk_event_publication PRIMARY KEY (id),
    CONSTRAINT ck_event_publication_status
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

CREATE INDEX ix_event_publication_serialized_event_hash ON event_publication USING hash (serialized_event);
CREATE INDEX ix_event_publication_completion_date ON event_publication (completion_date);
--rollback DROP TABLE event_publication;
