package com.hrsphere.department.mapper;

import com.hrsphere.department.dto.*;
import com.hrsphere.department.entity.Department;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

  @Mapping(target = "employeeCount", ignore = true)
  DepartmentResponse toResponse(Department department);

  @Mapping(target = "employeeCount", ignore = true)
  DepartmentSummaryResponse toSummaryResponse(Department department);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "departmentCode", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Department toEntity(CreateDepartmentRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "departmentCode", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(
      UpdateDepartmentRequest request, @MappingTarget Department department);
}
