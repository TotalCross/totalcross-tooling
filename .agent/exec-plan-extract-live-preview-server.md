<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Extrair o servidor de Live Preview para totalcross-tooling

Este ExecPlan é um documento vivo e deve ser mantido conforme `.agent/PLANS.md`. As seções `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective` e `Editorial Report` devem refletir o estado real do trabalho em cada ponto de parada.

## Purpose / Big Picture

Depois desta mudança, o servidor que oferece imagens Live Preview para IDEs será um projeto Java próprio deste repositório, publicado como `com.totalcross:totalcross-live-preview-server`. Uma integração, como `vscode-extension`, iniciará `com.totalcross.livepreview.LivePreviewServer` a partir da distribuição desse projeto, em vez de exigir que `totalcross-sdk.jar` contenha um servidor HTTP, configuração JSON, carregador de classes ou código de recarga.

O SDK continuará sendo necessário para executar uma aplicação TotalCross, mas sua parte específica de preview se reduzirá ao contrato Java público único `totalcross.preview.PreviewRuntime` (incluindo seu consumidor de frame aninhado). O servidor implementará a orquestração: leitura de `totalcross.preview.json`, carregamento de classes do aplicativo, captura de imagens, endpoints HTTP e ciclo de vida de reload. A demonstração final será observável: instalar a distribuição do novo projeto, iniciar o servidor com uma aplicação de exemplo, receber HTTP 200 em `/health`, PNG em `/frame`, e usar a extensão VS Code sem `totalcross.preview.PreviewServer` no SDK.

O código atual do servidor no repositório irmão `/Users/flsobral/repos/totalcross-github` possui cabeçalhos LGPL-2.1-only. O mantenedor confirmou que quaisquer fontes incluídas naquele repositório pelo commit `0db656ad9` ainda não foram publicadas e podem ser utilizadas neste novo projeto. Assim, `live-preview-server` será LGPL-2.1-only e poderá importar diretamente essas fontes, preservando seus cabeçalhos, autores e proveniência. A licença Apache-2.0 da raiz continua sendo apenas o padrão do repositório multi-projeto; o `LICENSE` próprio de `live-preview-server/` prevalece para seus arquivos e distribuições.

## Progress

- [x] (2026-07-17 00:50Z) Lidos `.agent/PLANS.md`, `AGENTS.md`, o plano de integração VS Code e a árvore atual do SDK.
- [x] (2026-07-17 00:50Z) Inventariados os componentes atuais: `totalcross.preview.PreviewServer`, `PreviewConfig`, `PreviewConfigLoader`, superfícies headless, `DisposableAppClassLoader` e `totalcross.PreviewRunner` no SDK.
- [x] (2026-07-17 01:05Z) Mantenedor definiu `live-preview-server` como LGPL-2.1-only e autorizou o uso das fontes adicionadas em `0db656ad9`; a política raiz foi documentada como padrão Apache com exceções por projeto.
- [ ] Criar o projeto Gradle `live-preview-server` sob LGPL-2.1-only, com artefato e distribuição instalável.
- [ ] Reduzir o namespace público `totalcross.preview` no SDK ao contrato de integração e remover do JAR SDK a implementação Live Preview.
- [ ] Implementar o servidor, configuração, carregamento de aplicativo, superfície PNG e endpoints HTTP no novo projeto.
- [ ] Atualizar a extensão VS Code e a documentação para iniciar o novo artefato, não uma classe do SDK.
- [ ] Executar testes unitários, testes entre os dois repositórios, prova HTTP, prova VS Code, inspeção de JAR/ZIP e validações de governança.
- [ ] Finalizar `Outcomes & Retrospective` e todos os subtítulos do `Editorial Report` com evidências observadas.

## Surprises & Discoveries

- Observation: o commit SDK `0db656ad9` colocou no JAR principal tanto o servidor HTTP quanto a infraestrutura de preview, mas a extensão VS Code só conhece o nome fixo `totalcross.preview.PreviewServer`.
  Evidence: `vscode-extension/src/live-preview.ts` define `PREVIEW_SERVER_CLASS = 'totalcross.preview.PreviewServer'`; `jar tf TotalCrossSDK/build/libs/totalcross-sdk-7.2.2.jar` da prova anterior continha `totalcross/preview/PreviewServer.class` e `totalcross/PreviewRunner.class`.

- Observation: a classe `totalcross.PreviewRunner` atualmente chama métodos de preview com visibilidade de pacote em `totalcross.LauncherRuntime`; um servidor externo não poderia reutilizá-los sem ampliar um contrato SDK.
  Evidence: `PreviewRunner.reload`, `showClass` e `startRuntime` chamam `preparePreviewMainWindowReload`, `replacePreviewMainWindow`, `showPreviewContainer` e `showPreviewControl`, hoje sem modificador `public`.

- Observation: `totalcross.preview` também abriga adaptadores AWT usados pelo launcher desktop (`AwtWindowBackend`, `AwtCanvasSurface`, `AppletPreviewSurface`), que não são a implementação do servidor IDE.
  Evidence: `totalcross.Launcher` e `totalcross.LauncherRuntime` importam essas classes, enquanto somente `PreviewServer` e `PreviewRunner` tratam HTTP, arquivo JSON, carregador de aplicação e recarga.

