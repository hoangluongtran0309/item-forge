package com.hoangluongtran0309.application.port;

import java.util.Optional;

public interface ResourcePackPort {

    void rebuild();

    Optional<ResourcePackInfo> currentPack();
}
