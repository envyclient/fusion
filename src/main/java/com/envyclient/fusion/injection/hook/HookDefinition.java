package com.envyclient.fusion.injection.hook;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.mat.jprocessor.jar.clazz.MemoryClass;

@RequiredArgsConstructor
public class HookDefinition {

    @NonNull
    public final MemoryClass typeClass;

    @NonNull
    public final String name;

    @NonNull
    public final String description;

    @NonNull
    public final int line;

}
