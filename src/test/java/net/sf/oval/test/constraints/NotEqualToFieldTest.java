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

import net.sf.oval.constraint.NotEqualToFieldCheck;

/**
 * @author Sebastian Thomschke
 */
public class NotEqualToFieldTest extends AbstractContraintsTest {

   public static class EnrichedEntity extends Entity {
      @SuppressWarnings("hiding")
      protected String password1;

      protected String password1Alternative;

      protected String password2Alternative;
   }

   public static class Entity {
      protected String password1 = "mug";
      protected String password2WhatEver;

      public String getPassword2() {
         return password2WhatEver;
      }
   }

   @Test
   public void testEqualToField() {
      final NotEqualToFieldCheck check = new NotEqualToFieldCheck();
      super.testCheck(check);
      assertThat(check.isSatisfied(null, null, null, null)).isTrue();

      final EnrichedEntity entity = new EnrichedEntity();
      entity.password1 = "secret";
      entity.password1Alternative = "zecret";

      check.setFieldName("password1");
      check.setUseGetter(false);

      assertThat(check.isSatisfied(entity, entity.password1Alternative, null, null)).isTrue();
      entity.password1Alternative = "secret";
      assertThat(check.isSatisfied(entity, entity.password1Alternative, null, null)).isFalse();

      entity.password2WhatEver = "secret";
      entity.password2Alternative = "zecret";

      check.setFieldName("password2");
      check.setUseGetter(true);

      assertThat(check.isSatisfied(entity, entity.password2Alternative, null, null)).isTrue();
      entity.password2Alternative = "secret";
      assertThat(check.isSatisfied(entity, entity.password2Alternative, null, null)).isFalse();
   }
}
