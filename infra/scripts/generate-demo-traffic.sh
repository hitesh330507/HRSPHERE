#!/bin/bash

# Simple load generation script for HRSphere
# Hits Nginx public port (default 8000) or Gateway directly.
# Simulates login, employee creation, retrieval, department fetching, and leave requests.

set -e

NGINX_URL="http://localhost:8000"
GATEWAY_URL="http://localhost:8080"
BASE_URL=$NGINX_URL

echo "Checking if HRSphere Gateway is reachable..."
if ! curl -s --connect-timeout 3 "$BASE_URL/actuator/health" > /dev/null; then
  echo "Nginx on $BASE_URL not reachable. Falling back to Gateway at $GATEWAY_URL"
  BASE_URL=$GATEWAY_URL
  if ! curl -s --connect-timeout 3 "$BASE_URL/actuator/health" > /dev/null; then
    echo "ERROR: Gateway and Nginx are not reachable. Please make sure the containers are running."
    exit 1
  fi
fi

echo "Using entrypoint: $BASE_URL"

# 1. Login to retrieve admin JWT token
echo "Logging in as admin..."
LOGIN_PAYLOAD='{"username":"admin","password":"Admin123!"}'
LOGIN_RESP=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" \
  "$BASE_URL/api/v1/auth/login")

# Extract token using grep/sed (no jq dependency)
ACCESS_TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "ERROR: Failed to retrieve access token. Login response:"
  echo "$LOGIN_RESP"
  exit 1
fi

echo "Login successful. Access token acquired."

# Helper function to generate authorization header
auth_header() {
  echo "Authorization: Bearer $ACCESS_TOKEN"
}

# 2. Simulate failed logins (for auth_login_failure metric)
echo "Simulating some failed login attempts..."
for i in {1..3}; do
  curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"invalid_user","password":"WrongPassword123!"}' \
    "$BASE_URL/api/v1/auth/login" > /dev/null || true
done

# 3. Fetch departments
echo "Fetching departments..."
DEPT_RESP=$(curl -s -X GET \
  -H "$(auth_header)" \
  "$BASE_URL/api/v1/department")

