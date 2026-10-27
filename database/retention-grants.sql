-- Apply with a database operator after Flyway V15. Replace main_database with
-- the actual database. Provision main_retention separately with a random secret.
-- Web administration manages holds but cannot execute content deletion.
GRANT SELECT,INSERT,UPDATE ON main_database.moderation_retention_hold TO 'main_django'@'127.0.0.1';
GRANT SELECT ON main_database.conversation TO 'main_retention'@'127.0.0.1';
GRANT UPDATE(id) ON main_database.conversation TO 'main_retention'@'127.0.0.1';
GRANT SELECT ON main_database.message TO 'main_retention'@'127.0.0.1';
GRANT SELECT,UPDATE(body,reason,state,purged_at) ON main_database.message_revision TO 'main_retention'@'127.0.0.1';
GRANT SELECT,UPDATE(body,reason,state,purged_at) ON main_database.status_update TO 'main_retention'@'127.0.0.1';
GRANT SELECT,DELETE ON main_database.message_attachment TO 'main_retention'@'127.0.0.1';
GRANT DELETE,SELECT(message_id) ON main_database.message_attachment_chunk TO 'main_retention'@'127.0.0.1';
GRANT DELETE,SELECT(status_id) ON main_database.status_media_chunk TO 'main_retention'@'127.0.0.1';
GRANT DELETE,SELECT(status_id) ON main_database.status_view TO 'main_retention'@'127.0.0.1';
GRANT DELETE,SELECT(status_id) ON main_database.status_recipient TO 'main_retention'@'127.0.0.1';
GRANT SELECT,DELETE ON main_database.automatic_assessment TO 'main_retention'@'127.0.0.1';
GRANT SELECT,INSERT,DELETE ON main_database.administrative_audit TO 'main_retention'@'127.0.0.1';
GRANT SELECT,DELETE ON main_database.moderation_retention_hold TO 'main_retention'@'127.0.0.1';
