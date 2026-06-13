package com.chatlycode.architecture.domain;

public record ArchitectureContainer(
        String id,
        String name,
        String description,
        String technology,
        String tag
) {

    public ArchitectureContainer {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        technology = technology == null ? "" : technology;
        tag = tag == null ? "" : tag;
    }
}
