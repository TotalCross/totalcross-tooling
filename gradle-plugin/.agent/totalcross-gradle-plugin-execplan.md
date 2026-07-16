<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Implementar o plugin Gradle TotalCross

Este ExecPlan é um documento vivo e segue `.agent/PLANS.md`.

## Purpose / Big Picture

Entregar um plugin Gradle `totalcross` que transforme um JAR Java 17 em um
pacote TotalCross usando `tc.Deploy`, com download automático do SDK correspondente
e de uma Zulu JDK 17. O exemplo em `examples/basic-app` prova o fluxo com uma
`MainWindow` vazia.

## Progress

- [x] (2026-07-10) Criado o esqueleto Gradle, DSL, resolutores de SDK/JDK e task de deploy.
- [x] (2026-07-10) Validado o SDK real 7.2.2; a distribuição moderna contém `dist`, mas não `etc`, e a CLI usa `/o` para o diretório de saída.
- [x] (2026-07-10) Gerado e validado o wrapper Gradle 9.6.1 com JDK 17.
- [x] (2026-07-10) Adicionados e executados testes unitários e funcionais.
- [x] (2026-07-10) Executado o exemplo com SDK 7.2.2 e Zulu JDK 17; gerado `build/totalcross/MainWindow.tcz`.
- [x] (2026-07-11) Priorizada a release GitHub do SDK, com fallback S3 e testes unitários e de rede opt-in para as versões 7.2.0 e 5.8.4.
- [x] (2026-07-13) Adaptados os plugins de aplicação e biblioteca, documentação e testes aos IDs e ao artefato definidos no build.

## Surprises & Discoveries

- Observation: não há instalação global do Gradle no ambiente; o wrapper será usado para validar o build.
  Evidence: `gradle --version` retorna `command not found`.

- Observation: Gradle 9.6.1 valida explicitamente a estratégia de cache e a normalização de entradas de tasks de plugin.
  Evidence: `./gradlew check` falhou até `TotalCrossPackageTask` declarar `@DisableCachingByDefault` e normalização absoluta para os diretórios configurados.

## Decision Log

- Decision: usar APIs nativas do Gradle e classes Java padrão, sem Maven APIs ou AWS SDK; usar Retrolambda 2.5.7 somente para SDK anterior a 7.3.0 com target 8.
  Rationale: preserva o fluxo de compatibilidade legado sem afetar SDKs que aceitam bytecode moderno.
  Date/Author: 2026-07-10 / Codex.

- Decision: aceitar layouts modernos de SDK que tenham `dist` e criar a ponte mínima `etc/fonts/Material Icons.tcz`; passar `/o` a `tc.Deploy`.
  Rationale: o arquivo público 7.2.2 não contém `etc`, embora o deployer derive `dist` desse caminho, e sua CLI atual não reconhece `/d`.
  Date/Author: 2026-07-10 / Codex.

- Decision: o exemplo executa e compila com a toolchain JDK 17, mas emite classfiles release 8 diretamente pelo `javac`.
  Rationale: após atualizar o leitor ASM, o conversor publicado 7.2.2 ainda rejeitou explicitamente classfiles acima de Java 8. Retrolambda não é usado; a limitação foi documentada para não prometer uma capacidade ausente no SDK público.
  Date/Author: 2026-07-10 / Codex.

- Decision: procurar primeiro o SDK na release `v<versão>` de `TotalCross/totalcross` no GitHub e usar a URL S3 histórica como fallback para release ausente, indisponibilidade do GitHub ou falha do asset.
  Rationale: releases novas podem ser disponibilizadas primeiro no GitHub, sem interromper versões antigas que existem somente no armazenamento S3.
  Date/Author: 2026-07-11 / Codex e usuário.

- Decision: declarar `TotalCrossPackageTask` não-cacheável e normalizar seus diretórios configurados por caminho absoluto.
  Rationale: `tc.Deploy` produz saídas específicas de plataforma fora do modelo de cache do Gradle, e a validação do plugin exige uma estratégia explícita para esses diretórios.
  Date/Author: 2026-07-11 / Codex.

