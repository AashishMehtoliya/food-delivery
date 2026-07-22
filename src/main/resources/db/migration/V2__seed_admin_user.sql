-- Bootstrap ADMIN account. Public registration (POST /auth/register) intentionally
-- disallows the ADMIN role, so a first admin must exist before any admin-only
-- endpoint can be used. Dev/test credentials only: admin@fooddelivery.com / admin123
-- (documented in README).
INSERT INTO app_user (name, email, password_hash, role)
VALUES ('Admin', 'admin@fooddelivery.com', '$2b$10$flJIDa9k3qIwuu/SPGe2I.3kwnVZ/cNnrr1R2OiuKxRJoIfYKP2wO', 'ADMIN');
