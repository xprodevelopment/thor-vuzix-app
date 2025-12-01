# GitHub Setup Instructions

## Step 1: Create GitHub Repository

1. Go to https://github.com/new
2. Name it: `thor-vuzix-app` (or whatever you prefer)
3. Make it Public (required for free Actions minutes)
4. Don't initialize with README/gitignore (we have them)
5. Click "Create repository"

## Step 2: Push Code to GitHub

```bash
cd /home/xproadmin/AGIPROJECT/vuzix-thor-stream/vuzix-app

# Initialize git (if not already done)
git init
git add .
git commit -m "Initial commit - Thor Vuzix native app"

# Add your GitHub repo (replace USERNAME with your GitHub username)
git remote add origin https://github.com/USERNAME/thor-vuzix-app.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## Step 3: Wait for Build

1. Go to your repo on GitHub
2. Click "Actions" tab
3. You should see "Build Android APK" workflow running
4. Wait ~5-10 minutes for build to complete

## Step 4: Download APK

1. Once build is complete (green checkmark ✓)
2. Click on the workflow run
3. Scroll down to "Artifacts"
4. Download `thor-vuzix-debug-apk.zip`
5. Unzip to get `app-debug.apk`

## Step 5: Install on Vuzix

```bash
adb connect 192.168.1.34:5555
adb install app-debug.apk
```

Done!

## Troubleshooting

**Build fails?**
- Check the Actions log for errors
- Usually dependency or configuration issues

**Need to rebuild?**
- Just push new code: `git add . && git commit -m "fix" && git push`
- GitHub Actions will rebuild automatically