- Decision: expor plugins distintos para aplicação e biblioteca, com `totalcrossLib` falso e verdadeiro por padrão, respectivamente.
  Rationale: os IDs novos expressam modos de empacotamento diferentes; apontar ambos para o mesmo default faria o plugin de biblioteca gerar uma aplicação.
  Date/Author: 2026-07-13 / Codex.

## Outcomes & Retrospective

O plugin `totalcross`, a task `totalcrossPackage`, o cache seguro de SDK/JDK
17, os testes e o exemplo foram entregues. `./gradlew test` passou e
`./gradlew -p examples/basic-app clean totalcrossPackage --console=plain` gerou
`examples/basic-app/build/totalcross/MainWindow.tcz` com a Zulu JDK 17 baixada
automaticamente. A limitação de classfile do SDK público 7.2.2 está documentada:
o exemplo usa toolchain 17 e emissão release 8 direta no caminho legado, que é transformado por Retrolambda antes do deploy.

Em 2026-07-10, o artefato de Actions `TotalCross-7.2.2` da execução 29122031626
foi testado isoladamente via `-PtestTotalcrossHome`. Sem ASM injetado, o exemplo
emitiu um classfile de major version 61 e `tc.Deploy` gerou `MainWindow.tcz`.
O URL oficial do resolvedor não foi alterado.

Em 2026-07-10, o ID público passou a ser `totalcross` e o artefato passou a ser
`com.totalcross.gradle.application.plugin`. A regra de runtime foi atualizada para
manter JDK 11 para SDKs anteriores a 7.3.0 e JDK 17 para 7.3.0 ou posterior. Para
o caso legado com target 8, Retrolambda 2.5.7 roda em JDK 11 e produz classfile
major 51 antes do deploy.

Em 2026-07-11, a resolução do SDK passou a consultar a API de releases GitHub
antes do S3. Os testes unitários cobrem seleção, fallback e cache; a task
`sdkSourceNetworkTest` é opt-in e faz somente requisições HTTP de um byte para
confirmar a release `7.2.0` e o fallback S3 da versão `5.8.4`.

Revisão em 2026-07-11: registrada a prioridade de releases GitHub e o teste de
rede parcial solicitado, para que o plano reflita o comportamento de resolução
atual e sua validação sem baixar arquivos completos.

Revisão em 2026-07-11: registrada a declaração explícita de cache e a
normalização exigidas por Gradle 9.6.1, pois elas permitiram a conclusão de
`./gradlew check` após a mudança do resolvedor.

Em 2026-07-13, os IDs de plugin foram alterados para
`com.totalcross.application` e `com.totalcross.library`, e o artefato Maven
passou a ser `totalcross-gradle-plugin`. O plugin de biblioteca ganhou uma
classe de entrada própria para habilitar `totalcrossLib` por padrão; exemplo,
documentação e testes funcionais usam os IDs novos.

Em 2026-07-13, os valores de `platforms` passaram a ser normalizados para a
sintaxe de `tc.Deploy`: `linux` é enviado como `-linux`, e valores já prefixados
são preservados.

Em 2026-07-15, foi adicionada a opção `deploySdkJar`. Ela coloca um
`totalcross-sdk.jar` reconstruído antes do JAR da distribuição no classpath de
`tc.Deploy`, permitindo validar uma correção sem publicar ou criar um SDK
completo.

Em 2026-07-15, o resolvedor Zulu passou a excluir builds CRaC. A variante CRaC
17.0.19 falhou em macOS ao criar subprocessos via `ProcessBuilder`, enquanto a
variante regular é necessária para executar ferramentas Android como `protoc`.

Em 2026-07-15, o exemplo passou a aceitar `-PtestJdkPath` para validar o
deployer com uma JDK 17 explicitamente fornecida quando a distribuição Zulu em
cache não consegue criar subprocessos no macOS.

Em 2026-07-15, foi adicionada a opção `logLevel`. SDKs 7.3.0 ou posteriores
recebem `/log-level` com `quiet`, `normal`, `verbose` ou `debug`; SDKs legados
aceitam somente `verbose`, traduzido para `/v`.
