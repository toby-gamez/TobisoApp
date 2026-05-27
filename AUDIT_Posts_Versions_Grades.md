# Audit struktury: Posts / PostVersions / Grades

> Kompletní přehled API, DTO, doménového modelu, databázového schématu a business logiky
> pro replikaci klientské aplikace napojené na Tobiso.Web API.

---

## 1. Doménový model (vztahy)

```
Grade (1) ──────────< PostVersion (N) >── (1) Post
  Id                    Id                    Id
  Name                  PostId                Title
  Level (6-9)           GradeId               FilePath
                        Content (Markdown)    CreatedAt
                        LastFix? (minor)      CategoryId?
                        LastEdit? (major)
```

| Entita | Role |
|--------|------|
| **Post** | Pouze metadata (title, file path, category). Obsah a timestampy jsou výhradně ve verzích. |
| **PostVersion** | Jeden obsah pro jeden ročník. `(PostId, GradeId)` je unikátní — max 1 verze na ročník. |
| **Grade** | Číselník ročníků / tříd. `Level` je unikátní. Default seed: 6.–9. ročník. |

---

## 2. Request / Response DTOs

### Grade

```kotlin
// Response
data class GradeResponse(
    val id: Int,
    val name: String,    // "6. ročník"
    val level: Int       // 6
)

// Requests
data class CreateGradeRequest(val name: String, val level: Int)
data class UpdateGradeRequest(val name: String, val level: Int)
```

### Post

```kotlin
// Plný detail (jeden post)
data class PostResponse(
    val id: Int,
    val title: String,
    val filePath: String,
    val categoryId: Int?,
    val versions: List<PostVersionResponse>
    // ?gradeId= zadáno → versions obsahuje 1 záznam (best-match)
    // bez gradeId → všechny verze (pro admin přepínání)
)

// Lightweight list (bez obsahu)
data class PostSummaryResponse(
    val id: Int,
    val title: String,
    val categoryId: Int?,
    val filePath: String,
    val lastEdit: String?,      // ISO datetime
    val lastFix: String?,
    val availableGradeNames: List<String>  // ["6. ročník", "9. ročník"]
)

// Pro výběrníky (link pickers)
data class PostLinkResponse(
    val id: Int,
    val title: String,
    val filePath: String
)
```

### Post Version

```kotlin
data class PostVersionResponse(
    val id: Int,
    val postId: Int,
    val gradeId: Int,
    val gradeName: String?,   // "8. ročník"
    val gradeLevel: Int?,     // 8
    val content: String,      // Markdown text
    val lastFix: String?,     // ISO datetime
    val lastEdit: String?
)

data class CreateVersionRequest(
    val postId: Int,
    val gradeId: Int,
    val content: String,
    val lastFix: String? = null,
    val lastEdit: String? = null
)

data class UpdateVersionRequest(
    val content: String,
    val lastFix: String? = null,
    val lastEdit: String? = null
)
```

### Create Post (s initial version)

```kotlin
data class CreatePostRequest(
    val title: String,
    val filePath: String,
    val categoryId: Int? = null,
    val gradeId: Int? = null,   // null = vytvoří se Post bez verze
    val content: String = "",
    val isFix: Boolean = false  // true → LastFix, false → LastEdit
)
```

### Update Post (jen metadata — verze se mění přes PostVersions API)

```kotlin
data class UpdatePostRequest(
    val title: String,
    val filePath: String,
    val categoryId: Int? = null
)
```

---

## 3. API Endpointy

### Posts

