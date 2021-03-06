/*********************************************************************
 * Copyright 2005-2020 by Sebastian Thomschke and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *********************************************************************/
package net.sf.oval.test.constraints;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import net.sf.oval.constraint.EmailCheck;

/**
 * @author Sebastian Thomschke
 */
public class EmailTest extends AbstractContraintsTest {

   @Test
   public void testEmailWithoutPersonalNameAllowed() {
      final EmailCheck check = new EmailCheck();

      check.setAllowPersonalName(false);

      super.testCheck(check);
      assertThat(check.isSatisfied(null, null, null, null)).isTrue();

      assertThat(check.isSatisfied(null, "testjee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.co", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.jee@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.jee@yahoo.co.biz", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.net", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "user@subname1.subname2.subname3.domainname.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.j'ee@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.j'e.e'@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "testj'ee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test&jee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test+jee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_j.s.@yahoo.com", null, null)).isTrue();

      assertThat(check.isSatisfied(null, "testjee@@yahoo.com", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee#marry@yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee@ yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee  @yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_j ee  @yah oo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee  @yah oo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee @ yahoo.com", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "user@subname1.subname2.subname3.domainn#ame.co.uk", null, null)).isFalse();

      assertThat(check.isSatisfied(null, "John <testjee@yahoo.com>", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "John Doe <testjee@yahoo.com>", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "John@ <testjee@yahoo.com>", null, null)).isFalse();
   }

   @Test
   public void testEmailWithPersonalNameAllowed() {
      final EmailCheck check = new EmailCheck();

      check.setAllowPersonalName(true);

      super.testCheck(check);
      assertThat(check.isSatisfied(null, null, null, null)).isTrue();

      assertThat(check.isSatisfied(null, "testjee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.co", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.jee@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.jee@yahoo.co.biz", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_jee@yahoo.net", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "user@subname1.subname2.subname3.domainname.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.j'ee@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test.j'e.e'@yahoo.co.uk", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "testj'ee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test&jee@yahoo.com", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "test_j.s.@yahoo.com", null, null)).isTrue();

      assertThat(check.isSatisfied(null, "testjee@@yahoo.com", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee#marry@yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee@ yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee  @yahoo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_j ee  @yah oo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee  @yah oo.co.uk", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "test_jee @ yahoo.com", null, null)).isFalse();
      assertThat(check.isSatisfied(null, "user@subname1.subname2.subname3.domainn#ame.co.uk", null, null)).isFalse();

      assertThat(check.isSatisfied(null, "John <testjee@yahoo.com>", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "John Doe <testjee@yahoo.com>", null, null)).isTrue();
      assertThat(check.isSatisfied(null, "John@ <testjee@yahoo.com>", null, null)).isFalse();
   }
}
