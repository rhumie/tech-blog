# Java 27：オブジェクトヘッダが 8 バイトになる（JEP 534）

## はじめに

Java 27 リリース記念連載の記事です。

Java 27 は 2026年9月15日にリリースされました。この記事では、Java 27 本体に入った [JEP 534: Compact Object Headers by Default](https://openjdk.org/jeps/534) を取り上げます。HotSpot JVM がすべてのオブジェクトに付けているヘッダを 12 バイトから 8 バイトに縮める機能、Compact Object Headers をデフォルトで有効にする JEP です。

同じ連載で取り上げた JEP 500 と違って、この変更で警告や例外が増えることはありません。Java 27 に上げれば、コードを 1 行も変えずにヒープの使用量が減ります。ただ、「ヘッダが 4 バイト減る」と聞いて、自分のアプリケーションのヒープでどれくらい減るのかを答えられる人は、そう多くないでしょう。オブジェクトによっては 1 バイトも減りません。手元で Java 25 と Java 27 を並べて測ったので、その結果から見ていきます。

## ヘッダには何が入っているのか

Java のオブジェクトは、フィールドの前に JVM が使うヘッダを持ちます。Java 25 までのヘッダは 2 つの部分からなります。前半の mark word は 64 ビットで、識別ハッシュコード（31 ビット）、GC が数える世代（4 ビット）、ロック状態のタグ（2 ビット）が入ります。後半はクラスポインタで、圧縮された形で 32 ビットです。合わせて 96 ビット、12 バイトになります。

JOL（Java Object Layout）0.17 で `new Object()` の内訳を表示すると、Java 25 では次のようになります。

```text
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00180dc8
 12   4        (object alignment gap)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

Object にはフィールドがないので、ヘッダ 12 バイトに 4 バイトの隙間が付いて 16 バイトです。JVM がオブジェクトを 8 バイト境界にそろえるからです。同じコードを Java 27 で動かすと次のようになります。

```text
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0018640000000009 (Lilliput)
Instance size: 8 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
```

class の行が消えて、mark word だけの 8 バイトになりました。クラスポインタがなくなったわけではありません。mark word の上位 22 ビットに、さらに圧縮した形で入っています。ハッシュコードの 31 ビット、世代の 4 ビット、タグの 2 ビットはそのまま残り、GC が使う 1 ビットと Project Valhalla のために予約した 4 ビットが加わって、ちょうど 64 ビットです。

では、なぜ今までクラスポインタは mark word の外にあったのでしょうか。mark word は、ロックや GC の都合で丸ごと別の値に上書きされることがあったからです。たとえば従来のスタックロック方式は、ロックを取るときにヘッダをスレッドのスタックへ退避して、ヘッダにはその退避先のポインタを書き込みます。クラスポインタが mark word の中にあると、ロックを取った瞬間に型情報が消えます。Java 27 の HotSpot はタグの 2 ビットだけを書き換える軽量ロック方式に統一されていて、スタックロック方式を選ぶ LockingMode オプション自体が認識されません。GC がオブジェクトの移動先を記録する処理も同じ理由で書き直され、mark word を上書きしなくなりました。

## どのオブジェクトが小さくなるのか

ヘッダが 4 バイト減ればオブジェクトも 4 バイト減る、というわけにはいきません。代表的なクラスについて、オブジェクト 1 個あたりのサイズを GC のクラスヒストグラム（jcmd の GC.class_histogram と同じ情報）から求めてみます。

| クラス                     | Java 25 | Java 27 |
| -------------------------- | ------: | ------: |
| Object                     |      16 |       8 |
| record Point(int x, int y) |      24 |      16 |
| HashMap.Node               |      32 |      24 |
| Integer                    |      16 |      16 |
| String                     |      24 |      24 |

Integer と String は 1 バイトも変わっていません。ここでも 8 バイト境界が働いています。Integer はヘッダ 12 バイトに int の 4 バイトで、ちょうど 16 バイトに収まっていました。ヘッダが 8 バイトになると 12 バイトですが、8 の倍数に切り上げられて 16 バイトに戻ります。String はフィールドが合計 10 バイトです。ヘッダを足した 22 バイトと 18 バイトは、どちらも 24 バイトに切り上げられます。小さくなるのは、フィールドの合計が 8 の倍数か、8 の倍数まで 1〜3 バイト足りないクラスです。

それでも、実際のデータ構造では減ります。100 万件の `HashMap<Integer, String>` を作って GC した直後のヒープ使用量を測ってみます。

```java
void main() {
  Map<Integer, String> map = new HashMap<>();
  for (int i = 0; i < 1_000_000; i++) {
    map.put(i, "value-" + i);
  }
  System.gc();
  Runtime rt = Runtime.getRuntime();
  long usedMiB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
  IO.println("entries=" + map.size() + " usedHeap=" + usedMiB + " MiB");
}
```

```text
Java 25: entries=1000000 usedHeap=112 MiB
Java 27: entries=1000000 usedHeap=96 MiB
```

16 MiB、14% の減少。1 エントリあたり 16 バイトで、Node の 8 バイトと、文字列の中身を持つ byte 配列の 8 バイトです。Integer と String 本体は上の表のとおり変わっていません。JEP 450 は、実アプリケーションの生存データが 10〜20% 減ると書いています。オブジェクトの平均サイズが 32〜64 バイトのワークロードが多く、ヘッダだけで生存データの 20% 以上を占めていたからだ、というのがその説明です。

## なぜ Java 27 でデフォルトになったのか

Compact Object Headers そのものは新しい機能ではありません。Java 24 の JEP 450 で実験的機能として入り、Java 25 の JEP 519 で正式な機能になりました。有効にするオプションは Java 24 では `-XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders` の 2 つ、Java 25 では `-XX:+UseCompactObjectHeaders` の 1 つです。JEP 534 がしたことは、このオプションのデフォルト値を true にすることだけです。

JEP 534 は、デフォルトにしてよい根拠として実績を挙げています。Oracle は JDK のテストスイート全体を有効な状態で通しています。Amazon は数百のサービスを本番で有効にして運用していて、その多くは JDK 17 や JDK 21 へのバックポートです。SAP は自社の OpenJDK ディストリビューションである SapMachine で、すでにデフォルトを有効に切り替えています。性能の数字も並んでいます。SPECjbb2015 のヒープ使用量が 22% 減って CPU 時間は 8% 減った、G1 と Parallel の GC 回数が 15% 減った、並列度の高い JSON パーサのベンチマークが 10% 速くなった、というものです。

引き換えに失ったものは、逃げ道です。圧縮クラスポインタで表せるクラスは約 400 万個が上限で、これは Java 25 までと同じです。ただし Java 25 までは、上限を超えるアプリケーションは `-XX:-UseCompressedClassPointers` で圧縮をやめられました。Compact Object Headers は圧縮クラスポインタを前提にしていて、Java 27 ではこのオプションが削除されました。

```text
OpenJDK 64-Bit Server VM warning: Ignoring option UseCompressedClassPointers; support was removed in 27.0
```

JEP 450 は、400 万個のクラスをロードするアプリケーションはまだ見たことがない、と書いています。

## 我々は何をすべきか

基本の答えは「何もしなくてよい」です。それでも、3 つ確認しておくことがあります。

1 つ目は、Java 25 を使っているなら今日から使えることです。JEP 519 で正式な機能になっているので、`-XX:+UseCompactObjectHeaders` を付けるだけです。実際にこのオプションを付けた Java 25 で先ほどのクラスヒストグラムを取ると、Java 27 と同じ数字になりました。Java 27 へ上げる前に、自分のアプリケーションで減り幅を測っておけます。

2 つ目は、減った分の扱いです。ヒープが 14% 減ったからといって、コンテナのメモリ制限をそのまま 14% 削るのは早計でしょう。減り幅はオブジェクトの形で決まり、上の表のとおりクラスによってはゼロです。GC の回数が減る効果は、ヒープを小さくすると相殺されます。まず現行の設定で GC ログを見て、それから決めるのが順当です。

3 つ目は、戻す手段です。何か問題が起きたら `-XX:-UseCompactObjectHeaders` で従来のレイアウトに戻せます。JDK には無効時のための CDS アーカイブ classes_nocoh.jsa も同梱されていて、リリースノートは起動性能も同等だとしています。ただし Java 27 のリリースノートは、このオプションを将来非推奨にして削除する計画だと書いています。恒久的な逃げ道ではなく、原因を調べる時間を稼ぐためのものです。

## おわりに

JEP 534 は、Java 27 に上げるだけで受け取れる変更です。JEP 500 が開発者にコードの見直しを求めていたのと比べると、こちらは JVM の側で完結しています。

ただ、どれくらい減ったかを知っているのは JVM だけです。手元の HashMap では 14% でしたが、Integer や String が大半を占めるヒープなら数字はもっと小さくなります。Java 25 でも試せるので、まず自分のアプリケーションで測ってみることをお勧めします。

この記事で動かしたサンプルコードは [GitHub](https://github.com/rhumie/tech-blog/tree/main/docs/20260929_jep534_compact_object_headers/example) で公開しています。
