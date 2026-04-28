package com.noh.zup.domain.tag;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTagService {

    private final TagRepository tagRepository;

    public AdminTagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTagResponse> getTags() {
        return tagRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(AdminTagResponse::from)
                .toList();
    }

    @Transactional
    public AdminTagResponse createTag(AdminTagCreateRequest request) {
        if (tagRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tag slug already exists");
        }

        Tag tag = new Tag(request.name(), request.slug(), request.displayOrder());
        tag.update(null, null, null, request.isActive());
        return AdminTagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public AdminTagResponse updateTag(Long id, AdminTagUpdateRequest request) {
        Tag tag = getTag(id);
        if (request.slug() != null && tagRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tag slug already exists");
        }

        tag.update(request.name(), request.slug(), request.displayOrder(), request.isActive());
        return AdminTagResponse.from(tag);
    }

    @Transactional
    public AdminTagResponse updateActive(Long id, Boolean isActive) {
        Tag tag = getTag(id);
        tag.changeActive(isActive);
        return AdminTagResponse.from(tag);
    }

    private Tag getTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Tag not found"));
    }
}
