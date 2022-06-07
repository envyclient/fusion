package com.envyclient.fusion.test.hooks;

import com.envyclient.fusion.injection.hook.manifest.At;
import com.envyclient.fusion.injection.hook.manifest.Definition;
import com.envyclient.fusion.injection.hook.manifest.Hook;
import com.envyclient.fusion.injection.hook.manifest.HookAt;
import com.envyclient.fusion.injection.hook.manifest.access.Final;
import com.envyclient.fusion.injection.hook.manifest.access.Private;
import com.envyclient.fusion.test.helper.Helper;
import com.envyclient.fusion.test.helper.HooksHelper;

@Hook(HooksHelper.class)
public abstract class FusionHook {

    // todo add
    @Private
    @Final
    private int number;

    @HookAt(method = "init()V", at = At.HEAD, definition = "help")
    private void initDefHeadHook() {
        System.out.println("here");
    }

    @HookAt(method = "init()V", at = At.TAIL, definition = "help")
    private void initDefTailHook() {
        System.out.println("here");
    }

    @HookAt(method = "init()V", at = At.HEAD)
    private void initHeadHook() {
        System.out.println("head");
    }

    @HookAt(method = "init()V", at = At.TAIL)
    private void initTailHook() {
        System.out.println("tail");
    }

    @Definition(type = Helper.class, line = 8, name = "help")
    protected abstract void help();

}
