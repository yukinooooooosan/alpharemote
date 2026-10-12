# 開発版の検証記録

2026-10-06更新。初期の自動検証は実機確認を後回しにして行った。その後、ユーザーのPixel 10 Proで5秒待機・5枚撮影の成功が報告された。使用APK版の詳細は未記録。網羅的な実機確認は未完了で、以下の各版の「未検証」はその版の作成時点の記録。

## 0.1.0の自動検証

- 上流 `Staacks/alpharemote` の `4a950a8` を別のworktreeで `testDebugUnitTest assembleDebug`：成功。
- 自撮りモード追加後の `testDebugUnitTest assembleDebug lintDebug`：成功。
- JUnit：10件成功、失敗0件。うち自撮りシーケンスのテスト8件。
- Android lint：エラー0件、警告29件。既存UI・非推奨API・未使用の元アイコン等の指摘を含む。
- `git diff --check`：成功。
- `CameraBLE.kt` と `LICENSE`：上流から変更なし。

自撮りテストでは、待機後のAF、古い合焦通知の除外、3秒のAF期限、遅延したタイマーと合焦通知の競合、AF失敗の回数カウント、音OFF時の失敗音・終了音、全保持時間、各状態のSTOP、無限ループ1000回、切断後の再開防止、重複STARTを確認した。

## ビルド環境

- Java：Eclipse Temurin 17
- Gradle：9.8.0（上流のwrapper）
- Android Gradle Plugin：9.4.1（上流設定）
- Kotlin：2.4.20（上流設定）
- Android SDK：37.0
- applicationId：`cc.yukino.selfieremote`
- versionName：`0.1.0`
- minSdk：31（Android 12）

## 後回しにする検証