- Observation: os arquivos Java do servidor no SDK carregam LGPL-2.1-only, enquanto arquivos novos neste repositório seguem Apache-2.0.
  Evidence: cabeçalhos de `TotalCrossSDK/src/main/java/totalcross/preview/PreviewServer.java` e `AGENTS.md` da raiz deste repositório.

- Observation: o mantenedor autorizou a utilização neste projeto de quaisquer fontes adicionadas ao repositório `totalcross` pelo commit `0db656ad9`, pois elas ainda não foram publicadas.
  Evidence: instrução do mantenedor em 2026-07-17; o commit local `0db656ad9 feat(sdk): expose live preview server` é o limite explícito de proveniência.

## Decision Log

- Decision: criar o projeto independente em `live-preview-server/`, com group `com.totalcross`, artifact `totalcross-live-preview-server` e classe principal `com.totalcross.livepreview.LivePreviewServer`.
  Rationale: o projeto fica próximo da extensão e dos demais artefatos de tooling, pode ter versão e distribuição próprias e deixa claro que o processo é uma ferramenta de desenvolvimento, não parte do runtime distribuído a aplicações TotalCross.
  Date/Author: 2026-07-17 / Codex.

- Decision: o único contrato público de Live Preview no SDK após a extração será `totalcross.preview.PreviewRuntime`, com `PreviewRuntime.FrameConsumer` aninhado; classes de janela desktop que permanecem necessárias ao launcher serão movidas para `totalcross.desktop` ou pacote interno equivalente, e nenhum servidor, JSON, carregador de classes, superfície headless ou `PreviewRunner` ficará no SDK.
  Rationale: separar o namespace do contrato da implementação permite ao SDK atender consumidores sem carregar uma ferramenta de IDE. Renomear as classes AWT evita que a inspeção de JAR confunda implementação desktop legada com Live Preview.
  Date/Author: 2026-07-17 / Codex.

- Decision: definir no SDK somente o contrato público `PreviewRuntime`. Seu tipo aninhado funcional `FrameConsumer` recebe uma imagem `BufferedImage`; a interface externa oferece `pumpEvents`, `replaceMainWindow`, `showContainer`, `showControl` e `close`.
  Rationale: uma interface pública única é o ponto mínimo de integração para o novo projeto comandar o launcher sem depender de tipos de configuração, HTTP ou carregamento de classes do servidor. A implementação concreta continua no runtime genérico `LauncherRuntime`, que passará a implementar `PreviewRuntime`; isso não coloca lógica de servidor no SDK.
  Date/Author: 2026-07-17 / Codex.

- Decision: licenciar `live-preview-server` sob LGPL-2.1-only e importar as fontes de Live Preview introduzidas em `0db656ad9`, preservando seus cabeçalhos LGPL e registrando a origem no `NOTICE` do projeto e nos corpos de commit.
  Rationale: o mantenedor autorizou expressamente o uso dessas fontes ainda não publicadas. Manter LGPL-2.1-only evita uma relicença implícita e mantém coerência com a origem do servidor, enquanto a proveniência explícita preserva autoria e permite auditoria futura.
  Date/Author: 2026-07-17 / Codex.

- Decision: manter `LICENSE` raiz como Apache-2.0 e declarar que ele é a licença padrão, não uma substituição das licenças de projetos individuais.
  Rationale: `totalcross-tooling` contém projetos independentes já documentados com licenças e históricos próprios. Um padrão raiz com exceções por diretório torna a política legível sem apagar a licença explícita de cada distribuição.
  Date/Author: 2026-07-17 / Codex.

- Decision: publicar dois formatos do servidor: JAR Maven fino, cuja dependência transitiva é o SDK, e distribuição Gradle `installDist`/ZIP que contém o lançador e todos os JARs de runtime em `lib/`.
  Rationale: Maven atende automação Java; a distribuição atende IDEs e usuários locais. A extensão já aceita diretórios em `totalcross.livePreview.extraClasspath` e acrescenta `/*`, portanto pode usar diretamente o diretório `lib` da distribuição sem embutir JARs no VSIX.
  Date/Author: 2026-07-17 / Codex.

- Decision: manter os endpoints e a configuração existentes na primeira extração: `GET /health`, `GET /frame`, `POST /show`, `POST /clear`, `POST /reload`, `POST /shutdown` e o arquivo `totalcross.preview.json`.
  Rationale: conservar o protocolo evita uma migração simultânea da extensão e permite comparar a nova implementação com a prova HTTP existente. Mudanças de protocolo só devem ocorrer em versão posterior e com compatibilidade explícita.
  Date/Author: 2026-07-17 / Codex.

## Outcomes & Retrospective

O trabalho de produto ainda não começou; nenhuma classe foi removida do SDK, o projeto novo não compila ainda e não existe versão publicada. A política de licença foi atualizada na raiz: Apache-2.0 é o padrão do repositório e `live-preview-server` será uma exceção LGPL-2.1-only, com uso autorizado das fontes introduzidas por `0db656ad9`. A pesquisa confirma que a extração requer alterações coordenadas no repositório atual e em `/Users/flsobral/repos/totalcross-github`, além de uma atualização do consumidor VS Code.

