import com.google.gson.Gson;

/*
 * Gson で JSON から復元.
 *
 * final フィールドへ書き込むので警告が出る.
 * --illegal-final-field-mutation=deny を付けると RuntimeException になり、
 * メッセージは ReflectionAccessFilter の設定を疑う内容になる.
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

void main() {
  Person p = new Gson().fromJson("{\"name\":\"Alice\"}", Person.class);
  IO.println("Gson      -> " + p);
}
