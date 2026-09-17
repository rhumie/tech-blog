/*
 * Jackson 2 系と 3 系のデフォルトを並べる.
 *
 * ALLOW_FINAL_FIELDS_AS_MUTATORS が false になったので、3 系は final フィールドへ書き込まない.
 * FAIL_ON_UNKNOWN_PROPERTIES も false なので、値を捨てても例外にならない.
 */

void main() {
  IO.println("Jackson 2 ALLOW_FINAL_FIELDS_AS_MUTATORS = "
      + com.fasterxml.jackson.databind.MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS
          .enabledByDefault());
  IO.println("Jackson 3 ALLOW_FINAL_FIELDS_AS_MUTATORS = "
      + tools.jackson.databind.MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS.enabledByDefault());
  IO.println("Jackson 2 FAIL_ON_UNKNOWN_PROPERTIES     = "
      + com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
          .enabledByDefault());
  IO.println("Jackson 3 FAIL_ON_UNKNOWN_PROPERTIES     = "
      + tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
          .enabledByDefault());
}
