-- user-service, PostgreSQL
-- Roles are projected from auth-service so the read model can answer without a remote call.
ALTER TABLE user_profiles ADD COLUMN roles VARCHAR(255) NOT NULL DEFAULT 'USER';
