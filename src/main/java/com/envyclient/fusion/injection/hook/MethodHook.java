package com.envyclient.fusion.injection.hook;

import com.envyclient.fusion.injection.hook.manifest.At;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.mat.jprocessor.jar.clazz.MemoryClass;
import me.mat.jprocessor.jar.clazz.MemoryInstructions;
import me.mat.jprocessor.jar.clazz.MemoryMethod;
import me.mat.jprocessor.transformer.MethodTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class MethodHook implements MethodTransformer {

    @NonNull
    private final ClassHook classHook;

    @NonNull
    private final String method;

    @NonNull
    private final At at;

    @NonNull
    private final HookDefinition hookDefinition;

    private MemoryMethod wrapperMethod;

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod) {
        // if the current class it not the hook class
        if (!memoryClass.equals(classHook.getHookClass())) {
            // return out of the method
            return;
        }

        // if the method is not the method that needs to be hooked
        if (!isHookMethod(memoryMethod)) {
            // return out of the method
            return;
        }

        // define a new list of instructions
        MemoryInstructions instructions = new MemoryInstructions();

        // add the method invoke
        instructions.addInvoke(wrapperMethod);

        // insert the invoke instruction into the hook method
        memoryMethod.instructions.insertAfter(findHookPoint(memoryMethod), instructions);
    }

    public MethodHook init(MemoryMethod definitionMethod) {
        // inject the wrapper method into the class
        this.wrapperMethod = classHook.injectMethod(
                Opcodes.ACC_PUBLIC,
                definitionMethod.name() + "Hook" + at + Math.abs(ThreadLocalRandom.current().nextInt(256)),
                definitionMethod.description(),
                null,
                null
        );

        // copy over the definition instructions into the new injected method
        definitionMethod.instructions.addInto(this.wrapperMethod.instructions, true);

        // return the handle of this hook
        return this;
    }

    private boolean isHookMethod(MemoryMethod memoryMethod) {
        // define the name and the description
        String name = method;
        String description = "";

        // if the name contains the special character
        if (!name.contains("<")) {

            // get the name of the method
            name = method.substring(0, method.indexOf("("));

            // and get the description of the method
            description = method.substring(method.indexOf("(") + 1);
        }

        // if the provided methods name matches with the fetched name
        if (memoryMethod.name().equals(name)) {

            // check if the description is empty
            if (description.equals("")) {

                // if so return true
                return true;
            }

            // else check that the descriptions match
            return memoryMethod.description().equals(description);
        }

        // if nothing was matched return false out of the method
        return false;
    }

    private AbstractInsnNode findHookPoint(MemoryMethod method) {
        boolean isOverLine = false;
        InsnList instructions = method.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode instruction = instructions.get(i);
            if (instruction instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) instruction;
                if (lineNumberNode.line >= hookDefinition.line) {
                    isOverLine = true;
                }
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                if (methodInsnNode.owner.endsWith(hookDefinition.typeClass.name())
                        && methodInsnNode.name.equals(hookDefinition.name)
                        && methodInsnNode.desc.equals(hookDefinition.description)
                        && isOverLine) {
                    return methodInsnNode;
                }
            }
        }
        throw new RuntimeException("Failed to find the hooking point");
    }

}
