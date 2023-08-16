package bettereither;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

interface ExFunction<A, R> {
  R apply(A a) throws Throwable;

  static <A, S> Function<A, Either<S, Throwable>> wrap(ExFunction<A, S> op) {
    return a -> {
      try {
        return Either.success(op.apply(a));
      } catch (Throwable t) {
        return Either.failure(t);
      }
    };
  }
}

class Either<S, F> {
  private S success;
  private F failure;

  private Either() {
  }

  public static <S, F> Either<S, F> success(S s) {
    Either<S, F> self = new Either<>();
    self.success = s;
    return self;
  }

  public static <S, F> Either<S, F> failure(F f) {
    Either<S, F> self = new Either<>();
    self.failure = f;
    return self;
  }

  public boolean isSuccess() {
    return failure == null;
  }

  public boolean isFailure() {
    return failure != null;
  }

  public S get() {
    if (isFailure()) {
      throw new IllegalStateException("Attempt to get success value from a failure");
    } else {
      return success;
    }
  }

  public F getFailure() {
    if (isSuccess()) {
      throw new IllegalStateException("Attempt to get failure value from a success");
    } else {
      return failure;
    }
  }

  public Either<S, F> report(Consumer<? super F> op) {
    if (isFailure()) {
      op.accept(failure);
    }
    return this;
  }

  public Either<S, F> flatMap(Function<Either<S, F>, Either<S, F>> op) {
    if (isSuccess()) {
      return op.apply(this);
    } else {
      return this;
    }
  }

  public Either<S, F> recover(Function<Either<S, F>, Either<S, F>> op) {
    if (isFailure()) {
      System.out.println(Colors.YELLOW + "*** recover: " + this.failure + Colors.RESET);
      return op.apply(this);
    } else {
      return this;
    }
  }

  public static <S, F> Function<Either<S, F>, Either<S, F>> recoveries(
    Function<Either<S, F>, Either<S, F>> op, int limit) {
    return e -> {
      int l = limit;
      while (e.isFailure() && l-- > 0) {
        System.out.println(Colors.YELLOW + "*** recoveries (limit " + limit + "): " + e.failure + Colors.RESET);
        e = op.apply(e);
      }
      return e;
    };
  }

  public static <S, F> Function<Either<S, F>, Either<S, F>> retryFunctions(
    Function<Either<S, F>, Either<S, F>> ... op) {
    return result -> {
      int index = 0;
      while (result.isFailure() && index < op.length) {
        System.out.println(Colors.YELLOW + "*** retryFunctions (index " + index + "): "
          + result.failure + Colors.RESET);
        result = op[index++].apply(result);
      }
      return result;
    };
  }
}

public class TryIt {
  private static Map<String, String> backups = Map.of(
    "b.txt", "e.txt",
    "e.txt", "recover.txt"
  );

  private static Either<Stream<String>, Throwable> useBackupFile(Either<Stream<String>, Throwable> e) {
    // Assumes this is a failure (shouldn't be called by recover if not!)
    String tryThis = backups.get(e.getFailure().getMessage());
    System.out.println(Colors.PURPLE + "*** useBackupFile: Retrying with " + tryThis + Colors.RESET);
    return ExFunction.wrap((String fn) -> Files.lines(Path.of(fn))).apply(tryThis);
  }

  private static Either<Stream<String>, Throwable> substituteMessage(Either<Stream<String>, Throwable> e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.getFailure().printStackTrace(pw);
    return Either.success(Stream.of(
      Colors.PURPLE,
      "------ stack trace -------",
      sw.toString(),
      "------- end trace --------",
      Colors.RESET
    ));
  }

  private static Either<Stream<String>, Throwable> retry(Either<Stream<String>, Throwable> e) {
    String fn = e.getFailure().getMessage();
    System.out.println(Colors.PURPLE + "*** retry: Retrying file " + fn + Colors.RESET);
    return ExFunction.wrap((String f) -> Files.lines(Path.of(f))).apply(fn);
  }

  private static Function<Either<Stream<String>, Throwable>, Either<Stream<String>, Throwable>> pause(int delay) {
    return e -> {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException ie) {
        // this is going to be complicated!
        return Either.failure(ie);
      }
      return e;
    };
  }

  public static void main(String[] args) {
    Stream.of("a.txt", "b.txt", "c.txt")
      .map(ExFunction.wrap(fn -> Files.lines(Path.of(fn))))
      .map(Either.retryFunctions(
        TryIt::retry,
        pause(3000),
        TryIt::retry,
        Either.recoveries(TryIt::useBackupFile, 3)))

      .map(e -> e.report(f -> System.out.println(Colors.PURPLE + "*** Stream reports: Failed with " + f.getMessage() + Colors.RESET)))
      .filter(Either::isSuccess)
      .flatMap(e -> Stream.of(e, Either.success(Stream.of("--------------------------"))))
      .flatMap(Either::get)
      .forEach(System.out::println);
  }
}