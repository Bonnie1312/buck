/*
 * Copyright 2018-present Facebook, Inc.
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

package com.facebook.buck.jvm.core;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.sourcepath.ArchiveMemberSourcePath;
import com.facebook.buck.core.sourcepath.BuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.ExplicitBuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolver;
import com.facebook.buck.util.unarchive.Unzip;
import com.facebook.infer.annotation.Assertions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import javax.annotation.Nullable;

/** The default inmplementation of JavaAbiInfo. */
public class DefaultJavaAbiInfo implements JavaAbiInfo {
  private final BuildTarget buildTarget;
  private final JarContentsSupplier jarContentsSupplier;

  public DefaultJavaAbiInfo(BuildTarget buildTarget, @Nullable SourcePath jarPath) {
    this.buildTarget = buildTarget;
    this.jarContentsSupplier = new JarContentsSupplier(jarPath);
  }

  @Override
  public BuildTarget getBuildTarget() {
    return buildTarget;
  }

  @Override
  public ImmutableSortedSet<SourcePath> getJarContents() {
    return jarContentsSupplier.get();
  }

  @Override
  public boolean jarContains(String path) {
    return jarContentsSupplier.jarContains(path);
  }

  public void load(SourcePathResolver pathResolver) throws IOException {
    jarContentsSupplier.load(pathResolver);
  }

  public void invalidate() {
    jarContentsSupplier.invalidate();
  }

  private static class JarContentsSupplier {
    @Nullable private final SourcePath jarSourcePath;
    @Nullable private ImmutableSortedSet<SourcePath> contents;
    @Nullable private ImmutableSet<Path> contentPaths;

    public JarContentsSupplier(@Nullable SourcePath jarSourcePath) {
      this.jarSourcePath = jarSourcePath;
    }

    public void load(SourcePathResolver resolver) throws IOException {
      Preconditions.checkState(
          contents == null,
          "load() called without a preceding invalidate(). This usually indicates "
              + "that a rule is calling load in initializeFromDisk() but failing to call "
              + "invalidate() in invalidateInitializeFromDiskState().");
      if (jarSourcePath == null) {
        contents = ImmutableSortedSet.of();
        contentPaths = ImmutableSet.of();
      } else {
        Path jarAbsolutePath = resolver.getAbsolutePath(jarSourcePath);
        if (Files.isDirectory(jarAbsolutePath)) {
          BuildTargetSourcePath buildTargetSourcePath = (BuildTargetSourcePath) jarSourcePath;
          contents =
              Files.walk(jarAbsolutePath)
                  .filter(path -> !path.endsWith(JarFile.MANIFEST_NAME))
                  .map(
                      path ->
                          ExplicitBuildTargetSourcePath.of(buildTargetSourcePath.getTarget(), path))
                  .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
        } else {
          SourcePath nonNullJarSourcePath = Assertions.assertNotNull(jarSourcePath);
          contents =
              Unzip.getZipMembers(jarAbsolutePath)
                  .stream()
                  .filter(path -> !path.endsWith(JarFile.MANIFEST_NAME))
                  .map(path -> ArchiveMemberSourcePath.of(nonNullJarSourcePath, path))
                  .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
        }
        contentPaths =
            contents
                .stream()
                .map(
                    sourcePath -> {
                      if (sourcePath instanceof ExplicitBuildTargetSourcePath) {
                        return ((ExplicitBuildTargetSourcePath) sourcePath).getResolvedPath();
                      } else {
                        return ((ArchiveMemberSourcePath) sourcePath).getMemberPath();
                      }
                    })
                .collect(ImmutableSet.toImmutableSet());
      }
    }

    public ImmutableSortedSet<SourcePath> get() {
      return Preconditions.checkNotNull(contents, "Must call load first.");
    }

    public boolean jarContains(String path) {
      return Preconditions.checkNotNull(contentPaths, "Must call load first.")
          .contains(Paths.get(path));
    }

    public void invalidate() {
      contents = null;
      contentPaths = null;
    }
  }
}