O resultado desejado é uma fronteira nítida: o SDK fornece o contrato único `PreviewRuntime` e a execução base da aplicação; `live-preview-server` possui o protocolo de IDE e todos os detalhes de Live Preview. A implementação deve atualizar esta seção com os commits, coordenadas publicadas, resultados de testes e quaisquer incompatibilidades descobertas.

## Editorial Report

Esta seção será preenchida somente durante e ao término da implementação. Nenhuma das afirmações abaixo deve ser interpretada como entrega concluída.

### Editorial Summary

Planejado: separar a ferramenta HTTP de visualização da biblioteca SDK para que IDEs consumam um artefato próprio, com ciclo de release independente. O resultado real, a versão distribuída e o comportamento observado serão registrados ao fim.

### Original Plan versus Actual Outcome

O plano inicial prevê extração para o tooling, contrato mínimo no SDK, compatibilidade do protocolo e atualização da extensão VS Code. Esta subseção deverá distinguir o que foi entregue, mudado, adiado ou descartado, especialmente a importação autorizada de fontes de `0db656ad9` e qualquer decisão de compatibilidade.

### What Changed

Ainda não há alterações de produto. Ao concluir, listar `live-preview-server/`, os contratos SDK, as classes removidas do SDK, a mudança de `vscode-extension/src/live-preview.ts`, documentação e coordenadas de publicação.

### Decisions and Trade-offs

O plano privilegia um novo artefato LGPL-2.1-only e a importação autorizada das fontes inéditas do commit `0db656ad9`. O custo é manter temporariamente duas implementações durante a prova de compatibilidade e documentar claramente a exceção à licença padrão Apache da raiz.

### Unexpected Problems and Discoveries

Registrar falhas reais de classpath, AWT, isolamento de classes, ordem de shutdown, publicação Maven, distribuição e compatibilidade VS Code. A descoberta já conhecida é que os métodos necessários ao runner ainda não são públicos fora de `totalcross`.

### Validation and Measurable Results

Registrar somente comandos e resultados executados: número de testes, versões e tamanhos dos artefatos, conteúdo dos JARs, resposta HTTP e comportamento da Webview. Ainda não existe medição para esta extração.

### Useful Evidence and Examples

Usar como linha de base o commit SDK `0db656ad9`, o commit da extensão `0f447a5`, o plano `.agent/exec-plan-integrate-vscode-live-preview.md`, o JAR e VSIX temporários da prova anterior. Ao finalizar, substituir referências temporárias por commits e caminhos estáveis.

### Limitations, Remaining Work, and Open Questions

Faltam a versão SDK que expõe os contratos, a primeira release do novo servidor e a escolha de repositório de publicação. Confirmar antes da publicação se o pacote deve suportar apenas Java 17 e quais plataformas desktop AWT são oficialmente compatíveis.

### Possible Article Angles

Para mantenedores de SDKs: extrair uma ferramenta de IDE sem ampliar o runtime distribuído. Para autores de integrações: projetar um contrato de captura de frame pequeno, deixando HTTP, JSON e recarga fora da biblioteca principal. Para responsáveis por licenças: manter um repositório multi-projeto com padrão Apache e uma ferramenta LGPL explícita, sem perder proveniência.

### Suggested Narrative

Começar pelo problema de um JAR SDK que acumulou um servidor de IDE; mostrar o contrato mínimo necessário para capturar imagens e trocar componentes; explicar a escolha de um artefato distribuível próprio e a validação que compara endpoints e a extensão; concluir com o SDK menor e a limitação de compatibilidade de release.

### Claims Requiring Human Review

A versão pública do SDK que fornecerá o contrato, as coordenadas de publicação e as plataformas suportadas exigem revisão humana antes de anúncio ou release. A autorização de usar fontes de `0db656ad9` e a licença LGPL-2.1-only do projeto foram confirmadas pelo mantenedor, mas ainda requerem revisão jurídica e técnica normal antes de publicação.

## Context and Orientation

O repositório atual é `totalcross-tooling`. Ele contém projetos independentes, inclusive `gradle-plugin/`, `maven-plugin/` e `vscode-extension/`; não há build Gradle raiz que agregue todos. Por orientação de `AGENTS.md`, a migração não deve refatorar plugins não relacionados. O novo projeto será, portanto, um diretório Gradle autônomo chamado `live-preview-server/`, com wrapper próprio, README e testes próprios.

`vscode-extension/src/live-preview.ts` cria o processo Java para a classe constante `totalcross.preview.PreviewServer`. Ele forma o classpath com caminhos de `totalcross.preview.json` e da configuração `totalcross.livePreview.extraClasspath`, e chama a classe com `--config`, `--host 127.0.0.1` e `--port`. A extensão só fala com o processo por HTTP local: verifica `/health`, mostra PNG de `/frame` e envia `/show`, `/clear`, `/reload` e `/shutdown`. A nova classe principal deve manter esses endpoints inicialmente, mas seu nome passará a `com.totalcross.livepreview.LivePreviewServer`.

