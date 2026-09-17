import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/*
 * 書き換える対象と方法の組み合わせごとに挙動を並べる.
 *
 * 通常のクラスは Field.set でも unreflectSetter でも書き換えられる.
 * record と static final は、どちらの方法でも IllegalAccessException になる.
 * 警告はモジュールごとに一度だけなので、出るのは最初の1件だけ.
 */

class Person {
  private final String name;

  Person(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Person{name='" + name + "'}";
  }
}

record Point(int x) {
}

class Config {
  @SuppressWarnings("unused")
  private static final String NAME = "Alice";
}

void main() {
  check("Field.set           / 通常のクラス", () -> {
    Field f = Person.class.getDeclaredField("name");
    f.setAccessible(true);
    f.set(new Person("Alice"), "Bob");
  });

  check("unreflectSetter     / 通常のクラス", () -> {
    Field f = Person.class.getDeclaredField("name");
    f.setAccessible(true);
    MethodHandles.lookup().unreflectSetter(f).invoke(new Person("Alice"), "Bob");
  });

  check("Field.set           / record", () -> {
    Field f = Point.class.getDeclaredField("x");
    f.setAccessible(true);
    f.set(new Point(1), 2);
  });

  check("unreflectSetter     / record", () -> {
    Field f = Point.class.getDeclaredField("x");
    f.setAccessible(true);
    MethodHandles.lookup().unreflectSetter(f).invoke(new Point(1), 2);
  });

  check("Field.set           / static final", () -> {
    Field f = Config.class.getDeclaredField("NAME");
    f.setAccessible(true);
    f.set(null, "Bob");
  });

  check("unreflectSetter     / static final", () -> {
    Field f = Config.class.getDeclaredField("NAME");
    f.setAccessible(true);
    MethodHandles.lookup().unreflectSetter(f).invoke("Bob");
  });
}

interface Mutation {
  void run() throws Throwable;
}

void check(String label, Mutation m) {
  try {
    m.run();
    IO.println(label + " -> 書き換え成功");
  } catch (Throwable t) {
    IO.println(label + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
  }
}
