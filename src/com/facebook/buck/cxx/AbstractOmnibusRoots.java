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

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.util.graph.AbstractBreadthFirstTraversal;
import com.facebook.buck.core.util.immutables.BuckStyleTuple;
import com.facebook.buck.cxx.toolchain.CxxPlatform;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkTargetGroup;
import com.facebook.buck.cxx.toolchain.nativelink.NativeLinkableGroup;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * A helper class for building the included and excluded omnibus roots to pass to the omnibus
 * builder.
 */
@Value.Immutable
@BuckStyleTuple
abstract class AbstractOmnibusRoots {

  /** @return the {@link NativeLinkTargetGroup} roots that are included in omnibus linking. */
  abstract ImmutableMap<BuildTarget, NativeLinkTargetGroup> getIncludedRoots();

  /** @return the {@link NativeLinkableGroup} roots that are excluded from omnibus linking. */
  abstract ImmutableMap<BuildTarget, NativeLinkableGroup> getExcludedRoots();

  public static Builder builder(
      CxxPlatform cxxPlatform,
      ImmutableSet<BuildTarget> excludes,
      ActionGraphBuilder graphBuilder) {
    return new Builder(cxxPlatform, excludes, graphBuilder);
  }

  public static class Builder {

    private final CxxPlatform cxxPlatform;
    private final ImmutableSet<BuildTarget> excludes;
    private final ActionGraphBuilder graphBuilder;

    private final Map<BuildTarget, NativeLinkTargetGroup> includedRoots = new LinkedHashMap<>();
    private final Map<BuildTarget, NativeLinkableGroup> excludedRoots = new LinkedHashMap<>();

    private Builder(
        CxxPlatform cxxPlatform,
        ImmutableSet<BuildTarget> excludes,
        ActionGraphBuilder graphBuilder) {
      this.cxxPlatform = cxxPlatform;
      this.excludes = excludes;
      this.graphBuilder = graphBuilder;
    }

    /** Add a root which is included in omnibus linking. */
    public void addIncludedRoot(NativeLinkTargetGroup root) {
      includedRoots.put(root.getBuildTarget(), root);
    }

    /** Add a root which is excluded from omnibus linking. */
    public void addExcludedRoot(NativeLinkableGroup root) {
      excludedRoots.put(root.getBuildTarget(), root);
    }

    /**
     * Add a node which may qualify as either an included root, and excluded root, or neither.
     *
     * @return whether the node was added as a root.
     */
    public void addPotentialRoot(NativeLinkableGroup node) {
      Optional<NativeLinkTargetGroup> target = node.getNativeLinkTarget(cxxPlatform, graphBuilder);
      if (target.isPresent()
          && !excludes.contains(node.getBuildTarget())
          && node.supportsOmnibusLinking(cxxPlatform)) {
        addIncludedRoot(target.get());
      } else {
        addExcludedRoot(node);
      }
    }

    private ImmutableMap<BuildTarget, NativeLinkableGroup> buildExcluded() {
      Map<BuildTarget, NativeLinkableGroup> excluded = new LinkedHashMap<>(excludedRoots);

      // Find all excluded nodes reachable from the included roots.
      Map<BuildTarget, NativeLinkableGroup> includedRootDeps = new LinkedHashMap<>();
      for (NativeLinkTargetGroup target : includedRoots.values()) {
        for (NativeLinkableGroup linkable :
            target.getNativeLinkTargetDeps(cxxPlatform, graphBuilder)) {
          includedRootDeps.put(linkable.getBuildTarget(), linkable);
        }
      }
      new AbstractBreadthFirstTraversal<NativeLinkableGroup>(includedRootDeps.values()) {
        @Override
        public Iterable<NativeLinkableGroup> visit(NativeLinkableGroup linkable)
            throws RuntimeException {
          if (!linkable.supportsOmnibusLinking(cxxPlatform)) {
            excluded.put(linkable.getBuildTarget(), linkable);
            return ImmutableSet.of();
          }
          return Iterables.concat(
              linkable.getNativeLinkableDepsForPlatform(cxxPlatform, graphBuilder),
              linkable.getNativeLinkableExportedDepsForPlatform(cxxPlatform, graphBuilder));
        }
      }.start();

      // Prepare the final map of excluded roots, starting with the pre-defined ones.
      Map<BuildTarget, NativeLinkableGroup> updatedExcludedRoots =
          new LinkedHashMap<>(excludedRoots);

      // Recursively expand the excluded nodes including any preloaded deps, as we'll need this full
      // list to know which roots to exclude from omnibus linking.
      new AbstractBreadthFirstTraversal<NativeLinkableGroup>(excluded.values()) {
        @Override
        public Iterable<NativeLinkableGroup> visit(NativeLinkableGroup linkable) {
          if (includedRoots.containsKey(linkable.getBuildTarget())) {
            updatedExcludedRoots.put(linkable.getBuildTarget(), linkable);
          }
          return Iterables.concat(
              linkable.getNativeLinkableDepsForPlatform(cxxPlatform, graphBuilder),
              linkable.getNativeLinkableExportedDepsForPlatform(cxxPlatform, graphBuilder));
        }
      }.start();

      return ImmutableMap.copyOf(updatedExcludedRoots);
    }

    private ImmutableMap<BuildTarget, NativeLinkTargetGroup> buildIncluded(
        ImmutableSet<BuildTarget> excluded) {
      return ImmutableMap.copyOf(
          Maps.filterKeys(includedRoots, Predicates.not(excluded::contains)));
    }

    public boolean isEmpty() {
      return includedRoots.isEmpty() && excludedRoots.isEmpty();
    }

    public OmnibusRoots build() {
      ImmutableMap<BuildTarget, NativeLinkableGroup> excluded = buildExcluded();
      ImmutableMap<BuildTarget, NativeLinkTargetGroup> included = buildIncluded(excluded.keySet());
      return OmnibusRoots.of(included, excluded);
    }
  }
}
