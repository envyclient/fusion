package com.envyclient.fusion.injection.hook;

import com.envyclient.fusion.injection.hook.manifest.At;
import com.envyclient.fusion.injection.hook.manifest.Definition;
import com.envyclient.fusion.injection.hook.manifest.Hook;
import com.envyclient.fusion.injection.hook.manifest.HookAt;
import lombok.Getter;
import lombok.NonNull;
import me.mat.jprocessor.jar.MemoryJar;
import me.mat.jprocessor.jar.clazz.MemoryAnnotation;
import me.mat.jprocessor.jar.clazz.MemoryClass;
import me.mat.jprocessor.jar.clazz.MemoryField;
import me.mat.jprocessor.jar.clazz.MemoryMethod;
import me.mat.jprocessor.transformer.ClassTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassHook implements ClassTransformer {

    private final Map<String, HookDefinition> hookDefinitions = new HashMap<>();

    private final List<MethodHook> hooks = new ArrayList<>();

    @NonNull
    private final MemoryClass hookDefinitionClass;

    @NonNull
    private final MemoryJar memoryJar;

    @NonNull
    @Getter
    private final MemoryClass hookClass;

    public ClassHook(@NonNull MemoryClass hookDefinitionClass, @NonNull MemoryJar memoryJar) {
        this.hookDefinitionClass = hookDefinitionClass;
        this.memoryJar = memoryJar;

        // get the class that needs to be hooked
        this.hookClass = getHookClass(memoryJar);

        // load all the hook definitions
        this.loadHookDefinitions(memoryJar);

        // load all the method hooks
        this.loadHooks();
    }

    @Override
    public void transform(MemoryClass memoryClass) {

    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryField memoryField) {
    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod) {
        // transform all the method hooks
        hooks.forEach(methodHook -> methodHook.transform(memoryClass, memoryMethod));
    }

    /**
     * Transforms the hook class and injects all the new data
     */

    public void transform() {
        // transform the hook class
        this.hookClass.transform(this);
    }

    /**
     * Gets the class that needs to be hooked
     *
     * @param memoryJar memory jar that you want to get the class from
     * @return {@link MemoryClass}
     */

    private MemoryClass getHookClass(MemoryJar memoryJar) {
        // check if the definition class has the hook annotation
        if (hookDefinitionClass.isAnnotationPresent(Hook.class)) {

            // get the annotation from the class
            MemoryAnnotation memoryAnnotation = hookDefinitionClass.getAnnotation(Hook.class);

            // if the annotation is valid
            if (memoryAnnotation != null) {

                // get the value of the annotation aka the class name of the class that will be hooked
                String className = memoryAnnotation.value().toString();

                // fix up the class name
                className = className.substring(1, className.length() - 1);

                // get the class that will be hooked
                MemoryClass hookClass = memoryJar.getClass(className);

                // if the class was found
                if (hookClass != null) {

                    // return the hook class
                    return hookClass;
                }

                // if the class was not found throw an exception
                throw new RuntimeException(className + " is not a valid class");
            }
        }

        // if the hook definition class does not have a hook annotation throw an exception
        throw new RuntimeException(hookDefinitionClass.getClass().getName() + " does not have a Hook annotation");
    }

    private void loadHookDefinitions(MemoryJar memoryJar) {
        hookDefinitionClass.methods.forEach(memoryMethod -> {
            if (memoryMethod.isAnnotationPresent(Definition.class)) {
                MemoryAnnotation memoryAnnotation = memoryMethod.getAnnotation(Definition.class);
                if (memoryAnnotation.hasValue("name")) {
                    String type = getValue(memoryAnnotation, "type").toString();
                    type = type.substring(1, type.length() - 1);
                    type = type.replaceAll("\\. ", "/");
                    hookDefinitions.put(
                            getValue(memoryAnnotation, "name").toString(),
                            new HookDefinition(
                                    memoryJar.getClass(type),
                                    memoryMethod.name(),
                                    memoryMethod.description(),
                                    Integer.parseInt(getValue(memoryAnnotation, "line").toString())
                            ));
                }
            }
        });
    }

    /**
     * Loads all the hook definitions
     */

    private void loadHooks() {
        // loop through all the methods
        hookDefinitionClass.methods.forEach(memoryMethod -> {

            // check that the method has the hook at annotation
            if (memoryMethod.isAnnotationPresent(HookAt.class)) {

                // get the hook at annotation
                MemoryAnnotation memoryAnnotation = memoryMethod.getAnnotation(HookAt.class);

                // check that the annotation is valid
                if (memoryAnnotation != null) {

                    // get the value of the at key
                    String[] data = (String[]) getValue(memoryAnnotation, "at");

                    // add the hook to the list of all the hooks
                    hooks.add(new MethodHook(
                            this,
                            getValue(memoryAnnotation, "method").toString(),
                            At.valueOf(data[1]),
                            hookDefinitions.get(getValue(memoryAnnotation, "definition").toString())
                    ).init(memoryMethod));
                }
            }
        });
    }

    /**
     * Gets a value from an annotation based on the provided key
     *
     * @param memoryAnnotation annotation that you want to get the value from
     * @param key              key of the value
     * @return {@link Object}
     */

    private Object getValue(MemoryAnnotation memoryAnnotation, String key) {
        // if the annotation does not contain the key
        if (!memoryAnnotation.hasValue(key)) {

            // throw an exception
            throw new RuntimeException(memoryAnnotation.description().substring(0, memoryAnnotation.description().length() - 1) + " does not have a '" + key + "'");
        }

        // else just return the value of the key
        return memoryAnnotation.getValue(key);
    }

    /**
     * Injects a method into the class
     * that the hook needs to be placed
     *
     * @param access     access of the method
     * @param name       name of the method
     * @param descriptor descriptor of the method
     * @param signature  signature of the method
     * @param exceptions exception of the method
     * @return {@link MemoryMethod}
     */

    public MemoryMethod injectMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return hookClass.addMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions
        ).init(memoryJar.getClasses());
    }

}
