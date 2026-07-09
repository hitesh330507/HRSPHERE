package com.hrsphere.leave.repository;

import com.hrsphere.leave.entity.LeaveRequest;
import com.hrsphere.leave.entity.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

  Page<LeaveRequest> findAllByEmployeeId(UUID employeeId, Pageable pageable);

  Page<LeaveRequest> findAllByStatus(LeaveStatus status, Pageable pageable);

  @Query(
      "SELECT COUNT(r) > 0 FROM LeaveRequest r "
          + "WHERE r.employeeId = :employeeId "
          + "AND r.status IN (com.hrsphere.leave.entity.enums.LeaveStatus.PENDING, com.hrsphere.leave.entity.enums.LeaveStatus.APPROVED) "
          + "AND :startDate <= r.endDate "
          + "AND :endDate >= r.startDate")
  boolean hasOverlappingRequest(
      @Param("employeeId") UUID employeeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT COUNT(r) > 0 FROM LeaveRequest r "
          + "WHERE r.employeeId = :employeeId "
          + "AND r.id <> :excludeRequestId "
          + "AND r.status IN (com.hrsphere.leave.entity.enums.LeaveStatus.PENDING, com.hrsphere.leave.entity.enums.LeaveStatus.APPROVED) "
          + "AND :startDate <= r.endDate "
          + "AND :endDate >= r.startDate")
  boolean hasOverlappingRequestExcluding(
      @Param("employeeId") UUID employeeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("excludeRequestId") UUID excludeRequestId);
}
