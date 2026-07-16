<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Instruções para agentes

Este repositório contém os plugins Gradle `com.totalcross.application` e
`com.totalcross.library`. Eles criam pacotes TotalCross (TCZ e instaladores
solicitados) chamando `tc.Deploy`. Os plugins são
autocontido: resolve o SDK TotalCross a partir da dependência de runtime e
baixa as JDKs Zulu necessárias para o cache do Gradle. Preserve esse
comportamento ao alterar o projeto.

## Regras obrigatórias de licença e copyright

Todo arquivo de primeira parte novo ou alterado deve ter o cabeçalho abaixo,
adaptado ao formato de comentário do arquivo, antes de qualquer conteúdo
executável ou de documentação:

    SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

Use `//` em Java e Gradle Groovy, `#` em scripts shell, propriedades, YAML e
`.gitignore`, e um comentário HTML em Markdown. Preserve shebangs e declarações
obrigatórias antes do cabeçalho. Não altere os arquivos gerados pelo Gradle
Wrapper em `gradle/wrapper/` ou o script `gradlew`: eles são material de
terceiro. `LICENSE` e `NOTICE` também não recebem cabeçalhos de comentário.

Antes de concluir uma mudança, execute o validador e seus testes:

    python3 tools/check-license-headers.py
    python3 tests/license_headers/test_check_license_headers.py

O primeiro comando informa a contagem de arquivos verificados; ambos devem
terminar com código zero.

## Como o projeto está organizado

- `build.gradle` define o build com Gradle 9.6.1, Java 17, publicação Maven e os
  plugins de ID `com.totalcross.application` e `com.totalcross.library`.
- `src/main/java/com/totalcross/gradle/` contém a implementação. A classe de
  entrada é `TotalCrossPlugin`; `TotalCrossPackageTask` implementa a task
  `totalcrossPackage`; `SdkResolver`, `JdkResolver` e `ArchiveDownloader`
  resolvem e armazenam as distribuições baixadas.
- `src/test/java/com/totalcross/gradle/` contém testes unitários e funcionais
  com Gradle TestKit.
- `examples/basic-app/` é um projeto consumidor mínimo. Seu
  `settings.gradle` usa `includeBuild('../..')`, portanto o exemplo testa o
  plugin local sem exigir publicação prévia.
- `.agent/totalcross-gradle-plugin-execplan.md` é o plano vivo da implementação
  já realizada. Atualize-o se uma alteração continuar ou modificar esse trabalho.
- `.agent/PLANS.md` define as regras obrigatórias para todo novo ExecPlan.

## Interface pública e publicação

Os IDs públicos são `com.totalcross.application` e
`com.totalcross.library`; não volte a usar o ID legado `totalcross`. O plugin
de aplicação configura `totalcrossLib = false` e o de biblioteca configura
`totalcrossLib = true`, ambos com possibilidade de substituição explícita no
bloco DSL. A task pública é:

    ./gradlew totalcrossPackage

O plugin aplica `java`, depende de `jar` e conecta `totalcrossPackage` a
`assemble`. A publicação local deve continuar produzindo:

    com.totalcross:totalcross-gradle-plugin:0.1.0-SNAPSHOT

e os marcadores de plugin novos. Confirme isso com:

    ./gradlew publishToMavenLocal --console=plain

O bloco DSL suportado é `totalcross { ... }`, incluindo `applicationName`,
`platforms`, `activationKey`, `totalcrossHome`, `jdkPath`, `totalcrossLib`,
`externalResources`, `certificates`, `deployArguments`, `outputDirectory`,
`deploySdkJar`, `logLevel` e
`sdkVersion`. `sdkVersion` é necessário quando `totalcrossHome` aponta a uma
distribuição completa cuja compatibilidade difere da versão da dependência Maven.
`deploySdkJar` é um JAR opcional que tem prioridade para executar `tc.Deploy`,
permitindo testar somente um JAR reconstruído enquanto `totalcrossHome` fornece
os arquivos de suporte.

## Compatibilidade de SDK, bytecode e JDK

A dependência de runtime obrigatória é
`com.totalcross:totalcross-sdk:<versão>`. A task deve falhar claramente antes
de downloads se ela não estiver presente ou se mais de uma versão for resolvida.
O resolvedor normal consulta primeiro a release `v<versão>` do repositório
`TotalCross/totalcross` no GitHub e baixa `TotalCross-<versão>.zip`; a URL S3
histórica é o fallback para release ausente, falha de consulta ou falha de
download. Não substitua esse fluxo pelo artefato de teste do GitHub Actions.

As regras de compatibilidade são parte da interface do plugin:

