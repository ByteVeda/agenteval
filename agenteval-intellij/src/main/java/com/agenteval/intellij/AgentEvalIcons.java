package com.agenteval.intellij;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Icon constants for the AgentEval IntelliJ plugin.
 *
 * <p>Icons are loaded from the plugin's resource directory.</p>
 */
public final class AgentEvalIcons {

    private AgentEvalIcons() {}

    /** Pass icon (green checkmark, 13x13). */
    public static final Icon PASS = IconLoader.getIcon(
            "/icons/agenteval-pass.svg", AgentEvalIcons.class);

    /** Fail icon (red X, 13x13). */
    public static final Icon FAIL = IconLoader.getIcon(
            "/icons/agenteval-fail.svg", AgentEvalIcons.class);

    /** Tool window icon (13x13). */
    public static final Icon TOOL_WINDOW = IconLoader.getIcon(
            "/icons/agenteval-toolwindow.svg", AgentEvalIcons.class);
}
