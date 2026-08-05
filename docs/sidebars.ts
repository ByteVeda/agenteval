import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  mainSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/installation',
        'getting-started/quickstart',
        'getting-started/configuration',
      ],
    },
    {
      type: 'category',
      label: 'Core Concepts',
      items: [
        'core-concepts/test-case-model',
        'core-concepts/eval-score',
      ],
    },
    {
      type: 'category',
      label: 'Metrics',
      link: {type: 'doc', id: 'metrics/overview'},
      items: [
        {
          type: 'category',
          label: 'Response Quality',
          items: [
            'metrics/response-quality/answer-relevancy',
            'metrics/response-quality/faithfulness',
            'metrics/response-quality/hallucination',
            'metrics/response-quality/toxicity',
            'metrics/response-quality/correctness',
            'metrics/response-quality/semantic-similarity',
            'metrics/response-quality/bias-detection',
            'metrics/response-quality/conciseness',
            'metrics/response-quality/coherence',
          ],
        },
        {
          type: 'category',
          label: 'RAG',
          items: [
            'metrics/rag/contextual-precision',
            'metrics/rag/contextual-recall',
            'metrics/rag/contextual-relevancy',
            'metrics/rag/retrieval-completeness',
          ],
        },
        {
          type: 'category',
          label: 'Agent',
          items: [
            'metrics/agent/tool-selection-accuracy',
            'metrics/agent/task-completion',
            'metrics/agent/tool-argument-correctness',
            'metrics/agent/tool-result-utilization',
            'metrics/agent/plan-quality',
            'metrics/agent/plan-adherence',
            'metrics/agent/trajectory-optimality',
            'metrics/agent/step-level-error-localization',
          ],
        },
        {
          type: 'category',
          label: 'Conversation',
          items: [
            'metrics/conversation/conversation-coherence',
            'metrics/conversation/context-retention',
            'metrics/conversation/topic-drift-detection',
            'metrics/conversation/conversation-resolution',
          ],
        },
        {
          type: 'category',
          label: 'Custom Metrics',
          items: [
            'metrics/custom/g-eval',
            'metrics/custom/deterministic',
            'metrics/custom/composite',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Judges',
      link: {type: 'doc', id: 'judges/overview'},
      items: [
        'judges/openai',
        'judges/anthropic',
        'judges/google',
        'judges/ollama',
        'judges/custom',
      ],
    },
    {
      type: 'category',
      label: 'JUnit 5',
      link: {type: 'doc', id: 'junit5/overview'},
      items: [
        'junit5/annotations',
        'junit5/assertions',
        'junit5/parameterized-tests',
      ],
    },
    {
      type: 'category',
      label: 'Datasets',
      link: {type: 'doc', id: 'datasets/overview'},
      items: [
        'datasets/formats',
        'datasets/golden-sets',
      ],
    },
    {
      type: 'category',
      label: 'Integrations',
      items: [
        'integrations/spring-ai',
        'integrations/langchain4j',
        'integrations/langgraph4j',
        'integrations/mcp',
      ],
    },
    {
      type: 'category',
      label: 'Reporting',
      link: {type: 'doc', id: 'reporting/overview'},
      items: [],
    },
    {
      type: 'category',
      label: 'Advanced',
      items: [
        'advanced/ci-cd',
        'advanced/maven-gradle-plugins',
        'advanced/red-teaming',
      ],
    },
  ],
};

export default sidebars;
