# Java 27 リリース記念連載：final を本当に final にする準備（JEP 500）

## はじめに

Java 27 リリース記念連載の記事です。武田です。

Java 27 は 2026年9月15日にリリースされました。この記事では Java 27 本体に入った JEP ではなく、半年前の Java 26 で入った [JEP 500: Prepare to Make Final Mean Final](https://openjdk.org/jeps/500) をひとつだけ取り上げます。直訳すると「final が final を意味するようにする準備」です。final は「変更できない」を表す修飾子だったはずで、それをあらためて final にするとはどういうことでしょうか。

選んだ理由は、この JEP が業務アプリケーションのテストコードに直接関係するからです。private final なフィールドをリフレクションで差し替えるテストは、今もよく残っています。Spring の ReflectionTestUtils.setField や、それを自作したユーティリティです。生成 AI にテストを書かせても、この型は出てきます。そのコードが Java 26 以降では警告を出すようになりました。

## final は final ではなかった

警告を出しているのは、たとえば次のようなコードです。名前を持つだけのクラスと、それをリフレクションで書き換えるコードです。

```java
public class Person {
    private final String name;
    public Person(String name) { this.name = name; }
    @Override public String toString() { return "Person{name='" + name + "'}"; }
}
```

```java
import java.lang.reflect.Field;

public class Mutate {
    public static void main(String[] args) throws Exception {
        Person p = new Person("Alice");
        Field f = Person.class.getDeclaredField("name");
        f.setAccessible(true);
        f.set(p, "Bob");
        System.out.println(p);
    }
}
```

Java 25 までは、これが何も言わずに動きます。final と書いたフィールドが Bob に書き換わります。例外は出ず、警告もありません。Java 27 で実行すると次のようになります。

```text
WARNING: Final field name in class Person has been mutated reflectively by class Mutate in unnamed module @18b4aac2 (file:/path/to/classes/)
WARNING: Use --enable-final-field-mutation=ALL-UNNAMED to avoid a warning
WARNING: Mutating final fields will be blocked in a future release unless final field mutation is enabled
Person{name='Bob'}
```

書き換えは成功しています。ただし、将来のリリースでは塞ぐと予告されました。

setAccessible(true) を経由した final フィールドの書き換えは、2004年の JDK 5 から許されてきました。20年もの間、final は「通常の Java コードからは変更できない」という意味でしかなかったわけです。

なぜそんな穴が開いていたのでしょうか。理由はシリアライズです。ObjectInputStream は、Serializable なクラスのオブジェクトをストリームから復元するとき、コンストラクタを通さずにフィールドへ値を書き込む必要があります。final フィールドもその対象です。この用途のために開けた通路が、誰からでも使える形で公開されました。DI コンテナ、モックライブラリ、JSON ライブラリがそこを通って final フィールドを埋めるようになり、今日に至ります。

## なぜ今になって塞ぐのか

JEP 500 は、OpenJDK が「Integrity by Default」と呼ぶ一連の取り組みの一部です。Java 21 の JEP 451 はエージェントの動的ロードに警告を付け、Java 23 と 24 の JEP 471 と JEP 498 は `sun.misc.Unsafe` のメモリアクセスを非推奨にして警告を付け、Java 24 の JEP 472 は JNI の利用に同じ扱いをしました。どれも手順が同じです。まずデフォルトで警告し、数リリース後にデフォルトで拒否し、明示的なフラグでだけ許す。JEP 500 は、この手順を final フィールドに適用した最初の一歩です。

塞ぐ動機は2つあります。

1つは、コードを読む側の推論です。final と書かれたフィールドを見た開発者は、コンストラクタを抜けた後は値が変わらないと考えて読みます。クラスパス上のどこかのライブラリが書き換えられる状態では、その前提は「たぶん変わらない」でしかありません。Java メモリモデルが final フィールドに与えている安全な公開の保証も、構築後に書き換えないことが前提です。

もう1つは JVM の最適化です。JIT コンパイラは、変わらないと確信できる値を定数としてコンパイル結果に埋め込めます（定数畳み込み）。HotSpot は現在、static final や record のフィールドは信頼していますが、通常のクラスのインスタンス final フィールドは信頼していません。Field.set で書き換えられうるからです。書き換えがデフォルトで拒否されれば、この制限を外せます。

record のフィールドは、Java 16 で正式導入された時点からリフレクションでも書き換えられませんでした。JEP 500 は、通常のクラスの final フィールドを record と同じ地位に引き上げる変更です。

## 何が警告され、何がすでに例外なのか

警告が出るのは Field.set だけではありません。逆に、警告すら出ずに以前から例外になる経路もあります。Java 27 で試した結果を表にまとめます。

| 書き換えの経路                             | Java 27 での挙動                           |
| ------------------------------------------ | ------------------------------------------ |
| Field.set で通常クラスのインスタンス final | 警告。将来は IllegalAccessException        |
| MethodHandles.Lookup.unreflectSetter       | 警告。文言は「unreflected for mutation」   |
| record のフィールド                        | 以前から IllegalAccessException            |
| static final                               | 以前から IllegalAccessException            |
| `sun.misc.Unsafe` の putObject             | JEP 500 の対象外。JEP 498 の別の警告が出る |

警告は書き換えた側のモジュールごとに一度だけ出ます。同じクラスから何回書き換えても、二度目以降は黙っています。

挙動は `--illegal-final-field-mutation` オプションで切り替えられます。

- `warn`: デフォルト。書き換えは成功し、モジュールごとに一度警告する
- `debug`: 毎回警告し、スタックトレースも付ける
- `deny`: IllegalAccessException を投げる。将来のデフォルト
- `allow`: 黙って許す。このオプション自体が将来削除される

## 鳴らすのは自分のコードとは限らない

業務アプリケーションで出会う警告は、自分で書いたリフレクションから出るとは限りません。たいていは別の場所から来ます。

さきほどの Person クラスを Gson 2.13.2 で JSON から復元してみます。

```java
Person p = new Gson().fromJson("{\"name\":\"Alice\"}", Person.class);
```

```text
WARNING: Final field name in class Person has been mutated reflectively by class com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$2 in unnamed module @e73f9ac (file:/path/to/gson.jar)
```

書き換えたのは Gson の内部クラスです。Jackson 2.20.0 でも同じ警告が出ました。setter と @JsonCreator のどちらも持たないクラスに対しては、フィールドへ直接書き込むからです。自分のコードに Field や setAccessible は一文字も出てきません。

`--illegal-final-field-mutation=deny` を付けると、両方とも復元に失敗します。Jackson は `unnamed module is not allowed to mutate final fields` と原因を書いた JsonMappingException を投げますが、Gson は `Unexpected IllegalAccessException occurred` と、ReflectionAccessFilter の設定を疑うメッセージを出します。ライブラリ側がまだこの例外を想定していないと、原因にたどり着くまでに一手間かかります。

依存が多いアプリケーションでは、モジュールごとに一度きりの警告では棚卸しに足りません。JDK Flight Recorder に jdk.FinalFieldMutation イベントが追加されているので、こちらで全件を拾えます。書き換えられたクラスとフィールド名が、書き換えた側のスタックトレース付きで記録されます。

```bash
java -XX:StartFlightRecording:filename=rec.jfr -jar app.jar
jfr print --events jdk.FinalFieldMutation rec.jfr
```

## 逃し方と、逃すべきでない場所

警告を消す手段は三段階あります。

一番良いのは、書き換えをやめることです。Jackson なら @JsonCreator を付けたコンストラクタか record にすれば、フィールドへの直接書き込みは起きません。テストで final フィールドを差し替えているなら、コンストラクタから依存を渡す形に直します。JEP 自身も、DI やテストのフレームワークに対して final フィールドを書き換えない設計への見直しを求めています。

ライブラリ側の対応を待つ間は、`--enable-final-field-mutation` で書き換えを許可します。

```bash
java --enable-final-field-mutation=com.google.gson -jar app.jar
```

指定するのは、書き換えられるクラスのモジュールではなく、書き換える側のモジュールです。ライブラリがクラスパス上にあれば ALL-UNNAMED を指定します。コマンドラインのほかに、環境変数 JDK_JAVA_OPTIONS や実行可能 JAR のマニフェストの Enable-Final-Field-Mutation 属性でも指定できます。`--add-opens` と同じ感覚で扱えます。

Serializable なクラスについては、開発者が何かする必要はありません。JDK のシリアライズと同じ手段が jdk.unsupported モジュールの ReflectionFactory を通してライブラリ向けに用意されており、ライブラリがそちらへ移行すればフラグなしで動きます。逆に言えば、Serializable でないクラスの final フィールドは、将来 JVM が不変だとみなしてよい対象です。

`--illegal-final-field-mutation=allow` で黙らせる手は、避けたほうがよいでしょう。このオプションは将来削除されると明記されていて、消えたときに一気に例外へ変わります。むしろ逆に、CI のテスト実行に `deny` を付けるほうに価値があります。将来のデフォルトを先取りして、どのライブラリがどこで落ちるかを今のうちに知っておけます。

## おわりに

冒頭のテストコードに戻ります。ReflectionTestUtils.setField で final フィールドを差し替えるテストは、Java 27 では警告付きで通ります。いつ通らなくなるかは、JEP が「将来のリリース」としか書いていないので分かりません。ただ、Integrity by Default の先行例はどれも数リリースでデフォルトを切り替えてきました。

final を本当に final にするのは JVM の仕事ですが、その日に備えて final フィールドを書き換えないコードにしておくのは開発者の仕事です。20年間続いた慣習ですから、まず自分のアプリケーションで `deny` を付けて動かし、どこが鳴るかを見るところから始めてみてください。
