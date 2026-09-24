import java.util.HashMap;

/*
 * 100 万件の HashMap<Integer, String> を作り、GC 後のヒープ使用量を表示する.
 */

void main() {
  Map<Integer, String> map = new HashMap<>();
  for (int i = 0; i < 1_000_000; i++) {
    map.put(i, "value-" + i);
  }
  System.gc();
  var rt = Runtime.getRuntime();
  var usedMiB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
  IO.println("entries=" + map.size() + " usedHeap=" + usedMiB + " MiB");
}
