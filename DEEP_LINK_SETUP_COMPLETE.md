# Complete Deep Link Setup Guide

## 🎯 Goal
Make email links open your Habit Tracker app directly when clicked.

---

## 📋 What You Need

1. Access to your domain DNS settings (for `atraj.it`)
2. A web server or hosting (GitHub Pages, Netlify, Vercel, or your existing server)
3. 15 minutes of your time

---

## 🚀 Quick Setup (3 Steps)

### **Step 1: Get Your SHA256 Fingerprint**

Open **Android Studio Terminal** or **Command Prompt** and run:

```bash
# For debug builds (testing)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256

# On Windows PowerShell:
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String SHA256
```

**Copy the SHA256 fingerprint** (looks like: `AB:CD:EF:12:34:56...`)

**Example output:**
```
SHA256: 14:6D:E9:83:C5:73:06:50:D8:EE:B9:95:2F:34:FC:64:16:A0:83:42:E6:1D:BE:A8:8A:04:96:B2:3F:CF:44:E5
```

Remove the `SHA256:` prefix and colons, so you get:
```
146DE983C5730650D8EEB9952F34FC6416A08342E61DBEA88A0496B23FCF44E5
```

---

### **Step 2: Create the Verification File**

I've already created `assetlinks.json` for you in your project folder.

1. Open the file: `E:\CodingWorld\AndroidAppDev\HabitTracker\assetlinks.json`
2. Replace `YOUR_SHA256_FINGERPRINT_HERE` with your fingerprint from Step 1
3. Final file should look like:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "it.atraj.habittracker",
    "sha256_cert_fingerprints": [
      "146DE983C5730650D8EEB9952F34FC6416A08342E61DBEA88A0496B23FCF44E5"
    ]
  }
}]
```

---

### **Step 3: Set Up Subdomain**

You have **3 options** - choose the easiest one for you:

---

## 🌟 **OPTION 1: GitHub Pages (Easiest - Free)**

### A. Create a New GitHub Repository

1. Go to GitHub: https://github.com/new
2. Repository name: `habittracker-deeplink`
3. Make it **Public**
4. Click "Create repository"

### B. Upload Files

In your new repository, upload these 3 files:

1. **`index.html`** (I created this for you)
2. **`assetlinks.json`** (with your SHA256 fingerprint)
3. **`CNAME`** (create new file with this content):
   ```
   habittracker.atraj.it
   ```

### C. Enable GitHub Pages

1. Go to repository **Settings** → **Pages**
2. Under "Source", select **main branch**
3. Click **Save**
4. Wait 2-3 minutes

### D. Configure DNS

In your domain provider (where you manage `atraj.it`):

Add a **CNAME** record:
```
Type: CNAME
Name: habittracker
Value: YOUR_GITHUB_USERNAME.github.io
TTL: 3600
```

**Example:**
```
Type: CNAME
Name: habittracker
Value: atrajit-sarkar.github.io
TTL: 3600
```

### E. Verify

Wait 10-15 minutes for DNS propagation, then:

1. Visit: `https://habittracker.atraj.it/.well-known/assetlinks.json`
2. Should show your assetlinks.json content
3. Visit: `https://habittracker.atraj.it/habit/123`
4. Should show the habit tracker page

---

## 🌟 **OPTION 2: Netlify (Also Easy - Free)**

### A. Sign Up

1. Go to https://netlify.com
2. Sign up with GitHub

### B. Deploy

1. Create a folder on your computer with these files:
   - `index.html`
   - `assetlinks.json` (with your SHA256)
   - Create `.well-known` folder
   - Move `assetlinks.json` inside `.well-known` folder

2. Drag and drop the folder to Netlify

### C. Configure Custom Domain

1. In Netlify dashboard → **Domain Settings**
2. Click **Add custom domain**
3. Enter: `habittracker.atraj.it`
4. Netlify will show you DNS records to add

### D. Add DNS Records

In your domain provider, add the records Netlify shows you (usually a CNAME)

---

## 🌟 **OPTION 3: Your Existing Server**

If you have SSH access to your web server:

### A. Upload Files

```bash
# SSH into your server
ssh user@your-server.com

# Create directory
mkdir -p /var/www/habittracker/.well-known

# Upload files
# - index.html to /var/www/habittracker/
# - assetlinks.json to /var/www/habittracker/.well-known/
```

### B. Configure Web Server

**For Nginx:**

Create `/etc/nginx/sites-available/habittracker.atraj.it`:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name habittracker.atraj.it;
    
    root /var/www/habittracker;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /.well-known/assetlinks.json {
        default_type application/json;
        add_header Access-Control-Allow-Origin *;
    }
}
```

Enable site:
```bash
ln -s /etc/nginx/sites-available/habittracker.atraj.it /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

**For Apache:**

Create `/etc/apache2/sites-available/habittracker.atraj.it.conf`:

