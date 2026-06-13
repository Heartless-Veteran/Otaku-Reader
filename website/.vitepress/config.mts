import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Otaku Reader',
  description:
    'Free and open-source manga reader for Android — no AI, no cloud, no ads. Tachiyomi extension compatible.',
  base: '/Otaku-Reader/',
  lang: 'en-US',
  lastUpdated: true,
  sitemap: {
    hostname: 'https://heartless-veteran.github.io/Otaku-Reader/',
  },
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/Otaku-Reader/logo.png' }],
    ['meta', { name: 'theme-color', content: '#0f172a' }],
    ['meta', { property: 'og:title', content: 'Otaku Reader' }],
    [
      'meta',
      {
        property: 'og:description',
        content: 'Free and open-source manga reader for Android — no AI, no cloud, no ads.',
      },
    ],
    ['meta', { property: 'og:image', content: 'https://heartless-veteran.github.io/Otaku-Reader/logo.png' }],
  ],
  themeConfig: {
    logo: '/logo.png',
    nav: [
      { text: 'Download', link: '/download' },
      { text: 'Docs', link: '/docs/getting-started' },
      { text: 'Changelog', link: '/changelog' },
    ],
    sidebar: {
      '/docs/': [
        {
          text: 'Getting Started',
          items: [
            { text: 'Installation & First Run', link: '/docs/getting-started' },
            { text: 'FAQ', link: '/docs/faq' },
          ],
        },
        {
          text: 'Guides',
          items: [
            { text: 'Extensions & Repositories', link: '/docs/guides/extensions' },
            { text: 'Library', link: '/docs/guides/library' },
            { text: 'Reader', link: '/docs/guides/reader' },
            { text: 'Tracking', link: '/docs/guides/tracking' },
            { text: 'Backups & Sync', link: '/docs/guides/backups' },
          ],
        },
      ],
    },
    socialLinks: [{ icon: 'github', link: 'https://github.com/Heartless-Veteran/Otaku-Reader' }],
    search: { provider: 'local' },
    footer: {
      message: 'Open source. Your library stays on your device.',
      copyright: 'Otaku Reader — built by a solo developer, powered by the Tachiyomi extension ecosystem.',
    },
    editLink: {
      pattern: 'https://github.com/Heartless-Veteran/Otaku-Reader/edit/main/website/:path',
      text: 'Edit this page on GitHub',
    },
  },
})
