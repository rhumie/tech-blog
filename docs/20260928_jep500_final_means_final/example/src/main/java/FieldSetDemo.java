import java.lang.reflect.Field;

/*
 * Field.set で final フィールドを書き換える.
 *
 * Java 26 以降は警告が出るが、書き換え自体は成功する.
 * --illegal-final-field-mutation=deny を付けると IllegalAccessException になる.
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

void main() throws Exception {
  Person p = new Person("Alice");
  Field f = Person.class.getDeclaredField("name");
  f.setAccessible(true);
  f.set(p, "Bob");
  IO.println(p);
}
