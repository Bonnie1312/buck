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

package com.facebook.buck.parser;

import com.facebook.buck.core.parser.buildtargetparser.UnconfiguredBuildTargetViewFactory;
import com.facebook.buck.core.rules.knowntypes.KnownRuleTypesProvider;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.io.watchman.Watchman;
import com.facebook.buck.manifestservice.ManifestService;
import com.facebook.buck.rules.coercer.ConstructorArgMarshaller;
import com.facebook.buck.rules.coercer.TypeCoercerFactory;
import com.facebook.buck.util.ThrowingCloseableMemoizedSupplier;
import com.facebook.buck.util.hashing.FileHashLoader;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public abstract class PerBuildStateFactory {

  protected final ThrowingCloseableMemoizedSupplier<ManifestService, IOException>
      manifestServiceSupplier;
  protected final FileHashLoader fileHashLoader;

  protected PerBuildStateFactory(
      ThrowingCloseableMemoizedSupplier<ManifestService, IOException> manifestServiceSupplier,
      FileHashLoader fileHashLoader) {
    this.manifestServiceSupplier = manifestServiceSupplier;
    this.fileHashLoader = fileHashLoader;
  }

  /**
   * Creates {@link PerBuildStateFactory} which can be used to create {@link PerBuildState}.
   * Depending on the configuration this method can create a factory that supports configurable
   * attributes.
   */
  public static PerBuildStateFactory createFactory(
      TypeCoercerFactory typeCoercerFactory,
      ConstructorArgMarshaller marshaller,
      KnownRuleTypesProvider knownRuleTypesProvider,
      ParserPythonInterpreterProvider parserPythonInterpreterProvider,
      Watchman watchman,
      BuckEventBus eventBus,
      ThrowingCloseableMemoizedSupplier<ManifestService, IOException> manifestServiceSupplier,
      FileHashLoader fileHashLoader,
      UnconfiguredBuildTargetViewFactory unconfiguredBuildTargetFactory) {
    return new PerBuildStateFactoryWithConfigurableAttributes(
        typeCoercerFactory,
        marshaller,
        knownRuleTypesProvider,
        parserPythonInterpreterProvider,
        watchman,
        eventBus,
        manifestServiceSupplier,
        fileHashLoader,
        unconfiguredBuildTargetFactory);
  }

  public PerBuildState create(
      ParsingContext parsingContext, DaemonicParserState daemonicParserState) {
    return create(parsingContext, daemonicParserState, Optional.empty());
  }

  public PerBuildState create(
      ParsingContext parsingContext,
      DaemonicParserState daemonicParserState,
      AtomicLong processedBytes) {
    return create(parsingContext, daemonicParserState, Optional.of(processedBytes));
  }

  protected abstract PerBuildState create(
      ParsingContext parsingContext,
      DaemonicParserState daemonicParserState,
      Optional<AtomicLong> parseProcessedBytes);
}
