package org.byteveda.agenteval.langchain4j;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LangChain4jContentRetrieverCaptureTest {

    @Test
    void rejectsNullDelegate() {
        assertThatNullPointerException()
                .isThrownBy(() -> new LangChain4jContentRetrieverCapture(null))
                .withMessageContaining("delegate");
    }

    @Test
    void delegatesRetrievalAndCapturesTextOfReturnedContent() {
        ContentRetriever delegate = query -> List.of(
                Content.from("doc one"),
                Content.from("doc two"));
        var capture = new LangChain4jContentRetrieverCapture(delegate);

        List<Content> results = capture.retrieve(Query.from("q"));

        assertThat(results).hasSize(2);
        assertThat(capture.consumeCapturedContext()).containsExactly("doc one", "doc two");
    }

    @Test
    void consumeClearsTheInternalBuffer() {
        ContentRetriever delegate = q -> List.of(Content.from("only"));
        var capture = new LangChain4jContentRetrieverCapture(delegate);

        capture.retrieve(Query.from("q1"));
        assertThat(capture.consumeCapturedContext()).containsExactly("only");
        assertThat(capture.consumeCapturedContext()).isEmpty();
    }

    @Test
    void accumulatesAcrossMultipleRetrievals() {
        ContentRetriever delegate = q -> List.of(Content.from(q.text()));
        var capture = new LangChain4jContentRetrieverCapture(delegate);

        capture.retrieve(Query.from("a"));
        capture.retrieve(Query.from("b"));

        assertThat(capture.consumeCapturedContext()).containsExactly("a", "b");
    }
}
