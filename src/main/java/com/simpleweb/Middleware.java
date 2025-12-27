package com.simpleweb;

@FunctionalInterface
public interface Middleware {

    // 'next' is a Runnable. When you call next.run(), the framework moves forward.
    void handle(Context ctx, Runnable next);
}
