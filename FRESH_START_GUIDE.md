# 🚀 Fresh Start Guide: Complete Azure AD & Dynamics 365 Setup

Since you've deleted the old application, let's set everything up correctly from scratch.

---

## 📋 Overview

You'll complete these steps in order:

1. Create Azure AD App Registration (5 min)
2. Configure Dynamics 365 Permissions (3 min)
3. Create Client Secret (2 min)
4. Grant Admin Consent (2 min)
5. Create Application User in Power Platform (5 min)
6. Update Code Credentials (1 min)
7. Test Everything (2 min)

**Total Time: ~20 minutes**

---

## Step 1: Create Azure AD App Registration

### 1.1 Open Azure Portal

1. Go to: **https://portal.azure.com**
2. Sign in with your **Bintara Solutions** account (`rizki@bintara.com.my`)
3. In the search bar at the top, type: **"Azure Active Directory"** or **"Microsoft Entra ID"**
4. Click on **Azure Active Directory** from the results

### 1.2 Create New App Registration

1. In the left sidebar, click: **App registrations**
2. Click: **+ New registration** (at the top)

### 1.3 Fill Registration Form

```
Name:               CRM Dashboard Backend API
Supported account types:  ☑ Accounts in this organizational directory only
                          (Bintara Solutions only - Single tenant)
Redirect URI:       Leave BLANK for now (we'll add later if needed)
```

4. Click: **Register** button at the bottom

### 1.4 Copy Important IDs

After registration, you'll see the Overview page. **COPY THESE VALUES:**

```
Application (client) ID:  00dbe0e0-d4d9-4497-b8d2-1be72563baff
Directory (tenant) ID:    b54af29e-7225-44f6-8f90-c592bb23d426
```

📝 **Save these somewhere safe!** You'll need them later.

---

## Step 2: Configure Dynamics 365 API Permissions

### 2.1 Add Dynamics CRM Permission

1. Still in your app, click: **API permissions** (left sidebar)
2. Click: **+ Add a permission**
3. In the panel that opens:
   - Click: **Dynamics CRM** (scroll down if needed)
   - Select: **Delegated permissions**
   - Check: ☑ **user_impersonation**
   - Click: **Add permissions** button

### 2.2 Add Application Permission (Critical!)

1. Click: **+ Add a permission** again
2. Click: **APIs my organization uses** (tab at top)
3. Search for: **Dynamics 365**
4. Click: **Dynamics CRM** from results
5. This time select: **Application permissions** (not Delegated!)
6. Check: ☑ **user_impersonation** or ☑ **Dynamics.Read/Write**
7. Click: **Add permissions**

> **Note:** If Application permissions are grayed out or unavailable, that's okay - we'll handle this in the next step with admin consent.

---

## Step 3: Create Client Secret

### 3.1 Generate Secret

1. Click: **Certificates & secrets** (left sidebar)
2. Click: **+ New client secret**
3. Fill in:
   ```
   Description:  Backend API Secret
   Expires:      24 months (recommended)
   ```
4. Click: **Add**

### 3.2 Copy Secret Value

⚠️ **CRITICAL:** After creation, you'll see the secret:

```
Value:    -jb8Q~nFqUjEMABsDhcAv6wmrG2i0NAzWaUfjaYG
           ⬆️ COPY THIS NOW! (you won't see it again)

Secret ID: (ignore this - not what you need)
```

📝 **Copy the "Value" field immediately!** This is your **Client Secret**.

---

## Step 4: Grant Admin Consent

This step creates the Enterprise Application (Service Principal) in Azure AD.

### 4.1 Grant Consent

1. Go back to: **API permissions** (left sidebar)
2. Click: **✓ Grant admin consent for Bintara Solutions** (button at top)
3. Click: **Yes** to confirm
4. Wait for green checkmarks to appear in the "Status" column

### 4.2 Verify Enterprise Application Created

1. In the search bar at top, type: **"Enterprise applications"**
2. Click: **Enterprise applications**
3. In the search box, paste your **Application (client) ID**
4. You should see: **CRM Dashboard Backend API** listed

✅ If you see your app, the Enterprise Application is created!

---

## Step 5: Create Application User in Power Platform

### 5.1 Open Power Platform Admin Center

1. Go to: **https://admin.powerplatform.microsoft.com**
2. Sign in with your Bintara Solutions account

### 5.2 Navigate to Your Environment

1. Click: **Environments** (left sidebar)
2. Find and click: **BINTARA CRM** (or your environment name)
3. Click: **Settings** (at the top)

### 5.3 Go to Application Users

1. Expand: **Users + permissions** section
2. Click: **Application users**

### 5.4 Create New Application User

1. Click: **+ New app user** (top right)
2. A panel opens on the right side

### 5.5 Add Your Application

1. Click: **+ Add an app** button
2. In the search box, type your **Application (client) ID** or search: **CRM Dashboard Backend API**
3. Select your app from the list
4. Click: **Add** at the bottom

