package com.hrsphere.employee.mapper;

import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.Address;
import com.hrsphere.employee.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

  EmployeeResponse toResponse(Employee employee);

  EmployeeSummaryResponse toSummaryResponse(Employee employee);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employeeCode", ignore = true)
  @Mapping(target = "employmentStatus", ignore = true)
  @Mapping(target = "dateOfTermination", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Employee toEntity(CreateEmployeeRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employeeCode", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "employmentStatus", ignore = true)
  @Mapping(target = "dateOfTermination", ignore = true)
  @Mapping(target = "bankAccountNumber", ignore = true)
  @Mapping(target = "bankName", ignore = true)
  @Mapping(target = "taxId", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(UpdateEmployeeRequest request, @MappingTarget Employee employee);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employeeCode", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "firstName", ignore = true)
  @Mapping(target = "lastName", ignore = true)
  @Mapping(target = "authUsername", ignore = true)
  @Mapping(target = "jobTitle", ignore = true)
  @Mapping(target = "employmentType", ignore = true)
  @Mapping(target = "employmentStatus", ignore = true)
  @Mapping(target = "dateOfJoining", ignore = true)
  @Mapping(target = "dateOfTermination", ignore = true)
  @Mapping(target = "departmentId", ignore = true)
  @Mapping(target = "managerId", ignore = true)
  @Mapping(target = "dateOfBirth", ignore = true)
  @Mapping(target = "gender", ignore = true)
  @Mapping(target = "nationality", ignore = true)
  @Mapping(target = "bankAccountNumber", ignore = true)
  @Mapping(target = "bankName", ignore = true)
  @Mapping(target = "taxId", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateOwnProfile(UpdateOwnProfileRequest request, @MappingTarget Employee employee);

  AddressDto toAddressDto(Address address);

  Address toAddress(AddressDto dto);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateAddressFromDto(AddressDto dto, @MappingTarget Address address);
}
