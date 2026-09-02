package com.hoangluongtran0309.application.port;

import java.util.List;

import com.hoangluongtran0309.domain.model.ArmorDefinition;

public interface ArmorConfigSourcePort {

    List<ArmorDefinition> loadAll();

    void save(ArmorDefinition definition);

    void delete(String id);
}