| Metoda | Path | Auth | Query | Request | Response |
|--------|------|------|-------|---------|----------|
| **GET** | `/api/Posts` | Anonym | `?gradeId=` | — | `List<PostResponse>` |
| **GET** | `/api/Posts/summaries` | Anonym | — | — | `List<PostSummaryResponse>` |
| **GET** | `/api/Posts/links` | Anonym | — | — | `List<PostLinkResponse>` |
| **GET** | `/api/Posts/{id}` | Anonym | `?gradeId=` | — | `PostResponse` |
| **POST** | `/api/Posts` | Authorize | — | `CreatePostRequest` | `PostResponse` |
| **PUT** | `/api/Posts/{id}` | Authorize | — | `UpdatePostRequest` | 204 NoContent |
| **DELETE** | `/api/Posts/{id}` | Authorize | — | — | 204 NoContent |
| **POST** | `/api/Posts/upload-md` | Authorize | `?directory=` | — | `{count, titles}` |

### PostVersions

| Metoda | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| **GET** | `/api/PostVersions/by-post/{postId}` | Anonym | — | `List<PostVersionResponse>` |
| **POST** | `/api/PostVersions` | Authorize | `CreateVersionRequest` | `PostVersionResponse` |
| **PUT** | `/api/PostVersions/{id}` | Authorize | `UpdateVersionRequest` | 204 NoContent |
| **DELETE** | `/api/PostVersions/{id}` | Authorize | — | 204 NoContent |

### Grades

| Metoda | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| **GET** | `/api/Grades` | Anonym | — | `List<GradeResponse>` |
| **GET** | `/api/Grades/{id}` | Anonym | — | `GradeResponse` |
| **POST** | `/api/Grades` | Authorize | `CreateGradeRequest` | `GradeResponse` |
| **PUT** | `/api/Grades/{id}` | Authorize | `UpdateGradeRequest` | 204 NoContent |
| **DELETE** | `/api/Grades/{id}` | Authorize | — | 204 / 409 Conflict |
| **POST** | `/api/Grades/seed` | Anonym | — | 204 NoContent |

> **Auth model**: Všechny **GET** endpointy jsou `[AllowAnonymous]`.  
> Mutující endpointy (**POST/PUT/DELETE**) vyžadují JWT Bearer token nebo Basic Authentication.

---

## 4. Business logika — Grade Matching Algorithm

Aplikovaný na serveru v `PostService.BestMatch()`. Klient **nikdy** neimplementuje matching — server vrací už vyfiltrovanou verzi.

```
Vstup: preferredLevel (např. 7 = 7. třída)

1. Najdi všechny verze postu, kde Grade.Level <= preferredLevel
2. Vezmi tu s nejvyšším Grade.Level (nejbližší zdola)
3. Pokud žádná nevyhovuje, fallback na nejvyšší dostupný Level vůbec
```

**Důsledky pro klienta**:
- Klient pošle `GET /api/Posts?gradeId=7` → dostane pro každý post obsah pro **8. třídu**, pokud 7. třída neexistuje
- Klient pošle `GET /api/Posts/123?gradeId=7` → dostane post s 1 verzí (best-match)
- Klient pošle **bez** `gradeId` → dostane **všechny verze** (pouze pro admin režim)
- Pro **grade switcher** (přepínač ročníků) stačí volat `GET /api/Posts/{id}?gradeId=X` pro každý Grade

---

## 5. Databázové schéma (SQL Server)

```sql
-- Grades (číselník ročníků)
CREATE TABLE Grades (
    Id   int           IDENTITY PRIMARY KEY,
    Name nvarchar(100) NOT NULL,
    Level int          NOT NULL,
    CONSTRAINT UQ_Grades_Level UNIQUE (Level)
);

-- Posts (metadata)
CREATE TABLE Posts (
    Id         int           IDENTITY PRIMARY KEY,
    Title      nvarchar(max) NOT NULL,
    FilePath   nvarchar(max) NOT NULL,
    CreatedAt  datetime2     NOT NULL,
    CategoryId int NULL      REFERENCES Categories(Id)
);

-- PostVersions (obsah + timestampy na úrovni verze)
CREATE TABLE PostVersions (
    Id       int           IDENTITY PRIMARY KEY,
    PostId   int           NOT NULL REFERENCES Posts(Id) ON DELETE CASCADE,
    GradeId  int           NOT NULL REFERENCES Grades(Id) ON DELETE NO ACTION,
    Content  nvarchar(max) NOT NULL,
    LastFix  datetime2     NULL,
    LastEdit datetime2     NULL,
    CONSTRAINT UQ_PostVersion_PostGrade UNIQUE (PostId, GradeId)
);
```

