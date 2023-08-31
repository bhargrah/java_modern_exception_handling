package streamexample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

// Define a function which takes a function and returns a function with optional
@FunctionalInterface
interface Transform<T,R> {
  R apply(T t) throws Throwable;

  static <T,R> Function<T,Optional<R>> wrap(Transform<T,R> op) {
    return t ->  {
      try {
        return Optional.of(op.apply(t));
      } catch (Throwable th) {
        return Optional.empty();
      }
    };
  }

}

public class StreamExceptionHandlingExample {

  public static Stream<String> getLines(String fn) {
    try {
      return Files.lines(Path.of(fn));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static Stream<String> getLines1(String fn) {
    try {
      return Files.lines(Path.of(fn));
    } catch (Throwable t) {
      //do we want to log this???
      return Stream.empty();
    }
  }

  public static Optional<Stream<String>> getLines2(String fn) {
    try {
      return Optional.of(Files.lines(Path.of(fn)));
    } catch (Throwable t) {
      // do we want to log this???
      return Optional.empty();
    }
  }

  public static void main(String[] args) {
    Stream.of("a.txt", "b.txt", "c.txt")
//      .flatMap(fn -> Files.lines(Path.of(fn)))
//      .flatMap(fn -> Ex1.getLines(fn))
      // using the Optional<Stream> approach
//      .map(fn -> Ex1.getLines(fn))
      .map(Transform.wrap(fn -> Files.lines(Path.of(fn))))
      .peek(opt -> {
        if (opt.isEmpty()) {
          System.out.println("oops, something broke!!!");
        }
      })
      .filter(opt -> opt.isPresent())
      .flatMap(Optional::get)
      .forEach(System.out::println);
  }
}




