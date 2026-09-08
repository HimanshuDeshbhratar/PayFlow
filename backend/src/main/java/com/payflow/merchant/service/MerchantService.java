package com.payflow.merchant.service;

import com.payflow.auth.security.UserPrincipal;
import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.merchant.dto.CreateMerchantRequest;
import com.payflow.merchant.dto.MerchantResponse;
import com.payflow.merchant.entity.Merchant;
import com.payflow.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public PageResponse<MerchantResponse> list(Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        Page<Merchant> page;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            page = merchantRepository.findAll(pageable);
        } else if (user.getMerchantId() != null) {
            Merchant merchant = merchantRepository.findById(user.getMerchantId())
                    .orElseThrow(() -> PayFlowException.notFound("Merchant not found"));
            page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(merchant), pageable, 1);
        } else {
            throw PayFlowException.forbidden("No merchant association");
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public MerchantResponse get(UUID id) {
        assertCanAccess(id);
        return toResponse(merchantRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Merchant not found")));
    }

    @Transactional
    public MerchantResponse create(CreateMerchantRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw PayFlowException.forbidden("Only ADMIN can create merchants");
        }
        Merchant merchant = Merchant.builder()
                .businessName(request.businessName())
                .legalName(request.legalName())
                .countryCode(request.countryCode() != null ? request.countryCode() : "US")
                .currency(request.currency() != null ? request.currency() : "USD")
                .build();
        return toResponse(merchantRepository.save(merchant));
    }

    public Merchant requireMerchant(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Merchant not found"));
    }

    public void assertCanAccess(UUID merchantId) {
        UserPrincipal user = SecurityUtils.currentUser();
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            return;
        }
        if (user.getMerchantId() == null || !user.getMerchantId().equals(merchantId)) {
            throw PayFlowException.forbidden("Access denied to merchant");
        }
    }

    public MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getBusinessName(),
                merchant.getLegalName(),
                merchant.getStatus(),
                merchant.getCountryCode(),
                merchant.getCurrency(),
                merchant.getSettlementStatus(),
                merchant.getCreatedAt()
        );
    }
}
