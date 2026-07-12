## GitHub Actions — Android APK build

This repo builds the TouchBridge Android APK automatically on GitHub.

### What runs

Workflow file: [`.github/workflows/android-build.yml`](../../.github/workflows/android-build.yml)

| Trigger | Result |
|---------|--------|
| Push to `main` / `master` (Android changes) | Debug + Release APK artifacts |
| Pull request | Same build (no release) |
| **Actions → Run workflow** | Manual build anytime |
| Push tag `v1.0.0` | Build + GitHub Release with APKs attached |

### Download the APK

1. Open your repo on GitHub.
2. Go to **Actions** → latest **Android APK Build** run.
3. Scroll to **Artifacts** at the bottom.
4. Download:
   - `touchbridge-debug-apk` → `app-debug.apk` (install directly)
   - `touchbridge-release-apk` → `app-release-unsigned.apk`

### First-time setup (push this repo to GitHub)

```powershell
cd "D:\Agentic Projects\Cursor\Ip Cursor"
git init
git add .
git commit -m "Add TouchBridge project with GitHub Actions Android build"
git branch -M main
git remote add origin https://github.com/cosmichackerx/touchbridge.git
git push -u origin main
```

Create the empty repo first on GitHub: **New repository** → name it (e.g. `touchbridge`) → do **not** add README if you already have one locally.

When `git push` asks for credentials:
- **Username:** your GitHub username
- **Password:** your Personal Access Token (not your GitHub password)

> **Security:** If you shared a PAT in a screenshot or chat, revoke it at  
> GitHub → Settings → Developer settings → Personal access tokens, then create a new one.

### Manual build (no push)

GitHub → your repo → **Actions** → **Android APK Build** → **Run workflow**.

### Optional: signed release APK

For Play Store or install without “unsigned” warnings, add these **repository secrets**  
(Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|--------|
| `ANDROID_KEYSTORE_BASE64` | Base64 of your `.jks` file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

Ask to enable signed release builds if you want this wired into the workflow.
