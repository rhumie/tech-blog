import org.openjdk.jol.info.ClassLayout;

/*
 * mark word のビットの並びを確かめる.
 *
 * 識別ハッシュコードを計算させてから mark word を読むと、 ハッシュがビット 11 から始まっていることを確認できる.
 */

void main() {
  Object o = new Object();
  IO.println("hash を計算する前 : " + mark(o));
  long hash = System.identityHashCode(o);
  IO.println("identityHashCode : 0x" + Long.toHexString(hash));
  IO.println("hash を計算した後 : " + mark(o));
  IO.println("hash << 11        : 0x" + Long.toHexString(hash << 11));
}

String mark(Object o) {
  return ClassLayout.parseInstance(o).toPrintable().lines()
      .filter(line -> line.contains("header: mark"))
      .map(line -> line.substring(line.indexOf("0x")).trim()).findFirst().orElse("");
}
