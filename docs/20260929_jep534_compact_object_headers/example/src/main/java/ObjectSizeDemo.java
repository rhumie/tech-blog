import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;

/*
 * 代表的なオブジェクトの 1 個あたりのサイズを、GC のクラスヒストグラムから求める.
 *
 * jcmd <pid> GC.class_histogram と同じ情報を DiagnosticCommand MBean 経由で取得し、
 * bytes / instances を表示する.
 */

record Point(int x, int y) {}

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

  var server = ManagementFactory.getPlatformMBeanServer();
  String histogram = (String) server.invoke(
      new ObjectName("com.sun.management:type=DiagnosticCommand"),
      "gcClassHistogram",
      new Object[] {new String[0]},
      new String[] {String[].class.getName()});

  for (String name : List.of("java.lang.Object", "Point", "java.lang.Integer",
      "java.lang.String", "java.util.HashMap$Node")) {
    histogram.lines()
        .map(line -> line.trim().split("\\s+"))
        .filter(cols -> cols.length >= 4 && (cols[3].equals(name) || cols[3].endsWith("$" + name)))
        .findFirst()
        .ifPresent(cols -> {
          long instances = Long.parseLong(cols[1]);
          long bytes = Long.parseLong(cols[2]);
          IO.println(String.format("%-24s %3d bytes", cols[3], bytes / instances));
        });
  }
  IO.println(keep.size() + map.size());
}
