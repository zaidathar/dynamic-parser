package com.zaidathar.dynamic.parser.engine;

import com.zaidathar.dynamic.parser.config.MappingRule;
import com.zaidathar.dynamic.parser.transform.TransformRegistry;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.lang.reflect.Field;
import java.util.Map;

@Component
public class MappingEngine {

    public Object map(Object source, Class<?> targetClass, MappingRule rule) throws Exception {
        Object target = targetClass.getDeclaredConstructor().newInstance();
        Class<?> sourceClass = source.getClass();
        Bindings bindings = new SimpleBindings();

        for (Field field : sourceClass.getDeclaredFields()) {
            field.setAccessible(true);
            bindings.put(field.getName(), field.get(source));
        }

        for (Map.Entry<String, String> entry : rule.fields.entrySet()) {
            String targetFieldName = entry.getKey();
            String expression = entry.getValue();

            Object value;
            if (expression.startsWith("transform:")) {
                String methodName = expression.split(":")[1];
                value = TransformRegistry.call(methodName, bindings);
            } else {
                value = JsExpressionEvaluator.evaluate(expression, bindings);
            }

            Field targetField = targetClass.getDeclaredField(targetFieldName);
            targetField.setAccessible(true);
            targetField.set(target, value);
        }
        return target;
    }
}