/**
 * Copyright (c) 2012 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	itemis AG - initial API and implementation
 * 
 */
package org.yakindu.sct.model.stext.test;

import static org.eclipse.xtext.junit4.validation.AssertableDiagnostics.errorCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.yakindu.sct.test.models.AbstractTestModelsUtil.VALIDATION_TESTMODEL_DIR;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.validation.AssertableDiagnostics;
import org.eclipse.xtext.junit4.validation.ValidatorTester;
import org.eclipse.xtext.validation.Check;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.yakindu.base.expressions.expressions.Expression;
import org.yakindu.sct.model.sgraph.Choice;
import org.yakindu.sct.model.sgraph.Entry;
import org.yakindu.sct.model.sgraph.Exit;
import org.yakindu.sct.model.sgraph.Region;
import org.yakindu.sct.model.sgraph.SGraphFactory;
import org.yakindu.sct.model.sgraph.Scope;
import org.yakindu.sct.model.sgraph.State;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.model.sgraph.Transition;
import org.yakindu.sct.model.sgraph.Trigger;
import org.yakindu.sct.model.sgraph.Vertex;
import org.yakindu.sct.model.stext.inferrer.STextTypeInferrer;
import org.yakindu.sct.model.stext.resource.StextResource;
import org.yakindu.sct.model.stext.stext.ImportScope;
import org.yakindu.sct.model.stext.stext.InterfaceScope;
import org.yakindu.sct.model.stext.stext.InternalScope;
import org.yakindu.sct.model.stext.stext.ReactionEffect;
import org.yakindu.sct.model.stext.stext.ReactionTrigger;
import org.yakindu.sct.model.stext.stext.StatechartSpecification;
import org.yakindu.sct.model.stext.stext.TransitionSpecification;
import org.yakindu.sct.model.stext.stext.impl.StextFactoryImpl;
import org.yakindu.sct.model.stext.test.util.AbstractSTextTest;
import org.yakindu.sct.model.stext.test.util.STextInjectorProvider;
import org.yakindu.sct.model.stext.validation.STextJavaValidator;
import org.yakindu.sct.model.stext.validation.STextValidationMessages;
import org.yakindu.sct.test.models.AbstractTestModelsUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author andreas muelder - Initial contribution and API
 * 
 */
@RunWith(XtextRunner.class)
@InjectWith(STextInjectorProvider.class)
public class STextJavaValidatorTest extends AbstractSTextTest implements STextValidationMessages {

	@Inject
	private STextJavaValidator validator;
	@Inject
	private Injector injector;
	private ValidatorTester<STextJavaValidator> tester;
	protected BasicDiagnostic diagnostics;
	protected SGraphFactory factory;
	protected Statechart statechart;
	protected StextResource resource;

	public STextJavaValidatorTest() {
	}

	@Before
	public void setup() {
		factory = SGraphFactory.eINSTANCE;

		resource = new StextResource(URI.createURI(""));
		injector.injectMembers(resource);
		statechart = factory.createStatechart();
		resource.getContents().add(statechart);

		diagnostics = new BasicDiagnostic();
		tester = new ValidatorTester<STextJavaValidator>(validator, injector);
	}

	@After
	public void teardown() {
		tester = null;
	}

