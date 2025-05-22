package com.zaidathar.dynamic.parser.transform;

import javax.script.Bindings;

public class TransformRegistry {
    public static Object call(String method, Bindings bindings) {
        return switch (method) {
            case "fullNameWithPrefix" -> fullNameWithPrefix(bindings);
            case "ageInMonths" -> ageInMonths(bindings);
            case "ageInDays" -> ageInDays(bindings);
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };
    }


    public static Object fullNameWithPrefix(Bindings bindings) {
        String name = (String) bindings.get("name");
        return "Mr. " + name;
    }

    public static Object ageInMonths(Bindings bindings) {
        int age = ((Number) bindings.get("age")).intValue();
        return age * 12;
    }

    public static Object ageInDays(Bindings bindings) {
        int age = ((Number) bindings.get("age")).intValue();
        return age * 365;
    }
}

