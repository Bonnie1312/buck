/*
 * Copyright 2016-present Facebook, Inc.
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
package com.facebook.buck.cxx;

import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolver;
import com.facebook.buck.cxx.toolchain.CxxPlatform;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkTargetGroup;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkTargetMode;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkableGroup;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkableInput;
import com.facebook.buck.rules.args.StringArg;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.Optional;

class OmnibusRootNode extends OmnibusNode implements NativeLinkTargetGroup, NativeLinkableGroup {

  public OmnibusRootNode(String target, Iterable<? extends NativeLinkableGroup> deps) {
    super(target, deps);
  }

  public OmnibusRootNode(String target) {
    super(target);
  }

  @Override
  public NativeLinkTargetMode getNativeLinkTargetMode(CxxPlatform cxxPlatform) {
    return NativeLinkTargetMode.library(getBuildTarget().getShortName());
  }

  @Override
  public Iterable<? extends NativeLinkableGroup> getNativeLinkTargetDeps(
      CxxPlatform cxxPlatform, ActionGraphBuilder graphBuilder) {
    return Iterables.concat(
        getNativeLinkableDepsForPlatform(cxxPlatform, graphBuilder),
        getNativeLinkableExportedDepsForPlatform(cxxPlatform, graphBuilder));
  }

  @Override
  public NativeLinkableInput getNativeLinkTargetInput(
      CxxPlatform cxxPlatform, ActionGraphBuilder graphBuilder, SourcePathResolver pathResolver) {
    return NativeLinkableInput.builder().addArgs(StringArg.of(getBuildTarget().toString())).build();
  }

  @Override
  public Optional<Path> getNativeLinkTargetOutputPath() {
    return Optional.empty();
  }
}
