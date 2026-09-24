import org.openjdk.jol.info.ClassLayout;

/*
 * JOL (Java Object Layout) 0.17 で java.lang.Object のヘッダの内訳を表示する.
 */

void main() {
  IO.println(ClassLayout.parseInstance(new Object()).toPrintable());
}
