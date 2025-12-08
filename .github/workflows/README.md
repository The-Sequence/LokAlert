# GitHub Actions Workflows

This directory contains automated workflows for the LokAlert project.

## Build APKs for All Branches

**File:** `build-apk-all-branches.yml`

### Purpose

This workflow automatically builds APK files for all branches in the repository. Each APK is:
- ✅ Properly signed with a debug certificate (installable on Android devices)
- 🏷️ Labeled with the branch name for easy identification
- 📦 Published as a GitHub release with auto-generated release notes
- 💾 Stored as a build artifact for 90 days

### Triggers

The workflow runs in two scenarios:
1. **Manual Trigger**: You can manually trigger it from the Actions tab
2. **Automatic**: Runs on every push to any branch

### What It Does

1. **Identifies All Branches**: Scans the repository to find all available branches
2. **Builds APKs in Parallel**: Creates debug APKs for each branch simultaneously
3. **Signs APKs**: All APKs are signed with a debug certificate (safe for testing)
4. **Generates Release Notes**: Automatically creates release notes from recent commits
5. **Creates Releases**: Publishes a GitHub release for each branch with the APK attached

### APK Naming Convention

APKs are named following this pattern:
```
LokAlert-{branch-name}-debug.apk
```

For example:
- `LokAlert-main-debug.apk`
- `LokAlert-milestone_1-debug.apk`
- `LokAlert-milestone_2-debug.apk`
- `LokAlert-NoAlarmNavigation-debug.apk`

### Release Naming Convention

Releases are tagged and named as:
```
Tag: {branch-name}-v{version}-{build-number}
Name: LokAlert {branch-name} - v{version}
```

### Installation

To install an APK from a release:
1. Go to the [Releases](https://github.com/The-Sequence/LokAlert/releases) page
2. Find the release for your desired branch
3. Download the APK file
4. Enable "Install from Unknown Sources" on your Android device
5. Install the APK
6. Grant necessary permissions when prompted

### Branches Currently Built

The workflow automatically detects and builds all branches. Current branches include:
- `main` - Main development branch
- `milestone_1` - Checkpoint 1 features
- `milestone_2` - Checkpoint 2 features with advanced UI/UX
- `NoAlarmNavigation` - Alternative navigation approach

### Important Notes

⚠️ **Debug Builds**: All APKs are debug builds, suitable for testing but not for production use.

⚠️ **Pre-releases**: All releases except those from the `main` branch are marked as pre-releases.

⚠️ **Artifact Retention**: Build artifacts are kept for 90 days. Releases are permanent.

### Technical Details

- **JDK Version**: OpenJDK 17 (Temurin distribution)
- **Gradle Caching**: Enabled for faster builds
- **Build Type**: Debug
- **Signing**: Android debug keystore

### Viewing Build Results

After a workflow run:
1. Go to the [Actions](https://github.com/The-Sequence/LokAlert/actions) tab
2. Click on the latest workflow run
3. Check the summary for build status and APK links
4. Download artifacts or visit the Releases page

### Troubleshooting

If a build fails:
1. Check the workflow run logs in the Actions tab
2. Look for error messages in the "Build APK" step
3. Ensure the branch's code compiles locally with `./gradlew assembleDebug`
4. Verify that build.gradle.kts has correct configuration

---

For more information about the LokAlert project, see the [main README](../README.md).
