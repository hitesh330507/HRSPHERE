package com.hrsphere.leave.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.event.EventEnvelope;
import com.hrsphere.leave.dto.ApplyLeaveRequest;
import com.hrsphere.leave.dto.LeaveRequestResponse;
import com.hrsphere.leave.dto.ReviewLeaveRequest;
import com.hrsphere.leave.entity.enums.LeaveStatus;
import com.hrsphere.leave.event.LeaveApprovedPayload;
import com.hrsphere.leave.integration.fixtures.LeaveTestFixtures;
import com.hrsphere.leave.repository.LeaveBalanceRepository;
import com.hrsphere.leave.repository.LeaveRequestRepository;
import com.hrsphere.leave.repository.LeaveTypeRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Leave Service Lifecycle Integration Tests")
public class LeaveLifecycleIntegrationTest extends BaseIntegrationTest {

  @Autowired private LeaveRequestRepository leaveRequestRepository;

  @Autowired private LeaveBalanceRepository leaveBalanceRepository;

  @Autowired private LeaveTypeRepository leaveTypeRepository;

  @Autowired private RedisConnectionFactory connectionFactory;

  private final List<String> receivedEvents = new CopyOnWriteArrayList<>();
  private RedisMessageListenerContainer testContainer;

  @BeforeEach
  void cleanDb() {
    leaveRequestRepository.deleteAll();
    leaveBalanceRepository.deleteAll();
  }

  @BeforeEach
  void startTestListener() {
    receivedEvents.clear();
    testContainer = new RedisMessageListenerContainer();
    testContainer.setConnectionFactory(connectionFactory);
    testContainer.addMessageListener(
        (message, pattern) -> {
          String body = new String(message.getBody(), StandardCharsets.UTF_8);
          receivedEvents.add(body);
        },
        new ChannelTopic("leave.approved"));
    testContainer.afterPropertiesSet();
    testContainer.start();
  }

  @AfterEach
  void stopTestListener() {
    if (testContainer != null) {
      testContainer.stop();
    }
  }

