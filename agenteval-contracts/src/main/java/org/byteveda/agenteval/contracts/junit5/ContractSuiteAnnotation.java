package org.byteveda.agenteval.contracts.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation that loads contracts from a JSON definition file.
 *
 * <pre>{@code
 * @ContractSuiteAnnotation("contracts/safety-suite.json")
 * class SafetyContractTests {
 *
 *     @ContractTest
 *     void testSafety(AgentTestCase testCase) {
 *         testCase.setActualOutput(agent.respond(testCase.getInput()));
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ContractEvalExtension.class)
public @interface ContractSuiteAnnotation {

    /**
     * Classpath resource path to the contract definition file (JSON).
     */
    String value();

    /**
     * Optional suite name for reporting.
     */
    String name() default "";
}
