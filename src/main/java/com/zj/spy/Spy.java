package com.zj.spy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MethodSpy 的核心代理引擎。
 * <p>
 * 设计目标：
 * <ul>
 *   <li>提供流畅 DSL（on/before/after/install）</li>
 *   <li>基于 JDK 动态代理实现“无业务入侵”的方法拦截</li>
 *   <li>支持注解模式，仅拦截标注 {@link MethodSpy} 的方法</li>
 *   <li>在日志里输出参数、返回值与耗时，并支持敏感参数脱敏</li>
 * </ul>
 *
 * <p>这不是完整 AOP 框架，而是一个轻量、可读、可扩展的小型方法观测器。</p>
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
public class Spy<T> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final Class<T> targetInterface;
    private final String methodName;
    private boolean annotationMode;
    private final List<BeforeCallback> beforeCallbacks = new ArrayList<>();
    private final List<AfterCallback> afterCallbacks = new ArrayList<>();
    private T targetInstance;

    private Spy(Class<T> targetInterface, String methodName) {
        this.targetInterface = Objects.requireNonNull(targetInterface, "targetInterface must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
    }

    public static <T> Spy<T> on(Class<T> targetInterface, String methodName) {
        return new Spy<>(targetInterface, methodName);
    }

    @SuppressWarnings("unchecked")
    public static <T> Spy<T> on(T targetInstance, String methodName) {
        Objects.requireNonNull(targetInstance, "targetInstance must not be null");
        Class<?> resolvedInterface = resolveTargetInterface(targetInstance.getClass());
        Spy<T> spy = new Spy<>((Class<T>) resolvedInterface, methodName);
        return spy.target(targetInstance);
    }

    public Spy<T> target(T targetInstance) {
        this.targetInstance = Objects.requireNonNull(targetInstance, "targetInstance must not be null");
        return this;
    }

    /**
     * 注解驱动入口：自动监听当前实例上标注了 {@link MethodSpy} 的方法。
     */
    public static <T> Spy<T> on(T targetInstance) {
        Spy<T> spy = on(targetInstance, "*");
        spy.annotationMode = true;
        return spy;
    }

    public Spy<T> before(BeforeCallback callback) {
        beforeCallbacks.add(Objects.requireNonNull(callback, "before callback must not be null"));
        return this;
    }

    public Spy<T> after(AfterCallback callback) {
        afterCallbacks.add(Objects.requireNonNull(callback, "after callback must not be null"));
        return this;
    }

    public Spy<T> printToStdout() {
        return before(args -> System.out.println("[MethodSpy][before] args=" + Arrays.toString(args)))
                .after(result -> System.out.println("[MethodSpy][after] result=" + result));
    }

    @SuppressWarnings("unchecked")
    public T install() {
        if (!targetInterface.isInterface()) {
            throw new IllegalArgumentException("JDK dynamic proxy requires an interface type");
        }
        if (targetInstance == null) {
            throw new IllegalStateException("target instance is required, call target(...) before install()");
        }

        InvocationHandler handler = (proxy, method, args) -> invoke(method, args);
        return (T) Proxy.newProxyInstance(
                targetInterface.getClassLoader(),
                new Class<?>[]{targetInterface},
                handler
        );
    }

    private Object invoke(Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return invokeTargetMethod(method, args);
        }

        Method targetMethod = resolveTargetMethod(method);
        boolean shouldSpy = annotationMode
                ? targetMethod.isAnnotationPresent(MethodSpy.class) || method.isAnnotationPresent(MethodSpy.class)
                : ("*".equals(methodName) || method.getName().equals(methodName));
        Object[] safeArgs = args == null ? new Object[0] : args;
        long startNs = shouldSpy ? System.nanoTime() : 0L;

        if (shouldSpy) {
            for (BeforeCallback beforeCallback : beforeCallbacks) {
                beforeCallback.accept(safeArgs);
            }
        }

        Object result = invokeTargetMethod(targetMethod, args);

        if (shouldSpy) {
            for (AfterCallback afterCallback : afterCallbacks) {
                afterCallback.accept(result);
            }
            if (annotationMode) {
                printPrettyLog(targetMethod, safeArgs, result, startNs);
            }
        }
        return result;
    }

    private Method resolveTargetMethod(Method interfaceMethod) throws NoSuchMethodException {
        Method targetMethod = targetInstance.getClass()
                .getDeclaredMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        // 解决跨包/内部类等场景下的反射访问限制，避免 IllegalAccessException。
        if (!targetMethod.canAccess(targetInstance)) {
            targetMethod.setAccessible(true);
        }
        return targetMethod;
    }

    private Object invokeTargetMethod(Method targetMethod, Object[] args) throws Throwable {
        try {
            return targetMethod.invoke(targetInstance, args);
        } catch (InvocationTargetException e) {
            // 保留业务异常语义，不让 InvocationTargetException 污染上层调用栈。
            throw e.getCause();
        }
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
            // 依赖 -parameters 编译参数拿到真实参数名；拿不到时回退 argN。
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

    private static Class<?> resolveTargetInterface(Class<?> targetClass) {
        if (targetClass.isInterface()) {
            return targetClass;
        }
        Class<?>[] interfaces = targetClass.getInterfaces();
        if (interfaces.length == 1) {
            return interfaces[0];
        }
        if (interfaces.length == 0) {
            throw new IllegalArgumentException(
                    "JDK dynamic proxy requires an interface; use on(Class, method).target(instance) when needed"
            );
        }
        throw new IllegalArgumentException(
                "Multiple interfaces found; use on(Class, method).target(instance) to specify one explicitly"
        );
    }

    @FunctionalInterface
    public interface BeforeCallback {
        void accept(Object[] args) throws Throwable;
    }

    @FunctionalInterface
    public interface AfterCallback {
        void accept(Object result) throws Throwable;
    }
}
