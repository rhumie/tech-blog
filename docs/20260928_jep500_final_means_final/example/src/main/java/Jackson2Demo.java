import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Jackson 2 系で JSON から復元.
 *
 * PersonBean（引数なしコンストラクタ）は final フィールドへ書き込むので警告が出る.
 * Person（引数ありコンストラクタ）は MismatchedInputException で失敗する.
 */

static class Person {
  private final String name;

  Person(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Person{name='" + name + "'}";
  }
}

static class PersonBean {
  private final String name;

  PersonBean() {
    this.name = null;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "PersonBean{name='" + name + "'}";
  }
}

void main() throws Exception {
  ObjectMapper mapper = new ObjectMapper();
  IO.println("Jackson 2 -> " + mapper.readValue("{\"name\":\"Alice\"}", PersonBean.class));

  try {
    IO.println("Jackson 2 -> " + mapper.readValue("{\"name\":\"Alice\"}", Person.class));
  } catch (Exception e) {
    IO.println("Jackson 2 -> Person は復元できない: " + e.getClass().getSimpleName());
  }
}
