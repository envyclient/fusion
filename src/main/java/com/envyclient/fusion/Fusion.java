package com.envyclient.fusion;

import com.envyclient.fusion.injection.InjectionConfiguration;
import com.envyclient.fusion.injection.hook.ClassHook;
import com.envyclient.fusion.util.ClassPath;
import lombok.Getter;
import me.mat.jprocessor.JProcessor;
import me.mat.jprocessor.jar.memory.MemoryClass;
import me.mat.jprocessor.jar.memory.MemoryJar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public class Fusion {

    @Getter
    private final List<InjectionConfiguration> configurationList = new ArrayList<>();

    private final List<ClassHook> classHooks = new ArrayList<>();

    private final MemoryJar memoryJar;

    private final ClassPath classPath;

    public Fusion(File... files) {
        // define a new class path
        this.classPath = new ClassPath(true);

        // load all the extra files to the class path
        this.loadToClassPath(classPath, files);

        // load the jar into memory with the main class provided
        this.memoryJar = JProcessor.Jar.load(
                classPath.getClasses(),
                classPath.getResources(),
                "net.minecraft.client.main.Main"
        );

        // load all the injection configurations
        classPath.getConfigurations().forEach((name, configuration)
                -> configurationList.add(InjectionConfiguration.load(configuration)));

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
    }

    /**
     * Transforms and injects all the hooks
     */

    public void transform() {
        // transform all the hooks classes
        classHooks.forEach(ClassHook::transform);
    }


    /**
     * Exports all the classes to a map
     *
     * @return {@link Map}
     */

    public Map<String, byte[]> export() {
        return memoryJar.getClassData();
    }

    /**
     * Saves the jar from memory
     * to a file on the disk
     *
     * @param file file that you want to save to
     */

    public void save(File file) {
        memoryJar.save(file, "net/minecraft", "com/");
    }

    /**
     * Loads all the provided files
     * into the provided class path
     *
     * @param classPath class path that you want to load into
     * @param files     files that you want to load into the class path
     */

    private void loadToClassPath(ClassPath classPath, File... files) {
        // if any files were provided
        if (files != null) {

            // loop through all the provided files
            Stream.of(files).forEach(file -> {

                // if the file is a directory
                if (!file.isDirectory()) {

                    // get the path to the file
                    String name = file.getAbsolutePath();

                    // if the path ends with a .jar
                    if (name.endsWith(".jar")) {

                        // load the jar into the class path
                        classPath.loadJar(file);
                    } else {

                        // else pass the file to the load function to load all the classes
                        classPath.load(file, file.getParentFile());
                    }
                } else {

                    // else scan the directory for jars and classes
                    classPath.scan(file, file);
                }
            });
        }
    }

}
