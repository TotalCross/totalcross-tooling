<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Integrar o Live Preview do TotalCross à extensão VS Code publicada

Este ExecPlan é um documento vivo e deve ser mantido de acordo com .agent/PLANS.md. As seções Progress, Surprises & Discoveries, Decision Log, Outcomes & Retrospective e Editorial Report devem refletir o estado real do trabalho, incluindo cada ponto de parada.

## Purpose / Big Picture

Depois desta mudança, a extensão publicada TotalCross.vscode-totalcross terá comandos para iniciar, abrir, recarregar, parar e configurar uma prévia visual somente para leitura de uma interface Java TotalCross. Em uma pasta de trabalho Gradle que já tenha classes compiladas e um SDK compatível, a pessoa poderá escolher TotalCross: Start Preview, selecionar a classe que estende totalcross.ui.MainWindow e ver, em um painel ao lado do editor, imagens PNG atualizadas pelo serviço Java local.

O resultado será uma extensão Marketplace unificada, não duas extensões concorrentes. O código candidato está hoje em /Users/flsobral/repos/totalcross-github/vscode/totalcross-live-preview e não está versionado naquele repositório. Por instrução do mantenedor, todo o código-fonte relativo a VS Code desse diretório é inédito e deve ser tratado como sem licença previamente distribuída; a incorporação em vscode-extension será trabalho novo sob Apache-2.0. A única pré-condição externa restante é a disponibilidade do serviço Java no SDK.

## Progress

- [x] (2026-07-16 23:58Z) Inspecionados o manifesto, entrada, testes e regras de governança de vscode-extension, o candidato externo e sua API Java.
- [x] (2026-07-16 23:58Z) Confirmado que npm run compile passa em vscode-extension e no candidato totalcross-live-preview; isto não prova a execução do serviço Java.
- [x] (2026-07-17 00:00Z) Mantenedor confirmou que todo o código-fonte relativo a VS Code no candidato é inédito, não publicado e deve ser tratado como sem licença previamente distribuída.
- [x] (2026-07-17 00:25Z) Tornado PreviewServer distribuível e verificável no commit SDK 0db656ad9; o jar contém o servidor e a prova HTTP passou.
- [x] (2026-07-17 00:31Z) Atualizados contrato de plataforma, dependências, manifesto e lockfile para VS Code 1.85 e TypeScript 5.3.3.
- [x] (2026-07-17 00:31Z) Integrada a sessão de preview sem shell ao ciclo de vida existente no commit 0f447a5.
- [x] (2026-07-17 00:45Z) Adicionados testes unitários e de ativação; `npm test` passou com 23 testes. A aceitação manual abriu e atualizou imagem no host de desenvolvimento, exercitou Reload e Open Preview Config, e Stop fechou a sessão.
- [x] (2026-07-17 00:39Z) Atualizados README e versão 0.1.0; o VSIX foi gerado e inspecionado, sem publicação Marketplace.
- [x] (2026-07-17 00:39Z) Finalizados Outcomes & Retrospective e Editorial Report a partir da evidência real.

## Surprises & Discoveries

- Observation: o candidato declara VS Code ^1.85.0, TypeScript ^5.3.3 e Node typings ^20.11.0; a extensão publicada declara VS Code ^1.40.0, TypeScript ^3.6.4 e Node typings ^12.11.7.
  Evidence: os dois package.json; a instalação atual da extensão resolve TypeScript 3.9.10, @types/vscode 1.40.0 e @types/node 12.20.55.

- Observation: o candidato declara LGPL-2.1-only, enquanto a extensão destino usa Apache-2.0, mas o mantenedor confirmou que essa marcação foi usada apenas ao iniciar arquivos inéditos e que nenhum código VS Code do diretório foi publicado.
  Evidence: instrução do mantenedor em 2026-07-17 e git status --untracked-files=all, que mostra o diretório candidato como não versionado.

- Observation: PreviewSession.startProcess cria uma string de comando e executa child_process.spawn com shell: true.
  Evidence: src/extension.ts do candidato usa quote(), junta argumentos em command e fornece shell: true. Valores de configuração e caminhos podem assim ser interpretados pelo shell.

