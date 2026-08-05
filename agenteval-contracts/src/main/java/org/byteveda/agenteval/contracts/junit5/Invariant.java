package org.byteveda.agenteval.contracts.junit5;

import org.byteveda.agenteval.contracts.Contract;
import org.byteveda.agenteval.contracts.ContractSeverity;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a contract invariant to verify on a {@code @ContractTest} method.
 *
 * <pre>{@code
 * @ContractTest
 * @Invariant(NoSystemPromptLeakContract.class)
 * @Invariant(value = MaxToolCallsContract.class, severity = ContractSeverity.WARNING)
 * void testContracts(AgentTestCase testCase) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Invariants.class)
@ExtendWith(ContractEvalExtension.class)
public @interface Invariant {

    /**
     * The contract class to instantiate and verify.
     */
    Class<? extends Contract> value();

    /**
     * Override the contract's default severity.
     * Use {@link ContractSeverity#ERROR} as the default.
     */
    ContractSeverity severity() default ContractSeverity.ERROR;
}
