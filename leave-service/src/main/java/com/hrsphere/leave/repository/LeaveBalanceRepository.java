package com.hrsphere.leave.repository;

import com.hrsphere.leave.entity.LeaveBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

  Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(
      UUID employeeId, UUID leaveTypeId, Integer year);

  List<LeaveBalance> findAllByEmployeeIdAndYear(UUID employeeId, Integer year);
}
