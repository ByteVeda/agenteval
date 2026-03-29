import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'AgentEval',
  tagline: 'JUnit 5-native AI agent evaluation for Java',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: 'https://byteveda.github.io',
  baseUrl: '/agenteval/',
  organizationName: 'ByteVeda',
  projectName: 'agenteval',

  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/ByteVeda/agenteval/tree/main/docs/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/agenteval-social-card.png',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'AgentEval',
      logo: {
        alt: 'AgentEval Logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'mainSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          to: '/docs/metrics/overview',
          label: 'Metrics',
          position: 'left',
        },
        {
          to: '/docs/judges/overview',
          label: 'Judges',
          position: 'left',
        },
        {
          href: 'https://github.com/ByteVeda/agenteval',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {label: 'Getting Started', to: '/docs/getting-started/installation'},
            {label: 'Metrics', to: '/docs/metrics/overview'},
            {label: 'Judges', to: '/docs/judges/overview'},
            {label: 'JUnit 5', to: '/docs/junit5/overview'},
          ],
        },
        {
          title: 'Integrations',
          items: [
            {label: 'Spring AI', to: '/docs/integrations/spring-ai'},
            {label: 'LangChain4j', to: '/docs/integrations/langchain4j'},
            {label: 'LangGraph4j', to: '/docs/integrations/langgraph4j'},
            {label: 'MCP', to: '/docs/integrations/mcp'},
          ],
        },
        {
          title: 'More',
          items: [
            {label: 'GitHub', href: 'https://github.com/ByteVeda/agenteval'},
            {label: 'Maven Central', href: 'https://central.sonatype.com/search?q=agenteval'},
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} ByteVeda. Licensed under Apache 2.0.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'yaml', 'bash', 'groovy'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
