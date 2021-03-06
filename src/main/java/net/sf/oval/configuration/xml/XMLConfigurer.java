/*********************************************************************
 * Copyright 2005-2020 by Sebastian Thomschke and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *********************************************************************/
package net.sf.oval.configuration.xml;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import net.sf.oval.AbstractCheck;
import net.sf.oval.Check;
import net.sf.oval.CheckExclusion;
import net.sf.oval.ConstraintTarget;
import net.sf.oval.configuration.CheckInitializationListener;
import net.sf.oval.configuration.Configurer;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.configuration.annotation.Constraint;
import net.sf.oval.configuration.pojo.POJOConfigurer;
import net.sf.oval.configuration.pojo.elements.ClassConfiguration;
import net.sf.oval.configuration.pojo.elements.ConstraintSetConfiguration;
import net.sf.oval.configuration.pojo.elements.ConstructorConfiguration;
import net.sf.oval.configuration.pojo.elements.FieldConfiguration;
import net.sf.oval.configuration.pojo.elements.MethodConfiguration;
import net.sf.oval.configuration.pojo.elements.MethodPostExecutionConfiguration;
import net.sf.oval.configuration.pojo.elements.MethodPreExecutionConfiguration;
import net.sf.oval.configuration.pojo.elements.MethodReturnValueConfiguration;
import net.sf.oval.configuration.pojo.elements.ObjectConfiguration;
import net.sf.oval.configuration.pojo.elements.ParameterConfiguration;
import net.sf.oval.constraint.AssertCheck;
import net.sf.oval.constraint.AssertConstraintSetCheck;
import net.sf.oval.constraint.AssertFalseCheck;
import net.sf.oval.constraint.AssertFieldConstraintsCheck;
import net.sf.oval.constraint.AssertNullCheck;
import net.sf.oval.constraint.AssertTrueCheck;
import net.sf.oval.constraint.AssertURLCheck;
import net.sf.oval.constraint.AssertURLCheck.URIScheme;
import net.sf.oval.constraint.AssertValidCheck;
import net.sf.oval.constraint.CheckWithCheck;
import net.sf.oval.constraint.CheckWithCheck.SimpleCheck;
import net.sf.oval.constraint.DateRangeCheck;
import net.sf.oval.constraint.DigitsCheck;
import net.sf.oval.constraint.EmailCheck;
import net.sf.oval.constraint.EqualToFieldCheck;
import net.sf.oval.constraint.FutureCheck;
import net.sf.oval.constraint.HasSubstringCheck;
import net.sf.oval.constraint.InstanceOfAnyCheck;
import net.sf.oval.constraint.InstanceOfCheck;
import net.sf.oval.constraint.LengthCheck;
import net.sf.oval.constraint.MatchPatternCheck;
import net.sf.oval.constraint.MaxCheck;
import net.sf.oval.constraint.MaxLengthCheck;
import net.sf.oval.constraint.MaxSizeCheck;
import net.sf.oval.constraint.MemberOfCheck;
import net.sf.oval.constraint.MinCheck;
import net.sf.oval.constraint.MinLengthCheck;
import net.sf.oval.constraint.MinSizeCheck;
import net.sf.oval.constraint.NoSelfReferenceCheck;
import net.sf.oval.constraint.NotBlankCheck;
import net.sf.oval.constraint.NotEmptyCheck;
import net.sf.oval.constraint.NotEqualCheck;
import net.sf.oval.constraint.NotEqualToFieldCheck;
import net.sf.oval.constraint.NotMatchPatternCheck;
import net.sf.oval.constraint.NotMemberOfCheck;
import net.sf.oval.constraint.NotNegativeCheck;
import net.sf.oval.constraint.NotNullCheck;
import net.sf.oval.constraint.PastCheck;
import net.sf.oval.constraint.RangeCheck;
import net.sf.oval.constraint.SizeCheck;
import net.sf.oval.constraint.ValidateWithMethodCheck;
import net.sf.oval.constraint.exclusion.NullableExclusion;
import net.sf.oval.exception.InvalidConfigurationException;
import net.sf.oval.guard.PostCheck;
import net.sf.oval.guard.PreCheck;
import net.sf.oval.internal.util.Assert;
import net.sf.oval.internal.util.ReflectionUtils;

