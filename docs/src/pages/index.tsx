import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import CodeBlock from '@theme/CodeBlock';

import styles from './index.module.css';

type FeatureItem = {
  title: string;
  icon: string;
  description: string;
};

const features: FeatureItem[] = [
  {
    title: '25+ Built-in Metrics',
    icon: '📊',
    description:
      'Response quality, RAG evaluation, agent tool use, multi-turn conversations. Every metric returns a 0–1 score with a human-readable reason.',
  },
  {
    title: 'JUnit 5 Native',
    icon: '🧪',
    description:
      '@AgentTest, @Metric, @DatasetSource annotations. Evaluations run as ordinary test methods — no new tooling to learn.',
  },
  {
    title: 'LLM-as-Judge',
    icon: '⚖️',
    description:
      'OpenAI, Anthropic, Google, Ollama, Azure, Bedrock — plug in any provider. Research-backed G-Eval prompts, fully inspectable.',
  },
  {
    title: 'Framework Agnostic',
    icon: '🔌',
    description:
      'Evaluates any Java AI agent. Optional auto-capture for Spring AI, LangChain4j, LangGraph4j, and MCP.',
  },
  {
    title: 'Local-First',
    icon: '🔒',
    description:
      'No cloud, no SaaS, no data leaves your machine. Pair with Ollama for fully private evaluations.',
  },
  {
    title: 'CI/CD Ready',
    icon: '🚀',
    description:
      'Standard JUnit XML output works with Jenkins, GitHub Actions, and GitLab CI. Maven and Gradle plugin included.',
  },
];

const quickstartCode = `@ExtendWith(AgentEvalExtension.class)
class RefundAgentTest {

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundQuestions() {
        var testCase = AgentTestCase.builder()
            .input("What is our refund policy?")
            .actualOutput(agent.run("What is our refund policy?"))
            .retrievalContext(List.of(
                "Customers may request a full refund within 30 days of purchase."
            ))
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}`;

function Feature({title, icon, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4', styles.feature)}>
      <div className={styles.featureIcon}>{icon}</div>
      <Heading as="h3">{title}</Heading>
      <p>{description}</p>
    </div>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={siteConfig.title}
      description="JUnit 5-native AI agent evaluation library for Java">
      <header className={clsx('hero', styles.heroBanner)}>
        <div className="container">
          <Heading as="h1" className={styles.heroTitle}>
            Evaluate Java AI Agents<br />with Confidence
          </Heading>
          <p className={styles.heroSubtitle}>
            JUnit 5-native · Local-first · Framework-agnostic<br />
            25+ metrics · LLM-as-judge · Zero SaaS required
          </p>
          <div className={styles.buttons}>
            <Link
              className="button button--primary button--lg"
              to="/docs/getting-started/quickstart">
              Get Started →
            </Link>
            <Link
              className="button button--outline button--lg"
              to="/docs/metrics/overview"
              style={{marginLeft: '1rem'}}>
              View Metrics
            </Link>
          </div>
        </div>
      </header>

      <section className={styles.quickstart}>
        <div className="container">
          <div className="row">
            <div className="col col--6">
              <Heading as="h2">Write your first evaluation in minutes</Heading>
              <p>
                Add the dependency, annotate your test, and AgentEval handles the rest.
                Scores are reported in the JUnit output you already know.
              </p>
              <Link className="button button--primary" to="/docs/getting-started/quickstart">
                Full Quickstart Guide →
              </Link>
            </div>
            <div className="col col--6">
              <CodeBlock language="java" title="RefundAgentTest.java">
                {quickstartCode}
              </CodeBlock>
            </div>
          </div>
        </div>
      </section>

      <main>
        <section className={styles.features}>
          <div className="container">
            <Heading as="h2" className={styles.featuresTitle}>
              Everything you need to evaluate AI agents
            </Heading>
            <div className="row">
              {features.map((props) => (
                <Feature key={props.title} {...props} />
              ))}
            </div>
          </div>
        </section>

        <section className={styles.install}>
          <div className="container">
            <Heading as="h2">Install</Heading>
            <div className="row">
              <div className="col col--6">
                <Heading as="h3">Maven</Heading>
                <CodeBlock language="xml">
{`<dependency>
  <groupId>com.agenteval</groupId>
  <artifactId>agenteval-junit5</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>`}
                </CodeBlock>
              </div>
              <div className="col col--6">
                <Heading as="h3">Gradle (Kotlin DSL)</Heading>
                <CodeBlock language="kotlin">
{`testImplementation("com.agenteval:agenteval-junit5:1.0.0")`}
                </CodeBlock>
              </div>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