- SDK `7.3.0` ou posterior aceita classfiles até Java 17 e falha para target
  superior a 17. O deploy usa a JDK 17.
- SDK anterior a `7.3.0` falha para target superior a Java 8. Para target Java
  8, Retrolambda `2.5.7` reduz o bytecode a Java 7 antes do deploy. Retrolambda
  e `tc.Deploy` usam a JDK 11 neste caminho.
- As demais combinações aceitas seguem sem Retrolambda.

Mantenha as duas VMs independentes no cache do Gradle:

    <GRADLE_USER_HOME>/caches/totalcross/jdk/zulu_jdk_11
    <GRADLE_USER_HOME>/caches/totalcross/jdk/zulu_jdk_17

Os SDKs baixados ficam em
`<GRADLE_USER_HOME>/caches/totalcross/sdk/<versão>`. A resolução deve continuar
atômica e validar o cache antes de reutilizá-lo. Para recuperar um download
corrompido, apague somente o diretório da versão ou JDK afetado.

Não injete ASM no classpath do deployer. O teste de compatibilidade Java 17 usa
um SDK completo externo, tratado como `7.3.0` por meio de `sdkVersion`, sem
alterar a URL de resolução normal.

## Build, testes e verificações

Execute os comandos a partir da raiz do repositório e use sempre o wrapper
versionado. Não dependa de uma instalação global do Gradle.

    ./gradlew test --console=plain
    ./gradlew check --console=plain
    ./gradlew publishToMavenLocal --console=plain
    ./gradlew sdkSourceNetworkTest --console=plain

O primeiro comando é a verificação rápida obrigatória após qualquer mudança de
código. `check` executa a validação completa. O exemplo legado, que resolve o
SDK público 7.2.2, é exercitado com:

    ./gradlew -p examples/basic-app clean totalcrossPackage --console=plain

Ele deve gerar `examples/basic-app/build/totalcross/MainWindow.tcz`. O exemplo
usa toolchain Java 17, mas o caminho público legado produz classfiles Java 8 e
usa Retrolambda, pois o SDK 7.2.2 não aceita bytecode mais novo.

Para validar o fluxo Java 17 de ponta a ponta, informe um SDK completo externo:

    ./gradlew -p examples/basic-app clean totalcrossPackage \
      -PtestTotalcrossHome=/caminho/para/TotalCross --console=plain

Esse modo compila o exemplo para Java 17, define `sdkVersion = '7.3.0'` e não
altera o comportamento normal do plugin. O artefato de Actions anteriormente
testado foi extraído em `/private/tmp/totalcross-java17-sdk/TotalCross`; mantenha
essa cópia local enquanto ela for necessária para os testes. As cópias em cache
dos SDKs 7.2.2 e 7.3.0 também devem ser preservadas para evitar novos downloads
nos testes de regressão.

Não remova os caches globais nem arquivos de terceiros como parte de testes
rotineiros. `clean` do exemplo remove apenas `examples/basic-app/build` e é
seguro.

## Planos de execução (ExecPlans)

Para tarefas novas, significativas ou com múltiplas etapas, crie um ExecPlan em
`.agent/` antes de implementar. Leia `.agent/PLANS.md` integralmente antes de
criar ou alterar qualquer plano e siga-o literalmente. Um ExecPlan deve ser
autossuficiente para uma pessoa sem contexto do repositório e explicar o
resultado observável, os arquivos, as decisões e os comandos exatos.

Todo ExecPlan deve conter e manter atualizadas as seções `Progress`,
`Surprises & Discoveries`, `Decision Log` e `Outcomes & Retrospective`. Registre
o andamento em cada ponto de parada, documente descobertas com evidências,
explique decisões, e inclua validação, recuperação e resultados finais. Se o
arquivo contiver somente o plano, não use uma cerca Markdown externa; ao enviar
um plano em uma resposta, use uma única cerca `md` sem cercas aninhadas.

Ao implementar um ExecPlan, prossiga pelos marcos sem pedir "próximos passos",
atualize o documento vivo conforme o trabalho evoluir e registre no fim a razão
de toda revisão do plano. Use o esqueleto e os requisitos de
`.agent/PLANS.md`; não trate este resumo como substituto daquele arquivo.

## Práticas de alteração

Preserve mudanças existentes que não pertençam à tarefa. Use `rg` para localizar
arquivos ou texto, `apply_patch` para edições e `git diff --check` antes de
concluir. Não use operações destrutivas de Git. Teste a alteração na proporção
do risco e atualize `README.md`, os testes e o ExecPlan aplicável quando a
interface pública, a compatibilidade ou a operação do plugin mudar.
