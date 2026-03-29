package org.byteveda.agenteval.reporting;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.EvalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Generates JUnit XML reports compatible with CI systems (Jenkins, GitHub Actions, GitLab CI).
 *
 * <p>Each (metric x test case) pair becomes a {@code <testcase>} element.
 * Failing metric scores map to {@code <failure>} elements.</p>
 */
public final class JunitXmlReporter implements EvalReporter {

    private static final Logger LOG = LoggerFactory.getLogger(JunitXmlReporter.class);

    private final Path outputPath;

    public JunitXmlReporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void report(EvalResult result) {
        try {
            String xml = generateXml(result);
            Files.writeString(outputPath, xml);
            LOG.info("JUnit XML report written to {}", outputPath);
        } catch (Exception e) {
            throw new ReportException("Failed to write JUnit XML report to " + outputPath, e);
        }
    }

    /**
     * Generates the JUnit XML string (visible for testing).
     */
    String generateXml(EvalResult result)
            throws ParserConfigurationException, TransformerException {
        var docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var doc = docBuilder.newDocument();

        var testsuite = doc.createElement("testsuite");
        testsuite.setAttribute("name", "AgentEval");
        testsuite.setAttribute("time",
                String.format(Locale.US, "%.3f", result.durationMs() / 1000.0));

        int tests = 0;
        int failures = 0;

        for (CaseResult cr : result.caseResults()) {
            String caseName = cr.testCase().getInput();
            for (Map.Entry<String, EvalScore> entry : cr.scores().entrySet()) {
                tests++;
                var testcase = doc.createElement("testcase");
                testcase.setAttribute("classname", entry.getKey());
                testcase.setAttribute("name", caseName);
                testcase.setAttribute("time", "0.000");

                EvalScore score = entry.getValue();
                if (!score.passed()) {
                    failures++;
                    var failure = doc.createElement("failure");
                    failure.setAttribute("message",
                            String.format(Locale.US, "%.3f < %.3f",
                                    score.value(), score.threshold()));
                    failure.setAttribute("type", "MetricFailure");
                    failure.setTextContent(score.reason());
                    testcase.appendChild(failure);
                }

                testsuite.appendChild(testcase);
            }
        }

        testsuite.setAttribute("tests", String.valueOf(tests));
        testsuite.setAttribute("failures", String.valueOf(failures));
        testsuite.setAttribute("errors", "0");
        doc.appendChild(testsuite);

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        var writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
