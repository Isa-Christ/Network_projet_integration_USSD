# 🚀 Guide de Déploiement - USSD Gateway

## 📋 Prérequis

- Docker et Docker Compose installés
- Compte GitHub (pour push du code)
- Compte Render.com (gratuit)
- Clé API HuggingFace (gratuite)

---

## 🔧 **DÉPLOIEMENT LOCAL (Test)**

### 1. Créer le fichier .env

```bash
cp .env.example .env
```

Puis éditez `.env` et remplissez vos vraies valeurs :
- `POSTGRES_PASSWORD` : Un mot de passe fort
- `HUGGINGFACE_API_KEY` : Votre clé API HuggingFace

### 2. Build et lancement

```bash
docker-compose up --build
```

### 3. Accès aux services

- **Backend + Simulateur Phone** : http://localhost:8080
- **Frontend Admin** : http://localhost:3000
- **Base de données** : localhost:5432

### 4. Arrêter les services

```bash
docker-compose down
```

---

## 🌐 **DÉPLOIEMENT EN LIGNE (Render.com)**

### Étape 1 : Préparer le repository

1. Commiter tous les fichiers Docker :
```bash
git add .
git commit -m "Add Docker configuration for deployment"
git push origin main
```

2. S'assurer que `.env` est dans `.gitignore` (déjà fait)

### Étape 2 : Créer un compte Render.com

1. Aller sur https://render.com
2. S'inscrire avec GitHub
3. Autoriser l'accès à votre repository

### Étape 3 : Déployer la base de données

1. Dashboard Render → **New** → **PostgreSQL**
2. Nom : `ussd-postgres`
3. Database : `ussd_gateway`
4. User : `ussd_user`
5. Région : Europe (West)
6. Plan : **Free**
7. Créer → **Noter l'URL de connexion interne**

### Étape 4 : Déployer le Backend

1. Dashboard Render → **New** → **Web Service**
2. Connecter votre repo GitHub
3. Configuration :
   - **Name** : `ussd-backend`
   - **Region** : Europe (West)
   - **Branch** : `main`
   - **Root Directory** : `.` (racine)
   - **Environment** : `Docker`
   - **Dockerfile Path** : `Dockerfile`
   - **Plan** : Free

4. Variables d'environnement :
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://[URL_INTERNE_POSTGRES]/ussd_gateway
   SPRING_DATASOURCE_USERNAME=ussd_user
   SPRING_DATASOURCE_PASSWORD=[MOT_DE_PASSE_POSTGRES]
   SPRING_PROFILES_ACTIVE=prod
   HUGGINGFACE_API_KEY=[VOTRE_CLE_HF]
   CORS_ALLOWED_ORIGINS=https://ussd-frontend.onrender.com
   ```

5. Créer le service

### Étape 5 : Déployer le Frontend Admin

1. Dashboard Render → **New** → **Web Service**
2. Même repository
3. Configuration :
   - **Name** : `ussd-frontend`
   - **Region** : Europe (West)
   - **Branch** : `main`
   - **Root Directory** : `frontend-admin`
   - **Environment** : `Docker`
   - **Dockerfile Path** : `frontend-admin/Dockerfile`
   - **Plan** : Free

4. Variables d'environnement :
   ```
   NEXT_PUBLIC_API_URL=https://ussd-backend.onrender.com
   NODE_ENV=production
   ```

5. Créer le service

### Étape 6 : Accéder à votre application

Après le déploiement (5-10 minutes) :

- **Backend + Phone Simulator** : `https://ussd-backend-xxxx.onrender.com`
- **Admin Interface** : `https://ussd-frontend-xxxx.onrender.com`

---

## 🔍 **Vérification du déploiement**

### Backend
```bash
curl https://ussd-backend-xxxx.onrender.com/actuator/health
```

### Frontend
```bash
curl https://ussd-frontend-xxxx.onrender.com/api/health
```

---

## 🛠️ **Commandes utiles**

### Voir les logs en local
```bash
docker-compose logs -f
docker-compose logs -f backend  # Seulement le backend
docker-compose logs -f frontend # Seulement le frontend
```

### Rebuild un service spécifique
```bash
docker-compose up --build backend
```

### Nettoyer tout
```bash
docker-compose down -v  # Supprime aussi les volumes
```

---

## ⚠️ **Problèmes courants**

### Le backend ne démarre pas
- Vérifier la connexion PostgreSQL
- Vérifier les logs : `docker-compose logs backend`

### Le frontend ne se connecte pas au backend
- Vérifier `NEXT_PUBLIC_API_URL` dans les variables d'environnement
- Vérifier CORS dans le backend

### Build échoue
- Vérifier que Java 21 est bien dans le pom.xml
- Vérifier que Node.js 20 est compatible

---

## 📞 **Support**

Pour toute question, consulter la documentation Render.com ou les logs Docker.