/**
 * XStream (http://xstream.codehaus.org/) based XML configuration class.
 *
 * @author Sebastian Thomschke
 */
public class XMLConfigurer implements Configurer {

   /**
    * The converter is needed to allow the rendering of Assert's expr attribute value as an XML node value and not an XML attribute
    * <code>&lt;assert&gt;&lt;expr&gt;...&lt;/expr&gt;&lt;/assert&gt;</code> instead of <code>&lt;assert expr="..."&gt;</code>
    * This allows users to write complex, multi-line expressions.
    */
   protected static class AssertCheckConverter implements Converter {

      @Override
      public boolean canConvert(final Class clazz) {
         return clazz.equals(AssertCheck.class);
      }

      @Override
      public void marshal(final Object value, final HierarchicalStreamWriter writer, final MarshallingContext context) {
         final AssertCheck assertCheck = (AssertCheck) value;
         writer.addAttribute("lang", assertCheck.getLang());
         if (!"net.sf.oval.constraint.Assert.violated".equals(assertCheck.getMessage())) {
            writer.addAttribute("message", assertCheck.getMessage());
         }
         if (!"net.sf.oval.constraint.Assert".equals(assertCheck.getErrorCode())) {
            writer.addAttribute("errorCode", assertCheck.getErrorCode());
         }
         writer.addAttribute("severity", Integer.toString(assertCheck.getSeverity()));
         if (assertCheck.getWhen() != null) {
            writer.addAttribute("when", assertCheck.getWhen());
         }
         writer.startNode("expr");
         writer.setValue(assertCheck.getExpr());
         writer.endNode();
         final String[] profiles = assertCheck.getProfiles();
         if (profiles != null && profiles.length > 0) {
            writer.startNode("profiles");
            for (final String profile : profiles) {
               writer.startNode("string");
               writer.setValue(profile);
               writer.endNode();
            }
            writer.endNode();
         }
         final ConstraintTarget[] appliesTo = assertCheck.getAppliesTo();
         if (appliesTo != null && appliesTo.length > 0) {
            writer.startNode("appliesTo");
            for (final ConstraintTarget ctarget : appliesTo) {
               writer.startNode("constraintTarget");
               writer.setValue(ctarget.name());
               writer.endNode();
            }
            writer.endNode();
         }
      }

      @Override
      public AssertCheck unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
         final AssertCheck assertCheck = new AssertCheck();
         assertCheck.setLang(reader.getAttribute("lang"));
         assertCheck.setMessage(reader.getAttribute("message"));
         assertCheck.setErrorCode(reader.getAttribute("errorCode"));
         if (reader.getAttribute("severity") != null) {
            assertCheck.setSeverity(Integer.parseInt(reader.getAttribute("severity")));
         }
         if (reader.getAttribute("expr") != null) {
            assertCheck.setExpr(reader.getAttribute("expr"));
         }
         assertCheck.setTarget(reader.getAttribute("target"));
         assertCheck.setWhen(reader.getAttribute("when"));

         while (reader.hasMoreChildren()) {
            reader.moveDown();
            switch (reader.getNodeName()) {
               case "appliesTo":
                  final List<ConstraintTarget> targets = new ArrayList<>(2);
                  while (reader.hasMoreChildren()) {
                     reader.moveDown();
                     if ("constraintTarget".equals(reader.getNodeName())) {
                        targets.add(ConstraintTarget.valueOf(reader.getValue()));
                     }
                     reader.moveUp();
                  }
                  assertCheck.setAppliesTo(targets.toArray(new ConstraintTarget[targets.size()]));
                  break;
               case "expr":
                  assertCheck.setExpr(reader.getValue());
                  break;
               case "profiles":
                  final List<String> profiles = new ArrayList<>(4);
                  while (reader.hasMoreChildren()) {
                     reader.moveDown();
                     if ("string".equals(reader.getNodeName())) {
                        profiles.add(reader.getValue());
                     }
                     reader.moveUp();
                  }
                  assertCheck.setProfiles(profiles.toArray(new String[profiles.size()]));
                  break;
            }
            reader.moveUp();
         }

