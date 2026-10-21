## WA Score: retail-store-ecs

> **Classification: CONFIDENTIAL** — セキュリティ上の指摘と未対処のギャップを含みます。配布は許可された担当者に限定し、承認なしに広く閲覧できるチャネルへ掲載しないでください。

**Overall: 2.7/5**

| Pillar | Score | Critical | High | Medium | Low |
|--------|-------|----------|------|--------|-----|
| Operational excellence（運用上の優秀性） | 3 | 0 | 1 | 2 | 1 |
| Security（セキュリティ） | 2 | 0 | 2 | 7 | 1 |
| Reliability（信頼性） | 2 | 0 | 3 | 1 | 1 |
| Performance efficiency（パフォーマンス効率） | 3 | 0 | 0 | 0 | 2 |
| Cost optimization（コスト最適化） | 3 | 0 | 0 | 1 | 2 |
| Sustainability（持続可能性） | 3 | 0 | 0 | 0 | 2 |

評価条件: score モード（質問単位のクイックスキャン）、重要度 standard、レンズ General（フレームワーク本体の 6 柱のみ、追加レンズなし）。設問は AWS 公式ドキュメントの目次（toc-contents.json、2026-10-20 14:42 UTC 取得）から取得した 6 柱 57 問 307 BP を基準にしています。対象は `terraform/ecs/default`、そこから呼ばれる `terraform/lib/{tags,vpc,dependencies,ecs,images}`、`.github/workflows`、`src` 配下です。アカウント側の設定（CloudTrail、GuardDuty、Budgets、Service Quotas など）はコードから確認できないため、指摘は IaC に定義が無いという事実に限っています。

### Findings（all: Critical〜Low）

