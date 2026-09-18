SRC := src/main/java
JAVA25 ?= $(SDKMAN_CANDIDATES_DIR)/java/25.0.4-amzn/bin/java
JAVA27 ?= $(SDKMAN_CANDIDATES_DIR)/java/27.0.0-amzn/bin/java
print = @printf '\n--- %s\n'

.PHONY: run libs clean
run: libs
	$(print) 'UseCompactObjectHeaders のデフォルト値'
	$(JAVA25) -XX:+PrintFlagsFinal -version 2>/dev/null | grep UseCompactObjectHeaders
	$(JAVA27) -XX:+PrintFlagsFinal -version 2>/dev/null | grep UseCompactObjectHeaders

	$(print) 'java.lang.Object のヘッダ（Java 25）'
	$(JAVA25) -cp 'libs/*' $(SRC)/JolDemo.java 2>/dev/null

	$(print) 'java.lang.Object のヘッダ（Java 27）'
	$(JAVA27) -cp 'libs/*' $(SRC)/JolDemo.java 2>/dev/null

	$(print) 'mark word のビットの並び（Java 25）'
	$(JAVA25) -cp 'libs/*' $(SRC)/MarkWordDemo.java 2>/dev/null

	$(print) 'mark word のビットの並び（Java 27）'
	$(JAVA27) -cp 'libs/*' $(SRC)/MarkWordDemo.java 2>/dev/null

	$(print) 'オブジェクト 1 個あたりのサイズ（Java 25）'
	$(JAVA25) $(SRC)/ObjectSizeDemo.java

	$(print) 'オブジェクト 1 個あたりのサイズ（Java 27）'
	$(JAVA27) $(SRC)/ObjectSizeDemo.java

	$(print) 'オブジェクト 1 個あたりのサイズ（Java 25 でフラグを付ける）'
	$(JAVA25) -XX:+UseCompactObjectHeaders $(SRC)/ObjectSizeDemo.java

	$(print) '100 万件の HashMap のヒープ使用量（Java 25）'
	$(JAVA25) -Xmx1g $(SRC)/HeapFootprintDemo.java

	$(print) '100 万件の HashMap のヒープ使用量（Java 27）'
	$(JAVA27) -Xmx1g $(SRC)/HeapFootprintDemo.java

	$(print) '100 万件の HashMap のヒープ使用量（Java 27 で無効にする）'
	$(JAVA27) -Xmx1g -XX:-UseCompactObjectHeaders $(SRC)/HeapFootprintDemo.java

	$(print) 'UseCompressedClassPointers は Java 27 で削除された'
	$(JAVA27) -XX:-UseCompressedClassPointers -version

libs:
	mvn -q dependency:copy-dependencies

clean:
	rm -rf libs target
