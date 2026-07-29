// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Versioned JSON codec for the complete build-tool project model. */
public final class ProjectModelCodec {
  public String toJson(ProjectModel model) {
    return "{" + field("schemaVersion", ProjectModel.SCHEMA_VERSION) + "," + field("buildTool", model.buildTool()) + ","
        + field("project", model.project()) + ",\"mainSourceRoots\":" + paths(model.sourceRoots().stream().map(SourceRoot::path).toList())
        + ",\"testSourceRoots\":" + paths(model.testSourceRoots().stream().map(SourceRoot::path).toList())
        + ",\"resourceRoots\":" + paths(model.resourceRoots().stream().map(ResourceRoot::path).toList())
        + "," + field("classOutput", model.classOutput().path()) + "," + field("testClassOutput", model.testClassOutput().path())
        + ",\"dependencies\":" + paths(model.dependencies().entries()) + "," + field("mainClass", model.mainClass())
        + "," + field("sdkVersion", model.sdkVersion()) + "," + field("sdkCoordinate", model.sdkCoordinate())
        + "," + field("toolingJdkIdentity", model.toolingJdkIdentity()) + ",\"javaPolicy\":{" + field("buildJvm", model.javaPolicy().buildJvm())
        + "," + field("toolingJdk", model.javaPolicy().toolingJdk()) + "," + field("applicationTarget", model.javaPolicy().applicationTarget())
        + "},\"retrolambda\":{" + field("required", model.retrolambda().required()) + "," + field("reason", model.retrolambda().reason())
        + "},\"launcherArguments\":" + strings(model.launcherArguments()) + ",\"deployArguments\":" + strings(model.deployArguments())
        + ",\"platforms\":" + strings(model.platforms()) + ",\"preview\":" + model.preview().toJson() + "}";
  }

  public ProjectModel fromJson(String json) {
    Map<String, Object> root = object(new Parser(json).value());
    if (number(root, "schemaVersion") != ProjectModel.SCHEMA_VERSION) throw new IllegalArgumentException("unsupported project model schema");
    Map<String, Object> policy = object(root.get("javaPolicy"));
    Map<String, Object> retrolambda = object(root.get("retrolambda"));
    Map<String, Object> preview = object(root.get("preview"));
    PreviewSessionDescriptor session = new PreviewSessionDescriptor(number(preview, "version"), buildTool(preview, "buildTool"),
        path(preview, "project"), path(preview, "descriptor"), string(preview, "mainClass"), string(preview, "sdkVersion"));
    return new ProjectModel(buildTool(root, "buildTool"), path(root, "project"), sourceRoots(root, "mainSourceRoots"),
        sourceRoots(root, "testSourceRoots"), resourceRoots(root, "resourceRoots"), new ClassOutput(path(root, "classOutput")),
        new ClassOutput(path(root, "testClassOutput")), new DependencyClasspath(paths(root, "dependencies")), string(root, "mainClass"),
        string(root, "sdkVersion"), string(root, "sdkCoordinate"), string(root, "toolingJdkIdentity"),
        new JavaCompatibilityPolicy(number(policy, "buildJvm"), number(policy, "toolingJdk"), number(policy, "applicationTarget")),
        new RetrolambdaPlan(bool(retrolambda, "required"), string(retrolambda, "reason")), strings(root, "launcherArguments"),
        strings(root, "deployArguments"), strings(root, "platforms"), session);
  }