O repositório irmão `/Users/flsobral/repos/totalcross-github` hospeda `TotalCrossSDK`. No commit local `0db656ad9`, o JAR contém a implementação atual: `totalcross.preview.PreviewServer`, `PreviewConfig`, `PreviewConfigLoader`, `DisposableAppClassLoader`, `HeadlessPreviewSurface`, `HeadlessPngSurface`, `PreviewRunner` e superfícies/integrações AWT. `PreviewRunner` controla `LauncherRuntime`; é ele que recarrega `MainWindow`, apresenta `Container` ou `Control`, captura o frame e fecha o runtime. O novo projeto deve reproduzir essas responsabilidades sem depender de classes de implementação do pacote antigo.

Um contrato é uma pequena API pública que define o que uma parte pode pedir à outra sem conhecer seus detalhes internos. Após a migração, `totalcross.preview.PreviewRuntime` permitirá ao servidor acionar a aplicação já iniciada e seu tipo aninhado `PreviewRuntime.FrameConsumer` receberá um frame pronto. O SDK implementará `PreviewRuntime` por meio do seu `LauncherRuntime` existente, mas não publicará servidor HTTP, leitor JSON, carregador de classes descartável, buffer PNG, CLI de preview ou modelo de configuração. `totalcross.desktop` conterá os adaptadores AWT ainda necessários ao launcher normal; eles não são a ferramenta Live Preview.

O novo projeto terá esta forma final:

    live-preview-server/
      AGENTS.md
      README.md
      settings.gradle
      build.gradle
      gradlew, gradlew.bat, gradle/wrapper/
      src/main/java/com/totalcross/livepreview/
        LivePreviewServer.java
        PreviewConfiguration.java
        PreviewConfigurationLoader.java
        PreviewSession.java
        ApplicationClassLoader.java
        PngFrameSurface.java
      src/test/java/com/totalcross/livepreview/
        LivePreviewServerProcessTest.java
        PreviewConfigurationTest.java
        PreviewSessionTest.java
        fixture/PreviewMainWindow.java
      .agent/exec-plan-extract-live-preview-server.md

Nomes internos podem variar apenas se o `Decision Log` registrar a razão; o main class, artifactId, coordenadas e endpoints são interfaces estáveis desta primeira versão.

## Plan of Work

Primeiro, criar `live-preview-server/` como projeto Gradle Java 17 independente. Usar o plugin `application` para definir `mainClass = 'com.totalcross.livepreview.LivePreviewServer'`, `maven-publish` para o JAR Maven e `distribution` para produzir `installDist` e um ZIP. Definir `group = 'com.totalcross'`, `archivesName = 'totalcross-live-preview-server'` e versão inicial `0.1.0-SNAPSHOT`. Criar `live-preview-server/LICENSE` com o texto canônico LGPL 2.1, `live-preview-server/NOTICE` com a proveniência `0db656ad9` e cabeçalhos `SPDX-License-Identifier: LGPL-2.1-only` em suas fontes. A dependência de runtime deve ser `com.totalcross:totalcross-sdk:${totalcrossSdkVersion}`; `totalcrossSdkVersion` é uma propriedade Gradle obrigatória. Falhar com mensagem clara se ela não estiver definida, em vez de selecionar silenciosamente o SDK público 7.2.2 que não possui o contrato. Para desenvolvimento entre árvores, documentar e testar `publishToMavenLocal` no SDK e `-PtotalcrossSdkVersion=<versão-local>` no novo projeto. Não adicionar dependência de runtime ao VSIX.

Criar `live-preview-server/AGENTS.md` antes das fontes. Ele deve declarar Java 17, LGPL-2.1-only para arquivos de primeira parte e fontes importadas, Gradle Wrapper obrigatório, o artifactId, a dependência SDK, o limite de proveniência `0db656ad9` e a obrigação de preservar cabeçalhos/autores dos arquivos importados. Copiar somente arquivos de wrapper gerados pelo Gradle como material de terceiro, sem adicionar cabeçalhos a eles. O README deve explicar instalação por `installDist`, configuração `extraClasspath` apontando para `<install>/lib`, requisitos JDK 17, o protocolo local e a precedência de seu `LICENSE` sobre o padrão Apache da raiz.

Antes de tornar o projeto ativo em `tools/license-policy.json`, ampliar `tools/check-license-headers.py`, `tools/build-license-provenance.py` e seus testes para que a licença seja escolhida pela entrada do projeto na política, em vez de assumir Apache-2.0 para toda proveniência. A regra padrão continua Apache-2.0; `live-preview-server` passa a exigir LGPL-2.1-only e os demais projetos não podem mudar de comportamento. Adicionar ao `NOTICE`, README e CONTRIBUTING da raiz a explicação de que `LICENSE` raiz é padrão e que um `LICENSE` de projeto prevalece. Só então marcar `live-preview-server` como ativo e gerar/adicionar seus registros de proveniência.

No SDK, criar exatamente o contrato público a seguir, com cabeçalho LGPL-2.1-only e documentação que o trate como API de integração, não como servidor:

    package totalcross.preview;

    public interface PreviewRuntime extends AutoCloseable {
      @FunctionalInterface
      interface FrameConsumer {
        void present(java.awt.image.BufferedImage image);
      }
      void pumpEvents();
      void replaceMainWindow(totalcross.ui.MainWindow mainWindow, String commandLine);
      void showContainer(totalcross.ui.Container container);
      void showControl(totalcross.ui.Control control);
      @Override void close();
    }

