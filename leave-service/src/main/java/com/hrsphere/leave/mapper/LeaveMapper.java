package com.hrsphere.leave.mapper;

import com.hrsphere.leave.dto.LeaveBalanceResponse;
import com.hrsphere.leave.dto.LeaveRequestResponse;
import com.hrsphere.leave.dto.LeaveRequestSummaryResponse;
import com.hrsphere.leave.entity.LeaveBalance;
import com.hrsphere.leave.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

  @Mapping(target = "leaveTypeId", source = "leaveType.id")
  @Mapping(target = "leaveTypeName", source = "leaveType.name")
  @Mapping(target = "leaveTypeCode", source = "leaveType.code")
  @Mapping(target = "employeeName", ignore = true)
  LeaveRequestResponse toResponse(LeaveRequest request);

  @Mapping(target = "leaveTypeCode", source = "leaveType.code")
  @Mapping(target = "employeeName", ignore = true)
  LeaveRequestSummaryResponse toSummaryResponse(LeaveRequest request);

  @Mapping(target = "leaveTypeName", source = "leaveType.name")
  @Mapping(target = "leaveTypeCode", source = "leaveType.code")
  LeaveBalanceResponse toBalanceResponse(LeaveBalance balance);
}
