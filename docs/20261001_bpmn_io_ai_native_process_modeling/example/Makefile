print = @printf '\n--- %s\n'

.PHONY: run install lint build clean

run: install lint build

install:
	npm install
	sudo apt-get update
	sudo apt-get install -y chromium fonts-noto-cjk
	echo 'export CHROMIUM_FLAGS="$$CHROMIUM_FLAGS --no-sandbox"' | sudo tee /etc/chromium.d/no-sandbox > /dev/null

lint:
	$(print) '下書き: bpmnlint が構造の誤りを検出する'
	-npx bpmnlint docs/purchase_request_draft/purchase_request_draft.bpmn
	$(print) '送受信を取り違えた版: 構造としては正しいので bpmnlint は通る'
	npx bpmnlint docs/purchase_request_swapped/purchase_request_swapped.bpmn
	$(print) '承認の YES と NO を逆にした版: 構造としては正しいので bpmnlint は通る'
	npx bpmnlint docs/purchase_request_inverted/purchase_request_inverted.bpmn
	$(print) '完成版'
	npx bpmnlint docs/purchase_request/purchase_request.bpmn

build:
	npm run --silent build

clean:
	rm -f docs/*/*.svg
