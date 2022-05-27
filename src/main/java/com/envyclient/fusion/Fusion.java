package com.envyclient.fusion;

import com.envyclient.fusion.injection.InjectionConfiguration;
import com.envyclient.fusion.injection.manifest.Hook;
import me.mat.jprocessor.JProcessor;
import me.mat.jprocessor.jar.MemoryJar;
import me.mat.jprocessor.jar.cls.MemoryAnnotation;
import me.mat.jprocessor.jar.cls.MemoryClass;
import me.mat.jprocessor.jar.cls.MemoryField;
import me.mat.jprocessor.jar.cls.MemoryMethod;
import me.mat.jprocessor.transformer.ClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Fusion implements ClassTransformer {

    private final List<InjectionConfiguration> configurationList = new ArrayList<>();
    private final List<String> hookTargets = new ArrayList<>();

    private final MemoryJar memoryJar;

    public Fusion(Map<String, byte[]> data, Map<String, String> configurations) {
        this.memoryJar = JProcessor.Jar.load(data);

        // load all the injection configurations
        configurations.forEach((name, configuration) -> configurationList.add(InjectionConfiguration.load(configuration)));

        // load all the hooks
        configurationList.forEach(injectionConfiguration
                -> Stream.of(injectionConfiguration.hookDefinitions).forEach(definition
                -> loadHooks(definition.className, memoryJar.getClass(definition.className))));

        // transform all the hook targets
        hookTargets.stream().map(memoryJar::getClass).forEach(memoryClass -> memoryClass.transform(this));
    }

    private void loadHooks(String className, MemoryClass memoryClass) {
        // if the provided class is invalid
        if (memoryClass == null) {
            // throw an exception
            throw new RuntimeException("Invalid hooks class: " + className);
        }
        if (memoryClass.isAnnotationPresent(Hook.class)) {
            MemoryAnnotation annotation = memoryClass.getAnnotation(Hook.class);
            String value = annotation.value().toString();
            if (value != null) {
                hookTargets.add(value);
            }
        }
    }

    /**
     * Exports all the classes to a map
     *
     * @return {@link Map}
     */

    public Map<String, byte[]> export() {
        return memoryJar.exportClasses();
    }

    @Override
    public void transform(MemoryClass memoryClass) {

    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryField memoryField) {

    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod) {
        if (memoryClass.name().equals("net/minecraft/client/Minecraft")) {
            System.out.println("> Found Minecraft");
            if (memoryMethod.name().equals("startAttack")) {
                System.out.println("> Found startAttack");

                InsnList instructions = memoryMethod.getInstructions();
                instructions.insert(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/envyclient/sdk/SDK",
                        "init",
                        "()V"
                ));
            }
        }
    }
}
