/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.cli;

import com.totalcross.tooling.conversion.plan.ConversionPlan;
import com.totalcross.tooling.conversion.plan.GradleProjectRenderer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** CLI-owned release version passed to the shared renderer; rendering itself stays in conversion tooling. */
final class ConversionPlanRenderer {
  Map<Path, String> render(ConversionPlan plan) throws IOException { return new GradleProjectRenderer().render(plan, "0.1.0"); }
}
