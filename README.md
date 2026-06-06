# TaskFlow Android

TaskFlow e um app Android nativo em Kotlin/Jetpack Compose para organizar tarefas, lembretes, materiais e compartilhamento de convites. O MVP atual esta em modo local-first: Room/DataStore sao a fonte principal, notificacoes locais funcionam no emulador e a fronteira Firebase ja esta preparada para integracao futura.

## Estado Atual

- App Android nativo em `app/`, pacote `com.taskflow`.
- UI em Jetpack Compose e Material 3.
- Persistencia local com Room e DataStore.
- Lembretes locais com `AlarmManager`, notificacoes e acoes `Abrir`, `Concluir` e `Adiar`.
- Materiais com picker de imagem/documento, camera, links, campos complementares e checklist.
- Compartilhamento local com texto de convite, deep link `taskflow://invite/...` e permissao local.
- Fila de operacoes pendentes para sync futuro de espacos, listas, tarefas, lembretes, anexos, links, campos, checklist, comentarios e convites.
- Contratos Firebase, regras Firestore/Storage e testes de regras preparados, mas Firebase real permanece desativado sem credenciais.

## Estrutura

```text
app/
  src/main/java/com/taskflow/
    core/          design, navegacao, notificacoes, permissoes
    data/          Room, DataStore, repositorios, contratos remotos
    domain/        modelos, repositorio, consultas
    feature/       telas Compose por area
docs/
  PRD_TaskFlow.md
  ROADMAP_TaskFlow.md
  DESIGN_TaskFlow.md
mockups/
firebase/
  firestore.rules
  storage.rules
tools/firebase-rules/
```

## Requisitos Locais

- Windows com PowerShell.
- Android SDK em `C:\TaskFlowAndroidSdk` ou SDK equivalente configurado.
- Java 17 disponivel pelo projeto em `.tools/jdk-17`.
- Node/npm para testes de regras Firebase.

O Gradle Wrapper esta no repositorio, entao Gradle global nao e necessario.

## Comandos Principais

Verificacao local completa do MVP:

```powershell
npm run verify:local-mvp
```

O mesmo fluxo esta preparado para GitHub Actions em `.github/workflows/android-ci.yml`.
O comando tambem atualiza `docs/qa/release-manifest.json` com tamanho e SHA-256 dos APK/AAB.

Comandos individuais:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest :app:compileDebugKotlin --console=plain
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug --console=plain
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease --console=plain
.\gradlew.bat --no-daemon --max-workers=1 :app:compileDebugAndroidTestKotlin --console=plain
npm run release:manifest
```

Teste instrumentado de notificacoes no emulador:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:connectedDebugAndroidTest --console=plain "-Pandroid.testInstrumentationRunnerArguments.class=com.taskflow.ReminderNotificationInstrumentedTest"
```

Execucao direta pelo ADB, util quando o Gradle/UTP fica instavel no emulador:

```powershell
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb shell am instrument -w -r -e class com.taskflow.ReminderNotificationInstrumentedTest com.taskflow.test/androidx.test.runner.AndroidJUnitRunner
```

## Instalar e Abrir no Emulador

```powershell
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell pm grant com.taskflow android.permission.POST_NOTIFICATIONS
& $adb shell monkey -p com.taskflow -c android.intent.category.LAUNCHER 1
```

## Firebase

O MVP nao exige `google-services.json` para compilar. A classe `FirebaseTaskFlowDataSource` falha fechada ate o projeto Firebase real ser configurado.

Para validar regras localmente:

```powershell
npm install
npm run test:firestore-rules
npm run test:storage-rules
```

Para concluir Firebase real, sera necessario fornecer:

- projeto Firebase;
- `google-services.json`;
- Auth com e-mail/senha habilitado;
- Firestore;
- Storage;
- FCM;
- credenciais/ambiente de teste.

## Artefatos

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Manifesto de release: `docs/qa/release-manifest.json`

O release atual usa uma assinatura local de desenvolvimento (`~/.android/debug.keystore`) para permitir validacao. Para distribuicao real, gere e guarde uma keystore de producao.

## Validacao Recente

Comandos executados com sucesso neste workspace:

- `.\gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest --console=plain`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:bundleRelease --console=plain --stacktrace`
- `npm run test:firebase-rules`
- `npm run verify:local-mvp`
- Instalacao e abertura do APK debug no emulador `TaskFlow_API35`, sem crash/ANR do pacote `com.taskflow` no log filtrado.

## Bloqueios Para Conclusao Total

- Sem remoto GitHub configurado (`git remote -v` vazio), entao push/PR dependem de URL de repositorio.
- Sem `google-services.json`, entao Firebase Auth/Firestore/Storage/FCM real nao pode ser validado.
- Sem aparelho fisico desbloqueado e acessivel, entao aceite fisico e instalacao release em aparelho real seguem pendentes.

Consulte `docs/ROADMAP_TaskFlow.md` para o checklist completo e os registros de decisao/bloqueio.
Consulte `docs/QA_TaskFlow.md` para a evidencia atual de build, artefatos e smoke QA no emulador.
