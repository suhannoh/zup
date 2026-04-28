package com.noh.zup.domain.benefit;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBenefitDetailItemService {

    private final BenefitRepository benefitRepository;
    private final BenefitDetailItemRepository benefitDetailItemRepository;

    public AdminBenefitDetailItemService(
            BenefitRepository benefitRepository,
            BenefitDetailItemRepository benefitDetailItemRepository
    ) {
        this.benefitRepository = benefitRepository;
        this.benefitDetailItemRepository = benefitDetailItemRepository;
    }

    @Transactional(readOnly = true)
    public List<BenefitDetailItemResponse> getItems(Long benefitId) {
        ensureBenefitExists(benefitId);
        return benefitDetailItemRepository.findAllByBenefitIdOrderByDisplayOrderAscIdAsc(benefitId).stream()
                .map(BenefitDetailItemResponse::from)
                .toList();
    }

    @Transactional
    public BenefitDetailItemResponse createItem(Long benefitId, BenefitDetailItemRequest request) {
        Benefit benefit = benefitRepository.findById(benefitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));
        if (!StringUtils.hasText(request.title())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Detail item title is required");
        }
        BenefitDetailItem item = new BenefitDetailItem(
                benefit,
                normalize(request.brandName()),
                request.title().trim(),
                normalize(request.description()),
                normalize(request.conditionText()),
                normalize(request.imageUrl()),
                request.displayOrder()
        );
        return BenefitDetailItemResponse.from(benefitDetailItemRepository.save(item));
    }

    @Transactional
    public BenefitDetailItemResponse updateItem(Long itemId, BenefitDetailItemRequest request) {
        BenefitDetailItem item = getItem(itemId);
        if (request.title() != null && !StringUtils.hasText(request.title())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Detail item title is required");
        }
        item.update(
                normalize(request.brandName()),
                request.title() == null ? null : request.title().trim(),
                normalize(request.description()),
                normalize(request.conditionText()),
                normalize(request.imageUrl()),
                request.displayOrder()
        );
        return BenefitDetailItemResponse.from(item);
    }

    @Transactional
    public BenefitDetailItemResponse updateActive(Long itemId, Boolean isActive) {
        BenefitDetailItem item = getItem(itemId);
        item.changeActive(isActive);
        return BenefitDetailItemResponse.from(item);
    }

    private void ensureBenefitExists(Long benefitId) {
        if (!benefitRepository.existsById(benefitId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found");
        }
    }

    private BenefitDetailItem getItem(Long itemId) {
        return benefitDetailItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "BenefitDetailItem not found"));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
