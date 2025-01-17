/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.buck.core.artifact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.BuildTargetFactory;
import com.facebook.buck.core.rules.analysis.action.ActionAnalysisData;
import com.facebook.buck.core.rules.analysis.action.ImmutableActionAnalysisDataKey;
import com.facebook.buck.core.sourcepath.ExplicitBuildTargetSourcePath;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class ArtifactImplTest {

  @Test
  public void artifactTransitionsToBuildArtifact() {
    BuildTarget target = BuildTargetFactory.newInstance("//my:foo");
    Path packagePath = Paths.get("my/foo__");
    Path path = Paths.get("bar");
    DeclaredArtifact artifact = ArtifactImpl.of(target, packagePath, path);
    assertFalse(artifact.isBound());

    ImmutableActionAnalysisDataKey key =
        ImmutableActionAnalysisDataKey.of(target, new ActionAnalysisData.ID() {});
    BuildArtifact materialized = artifact.materialize(key);

    assertTrue(materialized.isBound());
    assertEquals(key, materialized.getActionDataKey());
    assertEquals(
        ExplicitBuildTargetSourcePath.of(target, packagePath.resolve(path)),
        materialized.getSourcePath());
  }
}
