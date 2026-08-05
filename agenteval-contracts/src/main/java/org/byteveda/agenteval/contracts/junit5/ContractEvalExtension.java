package org.byteveda.agenteval.contracts.junit5;

import org.byteveda.agenteval.contracts.Contract;
import org.byteveda.agenteval.contracts.ContractDefinitionLoader;
import org.byteveda.agenteval.contracts.ContractSeverity;
import org.byteveda.agenteval.contracts.ContractVerdict;
import org.byteveda.agenteval.contracts.ContractViolation;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 extension that verifies {@link Contract} invariants after each test method.
 *
 * <p>Handles {@code @ContractTest}, {@code @Invariant}, and {@code @ContractSuiteAnnotation}
 * annotations. Captures the {@link AgentTestCase} from the test method and checks all
 * declared contracts after execution.</p>
 */
public class ContractEvalExtension
        implements ParameterResolver, InvocationInterceptor, AfterEachCallback {

    private static final Logger LOG = LoggerFactory.getLogger(ContractEvalExtension.class);
    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(ContractEvalExtension.class);
    private static final String TEST_CASE_KEY = "contractTestCase";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == AgentTestCase.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        AgentTestCase testCase = AgentTestCase.builder().input("").build();
        extensionContext.getStore(NS).put(TEST_CASE_KEY, testCase);
        return testCase;
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        invocation.proceed();

        // Capture first AgentTestCase argument from the method parameters
        for (Object arg : invocationContext.getArguments()) {
            if (arg instanceof AgentTestCase testCase) {
                extensionContext.getStore(NS).put(TEST_CASE_KEY, testCase);
                break;
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        List<Contract> contracts = resolveContracts(context);
        if (contracts.isEmpty()) {
            return;
        }

        AgentTestCase testCase = context.getStore(NS).get(TEST_CASE_KEY, AgentTestCase.class);
        if (testCase == null) {
            LOG.warn("No AgentTestCase found in extension context — skipping contract checks");
            return;
        }

        List<ContractViolation> allViolations = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (Contract contract : contracts) {
            ContractVerdict verdict = contract.check(testCase);
            if (!verdict.passed()) {
                for (ContractViolation v : verdict.violations()) {
                    if (v.severity() != ContractSeverity.WARNING) {
                        failures.add(formatViolation(v, testCase));
                    }
                    allViolations.add(v);
                }
                if (contract.severity() == ContractSeverity.CRITICAL) {
                    break;
                }
            }
        }

        // Log warnings
        for (ContractViolation v : allViolations) {
            if (v.severity() == ContractSeverity.WARNING) {
                LOG.warn("Contract warning [{}]: {}", v.contractName(), v.evidence());
            }
        }

        if (!failures.isEmpty()) {
            throw new ContractViolationError(
                    "Contract violations detected:\n  " + String.join("\n  ", failures),
                    allViolations);
        }
    }

    private List<Contract> resolveContracts(ExtensionContext context) {
        List<Contract> contracts = new ArrayList<>();

        // From @Invariant annotations on the method
        context.getTestMethod().ifPresent(method -> {
            Invariant[] invariants = method.getAnnotationsByType(Invariant.class);
            for (Invariant inv : invariants) {
                Contract contract = instantiateContract(inv.value());
                contracts.add(contract);
            }
        });

        // From @ContractSuiteAnnotation on the class
        context.getTestClass().ifPresent(cls -> {
            ContractSuiteAnnotation suiteAnn = cls.getAnnotation(ContractSuiteAnnotation.class);
            if (suiteAnn != null) {
                List<Contract> loaded = ContractDefinitionLoader.loadFromResource(
                        suiteAnn.value(), null);
                contracts.addAll(loaded);
            }
        });

        return contracts;
    }

    private Contract instantiateContract(Class<? extends Contract> contractClass) {
        try {
            Constructor<? extends Contract> ctor = contractClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new ContractViolationError(
                    "Failed to instantiate contract: " + contractClass.getSimpleName()
                            + ". Ensure it has a no-arg constructor.",
                    List.of());
        }
    }

    private static String formatViolation(ContractViolation v, AgentTestCase testCase) {
        return String.format("[%s] %s | input='%s' | evidence='%s'",
                v.severity(), v.contractName(),
                truncate(testCase.getInput(), 80),
                truncate(v.evidence(), 200));
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
