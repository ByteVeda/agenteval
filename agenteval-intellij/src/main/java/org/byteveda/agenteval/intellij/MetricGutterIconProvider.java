package org.byteveda.agenteval.intellij;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Shows pass/fail gutter icons on {@code @Metric} annotations by
 * cross-referencing with the latest report.
 */
public class MetricGutterIconProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiIdentifier)) {
            return null;
        }

        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiModifierListOwner owner)) {
            return null;
        }

        PsiAnnotation metricAnnotation = owner.getAnnotation("org.byteveda.agenteval.junit5.Metric");
        if (metricAnnotation == null) {
            return null;
        }

        // Extract metric name from annotation
        String metricName = getAnnotationValue(metricAnnotation);
        if (metricName == null) {
            return null;
        }

        // Check report for this metric
        Map<String, Boolean> metricStatus = loadMetricStatus(element.getProject());
        Boolean passed = metricStatus.get(metricName);
        if (passed == null) {
            return null;
        }

        Icon icon = passed ? AgentEvalIcons.PASS : AgentEvalIcons.FAIL;
        String tooltip = metricName + ": " + (passed ? "PASS" : "FAIL");

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                icon,
                e -> tooltip,
                null,
                GutterIconRenderer.Alignment.RIGHT,
                () -> tooltip);
    }

    private static String getAnnotationValue(PsiAnnotation annotation) {
        var value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue(null);
        }
        if (value == null) return null;
        String text = value.getText();
        // Strip quotes
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static Map<String, Boolean> loadMetricStatus(Project project) {
        if (project.getBasePath() == null) return Map.of();

        Path[] candidates = {
            Path.of(project.getBasePath(), "build", "agenteval", "agenteval-report.json"),
            Path.of(project.getBasePath(), "target", "agenteval", "agenteval-report.json")
        };

        for (Path path : candidates) {
            if (Files.exists(path)) {
                try {
                    ReportModel report = ReportParser.parseFile(path);
                    return ReportParser.extractMetricPassFail(report);
                } catch (IOException e) {
                    return Map.of();
                }
            }
        }
        return Map.of();
    }
}
