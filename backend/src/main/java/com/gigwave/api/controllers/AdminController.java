package com.gigwave.api.controllers;

import com.gigwave.api.dto.admin.AdminDebitRequest;
import com.gigwave.api.dto.admin.AdminDebitResponse;
import com.gigwave.api.dto.admin.CustomerDto;
import com.gigwave.application.admin.AdminService;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<AdminService.CustomerInfo> customers = adminService.getAllCustomers();
        List<CustomerDto> customerDtos = customers.stream()
                .map(c -> CustomerDto.builder()
                        .id(c.getId())
                        .phone(c.getPhone())
                        .email(c.getEmail())
                        .role(c.getRole())
                        .isDisabled(c.getIsDisabled())
                        .createdAt(c.getCreatedAt())
                        .hasActiveMandate(c.getHasActiveMandate())
                        .mandateRef(c.getMandateRef())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(customerDtos);
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable UUID customerId) {
        AdminService.CustomerInfo customer = adminService.getCustomerById(customerId);
        CustomerDto dto = CustomerDto.builder()
                .id(customer.getId())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .role(customer.getRole())
                .isDisabled(customer.getIsDisabled())
                .createdAt(customer.getCreatedAt())
                .hasActiveMandate(customer.getHasActiveMandate())
                .mandateRef(customer.getMandateRef())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/customers/{customerId}/debit")
    public ResponseEntity<AdminDebitResponse> debitCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody AdminDebitRequest request,
            @CurrentUser UUID adminId
    ) {
        AdminService.AdminDebitResult result = adminService.debitCustomer(
                customerId,
                request.getAmount(),
                request.getReason(),
                adminId
        );

        AdminDebitResponse response = AdminDebitResponse.builder()
                .customerId(result.getCustomerId())
                .amount(result.getAmount())
                .status(result.getStatus())
                .transactionRef(result.getTransactionRef())
                .message(result.getMessage())
                .build();

        return ResponseEntity.ok(response);
    }
}