Adaptar `TotalCrossSDK/src/main/java/totalcross/LauncherRuntime.java` para implementar `PreviewRuntime`. O parâmetro de captura de `startPreview(...)` passa a ser `PreviewRuntime.FrameConsumer`. Seus métodos internos de preview devem se tornar as implementações públicas, com nomes e semântica idênticos aos métodos da interface; `close()` deve chamar o atual `stop()`. `LauncherRuntime.startPreview(...)` pode conservar o retorno `LauncherRuntime` por compatibilidade de código fonte, pois a classe implementará o contrato e pode ser armazenada em uma variável `PreviewRuntime` pelo novo servidor. Não acrescentar referência ao projeto novo no SDK e não criar dependência circular.

Mover os adaptadores AWT que continuam necessários ao launcher de `totalcross.preview` para `totalcross.desktop`: `AwtCanvasSurface`, `AppletPreviewSurface`, `AwtWindowBackend`, `RenderSurface`, `WindowBackend` e `WindowConfig`. Atualizar somente os imports em `Launcher` e `LauncherRuntime` e seus testes; as superfícies AWT passam a implementar `PreviewRuntime.FrameConsumer`. `PreviewRuntime` permanece como o único contrato de Live Preview em `totalcross.preview` usado pelo SDK. Confirmar se qualquer outro consumidor público usa os nomes AWT antes de removê-los; caso exista, manter um adaptador obsoleto apenas durante uma versão major e registrar essa exceção no plano. Não tentar mover implementações entre repositórios por `git mv`.

Remover do SDK, em uma mudança posterior e testada, `totalcross.PreviewRunner` e todas as classes de implementação Live Preview: `PreviewServer`, `PreviewConfig`, `PreviewConfigLoader`, `DisposableAppClassLoader`, `HeadlessPreviewSurface`, `HeadlessPngSurface` e `package.html` que as descreve. Migrar a intenção dos testes para `live-preview-server`; deixar no SDK somente testes que provem que `LauncherRuntime` entrega frames a `PreviewRuntime.FrameConsumer` e satisfaz `PreviewRuntime`. Atualizar `TotalCrossSDK/build.gradle` para não incluir nem excluir classes inexistentes de preview e para que o JAR final não tenha `totalcross/PreviewRunner.class`, `totalcross/preview/PreviewServer.class` ou outras implementações do servidor.

Importar e adaptar para o novo projeto as fontes Live Preview introduzidas por `0db656ad9`, preservando cabeçalhos LGPL e registrando em cada commit a origem SDK. Renomear os pacotes de implementação para `com.totalcross.livepreview`; não levar para o novo projeto os adaptadores AWT que permanecerão no SDK. As responsabilidades são:

- `PreviewConfiguration` e `PreviewConfigurationLoader` leem e escrevem o esquema já usado pela extensão: `mainWindow`, `launcherArgs`, `buildCommand`, `classOutputPaths`, `resourcePaths`, `dependencyPaths`, `previewMode`, `reloadMode`, `width`, `height`, `scale`, `platform` e `headlessOutput`. A leitura deve tolerar campos ausentes e usar os mesmos defaults: MainWindow vazio, saída `build/classes/java/main`, recursos `src/main/resources`, dependências `build/libs` e `lib`, 500x600, escala 1 e plataforma `android`.
- `ApplicationClassLoader` é um `URLClassLoader` fechável. Ele resolve caminhos relativos a partir do diretório do JSON, inclui diretórios de classes, recursos, JARs e `*` de diretórios de dependências; deve bloquear nomes `com.totalcross.livepreview.*`, `totalcross.preview.*` e o contrato/SDK para que classes do aplicativo não substituam o servidor ou o runtime.
- `PngFrameSurface` implementa `PreviewRuntime.FrameConsumer`, copia a imagem recebida para um buffer protegido por lock, conta frames, produz um PNG e consegue limpar para um quadro preto do tamanho configurado. Não expor o `BufferedImage` mutável do launcher ao handler HTTP.
- `PreviewSession` possui `PreviewRuntime`, a superfície, o classloader atual e o classloader de uma classe mostrada. Ela inicia `LauncherRuntime.startPreview`, carrega e instancia o MainWindow configurado, chama `pumpEvents`, recarrega criando um novo classloader e trocando MainWindow, e aceita `MainWindow`, `Container` ou `Control` para `/show`. Uma classe sem construtor padrão ou tipo incompatível retorna erro textual sem derrubar a sessão. `clear` descarta a classe mostrada e publica quadro preto. `close` fecha os classloaders e o `PreviewRuntime` uma única vez.
- `LivePreviewServer` aceita `--config <arquivo>` ou `--class <classe>`, mais `--host`, `--port` e argumentos após `--`. O host padrão é `127.0.0.1`; rejeitar hosts não loopback por padrão e documentar qualquer exceção. Depois de iniciar, imprimir exatamente `TOTALCROSS_PREVIEW_URL=http://127.0.0.1:<porta>` e aguardar `POST /shutdown`, sem encerrar logo após imprimir. Implementar os seis endpoints atuais, métodos HTTP corretos, respostas JSON UTF-8, `Cache-Control: no-store` em `/frame`, limite de corpo de requisição de 64 KiB e JSON de `/show` estritamente limitado a `{ "className": "..." }`. Não usar framework HTTP ou JSON adicional na primeira versão; Java 17 fornece `HttpServer`, e o parser deve ser pequeno, testado e não aceitar conteúdo desconhecido de forma silenciosa.

