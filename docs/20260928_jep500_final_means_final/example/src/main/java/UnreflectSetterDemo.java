import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/*
 * MethodHandles.Lookup.unreflectSetter で final フィールドを書き換える.
 *
 * 警告の文言が Field.set とは違い「has been unreflected for mutation」になる.
 * deny を付けると、書き換えた時点ではなくメソッドハンドルを取得した時点で落ちる.
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

void main() throws Throwable {
  Person p = new Person("Alice");
  Field f = Person.class.getDeclaredField("name");
  f.setAccessible(true);
  MethodHandles.lookup().unreflectSetter(f).invoke(p, "Bob");
  IO.println(p);
}
