package com.hrsphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public class AddressDto {
  @Size(max = 255)
  @Schema(example = "123 MG Road")
  public String street;

  @Size(max = 100)
  @Schema(example = "Bengaluru")
  public String city;

  @Size(max = 100)
  @Schema(example = "Karnataka")
  public String state;

  @Size(max = 20)
  @Schema(example = "560001")
  public String postalCode;

  @Size(max = 100)
  @Schema(example = "India")
  public String country;
}