Preservar os códigos observáveis: `/health` responde 200 e `{ "ok": true, "mainWindow": ..., "frameNumber": ... }`; `/frame` responde 200 `image/png` quando há frame e 503 antes disso; `/show` responde 200 em sucesso e 422 para classe inválida; `/reload` responde 200 em sucesso e 500 em falha; `/clear` e `/shutdown` respondem 200; método errado retorna 405. A resposta `/shutdown` deve ser escrita antes de fechar o servidor e, depois, fechar a sessão e sinalizar uma trava para a thread principal terminar. Não chamar `System.exit` em código de biblioteca; o main pode retornar normalmente depois de todas as threads AWT pertencentes à sessão serem encerradas. Se threads externas impedirem o processo de terminar, registrar a evidência e escolher o menor ajuste de ciclo de vida antes de aceitar um `System.exit` controlado.

Usar testes unitários no novo projeto para defaults e JSON, argumentos CLI, limites de host/porta, classpath e bloqueio de classes, limpeza da superfície e recarga sem vazamento de carregador. `LivePreviewServerProcessTest` deve compilar uma fixture `fixture.PreviewMainWindow`, criar JSON temporário, iniciar um processo Java real a partir de `installDist` ou do classpath de teste, extrair a URL de `TOTALCROSS_PREVIEW_URL`, verificar `/health`, baixar `/frame` e validar a assinatura PNG, chamar `/show`, `/clear`, `/reload`, `/shutdown` e esperar saída zero. Esse teste é a prova principal de que o novo artefato não depende de `totalcross.preview.PreviewServer` no SDK.

Atualizar `vscode-extension/src/live-preview.ts` para usar `com.totalcross.livepreview.LivePreviewServer`. Não alterar os nomes de comandos nem endpoints. Atualizar `README.md`, testes de argumentos e o plano de integração para explicar que `extraClasspath` recebe o diretório `lib` da distribuição do servidor, que já contém o novo JAR e a dependência SDK. Remover afirmações de que o SDK inclui PreviewServer. Acrescentar uma validação de empacotamento VSIX que confirme o novo nome de classe no JavaScript compilado, mas que não inclui JARs ou a distribuição no VSIX.

## Concrete Steps

1. Na raiz de `totalcross-tooling`, confirmar que a árvore está limpa e registrar a linha de base:

       git status --short --branch
       sed -n '1,320p' .agent/exec-plan-extract-live-preview-server.md
       rg -n "PreviewServer|PreviewRunner|totalcross\.preview" vscode-extension .agent/exec-plan-integrate-vscode-live-preview.md

   Esperado: não há mudanças não relacionadas e a extensão ainda aponta para a classe antiga. Atualizar `Progress` antes de criar arquivos.

2. Criar o esqueleto Gradle independente e sua orientação local:

       mkdir -p live-preview-server/src/main/java/com/totalcross/livepreview
       mkdir -p live-preview-server/src/test/java/com/totalcross/livepreview/fixture
       cd live-preview-server
       gradle wrapper --gradle-version 9.6.1 --distribution-type all
       ./gradlew tasks --console=plain

   Criar os arquivos somente por patch revisado: `settings.gradle`, `build.gradle`, `AGENTS.md`, `README.md` e o ExecPlan copiado por referência para `.agent/` se a orientação local exigir. O wrapper é a única saída gerada aceita. Esperado: a tarefa `installDist` aparece e `./gradlew test` falha inicialmente apenas porque não há fontes/testes, nunca por resolução oculta de SDK.

3. Preparar o SDK de desenvolvimento no repositório irmão, sem tocar em mudanças não relacionadas:

       git -C /Users/flsobral/repos/totalcross-github status --short --branch
       cd /Users/flsobral/repos/totalcross-github/TotalCrossSDK
       ./gradlew-agent publishToMavenLocal

   Primeiro aplicar as mudanças de contrato e de pacote descritas em `Plan of Work`, com testes focados antes da publicação. Usar um número de versão de desenvolvimento que o novo projeto receba explicitamente em `-PtotalcrossSdkVersion=...`; registrar a versão real e o commit SDK no plano. Não adicionar, apagar ou incluir em commit os arquivos locais não relacionados já presentes nessa árvore.

4. No SDK, executar antes e depois da remoção a verificação de fronteira:

       ./gradlew-agent test --tests 'totalcross.LauncherPreviewSurfaceTest' --tests 'totalcross.LauncherRuntimeTest'
       ./gradlew-agent jar
       jar tf build/libs/totalcross-sdk-*.jar | rg 'totalcross/(PreviewRunner|preview/PreviewServer|preview/PreviewConfig|preview/Headless|preview/Disposable)'
       jar tf build/libs/totalcross-sdk-*.jar | rg 'totalcross/preview/PreviewRuntime(\$FrameConsumer)?\.class'

   Esperado no estado final: a primeira busca não imprime nada e termina sem considerar isso falha; a segunda lista somente `PreviewRuntime.class` e `PreviewRuntime$FrameConsumer.class`. Também confirmar que as classes AWT estão em `totalcross/desktop/` e o launcher continua compilando.