| # | Pillar | Risk Level | Finding | Evidence |
|---|--------|----------|---------|----------|
| 1 | Security | High | インターネット向け ALB が HTTP:80 のみで待ち受けている。0.0.0.0/0 から到達でき、ACM 証明書による HTTPS も AWS WAF も無い。チェックアウト画面の入力（配送先・支払い情報フォーム）が平文で流れる（SEC05 / SEC09） | `terraform/lib/ecs/alb.tf:9-10`, `terraform/lib/ecs/alb.tf:28-34`, `terraform/lib/ecs/outputs.tf:3` |
| 2 | Security / Reliability | High | UI の `/utility/*` が認証なしで ALB 経由で公開されている。`GET /utility/panic` はプロセスを終了させ、`POST /utility/health/down` はヘルスチェックを失敗させ、`/utility/stress/{n}` は CPU を消費させる。UI は 1 タスク構成なので、外部の誰でも 1 リクエストでサイト全体を停止できる（SEC06 / SEC11 / REL05） | `src/ui/src/main/java/com/amazon/sample/ui/web/UtilityController.java:61`, `:90`, `:108`, `terraform/lib/ecs/ui.tf:14` |
| 3 | Reliability | High | 全 ECS サービスが `desired_count = 1` 固定で、Application Auto Scaling の定義も無い。タスク障害やデプロイのたびに、そのコンポーネントが停止する（REL07 / REL10 / REL11） | `terraform/lib/ecs/service/ecs.tf:140` |
| 4 | Reliability | High | データ層とネットワークが単一 AZ に依存している。Aurora は 2 クラスターともインスタンス 1 台、Amazon MQ は `SINGLE_INSTANCE` で `subnet_ids[0]` のみ、OpenSearch は 1 ノードで `zone_awareness_enabled = false`、ElastiCache はモジュール既定の 1 ノード、NAT Gateway は `single_nat_gateway = true`。1 つの AZ が障害を起こすとサービス全体が止まる（REL10 / REL11） | `terraform/lib/dependencies/catalog_rds.tf:11-13`, `orders_rds.tf:10-12`, `mq.tf:18-19`, `catalog_opensearch.tf:43-44,55`, `elasticache.tf:1-13`, `terraform/lib/vpc/vpc.tf:25` |
| 5 | Reliability | High | バックアップと復旧の設計が無い。Aurora は `skip_final_snapshot = true` なので、destroy や置き換えのときに最終スナップショットが残らない。DynamoDB の carts テーブルは PITR を有効化していない（モジュール既定で無効）。AWS Backup の計画も無く、復元テストも行われていない（REL09） | `terraform/lib/dependencies/catalog_rds.tf:24`, `orders_rds.tf:24`, `dynamodb.tf:1-28` |
| 6 | Operational excellence | High | CloudWatch アラーム、SNS 通知、ダッシュボードが IaC に 1 つも無い。Container Insights（enhanced）でメトリクスは収集しているが、ALB の 5xx 増加、ターゲットの Unhealthy、タスク停止、DB の CPU 上昇が起きても誰にも通知されない（OPS08 / OPS10 / REL06） | `terraform/lib/ecs/cluster.tf:1-8`（`terraform/lib/ecs` 配下に `aws_cloudwatch_metric_alarm` が存在しない） |
| 7 | Security | Medium | すべてのタスクのセキュリティグループが 8080/tcp を 0.0.0.0/0 から許可している。プライベートサブネットに置かれているため外部からは到達しないが、VPC 内の任意のリソースからサービス間 API（`/chaos` を含む）を直接呼べる。ALB の SG と、呼び出し元サービスの SG だけに絞るべき（SEC05） | `terraform/lib/ecs/service/sg.tf:5-10` |
| 8 | Security | Medium | OpenSearch のリソースポリシーが `Principal "*"` / `es:*` を許可している。VPC と FGAC で緩和されているが、IAM 側の防御層が無い（SEC03） | `terraform/lib/dependencies/catalog_opensearch.tf:82-92` |
| 9 | Security | Medium | ElastiCache Redis の転送時暗号化が無効（`transit_encryption_enabled = false`）で、AUTH も設定されていない（SEC09） | `terraform/lib/dependencies/elasticache.tf:9` |
| 10 | Security | Medium | carts 用の IAM ポリシーが `dynamodb:*` を許可している。さらに、DynamoDB を使わない checkout のタスクロールにもこのポリシーがアタッチされている（SEC03） | `terraform/lib/dependencies/dynamodb.tf:42`, `terraform/lib/ecs/checkout.tf:21-23` |
| 11 | Security | Medium | DB と MQ の認証情報を `random_string` / `random_password` で生成しているため、値が Terraform state に平文で残る。Secrets Manager のローテーションが無く、orders DB と MQ のシークレットはカスタマー管理 KMS キーで暗号化されていない。KMS キーの自動ローテーションも有効化されていない（SEC02 / SEC08） | `terraform/lib/dependencies/catalog_rds.tf:38-41`, `orders_rds.tf:37-40`, `terraform/lib/ecs/orders.tf:68-70`, `terraform/lib/ecs/common.tf:1-4,11-13` |
| 12 | Security | Medium | 検知の仕組みが IaC に無い。VPC Flow Logs、ALB アクセスログ、OpenSearch の監査ログ（`log_publishing_options = []`）、GuardDuty、Security Hub の定義が無い。アカウント側で有効化されているかはコードから判断できない（SEC04） | `terraform/lib/vpc/vpc.tf:11-39`, `terraform/lib/ecs/alb.tf:16-55`, `terraform/lib/dependencies/catalog_opensearch.tf:95-96` |
| 13 | Security | Medium | ソフトウェアサプライチェーンの検証が CI に無い。PR ワークフローにはコンテナイメージの脆弱性スキャン、IaC のセキュリティスキャン（Checkov、Trivy など）、SAST が無い。サイドカーのイメージは `cloudwatch-agent:latest` で、イメージをダイジェストで固定していない。Renovate による依存関係の更新は導入済み（SEC11 / SEC06） | `.github/workflows/pr.yaml:39-57`, `terraform/lib/ecs/service/ecs.tf:86`, `terraform/lib/images/generated.tf.json` |
| 14 | Reliability | Medium | ECS サービスにデプロイサーキットブレーカー（`deployment_circuit_breaker { enable, rollback }`）もデプロイアラームも設定されていない。不良リリースが自動でロールバックされない（REL08 / OPS06） | `terraform/lib/ecs/service/ecs.tf:136-175` |
| 15 | Operational excellence | Medium | インフラのデプロイが自動化されていない。CI では `terraform fmt` と tflint は動いているが、`terraform validate`、`terraform plan`、`terraform apply` のパイプラインが無い。E2E テストは Docker Compose と kind でしか実行されておらず、ECS 構成は検証されていない（OPS05 / OPS06） | `lefthook.yml:37-42`, `.github/workflows/e2e-test.yml`, `.github/workflows/release-please.yml:23-29` |
| 16 | Operational excellence | Medium | 分散トレーシングが既定で無効になっている（`opentelemetry_enabled = false`）。有効にしても `OTEL_METRICS_EXPORTER = none` なので、アプリケーションのメトリクスは送信されない（OPS04） | `terraform/ecs/default/variables.tf:22-25`, `terraform/lib/ecs/service/ecs.tf:21-22` |
| 17 | Cost optimization | Medium | 需要に関係なく常時稼働の固定容量で動いている。Fargate 5 タスクがそれぞれ 1 vCPU / 2 GB、MQ は `mq.m5.large`、OpenSearch と Aurora 2 クラスターも常時稼働している。非本番環境で停止スケジュールを設定する余地や、利用実績に基づいてサイズを見直す余地がある（COST06 / COST09） | `terraform/lib/ecs/service/ecs.tf:130-131`, `terraform/lib/dependencies/mq.tf:17` |
| 18 | Reliability | Low | DR の目標（RTO / RPO）と手順が定義されていない。重要度 standard であればシングルリージョン構成で問題ないが、復旧手順の文書化は必要（REL13） | `terraform/ecs/default/main.tf` |
| 19 | Security | Low | 全サービスで ECS Exec が有効（`enable_execute_command = true`）で、`readonlyRootFilesystem = false` になっている。コンテナ自体は非 root ユーザーで動作している（SEC06） | `terraform/lib/ecs/service/ecs.tf:61,142`, `src/ui/Dockerfile:68` |
| 20 | Operational excellence | Low | ロググループに保持期間が無く、KMS 暗号化も設定されていない（OPS04 / COST） | `terraform/lib/ecs/cluster.tf:10-12` |
| 21 | Performance efficiency | Low | Fargate タスクが CPU アーキテクチャを指定しておらず、既定の X86_64 で動いている。リリース時に arm64 イメージもビルドしているのに Graviton を使っていない。全サービスが同じサイズで、計測に基づくサイジングもしていない（PERF02） | `terraform/lib/ecs/service/ecs.tf:125-134`, `.github/workflows/artifacts.yaml:21-25` |
| 22 | Performance efficiency | Low | Aurora が旧世代のバースト型 `db.t3.medium`、Redis が `cache.t3.micro` で、CPU クレジットを使い切ると性能が落ちる。OpenSearch はすでに Graviton の `r8g` を使っている（PERF01 / PERF03） | `terraform/lib/dependencies/catalog_rds.tf:8`, `orders_rds.tf:8`, `elasticache.tf:7` |
| 23 | Cost optimization | Low | コスト配分タグが `environment-name` と `created-by` の 2 つだけで、AWS Budgets などのコストガードレールが IaC に無い（COST02 / COST03） | `terraform/lib/tags/main.tf:6-11` |
| 24 | Cost optimization | Low | ECS モジュールの `vpc.tf` が空で、ECR、S3、DynamoDB、CloudWatch Logs、Secrets Manager 向けの VPC エンドポイントが無い。イメージの取得や AWS API の通信がすべて単一の NAT Gateway を経由するため、データ処理料金がかかり、NAT が単一障害点にもなる（COST08 / REL02） | `terraform/lib/ecs/vpc.tf`, `terraform/lib/vpc/vpc.tf:22-25` |
| 25 | Sustainability | Low | 需要に合わせたスケーリング（スケールイン、夜間停止）が無く、Graviton も活用していないため、使われていない時間帯の容量が電力を消費し続けている（SUS02 / SUS05） | `terraform/lib/ecs/service/ecs.tf:140`, `ecs.tf:125-134` |
| 26 | Sustainability | Low | ログの保持期限が無期限で、データのライフサイクルポリシーが定義されていない（SUS04） | `terraform/lib/ecs/cluster.tf:10-12` |

