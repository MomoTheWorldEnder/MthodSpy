package com.zj.spy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.BeansException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper PRETTY_JSON_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        if (!containsSpyMethod(targetClass)) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        Class<?>[] interfaces = targetClass.getInterfaces();
        if (interfaces.length > 0) {
            proxyFactory.setInterfaces(interfaces);
        } else {
            proxyFactory.setProxyTargetClass(true);
        }
        proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation ->
                invokeWithSpy(bean, invocation.getMethod(), invocation.getArguments())
        );
        return proxyFactory.getProxy();
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
        try {
            Object result = invokeTarget(bean, targetMethod, args);
            printPrettyLog(targetMethod, safeArgs, result, startNs);
            return result;
        } catch (Throwable e) {
            printErrorLog(targetMethod, safeArgs, e, startNs);
            throw e;
        }
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
        System.out.println("   ▶ 路径: " + resolveInvocationPath(method));
        System.out.println("   ▶ 参数: " + formatArgs(method.getParameters(), args, maskFields));
        System.out.println("   ▶ 结果: " + formatValue(result));
        System.out.println("   ◀ 耗时: " + durationMs + "ms");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void printErrorLog(Method method, Object[] args, Throwable error, long startNs) {
        MethodSpy config = method.getAnnotation(MethodSpy.class);
        String[] maskFields = config == null ? new String[0] : config.maskFields();
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        System.out.println("🔹 [" + timestamp + "] 🔹");
        System.out.println("   ▶ 路径: " + resolveInvocationPath(method));
        System.out.println("   ▶ 参数: " + formatArgs(method.getParameters(), args, maskFields));
        System.out.println("   ▶ 异常: " + error.getClass().getSimpleName() + " - " + error.getMessage());
        System.out.println("   ◀ 耗时: " + durationMs + "ms");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String resolveInvocationPath(Method method) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String queryString = request.getQueryString();
            String requestPath = request.getRequestURI();
            if (queryString != null && !queryString.isBlank()) {
                requestPath = requestPath + "?" + queryString;
            }
            return request.getMethod() + " " + requestPath;
        }
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private String formatArgs(Parameter[] parameters, Object[] args, String[] maskFields) {
        if (args.length == 0) {
            return "无参数";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String name = i < parameters.length ? parameters[i].getName() : ("arg" + i);
            Object value = isMaskedField(name, maskFields) ? "\"******\"" : formatValue(args[i]);
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

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            if (value instanceof String || value instanceof Character) {
                return "\"" + value + "\"";
            }
            return String.valueOf(value);
        }
        try {
            return PRETTY_JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // 兜底：序列化失败时至少保证日志可读，不影响主流程。
            return String.valueOf(value);
        }
    }
}
