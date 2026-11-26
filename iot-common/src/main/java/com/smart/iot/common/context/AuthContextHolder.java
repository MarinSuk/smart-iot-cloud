package com.smart.iot.common.context;

/**
 * ThreadLocal holder for storing authentication context per request thread.
 * 基于 ThreadLocal 的请求线程上下文持有器。
 */
public class AuthContextHolder {

    /** Thread-isolated storage for AuthContext. 线程隔离的鉴权上下文存储。 */
    private static final ThreadLocal<AuthContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * Bind authentication context to the current thread.
     * 将鉴权上下文绑定到当前线程。
     *
     * @param context AuthContext object 鉴权上下文对象
     */
    public static void setContext(AuthContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * Retrieve authentication context from the current thread.
     * 从当前线程获取鉴权上下文。
     *
     * @return AuthContext object or null 鉴权上下文对象或空
     */
    public static AuthContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * Clear ThreadLocal to prevent memory leaks in thread pools.
     * 清理 ThreadLocal 数据，防止线程池复用导致的内存泄漏。
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}