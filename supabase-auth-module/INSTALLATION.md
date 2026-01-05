# Guide d'Installation du Module Supabase Auth

## Installation Locale (Développement)

Puisque le module n'est pas encore publié dans un repository Maven public, vous devez l'installer localement :

### 1. Installer le module dans votre repository Maven local

```bash
cd supabase-auth-module
mvn clean install
```

Cela installera le module dans votre repository Maven local (`~/.m2/repository/com/supabase/supabase-auth-module/1.0.0/`).

### 2. Ajouter la dépendance dans votre projet

Dans le `pom.xml` de votre projet backend :

```xml
<dependency>
    <groupId>com.supabase</groupId>
    <artifactId>supabase-auth-module</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Configuration Supabase

#### Obtenir les clés Supabase

1. Allez sur https://supabase.com et créez un projet
2. Dans le dashboard Supabase :
   - **Settings → API** :
     - `URL` : Votre URL Supabase (ex: `https://xxxxx.supabase.co`)
     - `anon key` : Clé publique
     - `service_role key` : Clé secrète (à garder confidentielle)
   - **Settings → Auth → JWT Settings** :
     - `JWT Secret` : La clé secrète JWT (en base64)

#### Configurer Google OAuth dans Supabase

1. Dans Supabase Dashboard → **Authentication → Providers**
2. Activez **Google**
3. Configurez :
   - **Client ID** : Votre Google Client ID
   - **Client Secret** : Votre Google Client Secret
   - **Redirect URL** : `https://your-project.supabase.co/auth/v1/callback`

#### Obtenir les credentials Google OAuth

1. Allez sur https://console.cloud.google.com
2. Créez un projet ou sélectionnez un projet existant
3. Activez l'API Google+ (si nécessaire)
4. **Credentials → Create Credentials → OAuth client ID**
5. Type : **Web application**
6. **Authorized redirect URIs** : 
   - `https://your-project.supabase.co/auth/v1/callback`
   - `http://localhost:3000/auth/callback` (pour le développement)

### 4. Configuration dans application.properties

```properties
# Supabase Configuration
supabase.auth.enabled=true
supabase.auth.url=https://your-project.supabase.co
supabase.auth.anon-key=your-anon-key-here
supabase.auth.service-role-key=your-service-role-key-here
supabase.auth.jwt-secret=your-jwt-secret-base64-here
supabase.auth.auto-sync-enabled=true
supabase.auth.webhook-url=/api/webhooks/supabase
supabase.auth.webhook-secret=your-webhook-secret-here
supabase.auth.google-oauth-enabled=true
```

### 5. Configuration du Webhook Supabase (Optionnel mais recommandé)

Pour synchroniser automatiquement les utilisateurs :

1. Dans Supabase Dashboard → **Database → Webhooks**
2. Créez un nouveau webhook :
   - **Table** : `auth.users`
   - **Events** : `INSERT`, `UPDATE`, `DELETE`
   - **HTTP Method** : `POST`
   - **URL** : `https://your-backend.com/api/webhooks/supabase`
   - **HTTP Headers** : 
     ```
     Content-Type: application/json
     ```
   - **Secret** : Générez un secret et configurez-le dans `supabase.auth.webhook-secret`

### 6. Vérification

1. Compilez votre projet :
   ```bash
   mvn clean install
   ```

2. Démarrez votre application :
   ```bash
   mvn spring-boot:run
   ```

3. Vérifiez les logs pour confirmer que le module est chargé :
   ```
   Module d'authentification Supabase chargé
   ```

## Utilisation dans un autre projet

Pour utiliser ce module dans un autre projet Spring Boot :

1. Copiez le dossier `supabase-auth-module` dans votre workspace
2. Installez-le localement : `mvn clean install`
3. Ajoutez la dépendance dans votre `pom.xml`
4. Créez une classe de configuration similaire à `SupabaseAuthIntegrationConfig`
5. Configurez les propriétés dans `application.properties`

## Publication dans un Repository Maven (Optionnel)

Si vous souhaitez publier le module dans un repository Maven privé ou public :

1. Configurez le `pom.xml` avec les informations du repository
2. Ajoutez les credentials dans `~/.m2/settings.xml`
3. Exécutez : `mvn clean deploy`
