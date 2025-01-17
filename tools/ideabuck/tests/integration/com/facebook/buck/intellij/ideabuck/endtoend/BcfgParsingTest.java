/*
 * Copyright 2015-present Facebook, Inc.
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

package com.facebook.buck.intellij.ideabuck.endtoend;

import com.facebook.buck.intellij.ideabuck.lang.BcfgParserDefinition;
import com.intellij.testFramework.ParsingTestCase;

public class BcfgParsingTest extends ParsingTestCase {

  public BcfgParsingTest() {
    super("bcfg", "bcfg", new BcfgParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "tests/testdata/psi";
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }

  private void doTest() {
    doTest(true);
  }

  public void testEmpty() {
    doTest();
  }

  public void testSimple() {
    doTest();
  }

  public void testImportFile() {
    doTest();
  }

  public void testComments() {
    doTest();
  }

  public void testSample() {
    doTest();
  }

  public void testRecoverFromErrors() {
    doTest();
  }
}
