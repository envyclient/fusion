package com.envyclient.fusion.test.helper;

public class HooksHelper extends Helper {

    private final Helper helper = new Helper();

    public void init() {
        System.out.println("pre help");
        super.help();
        System.out.println("post help");

        System.out.println("helper.pre help");
        helper.help();
        System.out.println("helper.post help");
    }

}
