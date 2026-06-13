# Download

Otaku Reader is distributed as a direct APK from GitHub Releases.

<div id="latest-release">

<a class="download-button" href="https://github.com/Heartless-Veteran/Otaku-Reader/releases/latest" target="_blank" rel="noopener">
  ⬇ Download the latest release
</a>

<p id="release-version" style="margin-top: 8px; color: var(--vp-c-text-2);">Fetching latest version…</p>

</div>

## Requirements

- **Android 8.0 (API 26) or newer.** Android 15 gets full edge-to-edge treatment.
- Around 40 MB of space for the app itself; downloaded chapters use whatever you give them.
- "Install unknown apps" permission for your browser or file manager (Android asks the first time).

## Installing

1. Download the APK from the button above.
2. Open it and confirm the install prompt.
3. The first-run wizard walks you through notifications, battery optimization, appearance, and adding your first extension repository — see [Getting Started](/docs/getting-started).

## Updating

Otaku Reader checks GitHub for new releases and offers in-app updates — you don't need to come back here for every version.

## Verifying a release

Every release is built and signed by the project's CI from a tagged commit. If you want to verify, compare the APK's signing certificate against the one listed in the release notes, and check the [release workflow](https://github.com/Heartless-Veteran/Otaku-Reader/blob/main/.github/workflows/release.yml) that produced it.

<script setup>
import { onMounted } from 'vue'

onMounted(async () => {
  const el = document.getElementById('release-version')
  try {
    const res = await fetch(
      'https://api.github.com/repos/Heartless-Veteran/Otaku-Reader/releases/latest'
    )
    if (!res.ok) throw new Error(String(res.status))
    const release = await res.json()
    const apk = (release.assets || []).find((a) => a.name.endsWith('.apk'))
    const date = release.published_at ? new Date(release.published_at).toLocaleDateString() : ''
    el.textContent = `Latest: ${release.tag_name}${date ? ` · ${date}` : ''}`
    if (apk) {
      const btn = document.querySelector('.download-button')
      btn.href = apk.browser_download_url
      el.textContent += ` · ${(apk.size / 1048576).toFixed(1)} MB`
    }
  } catch {
    el.textContent = 'See the releases page for the latest version.'
  }
})
</script>

<style>
.download-button {
  display: inline-block;
  margin-top: 16px;
  padding: 12px 28px;
  border-radius: 24px;
  background: var(--vp-c-brand-1);
  color: var(--vp-c-white, #fff) !important;
  font-weight: 600;
  text-decoration: none !important;
  transition: background 0.2s;
}
.download-button:hover {
  background: var(--vp-c-brand-2);
}
</style>
