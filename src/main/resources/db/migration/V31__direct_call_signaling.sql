CREATE TABLE direct_call (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    caller VARCHAR(60) NOT NULL,
    callee VARCHAR(60) NOT NULL,
    kind VARCHAR(10) NOT NULL,
    state VARCHAR(12) NOT NULL,
    offer TEXT,
    answer TEXT,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id)
);
CREATE INDEX direct_call_incoming ON direct_call(callee,state,expires_at);
CREATE INDEX direct_call_conversation ON direct_call(conversation_id,state,expires_at);
