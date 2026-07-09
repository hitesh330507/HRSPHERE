INSERT INTO leave_types (id, name, code, default_annual_days, is_paid, 
  requires_approval, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'Annual Leave', 'AL', 20, true, true, now(), now()),
  (gen_random_uuid(), 'Sick Leave',   'SL', 10, true, true, now(), now()),
  (gen_random_uuid(), 'Casual Leave', 'CL', 7,  true, true, now(), now())
ON CONFLICT DO NOTHING;
