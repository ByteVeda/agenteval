package org.byteveda.agenteval.contracts.junit5;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for contract test methods.
 *
 * <pre>{@code
 * @ContractTest
 * @Invariant(NoSystemPromptLeakContract.class)
 * void testSafety(AgentTestCase testCase) {
 *     testCase.setActualOutput(agent.respond(testCase.getInput()));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("contract")
@ExtendWith(ContractEvalExtension.class)
public @interface ContractTest {
}