確認できた強み: タスクはプライベートサブネットに配置され、パブリック IP を持たない（`service/ecs.tf:145-149`）。Aurora と OpenSearch は保存時に暗号化され、OpenSearch は HTTPS を強制し TLS 1.2 以上、ノード間も暗号化している。DB の認証情報は Secrets Manager 経由で `secrets` としてタスクに注入され、平文の環境変数にはなっていない。サービス間通信は ECS Service Connect を使っている。ALB とコンテナの両方にヘルスチェックがある。UI のクライアントはリトライと指数バックオフを実装している（`RetryUtils.java:37-40`）。Container Insights は enhanced で有効になっている。リリースは release-please とセマンティック PR で管理され、GitHub Actions は OIDC で AWS ロールを引き受けており、Renovate が依存関係を更新している。E2E テストにはトレースの検証と負荷テストも含まれている。

### Summary

コンテナ基盤の土台（プライベート配置、保存時暗号化、Secrets Manager、OIDC を使った CI）はできていますが、インターネットからの入口と可用性が弱点です。重要度 standard の基準でも、HTTP のみの公開 ALB、認証なしの `/utility/panic`、全サービス 1 タスクかつ単一 AZ のデータ層によって、外部からの 1 リクエストや 1 つの AZ 障害でサイト全体が止まり得ます。最も効果の大きい対策は、ALB を HTTPS（ACM）と AWS WAF で保護し、`/utility/*` を ALB のリスナールールで遮断するか、アプリ側でプロパティにより無効化したうえで、各サービスを 2 タスク以上の Auto Scaling 構成にすることです。
