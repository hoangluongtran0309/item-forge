package com.hoangluongtran0309.application.port;

import java.util.List;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public interface CustomBlockConfigSourcePort {

    List<CustomBlockDefinition> loadAll();

    void save(CustomBlockDefinition definition);

    void delete(String id);
}
