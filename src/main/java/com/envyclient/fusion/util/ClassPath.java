package com.envyclient.fusion.util;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

@Getter
public class ClassPath {

    private static final String CLASS_SUFFIX = ".class";
    private static final String JAR_SUFFIX = ".jar";
    private static final String MOD_MANIFEST_SUFFIX = "mod.json";

    private final Map<String, byte[]> classes = new HashMap<>();
    private final Map<String, byte[]> resources = new HashMap<>();

    private final Map<String, String> configurations = new HashMap<>();

    private URL[] urls;

    public ClassPath(boolean loadFromClassPath) {
        // if the load from class path option is true
        if (loadFromClassPath) {
            // load all the urls from the class path
            this.urls = loadURLs();

            // loop through all the urls
            for (URL url : urls) {

                // create a file for the current url
                File file = new File(url.getFile());

                // if the file is not a directory
                if (!file.isDirectory()) {

                    // get the path to the file
                    String name = file.getAbsolutePath();

                    // if the path ends with a .jar
                    if (name.endsWith(JAR_SUFFIX)) {

                        // load the jar into the class path
                        loadJar(file);
                    } else {

                        // else pass the file to the load function to load all the classes
                        load(file, file.getParentFile());
                    }
                } else {

                    // else scan the directory for jars and classes
                    scan(file, file);
                }
            }
        }
    }

    /**
     * Scans the directory for classes and jars
     *
     * @param directory directory that you want to scan
     * @param parent    parent of that directory
     */

    public void scan(File directory, File parent) {
        // list all the files in the directory
        File[] files = directory.listFiles();

        // if any files were found
        if (files != null) {

            // loop through the files
            for (File file : files) {

                // if the current file is not a directory
                if (!file.isDirectory()) {

                    // load the file into the class path
                    load(file, parent);
                } else {

                    // else continue the scan
                    scan(file, parent);
                }
            }
        }
    }

    /**
     * Loads a file into the class path
     *
     * @param file   file that you want to load
     * @param parent parent of the file
     */

    private void load(File file, File parent) {
        // get the name of the file
        String name = file.getAbsolutePath().substring(parent.getAbsolutePath().length() + 1);

        // if the name ends with the jar suffix
        if (name.endsWith(JAR_SUFFIX)) {

            // load the jar file into the class path
            loadJar(file);
        } else {

            // else replace all the slashes with dots
            name = name.replaceAll("\\\\", ".");
            name = name.replaceAll("/", ".");

            // read the data of the file
            byte[] data;
            try {
                data = read(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // if the name ends with a class suffix
            if (name.endsWith(CLASS_SUFFIX)) {

                // load the class into the class path
                classes.put(name.substring(0, name.length() - CLASS_SUFFIX.length()), data);
            } else {

                // else load the resource
                resources.put(name, data);

                // if the name ends with the mod manifest suffix
                if (name.endsWith(MOD_MANIFEST_SUFFIX)) {

                    // get the mod name
                    name = name.substring(0, name.length() - MOD_MANIFEST_SUFFIX.length());

                    // load the configuration into the configuration map
                    configurations.put(name, new String(data, StandardCharsets.UTF_8));
                }
            }
        }
    }

    /**
     * Loads all the resources from the
     * provided file into the provided map
     *
     * @param file file that you want to load from
     */

    public void loadJar(File file) {
        // load the jar file
        try (JarFile jarFile = new JarFile(file)) {

            // get the enumeration for all the jar entries
            Enumeration<JarEntry> entries = jarFile.entries();

            // loop while there is valid jar entries
            while (entries.hasMoreElements()) {

                // get the current jar entry
                JarEntry jarEntry = entries.nextElement();

                // get current entries name
                String name = jarEntry.getName();

                // replace the slashes with the dots
                name = name.replaceAll("/", ".");

                // test it against the predicate
                if (!jarEntry.isDirectory()) {

                    // get the input stream from the jar for the current entry
                    InputStream inputStream = jarFile.getInputStream(jarEntry);

                    // if the stream is valid
                    if (inputStream != null) {

                        // read the data
                        byte[] data = read(inputStream);

                        // if the name ends with the class suffix
                        if (name.endsWith(CLASS_SUFFIX)) {

                            // remove the class suffix from the name
                            name = name.substring(0, name.length() - CLASS_SUFFIX.length());

                            // else read and put resources bytes into the resources pool
                            classes.put(name, data);
                        } else {
                            // else read and put resources bytes into the resources pool
                            resources.put(name, data);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a byte[] from an input stream
     *
     * @param in the input stream
     * @return byte[] of the input stream
     */

    private byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[0x1000];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }


    /**
     * Loads all the current jars into
     * an array of urls
     *
     * @return {@link URL[]}
     */

    private URL[] loadURLs() {
        // define a list that will hold all the urls
        List<URL> urls = new ArrayList<>();

        // loop through all the entries of the class path
        Stream.of(System.getProperty("java.class.path").split(";")).forEach(library -> {
            // add the library to the urls list
            try {
                urls.add(new File(library).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });

        // return the array of urls
        return urls.toArray(new URL[0]);
    }

    /**
     * Checks if the class path contains the provided class
     *
     * @param className name of the class that you want to check
     * @return {@link Boolean}
     */

    public boolean hasClass(String className) {
        return classes.containsKey(className);
    }

    /**
     * Gets the data of the provided class
     *
     * @param className name of the class that you want to get the data for
     * @return {@link Byte[]}
     */

    public byte[] getClassData(String className) {
        return classes.getOrDefault(className, null);
    }

    /**
     * Checks if the class path contains provided resource
     *
     * @param path path to the resource
     * @return {@link Boolean}
     */

    public boolean hasResource(String path) {
        return resources.containsKey(path);
    }

    /**
     * Gets the data of the provided resource
     *
     * @param path path to the resource that you want to get the data for
     * @return {@link Byte[]}
     */

    public byte[] getResourceData(String path) {
        return resources.getOrDefault(path, null);
    }

}