SRC := src/main/java
JAVA25 ?= $(SDKMAN_CANDIDATES_DIR)/java/25.0.4-amzn/bin/java
print = @printf '\n--- %s\n'

.PHONY: run libs clean
run: libs
	$(print) 'final フィールドをリフレクション（Field.set）で書き換える（Java 25）'
	$(JAVA25) $(SRC)/FieldSetDemo.java

	$(print) 'final フィールドをリフレクション（Field.set）で書き換える'
	java $(SRC)/FieldSetDemo.java

	$(print) 'final フィールドをリフレクション（MethodHandles.lookup().unreflectSetter）で書き換える'
	java $(SRC)/UnreflectSetterDemo.java

	$(print) '対象と方法の組み合わせ'
	java $(SRC)/MutationPathsDemo.java

	$(print) '警告は書き換える側のモジュールごとに一度だけ出力される'
	java $(SRC)/WarningGranularityDemo.java

	$(print) 'ALL-UNNAMED で許可すると警告が消える'
	java --enable-final-field-mutation=ALL-UNNAMED $(SRC)/WarningGranularityDemo.java

	$(print) 'モジュール名はクラスパス上のコードには効かない'
	java --enable-final-field-mutation=com.google.gson $(SRC)/WarningGranularityDemo.java

	$(print) 'Gson で書き換える'
	java -cp 'libs/*' $(SRC)/GsonDemo.java

	$(print) 'Gson を deny で実行する'
	-java --illegal-final-field-mutation=deny -cp 'libs/*' $(SRC)/GsonDemo.java

	$(print) 'Jackson 2 系で書き換える'
	java -cp 'libs/*' $(SRC)/Jackson2Demo.java

	$(print) 'Jackson 2 系を deny で実行する'
	-java --illegal-final-field-mutation=deny -cp 'libs/*' $(SRC)/Jackson2Demo.java

	$(print) 'Jackson 3 系で書き換える'
	java -cp 'libs/*' $(SRC)/Jackson3Demo.java

	$(print) '2 系と 3 系のデフォルトの違い'
	java -cp 'libs/*' $(SRC)/JacksonDefaultsDemo.java

libs:
	mvn -q dependency:copy-dependencies

clean:
	rm -rf libs target