5. Implementar a configuração, superfície, classloader, sessão e servidor no novo projeto. Depois de cada grupo, rodar:

       cd /Users/flsobral/repos/totalcross-tooling/live-preview-server
       ./gradlew test -PtotalcrossSdkVersion=<versão-local> --console=plain
       ./gradlew installDist -PtotalcrossSdkVersion=<versão-local> --console=plain
       find build/install/totalcross-live-preview-server/lib -maxdepth 1 -type f | sort

   Esperado: testes unitários passam e o diretório `lib` contém `totalcross-live-preview-server-<versão>.jar` e `totalcross-sdk-<versão>.jar` ou a dependência equivalente resolvida pelo Gradle.

6. Executar a prova de processo em um diretório temporário explícito. Criar o JSON e a fixture pelos testes, não no repositório. Um formato de execução esperado é:

       cd /Users/flsobral/repos/totalcross-tooling/live-preview-server
       ./gradlew test --tests 'com.totalcross.livepreview.LivePreviewServerProcessTest' -PtotalcrossSdkVersion=<versão-local> --console=plain
       ./gradlew installDist -PtotalcrossSdkVersion=<versão-local> --console=plain
       build/install/totalcross-live-preview-server/bin/totalcross-live-preview-server --config /tmp/totalcross-preview-e2e/totalcross.preview.json --host 127.0.0.1 --port 0

   Esperado: a primeira linha útil é `TOTALCROSS_PREVIEW_URL=http://127.0.0.1:<porta>`. `curl -fsS http://127.0.0.1:<porta>/health` retorna JSON com `"ok":true`; `curl -fsS http://127.0.0.1:<porta>/frame -o /tmp/frame.png` cria PNG; `curl -X POST -fsS http://127.0.0.1:<porta>/shutdown` retorna `{"ok":true}` e o processo termina.

7. Atualizar o consumidor VS Code e seus testes:

       cd /Users/flsobral/repos/totalcross-tooling/vscode-extension
       npm ci
       npm run compile
       npm test
       rg -n "totalcross\.preview\.PreviewServer|com\.totalcross\.livepreview\.LivePreviewServer" src README.md out

   Esperado: a referência antiga não aparece em fontes/README compiladas e a nova aparece em `src/live-preview.ts` e `out/live-preview.js`. Os 23 testes existentes continuam passando ou o novo total é registrado no plano.

8. Validar cada projeto e a governança da raiz antes de commits:

       cd /Users/flsobral/repos/totalcross-tooling/live-preview-server
       ./gradlew check -PtotalcrossSdkVersion=<versão-local> --console=plain
       ./gradlew publishToMavenLocal -PtotalcrossSdkVersion=<versão-local> --console=plain
       cd /Users/flsobral/repos/totalcross-tooling
       python3 tools/check-license-headers.py
       python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
       git diff --check

   Executar também qualquer validador de cabeçalho indicado pelo novo `live-preview-server/AGENTS.md`. Esperado: todos terminam com código zero, a política confirma Apache-2.0 como padrão e LGPL-2.1-only para `live-preview-server`, e o Maven Local contém `com/totalcross/totalcross-live-preview-server/0.1.0-SNAPSHOT`.

9. Repetir a aceitação manual em VS Code apontando `totalcross.livePreview.extraClasspath` para `live-preview-server/build/install/totalcross-live-preview-server/lib`, nunca para um JAR SDK contendo servidor. Executar Start, observar `Live preview updated` e imagem, executar Reload, Open Preview Config e Stop. Empacotar a extensão:

       cd /Users/flsobral/repos/totalcross-tooling/vscode-extension
       npx --yes @vscode/vsce@latest package --out /tmp/vscode-totalcross-0.1.0.vsix
       unzip -p /tmp/vscode-totalcross-0.1.0.vsix extension/out/live-preview.js | rg 'com\.totalcross\.livepreview\.LivePreviewServer'
       unzip -l /tmp/vscode-totalcross-0.1.0.vsix | rg 'totalcross-live-preview-server|totalcross-sdk.*\.jar'

   Esperado: a primeira busca encontra a classe nova; a segunda não encontra JARs. Guardar somente a saída concisa e uma captura da Webview, sem versionar artefatos temporários.

10. Criar commits lógicos separados nos dois repositórios: contrato/remoção SDK, novo servidor com testes/distribuição, consumidor VS Code/documentação e atualização dos ExecPlans. Cada commit deve ter título convencional em inglês, corpo que explique a fronteira de artefatos e não incluir alterações locais alheias. Atualizar todas as seções vivas deste plano antes do último commit; somente uma autorização posterior pode criar tags, publicar Maven ou publicar o VSIX.

## Validation and Acceptance

A extração é aceita quando uma pessoa consegue obter o SDK que contém somente `PreviewRuntime` e seu `FrameConsumer` aninhado como contrato de Live Preview no namespace `totalcross.preview`, instalar o novo servidor e iniciar a classe `com.totalcross.livepreview.LivePreviewServer` sem qualquer classe `PreviewServer` no JAR SDK. A inspeção do JAR deve demonstrar ambas as condições, não apenas compilação.

