package com.noh.zup.domain.tag;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(TagResponse::from)
                .toList();
    }
}
