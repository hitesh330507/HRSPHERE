package com.hrsphere.employee.repository;

import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

  Optional<Employee> findByEmployeeCodeAndIsDeletedFalse(String code);

  Optional<Employee> findByEmailAndIsDeletedFalse(String email);

  Optional<Employee> findByAuthUsernameAndIsDeletedFalse(String username);

  Page<Employee> findAllByIsDeletedFalse(Pageable pageable);

  @Query(
      """
    SELECT e FROM Employee e
    WHERE e.isDeleted = false
    AND (:status IS NULL OR e.employmentStatus = :status)
    AND (:departmentId IS NULL OR e.departmentId = :departmentId)
    AND (:employmentType IS NULL OR e.employmentType = :employmentType)
    """)
  Page<Employee> findAllWithFilters(
      @Param("status") EmploymentStatus status,
      @Param("departmentId") UUID departmentId,
      @Param("employmentType") EmploymentType employmentType,
      Pageable pageable);
}
