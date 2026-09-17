import java.lang.reflect.Field;

/*
 * 警告が何を単位に一度だけ出るのかを確かめる.
 *
 * 4回書き換えるが、警告が出るのは最初の1件だけ.
 * 書き換える先のフィールドが違っても、書き換える側のクラスが違っても、2件目以降は出ない.
 * 単位は書き換える側のモジュールで、クラスパス上のコードは無名モジュール1つにまとまる.
 */

static class Customer {
  @SuppressWarnings("unused")
  private final String first = "Alice";
  @SuppressWarnings("unused")
  private final String last = "Smith";
}

static class Order {
  @SuppressWarnings("unused")
  private final String id = "A-1";
}

static class Mutator1 {
  static void mutate(Object target, String field) throws Exception {
    Field f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, "changed");
  }
}

static class Mutator2 {
  static void mutate(Object target, String field) throws Exception {
    Field f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, "changed");
  }
}

void main() throws Exception {
  IO.println("-- Mutator1 が Customer.first を書き換える --");
  Mutator1.mutate(new Customer(), "first");

  IO.println("-- Mutator1 が Customer.last を書き換える --");
  Mutator1.mutate(new Customer(), "last");

  IO.println("-- Mutator1 が Order.id を書き換える --");
  Mutator1.mutate(new Order(), "id");

  IO.println("-- Mutator2 が Customer.first を書き換える --");
  Mutator2.mutate(new Customer(), "first");
}
