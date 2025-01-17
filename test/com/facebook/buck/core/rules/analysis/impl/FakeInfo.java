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
package com.facebook.buck.core.rules.analysis.impl;

import com.facebook.buck.core.rules.providers.Provider;
import com.facebook.buck.core.rules.providers.ProviderInfo;

public class FakeInfo implements ProviderInfo<FakeInfo> {

  private final Provider<FakeInfo> provider;

  public FakeInfo(Provider<FakeInfo> provider) {
    this.provider = provider;
  }

  @Override
  public Provider<FakeInfo> getProvider() {
    return provider;
  }
}
