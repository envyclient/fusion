package com.envyclient.fusion;

import com.envyclient.fusion.injection.InjectionConfiguration;
import com.envyclient.fusion.injection.hook.ClassHook;
import me.mat.jprocessor.JProcessor;
import me.mat.jprocessor.jar.MemoryJar;
import me.mat.jprocessor.jar.clazz.MemoryClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Fusion {

    private final List<InjectionConfiguration> configurationList = new ArrayList<>();
    private final List<ClassHook> classHooks = new ArrayList<>();

    private final MemoryJar memoryJar;

    public Fusion(Map<String, byte[]> data, Map<String, String> configurations) {
        this.memoryJar = JProcessor.Jar.load(data, "net.minecraft.client.main.Main");

        // load all the injection configurations
        configurations.forEach((name, configuration) -> configurationList.add(InjectionConfiguration.load(configuration)));

        // loop through all the hook definitions
        configurationList.forEach(injectionConfiguration
                -> Stream.of(injectionConfiguration.hookDefinitions).forEach(definition -> {

            // get the class of the definition
            MemoryClass definitionClass = memoryJar.getClass(definition);

            // if the class is valid
            if (definitionClass != null) {

                // add a new class hook
                classHooks.add(new ClassHook(definitionClass, memoryJar));
            }
        }));

        // transform all the hooks classes
        classHooks.forEach(ClassHook::transform);
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
