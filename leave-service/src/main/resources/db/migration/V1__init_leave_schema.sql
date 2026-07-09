CREATE TABLE leave_types (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name                 VARCHAR(100) NOT NULL UNIQUE,
  code                 VARCHAR(10)  NOT NULL UNIQUE,
  default_annual_days  INTEGER      NOT NULL,
  is_paid              BOOLEAN      NOT NULL DEFAULT TRUE,
  requires_approval    BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at           TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at           TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE leave_requests (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_id       UUID         NOT NULL,
  leave_type_id     UUID         NOT NULL REFERENCES leave_types(id),
  start_date        DATE         NOT NULL,
  end_date          DATE         NOT NULL,
  number_of_days    INTEGER      NOT NULL,
  reason            VARCHAR(500),
  status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  applied_at        TIMESTAMP    NOT NULL,
  reviewed_by       VARCHAR(50),
  reviewed_at       TIMESTAMP,
  review_comments   VARCHAR(500),
  created_at        TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
  CHECK (end_date >= start_date)
);

CREATE TABLE leave_balances (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_id       UUID         NOT NULL,
  leave_type_id     UUID         NOT NULL REFERENCES leave_types(id),
  year              INTEGER      NOT NULL,
  allocated_days    INTEGER      NOT NULL,
  used_days         INTEGER      NOT NULL DEFAULT 0,
  remaining_days    INTEGER      NOT NULL,
  created_at        TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
  UNIQUE (employee_id, leave_type_id, year)
);

CREATE INDEX idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status      ON leave_requests(status);
CREATE INDEX idx_leave_balances_employee_id ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_lookup      
  ON leave_balances(employee_id, leave_type_id, year);
