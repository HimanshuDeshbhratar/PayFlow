package com.payflow.customer.service;

import com.payflow.auth.security.UserPrincipal;
import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.CustomerStatus;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.customer.dto.CreateCustomerRequest;
import com.payflow.customer.dto.CustomerResponse;
import com.payflow.customer.entity.Customer;
import com.payflow.customer.repository.CustomerRepository;
import com.payflow.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantService merchantService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID merchantId, Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        Page<Customer> page;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            page = merchantId != null
                    ? customerRepository.findByMerchantId(merchantId, pageable)
                    : customerRepository.findAll(pageable);
        } else {
            UUID mid = user.getMerchantId();
            if (mid == null) {
                throw PayFlowException.forbidden("No merchant association");
            }
            page = customerRepository.findByMerchantId(mid, pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Customer not found"));
        merchantService.assertCanAccess(customer.getMerchantId());
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        merchantService.assertCanAccess(request.merchantId());
        merchantService.requireMerchant(request.merchantId());
        if (customerRepository.existsByMerchantIdAndEmailIgnoreCase(request.merchantId(), request.email())) {
            throw PayFlowException.conflict("Customer email already exists for merchant");
        }
        Customer customer = Customer.builder()
                .merchantId(request.merchantId())
                .email(request.email().trim().toLowerCase())
                .fullName(request.fullName().trim())
                .phone(request.phone())
                .countryCode(request.countryCode())
                .metadata(request.metadata())
                .status(CustomerStatus.ACTIVE)
                .blocked(false)
                .build();
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public Customer blockCustomer(UUID customerId, String reason) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> PayFlowException.notFound("Customer not found"));
        customer.setBlocked(true);
        customer.setStatus(CustomerStatus.BLOCKED);
        customer.setBlockedReason(reason);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer markSafe(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> PayFlowException.notFound("Customer not found"));
        customer.setBlocked(false);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setBlockedReason(null);
        return customerRepository.save(customer);
    }

    public Customer requireCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Customer not found"));
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getMerchantId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getCountryCode(),
                customer.getStatus(),
                customer.isBlocked(),
                customer.getBlockedReason(),
                customer.getMetadata(),
                customer.getCreatedAt()
        );
    }
}
