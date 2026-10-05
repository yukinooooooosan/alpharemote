# 開発版の検証記録

2026-10-06、実機確認は後回しとする方針で検証した。

## 自動検証

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
