package com.zj.spy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 在 Spring 容器初始化阶段，为带有 @MethodSpy 方法的 Bean 自动套上 JDK 代理。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
class MethodSpyBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Class<?>[] interfaces = targetClass.getInterfaces();
        if (interfaces.length == 0 || !containsSpyMethod(targetClass)) {
            return bean;
        }

        return Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                interfaces,
                (proxy, method, args) -> invokeWithSpy(bean, method, args)
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private Object invokeWithSpy(Object bean, Method interfaceMethod, Object[] args) throws Throwable {
        Method targetMethod = bean.getClass()
                .getDeclaredMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        if (!targetMethod.canAccess(bean)) {
            targetMethod.setAccessible(true);
        }
        if (!targetMethod.isAnnotationPresent(MethodSpy.class)) {
            return invokeTarget(bean, targetMethod, args);
        }

        Object[] safeArgs = args == null ? new Object[0] : args;
        long startNs = System.nanoTime();
        Object result = invokeTarget(bean, targetMethod, args);
        printPrettyLog(targetMethod, safeArgs, result, startNs);
        return result;
    }

    private Object invokeTarget(Object bean, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(bean, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private boolean containsSpyMethod(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MethodSpy.class)) {
                return true;
            }
        }
        return false;
    }

    private void printPrettyLog(Method method, Object[] args, Object result, long startNs) {
        MethodSpy config = method.getAnnotation(MethodSpy.class);
        String[] maskFields = config == null ? new String[0] : config.maskFields();
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        System.out.println("🔹 [" + timestamp + "] 🔹");
        System.out.println("   ▶ 方法: " + method.getName());
        System.out.println("   ▶ 参数: " + formatArgs(method.getParameters(), args, maskFields));
        System.out.println("   ▶ 结果: " + result + " (success)");
        System.out.println("   ◀ 耗时: " + durationMs + "ms");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String formatArgs(Parameter[] parameters, Object[] args, String[] maskFields) {
        if (args.length == 0) {
            return "(none)";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String name = i < parameters.length ? parameters[i].getName() : ("arg" + i);
            Object value = isMaskedField(name, maskFields) ? "\"******\"" : quoteIfString(args[i]);
            parts.add(name + " = " + value);
        }
        return String.join(", ", parts);
    }

    private boolean isMaskedField(String name, String[] maskFields) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String field : maskFields) {
            if (lowerName.contains(field.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Object quoteIfString(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
