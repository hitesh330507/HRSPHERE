-- Create sequence for employee codes
CREATE SEQUENCE IF NOT EXISTS employee_code_seq START 1;

-- Employees table
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE employees (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_code VARCHAR(20) NOT NULL UNIQUE,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20),
  auth_username VARCHAR(50) UNIQUE,
  job_title VARCHAR(100) NOT NULL,
  employment_type VARCHAR(20) NOT NULL,
  employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  date_of_joining DATE NOT NULL,
  date_of_termination DATE,
  department_id UUID,
  manager_id UUID,
  date_of_birth DATE,
  gender VARCHAR(30),
  nationality VARCHAR(100),
  street VARCHAR(255),
  city VARCHAR(100),
  state VARCHAR(100),
  postal_code VARCHAR(20),
  country VARCHAR(100),
  bank_account_number VARCHAR(50),
  bank_name VARCHAR(100),
  tax_id VARCHAR(50),
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_employee_code ON employees(employee_code);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_auth_username ON employees(auth_username);
CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_employment_status ON employees(employment_status);
CREATE INDEX idx_employees_is_deleted ON employees(is_deleted);
