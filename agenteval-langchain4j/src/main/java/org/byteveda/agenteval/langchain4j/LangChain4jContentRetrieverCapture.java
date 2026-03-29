package org.byteveda.agenteval.langchain4j;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wraps a LangChain4j {@link ContentRetriever} to capture retrieval context.
 *
 * <p>Delegates all retrieval calls to the wrapped retriever while recording
 * the content for use as retrieval context in AgentEval test cases.</p>
 */
public final class LangChain4jContentRetrieverCapture implements ContentRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(
            LangChain4jContentRetrieverCapture.class);

    private final ContentRetriever delegate;
    private final List<String> capturedContext = new ArrayList<>();

    public LangChain4jContentRetrieverCapture(ContentRetriever delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> results = delegate.retrieve(query);
        for (Content content : results) {
            capturedContext.add(content.textSegment().text());
        }
        LOG.debug("Captured {} retrieval context items", results.size());
        return results;
    }

    /**
     * Returns the captured retrieval context and clears the buffer.
     */
    public List<String> consumeCapturedContext() {
        var result = List.copyOf(capturedContext);
        capturedContext.clear();
        return result;
    }
}
