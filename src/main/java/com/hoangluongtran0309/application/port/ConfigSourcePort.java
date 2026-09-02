package com.hoangluongtran0309.application.port;

import java.util.List;

import com.hoangluongtran0309.domain.model.ItemDefinition;

public interface ConfigSourcePort {

    List<ItemDefinition> loadAll();

    void save(ItemDefinition definition);

    void delete(String id);
}
