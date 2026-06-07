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
- Contratos Firebase, regras Firestore/Storage e testes de regras preparados; a configuracao Android Firebase esta aplicada e as regras reais foram publicadas no projeto Firebase.
- Config Firebase Android criado para o projeto `gen-lang-client-0780081219`, app `TaskFlow Android`, package `com.taskflow`; `app/google-services.json` esta presente e o Gradle processa `google-services` em debug/release.

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

- PowerShell 7+ (`pwsh`) para executar os scripts de QA no Windows, Linux ou GitHub Actions.
- Android SDK em `C:\TaskFlowAndroidSdk` ou SDK equivalente configurado.
- Java 17 disponivel pelo projeto em `.tools/jdk-17`.
- Node/npm para testes de regras Firebase.

O Gradle Wrapper esta no repositorio, entao Gradle global nao e necessario.

## Comandos Principais

Verificacao local completa do MVP:

```powershell
npm run verify:local-mvp
```

Diagnostico dos insumos externos ainda necessarios:

```powershell
npm run verify:external-readiness
```

Para usar esse diagnostico como gate de release, execute:

```powershell
pwsh -NoProfile -File tools/qa/verify-external-readiness.ps1 -Strict
```

O mesmo fluxo esta preparado para GitHub Actions em `.github/workflows/android-ci.yml`.
O comando tambem atualiza `docs/qa/release-manifest.json` com tamanho e SHA-256 dos APK/AAB.

Se estiver usando apenas Windows PowerShell 5 localmente, execute os scripts `.ps1` diretamente com `powershell`; os scripts tambem foram validados com `pwsh`, que e o caminho recomendado para CI.

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

O projeto inclui `app/google-services.json`, processa o plugin Google Services em debug/release e possui `FirebaseTaskFlowDataSource` ligado ao SDK real para Auth, Firestore, Storage e FCM. O app continua local-first, mas tenta Auth Firebase real no login/cadastro e mantem fallback local quando o Firebase real nao estiver disponivel. No cadastro, a criacao do usuario no Auth nao depende da gravacao imediata do perfil no Firestore; se o Firestore estiver temporariamente indisponivel, essa gravacao remota fica em best effort e o app preserva a sessao local-first.

Para validar regras localmente:

```powershell
npm install
npm run test:firestore-rules
npm run test:storage-rules
npm run verify:firebase-real
```

Estado Firebase real:

- Auth e-mail/senha: validado com criacao, login e exclusao de usuario smoke via API Firebase Auth.
- Firestore: regras reais publicadas e dry-run validado no projeto Firebase.
- Storage: regras reais publicadas e dry-run validado no projeto Firebase.
- FCM: SDK e registro de token preparados; validacao real depende do app rodando em dispositivo/emulador com o projeto Firebase ativo.

Projeto atualmente associado em `.firebaserc`:

- Project ID: `gen-lang-client-0780081219`
- Android App ID: `1:209004797664:android:ef4fc149b5b033f782ba85`
- Package: `com.taskflow`

Pendencias do Firebase real:

- FCM: validacao de token/push em runtime ainda depende de execucao do app em dispositivo/emulador com Google Play Services ativo.
- Fluxos remotos no app: login/cadastro Auth estao implementados; sincronizacao Firestore/Storage precisa de QA de runtime apos as regras reais publicadas.

## Artefatos

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Manifesto de release: `docs/qa/release-manifest.json`

O release atual usa uma assinatura local de desenvolvimento (`~/.android/debug.keystore`) para permitir validacao. Para distribuicao real, gere e guarde uma keystore de producao.

Para assinatura de producao, defina as variaveis abaixo antes de gerar release:

```powershell
$env:TASKFLOW_RELEASE_STORE_FILE='C:\caminho\release.keystore'
$env:TASKFLOW_RELEASE_STORE_PASSWORD='<senha-da-store>'
$env:TASKFLOW_RELEASE_KEY_ALIAS='<alias-da-chave>'
$env:TASKFLOW_RELEASE_KEY_PASSWORD='<senha-da-chave>'
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease :app:bundleRelease --console=plain
npm run release:manifest
```

Sem essas variaveis, o Gradle usa a assinatura local de desenvolvimento apenas para QA.

## Validacao Recente

Comandos executados com sucesso neste workspace:

- `.\gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest --console=plain`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:bundleRelease --console=plain --stacktrace`
- `npm run test:firebase-rules`
- `npm run verify:local-mvp`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:compileDebugKotlin --console=plain --stacktrace`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest --console=plain`
- `.\gradlew.bat --no-daemon --max-workers=1 :app:bundleRelease --console=plain`
- Instalacao e abertura do APK debug no emulador `TaskFlow_API35`, sem crash/ANR do pacote `com.taskflow` no log filtrado.

## Bloqueios Para Conclusao Total

- Repositorio GitHub publicado em `https://github.com/gutobaddini-create/taskflow`; GitHub CLI autenticado para PR/releases/automacoes.
- Firebase Android config esta presente; Auth e-mail/senha foi validado e regras reais de Firestore/Storage foram publicadas no projeto Firebase.
- Sem aparelho fisico desbloqueado e acessivel, entao aceite fisico e instalacao release em aparelho real seguem pendentes.

Consulte `docs/ROADMAP_TaskFlow.md` para o checklist completo e os registros de decisao/bloqueio.
Consulte `docs/QA_TaskFlow.md` para a evidencia atual de build, artefatos e smoke QA no emulador.
Consulte `docs/RELEASE_HANDOFF_TaskFlow.md` para o resumo de entrega local, artefatos, hashes e proximos insumos externos.
Consulte `docs/COMPLETION_AUDIT_TaskFlow.md` para a matriz requisito/evidencia/status do objetivo completo.