	/**
	 * @see STextJavaValidator#checkVariableDefinition(org.yakindu.sct.model.stext.stext.VariableDefinition)
	 */
	@Test
	public void checkVariableDefinition() {
		Scope context = (Scope) parseExpression("interface if : var i : void", null,
				InterfaceScope.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(context);
		validationResult.assertErrorContains(STextTypeInferrer.VARIABLE_VOID_TYPE);
	}

	/**
	 * @see STextJavaValidator#checkAssignmentExpression(org.yakindu.sct.model.stext.stext.AssignmentExpression)
	 */
	@Test
	public void checkAssignmentExpression() {

		Scope context = (Scope) parseExpression("interface: var i : integer = 42 var j : integer =23", null,
				InterfaceScope.class.getSimpleName());

		EObject expression = super.parseExpression("i += (i+=3) +4", context, Expression.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(expression);
		validationResult.assertErrorContains(STextJavaValidator.ASSIGNMENT_EXPRESSION);

		expression = super.parseExpression("i += (j+=3) +4", context, Expression.class.getSimpleName());
		validationResult = tester.validate(expression);
		validationResult.assertOK();
	}

	@Test
	public void checkTimeEventSpecValueExpression() {
		EObject expression = super.parseExpression("after true s", ReactionTrigger.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(expression);
		validationResult.assertErrorContains(STextJavaValidator.TIME_EXPRESSION);
	}

	@Test
	public void checkReactionEffectActionExpression() {
		// covered by inferrer tests
	}

	@Test
	public void checkLeftHandAssignment() {

		Scope scope = (Scope) parseExpression(
				"interface if : operation myOperation() : boolean event Event1 : boolean var myVar : boolean", null,
				InterfaceScope.class.getSimpleName());

		EObject model = super.parseExpression("3 = 3", Expression.class.getSimpleName(), scope);
		AssertableDiagnostics validationResult = tester.validate(model);
		validationResult.assertErrorContains(STextJavaValidator.LEFT_HAND_ASSIGNMENT);

		// Check for referenced elements in interface
		model = super.parseExpression("if.myOperation() = true", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertErrorContains(STextJavaValidator.LEFT_HAND_ASSIGNMENT);

		model = super.parseExpression("if.Event1 = true", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertErrorContains(STextJavaValidator.LEFT_HAND_ASSIGNMENT);

		model = super.parseExpression("if.myVar = true", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertOK();

		// check for internal referenced elements
		scope = (Scope) parseExpression(
				"internal : operation myOperation() : integer event Event1 : integer var myVar : integer", null,
				InternalScope.class.getSimpleName());

		model = super.parseExpression("myOperation() = 5", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertErrorContains(STextJavaValidator.LEFT_HAND_ASSIGNMENT);

		model = super.parseExpression("Event1 = true", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertErrorContains(STextJavaValidator.LEFT_HAND_ASSIGNMENT);

		model = super.parseExpression("myVar = 5", Expression.class.getSimpleName(), scope);
		validationResult = tester.validate(model);
		validationResult.assertOK();

	}

	/**
	 * @see STextJavaValidator#checkOperationArguments_FeatureCall(org.yakindu.sct.model.stext.stext.FeatureCall)
	 */
	@Test
	public void checkOperationArguments_FeatureCall() {
		Scope scope = (Scope) parseExpression(
				"interface if : operation myOperation(param1 : integer, param2: boolean)", null,
				InterfaceScope.class.getSimpleName());
		EObject model = super.parseExpression("if.myOperation(5,true)", Expression.class.getSimpleName(), scope);
		AssertableDiagnostics validationResult = tester.validate(model);
		validationResult.assertOK();
	}

	/**
	 * @see STextJavaValidator#checkOperationArguments_TypedElementReferenceExpression(TypedElementReferenceExpression)
	 */
	@Test
	public void checkOperationArguments_TypedElementReferenceExpression() {
		Scope context = createInternalScope("internal: operation myOperation(param1 : integer, param2: boolean)");
		EObject model = super.parseExpression("myOperation(5,true)", context, Expression.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(model);
		validationResult.assertOK();
	}

	/**
	 * @see STextJavaValidator#checkGuardHasBooleanExpression(org.yakindu.sct.model.stext.stext.ReactionTrigger)
	 */
	@Test
	public void checkGuard() {
		EObject expression = super.parseExpression("[3 * 3]", ReactionTrigger.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(expression);
		validationResult.assertErrorContains(STextJavaValidator.GUARD_EXPRESSION);

		Scope context = createInternalScope("internal: var myInt : integer var myBool : boolean = true");
		expression = super.parseExpression("[myInt = 5]", context, ReactionTrigger.class.getSimpleName());
		validationResult = tester.validate(expression);
		validationResult.assertErrorContains(STextJavaValidator.GUARD_EXPRESSION);

		expression = super.parseExpression("[myInt <= 5 || !myBool ]", context, ReactionTrigger.class.getSimpleName());
		validationResult = tester.validate(expression);
		validationResult.assertOK();

	}

	/**
	 * @see STextJavaValidator#checkFeatureCall(org.yakindu.sct.model.stext.stext.FeatureCall)
	 * @see STextJavaValidator#checkFeatureCall(TypedElementReferenceExpression)
	 */
	@Test
	public void checkFeatureCall() {
		Scope context = (Scope) parseExpression("interface if : in event a : integer", null,
				InterfaceScope.class.getSimpleName());
		EObject model = super.parseExpression("if.a / raise if.a:1", context,
				TransitionSpecification.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(model);
		validationResult.assertOK();
	}

	/**
	 * 
	 * @see STextJavaValidator#checkReactionTrigger(org.yakindu.sct.model.stext.stext.ReactionTrigger)
	 */
	@Test
	public void checkReactionTrigger() {
		// ENTRY, EXIT not allowed in transitions
		Scope context = (Scope) parseExpression("internal : event a : integer var myVar : integer", null,
				InternalScope.class.getSimpleName());
		EObject model = super.parseExpression("entry / myVar = 5", context,
				TransitionSpecification.class.getSimpleName());
		AssertableDiagnostics validationResult = tester.validate(model);
		validationResult.assertError(LOCAL_REACTIONS_NOT_ALLOWED);

		model = super.parseExpression("exit / myVar = 5", context, TransitionSpecification.class.getSimpleName());
		validationResult = tester.validate(model);
		validationResult.assertError(LOCAL_REACTIONS_NOT_ALLOWED);

		model = super.parseExpression("oncycle / myVar = 5", context, TransitionSpecification.class.getSimpleName());
		validationResult = tester.validate(model);
		validationResult.assertOK();

		model = super.parseExpression("always / myVar = 5", context, TransitionSpecification.class.getSimpleName());
		validationResult = tester.validate(model);
		validationResult.assertOK();
	}

	/**
	 * @see STextJavaValidator#checkReactionEffectActions(org.yakindu.sct.model.stext.stext.ReactionEffect)
	 */
	@Test
	public void checkReactionEffectActions() {
		Scope s1 = (InternalScope) parseExpression("internal : var a : integer event e operation o () : void", null,
				InternalScope.class.getSimpleName());
		Scope s2 = (InterfaceScope) parseExpression("interface if : var a : integer in event e operation o()", null,
				InterfaceScope.class.getSimpleName());

		EObject model = super.parseExpression("a", s1, ReactionEffect.class.getSimpleName());
		AssertableDiagnostics result = tester.validate(model);
		result.assertError(FEATURE_CALL_HAS_NO_EFFECT);

		model = super.parseExpression("1+3", s1, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertError(FEATURE_CALL_HAS_NO_EFFECT);

		model = super.parseExpression("valueof(e)", s1, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertError(FEATURE_CALL_HAS_NO_EFFECT);

		model = super.parseExpression("o()", s1, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertOK();

		model = super.parseExpression("if.a", s2, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertError(FEATURE_CALL_HAS_NO_EFFECT);

		model = super.parseExpression("valueof(if.e)", s2, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertError(FEATURE_CALL_HAS_NO_EFFECT);

		model = super.parseExpression("if.o", s2, ReactionEffect.class.getSimpleName());
		result = tester.validate(model);
		result.assertOK();

	}

	/**
	 * @see STextJavaValidator#checkEventDefinition(org.yakindu.sct.model.stext.stext.EventDefinition)
	 */
	@Test
	public void checkEventDefinition() {
		// No local declarations in interface scope
		EObject model = super.parseExpression("interface MyInterface: event Event1", null,
				InterfaceScope.class.getSimpleName());
		AssertableDiagnostics result = tester.validate(model);
		result.assertErrorContains(LOCAL_DECLARATIONS);
		// No in declarations in internal scope
		model = super.parseExpression("internal: in event Event1", null, InternalScope.class.getSimpleName());
		result = tester.validate(model);
		result.assertDiagnosticsCount(1);		
		result.assertErrorContains(STextJavaValidator.IN_OUT_DECLARATIONS);
		// No out declarations in internal scope
		model = super.parseExpression("internal: out event Event1", null, InternalScope.class.getSimpleName());
		result = tester.validate(model);
		result.assertDiagnosticsCount(1);
		result.assertErrorContains(IN_OUT_DECLARATIONS);
	}

	/**
	 * @see STextJavaValidator#checkInterfaceScope(Statechart)
	 */
	@Test
	public void checkInterfaceScope() {
		EObject model = super.parseExpression("interface: in event event1 interface: in event event2", null,
				StatechartSpecification.class.getSimpleName());
		AssertableDiagnostics result = tester.validate(model);
		result.assertDiagnosticsCount(2);
		result.assertAll(errorCode(ONLY_ONE_INTERFACE), errorCode(ONLY_ONE_INTERFACE));
	}

	/**
	 * @see STextJavaValidator#checkChoiceWithoutDefaultTransition(org.yakindu.sct.model.sgraph.Choice)
	 */
	@Test
	public void checkChoiceWithoutDefaultTransition() {
		// TODO
	}

	/**
	 * @see STextJavaValidator#checkUnresolvableProxies(Statechart)
	 */
	@Test
	public void checkUnresolvableProxies() {
		// Nothing to do
	}

	/**
	 * @see STextJavaValidator#checkcheckSyntaxErrors(Statechart)
	 */
	@Test
	public void checkSyntaxErrors() {
		// Nothing to do
	}
	
	@Test
	public void checkExpression() {
		// Nothing to do
	} 

	/**
	 * checks tht each @Check method of {@link STextJavaValidator} has a @Test
	 * method in this class with the same name
	 */
	@Test
	public void testAllChecksHaveTests() throws Exception {
		Iterable<Method> methods = Lists.newArrayList(STextJavaValidator.class.getDeclaredMethods());
		methods = Iterables.filter(methods, new Predicate<Method>() {
			public boolean apply(Method input) {
				return input.getAnnotation(Check.class) != null;
			}
		});
		for (Method checkMethod : methods) {
			Method testMethod = getClass().getMethod(checkMethod.getName());
			assertNotNull("Missing @Test Annotation for method " + checkMethod.getName(),
					testMethod.getAnnotation(Test.class));
		}
	}

	@Test
	public void checkUnusedEntry() {
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "UnusedEntryPoint.sct");
		Iterator<EObject> iter = statechart.eAllContents();
		while (iter.hasNext()) {
			EObject element = iter.next();
			if (element instanceof Entry) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}

		assertIssueCount(diagnostics, 1);
		assertWarning(diagnostics, ENTRY_UNUSED);
	}
	@Test
	public void checkTopLeveEntryIsDefaultEntry(){
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "TopLevelEntryIsDefaultEntryError.sct");
		doValidateAllContents(Entry.class);
		
		assertIssueCount(diagnostics, 1);
		assertError(diagnostics, TOP_LEVEL_REGION_ENTRY_HAVE_TO_BE_A_DEFAULT_ENTRY);
		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "TopLevelEntryIsDefaultEntryWarn.sct");
		doValidateAllContents(Entry.class);
		
		assertIssueCount(diagnostics, 1);
		assertWarning(diagnostics, TOP_LEVEL_REGION_ENTRY_HAVE_TO_BE_A_DEFAULT_ENTRY);
	}

	@Test
	public void checkUnusedExit() {
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "UnusedExitPoint.sct");
		doValidateAllContents(Exit.class);

		assertIssueCount(diagnostics, 1);
		assertError(diagnostics, EXIT_UNUSED);

		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "UnusedDefaultExitPoint.sct");
		doValidateAllContents(Exit.class);

		assertIssueCount(diagnostics, 1);
		assertError(diagnostics, EXIT_DEFAULT_UNUSED);
	}

	protected void doValidateAllContents(Class<? extends EObject> clazz) {
		Iterator<EObject> iter = statechart.eAllContents();
		while (iter.hasNext()) {
			EObject element = iter.next();
			if (clazz.isInstance(element)) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}
	}
	protected void assertAllTransitionsAreValid(Class<? extends EObject> clazz) {
		Iterator<EObject> iter;
		iter = statechart.eAllContents();

		while (iter.hasNext()) {
			EObject element = iter.next();
			if (clazz.isInstance(element)) {
				assertTrue(validator.validate(element, diagnostics, new HashMap<Object, Object>()));
			}
		}
	}

	@Test
	public void checkTransitionPropertySpec() {
		// Test source state isn't composite
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "TransitionEntrySpecNotComposite.sct");
		doValidateAllContents(Transition.class);
		// Test target state isn't composite
		assertIssueCount(diagnostics, 2);
		assertWarning(diagnostics, TRANSITION_ENTRY_SPEC_NOT_COMPOSITE);

		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "TransitionExitSpecNotComposite.sct");
		assertAllTransitionsAreValid(Transition.class);

		assertIssueCount(diagnostics, 1);
		assertWarning(diagnostics, TRANSITION_EXIT_SPEC_NOT_COMPOSITE);

		// Test exit spec is used on multiple transition siblings.
		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "TransitionExitSpecOnMultipleSiblings.sct");
		assertAllTransitionsAreValid(Transition.class);

		assertIssueCount(diagnostics, 4);
		assertWarning(diagnostics, TRANSITION_EXIT_SPEC_ON_MULTIPLE_SIBLINGS);

		// Test transition unbound named exit point spec.
		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "TransitionNotExistingNamedExitPoint.sct");
		doValidateAllContents(Transition.class);

		assertIssueCount(diagnostics, 1);
		assertError(diagnostics, TRANSITION_NOT_EXISTING_NAMED_EXIT_POINT);
	}



	@Test
	public void checkUnboundEntryPoints() {
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "UnboundDefaultEntryPoints.sct");
		Iterator<EObject> iter = statechart.eAllContents();

		// create and add triggers to all transitions to prevent to trigger
		// additional warnings
		// (see Check in SGrapJavaValidator transitionsWithNoGuard)
		Trigger trigger = StextFactoryImpl.init().createDefaultTrigger();

		while (iter.hasNext()) {
			EObject element = iter.next();
			if (element instanceof Transition) {
				((Transition) element).setTrigger(trigger);
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
			if (element instanceof State) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}

		assertIssueCount(diagnostics, 4);
		assertError(diagnostics, TRANSITION_UNBOUND_DEFAULT_ENTRY_POINT);
		assertError(diagnostics, REGION_UNBOUND_DEFAULT_ENTRY_POINT);

		resetDiagnostics();
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR + "UnboundEntryPoints02.sct");
		iter = statechart.eAllContents();

		while (iter.hasNext()) {
			EObject element = iter.next();
			if (element instanceof Transition) {
				((Transition) element).setTrigger(trigger);
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
			if (element instanceof State) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}
		assertIssueCount(diagnostics, 4);
	}

	@Test
	public void checkExitPointSpecWithTrigger() {
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "NoTriggerOnTransitionWithExitPointSpec.sct");
		Iterator<EObject> iter = statechart.eAllContents();
		while (iter.hasNext()) {
			EObject element = iter.next();
			if (element instanceof Transition) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}

		assertIssueCount(diagnostics, 2);
		assertError(diagnostics, EXITPOINTSPEC_WITH_TRIGGER);
	}

	@Test
	public void checkAssignmentToFinalVariable() {
		Statechart statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "AssignmentToValue.sct");
		Diagnostic diagnostics = Diagnostician.INSTANCE.validate(statechart);
		assertIssueCount(diagnostics, 2);
		assertError(diagnostics, ASSIGNMENT_TO_VALUE);
	}

	@Test
	public void checkValueDefinitionExpression() {
		Statechart statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "ConstWithVariable.sct");
		Diagnostic diagnostics = Diagnostician.INSTANCE.validate(statechart);
		assertIssueCount(diagnostics, 2); //
		assertError(diagnostics, REFERENCE_TO_VARIABLE);
	}

	@Test
	public void checkValueReferenedBeforeDefined() {
		Statechart statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "ReferenceBeforeDefined.sct");
		Diagnostic diagnostics = Diagnostician.INSTANCE.validate(statechart);
		assertIssueCount(diagnostics, 2);
		assertError(diagnostics, REFERENCE_CONSTANT_BEFORE_DEFINED);
	}
		
	@Test
	public void checkUnusedVariablesInInternalScope(){
		Statechart statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "UnusedInternalDeclarations.sct");
		Diagnostic diagnostics = Diagnostician.INSTANCE.validate(statechart);
		assertIssueCount(diagnostics, 3);
		assertWarning(diagnostics, INTERNAL_DECLARATION_UNUSED);
	}
	
	/**
	 * Show warning when transition has no guard
	 */
	@Test
	public void transitionsWithNoTrigger() {

		Region region = factory.createRegion();

		// create vertices for main region
		Entry entry = factory.createEntry();
		State a = factory.createState();
		a.setName("A");
		State b = factory.createState();
		b.setName("B");
		State c = factory.createState();
		c.setName("C");
		State d = factory.createState();
		c.setName("D");
		Choice e = factory.createChoice();
		State f = factory.createState();
		f.setName("F");
		Transition entryToA = createTransition(entry, a);
		Transition aToB = createTransition(a, b);
		Transition bToC = createTransition(b, c);
		Transition cToD = createTransition(c, d);
		Transition eToF = createTransition(e, f);

		// create vertices for compositState
		State bb = factory.createState();
		bb.setName("BB");
		Entry entryB = factory.createEntry();
		Exit exitB = factory.createExit();

		Region b_region = factory.createRegion();
		b_region.getVertices().add(entryB);
		b_region.getVertices().add(bb);
		b_region.getVertices().add(exitB);
		b.getRegions().add(b_region);
		Transition entryBToBB = createTransition(entryB, bb);
		Transition bbToExitB = createTransition(bb, exitB);

		region.getVertices().add(entry);
		region.getVertices().add(a);
		region.getVertices().add(b);
		region.getVertices().add(c);
		region.getVertices().add(d);
		statechart.getRegions().add(region);

		// transitions from entry point to State A -> valid model with no
		// warnings
		assertTrue(validator.validate(eToF, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 0);

		// transitions from entry point to State A -> valid model with no
		// warnings
		assertTrue(validator.validate(entryToA, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 0);

		// transition from StateA to StateB -> valid model with warnings expect
		// 1 warning in total
		assertTrue(validator.validate(aToB, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 1);
		resetDiagnostics();

		// transition from EntryB to StateBB -> valid model with no warnings
		// expect 1 warning in total
		assertTrue(validator.validate(entryBToBB, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 0);

		// transition from BB to ExitB -> valid model with warnings expect 2
		// warning in total
		assertTrue(validator.validate(bbToExitB, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 1);
		resetDiagnostics();

		// transition from B to C -> valid model with no warning warnings expect
		// 2 warning in total
		assertTrue(validator.validate(bToC, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 0);

		// transition from C to D -> valid model with warning warning expect 3
		// warning in total
		assertTrue(validator.validate(cToD, diagnostics, new HashMap<Object, Object>()));
		assertIssueCount(diagnostics, 1);
		resetDiagnostics();

	}

	/**
	 * The outgoing transitions of a sync (fork / join) node must not have any
	 * trigger part. Thus empty transitions must not have any warning. This test
	 * case addresses a bug #75 (
	 * https://code.google.com/a/eclipselabs.org/p/yakindu/issues/detail?id=75 )
	 * .
	 */
	@Test
	public void validEmptyTransitionFromSync() {
		statechart = AbstractTestModelsUtil.loadStatechart(VALIDATION_TESTMODEL_DIR
				+ "ValidEmptyTransitionFromSync.sct");
		Iterator<EObject> iter = statechart.eAllContents();
		while (iter.hasNext()) {
			EObject element = iter.next();
			if (element instanceof Transition) {
				validator.validate(element, diagnostics, new HashMap<Object, Object>());
			}
		}

		assertIssueCount(diagnostics, 0);
	}

	@Test
	public void checkImportExists() {
		ImportScope importScope = (ImportScope) parseExpression("import: not.existing.*", null,
				ImportScope.class.getSimpleName());

		AssertableDiagnostics validationResult = tester.validate(importScope.getImports().get(0));
		validationResult.assertError(STextJavaValidator.IMPORT_NOT_RESOLVED);
	}

	protected Transition createTransition(Vertex source, Vertex target) {
		Transition trans = SGraphFactory.eINSTANCE.createTransition();
		trans.setSource(source);
		trans.setTarget(target);
		source.getOutgoingTransitions().add(trans);
		target.getIncomingTransitions().add(trans);
		return trans;
	}

	protected void assertError(Diagnostic diag, String message) {
		Diagnostic d = issueByName(diag, message);
		assertNotNull("Issue '" + message + "' does not exist.", issueByName(diag, message));
		assertEquals("Issue '" + message + "' is no error.", Diagnostic.ERROR, d.getSeverity());
	}

	protected void assertWarning(Diagnostic diag, String message) {
		Diagnostic d = issueByName(diag, message);
		assertNotNull("Issue '" + message + "' does not exist.", issueByName(diag, message));
		assertEquals("Issue '" + message + "' is no warning.", Diagnostic.WARNING, d.getSeverity());
	}

	protected void assertIssueCount(Diagnostic diag, int count) {
		int c = diag.getChildren().size();
		assertEquals("expected " + count + " issue(s) but were " + c + " [" + diag.toString() + "]", count, c);
	}

	protected Diagnostic issueByName(Diagnostic diag, String message) {
		for (Diagnostic issue : diag.getChildren()) {
			if (message.equals(issue.getMessage()))
				return issue;
		}
		return null;
	}

	protected void resetDiagnostics() {
		diagnostics = new BasicDiagnostic();
	}
}
