-- Dev seed only; change this password before any real deployment.
INSERT INTO users (username, email, password_hash, enabled, created_at, updated_at)
VALUES (
  'admin',
  'admin@hrsphere.dev',
  '$2a$10$JgInRxe2UjXuoaa4awv/du.bGGZ9I7mKDYvhAdCNMhoj3/sszoaiq',
  TRUE,
  now(),
  now()
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;