  private UUID getLeaveTypeIdByCode(String code) {
    return leaveTypeRepository.findAll().stream()
        .filter(t -> t.getCode().equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Seeded leave type " + code + " not found"))
        .getId();
  }

  @Test
  @DisplayName("Full apply -> approve -> balance deduction -> event publishing lifecycle")
  void applyAndApprove_fullLifecycle_withEventVerification() throws Exception {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    // Step 4 — Check initial balance (lazy allocation triggers here)
    ResponseEntity<List> balanceResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    assertThat(balanceResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> balances = balanceResp.getBody();
    assertThat(balances).isNotEmpty();

    Map<String, Object> alBalance =
        balances.stream()
            .filter(b -> "AL".equals(b.get("leaveTypeCode")))
            .findFirst()
            .orElseThrow();
    assertThat(alBalance.get("remainingDays")).isEqualTo(20);
    assertThat(alBalance.get("usedDays")).isEqualTo(0);

    // Step 5 — Apply for 5 days of Annual Leave
    LocalDate start = LocalDate.now().plusDays(1);
    LocalDate end = start.plusDays(4); // 5 days total inclusive
    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(leaveTypeId, start, end, "Vacation");

    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);

    assertThat(applyResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    LeaveRequestResponse applied = applyResp.getBody();
    assertThat(applied).isNotNull();
    assertThat(applied.status).isEqualTo(LeaveStatus.PENDING);
    assertThat(applied.numberOfDays).isEqualTo(5);

    // Step 6 — Confirm balance unchanged before approval
    ResponseEntity<List> balancePreApproveResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    List<Map<String, Object>> balancesPre = balancePreApproveResp.getBody();
    Map<String, Object> alBalancePre =
        balancesPre.stream()
            .filter(b -> "AL".equals(b.get("leaveTypeCode")))
            .findFirst()
            .orElseThrow();
    assertThat(alBalancePre.get("remainingDays")).isEqualTo(20);

    // Step 7 — Approve request
    ReviewLeaveRequest reviewReq = LeaveTestFixtures.approveRequest();
    ResponseEntity<LeaveRequestResponse> approveResp =
        restTemplate.exchange(
            "/leave/requests/" + applied.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(reviewReq, adminHeaders()),
            LeaveRequestResponse.class);

    assertThat(approveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    LeaveRequestResponse approved = approveResp.getBody();
    assertThat(approved).isNotNull();
    assertThat(approved.status).isEqualTo(LeaveStatus.APPROVED);
    assertThat(approved.reviewedBy).isEqualTo("admin");

    // Step 8 — Confirm balance deducted
    ResponseEntity<List> balancePostApproveResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    List<Map<String, Object>> balancesPost = balancePostApproveResp.getBody();
    Map<String, Object> alBalancePost =
        balancesPost.stream()
            .filter(b -> "AL".equals(b.get("leaveTypeCode")))
            .findFirst()
            .orElseThrow();
    assertThat(alBalancePost.get("remainingDays")).isEqualTo(15);
    assertThat(alBalancePost.get("usedDays")).isEqualTo(5);

    // Step 9 & 10 — Confirm event published and envelope matches shape
    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(() -> assertThat(receivedEvents).hasSize(1));

    String eventJson = receivedEvents.get(0);
    EventEnvelope<LeaveApprovedPayload> envelope =
        objectMapper.readValue(
            eventJson, new TypeReference<EventEnvelope<LeaveApprovedPayload>>() {});

    assertThat(envelope.eventType()).isEqualTo("leave.approved");
    assertThat(envelope.source()).isEqualTo("leave-service");
    assertThat(envelope.version()).isEqualTo("v1");
    assertThat(envelope.eventId()).isNotBlank();
    UUID.fromString(envelope.eventId()); // validates UUID structure

    LeaveApprovedPayload payload = envelope.payload();
    assertThat(payload.employeeId()).isEqualTo(employeeId);
    assertThat(payload.leaveTypeCode()).isEqualTo("AL");
    assertThat(payload.numberOfDays()).isEqualTo(5);
    assertThat(payload.startDate()).isEqualTo(start);
    assertThat(payload.endDate()).isEqualTo(end);
  }

  @Test
  @DisplayName("Apply -> reject lifecycle, balance remains unchanged, no event published")
  void applyAndReject_noBalanceChange_noEventPublished() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee2";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Reject me");

    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);

    assertThat(applyResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    LeaveRequestResponse applied = applyResp.getBody();

    // Reject request
    ReviewLeaveRequest reviewReq = LeaveTestFixtures.rejectRequest("Sorry, not this time");
    ResponseEntity<LeaveRequestResponse> rejectResp =
        restTemplate.exchange(
            "/leave/requests/" + applied.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(reviewReq, adminHeaders()),
            LeaveRequestResponse.class);

    assertThat(rejectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(rejectResp.getBody().status).isEqualTo(LeaveStatus.REJECTED);
    assertThat(rejectResp.getBody().reviewComments).isEqualTo("Sorry, not this time");

    // Check balance is still 20
    ResponseEntity<List> balanceResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    List<Map<String, Object>> balances = balanceResp.getBody();
    Map<String, Object> alBalance =
        balances.stream()
            .filter(b -> "AL".equals(b.get("leaveTypeCode")))
            .findFirst()
            .orElseThrow();
    assertThat(alBalance.get("remainingDays")).isEqualTo(20);

    // Verify negative event assertion: no event received
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ignored) {
    }
    assertThat(receivedEvents).isEmpty();
  }

  @Test
  @DisplayName("Applying for leave exceeding remaining balance returns 400 Bad Request")
  void insufficientBalance_rejected() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee3";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID casualLeaveId = getLeaveTypeIdByCode("CL"); // Default 7 days

    // Apply for 10 days
    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            casualLeaveId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(10),
            "Extended Casual");

