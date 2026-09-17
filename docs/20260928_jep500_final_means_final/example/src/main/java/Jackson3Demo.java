import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/*
 * Jackson 3 系（tools.jackson.core:jackson-databind）で JSON から復元.
 *
 * デフォルトでは final フィールドへ書き込まないので、警告は出ないが name は null のまま.
 * ALLOW_FINAL_FIELDS_AS_MUTATORS を有効にすると書き込むようになり、警告が出る.
 */

static class Person {
  private final String name;

  Person() {
    this.name = null;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "Person{name='" + name + "'}";
  }
}

void main() {
  IO.println("Jackson 3 (デフォルト) -> "
      + JsonMapper.builder().build().readValue("{\"name\":\"Alice\"}", Person.class));

  IO.println("Jackson 3 (フィールド書き込みを有効化) -> "
      + JsonMapper.builder().enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS).build()
          .readValue("{\"name\":\"Alice\"}", Person.class));
}
