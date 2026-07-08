package com.hrsphere.department.repository;

import com.hrsphere.department.entity.Department;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

  Optional<Department> findByDepartmentCodeAndIsDeletedFalse(String code);

  Optional<Department> findByNameAndIsDeletedFalse(String name);

  Page<Department> findAllByIsDeletedFalse(Pageable pageable);
}