    ResponseEntity<ApiErrorResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            ApiErrorResponse.class);

    assertThat(applyResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(applyResp.getBody()).isNotNull();
    assertThat(applyResp.getBody().message()).contains("Insufficient leave balance");
  }

  @Test
  @DisplayName("Overlapping pending or approved leave requests are rejected with 409 Conflict")
  void overlappingRequests_rejected() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee4";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    // Request 1: Aug 1 to Aug 5
    ApplyLeaveRequest req1 =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), "Vacation 1");
    ResponseEntity<LeaveRequestResponse> resp1 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req1, employeeHeaders(username)),
            LeaveRequestResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // Request 2: Aug 3 to Aug 4 (overlaps)
    ApplyLeaveRequest req2 =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4), "Vacation 2");
    ResponseEntity<ApiErrorResponse> resp2 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req2, employeeHeaders(username)),
            ApiErrorResponse.class);

    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resp2.getBody().message()).contains("overlaps");
  }

  @Test
  @DisplayName("State machine guard prevents reviewing non-pending requests")
  void stateGuard_cannotReviewNonPendingRequest() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee5";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Vacation");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Approve once
    restTemplate.exchange(
        "/leave/requests/" + request.id + "/review",
        HttpMethod.PATCH,
        new HttpEntity<>(LeaveTestFixtures.approveRequest(), adminHeaders()),
        LeaveRequestResponse.class);

    // Attempt to approve again
    ResponseEntity<ApiErrorResponse> reReviewResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(LeaveTestFixtures.approveRequest(), adminHeaders()),
            ApiErrorResponse.class);

    assertThat(reReviewResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(reReviewResp.getBody().message())
        .contains("Only pending leave requests can be reviewed");
  }

  @Test
  @DisplayName("State machine guard prevents reviewing already rejected requests")
  void stateGuard_cannotReviewAlreadyRejected() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee6";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Vacation");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Reject request
    restTemplate.exchange(
        "/leave/requests/" + request.id + "/review",
        HttpMethod.PATCH,
        new HttpEntity<>(LeaveTestFixtures.rejectRequest("Busy"), adminHeaders()),
        LeaveRequestResponse.class);

    // Attempt to approve now
    ResponseEntity<ApiErrorResponse> reReviewResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(LeaveTestFixtures.approveRequest(), adminHeaders()),
            ApiErrorResponse.class);

    assertThat(reReviewResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(reReviewResp.getBody().message()).contains("Only pending");
  }

  @Test
  @DisplayName("Cancelling a pending request changes status to CANCELLED and has no balance effect")
  void cancelPending_noBalanceEffect() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee7";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            "Pending Cancel");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Cancel before review
    ResponseEntity<LeaveRequestResponse> cancelResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/cancel",
            HttpMethod.PATCH,
            new HttpEntity<>(employeeHeaders(username)),
            LeaveRequestResponse.class);

    assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResp.getBody().status).isEqualTo(LeaveStatus.CANCELLED);

    // Check balance is still 20
    ResponseEntity<List> balanceResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    List<Map<String, Object>> balances = balanceResp.getBody();
    Map<String, Object> alBalance =
        balances.stream()
            .filter(b -> "AL".equals(b.get("leaveTypeCode")))
            .findFirst()
            .orElseThrow();
    assertThat(alBalance.get("remainingDays")).isEqualTo(20);
  }

  @Test
  @DisplayName("Cancelling an approved request changes status to CANCELLED and refunds the balance")
  void cancelApproved_refundsBalance() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee8";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            "Approved Cancel"); // 3 days
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(username)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Approve request
    restTemplate.exchange(
        "/leave/requests/" + request.id + "/review",
        HttpMethod.PATCH,
        new HttpEntity<>(LeaveTestFixtures.approveRequest(), adminHeaders()),
        LeaveRequestResponse.class);

    // Balance should be 17
    ResponseEntity<List> balanceResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    Map<String, Object> alBalance =
        ((List<Map<String, Object>>) balanceResp.getBody())
            .stream().filter(b -> "AL".equals(b.get("leaveTypeCode"))).findFirst().orElseThrow();
    assertThat(alBalance.get("remainingDays")).isEqualTo(17);

    // Cancel approved request
    ResponseEntity<LeaveRequestResponse> cancelResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/cancel",
            HttpMethod.PATCH,
            new HttpEntity<>(employeeHeaders(username)),
            LeaveRequestResponse.class);

    assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResp.getBody().status).isEqualTo(LeaveStatus.CANCELLED);

    // Balance should be restored to 20
    ResponseEntity<List> balanceRestoredResp =
        restTemplate.exchange(
            "/leave/balances/me",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(username)),
            List.class);
    Map<String, Object> alBalanceRestored =
        ((List<Map<String, Object>>) balanceRestoredResp.getBody())
            .stream().filter(b -> "AL".equals(b.get("leaveTypeCode"))).findFirst().orElseThrow();
    assertThat(alBalanceRestored.get("remainingDays")).isEqualTo(20);
    assertThat(alBalanceRestored.get("usedDays")).isEqualTo(0);
  }

  @Test
  @DisplayName("Employee cannot cancel another employee's leave request")
  void ownershipEnforcement_cannotCancelOthersRequest() {
    UUID employeeA = UUID.randomUUID();
    String usernameA = "employeeA";
    stubEmployeeLookup(usernameA, employeeA, "Alice", "Smith");
    stubEmployeeGet(employeeA, "Alice", "Smith");

    UUID employeeB = UUID.randomUUID();
    String usernameB = "employeeB";
    stubEmployeeLookup(usernameB, employeeB, "Bob", "Jones");
    stubEmployeeGet(employeeB, "Bob", "Jones");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            "Alice Vacation");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(usernameA)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Bob attempts to cancel Alice's request
    ResponseEntity<ApiErrorResponse> cancelResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/cancel",
            HttpMethod.PATCH,
            new HttpEntity<>(employeeHeaders(usernameB)),
            ApiErrorResponse.class);

    assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(cancelResp.getBody().message()).contains("You do not have permission");
  }

  @Test
  @DisplayName("Employee cannot view another employee's specific request, but HR/Admin can")
  void ownershipEnforcement_cannotViewOthersSpecificRequest() {
    UUID employeeA = UUID.randomUUID();
    String usernameA = "employeeA";
    stubEmployeeLookup(usernameA, employeeA, "Alice", "Smith");
    stubEmployeeGet(employeeA, "Alice", "Smith");

    UUID employeeB = UUID.randomUUID();
    String usernameB = "employeeB";
    stubEmployeeLookup(usernameB, employeeB, "Bob", "Jones");
    stubEmployeeGet(employeeB, "Bob", "Jones");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            "Alice Vacation");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(usernameA)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Bob attempts to view Alice's request directly
    ResponseEntity<ApiErrorResponse> viewByBobResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id,
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders(usernameB)),
            ApiErrorResponse.class);
    assertThat(viewByBobResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // HR attempts to view Alice's request
    ResponseEntity<LeaveRequestResponse> viewByHrResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id,
            HttpMethod.GET,
            new HttpEntity<>(hrHeaders()),
            LeaveRequestResponse.class);
    assertThat(viewByHrResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(viewByHrResp.getBody().id).isEqualTo(request.id);

    // Admin attempts to view Alice's request
    ResponseEntity<LeaveRequestResponse> viewByAdminResp =
        restTemplate.exchange(
            "/leave/requests/" + request.id,
            HttpMethod.GET,
            new HttpEntity<>(adminHeaders()),
            LeaveRequestResponse.class);
    assertThat(viewByAdminResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(viewByAdminResp.getBody().id).isEqualTo(request.id);
  }

  @Test
  @DisplayName("Only HR/Admin roles are authorized to review leave requests")
  void roleEnforcement_onlyHrAdminCanReview() {
    UUID employeeA = UUID.randomUUID();
    String usernameA = "employeeA";
    stubEmployeeLookup(usernameA, employeeA, "Alice", "Smith");
    stubEmployeeGet(employeeA, "Alice", "Smith");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            "Alice Vacation");
    ResponseEntity<LeaveRequestResponse> applyResp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders(usernameA)),
            LeaveRequestResponse.class);
    LeaveRequestResponse request = applyResp.getBody();

    // Employee attempts to review
    ResponseEntity<ApiErrorResponse> reviewByEmp =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(LeaveTestFixtures.approveRequest(), employeeHeaders(usernameA)),
            ApiErrorResponse.class);
    assertThat(reviewByEmp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // HR attempts to review
    ResponseEntity<LeaveRequestResponse> reviewByHr =
        restTemplate.exchange(
            "/leave/requests/" + request.id + "/review",
            HttpMethod.PATCH,
            new HttpEntity<>(LeaveTestFixtures.approveRequest(), hrHeaders()),
            LeaveRequestResponse.class);
    assertThat(reviewByHr.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Only HR/Admin roles can list all leave requests")
  void roleEnforcement_onlyHrAdminCanListAll() {
    // Employee attempts to list all requests
    ResponseEntity<ApiErrorResponse> listByEmp =
        restTemplate.exchange(
            "/leave/requests",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders("someemp")),
            ApiErrorResponse.class);
    assertThat(listByEmp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // Admin attempts to list all requests
    ResponseEntity<Map> listByAdmin =
        restTemplate.exchange(
            "/leave/requests", HttpMethod.GET, new HttpEntity<>(adminHeaders()), Map.class);
    assertThat(listByAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Gracefully fails with 503 Service Unavailable when employee-service is offline")
  void employeeServiceDown_applyFailsGracefully() {
    // Simulate Peer Connection Failure via WireMock fault injection
    employeeServiceMock.stubFor(
        get(urlPathEqualTo("/employees/lookup"))
            .withQueryParam("authUsername", equalTo("downemployee"))
            .willReturn(
                aResponse()
                    .withFault(
                        com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");
    ApplyLeaveRequest applyReq =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Vacation");

    ResponseEntity<ApiErrorResponse> resp =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(applyReq, employeeHeaders("downemployee")),
            ApiErrorResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().message()).contains("Downstream service is offline");
  }

  @Test
  @DisplayName("Server computes number of days correctly across various date ranges")
  void numberOfDaysCalculation_variousRanges() {
    UUID employeeId = UUID.randomUUID();
    String username = "testemployee9";
    stubEmployeeLookup(username, employeeId, "Hitesh", "L");
    stubEmployeeGet(employeeId, "Hitesh", "L");

    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    // Case 1: Same day leave (1 day)
    LocalDate day1 = LocalDate.now().plusDays(1);
    ApplyLeaveRequest req1 =
        LeaveTestFixtures.validApplyRequest(leaveTypeId, day1, day1, "Same day");
    ResponseEntity<LeaveRequestResponse> resp1 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req1, employeeHeaders(username)),
            LeaveRequestResponse.class);
    assertThat(resp1.getBody().numberOfDays).isEqualTo(1);

    // Case 2: Multi-week leave (10 days inclusive)
    LocalDate start = LocalDate.now().plusDays(5);
    LocalDate end = start.plusDays(9);
    ApplyLeaveRequest req2 =
        LeaveTestFixtures.validApplyRequest(leaveTypeId, start, end, "10 days");
    ResponseEntity<LeaveRequestResponse> resp2 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req2, employeeHeaders(username)),
            LeaveRequestResponse.class);
    assertThat(resp2.getBody().numberOfDays).isEqualTo(10);
  }

  @Test
  @DisplayName("Invalid request payload inputs return 400 Bad Request")
  void validation_failures() {
    UUID leaveTypeId = getLeaveTypeIdByCode("AL");

    // 1. Missing leaveTypeId
    ApplyLeaveRequest req1 =
        LeaveTestFixtures.validApplyRequest(
            null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "No type");
    ResponseEntity<ApiErrorResponse> resp1 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req1, employeeHeaders("emp")),
            ApiErrorResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 2. End date before start date
    ApplyLeaveRequest req2 =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().plusDays(3), LocalDate.now().plusDays(1), "Bad dates");
    ResponseEntity<ApiErrorResponse> resp2 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req2, employeeHeaders("emp")),
            ApiErrorResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resp2.getBody().message()).contains("End date must be on or after start date");

    // 3. Past start date
    ApplyLeaveRequest req3 =
        LeaveTestFixtures.validApplyRequest(
            leaveTypeId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), "Past date");
    ResponseEntity<ApiErrorResponse> resp3 =
        restTemplate.postForEntity(
            "/leave/requests",
            new HttpEntity<>(req3, employeeHeaders("emp")),
            ApiErrorResponse.class);
    assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resp3.getBody().message()).contains("startDate cannot be in the past");
  }
}
