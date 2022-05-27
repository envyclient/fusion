package com.envyclient.fusion.injection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class InjectionConfiguration {

    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    @NonNull
    @SerializedName("hooks")
    public final Definition[] hookDefinitions;

    public static InjectionConfiguration load(String configuration) {
        return GSON.fromJson(configuration, InjectionConfiguration.class);
    }

    @AllArgsConstructor
    private static final class Definition {

        @NonNull
        public final String className;

    }

}
