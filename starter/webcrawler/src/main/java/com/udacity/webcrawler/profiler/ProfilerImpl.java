package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
    List<Method> methods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
    boolean profilerFlag = false;
    for (Method method : methods) {
      Annotation[] annotations = method.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation instanceof Profiled) {
          profilerFlag = true;
          break;
        }
      }
    }
    if (!profilerFlag) {
      throw new IllegalArgumentException(klass.getName() + "doesn't have profiled methods.");
    }
    Object proxy = Proxy.newProxyInstance(
            delegate.getClass().getClassLoader(),
            new Class<?>[]{klass},
            new ProfilingMethodInterceptor(clock, delegate, state));
    return (T) proxy;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    if (Objects.nonNull(path)) {
      try(Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, CREATE, APPEND)) {
        writeData(writer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