### 5.6 Select Business Unit

1. For "Business unit", select: **Bintara Solutions** (or your org name)

### 5.7 Assign Security Role

1. Click: **Edit security roles** (pencil icon)
2. Check: ☑ **System Administrator**
3. Click: **Save** at the bottom

### 5.8 Finish Setup

1. Click: **Create** button at the bottom of the panel

✅ Your Application User is now created with full permissions!

---

## Step 6: Update Code Credentials

### 6.1 Prepare Your Values

You should have these three values ready:

```
Tenant ID:       b54af29e-7225-44f6-8f90-c592bb23d426 (from Step 1.4)
Client ID:       00dbe0e0-d4d9-4497-b8d2-1be72563baff (from Step 1.4)
Client Secret:   -jb8Q~nFqUjEMABsDhcAv6wmrG2i0NAzWaUfjaYG (from Step 3.2)
```

### 6.2 Run Configuration Script

**Option A: Use Interactive Script**

```bash
cd "/Users/abdulhadiy/Documents/Documents/kiki/Slide and Assigment/Internship/Dashboard-CRM"
./configure-azure-credentials.sh
```

Follow the prompts and enter your new credentials.

**Option B: Manual Update**

Edit `backend/.env` file directly:

```env
# Azure AD Configuration
AZURE_TENANT_ID=<your-tenant-id>
AZURE_CLIENT_ID=<your-client-id>
AZURE_CLIENT_SECRET=<your-client-secret>

# Dynamics 365 Configuration (same credentials)
D365_TENANT_ID=<your-tenant-id>
D365_CLIENT_ID=<your-client-id>
D365_CLIENT_SECRET=<your-client-secret>

# Dynamics 365 Endpoints (already correct)
D365_BASE_URL=https://bintarasolutions.crm5.dynamics.com/api/data/v9.2
D365_SCOPE=https://bintarasolutions.crm5.dynamics.com/.default

# JWT Secret (change in production)
JWT_SECRET=your-very-long-secret-key-min-256-bits-for-hs256-algorithm-please-change-this-in-production-environment

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
```

---

## Step 7: Test Everything

### 7.1 Start Backend

```bash
./start-backend.sh
```

Watch the logs for:

- ✅ `Started BackendApplication in X seconds`
- ❌ No errors about "403 Forbidden"
- ❌ No errors about "unauthorized"

### 7.2 Start Frontend

```bash
cd frontend
npm start
```

### 7.3 Test in Browser

1. Open: **http://localhost:4200**
2. **Login** with your Microsoft account
3. Check the dashboard:
   - ✅ Staff data loads
   - ✅ Activities load
   - ✅ Accounts load
   - ✅ Opportunities load
   - ❌ No "Unable to Load" errors

---

## 🎯 Success Checklist

Before considering this complete, verify:

- [ ] Azure AD app created with correct name
- [ ] Application (client) ID copied
- [ ] Directory (tenant) ID copied
- [ ] Dynamics CRM API permission added
- [ ] Admin consent granted (green checkmarks visible)
- [ ] Client secret created and value copied
- [ ] Enterprise Application visible in Azure AD
- [ ] Application User created in Power Platform
- [ ] System Administrator role assigned
- [ ] Credentials updated in `backend/.env`
- [ ] Backend starts without errors
- [ ] Frontend starts without errors
- [ ] Login works
- [ ] Dashboard loads all data
- [ ] No 403 Forbidden errors in backend logs

---

## 🆘 Troubleshooting

### Problem: Can't find app in Power Platform search

**Solution:**

- Make sure you granted admin consent (Step 4)
- Search using the full Application (client) ID
- Wait 5-10 minutes for Azure AD sync

### Problem: Still getting 403 Forbidden

**Solution:**

- Verify Application User has "System Administrator" role
- Check that Client Secret was copied correctly (no extra spaces)
- Restart backend after updating credentials

### Problem: API permissions are grayed out

**Solution:**

- This is normal for some tenants
- Admin consent will handle it (Step 4)
- App will work after Application User is created

### Problem: Backend won't start

**Solution:**

```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

Check logs for specific error messages.

---

## 📚 Additional Resources

- **Azure Portal:** https://portal.azure.com
- **Power Platform:** https://admin.powerplatform.microsoft.com
- **Your Dynamics 365:** https://bintarasolutions.crm5.dynamics.com

---

## ✅ Next Steps After Success

Once everything works:

1. **Test all features:**
   - Create account
   - Add activity
   - Create opportunity
   - Logout and login again

2. **Security improvements:**
   - Change `JWT_SECRET` in `.env` to a strong random value
   - Consider rotating Client Secret regularly

3. **Documentation:**
   - Keep your credentials in a secure password manager
   - Document which Azure AD app is used for this project

---

**Good luck! 🚀** Follow each step carefully and you'll have everything working in ~20 minutes.
