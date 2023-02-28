package com.udacity.webcrawler.profiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
record ProfilingMethodInterceptor(Clock clock, Object delegate,
                                  ProfilingState state) implements InvocationHandler {

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Object invoked;

    if (isMethodProfiled(method)) {
      Instant start = clock.instant();
      try {
        invoked = method.invoke(delegate, args);
      } catch (IllegalAccessException e) {
        throw new RuntimeException();
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } finally {
        Instant endTime = clock.instant();
        Duration duration = Duration.between(start, endTime);
        state.record(delegate.getClass(), method, duration);
      }
    } else {
      try {
        invoked = method.invoke(delegate, args);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }

    }
    return invoked;
  }

  private boolean isMethodProfiled(Method method) {
    Annotation[] annotations = method.getAnnotations();
    boolean isProfiled = false;
    for (Annotation a : annotations) {
      if (a instanceof Profiled) {
        isProfiled = true;
        break;
      }
    }
    return isProfiled;
  }
}