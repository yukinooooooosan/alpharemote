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
