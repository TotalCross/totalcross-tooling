/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import com.totalcross.tooling.conversion.inventory.ProjectInventoryReader;
import com.totalcross.tooling.conversion.plan.ConversionPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Applies only reviewed source moves and keeps a hash-verifiable rollback journal. */
public final class ProjectConversionTransaction {
  private static final String BACKUPS = ".totalcross-conversion-backup";
  private static final String JOURNALS = ".totalcross-conversion-journal";
  private final ProjectInventoryReader inventoryReader;

  public ProjectConversionTransaction() { this(new ProjectInventoryReader()); }
  ProjectConversionTransaction(ProjectInventoryReader inventoryReader) { this.inventoryReader = inventoryReader; }

  public Result apply(ConversionPlan plan) throws IOException {
    Path root = plan.project();
    if (!inventoryReader.read(root).fingerprint().equals(plan.inventoryFingerprint())) {
      throw new IOException("project changed after analysis; run convert-project analyze again");
    }
    if (plan.mainWindowCandidates().size() != 1) {
      throw new IOException("conversion requires exactly one selected MainWindow candidate");
    }
    String operation = Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
    Path backup = root.resolve(BACKUPS).resolve(operation);
    Path journal = root.resolve(JOURNALS).resolve(operation + ".journal");
    List<ConversionPlan.Move> moves = plan.moves().stream().filter(move -> !move.source().equals(move.destination())).toList();
    verifyDestinations(root, moves);
    Files.createDirectories(backup);
    Files.createDirectories(journal.getParent());
    List<ConversionPlan.Move> completed = new ArrayList<>();
    try {
      for (ConversionPlan.Move move : moves) {
        Path source = within(root, move.source());
        Path destination = within(root, move.destination());
        if (!Files.isRegularFile(source)) throw new IOException("source changed after analysis: " + move.source());
        Path copy = backup.resolve(move.source());
        Files.createDirectories(copy.getParent());
        Files.copy(source, copy, StandardCopyOption.COPY_ATTRIBUTES);
        move(source, destination);
        completed.add(move);
        append(journal, move);
      }
      return new Result(backup, journal, completed.size(), List.of());
    } catch (Exception failure) {
      rollback(root, backup, completed);
      throw failure instanceof IOException io ? io : new IOException("conversion transaction failed", failure);
    }
  }

  /** Applies reviewed source moves and generated build files as one reversible operation. */
  public Result apply(ConversionPlan plan, java.util.Map<Path, String> generated) throws IOException {
    GeneratedFileTransaction generatedFiles = new GeneratedFileTransaction();
    Result moved = apply(plan);
    List<Path> created = List.of();
    try {
      created = generatedFiles.apply(plan.project(), generated);
      for (Path file : created) Files.writeString(moved.journal(), "G\t" + plan.project().relativize(file) + System.lineSeparator(),
          java.nio.file.StandardOpenOption.APPEND);
      return new Result(moved.backup(), moved.journal(), moved.movedFiles(), created);
    } catch (Exception failure) {
      generatedFiles.rollback(created);
      rollback(moved, plan.project());
      throw failure instanceof IOException io ? io : new IOException("conversion transaction failed", failure);
    }
  }

  public void rollback(Result result, Path project) throws IOException {
    rollback(project, result.backup(), journal(result.journal()));
    new GeneratedFileTransaction().rollback(result.generatedFiles());
  }

  /** Reverses a completed operation from its persisted journal without re-analyzing the project. */
  public int rollback(Path journal) throws IOException {
    Path file = journal.toAbsolutePath().normalize();
    Path journalDirectory = file.getParent();
    if (journalDirectory == null || !JOURNALS.equals(journalDirectory.getFileName().toString())) {
      throw new IOException("not a TotalCross conversion journal: " + journal);
    }
    Path root = journalDirectory.getParent();
    if (root == null) throw new IOException("conversion journal has no project root: " + journal);
    String name = file.getFileName().toString();
    if (!name.endsWith(".journal")) throw new IOException("invalid conversion journal name: " + journal);
    List<ConversionPlan.Move> moves = journal(file);
    rollback(root, root.resolve(BACKUPS).resolve(name.substring(0, name.length() - ".journal".length())), moves);
    new GeneratedFileTransaction().rollback(journalGenerated(root, file));
    return moves.size();
  }

  private void verifyDestinations(Path root, List<ConversionPlan.Move> moves) throws IOException {
    var destinations = new java.util.HashSet<Path>();
    for (ConversionPlan.Move move : moves) {
      Path source = within(root, move.source());
      Path destination = within(root, move.destination());
      if (!destinations.add(destination)) throw new IOException("multiple conversion moves target " + move.destination());
      if (!Files.isRegularFile(source)) throw new IOException("source is unavailable: " + move.source());
      if (Files.exists(destination)) throw new IOException("destination collision: " + move.destination());
    }
  }

  private static void move(Path source, Path destination) throws IOException {
    Files.createDirectories(destination.getParent());
    try { Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE); }
    catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
      Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
      if (!hash(source).equals(hash(destination))) throw new IOException("copied file hash does not match: " + source);
      Files.delete(source);
    }
  }

  private static void rollback(Path root, Path backup, List<ConversionPlan.Move> moves) throws IOException {
    for (ConversionPlan.Move move : moves.stream().sorted(Comparator.comparingInt(
        (ConversionPlan.Move item) -> item.destination().getNameCount()).reversed()).toList()) {
      Path original = backup.resolve(move.source());
      Path source = within(root, move.source());
      Path destination = within(root, move.destination());
      Files.deleteIfExists(destination);
      Files.createDirectories(source.getParent());
      Files.copy(original, source, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  private static List<ConversionPlan.Move> journal(Path file) throws IOException {
    List<ConversionPlan.Move> moves = new ArrayList<>();
    for (String line : Files.readAllLines(file)) {
      String[] fields = line.split("\\t", -1);
      if (fields.length == 3) moves.add(new ConversionPlan.Move(Path.of(fields[0]), Path.of(fields[1]), fields[2]));
    }
    return moves;
  }

  private static List<Path> journalGenerated(Path root, Path file) throws IOException {
    return Files.readAllLines(file).stream().filter(line -> line.startsWith("G\t")).map(line -> line.substring(2))
        .map(Path::of).map(root::resolve).toList();
  }

  private static void append(Path journal, ConversionPlan.Move move) throws IOException {
    Files.writeString(journal, move.source() + "\t" + move.destination() + "\t" + move.kind() + System.lineSeparator(),
        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
  }

  private static Path within(Path root, Path relative) throws IOException {
    Path path = root.resolve(relative).normalize();
    if (!path.startsWith(root)) throw new IOException("conversion path escapes project: " + relative);
    return path;
  }

  private static String hash(Path file) throws IOException {
    try (var input = Files.newInputStream(file)) {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      for (int count; (count = input.read(buffer)) >= 0;) digest.update(buffer, 0, count);
      return java.util.HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
  }

  public record Result(Path backup, Path journal, int movedFiles, List<Path> generatedFiles) {
    public Result { generatedFiles = List.copyOf(generatedFiles); }
  }
}