Com uma fixture `MainWindow` compilada, o processo novo precisa imprimir uma URL loopback, responder `/health` com HTTP 200 e JSON válido, servir uma imagem PNG não vazia em `/frame`, aceitar `/show`, `/clear` e `/reload`, responder a `/shutdown` antes de terminar e sair com código zero. O teste de processo deve cobrir isso automaticamente com o executável em `installDist`, para verificar a distribuição e não apenas classpath de IDE.

Em VS Code 1.85+, apontar `totalcross.livePreview.extraClasspath` para o diretório `lib` da distribuição do novo projeto deve produzir imagem na Webview. Start, Reload, Open Preview Config e Stop devem continuar observáveis. A extensão não pode referenciar `totalcross.preview.PreviewServer`, e o VSIX não pode carregar JARs do servidor ou SDK.

`live-preview-server` deve passar `./gradlew check`; o SDK deve passar seus testes focados e gerar JAR limpo; `vscode-extension` deve passar `npm run compile`, `npm test`, governança e audit; e a raiz deve passar `python3 tools/check-repository-governance.py`, `python3 -m unittest tests.test_repository_governance` e `git diff --check`. O plano só estará completo depois de registrar esses resultados em `Outcomes & Retrospective` e no `Editorial Report`.

## Idempotence and Recovery

`./gradlew test`, `check`, `installDist`, `publishToMavenLocal`, testes SDK, testes VS Code e inspeções `jar tf` são repetíveis. Usar diretórios `/tmp/totalcross-preview-*` únicos para provas manuais e apagar somente o diretório temporário criado explicitamente pela execução, nunca caches Gradle ou diretórios amplos.

Durante a migração, manter a implementação SDK até a prova do novo projeto passar. Se o novo servidor falhar, restaurar a extensão temporariamente para a classe antiga por um commit de reversão ou patch mínimo, sem `git reset --hard`, e registrar a falha. Só remover classes SDK depois de a prova de processo e a extensão funcionarem contra o novo artefato. Se uma publicação Maven Local ficar inconsistente, publicar uma nova versão de desenvolvimento explícita e atualizar `totalcrossSdkVersion`/a versão do servidor na prova; não apagar automaticamente caches ou repositórios Maven locais.

Se uma fonte necessária não estiver no conjunto introduzido por `0db656ad9`, parar antes de copiá-la, registrar o bloqueio e pedir autorização/proveniência específica. Não alterar cabeçalhos LGPL, `NOTICE` ou histórico de nenhum repositório sem essa autorização.

## Artifacts and Notes

No início, registrar os hashes dos commits de base: SDK `0db656ad9`, extensão `0f447a5` e plano de integração `34873f2`. Ao concluir, acrescentar os hashes reais de contrato SDK, servidor, consumidor e ExecPlans, a versão SDK usada, a versão do servidor e o tamanho do ZIP `installDist`/distribution.

Conservar como evidência concisa: lista `jar tf` que prova a remoção, trecho `TOTALCROSS_PREVIEW_URL`, resposta `/health`, tipo/tamanho do PNG, resposta `/shutdown`, resultado do teste de processo e captura do painel VS Code. Não adicionar JARs, ZIPs, PNGs, logs extensos, SDKs extraídos ou cache Gradle ao Git.

## Interfaces and Dependencies

No estado final, o SDK expõe um único contrato público específico de Live Preview:

    totalcross.preview.PreviewRuntime
    totalcross.preview.PreviewRuntime.FrameConsumer

`LauncherRuntime` implementa `PreviewRuntime`; sua criação continua por `LauncherRuntime.startPreview(String, PreviewRuntime.FrameConsumer, ClassLoader, String...)`. O novo projeto usa apenas essa API e os tipos públicos `MainWindow`, `Container` e `Control`; ele não importa `Launcher`, `LauncherConfig`, `LauncherParsedConfig`, `totalcross.desktop` ou qualquer classe `totalcross.preview` que não seja esse contrato.

O contrato de processo é:

    com.totalcross.livepreview.LivePreviewServer
      --config <workspace>/totalcross.preview.json
      --host 127.0.0.1
      --port <0..65535>

O servidor é sempre local por padrão. O protocolo é HTTP sem autenticação porque só aceita loopback; se uma mudança futura permitir host remoto, ela deverá incluir autenticação, política de origem e revisão de segurança em novo ExecPlan.

O novo JAR declara `com.totalcross:totalcross-sdk` como dependência em vez de incluir suas classes. A distribuição Gradle inclui a dependência para execução local; o VSIX permanece TypeScript/Node e recebe o caminho dessa distribuição por configuração. Não há dependência de `vscode-extension` para `live-preview-server` durante a compilação do VSIX, nem dependência do servidor para o código da extensão.

Revision note (2026-07-17): criado após a integração inicial de Live Preview, cuja implementação ainda consome `totalcross.preview.PreviewServer` do SDK. O plano substitui essa fronteira por contrato SDK e artefato de tooling próprio.

Revision note (2026-07-17): revisado após confirmação do mantenedor de que o novo projeto deve ser LGPL-2.1-only e que fontes introduzidas no commit SDK `0db656ad9` ainda não publicadas podem ser utilizadas. A raiz mantém Apache-2.0 como padrão de repositório, com licenças de projeto que prevalecem quando declaradas.
