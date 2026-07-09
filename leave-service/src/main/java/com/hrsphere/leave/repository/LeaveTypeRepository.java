package com.hrsphere.leave.repository;

import com.hrsphere.leave.entity.LeaveType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
  Optional<LeaveType> findByCode(String code);
}