         onCheckInitialized(assertCheck);
         return assertCheck;
      }
   }

   protected static class ListConverter extends CollectionConverter {
      protected ListConverter(final Mapper mapper) {
         super(mapper);
      }

      @Override
      public boolean canConvert(final Class type) {
         return List.class.isAssignableFrom(type);
      }
   }

   protected static class PatternConverter implements Converter {

      @Override
      public boolean canConvert(final Class type) {
         return type == Pattern.class;
      }

      @Override
      public void marshal(final Object value, final HierarchicalStreamWriter writer, final MarshallingContext context) {
         final Pattern pattern = (Pattern) value;
         writer.addAttribute("pattern", pattern.pattern());
         writer.addAttribute("flags", Integer.toString(pattern.flags()));
      }

      @Override
      public Pattern unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
         final String pattern = reader.getAttribute("pattern");
         final String flags = reader.getAttribute("flags");
         if (flags == null || flags.trim().length() == 0)
            return Pattern.compile(pattern);
         return Pattern.compile(pattern, Integer.parseInt(flags));
      }
   }

   /**
    * This reflection provider applies default values declared on constraint annotations to the corresponding check class
    */
   private static final class XStreamReflectionProvider extends PureJavaReflectionProvider {
      @SuppressWarnings("unchecked")
      @Override
      public Object newInstance(final Class type) {
         final Object instance = super.newInstance(type);

         // test if a AnnotationCheck instance is requested
         if (instance instanceof AbstractAnnotationCheck) {
            // determine the constraint annotation
            Class<Annotation> constraintAnnotation = null;
            final ParameterizedType genericSuperclass = (ParameterizedType) type.getGenericSuperclass();
            for (final Type genericType : genericSuperclass.getActualTypeArguments()) {
               final Class<?> genericClass = (Class<?>) genericType;
               if (genericClass.isAnnotation() && genericClass.isAnnotationPresent(Constraint.class)) {
                  constraintAnnotation = (Class<Annotation>) genericClass;
                  break;
               }
            }
            // in case we could determine the constraint annotation, read the attributes and
            // apply the declared default values to the check instance
            if (constraintAnnotation != null) {
               for (final Method m : constraintAnnotation.getMethods()) {
                  final Object defaultValue = m.getDefaultValue();
                  if (defaultValue != null) {
                     ReflectionUtils.setViaSetter(instance, m.getName(), defaultValue);
                  }
               }
            }
         }
         return instance;
      }
   }

   private static final ThreadLocal<Collection<CheckInitializationListener>> CURRENT_LISTENERS = new ThreadLocal<>();

   public static XStream createXStream() {
      final XStream xStream = new XStream(new XStreamReflectionProvider(), new XIncludeAwareDOMDriver());
      XStream.setupDefaultSecurity(xStream);
      xStream.allowTypesByWildcard(new String[] {"net.sf.oval.**"});

      xStream.registerConverter(new ReflectionConverter(xStream.getMapper(), xStream.getReflectionProvider()) {
         @Override
         public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            final Object instance = super.unmarshal(reader, context);
            if (instance instanceof Check) {
               onCheckInitialized((Check) instance);
            }
            return instance;
         }
      }, XStream.PRIORITY_VERY_LOW);
      xStream.registerConverter(new ListConverter(xStream.getMapper()));
      xStream.registerConverter(new AssertCheckConverter());
      xStream.registerConverter(new PatternConverter());

      xStream.omitField(AbstractCheck.class, "messageVariablesUpToDate");

      xStream.useAttributeFor(Class.class);
      xStream.useAttributeFor(boolean.class);
      xStream.useAttributeFor(byte.class);
      xStream.useAttributeFor(char.class);
      xStream.useAttributeFor(double.class);
      xStream.useAttributeFor(float.class);
      xStream.useAttributeFor(int.class);
      xStream.useAttributeFor(long.class);
      xStream.useAttributeFor(Boolean.class);
      xStream.useAttributeFor(Byte.class);
      xStream.useAttributeFor(Character.class);
      xStream.useAttributeFor(Double.class);
      xStream.useAttributeFor(Float.class);
      xStream.useAttributeFor(Integer.class);
      xStream.useAttributeFor(Long.class);
      xStream.useAttributeFor(String.class);

      xStream.alias("java-type", Class.class);
      xStream.alias("constraintTarget", ConstraintTarget.class);

      // constraint check short forms
      xStream.alias("assert", AssertCheck.class);
      xStream.alias("assertConstraintSet", AssertConstraintSetCheck.class);
      xStream.alias("assertFalse", AssertFalseCheck.class);
      xStream.alias("assertFieldConstraints", AssertFieldConstraintsCheck.class);
      xStream.alias("assertNull", AssertNullCheck.class);
      xStream.alias("assertTrue", AssertTrueCheck.class);
      {
         xStream.alias("assertURL", AssertURLCheck.class);
         xStream.alias("permittedURIScheme", URIScheme.class);
         xStream.addImplicitCollection(AssertURLCheck.class, "permittedURISchemes", URIScheme.class);
      }
      xStream.alias("assertValid", AssertValidCheck.class);
      xStream.alias("checkWith", CheckWithCheck.class);
      xStream.alias("dateRange", DateRangeCheck.class);
      xStream.alias("digits", DigitsCheck.class);
      xStream.alias("email", EmailCheck.class);
      xStream.alias("equalToField", EqualToFieldCheck.class);
      xStream.alias("future", FutureCheck.class);
      xStream.alias("hasSubstring", HasSubstringCheck.class);
      xStream.alias("instanceOf", InstanceOfCheck.class);
      xStream.alias("instanceOfAny", InstanceOfAnyCheck.class);
      xStream.alias("length", LengthCheck.class);
      {
         xStream.alias("matchPattern", MatchPatternCheck.class);
         xStream.alias("pattern", Pattern.class);
         xStream.addImplicitCollection(MatchPatternCheck.class, "patterns", Pattern.class);
      }
      xStream.alias("max", MaxCheck.class);
      xStream.alias("maxLength", MaxLengthCheck.class);
      xStream.alias("maxSize", MaxSizeCheck.class);
      xStream.alias("memberOf", MemberOfCheck.class);
      xStream.alias("min", MinCheck.class);
      xStream.alias("minLength", MinLengthCheck.class);
      xStream.alias("minSize", MinSizeCheck.class);
      xStream.alias("noSelfReference", NoSelfReferenceCheck.class);
      xStream.alias("notBlank", NotBlankCheck.class);
      xStream.alias("notEmpty", NotEmptyCheck.class);
      xStream.alias("notEqual", NotEqualCheck.class);
      xStream.alias("notEqualToField", NotEqualToFieldCheck.class);
      {
         xStream.alias("notMatchPattern", NotMatchPatternCheck.class);
         xStream.addImplicitCollection(NotMatchPatternCheck.class, "patterns", Pattern.class);
      }
      xStream.alias("notMemberOf", NotMemberOfCheck.class);
      xStream.alias("notNegative", NotNegativeCheck.class);
      xStream.alias("notNull", NotNullCheck.class);
      xStream.alias("past", PastCheck.class);
      xStream.alias("range", RangeCheck.class);
      xStream.alias("simpleCheck", SimpleCheck.class);
      xStream.alias("size", SizeCheck.class);
      xStream.alias("validateWithMethod", ValidateWithMethodCheck.class);
      xStream.omitField(ValidateWithMethodCheck.class, "validationMethodsByClass");

      // check exclusions short forms
      xStream.alias("nullable", NullableExclusion.class);

      // <oval> -> net.sf.oval.configuration.POJOConfigurer
      xStream.alias("oval", POJOConfigurer.class);
      {
         // <constraintSet> -> net.sf.oval.configuration.elements.ConstraintSetConfiguration
         xStream.addImplicitCollection(POJOConfigurer.class, "constraintSetConfigurations", ConstraintSetConfiguration.class);
         xStream.alias("constraintSet", ConstraintSetConfiguration.class);
         xStream.addImplicitCollection(ConstraintSetConfiguration.class, "checks");

         // <class> -> net.sf.oval.configuration.elements.ClassConfiguration
         xStream.addImplicitCollection(POJOConfigurer.class, "classConfigurations", ClassConfiguration.class);
         xStream.alias("class", ClassConfiguration.class);
         {
            // <object> -> net.sf.oval.configuration.elements.ObjectConfiguration
            xStream.aliasField("object", ClassConfiguration.class, "objectConfiguration");
            {
               xStream.addImplicitCollection(ObjectConfiguration.class, "checks");
            }
            // <field> -> net.sf.oval.configuration.elements.FieldConfiguration
            xStream.addImplicitCollection(ClassConfiguration.class, "fieldConfigurations", FieldConfiguration.class);
            xStream.alias("field", FieldConfiguration.class);
            xStream.addImplicitCollection(FieldConfiguration.class, "checks");

            // <parameter> -> net.sf.oval.configuration.elements.ParameterConfiguration
            // used within ConstructorConfiguration and MethodConfiguration
            xStream.alias("parameter", ParameterConfiguration.class);
            xStream.addImplicitCollection(ParameterConfiguration.class, "checks", Check.class);
            xStream.addImplicitCollection(ParameterConfiguration.class, "checkExclusions", CheckExclusion.class);

            // <constructor> -> net.sf.oval.configuration.elements.ConstructorConfiguration
            xStream.addImplicitCollection(ClassConfiguration.class, "constructorConfigurations", ConstructorConfiguration.class);
            xStream.alias("constructor", ConstructorConfiguration.class);
            {
               // <parameter> -> net.sf.oval.configuration.elements.ParameterConfiguration
               xStream.addImplicitCollection(ConstructorConfiguration.class, "parameterConfigurations", ParameterConfiguration.class);
            }

            // <method> -> net.sf.oval.configuration.elements.MethodConfiguration
            xStream.addImplicitCollection(ClassConfiguration.class, "methodConfigurations", MethodConfiguration.class);
            xStream.alias("method", MethodConfiguration.class);
            {
               // <parameter> -> net.sf.oval.configuration.elements.ParameterConfiguration
               xStream.addImplicitCollection(MethodConfiguration.class, "parameterConfigurations", ParameterConfiguration.class);

               // <returnValue> -> net.sf.oval.configuration.elements.MethodConfiguration.returnValueConfiguration
               // -> MethodReturnValueConfiguration
               xStream.aliasField("returnValue", MethodConfiguration.class, "returnValueConfiguration");
               xStream.addImplicitCollection(MethodReturnValueConfiguration.class, "checks", Check.class);

               // <pre> -> net.sf.oval.configuration.elements.MethodConfiguration.preExecutionConfiguration ->
               // MethodPreExecutionConfiguration
               xStream.aliasField("preExecution", MethodConfiguration.class, "preExecutionConfiguration");
               xStream.addImplicitCollection(MethodPreExecutionConfiguration.class, "checks", PreCheck.class);
               xStream.alias("pre", PreCheck.class);

               // <post> -> net.sf.oval.configuration.elements.MethodConfiguration.postExecutionConfiguration ->
               // MethodPostExecutionConfiguration
               xStream.aliasField("postExecution", MethodConfiguration.class, "postExecutionConfiguration");
               xStream.addImplicitCollection(MethodPostExecutionConfiguration.class, "checks", PostCheck.class);
               xStream.alias("post", PostCheck.class);
            }
         }
      }
      return xStream;
   }

   protected static void onCheckInitialized(final Check check) {
      final Collection<CheckInitializationListener> listeners = CURRENT_LISTENERS.get();
      if (listeners != null) {
         for (final CheckInitializationListener listener : listeners) {
            listener.onCheckInitialized(check);
         }
      }
   }

   protected final Set<CheckInitializationListener> listeners = new LinkedHashSet<>(2);
   private POJOConfigurer pojoConfigurer = new POJOConfigurer();
   private final XStream xStream;

   /**
    * creates an XMLConfigurer instance backed by a new XStream instance
    * using the com.thoughtworks.xstream.io.xml.StaxDriver for XML parsing
    * if the StAX API is available
    *
    * @see com.thoughtworks.xstream.io.xml.StaxDriver
    */
   public XMLConfigurer() {
      xStream = createXStream();
   }

   public XMLConfigurer(final File xmlConfigFile) {
      this();
      fromXML(xmlConfigFile);
   }

   public XMLConfigurer(final InputStream xmlConfigStream) {
      this();
      fromXML(xmlConfigStream);
   }

   public XMLConfigurer(final Reader xmlConfigReader) {
      this();
      fromXML(xmlConfigReader);
   }

   public XMLConfigurer(final String xmlConfigAsString) {
      this();
      fromXML(xmlConfigAsString);
   }

   public XMLConfigurer(final XStream xStream) {
      this.xStream = xStream == null ? createXStream() : xStream;
   }

   public XMLConfigurer(final XStream xStream, final File xmlConfigFile) {
      this(xStream);
      fromXML(xmlConfigFile);
   }

   public XMLConfigurer(final XStream xStream, final InputStream xmlConfigStream) {
      this(xStream);
      fromXML(xmlConfigStream);
   }

   public XMLConfigurer(final XStream xStream, final Reader xmlConfigReader) {
      this(xStream);
      fromXML(xmlConfigReader);
   }

   public XMLConfigurer(final XStream xStream, final String xmlConfigAsString) {
      this(xStream);
      fromXML(xmlConfigAsString);
   }

   public boolean addCheckInitializationListener(final CheckInitializationListener listener) {
      Assert.argumentNotNull("listener", listener);
      return listeners.add(listener);
   }

   public void fromXML(final File input) {
      CURRENT_LISTENERS.set(listeners);
      try {
         pojoConfigurer = (POJOConfigurer) xStream.fromXML(input);
      } finally {
         CURRENT_LISTENERS.remove();
      }
   }

   public void fromXML(final InputStream input) {
      CURRENT_LISTENERS.set(listeners);
      try {
         pojoConfigurer = (POJOConfigurer) xStream.fromXML(input);
      } finally {
         CURRENT_LISTENERS.remove();
      }
   }

   public void fromXML(final Reader input) {
      CURRENT_LISTENERS.set(listeners);
      try {
         pojoConfigurer = (POJOConfigurer) xStream.fromXML(input);
      } finally {
         CURRENT_LISTENERS.remove();
      }
   }

   public void fromXML(final String input) {
      CURRENT_LISTENERS.set(listeners);
      try {
         pojoConfigurer = (POJOConfigurer) xStream.fromXML(input);
      } finally {
         CURRENT_LISTENERS.remove();
      }
   }

   @Override
   public ClassConfiguration getClassConfiguration(final Class<?> clazz) throws InvalidConfigurationException {
      return pojoConfigurer.getClassConfiguration(clazz);
   }

   @Override
   public ConstraintSetConfiguration getConstraintSetConfiguration(final String constraintSetId) throws InvalidConfigurationException {
      return pojoConfigurer.getConstraintSetConfiguration(constraintSetId);
   }

   public POJOConfigurer getPojoConfigurer() {
      return pojoConfigurer;
   }

   public XStream getXStream() {
      return xStream;
   }

   public boolean removeCheckInitializationListener(final CheckInitializationListener listener) {
      return listeners.remove(listener);
   }

   public void setPojoConfigurer(final POJOConfigurer pojoConfigurer) {
      this.pojoConfigurer = pojoConfigurer;
   }

   public synchronized String toXML() {
      return xStream.toXML(pojoConfigurer);
   }

   public synchronized void toXML(final OutputStream out) {
      xStream.toXML(pojoConfigurer, out);
   }

   public synchronized void toXML(final Writer out) {
      xStream.toXML(pojoConfigurer, out);
   }
}
