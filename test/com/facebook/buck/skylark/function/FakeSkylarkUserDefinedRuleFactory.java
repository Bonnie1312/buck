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
package com.facebook.buck.skylark.function;

import static com.facebook.buck.skylark.function.SkylarkRuleFunctions.IMPLICIT_ATTRIBUTES;

import com.facebook.buck.core.starlark.rule.SkylarkUserDefinedRule;
import com.facebook.buck.core.starlark.rule.attr.Attribute;
import com.facebook.buck.core.starlark.rule.attr.impl.ImmutableStringAttribute;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.LabelSyntaxException;
import com.google.devtools.build.lib.events.Location;
import com.google.devtools.build.lib.syntax.BaseFunction;
import com.google.devtools.build.lib.syntax.Environment;
import com.google.devtools.build.lib.syntax.EvalException;
import com.google.devtools.build.lib.syntax.FuncallExpression;
import com.google.devtools.build.lib.syntax.FunctionSignature;
import com.google.devtools.build.lib.syntax.Runtime;
import javax.annotation.Nullable;

/** Simple container class to make constructing {@link SkylarkUserDefinedRule}s easier in tests */
public class FakeSkylarkUserDefinedRuleFactory {
  private FakeSkylarkUserDefinedRuleFactory() {}

  /**
   * @return a simple rule with a single string argument "baz" that is exported as {@code
   *     //foo:bar.bzl:_impl}
   */
  public static SkylarkUserDefinedRule createSimpleRule()
      throws EvalException, LabelSyntaxException {
    return createSingleArgRule(
        "baz", new ImmutableStringAttribute("default", "", false, ImmutableList.of()));
  }

  /** Create a single argument rule with the given argument name and attr to back it */
  public static SkylarkUserDefinedRule createSingleArgRule(String attrName, Attribute<?> attr)
      throws EvalException, LabelSyntaxException {
    FunctionSignature signature = FunctionSignature.of(1, 0, 0, false, false, "ctx");
    BaseFunction implementation =
        new BaseFunction("unconfigured", signature) {
          @Override
          public Object call(Object[] args, @Nullable FuncallExpression ast, Environment env) {
            return Runtime.NONE;
          }
        };
    SkylarkUserDefinedRule ret =
        SkylarkUserDefinedRule.of(
            Location.BUILTIN, implementation, IMPLICIT_ATTRIBUTES, ImmutableMap.of(attrName, attr));
    ret.export(Label.parseAbsolute("//foo:bar.bzl", ImmutableMap.of()), "_impl");
    return ret;
  }
}
