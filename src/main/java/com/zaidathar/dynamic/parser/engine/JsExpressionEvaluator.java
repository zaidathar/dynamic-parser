package com.zaidathar.dynamic.parser.engine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;

public class JsExpressionEvaluator {
    public static Object evaluate(String expression, Map<String, Object> bindings) {
        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
            Value bindingsObj = context.getBindings("js");
            bindings.forEach(bindingsObj::putMember);
            return context.eval("js", expression).as(Object.class);
        }
    }
}
