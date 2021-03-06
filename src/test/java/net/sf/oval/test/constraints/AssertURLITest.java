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

import net.sf.oval.constraint.AssertURLCheck;
import net.sf.oval.constraint.AssertURLCheck.URIScheme;
import net.sf.oval.internal.util.ArrayUtils;

/**
 * @author Makkari - initial implementation
 * @author Sebastian Thomschke
 */
public class AssertURLITest extends AbstractContraintsTest {

   private static final URIScheme[] PERMITTED_SCHEMES = {URIScheme.FTP, URIScheme.HTTP, URIScheme.HTTPS};

   @Test
   public void testAssertURL() {
      final AssertURLCheck check = new AssertURLCheck();
      super.testCheck(check);
      assertThat(check.getPermittedURISchemes()).isNull();

      check.setPermittedURISchemes(PERMITTED_SCHEMES);
      final URIScheme[] actualPermittedSchemes = check.getPermittedURISchemes();
      assertThat(actualPermittedSchemes).isNotNull();
      assertThat(actualPermittedSchemes).hasSameSizeAs(PERMITTED_SCHEMES);
      for (final URIScheme element : PERMITTED_SCHEMES) {
         ArrayUtils.containsEqual(actualPermittedSchemes, element);
      }

      assertThat(check.isConnect()).isFalse();
      check.setConnect(true);
      assertThat(check.isConnect()).isTrue();

      check.setConnect(false);
      assertThat(check.isSatisfied(this, null, null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "http", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "https", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "ftp", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "http://www.google.com", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "httpa://www.google.com", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "https://www.google.com", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "httPs://www.google.com", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "ftp://ftp.uni-erlangen.de/debian/README.mirrors.txt", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "ptth://www.google.com", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "http://www.g[oogle.com", null, validator)).isFalse();

      check.setConnect(true);
      assertThat(check.isSatisfied(this, null, null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "http", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "https", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "ftp", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "http://www.google.com", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "https://www.google.com", null, validator)).isTrue();
      assertThat(check.isSatisfied(this, "http://127.0.0.1:34343", null, validator)).isFalse();
      assertThat(check.isSatisfied(this, "ftp://ftp.uni-erlangen.de/debian/foo.html", null, validator)).isFalse();
      if (!System.getenv().containsKey("TRAVIS")) {
         assertThat(check.isSatisfied(this, "ftp://ftp.uni-erlangen.de/debian/README.mirrors.txt", null, validator)).isTrue();
      }

      check.setPermittedURISchemes((URIScheme[]) null);
   }
}