# We will need a department ID. Let's try to extract first department ID.
DEPT_ID=$(echo "$DEPT_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
if [ -z "$DEPT_ID" ]; then
  echo "Warning: No departments found, we might need to create one."
  # Let's create a department if none exists
  CREATE_DEPT_PAYLOAD='{"name":"Engineering","code":"ENG","description":"Software Engineering"}'
  DEPT_CREATE_RESP=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -H "$(auth_header)" \
    -d "$CREATE_DEPT_PAYLOAD" \
    "$BASE_URL/api/v1/department")
  DEPT_ID=$(echo "$DEPT_CREATE_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
fi
echo "Using Department ID: $DEPT_ID"

# 4. Create multiple employees (lifecycle events)
echo "Creating new employees..."
EMPLOYEE_IDS=()
for i in {1..3}; do
  RAND_VAL=$(date +%s%N | cut -b10-17)
  EMP_USER="emp_${RAND_VAL}"
  EMP_EMAIL="emp_${RAND_VAL}@hrsphere.com"
  
  # Register the user first in auth-service
  echo "Registering user $EMP_USER..."
  REG_PAYLOAD="{\"username\":\"$EMP_USER\",\"email\":\"$EMP_EMAIL\",\"password\":\"EmpPassword123!\",\"role\":\"ROLE_EMPLOYEE\"}"
  curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$REG_PAYLOAD" \
    "$BASE_URL/api/v1/auth/register" > /dev/null
  
  # Create employee profile
  EMP_PAYLOAD="{\"firstName\":\"John\",\"lastName\":\"Doe_${RAND_VAL}\",\"email\":\"$EMP_EMAIL\",\"authUsername\":\"$EMP_USER\",\"phoneNumber\":\"+155555${RAND_VAL}\",\"departmentId\":\"$DEPT_ID\",\"jobTitle\":\"Software Engineer\",\"salary\":85000.00,\"employmentType\":\"FULL_TIME\",\"dateOfJoining\":\"$(date +%Y-%m-%d)\"}"
  
  EMP_CREATE_RESP=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -H "$(auth_header)" \
    -d "$EMP_PAYLOAD" \
    "$BASE_URL/api/v1/employees/create")
  
  EMP_ID=$(echo "$EMP_CREATE_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
  if [ -n "$EMP_ID" ]; then
    echo "Created employee: $EMP_ID"
    EMPLOYEE_IDS+=("$EMP_ID")
  else
    echo "Failed to create employee profile for $EMP_USER. Response: $EMP_CREATE_RESP"
  fi
done

# 5. Fetch employees list
echo "Fetching employees list..."
curl -s -X GET \
  -H "$(auth_header)" \
  "$BASE_URL/api/v1/employees/list" > /dev/null

# 6. Apply and Review Leaves (leave-service metrics)
if [ ${#EMPLOYEE_IDS[@]} -gt 0 ]; then
  # We need a leave token for the employee to apply for leave.
  # Let's log in as one of the new employees to apply for leave.
  ACTIVE_EMP_USER="emp_${RAND_VAL}"
  echo "Logging in as employee $ACTIVE_EMP_USER to apply for leave..."
  EMP_LOGIN_PAYLOAD="{\"username\":\"$ACTIVE_EMP_USER\",\"password\":\"EmpPassword123!\"}"
  EMP_LOGIN_RESP=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$EMP_LOGIN_PAYLOAD" \
    "$BASE_URL/api/v1/auth/login")
  
  EMP_TOKEN=$(echo "$EMP_LOGIN_RESP" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
  
  if [ -n "$EMP_TOKEN" ]; then
    # First get leave types
    echo "Fetching leave types..."
    TYPES_RESP=$(curl -s -X GET \
      -H "Authorization: Bearer $EMP_TOKEN" \
      "$BASE_URL/api/v1/leave/types")
    
    LEAVE_TYPE_ID=$(echo "$TYPES_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
    
    if [ -n "$LEAVE_TYPE_ID" ]; then
      START_DATE=$(date -d "+10 days" +%Y-%m-%d 2>/dev/null || date -v+10d +%Y-%m-%d)
      END_DATE=$(date -d "+12 days" +%Y-%m-%d 2>/dev/null || date -v+12d +%Y-%m-%d)
      
      LEAVE_PAYLOAD="{\"leaveTypeId\":\"$LEAVE_TYPE_ID\",\"startDate\":\"$START_DATE\",\"endDate\":\"$END_DATE\",\"reason\":\"Vacation time!\"}"
      
      echo "Applying for annual leave using leave type $LEAVE_TYPE_ID..."
      LEAVE_RESP=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $EMP_TOKEN" \
        -d "$LEAVE_PAYLOAD" \
        "$BASE_URL/api/v1/leave/requests")
      
      LEAVE_ID=$(echo "$LEAVE_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
      
      if [ -n "$LEAVE_ID" ]; then
        echo "Leave request applied: $LEAVE_ID"
        
        # Review the leave request as admin
        echo "Approving leave request $LEAVE_ID as admin..."
        REVIEW_PAYLOAD='{"decision":"APPROVE","comments":"Enjoy your vacation!"}'
        curl -s -X PATCH \
          -H "Content-Type: application/json" \
          -H "$(auth_header)" \
          -d "$REVIEW_PAYLOAD" \
          "$BASE_URL/api/v1/leave/requests/$LEAVE_ID/review" > /dev/null
          
        echo "Leave approved."
      else
        echo "Failed to apply for leave. Response: $LEAVE_RESP"
      fi
    else
      echo "Could not fetch leave type ID from response: $TYPES_RESP"
    fi
  else
    echo "Could not login as employee to apply for leave."
  fi
  
  # Terminate one employee to trigger terminate metric
  TERM_EMP_ID=${EMPLOYEE_IDS[0]}
  echo "Terminating employee $TERM_EMP_ID..."
  TERM_PAYLOAD="{\"dateOfTermination\":\"$(date +%Y-%m-%d)\",\"reason\":\"Voluntary Resignation\"}"
  curl -s -X PATCH \
    -H "Content-Type: application/json" \
    -H "$(auth_header)" \
    -d "$TERM_PAYLOAD" \
    "$BASE_URL/api/v1/employees/$TERM_EMP_ID/terminate" > /dev/null
  echo "Employee terminated."
fi

echo "Traffic simulation completed successfully!"
