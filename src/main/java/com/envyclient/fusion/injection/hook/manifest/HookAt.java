package com.envyclient.fusion.injection.hook.manifest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HookAt {

    String method();

    At at();

    String definition() default "";

}
