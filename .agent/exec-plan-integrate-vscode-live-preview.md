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
- [ ] Tornar o PreviewServer uma pré-condição de SDK distribuível, compilável e verificável.
- [ ] Atualizar contrato de plataforma, dependências e manifesto da extensão.
- [ ] Importar e adaptar a sessão de preview, sem shell, e conectá-la ao ciclo de vida existente.
- [ ] Adicionar testes unitários, de ativação e uma prova manual de ponta a ponta.
- [ ] Atualizar documentação, aviso de licença, versão e fluxo de publicação.
- [ ] Finalizar Outcomes & Retrospective e Editorial Report a partir de evidência real.

## Surprises & Discoveries

- Observation: o candidato declara VS Code ^1.85.0, TypeScript ^5.3.3 e Node typings ^20.11.0; a extensão publicada declara VS Code ^1.40.0, TypeScript ^3.6.4 e Node typings ^12.11.7.
  Evidence: os dois package.json; a instalação atual da extensão resolve TypeScript 3.9.10, @types/vscode 1.40.0 e @types/node 12.20.55.

- Observation: o candidato declara LGPL-2.1-only, enquanto a extensão destino usa Apache-2.0, mas o mantenedor confirmou que essa marcação foi usada apenas ao iniciar arquivos inéditos e que nenhum código VS Code do diretório foi publicado.
  Evidence: instrução do mantenedor em 2026-07-17 e git status --untracked-files=all, que mostra o diretório candidato como não versionado.

- Observation: PreviewSession.startProcess cria uma string de comando e executa child_process.spawn com shell: true.
  Evidence: src/extension.ts do candidato usa quote(), junta argumentos em command e fornece shell: true. Valores de configuração e caminhos podem assim ser interpretados pelo shell.

- Observation: totalcross.preview.PreviewServer está em arquivos modificados ou não versionados no checkout do SDK, e TotalCrossSDK/build.gradle exclui totalcross/preview/** e totalcross/LauncherRuntime.java de sourceSets.main.
  Evidence: sourceSets.main em /Users/flsobral/repos/totalcross-github/TotalCrossSDK/build.gradle e o status Git da árvore do SDK.

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

## Outcomes & Retrospective

O resultado atual é somente o plano e a pesquisa de integração. Nenhum arquivo de produto foi copiado ou alterado. A origem do cliente VS Code foi esclarecida como código inédito sem licença publicada, portanto será incorporada sob Apache-2.0. As duas compilações TypeScript passaram, mas não demonstram que um SDK distribuído contém PreviewServer; portanto não há Live Preview entregue ainda.

## Editorial Report

Esta seção será reconciliada no marco final. As afirmações abaixo descrevem apenas o planejamento, não uma implementação concluída.

### Editorial Summary

A integração pretende permitir a visualização de interfaces Java TotalCross na extensão VS Code já publicada. A pesquisa encontrou um cliente TypeScript local não versionado que depende de um serviço Java ainda não demonstrado no artefato do SDK. O mantenedor confirmou que o código VS Code candidato é inédito e sem licença publicada, permitindo sua entrada sob Apache-2.0. A entrega só será declarada depois de o serviço Java distribuível e a prévia real serem observados.

### Original Plan versus Actual Outcome

O objetivo é importar o cliente e expor comandos na extensão existente. Até aqui não houve importação, mudança de dependência ou publicação. A origem inicialmente parecia uma ambiguidade por causa dos cabeçalhos LGPL, mas o mantenedor a resolveu como código inédito sem licença publicada. A execução agora começa pelo SDK distribuível para que uma compilação TypeScript isolada não seja confundida com integração funcional.

### What Changed

Foi criado .agent/exec-plan-integrate-vscode-live-preview.md. Nenhum componente de produto, manifesto, dependência ou artefato mudou nesta etapa.

### Decisions and Trade-offs

O plano prefere extensão unificada e uma linha mínima moderna de VS Code, ao custo de não mais declarar suporte a clientes anteriores a 1.85. Como o cliente VS Code é inédito, a integração adotará os cabeçalhos Apache-2.0 do destino em vez de transportar uma marcação LGPL provisória.

### Unexpected Problems and Discoveries

O diretório fonte não tem histórico Git a importar, o PreviewServer não faz parte do conjunto principal compilado do SDK atual, e o lançamento Java usa shell. A confirmação de que os arquivos VS Code são inéditos resolveu a proveniência; a mudança continua coordenada entre distribuição, segurança e TypeScript.

### Validation and Measurable Results

Em 2026-07-16 foram observados status zero para npm run compile em vscode-extension e em /Users/flsobral/repos/totalcross-github/vscode/totalcross-live-preview. Não foi executado teste de integração, não foi iniciado PreviewServer e não houve medição de desempenho ou tamanho de VSIX.

### Useful Evidence and Examples

PreviewSession.startProcess, o manifesto do candidato e TotalCrossSDK/build.gradle são as evidências iniciais dos contratos e limitações. A saída concisa das duas compilações deve permanecer no histórico deste plano quando a execução começar.

### Limitations, Remaining Work, and Open Questions

Faltam SDK publicado que contenha PreviewServer, implementação integrada, testes e publicação. O impacto da retirada de suporte a VS Code anterior a 1.85 deve constar das notas de versão.

### Possible Article Angles

Para autores de extensões VS Code: como integrar uma prévia baseada em processo local sem injeção de shell. Para mantenedores de SDKs: por que o cliente de IDE e seu serviço Java precisam de contratos de distribuição testáveis, e não apenas de checkouts vizinhos que compilam.

### Suggested Narrative

Apresentar a necessidade de visualizar interfaces compiladas, a separação entre Webview e servidor Java local, os riscos de artefato SDK incompleto e shell, a migração para argumentos estruturados, a prova ponta a ponta e os limites da prévia somente de leitura.

### Claims Requiring Human Review

A versão do SDK que expõe PreviewServer e a decisão comercial de retirar suporte anterior a VS Code 1.85 exigem revisão humana antes de anúncio ou publicação. A origem do código VS Code candidato foi confirmada pelo mantenedor como inédita e sem licença publicada, mas permanece sujeita à revisão editorial e técnica normal.

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

3. Na árvore totalcross-github, executar o ExecPlan próprio de SDK e provar artefato publicado, usando diretório temporário explícito:

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

   Esperado: o VSIX contém ambos JavaScript compilados e manifesto; não contém src, node_modules, .agent ou checkout do SDK. Só então criar commit, tag v0.1.0 e seguir o workflow de publicação autorizado.

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

Revision note (2026-07-17): revisado após declaração do mantenedor de que todo o código-fonte relativo a VS Code no candidato é inédito, não publicado e deve ser tratado como sem licença previamente distribuída. O plano passa a importá-lo como novo trabalho Apache-2.0 e mantém como única pré-condição externa o SDK com PreviewServer distribuível.
