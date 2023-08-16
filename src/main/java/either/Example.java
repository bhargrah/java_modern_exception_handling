package either;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
interface ExFunction<A, R> {
  R apply(A a) throws Throwable;

  static <A, R> Function<A, Either<R, Throwable>> wrap(ExFunction<A, R> op) {
    return a -> {
      try {
        return Either.success(op.apply(a));
      } catch (Throwable t) {
        return Either.failure(t);
      }
    };
  }
}

// most "Either" use "left" and "right"
class Either<S, F> {
  // these could be done using distinct subclasses implementing an abstract class
  // or interface, here one class with wasted storage for simplicity.
  private F failure;
  private S success;

  private Either(S success, F failure) {
    this.failure = failure;
    this.success = success;
  }

  public static <S, F> Either<S, F> success(S s) {
    return new Either<>(s, null);
  }

  public static <S, F> Either<S, F> failure(F f) {
    return new Either<>(null, f);
  }

  public boolean isSuccess() {
    return this.failure == null;
  }

  public boolean isFailure() {
    return this.failure != null;
  }

  public S get() {
    if (isFailure()) {
      throw new IllegalStateException("Attempt to get success from a failure!");
    }
    return this.success;
  }

  public F getFailure() {
    if (!isFailure()) {
      throw new IllegalStateException("Attempt to get failure from a success!");
    }
    return this.failure;
  }

  public void report(Consumer<F> op) {
    if (isFailure()) op.accept(this.failure);
  }

  public Either<S, F> reporter(Function<Either<S, F>, Either<S, F>> op) {
    if (isFailure()) op.apply(this);
    return this;
  }

  public Either<S, F> recover(Function<Either<S, F>, Either<S, F>> op) {
    if (isFailure()) {
      return op.apply(this);
    } else {
      return this;
    }
  }

  public Function<Either<S, F>, Either<S, F>> repeat(
    Function<Either<S, F>, Either<S, F>> op, int count) {
    return e -> {
      Either<S, F> self = this;
      int retries = count;
      while (self.isFailure() && (retries--) > 0) {
        System.out.println("repeating (counter = " + retries + ")");
        self = op.apply(self);
        System.out.println("result is success? " + self.isSuccess());
      }
      return self;
    };
  }
}

public class Example {
  public static void main(String[] args) {
    Function<String, Either<Stream<String>, Throwable>> readLines =
      ExFunction.wrap(fn -> Files.lines(Path.of(fn)));

    Function<Either<Stream<String>, Throwable>, Either<Stream<String>, Throwable>>
      retry = e -> {
      System.out.println("*** retrying!!!");
      return readLines.apply(e.getFailure().getMessage());
    };

    Function<Either<Stream<String>, Throwable>, Either<Stream<String>, Throwable>>
      delay = e -> {
      try {
        System.out.println("delaying...");
        Thread.sleep(1000);
        System.out.println("done waiting!...");
        return e;
      } catch (InterruptedException ie) {
        // Hmm, what to do here!!!
        return Either.failure(ie);
      }
    };
    Map<String, String> fallbacks = Map.of(
      "b.txt", "recover.txt"
    );
    Function<Either<Stream<String>, Throwable>, Either<Stream<String>, Throwable>>
      fallback = e -> {
        String target = e.getFailure().getMessage();
      System.out.println("looking for fallback for " + target);
      String replacement = fallbacks.get(target);
      if (replacement == null) return e;
      else return readLines.apply(replacement);
    };

    Stream.of("a.txt", "b.txt", "c.txt")
      .map(readLines)
//      .peek(e -> e.report(t -> System.out.println("It failed, with message " + t.getMessage())))
      .map(e -> e.reporter(e1 -> {
        System.out.println(
          "It failed, with message " + e1.getFailure().getMessage());
        return e1;
      }))
      .map(e -> e.recover(e1 -> e1.repeat(delay, 3).apply(e)))
      .map(e -> e.recover(delay))
      .map(e -> e.recover(fallback))
//      .peek(e -> e.report(t -> System.out.println("It failed again, with message " + t.getMessage())))
      .map(e -> e.reporter(e1 -> {
        System.out.println(
          "It failed again, with message " + e1.getFailure().getMessage());
        return e1;
      }))
      .filter(e -> e.isSuccess())
      .flatMap(Either::get)
      .forEach(System.out::println);
  }
}