- Observation: totalcross.preview.PreviewServer está em arquivos modificados ou não versionados no checkout do SDK, e TotalCrossSDK/build.gradle exclui totalcross/preview/** e totalcross/LauncherRuntime.java de sourceSets.main.
  Evidence: sourceSets.main em /Users/flsobral/repos/totalcross-github/TotalCrossSDK/build.gradle e o status Git da árvore do SDK.

- Observation: HttpServer não manteve o processo Java vivo depois de imprimir a URL, e threads AWT sobreviveram ao primeiro shutdown.
  Evidence: a primeira prova recebeu connection refused; PreviewServerTest inicialmente recebeu shutdown HTTP 200, mas o processo não saiu em dez segundos. O commit SDK 0db656ad9 usa CountDownLatch e System.exit(0) após /shutdown; a prova final observou health 200, frame 200 de 487 bytes e término do processo.

- Observation: ativar a extensão completa no host de teste dependeria de vscjava.vscode-java-pack, que não é instalada pelo host isolado de @vscode/test-electron.
  Evidence: a primeira execução de `npm test` falhou ao ativar a extensão por dependência desconhecida. O teste final registra diretamente o módulo Live Preview e confirma seus cinco comandos, enquanto a aceitação manual usou o host de desenvolvimento da extensão completa.

- Observation: o VSIX de produção inclui 535 arquivos de node_modules, que são dependências de runtime existentes; ele não inclui src, .agent nem o checkout do SDK.
  Evidence: `npx --yes @vscode/vsce@latest package --out /tmp/vscode-totalcross-0.1.0.vsix` gerou 580 arquivos, 1,59 MB, e `unzip -l` confirmou package.json, out/extension.js e out/live-preview.js.

## Decision Log

- Decision: consolidar como vscode-extension/src/live-preview.ts, ativado pela extensão existente, em vez de publicar o candidato como uma segunda extensão.
  Rationale: existe um só publisher Marketplace e uma experiência única de comandos TotalCross; o ExtensionContext, a desativação e o conjunto de testes devem ser compartilhados.
  Date/Author: 2026-07-16 / Codex.

- Decision: elevar o mínimo para VS Code ^1.85.0 e usar TypeScript 5.3.x, em vez de retroportar a API do candidato para VS Code 1.40.
  Rationale: o candidato já foi escrito e compilado para essa base. Declarar 1.40 sem uma prova de compatibilidade seria incorreto e manteria uma adaptação antiga sem testes.
  Date/Author: 2026-07-16 / Codex.

- Decision: tratar o candidato VS Code como trabalho inédito sem licença distribuída e incorporá-lo com os cabeçalhos Apache-2.0 vigentes em vscode-extension; não transportar cabeçalhos, campo license ou avisos LGPL do diretório candidato.
  Rationale: o mantenedor declarou explicitamente que a marcação LGPL foi provisória, que os arquivos não foram publicados e que todo o código-fonte relativo a VS Code deve ser tratado como sem licença. Isso elimina a ambiguidade de proveniência sem reescrever o histórico de qualquer material publicado.
  Date/Author: 2026-07-17 / Mantenedor e Codex.

- Decision: iniciar Java com executável e vetor de argumentos em child_process.spawn(javaCommand, args, { shell: false }), fixando o serviço em loopback.
  Rationale: argumentos estruturados preservam caminhos com espaços e impedem que configuração seja interpretada como shell. O Webview e a API são locais; preview remoto não é requisito.
  Date/Author: 2026-07-16 / Codex.

- Decision: consumir o contrato do SDK no commit 0db656ad9 durante o desenvolvimento da extensão.
  Rationale: esse commit torna PreviewServer, PreviewRunner e LauncherRuntime parte de totalcross-sdk e prova os endpoints essenciais. Uma release Maven/versionada deve substituir a referência de desenvolvimento antes da publicação Marketplace.
  Date/Author: 2026-07-17 / Codex.

- Decision: concluir a integração e empacotar o VSIX, mas não criar tag nem publicar no Marketplace nesta etapa.
  Rationale: a autorização solicitada cobre execução e commits; o PreviewServer ainda está representado pelo commit SDK 0db656ad9, não por um artefato SDK releaseado e documentado para consumidores. A publicação exige essa versão e autorização de release.
  Date/Author: 2026-07-17 / Codex.

## Outcomes & Retrospective

A extensão publicada agora contém o cliente de Live Preview no commit 0f447a5. O novo módulo cria uma única sessão descartável, registra os cinco comandos, usa Webview somente de leitura e chama PreviewServer apenas em `127.0.0.1`. O lançamento Java usa `spawn(javaCommand, args, { shell: false })`; os testes preservam caminhos com espaços como argumentos únicos e a busca por `shell: true`, `spawn(command)` e `quote` não encontrou ocorrências.

O contrato Java foi tornado distribuível e exercitado no commit SDK 0db656ad9: compilação, cinco testes `totalcross.preview.*`, jar contendo PreviewServer e uma prova HTTP com health 200, frame PNG de 487 bytes e shutdown do processo. A aceitação na extensão usou esse SDK local somente como prova de desenvolvimento: os comandos apareceram no host de desenvolvimento, Start abriu a aba TotalCross Live Preview com `Live preview updated` e imagem, Reload preservou a imagem, Open Preview Config abriu `totalcross.preview.json` e Stop fechou a aba. A primeira execução sem `extraClasspath` falhou como esperado; depois da configuração do jar e de `dist/libs`, a sessão pôde ser iniciada e encerrada.

As validações finais foram: `npm run compile`; `npm test` com 23 testes; `python3 tools/check-repository-governance.py`; `python3 -m unittest tests.test_repository_governance` com 17 testes; `npm run audit` sem vulnerabilidades; `git diff --check`; e geração de `/tmp/vscode-totalcross-0.1.0.vsix` (1,59 MB). A extensão não foi publicada nem etiquetada, pois ainda falta uma release SDK que consumidores possam referenciar e a respectiva autorização de release.

## Editorial Report

### Editorial Summary

A extensão TotalCross passou a reunir a prévia visual local no mesmo pacote Marketplace. Ela inicia um PreviewServer Java em loopback, busca frames PNG em Webview e encerra recursos ao fechar o painel ou executar Stop. O cliente candidato era código VS Code inédito e sem licença previamente distribuída; por isso, a implementação entrou como novo trabalho Apache-2.0.

### Original Plan versus Actual Outcome

O plano previa uma extensão unificada, argumentos Java sem shell, testes e um VSIX verificável. Todos esses resultados foram entregues. O serviço Java exigiu uma correção coordenada no SDK, concluída no commit 0db656ad9. A única parte intencionalmente adiada é publicação/tag: não há ainda artefato SDK releaseado para ser indicado ao usuário final.

### What Changed

`vscode-extension` agora exige VS Code 1.85+, TypeScript 5.3.3 e Node typings 20. O manifesto oferece os comandos Start, Open, Stop, Reload e Open Preview Config, mais as configurações de Java, classpath, porta, dimensões e polling. `src/live-preview.ts` implementa processo, configuração, descoberta de MainWindow, Webview, controle HTTP e observação de classes. README, testes e lockfile acompanham a mudança.

### Decisions and Trade-offs

O suporte mínimo foi elevado para VS Code 1.85 para conservar uma base TypeScript atual e compatível com o cliente. O host é fixado em loopback e não há configuração pública para classe do servidor, evitando exposição remota e variações de contrato. O VSIX ainda leva dependências Node de runtime já existentes; não houve refatoração de empacotamento fora deste plugin.

### Unexpected Problems and Discoveries

O PreviewServer originalmente não permanecia vivo e deixava threads AWT após shutdown; o SDK passou a aguardar shutdown e a encerrar o processo. O host de teste isolado não fornece a extensão Java declarada como dependência, então o teste registra o módulo de preview diretamente e a aceitação manual cobre a extensão completa. Um descarte imediato gerava avisos de DisposableStore; aguardar um turno do event loop no teste removeu os avisos.

### Validation and Measurable Results

Em 2026-07-17, `npm test` passou com 23 testes e sem avisos de descarte; a governança passou e seus 17 testes passaram; a auditoria informou zero vulnerabilidades. O VSIX contém `extension/package.json`, `extension/out/extension.js` e `extension/out/live-preview.js`, tem 580 arquivos e 1,59 MB. A prova SDK produziu frame PNG de 487 bytes. A aceitação de VS Code confirmou imagem atualizada em Start, Reload, abertura do arquivo de configuração e fechamento da aba por Stop.

### Useful Evidence and Examples

Os commits são `0db656ad9 feat(sdk): expose live preview server`, `0f447a5 feat(vscode): add local live preview` e `dbbf571 test(vscode): await preview activation cleanup`. O VSIX temporário está em `/tmp/vscode-totalcross-0.1.0.vsix`; não deve ser versionado. A configuração de desenvolvimento usada na aceitação incluiu o jar do SDK e seu diretório `dist/libs` em `totalcross.livePreview.extraClasspath`.

### Limitations, Remaining Work, and Open Questions

Antes de Marketplace, produzir e documentar uma versão SDK distribuída que inclua PreviewServer, substituir a referência de desenvolvimento por essa versão no guia de release e obter autorização para tag/publicação. Avaliar depois a redução do conteúdo de node_modules no VSIX, como tarefa separada para não refatorar plugins durante esta migração.

### Possible Article Angles

Como integrar uma prévia Java local segura a uma extensão VS Code: loopback, argumentos estruturados e ciclo de vida do processo. Outro ângulo é transformar um serviço interno de SDK em contrato distribuível e testável antes de conectá-lo a uma IDE.

### Suggested Narrative

Apresentar a prévia como ponte entre classes Java compiladas e uma Webview somente de leitura; explicar por que a entrega exigiu tanto um SDK que permanece vivo quanto um cliente que não passa configurações pelo shell; concluir com o encerramento coordenado e os limites de release.

### Claims Requiring Human Review

A versão pública do SDK que conterá PreviewServer e a comunicação da retirada de suporte anterior ao VS Code 1.85 precisam de revisão humana antes do anúncio. Não há dúvida pendente de licença do cliente VS Code: o mantenedor confirmou que ele era inédito e sem licença distribuída.

## Context and Orientation

vscode-extension é o subprojeto Marketplace deste repositório. vscode-extension/package.json declara out/extension.js como entrada, comandos atuais extension.* e compatibilidade ^1.40.0. vscode-extension/src/extension.ts é o único ponto de ativação e registra criação de projeto, pacote, deploy e migração; deactivate ainda não libera recursos.

O candidato externo concentra tudo em /Users/flsobral/repos/totalcross-github/vscode/totalcross-live-preview/src/extension.ts. Ele cria uma PreviewSession, um objeto descartável que abre um WebviewPanel, inicia processo Java local, consulta GET /health, busca repetidamente GET /frame e envia POST /show, /clear, /reload e /shutdown. Ele cria ou atualiza totalcross.preview.json na raiz da primeira pasta de trabalho. Essa configuração contém MainWindow, diretórios de classes compiladas, recursos e dependências, tamanho, modo de reload e caminho da imagem headless.

O processo esperado é:

    java <jvmArgs> -cp <classpath> totalcross.preview.PreviewServer --config <workspace>/totalcross.preview.json --host 127.0.0.1 --port <porta>

Classpath reúne caminhos do JSON e totalcross.livePreview.extraClasspath, resolvidos a partir da pasta de trabalho. Uma instalação funcional precisa conter classes do projeto e o jar do SDK que traz PreviewServer e dependências. A porta é local e efêmera se configurada como 0.

Os testes atuais residem em vscode-extension/src/test/suite, são iniciados por src/test/runTest.ts com @vscode/test-electron e index.ts encontra arquivos *.test.js. As regras em vscode-extension/AGENTS.md exigem npm run compile, npm test, python3 tools/check-repository-governance.py, python3 -m unittest tests.test_repository_governance e npm run audit.

## Plan of Work

O código-fonte relativo a VS Code no candidato é inédito e não publicado. Ao importá-lo, criar os arquivos de destino com os cabeçalhos Apache-2.0 correntes de vscode-extension e não transportar o campo license, cabeçalhos ou avisos LGPL provisórios do candidato. Como não há commit publicado nem autores históricos a preservar, a importação deve ser apresentada como novo trabalho da extensão, sem inventar proveniência adicional.

Em uma mudança coordenada, porém separada desta extensão, promover PreviewServer para um artefato SDK versionado e distribuível no repositório totalcross-github. O outro ExecPlan deve incluir totalcross.preview.PreviewServer, PreviewRunner, LauncherRuntime e a implementação de totalcross.preview no jar principal ou em jar companion documentado, removendo exclusões que os impedem. Não embutir classes Java na extensão nem apontar a documentação a um checkout pessoal. Produzir prova de java -cp que inicia o servidor, emite TOTALCROSS_PREVIEW_URL e responde 200 em /health; registrar versão, release e commit no plano.

Depois dos portões, atualizar apenas vscode-extension. Em package.json, definir engines.vscode como ^1.85.0 e version como 0.1.0. Preservar todos os comandos extension.* e acrescentar eventos de ativação, contribuições de comandos e configurações para totalcross.startPreview, totalcross.openLivePreview, totalcross.stopPreview, totalcross.reloadPreview, totalcross.openPreviewConfig e onWebviewPanel:totalcrossLivePreview. Conservar os nomes de configuração existentes no namespace totalcross.livePreview: javaCommand, jvmArgs, extraClasspath, port, width, height, orientation, deviceProfile, framePollInterval e controlTimeout. Não manter host ou serverClass como configurações públicas: usar sempre 127.0.0.1 e totalcross.preview.PreviewServer.

Fixar @types/vscode em 1.85.0, typescript em 5.3.3 e @types/node numa versão Node 20 compatível, e atualizar @vscode/test-electron para uma versão que rode VS Code 1.85 em Node 20. Regenerar package-lock.json apenas com npm; não mudar dependências de runtime sem necessidade. Em tsconfig.json, usar ES2020 como target e lib, mantendo commonjs, rootDir src, outDir out, source maps e strict. Corrigir apenas erros inevitáveis da migração de TypeScript, sem reformar criação, pacote, deploy ou migração.

Criar src/live-preview.ts a partir do código candidato, com cabeçalho Apache-2.0 vigente no destino. Não copiar os cabeçalhos LGPL do candidato nem adicionar uma nota de atribuição que sugira material anteriormente publicado. A interface de ciclo de vida deve ser:

    export function activateLivePreview(context: vscode.ExtensionContext): vscode.Disposable;
    export function deactivateLivePreview(): void;

activateLivePreview cria exatamente uma PreviewSession, registra os cinco comandos e o serializador do painel, agrega descartáveis ao context e retorna um descartável que encerra processo, painel, watchers, timer e canal de saída. deactivateLivePreview é idempotente. Em src/extension.ts, chamar activateLivePreview(context) após os recursos existentes e chamar deactivateLivePreview() em deactivate(); não criar segundo activate nem registrar comandos duas vezes.

Preservar o comportamento do candidato: usar primeira pasta da área de trabalho, criar totalcross.preview.json com defaults, descobrir MainWindow nas fontes Java e preferir Run*Application, aguardar /health até 15 segundos, criar painel ao lado do editor, observar classOutputPaths e mostrar ou limpar a classe Java ativa conforme sua classe compilada. Adaptar startProcess para montar const args: string[] com JVM args, -cp, classpath, classe fixa e --config, --host, --port; chamar childProcess.spawn(javaCommand, args, { cwd, shell: false, env: process.env }). Não manter quote() nem uma string de comando executável. Registrar uma representação diagnóstica dos argumentos sem executá-la. Validar porta inteira de 0 a 65535, intervalo de frame mínimo de 100 ms e timeout de controle mínimo de 1000 ms. No macOS, conservar -Dapple.awt.UIElement=true se o usuário não a informou.

O HTML do Webview mantém uma Content Security Policy que aceita imagens somente de webview.cspSource, http://127.0.0.1:*, http://localhost:* e data:. Escapar todo texto interpolado e consultar /frame com query de cache. A prévia fica somente de leitura; não encaminhar mouse, toque, teclado ou navegação.

Para testes sem Java, exportar somente um namespace documentado de teste:

    export const livePreviewTest = { defaultConfigFromSettings, packageName, topLevelClassName, relativeClassPath, previewJvmArgs, buildJavaArguments };

A chave é interna a testes. Cobrir orientação, pacote/classe Java, conversão de caminho .class inclusive classe interna, JVM args de macOS e argumentos que preservam caminho com espaços sem shell quoting. Permitir plataforma opcional em previewJvmArgs para o teste não depender da máquina.

Em src/test/suite/extension.test.ts, substituir o exemplo por teste que localiza TotalCross.vscode-totalcross, aguarda activate() e confirma os cinco comandos em vscode.commands.getCommands(true). Criar src/test/suite/live-preview.test.ts para utilidades. Não tornar o suite dependente da outra árvore local.

Atualizar README.md com comandos, VS Code 1.85+, JDK 17, SDK PreviewServer compatível, JSON criado, classpath e extraClasspath, atualização, loopback e limitações. Incluir exemplo que aponta a um jar SDK publicado, nunca a caminho pessoal. A decisão atual não exige mudança em NOTICE; preservar autores e avisos históricos existentes. Ajustar .vscodeignore somente se a inspeção comprovar que out/live-preview.js não é embalado ou que arquivo de desenvolvimento entraria no VSIX.

## Concrete Steps

1. Na raiz /Users/flsobral/repos/totalcross-tooling, confirmar estado e ler o plano:

       git status --short --branch
       git -C vscode-extension status --short --branch
       sed -n '1,360p' .agent/exec-plan-integrate-vscode-live-preview.md

   Esperado: vscode-extension está limpo antes da implementação e Progress é atualizado com data e resultado.

2. Confirmar no Decision Log que a declaração do mantenedor de 2026-07-17 continua aplicável ao conjunto de arquivos importado. Criar os novos arquivos do destino com cabeçalhos Apache-2.0; não copiar package.json, README, .vscodeignore, .gitignore, .vscode nem comentários de licença do candidato sem revisão específica.

3. Na árvore totalcross-github, o ExecPlan próprio foi concluído no commit 0db656ad9. Antes da publicação Marketplace, provar o artefato versionado que sucederá esse commit usando diretório temporário explícito:

       java -cp "/caminho/para/totalcross-sdk-preview.jar:/caminho/para/classes-do-app" totalcross.preview.PreviewServer --config /tmp/totalcross-preview-e2e/totalcross.preview.json --host 127.0.0.1 --port 0
       curl -fsS http://127.0.0.1:<porta>/health

   Esperado: a primeira saída contém TOTALCROSS_PREVIEW_URL e curl devolve JSON com "ok":true. Registrar versão e commit; não usar jar de arquivos não versionados como evidência de release.

4. Em vscode-extension, atualizar manifesto, tsconfig e lockfile:

       npm install --save-dev @types/vscode@1.85.0 @types/node@20 typescript@5.3.3 @vscode/test-electron@latest
       npm run compile
       git diff -- package.json package-lock.json tsconfig.json

   Esperado: TypeScript 5.3.x compila. Se a última versão de @vscode/test-electron não rodar em Node 20, selecionar a mais recente compatível e registrar versão e razão.

5. Criar o módulo, alterar a entrada, manifesto, README e testes, validando cada grupo:

       npm run compile
       git diff --check
       rg -n "shell:\\s*true|spawn\\(command|function quote" src/live-preview.ts

   Esperado: compilação limpa, diff sem espaços finais e nenhuma ocorrência indicada pela busca.

6. Rodar validação focada a partir de vscode-extension:

       npm test
       python3 tools/check-repository-governance.py
       python3 -m unittest tests.test_repository_governance
       npm run audit

   Esperado: status zero. Se npm test falhar por download, sandbox ou display, registrar mensagem completa, repetir em ambiente apropriado e não confundir compilação com teste de integração.

7. Fazer aceitação manual em cópia descartável de app TotalCross Gradle, configurando extraClasspath para o jar SDK validado. Compilar, abrir como workspace único, executar Start Preview, selecionar MainWindow, observar painel e canal TotalCross Live Preview, executar Reload, Stop e Open Preview Config, e fechar/restaurar o painel. Guardar captura e trecho de log sem caminhos pessoais.

8. Atualizar todas as seções vivas e inspecionar pacote sem publicar:

       npx @vscode/vsce package --out /tmp/vscode-totalcross-0.1.0.vsix
       unzip -l /tmp/vscode-totalcross-0.1.0.vsix | rg 'extension/out/(extension|live-preview)\\.js|extension/package\\.json'
       git status --short

   Esperado: o VSIX contém ambos JavaScript compilados e manifesto; dependências Node de runtime podem continuar presentes até uma tarefa separada de empacotamento, mas ele não contém src, .agent ou checkout do SDK. Só então criar commits e, quando houver artefato SDK releaseado e autorização, criar tag v0.1.0 e seguir o workflow de publicação.

## Validation and Acceptance

Em VS Code 1.85+, com workspace único de app TotalCross compilado e jar PreviewServer identificado, os cinco comandos aparecem na Paleta. Start Preview cria totalcross.preview.json quando necessário, permite selecionar MainWindow e abre o painel. O canal mostra início Java sem string de shell e o painel recebe imagem de /frame.

Ao focar classe Java compilada exibível, o canal registra Preview is showing e o painel muda. Ao focar classe não compilada ou incompatível, a prévia fica preta por /clear sem derrubar a extensão. Reload usa /reload ou reinicia só quando reloadMode é full ou a chamada falha. Stop chama /shutdown, encerra processo, watchers e painel; fechar painel tem a mesma limpeza e restaurá-lo reinicia a sessão.

npm run compile, npm test, os dois checks de governança e npm run audit devem passar. Os testes devem provar preservação de caminho com espaço como um único argumento e a busca no módulo deve provar ausência de shell: true. A prova manual deve observar /health com 200 e uma imagem real. Copiar essa evidência para Outcomes & Retrospective e Editorial Report antes da conclusão.

## Idempotence and Recovery

Compilação, testes, audit e governança são repetíveis. A criação de totalcross.preview.json é idempotente: quando existe, defaults são mesclados somente em memória e escolhas do usuário não são sobrescritas; apagar o arquivo permite novo padrão. Porta 0 é reescolhida em cada tentativa.

Se Java falhar, usar canal para verificar classpath, classe e versão do SDK, parar a sessão e corrigir configuração; não introduzir fallback de shell. Se dependências precisarem ser revertidas, usar commit de reversão ou patch reverso revisado apenas nos arquivos desta mudança, nunca git reset --hard em árvore possivelmente compartilhada. O plano não autoriza apagar o diretório externo ou arquivos não versionados do SDK.

## Artifacts and Notes

Referenciar no fim: a decisão registrada de que o cliente VS Code era inédito; release/commit do SDK; diffs de package.json, package-lock.json, src/extension.ts e src/live-preview.ts; os testes; logs concisos; captura do Webview; e o VSIX temporário. Não adicionar JARs, capturas ou logs grandes ao Git sem escopo explícito.

## Interfaces and Dependencies

O manifesto expõe identificadores estáveis totalcross.startPreview, totalcross.openLivePreview, totalcross.stopPreview, totalcross.reloadPreview e totalcross.openPreviewConfig. O módulo fornece activateLivePreview e deactivateLivePreview a src/extension.ts e não exporta um segundo activate.

O módulo usa apenas APIs Node child_process, fs, http, net e path e APIs VS Code, sem pacote de runtime novo. O processo Java depende de totalcross.preview.PreviewServer numa versão documentada do SDK e do contrato GET /health, GET /frame e POST /show, /clear, /reload e /shutdown. Mudança de contrato exige atualizar plano, módulo, testes e README juntos.

Revision note (2026-07-16): criado após inspecionar vscode-extension, o candidato local totalcross-live-preview e o SDK vizinho. Inicialmente registrou a licença do candidato como pré-condição por causa de seus cabeçalhos LGPL.

Revision note (2026-07-17): revisado após declaração do mantenedor de que todo o código-fonte relativo a VS Code no candidato é inédito, não publicado e deve ser tratado como sem licença previamente distribuída. O plano passa a importá-lo como novo trabalho Apache-2.0.

Revision note (2026-07-17): atualizado após o commit SDK 0db656ad9 compilar, testar e provar PreviewServer. A integração TypeScript pode usar o contrato desse commit, mas a publicação Marketplace continuará condicionada a uma versão de SDK distribuível.

Revision note (2026-07-17): implementação concluída nos commits 0f447a5 e dbbf571; compilação, 23 testes de extensão, governança, auditoria, VSIX e aceitação manual de Start, Reload, Open Preview Config e Stop passaram. A inspeção do VSIX corrigiu a expectativa inicial: node_modules de runtime continua no pacote atual, sem fontes, plano ou checkout SDK. Publicação e tag permanecem deliberadamente fora do escopo até existir release SDK e autorização.