**Constraints**:
- `Grades.Level` — unikátní index
- `PostVersions(PostId, GradeId)` — unikátní párový index (max 1 verze na ročník)
- `GradeId` FK má `DeleteBehavior.Restrict` — nelze smazat Grade, pokud na něj míří nějaká PostVersion
- `PostId` FK má `DeleteBehavior.Cascade` — smazání Postu smaže i všechny jeho verze

**Seed data**:
```sql
INSERT INTO Grades (Name, Level) VALUES
    ('6. ročník', 6),
    ('7. ročník', 7),
    ('8. ročník', 8),
    ('9. ročník', 9);
```

---

## 6. Chybové stavy (Error Handling)

| Situace | HTTP Status | Body |
|---------|------------|------|
| Grade neexistuje (při GET {id}) | 404 | — |
| Post neexistuje (při GET/PUT/DELETE) | 404 | — |
| Duplicitní (PostId, GradeId) při vytváření verze | 400 | `"Version could not be created..."` |
| Grade s daným `Level` už existuje | 400 | `"Grade level already exists..."` |
| Mazání Grade, který má PostVersions | 409 Conflict | `{ "message": "Grade has post versions..." }` |
| Grade při updatu — level konflikt | 400 | `"Update failed..."` |
| Neautorizovaný přístup | 401 | — |
| Token vypršel / neplatný | 401 / 403 | — |

---

## 7. Architektura projektu pro replikaci v Android/Kotlin klientovi

```
TobisoAppNative/
├── app/src/main/java/.../tobisoapp/
│   ├── model/                  # Doménové entity
│   │   ├── Grade.kt
│   │   ├── Post.kt
│   │   └── PostVersion.kt
│   ├── dto/                    # Request/Response DTOs
│   │   ├── PostResponse.kt
│   │   ├── PostVersionResponse.kt
│   │   ├── PostSummaryResponse.kt
│   │   ├── PostLinkResponse.kt
│   │   ├── GradeResponse.kt
│   │   ├── CreatePostRequest.kt
│   │   ├── UpdatePostRequest.kt
│   │   ├── CreateVersionRequest.kt
│   │   ├── UpdateVersionRequest.kt
│   │   ├── CreateGradeRequest.kt
│   │   └── UpdateGradeRequest.kt
│   ├── api/                    # Retrofit/HTTP rozhraní
│   │   ├── TobisoApi.kt        # Rozhraní s endpointy
│   │   └── TobisoApiClient.kt  # Instance Retrofit cliente
│   ├── repository/
│   │   ├── PostRepository.kt
│   │   ├── GradeRepository.kt
│   │   └── PostVersionRepository.kt
│   └── di/                     # Dependency injection moduly
│       └── NetworkModule.kt
├── CLAUDE.md
└── INSTRUCTIONS.md
```

---

## 8. Klíčová pravidla pro klienta

1. **Post → Grade**: Post sám `GradeId` nemá. Grade je na úrovni **PostVersion**.
2. **Best-match** je **výhradně na serveru** — klient jen posílá `?gradeId=` v query parametru.
3. **Unikátnost**: Jeden Post může mít max 1 verzi na Grade. Při pokusu o duplicitní `(PostId, GradeId)` server vrací 400.
4. **Mazání Grade**: Pokud na Grade míří PostVersion, server vrací 409 Conflict.
5. **Timestamps**: `LastFix` = kosmetická/formátovací oprava, `LastEdit` = věcná změna obsahu. Oba na úrovni verze.
6. **Admin vs public**: Mutující endpointy vyžadují autorizaci. Při 401/403 přepnout na login / refresh token.
7. **Grade switcher** (přepínání ročníků): stačí volat `GET /api/Posts/{id}?gradeId=X` — server vybere správnou verzi.

