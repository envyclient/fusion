package com.envyclient.fusion;

import com.envyclient.fusion.injection.InjectionConfiguration;
import me.mat.jprocessor.JProcessor;
import me.mat.jprocessor.jar.MemoryJar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Fusion {

    private final List<InjectionConfiguration> configurationList = new ArrayList<>();

    private final MemoryJar memoryJar;

    public Fusion(Map<String, byte[]> data, Map<String, String> configurations) {
        this.memoryJar = JProcessor.Jar.load(data);

        // load all the injection configurations
        configurations.forEach((name, configuration) -> configurationList.add(InjectionConfiguration.load(configuration)));

        configurationList.forEach(injectionConfiguration -> {

        });
    }

    /**
     * Exports all the classes to a map
     *
     * @return {@link Map}
     */

    public Map<String, byte[]> export() {
        return memoryJar.exportClasses();
    }

}
