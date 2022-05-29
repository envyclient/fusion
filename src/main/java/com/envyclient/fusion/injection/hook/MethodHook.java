package com.envyclient.fusion.injection.hook;

import com.envyclient.fusion.injection.hook.manifest.At;
import lombok.NonNull;
import me.mat.jprocessor.jar.cls.MemoryClass;
import me.mat.jprocessor.jar.cls.MemoryMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.concurrent.ThreadLocalRandom;

public class MethodHook {

    @NonNull
    private final MemoryClass memoryClass;

    @NonNull
    private final ClassHook classHook;

    @NonNull
    private final String method;

    @NonNull
    private final At at;

    @NonNull
    private final MemoryMethod wrapperMethod;

    private String targetOwner;
    private String targetName;
    private String targetDescription;

    @NonNull
    private final int line;

    public MethodHook(@NonNull MemoryMethod definitionMethod, @NonNull MemoryClass memoryClass, @NonNull ClassHook classHook, @NonNull String method, @NonNull At at, @NonNull String description, String instruction, @NonNull int line) {
        this.memoryClass = memoryClass;
        this.classHook = classHook;
        this.method = method;
        this.at = at;
        this.line = line;

        String inst = instruction;
        if (inst != null) {
            if (inst.startsWith("this.")) {
                inst = inst.substring(5);
            }

            String[] data = inst.split("\\.");
            this.targetOwner = data[0];
            this.targetName = data[1].substring(0, data[1].indexOf("("));
            this.targetDescription = data[1].substring(data[1].indexOf("("));

            System.out.println("-----");
            System.out.println("owner: " + targetOwner);
            System.out.println("name: " + targetName);
            System.out.println("description: " + targetDescription);
            System.out.println("-----");
        }

        // inject the wrapper method into the class
        this.wrapperMethod = classHook.injectMethod(
                Opcodes.ACC_PUBLIC,
                method + "Hook" + at + Math.abs(ThreadLocalRandom.current().nextInt(256)),
                description,
                null,
                null
        );

        // clear all the instructions in the wrapper method
        this.wrapperMethod.getInstructions().clear();

        // loop through all the instructions in the definition method
        for (AbstractInsnNode abstractInsnNode : definitionMethod.getInstructions()) {

            // copy the instruction over into the wrapper method
            this.wrapperMethod.getInstructions().add(abstractInsnNode);
        }

        // hook the method
        this.hook();
    }

    private void hook() {
        MemoryMethod memoryMethod = getMethodToHook();
        if (at.equals(At.TAIL)) {
            InsnList instructions = memoryMethod.getInstructions();
            instructions.insertBefore(
                    instructions.get(findHookPoint(memoryMethod) + 1),
                    new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            classHook.getHookClass().name(),
                            wrapperMethod.name(),
                            wrapperMethod.description()
                    )
            );
            System.out.println("[INFO]: Hook injected");
        }
    }

    private MemoryMethod getMethodToHook() {
        String name = method.substring(0, method.indexOf("("));
        String description = method.substring(method.indexOf("(") + 1);
        for (MemoryMethod memoryMethod : classHook.getHookClass().methods) {
            if (memoryMethod.name().equals(name) && memoryMethod.description().equals(description)) {
                return memoryMethod;
            }
        }
        throw new RuntimeException(classHook.getHookClass().name() + " does not contain method '" + method + "'");
    }

    private int findHookPoint(MemoryMethod method) {
        boolean isOverLine = false;
        InsnList instructions = method.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode instruction = instructions.get(i);
            if (instruction instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) instruction;
                if (lineNumberNode.line >= line) {
                    isOverLine = true;
                }
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                if (methodInsnNode.owner.endsWith(targetOwner)
                        && methodInsnNode.name.equals(targetName)
                        && methodInsnNode.desc.equals(targetDescription)
                        && isOverLine) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Failed to find the hooking point");
    }

}