Android端末とSonyカメラを使うBLE操作、実際の画像枚数、AFの挙動、音の聞こえ方、画面OFFでの継続、ロック画面通知の操作は未検証。
[実機チェックリスト](SELFIE-MVP.md#実機で確認する項目)を使って後日確認する。
この版はインストール可能なデバッグビルドで、MVPの実写完成条件を確認した版ではない。


## 0.1.1：起動クラッシュの修正

Pixel 10 Proで起動直後に閉じるとの報告を受け、Android画面の起動テストを追加した。
0.1.0のSELFIE画面で、カウントダウン音スイッチの初回計測中に `SwitchCompat.makeLayout` → `StaticLayout` へnullのラベルが渡り、NullPointerExceptionになることをRobolectric（API 31）で再現した。

既存のMaterial Componentsテーマに合わせて、Material 3の `MaterialSwitch` を `SwitchMaterial` に置き換え、`showText=false` を明示した。
versionCodeを2、versionNameを0.1.1に更新。同じデバッグ署名を維持しているため、0.1.0へ上書きインストールできる。

- `testDebugUnitTest assembleDebug lintDebug`：成功。
- 全14件成功、失敗0件。うち起動テストはAPI 31 / 36の各々で通常・ダークテーマの4件。
- Android 12 / 16相当で、Activityの起動・初回の画面計測・SELFIE画面の表示を検証した。
- APK署名検証：成功。0.1.0と証明書のSHA-256が一致。
- ビルドとテストの実行環境はJava 21。アプリのJavaターゲットは17を維持。
- RobolectricはJVM上でAndroid画面を実行するテスト。Pixel 10 Proでの修正版の起動と、Sonyカメラを使った撮影は別途確認する。

## 0.1.2：自撮りに必要な3画面へ整理

利用者向け画面を「SELFIE」「カメラ接続」「アプリ情報」に絞った。
通常リモコン、カスタムボタン、物理キー割り当て、通知ボタンサイズ、Broadcast controlを外した。
旧通常操作・詳細シーケンスのService Intentも実行しない。
接続設定は既存のペアリング・権限ロジックを維持し、日本語の状態表示と接続案内に整理した。
通知は接続状態と撮影中の進行状況・STOPに絞り、切断通知はService終了後も残す。

既存のシーケンステストと起動テストに加え、API 31 / 36で3画面への遷移、古い画面IDの扱い、外部Broadcast受信入口の削除、通知のSTOP・停止後のボタン除去・切断通知の維持を確認した。
versionCodeは3、versionNameは0.1.2。

- `testDebugUnitTest assembleDebug lintDebug --rerun-tasks`：成功。
- 全22件成功、失敗0件。画面関連8件、通知4件、シーケンス8件、既存テスト2件。
- Android lint：エラー0件、警告55件。未使用の上流リソースと既存の非推奨API等を含む。
- `CameraBLE.kt` と `LICENSE`：上流から変更なし。
- APK署名検証：成功。0.1.1と証明書が一致し、上書きインストール可能。
- 実機のBLE撮影・画面OFF・通知操作は引き続き未検証。

## 0.1.3：取説の追加とアプリ情報の整理

「SELFIE」「カメラ接続」「取説」「アプリ情報」の4画面にした。
取説は通信なしで読み、準備・撮影手順・設定・AF失敗・音・STOP・切断・接続トラブルの案内を確認できる。
アプリ情報からブログ・YouTube・寄付・上流の不具合報告ボタンを外し、元作者のクレジット、改変表示と日付、GPL-3.0と無保証の案内、元／改変プロジェクトのソースへのリンクを残した。
GPL-3.0全文はrootのLICENSEと同一のファイルをAPK内に同梱し、オフラインで開ける。
versionCodeは4、versionNameは0.1.3。

- `testDebugUnitTest assembleDebug lintDebug`：成功。
- 全24件成功、失敗0件。画面関連10件、通知4件、シーケンス8件、既存テスト2件。
- API 31 / 36で4画面の遷移、通常・ダーク表示で取説とGPL全文を開けることを確認した。
- Android lint：エラー0件、警告76件。上流の未使用リソースと非推奨API等を含む。
- 同梱GPLテキストとrootのLICENSE：同一。
- APK署名検証：成功。前の版と同じ署名で上書きインストール可能。
- `CameraBLE.kt` と `LICENSE`：上流から変更なし。実機撮影の検証は引き続き後回し。

## 0.1.4：SELFIE画面だけをComposeへ移行

SELFIEをComposeView内のCompose / Material 3画面に置き換えた。カメラ接続・取説・アプリ情報と下部ナビゲーションは既存XMLのまま。
Fragmentがライフサイクルに合わせてService / ViewModelの状態を収集し、表示とイベントだけを扱う`SelfieScreen`へ渡す。
`SelfiePreview`は7状態のサンプルと空のイベント処理を使い、通常・ダークの14通りを生成する。カメラ操作・Service起動・設定保存・タイマー処理はプレビューから呼ばない。
Macでプレビュー調整、N100でテスト・APKビルド・署名を行う手順書を追加した（その後、端末固有の手順書はローカルに保持し、Git管理対象から除外した）。versionCodeは5、versionNameは0.1.4。

- `testDebugUnitTest assembleDebug lintDebug --no-daemon`：成功。
- 全40件成功、失敗・エラー・スキップ0件。既存24件にCompose画面12件とFragmentの接続案内／ナビゲーション4件を追加。
- API 31 / 36のRobolectricで7状態×通常・ダークを描画し、状態表示、START / STOP、実行中の設定非表示、終了時の回数表示を確認した。
- 設定イベント、復元待ちのSTART無効化とSTOP有効化、切断時のSTARTへの復帰、実際の`SelfiePreview`をクリックしてもServiceや設定へ影響しないことを確認した。
- 実アプリの未接続STARTが接続案内を表示し、接続ボタンが既存XML画面へ遷移することを確認した。4画面間の移動とGPLダイアログの起動テストも維持。
- Android lint：エラー0件、警告79件。主に上流の未使用リソースと非推奨API等。Compose画面ソースへのlint指摘なし。
- APK署名検証：成功。証明書SHA-256は既存APKと同じ `c8a131c390f88da5600d8163649eaf721fccfaf66898776756d38263d2b6d0e1`。アプリIDも維持し、上書き可能。
- Java 21、Gradle 9.8.0、AGP 9.4.1、Kotlin / Compose Compiler 2.4.20、Compose BOM 2026.09.00、compile / target SDK 37。アプリのJVMターゲットは17。
- 今回の差分にBLE、Service、撮影Controller、SelfieViewModel、既存XMLの3画面の変更なし。`CameraBLE.kt`と`LICENSE`は上流から変更なし。同梱GPLテキストもLICENSEと同一。

**実機確認の現状：** ユーザーがPixel 10 Proで5秒待機・5枚撮影を確認済み。Compose移行前の報告で、使用APK版とカメラ設定の詳細は未記録。網羅的な実機確認は未完了。
新しいCompose版APKの実機動作とMacのAndroid Studio上でのPreview表示は、このN100環境では検証していない。Robolectricの描画確認とIDEの実操作確認は区別する。実機チェックリストは未確認の詳細条件を残し、mainへはマージしない。

## 0.1.5：4画面とタブをComposeに統一

SELFIE・カメラ接続・取説・アプリ情報と下部タブをCompose / Material 3へ移行した。
Fragmentと既存ナビゲーションのホストは維持し、登録・解除・Companion Device・権限要求は既存処理へ接続する。
共通テーマは通常／ダークとも色数を抑え、余白・角丸・文字を統一した。
SELFIEは一体型の選択UIに変更し、実行中のSTOPを状態のスクロール領域から独立させた。
カウントは「AF開始まで」、進捗は「試行」と表示する。取説は4ステップ・撮影直前AFの図・折りたたみFAQに整理した。
versionCodeは6、versionNameは0.1.5。

- `testDebugUnitTest assembleDebug lintDebug --no-daemon`：成功。
- 全59件成功、失敗・エラー・スキップ0件。API 31 / 36の画面・通知・シーケンステストを維持した。
- SELFIEの12状態×通常／ダーク、接続の8状態×通常／ダーク、取説のFAQ開閉、アプリ情報のクレジット・ソースリンクを確認した。
- タブを実際に押した画面遷移、戻ったときの取説の開閉状態、Activity再生成時の選択タブ／開閉状態の復元を確認した。
- 未接続STARTの接続案内と、既存のペアリング準備ダイアログ／キャンセルへの接続を確認した。実際のカメラ登録は未検証。
- GPLボタンから同梱ライセンス全文をオフラインで開けることを確認した。本文はrootのLICENSEと同一。
- 小画面（320×480dp）・文字倍率1.5で、撮影中のSTOPが表示され、有効なことを確認した。
- カメラ操作を伴わないCompose Previewを維持した。SELFIEの停止・切断・設定復元待ち・無限撮影・スキップあり終了、接続の権限待ち・ペアリング未完了等を追加した。
- API 36 / Robolectric Native Graphicsで、4画面とカウント／AF／撮影の通常・ダーク計14枚を生成し、見た目を確認した。生成先は `app/build/outputs/ui-previews/`（Git対象外）。MacのIDE Previewとは別の検証。
- Android lint：エラー0件、警告106件。未使用になった旧画面のリソース、上流由来の非推奨API等を含む。
- APK署名検証：成功。証明書SHA-256は既存APKと同じ `c8a131c390f88da5600d8163649eaf721fccfaf66898776756d38263d2b6d0e1`。アプリIDを維持し、上書き可能。
- BLE・Service・撮影Controller・SelfieViewModel・SettingsViewModel・DataStore・LICENSEに今回の差分なし。通知の変更はカウントダウン文言のみ。
- 端末固有のMac／N100手順書はローカルで更新し、引き続きGit管理対象外とした。

**実機確認の現状：** Pixel 10 Proで5秒待機・5枚撮影はユーザー確認済み（Compose移行前、APK版未記録）。0.1.5での実機確認、ペアリング・画面OFF・切断などの網羅的確認と、MacのAndroid StudioでのPreview操作は未検証。mainにはマージしない。


## 0.1.6：シンプル／kawaiiのスキン切り替え

2026-10-09。既存の見た目をシンプルとして維持し、ピンク・ラベンダー、太い文字、丸い部品と小さなハート／星を使うkawaiiを追加した。
設定タブの「外観」から選び、Activity共通のViewModelと独立したDataStoreキー `uiSkin` で4画面・下部タブへ反映する。初期値・不明な保存値はシンプル。通常／ダークは端末に従う。
versionCodeは7、versionNameは0.1.6。

- `testDebugUnitTest assembleDebug lintDebug --no-daemon`：成功。全67件、失敗・エラー・スキップ0件。
- 両スキン×12状態×通常／ダークのSELFIE表示、設定の非表示、START / STOP、接続状態と進捗をAPI 31 / 36で確認した。320×480dp・文字倍率1.5でも両スキンのSTOPが表示され、有効。
- 保存値の復元、未保存・不明値の初期値、撮影設定／登録カメラの維持、実際の設定タブでの切り替え、画面移動とActivity再生成後の復元、連続切り替えの最終値保存を確認した。スキン変更前後のServiceの撮影状態も同一。
- Compose Previewは実行時ViewModelから分離したまま、SELFIE48状態と小画面2状態、接続32状態、取説・設定各4状態を用意した。
- API 36 / Robolectric Native Graphicsで4画面とカウント／AF／撮影の両スキン×通常／ダーク計28枚を生成した。シンプルのSELFIE通常／ダーク画像はv0.1.5の対応画像とSHA-256が一致した。MacのIDE Previewとは別の検証。
- Android lint：エラー0件、警告106件。旧画面の未使用リソース・上流由来の非推奨API等を含み、件数は前の版と同じ。
- APK署名検証：成功。既存と同じ証明書SHA-256 `c8a131c390f88da5600d8163649eaf721fccfaf66898776756d38263d2b6d0e1` とアプリIDを維持。
- CameraBLE・Service・撮影Controller・SelfieViewModel・撮影設定の仕様・GPL本文に差分なし。DataStoreにはスキン専用の読み書きだけを追加した。
- 端末固有のMac／N100手順はローカルだけで更新し、Gitと配布ソースから除外する。

**実機確認の現状：** Pixel 10 Proで5秒待機・5枚撮影はユーザー確認済み（Compose移行前、APK版未記録）。0.1.6の実機動作、網羅的な撮影確認とMacのAndroid StudioでのPreview操作は未検証。mainにはマージしない。

## 未ビルド：SELFIE画面の視認性の調整

クラウド環境（Android SDK・Google Mavenに接続不可）でコードのみ変更した。コンパイル・テスト・lintは未実行。N100で `testDebugUnitTest assembleDebug lintDebug` を通してから版番号を上げる。

- 選択中の設定チップを `primary` 塗り＋`onPrimary` 文字にした。選択中とトラックのコントラスト比は約1.2から4.7以上（Simple / kawaii、通常／ダーク）。
- 撮影中のSTOPを `onBackground` 塗り＋`background` 文字にした。背景とのコントラスト比は約1.2から12以上。
- 大表示を日本語に統一（AF中・撮影中・AF失敗・切断・撮影終了・停止・5秒）。`"AF ×"` と `"—"` の直書きをリソース化し、ドイツ語も追加。
- カウント以外の大表示を48spにそろえ、状態遷移での文字サイズの揺れを抑えた。撮影中は `primary` 色。
- 撮影中に「撮影操作 n回 · スキップ n回」を表示。待機中は「撮影 1秒 · 回数 5回」の設定要約を表示。
- 通知の進行表示を画面と同じ「試行 n / m · 撮影操作 · スキップ」にそろえた（従来は完了済み回数で、画面と1ずれていた）。
- `SelfieScreenTest` の期待文言を更新し、`running_counts` と `settings_summary` の確認を追加した。

## 未ビルド：kawaiiモードのSELFIE画面と音声の土台

前の「SELFIE画面の視認性の調整」はN100で67件のテスト・APKビルド成功、lintエラー0件を確認済み（ユーザー報告）。
今回の変更はクラウド環境で作成した。Android SDKとGoogle Mavenに接続できないため、Android・Compose部分はコンパイルしていない。N100で `testDebugUnitTest assembleDebug lintDebug` を通してから版番号を上げる。

- 純Kotlinの撮影制御と演出ロジックは、Kotlin 2.2コンパイラとJUnit 4で単体テストを実行した（`SelfieControllerTest` 10件・`SelfieShowTest` 4件、すべて成功。カメラ操作の定義は最小限の代用を使用）。
- `SelfieSettings` に初回だけの待機秒数と `show` を追加。`SelfieSettings.KAWAII`（1枚撮り・5回・初回10秒／以降5秒）。保存形式は変更なし。
- 切断時の音を `SelfieSound.DISCONNECTED` に分けた（音色は従来のAF失敗音と同じ）。
- `SelfieShow`：ポーズ・褒め言葉の抽選と音声の合図。撮影制御が公開する状態だけを見て、タイミングを変えない。
- `SelfieVoice`：`res/raw/kw_*` を1チャンネル・割り込み／待ちのルールで再生。音声ファイルはまだ無いため、現状は何も鳴らず、従来の電子音が鳴る。
- `KawaiiSelfieScreen`：モックを移植したプリクラ風画面。kawaiiスキンのときだけSELFIEタブで使う。
- テスト追加：`SelfieShowTest`、`KawaiiSelfieScreenTest`（Robolectric）。`ComposeRenderTest` にkawaii画面とAF失敗・終了を追加。
- 実機未確認：アニメーションの見え方、2〜3m先からの読みやすさ、小画面での中央部のはみ出し。


## 0.1.7：kawaii改造版のN100検証とAPKビルド

2026-10-13。クラウドで作成した `4bd0f36` の改造版を取得し、N100の実プロジェクト・依存関係でAndroid / Composeをコンパイルして検証した。配布版をversionName 0.1.7、versionCode 8に更新した。

- `testDebugUnitTest assembleDebug lintDebug --no-daemon`：成功。全81件、失敗・エラー・スキップ0件。
- 既存のテスト67件に、撮影Controllerの初回待機2件、SelfieShowの4件、API 31 / 36のkawaii画面8件を追加した構成。
- 初回10秒・以降5秒、全押し100ms・5試行、AF失敗のスキップ、撮影直前AF、STOPと切断、シンプルの従来設定を検証した。ポーズの重複回避と画面用ID／音声合図の一致も確認した。
- kawaiiの10状態、操作イベント、撮影中の接続操作無効化、320×480dp・文字倍率1.5でSTOPが表示され有効なことを確認した。
- API 36 / Robolectric Native Graphicsで4画面とカウント／AF／撮影／AF失敗／終了の両スキン・通常／ダーク計36枚を生成した。kawaiiのSELFIEは端末の通常／ダークによらず明るい配色という仕様。
- Android lint：エラー0件、警告106件。旧リソース・非推奨API・依存関係の更新案内等を含む。
- 同梱GPL本文はroot LICENSEと同一。フォントのOFL全文をAPK内に同梱している。CameraBLEの通信部分に今回の変更なし。
- APK署名は従来の証明書SHA-256 `c8a131c390f88da5600d8163649eaf721fccfaf66898776756d38263d2b6d0e1` とアプリIDを維持。上書きインストール用のversionCodeを増やした。
- 音声ファイルは未収録。今回の検証は演出合図と画面表示を対象とし、実際のMediaPlayerによる音声再生・待ち列／割り込みは未検証。音声未収録のため現在のAPKでは従来の電子音を使う。

**実機確認の現状：** Pixel 10 Proで5秒待機・5枚撮影はCompose移行前にユーザー確認済み。0.1.7の実機起動・撮影、画面OFF、通知STOP、2〜3mからの演出の見やすさ、Mac IDEのPreview操作は未検証。mainへはマージしない。
