package com.ad.service;

import com.ad.dto.PublisherCreateDTO;
import com.ad.dto.PublisherDTO;
import java.util.List;

public interface PublisherService {
    List<PublisherDTO> listAll();
    PublisherDTO getById(Long id);
    Long create(PublisherCreateDTO dto);
    void update(Long id, PublisherCreateDTO dto);
    void delete(Long id);
}
