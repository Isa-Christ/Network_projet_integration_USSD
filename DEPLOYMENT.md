# 🚀 GUIDE DE DÉPLOIEMENT ULTIME - USSD GATEWAY
**Plateforme : Render.com** | **Branche : `feature/develop-frontend-admin`**

Ce guide vous accompagne pas à pas pour mettre votre projet en ligne. Suivez l'ordre EXACT.

---

## �️ PRÉPARATION (À faire une seule fois)

1. **Créer un compte sur [Render.com](https://render.com/)** (Login with GitHub recommandé).
2. **Avoir votre clé HuggingFace** sous la main (commence par `hf_...`).

---

## 💾 ÉTAPE 1 : LA BASE DE DONNÉES (PostgreSQL)

C'est la fondation. On commence par elle pour obtenir l'URL de connexion nécessaire au backend.

1. Sur le **Dashboard Render**, cliquez sur **[New +]** → **PostgreSQL**.
2. Remplissez le formulaire :
   - **Name** : `ussd-db-prod` (ou ce que vous voulez)
   - **Database** : `ussd_db` (⚠️ Important : doit correspondre à votre config)
   - **User** : `ussd_user`
   - **Region** : `Frankfurt (EU Central)` (Plus proche, plus rapide)
   - **PostgreSQL Version** : `16`
   - **Instance Type** : **Free**
3. Cliquez sur **Create Database**.

🛑 **PAUSE ! Notez les informations "Internal Connection URL"**
Une fois créée, Render vous affiche des infos. Cherchez **Internal Database URL**.
Elle ressemble à : `postgres://ussd_user:A1b2C3d4...@dpg-cn...a.frankfurt-postgres.render.com/ussd_db`
👉 **Copiez cette URL**, vous en aurez besoin à l'étape 2.

---

## ⚙️ ÉTAPE 2 : LE BACKEND (Spring Boot)

1. Sur le Dashboard, cliquez sur **[New +]** → **Web Service**.
2. Connectez votre compte GitHub et choisissez votre dépôt `Network_projet_integration_USSD`.
3. Remplissez la configuration de base :
   - **Name** : `ussd-backend`
   - **Region** : `Frankfurt` (Même que la DB !)
   - **Branch** : `feature/develop-frontend-admin` (⚠️ **TRÈS IMPORTANT**)
   - **Root Directory** : `.` (laisser vide ou mettre un point)
   - **Runtime** : **Docker**
   - **Instance Type** : **Free**

4. **LES VARIABLES D'ENVIRONNEMENT** (Section "Environment Variables")
   Cliquez sur "Add Environment Variable" pour chaque ligne ci-dessous :

   | Clé (Key) | Valeur (Value) | Description |
   |-----------|----------------|-------------|
   | `SPRING_PROFILES_ACTIVE` | `prod` | Active le mode production |
   | `SERVER_PORT` | `8080` | Port d'écoute du backend |
   | `SPRING_DATASOURCE_URL` | *(Collez l'URL Interne copiée à l'étape 1)* | Connexion à la BD |
   | `SPRING_DATASOURCE_USERNAME` | `ussd_user` | Utilisateur BD |
   | `SPRING_DATASOURCE_PASSWORD` | *(Le mot de passe de la BD Render)* | Mot de passe BD |
   | `SPRING_R2DBC_URL` | *(Remplacez `postgres://` par `r2dbc:postgresql://` dans l'URL Interne)* | Connexion Réactive (Ex: `r2dbc:postgresql://...`) |
   | `SPRING_LIQUIBASE_URL` | *(Re-collez l'URL Interne normale)* | Pour les migrations DB |
   | `HUGGINGFACE_API_KEY` | `hf_VotreVraieCle...` | Votre clé pour l'IA |
   | `CORS_ALLOWED_ORIGINS` | `*` | *(On mettra l'URL du frontend plus tard pour sécuriser)* |

   > **Astuce R2DBC** : Prenez l'URL interne `postgres://...` et changez juste le début en `r2dbc:postgresql://...`.

5. Cliquez sur **Create Web Service**.
   ⏳ Le déploiement va prendre 5-10 minutes. 
   Une fois fini, en haut à gauche, vous verrez l'URL : `https://ussd-backend-xxxx.onrender.com`
   👉 **Copiez cette URL**, on en a besoin pour le frontend !

---

## 🖥️ ÉTAPE 3 : LE FRONTEND ADMIN (Next.js)

1. Sur le Dashboard, cliquez sur **[New +]** → **Web Service**.
2. Choisissez le **MÊME dépôt GitHub**.
3. Configuration :
   - **Name** : `ussd-frontend`
   - **Region** : `Frankfurt`
   - **Branch** : `feature/develop-frontend-admin` (⚠️ Toujours elle !)
   - **Root Directory** : `frontend-admin` (⚠️ **IMPORTANT : dossier du frontend**)
   - **Runtime** : **Docker**
   - **Instance Type** : **Free**

4. **LES VARIABLES D'ENVIRONNEMENT** :

   | Clé (Key) | Valeur (Value) | Description |
   |-----------|----------------|-------------|
   | `NODE_ENV` | `production` | Mode optmisé |
   | `NEXT_PUBLIC_API_URL` | *(Collez l'URL du Backend de l'étape 2)* | Ex: `https://ussd-backend-xyz.onrender.com` |

5. Cliquez sur **Create Web Service**.
   ⏳ Attendez que ce soit "Live".

---

## � ÉTAPE 4 : SÉCURISATION FINALE (Optionnel mais recommandé)

Maintenant que le frontend existe, on va dire au backend de n'accepter que lui.

1. Retournez sur votre service **Backend** (`ussd-backend`).
2. Allez dans **Environment**.
3. Modifiez `CORS_ALLOWED_ORIGINS`.
4. Mettez l'URL de votre frontend (sans le slash à la fin).
   Exemple : `https://ussd-frontend-dV4s.onrender.com`
5. Sauvegardez (cela va redéployer le backend rapidement).

---

## ✅ VÉRIFICATION

1. Ouvrez l'URL de votre **Frontend**.
2. Tentez de vous connecter ou de voir les services.
3. Si vous voyez les données, **BRAVO ! C'EST EN LIGNE !** 🌍🚀
