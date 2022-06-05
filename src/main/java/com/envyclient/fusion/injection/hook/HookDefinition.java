package com.envyclient.fusion.injection.hook;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.mat.jprocessor.jar.memory.MemoryClass;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

@RequiredArgsConstructor
public class HookDefinition {

    @NonNull
    public final MemoryClass typeClass;

    public final String name;

    @NonNull
    public final String description;

    @NonNull
    public final int line;

    private boolean isOverLine;
    private boolean isHooked;

    public boolean isInstruction(AbstractInsnNode instruction) {
        // check if the node is a line number node
        if (instruction instanceof LineNumberNode) {

            // cast the node to the line number node
            LineNumberNode lineNumberNode = (LineNumberNode) instruction;

            // if the line number is
            if (lineNumberNode.line >= line) {
                isOverLine = true;
            }
        } else if (instruction instanceof MethodInsnNode) {
            // cast to the method node
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;

            // if the node is matching the definition, and it is over the line
            return methodInsnNode.owner.equals(typeClass.name())
                    && methodInsnNode.name.equals(name)
                    && methodInsnNode.desc.equals(description) && isOverLine;
        }
        return false;
    }

    /**
     * Checks if the definition has an instruction as a target
     *
     * @return {@link Boolean}
     */

    public boolean hasTargetInstruction() {
        return !name.isEmpty();
    }

}
