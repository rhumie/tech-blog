import java.lang.management.ManagementFactory;
import javax.management.ObjectName;

/*
 * 代表的なオブジェクトの 1 個あたりのサイズを、GC のクラスヒストグラムから求める.
 *
 * jcmd <pid> GC.class_histogram と同じ情報を DiagnosticCommand MBean 経由で取得し、 bytes / instances を表示する.
 */

record Point(int x, int y) {
}

static final int N = 100_000;

void main() throws Exception {
  List<Object> keep = new ArrayList<>();
  Map<Integer, String> map = new HashMap<>();
  for (int i = 0; i < N; i++) {
    keep.add(new Object());
    keep.add(new Point(i, i));
    keep.add(Integer.valueOf(i + 1_000_000));
    map.put(i, "v" + i);
  }

  String histogram = classHistogram();
  for (String name : List.of("Object", "Point", "Integer", "String", "HashMap$Node")) {
    printSize(histogram, name);
  }
  IO.println(keep.size() + map.size());
}

String classHistogram() throws Exception {
  var server = ManagementFactory.getPlatformMBeanServer();
  var command = new ObjectName("com.sun.management:type=DiagnosticCommand");
  var args = new Object[] {new String[0]};
  var signature = new String[] {String[].class.getName()};
  return (String) server.invoke(command, "gcClassHistogram", args, signature);
}

void printSize(String histogram, String name) {
  for (String line : histogram.lines().toList()) {
    String[] cols = line.trim().split("\\s+");
    if (cols.length < 4) {
      continue;
    }
    if (cols[3].endsWith("." + name) || cols[3].endsWith("$" + name)) {
      long size = Long.parseLong(cols[2]) / Long.parseLong(cols[1]);
      IO.println("%-24s %3d bytes".formatted(cols[3], size));
      return;
    }
  }
}
