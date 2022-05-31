package com.envyclient.fusion.test;

import com.envyclient.fusion.Fusion;
import com.envyclient.fusion.injection.hook.manifest.At;
import com.envyclient.fusion.injection.hook.manifest.Definition;
import com.envyclient.fusion.injection.hook.manifest.Hook;
import com.envyclient.fusion.injection.hook.manifest.HookAt;

@Hook(Fusion.class)
public abstract class HookTest {

    @Definition(type = Fusion.class, line = 545, name = "error_callback")
    public abstract void setErrorCallback(Object callback);

    @HookAt(method = "<init>", at = At.TAIL, definition = "error_callback")
    void doHook() {
        // do you hook stuff here
    }

}
