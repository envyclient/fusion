package com.envyclient.fusion.injection.hook;

import com.envyclient.fusion.injection.hook.manifest.At;
import com.envyclient.fusion.injection.hook.manifest.Hook;
import com.envyclient.fusion.injection.hook.manifest.HookAt;
import lombok.Getter;
import lombok.NonNull;
import me.mat.jprocessor.jar.MemoryJar;
import me.mat.jprocessor.jar.cls.MemoryAnnotation;
import me.mat.jprocessor.jar.cls.MemoryClass;
import me.mat.jprocessor.jar.cls.MemoryField;
import me.mat.jprocessor.jar.cls.MemoryMethod;
import me.mat.jprocessor.transformer.ClassTransformer;

import java.util.ArrayList;
import java.util.List;

public class ClassHook implements ClassTransformer {

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
        this.hookClass = getHookClass(memoryJar);

        // load all the method hooks
        this.loadHooks();
    }

    public MemoryMethod injectMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return hookClass.addMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions
        ).init(memoryJar.getClasses());
    }

    private void loadHooks() {
        // loop through all the methods
        hookDefinitionClass.methods.forEach(memoryMethod -> {

            // check that the method has the hook at annotation
            if (memoryMethod.isAnnotationPresent(HookAt.class)) {

                // get the hook at annotation
                MemoryAnnotation memoryAnnotation = memoryMethod.getAnnotation(HookAt.class);

                // check that the annotation is valid
                if (memoryAnnotation != null) {

                    // add the hook to the list of all the hooks
                    hooks.add(new MethodHook(
                            memoryMethod,
                            hookClass,
                            this,
                            getValue(memoryAnnotation, "method"),
                            At.valueOf(getValue(memoryAnnotation, "at")),
                            memoryMethod.description(),
                            getValue(memoryAnnotation, "instruction"),
                            Integer.parseInt(getValue(memoryAnnotation, "line"))
                    ));
                }

                // else throw an exception
                throw new RuntimeException("Failed to get the memory annotation");
            }
        });
    }

    private String getValue(MemoryAnnotation memoryAnnotation, String key) {
        // if the annotation does not contain the key
        if (!memoryAnnotation.hasValue(key)) {

            // throw an exception
            throw new RuntimeException(memoryAnnotation.description().substring(0, memoryAnnotation.description().length() - 1) + " does not have a '" + key + "'");
        }

        // else just return the value of the key
        return memoryAnnotation.getValue(key).toString();
    }

    private MemoryClass getHookClass(MemoryJar memoryJar) {
        // check if the definition class has the hook annotation
        if (hookDefinitionClass.isAnnotationPresent(Hook.class)) {

            // get the annotation from the class
            MemoryAnnotation memoryAnnotation = hookDefinitionClass.getAnnotation(Hook.class);

            // if the annotation is valid
            if (memoryAnnotation != null) {

                // get the value of the annotation aka the class name of the class that will be hooked
                String className = memoryAnnotation.value().toString();

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

            // if the annotation was not found throw an exception
            throw new RuntimeException("Failed to get the annotation from the class");
        }

        // if the hook definition class does not have a hook annotation throw an exception
        throw new RuntimeException(hookDefinitionClass.getClass().getName() + " does not have a Hook annotation");
    }

    @Override
    public void transform(MemoryClass memoryClass) {

    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryField memoryField) {

    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod) {

    }

}
