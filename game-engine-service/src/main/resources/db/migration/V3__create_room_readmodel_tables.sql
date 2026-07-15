-- Local, eventually-consistent read-model populated by consuming room-service's RabbitMQ
-- events — see ADR-004. Not the source of truth for Room; room-service owns that.
CREATE TABLE IF NOT EXISTS room_read_model (
    room_id VARCHAR(255) PRIMARY KEY,
    concept_bank_id VARCHAR(255),
    max_participants INT NOT NULL DEFAULT 0,
    game_started BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS participant_read_model (
    id VARCHAR(255) PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT uq_participant_read_model_room_user UNIQUE (room_id, user_id)
);
