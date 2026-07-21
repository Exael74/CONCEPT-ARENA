-- Room ownership: StartRoundCommandHandler now restricts POST /api/game/{roomId}/start to the
-- room's creator (any authenticated participant could previously start a round for anyone's room).
-- Nullable: rooms created before this column existed have no creator on record — StartRoundCommandHandler
-- fails open (allows the start) when creatorUserId is null, rather than permanently locking them out.
ALTER TABLE room_read_model ADD COLUMN IF NOT EXISTS creator_user_id VARCHAR(255);