```apache
<VirtualHost *:80>
    ServerName habittracker.atraj.it
    DocumentRoot /var/www/habittracker
    
    <Directory /var/www/habittracker>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
    
    <Directory /var/www/habittracker/.well-known>
        Header set Content-Type application/json
        Header set Access-Control-Allow-Origin *
    </Directory>
</VirtualHost>
```

Enable site:
```bash
a2ensite habittracker.atraj.it
apache2ctl configtest
systemctl reload apache2
```

### C. Add SSL (Highly Recommended)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx  # for Nginx
# OR
sudo apt install certbot python3-certbot-apache  # for Apache

# Get SSL certificate
sudo certbot --nginx -d habittracker.atraj.it
# OR
sudo certbot --apache -d habittracker.atraj.it
```

### D. Configure DNS

Add an **A record** or **CNAME**:

```
Type: A (or CNAME)
Name: habittracker
Value: YOUR_SERVER_IP (or your-server.com)
TTL: 3600
```

---

## ✅ Testing Your Setup

### 1. Verify DNS

```bash
# Check if DNS is working
nslookup habittracker.atraj.it

# Should show your server IP or GitHub Pages
```

### 2. Verify assetlinks.json

Visit in browser:
```
https://habittracker.atraj.it/.well-known/assetlinks.json
```

**Should return JSON** with your package name and fingerprint.

### 3. Test Deep Link

**On your phone:**

1. Send yourself a test email from the app
2. Open email on your phone
3. Click "OPEN HABIT TRACKER" button
4. Android should show "Open with Habit Tracker" dialog
5. Select Habit Tracker → App opens! 🎉

**Alternative test:**

Open Chrome on your phone and visit:
```
https://habittracker.atraj.it/habit/1
```

Should prompt to open Habit Tracker app.

---

## 🔧 Troubleshooting

### App Doesn't Open

**1. Check assetlinks.json is accessible**
   ```
   curl https://habittracker.atraj.it/.well-known/assetlinks.json
   ```

**2. Verify content-type is application/json**
   ```
   curl -I https://habittracker.atraj.it/.well-known/assetlinks.json
   ```
   Should show: `Content-Type: application/json`

**3. Check SHA256 fingerprint matches**
   - Run keytool command again
   - Compare with assetlinks.json
   - Must be **EXACT** match (no colons, no spaces)

**4. Clear app data and reinstall**
   ```bash
   adb uninstall it.atraj.habittracker
   adb install app-debug.apk
   ```

**5. Force Android to reverify**
   ```bash
   adb shell pm set-app-links-user-selection --user 0 it.atraj.habittracker true habittracker.atraj.it
   ```

### DNS Not Working

**Wait 10-30 minutes** for DNS propagation, then:

```bash
# Clear DNS cache
ipconfig /flushdns  # Windows
```

### SSL Certificate Issues

Make sure your site is **HTTPS**, not HTTP. Android App Links require HTTPS.

---

## 📱 For Production Release

When you publish to Play Store, you'll need to:

1. **Get release keystore SHA256**:
   ```bash
   keytool -list -v -keystore /path/to/release.keystore -alias your-alias
   ```

2. **Update assetlinks.json** to include BOTH debug and release fingerprints:
   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": {
       "namespace": "android_app",
       "package_name": "it.atraj.habittracker",
       "sha256_cert_fingerprints": [
         "DEBUG_FINGERPRINT_HERE",
         "RELEASE_FINGERPRINT_HERE"
       ]
     }
   }]
   ```

---

## 📄 Files Provided

I've created these files for you:

1. **`assetlinks.json`** - Android verification file
2. **`index.html`** - Beautiful landing page
3. **This guide** - Complete setup instructions

All files are in your project folder:
```
E:\CodingWorld\AndroidAppDev\HabitTracker\
```

---

## 🎉 Summary

Once setup is complete:

✅ Email links open your app directly
✅ Users see "Open with Habit Tracker" dialog
✅ Beautiful fallback page if app not installed
✅ Works on all Android devices
✅ Professional, secure implementation

---

## 🆘 Need Help?

If you get stuck:

1. Check the troubleshooting section above
2. Verify each step carefully
3. Wait for DNS propagation (can take up to 24 hours)
4. Test with `curl` commands provided

**Most common issues:**
- Wrong SHA256 fingerprint
- assetlinks.json not accessible at `/.well-known/` path
- DNS not propagated yet
- Using HTTP instead of HTTPS

---

## 🔗 Quick Reference

**Your URLs:**
- Subdomain: `habittracker.atraj.it`
- Verification file: `https://habittracker.atraj.it/.well-known/assetlinks.json`
- Deep link format: `https://habittracker.atraj.it/habit/{habitId}`
- Package name: `it.atraj.habittracker`

**DNS Record:**
```
Type: CNAME
Name: habittracker
Value: [depends on hosting choice]
TTL: 3600
```

Good luck! 🚀

