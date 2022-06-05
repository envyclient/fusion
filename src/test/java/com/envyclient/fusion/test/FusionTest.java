package com.envyclient.fusion.test;

import com.envyclient.fusion.Fusion;
import org.junit.jupiter.api.Test;

import java.io.File;

public class FusionTest {

    private static final File TESTS_DIRECTORY = new File("tests/");
    private static final File CLASSES_DIRECTORY = new File("target/classes/");
    private static final File TEST_CLASSES_DIRECTORY = new File("target/test-classes/");

    @Test
    public void test() {
        // if the tests directory does not exist
        if (!TESTS_DIRECTORY.exists()) {

            // create it
            if (!TESTS_DIRECTORY.mkdirs()) {
                throw new RuntimeException("Failed to create the tests directory");
            }
        }

        // define a new fusion instance
        Fusion fusion = new Fusion(CLASSES_DIRECTORY, TEST_CLASSES_DIRECTORY);

        // transform the hooks
        fusion.transform();

        // save the fused jar
        fusion.save(new File(TESTS_DIRECTORY, "Fusion-Fused.jar"));
    }

}
