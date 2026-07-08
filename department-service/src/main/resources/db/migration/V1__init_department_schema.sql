-- Create extension for random UUIDs if not exists
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE departments (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  department_code       VARCHAR(20)  NOT NULL UNIQUE,
  name                  VARCHAR(100) NOT NULL UNIQUE,
  description           VARCHAR(500),
  head_of_department    UUID,
  is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at            TIMESTAMP,
  created_at            TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_departments_code       ON departments(department_code);
CREATE INDEX idx_departments_name       ON departments(name);
CREATE INDEX idx_departments_is_deleted ON departments(is_deleted);
