CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    room_code VARCHAR(100) NOT NULL,
    meeting_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    max_participants INTEGER NOT NULL DEFAULT 50,
    organization_id UUID,
    project_id UUID,
    created_by UUID NOT NULL,
    scheduled_start TIMESTAMPTZ,
    scheduled_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_meetings_room_code UNIQUE (room_code)
);
CREATE INDEX ix_meetings_created_by ON meetings(created_by);
CREATE INDEX ix_meetings_status ON meetings(status);
CREATE INDEX ix_meetings_org_id ON meetings(organization_id);
CREATE INDEX ix_meetings_scheduled ON meetings(scheduled_start);

CREATE TABLE meeting_participants (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id UUID,
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(320),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by UUID,
    joined_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_meeting_participant_user UNIQUE (meeting_id, user_id)
);
CREATE INDEX ix_participants_meeting ON meeting_participants(meeting_id);
CREATE INDEX ix_participants_user ON meeting_participants(user_id, status);

CREATE TABLE meeting_chat_messages (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    sender_name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_chat_meeting ON meeting_chat_messages(meeting_id, created_at);
