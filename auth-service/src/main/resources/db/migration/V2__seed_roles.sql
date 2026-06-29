INSERT INTO roles (name, created_at, updated_at)
VALUES
  ('ROLE_ADMIN', now(), now()),
  ('ROLE_HR', now(), now()),
  ('ROLE_EMPLOYEE', now(), now())
ON CONFLICT (name) DO NOTHING;