  private static String field(String name, Object value) { return "\"" + name + "\":" + json(value.toString()); }
  private static String field(String name, int value) { return "\"" + name + "\":" + value; }
  private static String field(String name, boolean value) { return "\"" + name + "\":" + value; }
  private static String paths(List<Path> values) { return strings(values.stream().map(Path::toString).toList()); }
  private static String strings(List<String> values) { return values.stream().map(ProjectModelCodec::json).collect(java.util.stream.Collectors.joining(",", "[", "]")); }
  private static String json(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
  private static Map<String, Object> object(Object value) { if (value instanceof Map<?, ?> map) return cast(map); throw new IllegalArgumentException("expected JSON object"); }
  private static Map<String, Object> cast(Map<?, ?> source) { Map<String, Object> result = new LinkedHashMap<>(); source.forEach((k, v) -> result.put(String.valueOf(k), v)); return result; }
  private static String string(Map<String, Object> value, String name) { Object result = value.get(name); if (result instanceof String text) return text; throw new IllegalArgumentException("missing string " + name); }
  private static int number(Map<String, Object> value, String name) { Object result = value.get(name); if (result instanceof Number number) return number.intValue(); throw new IllegalArgumentException("missing number " + name); }
  private static boolean bool(Map<String, Object> value, String name) { Object result = value.get(name); if (result instanceof Boolean flag) return flag; throw new IllegalArgumentException("missing boolean " + name); }
  private static Path path(Map<String, Object> value, String name) { return Path.of(string(value, name)); }
  private static BuildTool buildTool(Map<String, Object> value, String name) { return BuildTool.valueOf(string(value, name)); }
  private static List<Path> paths(Map<String, Object> value, String name) { return strings(value, name).stream().map(Path::of).toList(); }
  private static List<SourceRoot> sourceRoots(Map<String, Object> value, String name) { return paths(value, name).stream().map(SourceRoot::new).toList(); }
  private static List<ResourceRoot> resourceRoots(Map<String, Object> value, String name) { return paths(value, name).stream().map(ResourceRoot::new).toList(); }
  private static List<String> strings(Map<String, Object> value, String name) {
    Object raw = value.get(name); if (!(raw instanceof List<?> list)) throw new IllegalArgumentException("missing array " + name);
    return list.stream().map(item -> { if (item instanceof String text) return text; throw new IllegalArgumentException("expected string array"); }).toList();
  }

  private static final class Parser {
    private final String source; private int index;
    Parser(String source) { this.source = source; }
    Object value() { skip(); Object result = next(); skip(); if (index != source.length()) throw invalid(); return result; }
    private Object next() { skip(); if (index >= source.length()) throw invalid(); char token = source.charAt(index);
      if (token == '{') return object(); if (token == '[') return array(); if (token == '\"') return string(); if (token == 't' || token == 'f') return bool(); return number(); }
    private Map<String, Object> object() { expect('{'); Map<String, Object> result = new LinkedHashMap<>(); skip(); if (take('}')) return result;
      do { String key = string(); skip(); expect(':'); result.put(key, next()); skip(); } while (take(',')); expect('}'); return result; }
    private List<Object> array() { expect('['); List<Object> result = new ArrayList<>(); skip(); if (take(']')) return result;
      do { result.add(next()); skip(); } while (take(',')); expect(']'); return result; }
    private String string() { expect('\"'); StringBuilder result = new StringBuilder(); while (index < source.length()) { char token = source.charAt(index++); if (token == '\"') return result.toString(); if (token != '\\') { result.append(token); continue; } if (index >= source.length()) throw invalid(); char escaped = source.charAt(index++); result.append(escaped == 'n' ? '\n' : escaped); } throw invalid(); }
    private Boolean bool() { if (source.startsWith("true", index)) { index += 4; return true; } if (source.startsWith("false", index)) { index += 5; return false; } throw invalid(); }
    private Integer number() { int start = index; while (index < source.length() && Character.isDigit(source.charAt(index))) index++; try { return Integer.valueOf(source.substring(start, index)); } catch (NumberFormatException error) { throw invalid(); } }
    private void skip() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
    private void expect(char token) { skip(); if (index >= source.length() || source.charAt(index++) != token) throw invalid(); }
    private boolean take(char token) { skip(); if (index < source.length() && source.charAt(index) == token) { index++; return true; } return false; }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("invalid project model JSON at " + index); }
  }
}
