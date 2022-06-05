package com.envyclient.fusion.injection.hook;

import com.envyclient.fusion.injection.hook.manifest.At;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.mat.jprocessor.jar.memory.MemoryClass;
import me.mat.jprocessor.jar.memory.MemoryInstructions;
import me.mat.jprocessor.jar.memory.MemoryMethod;
import me.mat.jprocessor.transformer.MethodTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class MethodHook implements MethodTransformer {

    @NonNull
    private final ClassHook classHook;

    @NonNull
    private final String method;

    @NonNull
    private final At at;

    private final HookDefinition hookDefinition;

    private MemoryMethod wrapperMethod;

    private boolean isInjected;

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod) {
        // if the definition has an instruction as a target
        if (hookDefinition != null || isInjected) {
            // return out of the method
            return;
        }

        // if the method is not the method that needs to be hooked
        if (!isHookMethod(memoryMethod)) {
            // return out of the method
            return;
        }

        // inject the hook
        injectHook(memoryMethod, null);
    }

    @Override
    public void transform(MemoryClass memoryClass, MemoryMethod memoryMethod,
                          MemoryInstructions memoryInstructions, AbstractInsnNode abstractInsnNode) {
        // if the definition does not have an instruction as a target
        if (hookDefinition == null || isInjected) {
            // return out of the method
            return;
        }

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

        // if the node does not match the definition
        if (!hookDefinition.isInstruction(abstractInsnNode)) {
            // return out of the method
            return;
        }

        // inject the hook
        injectHook(memoryMethod, abstractInsnNode);
    }

    /**
     * Initializes the method hook
     *
     * @param definitionMethod definition of the method that will be copied over
     * @return {@link MethodHook}
     */

    public MethodHook init(MemoryMethod definitionMethod) {
        // inject the wrapper method into the class
        wrapperMethod = classHook.injectMethod(
                Opcodes.ACC_PUBLIC,
                definitionMethod.name() + "Hook" + at + Math.abs(ThreadLocalRandom.current().nextInt(256)),
                definitionMethod.description(),
                null,
                null
        );

        // copy over the definition instructions into the new injected method
        definitionMethod.instructions.addInto(wrapperMethod.instructions, true);

        for (int i = 0; i < wrapperMethod.instructions.size(); i++) {
            AbstractInsnNode instruction = wrapperMethod.instructions.get(i);
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                if (fieldInsnNode.owner.equals(definitionMethod.parent.name())) {
                    if (!fieldInsnNode.owner.equals(classHook.getHookClass().name())) {
                        fieldInsnNode.owner = classHook.getHookClass().name();
                    }
                }
            }
        }

        // clear all the definition instructions
        definitionMethod.instructions.clear();

        // return the handle of this hook
        return this;
    }

    /**
     * Injects a hook into the method at the instruction
     *
     * @param memoryMethod     method that you want to hook
     * @param abstractInsnNode instruction that you want to hook at
     */

    private void injectHook(MemoryMethod memoryMethod, AbstractInsnNode abstractInsnNode) {
        // define a new list of instructions
        MemoryInstructions instructions = new MemoryInstructions();

        // if the injection point is at the head and the instruction is not provided
        if (at == At.HEAD && abstractInsnNode == null) {

            // insert the method invoke
            instructions.insertInvoke(wrapperMethod);

            // insert the hook instruction
            memoryMethod.instructions.insert(instructions);

            // update the injected flag
            isInjected = true;

            // and return out of the method
            return;
        } else if (at == At.TAIL && abstractInsnNode == null) {

            // add the method invoke
            instructions.addInvoke(wrapperMethod);

            // insert the hook instructions before the last instruction
            memoryMethod.instructions.insertBefore(memoryMethod.instructions.get(memoryMethod.instructions.size() - 2), instructions);

            // update the injected flag
            isInjected = true;

            // and return out of the method
            return;
        }

        // add the method invoke
        instructions.addInvoke(wrapperMethod);

        // if the injection point is at the head
        if (at == At.HEAD) {
            // find the current label node
            AbstractInsnNode labelNode = getCurrentLabel(memoryMethod, abstractInsnNode);

            // insert the invoke instruction before the label node
            memoryMethod.instructions.insertBefore(labelNode, instructions);
        } else {
            // insert the invoke instruction into the hook method
            memoryMethod.instructions.insertAfter(abstractInsnNode, instructions);
        }

        // update the injected flag
        isInjected = true;
    }

    /**
     * Gets the label that the instruction is under
     *
     * @param memoryMethod     method that the instruction is in
     * @param abstractInsnNode instruction that you want to search for
     * @return {@link AbstractInsnNode}
     */

    private AbstractInsnNode getCurrentLabel(MemoryMethod memoryMethod, AbstractInsnNode abstractInsnNode) {
        boolean isPast = false;
        MemoryInstructions instructions = memoryMethod.instructions;
        for (int i = instructions.size() - 1; i > 0; i--) {
            AbstractInsnNode instruction = instructions.get(i);
            if (instruction.equals(abstractInsnNode)) {
                isPast = true;
            } else if (instruction instanceof LabelNode) {
                if (isPast) {
                    return instruction;
                } else {
                    System.out.println("label");
                }
            }
        }
        return null;
    }

    /**
     * Checks if the provided method should be hooked
     *
     * @param memoryMethod method that you want to check
     * @return {@link Boolean}
     */

    private boolean isHookMethod(MemoryMethod memoryMethod) {
        // define the name and the description
        String name = method;
        String description = "";

        // if the name contains the special character
        if (!name.contains("<")) {

            // get the name of the method
            name = method.substring(0, method.indexOf("("));

            // and get the description of the method
            description = method.substring(method.indexOf("("));
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

}