---

## 9. Příklad volání (Retrofit — Kotlin)

```kotlin
// === Rozhraní API ===
interface TobisoApi {

    // Grades
    @GET("api/Grades")
    suspend fun getGrades(): List<GradeResponse>

    // Posts (s volitelným gradeId)
    @GET("api/Posts")
    suspend fun getPosts(@Query("gradeId") gradeId: Int? = null): List<PostResponse>

    @GET("api/Posts/summaries")
    suspend fun getPostSummaries(): List<PostSummaryResponse>

    @GET("api/Posts/{id}")
    suspend fun getPostById(
        @Path("id") id: Int,
        @Query("gradeId") gradeId: Int? = null
    ): PostResponse

    @POST("api/Posts")
    suspend fun createPost(@Body req: CreatePostRequest): PostResponse

    @PUT("api/Posts/{id}")
    suspend fun updatePost(@Path("id") id: Int, @Body req: UpdatePostRequest)

    @DELETE("api/Posts/{id}")
    suspend fun deletePost(@Path("id") id: Int)

    // PostVersions
    @GET("api/PostVersions/by-post/{postId}")
    suspend fun getVersionsByPost(@Path("postId") postId: Int): List<PostVersionResponse>

    @POST("api/PostVersions")
    suspend fun createVersion(@Body req: CreateVersionRequest): PostVersionResponse

    @PUT("api/PostVersions/{id}")
    suspend fun updateVersion(@Path("id") id: Int, @Body req: UpdateVersionRequest)

    @DELETE("api/PostVersions/{id}")
    suspend fun deleteVersion(@Path("id") id: Int)
}

// === Použití v Repository ===
class PostRepository(private val api: TobisoApi) {

    suspend fun getPostsForGrade(gradeId: Int): List<PostResponse> {
        return api.getPosts(gradeId = gradeId)
    }

    suspend fun getPostWithBestMatch(postId: Int, gradeId: Int): PostResponse {
        return api.getPostById(postId, gradeId = gradeId)
    }

    suspend fun createPostWithVersion(
        title: String,
        filePath: String,
        gradeId: Int,
        content: String,
        isFix: Boolean = false
    ): PostResponse {
        return api.createPost(
            CreatePostRequest(
                title = title,
                filePath = filePath,
                gradeId = gradeId,
                content = content,
                isFix = isFix
            )
        )
    }
}

// === DI (Hilt / Koin) ===
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${SessionManager.token}")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        .build()

    @Provides @Singleton
    fun provideTobisoApi(retrofit: Retrofit): TobisoApi =
        retrofit.create(TobisoApi::class.java)
}
```

---

## 10. Srovnání: Admin vs Public API

| Aspekt | Public API (`/api/Pages`) | Admin API (`/api/Posts`) |
|--------|--------------------------|--------------------------|
| Auth | Anonym (pouze GET) | GET anonym, mutace Authorize |
| GradeId filtr | Ano | Ano |
| Všechny verze | Ne (pouze best-match) | Ano (bez gradeId) |
| Grade seed | — | Ano |
| Upload MD | — | Ano |

> **Poznámka**: Public API routuje přes `/api/Pages` → `PostsController`. Admin API routuje přes `/api/Posts` → tentýž `PostsController`. Responses jsou identické.

---

## 11. Migrace / Historie změn

| Migrace | Datum | Co se změnilo |
|---------|-------|---------------|
| `AddGradesAndPostVersions` | 2026-05-17 | Vytvoření tabulek Grades + PostVersions. Migrace obsahu Post → PostVersions (Grade=9). Drop legacy sloupců z Posts. |
| `EnforceGradeNonNullAndUnique` | 2026-05-18 | GradeId NOT NULL na PostVersions. Unique index (PostId, GradeId). |
| `SyncPostVersionGradeIdNonNullable` | 2026-05-18 | Oprava FK GradeId na Restrict delete. |
