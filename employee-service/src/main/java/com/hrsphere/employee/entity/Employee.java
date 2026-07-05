package com.hrsphere.employee.entity;

import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "employees",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "employee_code"),
      @UniqueConstraint(columnNames = "email"),
      @UniqueConstraint(columnNames = "auth_username")
    })
public class Employee {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "employee_code", nullable = false, length = 20)
  private String employeeCode;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "auth_username", length = 50)
  private String authUsername;

  @Column(name = "job_title", nullable = false, length = 100)
  private String jobTitle;

  @Enumerated(EnumType.STRING)
  @Column(name = "employment_type", nullable = false, length = 20)
  private EmploymentType employmentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "employment_status", nullable = false, length = 20)
  private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

  @Column(name = "date_of_joining", nullable = false)
  private LocalDate dateOfJoining;

  @Column(name = "date_of_termination")
  private LocalDate dateOfTermination;

  @Column(name = "department_id")
  private UUID departmentId;

  @Column(name = "manager_id")
  private UUID managerId;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 30)
  private Gender gender;

  @Column(name = "nationality", length = 100)
  private String nationality;

  @Embedded private Address address;

  @Column(name = "bank_account_number", length = 50)
  private String bankAccountNumber;

  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Column(name = "tax_id", length = 50)
  private String taxId;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Employee() {}

  // Getters and setters omitted for brevity in this scaffold; implement as needed

  public UUID getId() {
    return id;
  }

  public String getEmployeeCode() {
    return employeeCode;
  }

  public void setEmployeeCode(String employeeCode) {
    this.employeeCode = employeeCode;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAuthUsername() {
    return authUsername;
  }

  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public EmploymentType getEmploymentType() {
    return employmentType;
  }

  public void setEmploymentType(EmploymentType employmentType) {
    this.employmentType = employmentType;
  }

  public EmploymentStatus getEmploymentStatus() {
    return employmentStatus;
  }

  public void setEmploymentStatus(EmploymentStatus employmentStatus) {
    this.employmentStatus = employmentStatus;
  }

  public LocalDate getDateOfJoining() {
    return dateOfJoining;
  }

  public void setDateOfJoining(LocalDate dateOfJoining) {
    this.dateOfJoining = dateOfJoining;
  }

  public LocalDate getDateOfTermination() {
    return dateOfTermination;
  }

  public void setDateOfTermination(LocalDate dateOfTermination) {
    this.dateOfTermination = dateOfTermination;
  }

  public java.util.UUID getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(java.util.UUID departmentId) {
    this.departmentId = departmentId;
  }

  public java.util.UUID getManagerId() {
    return managerId;
  }

  public void setManagerId(java.util.UUID managerId) {
    this.managerId = managerId;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getBankAccountNumber() {
    return bankAccountNumber;
  }

  public void setBankAccountNumber(String bankAccountNumber) {
    this.bankAccountNumber = bankAccountNumber;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
