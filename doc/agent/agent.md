# AGENT.md — DIY Wallpaper Maker & Photo Collage

## 1. Project Context

This project is an Android app named **DIY Wallpaper Maker & Photo Collage**.

The app allows users to:

- Browse wallpaper categories.
- Download and set static wallpapers.
- Display live wallpaper video items in content lists.
- Preview live wallpaper by thumbnail in MVP.
- Support actual video live wallpaper service in a later phase.
- Browse DIY templates.
- Insert personal photos into image areas inside DIY templates.
- Create wallpapers from background templates.
- Add stickers, including animated GIF stickers for preview.
- Export final wallpapers as static images.
- Support `diy-live` in MVP by previewing animation only if parsable and exporting a static frame.

The app is built with **Kotlin**, **Jetpack Compose**, **Clean Architecture + MVVM**, **Coroutines/Flow**, **Hilt**, **Retrofit/OkHttp**, **Kotlin Serialization**, **Room**, **DataStore**, **Coil**, and Android **WallpaperManager**.

Use **C/C++ NDK** only to make endpoint extraction harder. Do not treat CDN endpoints as secrets.

---

## 2. Core CDN Endpoints

```text
data_url_full:
https://cdn.leansoft-ai.com/ls36-diy-wallpaper/json/data_20260227_1339.json

data_bgcreate_url:
https://cdn.leansoft-ai.com/ls36-diy-wallpaper/data/bgcreate.json

data_stickers_url:
https://cdn.leansoft-ai.com/ls36-diy-wallpaper/data/stickers.json
```

Endpoint rules:

- `data_url_full` contains normal wallpaper categories, live video wallpaper items, and special category `DIY`.
- `data_bgcreate_url` contains background/template images for creating wallpapers.
- `data_stickers_url` contains a flat sticker list, not categories.
- Category `DIY` must not be handled as normal wallpaper.
- `type = "2d"` means static wallpaper.
- `type = "live"` means live wallpaper video.
- For `type = "live"`, `thumb` is preview image/webp and `content` is the `.mp4` video URL.

---

## 3. Architecture Rules

Use Clean Architecture + MVVM.

```text
Presentation Layer
        ↓
Domain Layer
        ↓
Data Layer
        ↓
Native Config / Remote CDN / Room / DataStore / File Cache
```

Rules:

- UI must not call endpoints directly.
- UI calls ViewModel.
- ViewModel calls UseCase.
- UseCase calls Repository.
- Repository uses `EndpointProvider`, not `NativeConfig` directly.
- Data layer returns `AppResult<T>` / `AppError`, not raw exceptions to UI.
- Export and heavy bitmap work must run on `Dispatchers.Default` or `Dispatchers.IO`, never Main.

---

## 4. Endpoint Provider / NDK Rules

Endpoint URLs should be provided through:

```kotlin
interface EndpointProvider {
    fun getDataFullUrl(): String
    fun getBgCreateUrl(): String
    fun getStickersUrl(): String
}
```

Implementation uses `NativeConfig`:

```kotlin
object NativeConfig {
    init { System.loadLibrary("native_config") }

    external fun getDataFullUrl(): String
    external fun getBgCreateUrl(): String
    external fun getStickersUrl(): String
}
```

Security rules:

- CDN endpoints are public metadata, not secrets.
- Do not store secret keys in APK.
- Do not protect premium assets only by hiding URLs.
- Do not log endpoint URLs in release.
- Do not log full JSON responses in release.
- Use backend/signed URLs later if true premium asset protection is needed.

---

## 5. Data Contract: `data_url_full`

### 5.1 Category DTO

```kotlin
@Serializable
data class RemoteCategoryDto(
    val category: String = "",
    val rank: Int = Int.MAX_VALUE,
    val icon: String? = null,
    val items: List<RemoteItemDto> = emptyList()
)
```

### 5.2 Item DTO

```kotlin
@Serializable
data class RemoteItemDto(
    val id: Int = 0,
    val type: String? = null,
    val rank: Int = Int.MAX_VALUE,
    val thumb: String? = null,

    // Static / live wallpaper
    val data: String? = null,
    val preview: String? = null,
    val content: String? = null,

    // DIY
    @SerialName("diy_data")
    val diyData: String? = null,

    @SerialName("diy_animation")
    val diyAnimation: String? = null
)
```

Mapping rules:

```text
if category == "DIY":
    map item to DiyTemplate
else:
    map item to WallpaperItem
```

---

## 6. Static / Live Wallpaper Mapping

### 6.1 WallpaperType

```kotlin
enum class WallpaperType {
    STATIC_2D,
    LIVE_VIDEO,
    UNKNOWN
}
```

Mapping:

```text
type = "2d"   -> STATIC_2D
type = "live" -> LIVE_VIDEO
other/null     -> UNKNOWN
```

### 6.2 Domain Model

```kotlin
data class WallpaperItem(
    val id: String,
    val categoryId: String,
    val type: WallpaperType,
    val rank: Int,
    val thumbUrl: String,
    val imageUrl: String?,
    val videoUrl: String?
)
```

Meaning:

- `thumbUrl`: preview image for both static and live wallpaper.
- `imageUrl`: static image URL for `type = "2d"`.
- `videoUrl`: `.mp4` URL from `content` for `type = "live"`.

### 6.3 Mapper

```kotlin
fun RemoteItemDto.toWallpaperDomainOrNull(categoryId: String): WallpaperItem? {
    val validThumb = thumb?.takeIf { it.isNotBlank() } ?: return null

    return WallpaperItem(
        id = "wallpaper_${categoryId}_$id",
        categoryId = categoryId,
        type = when (type) {
            "2d" -> WallpaperType.STATIC_2D
            "live" -> WallpaperType.LIVE_VIDEO
            else -> WallpaperType.UNKNOWN
        },
        rank = rank,
        thumbUrl = validThumb,
        imageUrl = when (type) {
            "2d" -> data?.takeIf { it.isNotBlank() } ?: validThumb
            else -> null
        },
        videoUrl = when (type) {
            "live" -> content?.takeIf { it.isNotBlank() }
            else -> null
        }
    )
}
```

Important rules:

- For `type = "2d"`, use `data` as `imageUrl` if available; otherwise use `thumb`.
- For `type = "live"`, map `content` to `videoUrl`.
- Never map `content` into `imageUrl`.
- If live item has `thumb` but no `content`, show thumbnail but disable preview video/download/set live wallpaper actions.
- If item has no usable `thumb`, skip it from UI.

---

## 7. DIY Template Mapping

Category `DIY` items contain:

```json
{
  "id": 1,
  "type": "diy-live",
  "rank": 1,
  "thumb": "https://.../preview_01.webp",
  "diy_data": "https://.../diy-data/1/data.json",
  "diy_animation": "https://.../diy-data/1/animation.json"
}
```

### 7.1 DTO

```kotlin
@Serializable
data class DiyTemplateDto(
    val id: Int = 0,
    val type: String? = null,
    val rank: Int = Int.MAX_VALUE,
    val thumb: String? = null,

    @SerialName("diy_data")
    val diyData: String? = null,

    @SerialName("diy_animation")
    val diyAnimation: String? = null
)
```

### 7.2 Domain

```kotlin
data class DiyTemplate(
    val id: String,
    val type: DiyTemplateType,
    val rank: Int,
    val thumbUrl: String,
    val diyDataUrl: String,
    val diyAnimationUrl: String?
)

enum class DiyTemplateType {
    DIY_STATIC,
    DIY_LIVE
}
```

### 7.3 Mapper

```kotlin
fun DiyTemplateDto.toDomainOrNull(): DiyTemplate? {
    val validDiyData = diyData?.takeIf { it.isNotBlank() } ?: return null

    return DiyTemplate(
        id = id.toString(),
        type = when (type) {
            "diy-live" -> DiyTemplateType.DIY_LIVE
            "diy-static" -> DiyTemplateType.DIY_STATIC
            else -> DiyTemplateType.DIY_STATIC
        },
        rank = rank,
        thumbUrl = thumb.orEmpty(),
        diyDataUrl = validDiyData,
        diyAnimationUrl = diyAnimation?.takeIf { it.isNotBlank() }
    )
}
```

Rules:

- DIY item without valid `diy_data` must be skipped.
- Unknown/null DIY type falls back to `DIY_STATIC`.
- Empty `diy_animation` means no animation URL.
- Missing `thumb` is allowed; UI should show placeholder thumbnail.

---

## 8. Data Contract: `bgcreate.json`

Actual schema:

```kotlin
@Serializable
data class BackgroundCreateDto(
    val id: Int = 0,

    @SerialName("category_rank")
    val categoryRank: Int = Int.MAX_VALUE,

    val name: String = "",
    val data: String = ""
)
```

Domain:

```kotlin
data class BackgroundCreateItem(
    val id: String,
    val rank: Int,
    val name: String,
    val imageUrl: String,
    val thumbnailUrl: String
)
```

Mapping:

```text
id -> id
category_rank -> categoryRank -> rank
name -> name
data -> imageUrl
data -> thumbnailUrl
```

Rule:

- If `data` is blank, skip the item.
- Do not assume fields like `category`, `imageUrl`, `thumbnailUrl`, `isPremium`, `width`, or `height`; they are not present in the current schema.

---

## 9. Data Contract: `stickers.json`

Actual schema:

```kotlin
@Serializable
data class StickerDto(
    val id: Int = 0,
    val rank: Int = Int.MAX_VALUE,
    val stickers: String = ""
)
```

Domain:

```kotlin
data class StickerItem(
    val id: String,
    val rank: Int,
    val stickerUrl: String,
    val thumbnailUrl: String,
    val isAnimated: Boolean
)
```

Mapping:

```text
id -> id
rank -> rank
stickers -> stickerUrl
stickers -> thumbnailUrl
isAnimated -> stickers.endsWith(".gif", ignoreCase = true)
```

Rules:

- Sticker list is flat; there are no sticker categories in MVP.
- If `stickers` is blank, skip the item.
- Stickers may be animated GIFs.
- UI can preview GIFs with Coil GIF decoder.
- Static export only needs first/current frame; do not export animated GIF in MVP.

---

## 10. Data Contract: `diy_data`

Actual schema:

```kotlin
@Serializable
data class DiyTemplateDataDto(
    val width: Int = 1080,
    val height: Int = 1920,
    val background: String = "#FFFFFF",
    val elements: List<DiyElementDto> = emptyList()
)
```

```kotlin
@Serializable
data class DiyElementDto(
    val type: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val angle: Float = 0f,
    val layoutIndex: Int = Int.MAX_VALUE,
    val srcName: String = ""
)
```

Do not assume these fields in MVP:

```text
layers
photoPlaceholders
maskUrl
placeholderId
zIndex
alpha
```

Element meaning:

```text
type = Picture -> fixed template asset
type = Image   -> user photo placeholder
```

`PhotoPlaceholder` is derived from `Image` elements. It is not directly present in JSON.

---

## 11. DIY Domain Model

```kotlin
data class DiyTemplateData(
    val width: Int,
    val height: Int,
    val background: String,
    val elements: List<DiyElement>,
    val placeholders: List<PhotoPlaceholder>
)
```

```kotlin
data class DiyElement(
    val type: DiyElementType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int,
    val srcName: String,
    val assetUrl: String?
)
```

```kotlin
enum class DiyElementType {
    PICTURE,
    IMAGE,
    UNKNOWN
}
```

```kotlin
data class PhotoPlaceholder(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int
)
```

Mapping rules:

- `Picture` -> `DiyElementType.PICTURE` and asset URL resolved from `diyDataUrl + srcName`.
- `Image` -> `DiyElementType.IMAGE` and converted into a `PhotoPlaceholder`.
- `layoutIndex` -> `zIndex`.
- `angle` -> `rotation`.

---

## 12. DIY Asset URL Resolver

`Picture` elements only have `srcName`, not full URL.

Resolve rule:

```text
assetBaseUrl = parent directory of diyDataUrl
assetUrl = assetBaseUrl + "/" + srcName
```

Example:

```text
diyDataUrl:
https://cdn.leansoft-ai.com/ls36-diy-wallpaper/diy-data/1/data.json

srcName:
layer_01.webp

assetUrl:
https://cdn.leansoft-ai.com/ls36-diy-wallpaper/diy-data/1/layer_01.webp
```

Implementation:

```kotlin
fun resolveDiyAssetUrl(diyDataUrl: String, srcName: String): String {
    val baseUrl = diyDataUrl.substringBeforeLast("/")
    return "$baseUrl/$srcName"
}
```

---

## 13. DIY Background Resolver

`background` in `diy_data` may be:

- Hex color, for example `#FFFFFF` or `#FFFFFFFF`.
- Full URL.
- File name relative to the `diy_data` directory.
- Blank.

Never resolve hex colors as URLs.

```kotlin
sealed interface DiyBackgroundValue {
    data class ColorHex(val value: String) : DiyBackgroundValue
    data class RemoteUrl(val url: String) : DiyBackgroundValue
    data class AssetUrl(val url: String) : DiyBackgroundValue
    data object Empty : DiyBackgroundValue
}
```

```kotlin
fun resolveDiyBackgroundValue(
    diyDataUrl: String,
    background: String
): DiyBackgroundValue {
    val value = background.trim()

    if (value.isBlank()) return DiyBackgroundValue.Empty

    if (value.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
        return DiyBackgroundValue.ColorHex(value)
    }

    if (value.startsWith("http://") || value.startsWith("https://")) {
        return DiyBackgroundValue.RemoteUrl(value)
    }

    return DiyBackgroundValue.AssetUrl(resolveDiyAssetUrl(diyDataUrl, value))
}
```

Rules:

- `#FFFFFF`, `#FFFFFFFF`, `#000000`, `#00000000` are colors.
- `https://...` is remote URL.
- `background.webp` is relative asset file.
- Blank background becomes `Empty`.

---

## 14. Placeholder ID

Do not use temporary index-only placeholder IDs like:

```text
placeholder_0
placeholder_1
```

Use stable format:

```text
image_${layoutIndex}_${srcName}
```

Implementation:

```kotlin
fun buildPlaceholderId(element: DiyElementDto): String {
    val safeSrcName = element.srcName
        .ifBlank { "empty" }
        .replace("/", "_")
        .replace("\\", "_")

    return "image_${element.layoutIndex}_$safeSrcName"
}
```

Rules:

- User photo must be stored by `placeholderId`.
- Auto-save must keep user photo associated with this `placeholderId`.
- If `srcName` is blank, use `empty`.

---

## 15. DIY Render Rules

Render order:

```text
1. Create canvas using diy_data width/height.
2. Resolve background:
   - Hex color -> fill color.
   - URL -> load image from URL/cache.
   - File name -> resolve from diyDataUrl.
   - Empty -> transparent/default fallback.
3. Sort elements by layoutIndex/zIndex.
4. Render Picture elements from resolved assetUrl.
5. Render Image elements:
   - If user photo exists for placeholderId -> draw user photo.
   - If no user photo:
       EDITOR_PREVIEW -> draw placeholder indicator.
       EXPORT -> draw nothing.
6. Apply angle/rotation around element center.
7. Export bitmap.
```

Render modes:

```kotlin
enum class RenderMode {
    EDITOR_PREVIEW,
    EXPORT
}
```

Important:

- Export must not include placeholder indicator.
- Allow export even if some placeholders are empty, but show warning before export.
- User photo fit behavior for MVP: center crop into Image element.
- Move/scale/rotate photo inside placeholder is phase later.

---

## 16. `diy_animation` Rules

Do not lock a complex animation schema in MVP.

MVP behavior:

- If template type is `diy-live` and animation URL is not blank, fetch raw animation JSON.
- Save raw JSON.
- If animation can be parsed, preview it.
- If animation cannot be parsed, still allow user photo insertion and static frame export.
- Static export is enough for MVP.
- Real live wallpaper service is later phase.

Entity can store raw JSON only:

```kotlin
data class DiyAnimationRaw(
    val templateId: String,
    val animationUrl: String,
    val rawJson: String?
)
```

---

## 17. Room Entities

### 17.1 WallpaperCategoryEntity

```kotlin
@Entity(tableName = "wallpaper_categories")
data class WallpaperCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconUrl: String?,
    val rank: Int,
    val rawJson: String?
)
```

### 17.2 WallpaperItemEntity

```kotlin
@Entity(tableName = "wallpaper_items")
data class WallpaperItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val type: String?,
    val rank: Int,
    val thumbUrl: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val localPath: String?,
    val rawJson: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 17.3 DiyTemplateEntity

```kotlin
@Entity(tableName = "diy_templates")
data class DiyTemplateEntity(
    @PrimaryKey val id: String,
    val type: String,
    val rank: Int,
    val thumbUrl: String,
    val diyDataUrl: String,
    val diyAnimationUrl: String?,
    val diyDataLocalPath: String?,
    val diyAnimationLocalPath: String?,
    val rawJson: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 17.4 DiyTemplateDataEntity

```kotlin
@Entity(tableName = "diy_template_data")
data class DiyTemplateDataEntity(
    @PrimaryKey val templateId: String,
    val dataUrl: String,
    val localPath: String?,
    val rawJson: String,
    val updatedAt: Long
)
```

### 17.5 DiyAnimationEntity

```kotlin
@Entity(tableName = "diy_animations")
data class DiyAnimationEntity(
    @PrimaryKey val templateId: String,
    val animationUrl: String,
    val localPath: String?,
    val rawJson: String?,
    val updatedAt: Long
)
```

### 17.6 BackgroundCreateEntity

```kotlin
@Entity(tableName = "background_create_items")
data class BackgroundCreateEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val name: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val localPath: String?,
    val rawJson: String?
)
```

### 17.7 StickerItemEntity

```kotlin
@Entity(tableName = "sticker_items")
data class StickerItemEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val stickerUrl: String,
    val thumbnailUrl: String,
    val localPath: String?,
    val isAnimated: Boolean,
    val rawJson: String?
)
```

### 17.8 WallpaperDesignEntity

```kotlin
@Entity(tableName = "wallpaper_designs")
data class WallpaperDesignEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailPath: String?,
    val exportPath: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val sourceType: String,
    val sourceAssetId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean,
    val isDraft: Boolean,
    val isExported: Boolean
)
```

### 17.9 DesignLayerEntity

```kotlin
@Entity(tableName = "design_layers")
data class DesignLayerEntity(
    @PrimaryKey val id: String,
    val designId: String,
    val layerType: String,
    val contentUri: String?,
    val assetId: String?,
    val placeholderId: String?,
    val textValue: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
    val zIndex: Int,
    val color: String?,
    val fontFamily: String?,
    val fontSize: Float?,
    val extraJson: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

Layer type MVP:

```text
BACKGROUND
PICTURE_TEMPLATE
PHOTO_PLACEHOLDER
USER_PHOTO_IN_PLACEHOLDER
STICKER
TEXT
FRAME
SHAPE
```

### 17.10 FavoriteEntity

```kotlin
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val targetId: String,
    val targetType: String,
    val createdAt: Long
)
```

Target types:

```text
WALLPAPER
LIVE_WALLPAPER
DIY_TEMPLATE
BACKGROUND_CREATE
STICKER
DESIGN
```

### 17.11 ExportHistoryEntity

```kotlin
@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey val id: String,
    val designId: String,
    val exportPath: String,
    val width: Int,
    val height: Int,
    val format: String,
    val quality: Int,
    val exportedAt: Long,
    val setAsWallpaper: Boolean
)
```

---

## 18. Cache / Sync Policy

Metadata endpoints:

```text
data_url_full
data_bgcreate_url
data_stickers_url
```

Metadata policy:

- Cache metadata in Room.
- Store `lastSyncedAt` in DataStore.
- On app start:
    - If no cache -> remote sync is required.
    - If cache is fresh -> use cache.
    - If cache is stale -> show cache first, sync in background.
- If sync fails but cache exists -> keep using cache.
- If sync fails and no cache exists -> show error.

TTL:

```text
metadataTtlHours = 24
diyDataTtlDays = 7
```

DIY data policy:

- Load `diy_data` only when user opens a template.
- Cache raw `diy_data` JSON in Room.
- Cache raw `diy_animation` if available.
- Use cache first if available.
- Refresh if too old or user pull-to-refresh.

Asset cache policy:

- Use Coil disk cache for image preview/display.
- Use file cache for video if download is enabled.
- Do not download all assets during metadata sync.
- Download assets on demand.
- Add Clear Cache in Settings.

---

## 19. Error Model

```kotlin
sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object Timeout : AppError
    data object EmptyResponse : AppError

    data class HttpError(val code: Int, val message: String?) : AppError
    data class JsonParseError(val source: String, val reason: String?) : AppError
    data class InvalidDataContract(val source: String, val field: String?, val reason: String) : AppError
    data class AssetLoadError(val url: String, val reason: String?) : AppError
    data class VideoLoadError(val url: String, val reason: String?) : AppError
    data class ExportError(val reason: String?) : AppError
    data class StorageError(val reason: String?) : AppError
    data class Unknown(val throwable: Throwable?) : AppError
}
```

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}
```

UI message mapping:

- `NetworkUnavailable`: use cached data if available.
- `JsonParseError`: remote data is invalid.
- `InvalidDataContract`: skip invalid content where possible.
- `AssetLoadError`: asset cannot be loaded.
- `VideoLoadError`: video wallpaper cannot be loaded.
- `ExportError`: export failed.
- `StorageError`: cannot write to storage/media library.

---

## 20. Core Flows

### 20.1 Startup Sync

```text
App start
  -> Load Room cache if available
  -> Check TTL
  -> If stale, sync remote in background
  -> Fetch data_url_full, bgcreate, stickers
  -> Parse DTO
  -> Category DIY -> DiyTemplateEntity
  -> Category normal:
       type 2d   -> WallpaperItemEntity.imageUrl
       type live -> WallpaperItemEntity.videoUrl
  -> bgcreate -> BackgroundCreateEntity
  -> stickers -> StickerItemEntity
  -> Room emits Flow
  -> UI displays data
```

### 20.2 Wallpaper 2D

```text
Open static wallpaper
  -> Show preview
  -> Download imageUrl or use localPath
  -> Save to MediaStore
  -> Set with WallpaperManager
```

### 20.3 Wallpaper Live Video MVP

```text
Open live wallpaper
  -> Show thumb preview
  -> videoUrl = content
  -> MVP disables Set Live Wallpaper if service not implemented
  -> If videoUrl is null, disable video actions
```

### 20.4 DIY Template

```text
Open DIY category
  -> Show DiyTemplateEntity list
  -> Open template
  -> Load/cache diy_data
  -> Parse width, height, background, elements
  -> Resolve background
  -> Picture -> resolve asset URL
  -> Image -> derive PhotoPlaceholder
  -> Create WallpaperDesign
  -> Open DiyEditorScreen
```

### 20.5 Export DIY Wallpaper

```text
Render canvas with diy_data width/height
  -> Resolve/draw background
  -> Sort elements by zIndex/layoutIndex
  -> Draw Picture assets
  -> Draw user photos in Image placeholders
  -> Draw stickers/text
  -> Encode JPEG/PNG
  -> Save MediaStore
  -> Save ExportHistoryEntity
```

---

## 21. Invalid Item Rules

Apply these during mapper/sync:

```text
Wallpaper type = 2d:
- If thumb blank and data blank -> skip item.
- If thumb exists but data blank -> imageUrl = thumb.

Wallpaper type = live:
- If thumb blank -> skip item.
- If content blank -> show thumb but disable live actions.

DIY:
- If diyData blank -> skip item.
- If thumb blank -> keep item and show placeholder thumbnail.

BackgroundCreate:
- If data blank -> skip item.

Sticker:
- If stickers blank -> skip item.
```

---

## 22. MVP Phase Plan

### Phase 0 — Data Contract Validation

- Fetch all 3 endpoint JSON files.
- Parse normal category.
- Parse `type = 2d`.
- Parse `type = live` and `content`.
- Parse category `DIY`.
- Fetch sample `diy_data`.
- Check background type: hex, URL, file name.
- Fetch sample `diy_animation`.
- Fetch `bgcreate.json`.
- Fetch `stickers.json`.
- Write parser unit tests.

### Phase 1 — Core Data + Native Endpoint

- Add NDK module.
- Add `NativeConfig`.
- Add `EndpointProvider`.
- Add Retrofit API.
- Sync all metadata.
- Map entities.
- Cache Room.
- Add DataStore `lastSyncedAt`.

### Phase 2 — Wallpaper Gallery

- Show categories.
- Show 2D and live items.
- Detail screen for 2D.
- Detail screen for live.
- Download/set static wallpaper.

### Phase 3 — Live Wallpaper MVP

- Show thumb preview.
- Map `content` to `videoUrl`.
- Disable set live wallpaper if service not implemented.
- Optional video download only if product requires it.

### Phase 4 — DIY Static Template

- Show DIY category.
- Load and parse `diy_data`.
- Resolve background.
- Resolve Picture assets.
- Derive Image placeholders.
- Select user photo.
- Center crop photo into placeholder.
- Export static wallpaper.

### Phase 5 — DIY Live MVP

- Load animation raw JSON.
- Preview only if parseable.
- Export static frame.

### Phase 6 — Background Create + Sticker

- Show backgrounds.
- Show flat sticker list.
- Preview GIF stickers.
- Add sticker to editor.
- Export static frame.

### Phase 7 — Full Editor

- Text.
- Sticker transform.
- Photo transform inside placeholder.
- My Designs.
- Favorites.
- Export history.
- Settings.
- Clear cache.

### Phase 8 — Video Live Wallpaper Service

Later phase only, not MVP:

- `VideoLiveWallpaperService`.
- `VideoLiveWallpaperEngine`.
- Render `.mp4` realtime.
- Optimize FPS/RAM/battery.
- Pause on screen off.
- Release player correctly.
- Set through Android live wallpaper picker.

---

## 23. Testing Checklist

Required unit tests:

- Parse `data_url_full` into `RemoteCategoryDto`.
- Missing rank still parses.
- `type = 2d` maps to static wallpaper.
- `type = live` maps `content -> videoUrl`.
- Live item does not map `content` into `imageUrl`.
- Live item without `content` has `videoUrl = null` and disables live action.
- `type = 2d` without `data` uses `thumb` as `imageUrl`.
- Category `DIY` is separated from normal wallpaper.
- DIY item without `diy_data` is skipped.
- `diy-live` / `diy-static` map correctly.
- `bgcreate.json` maps `category_rank -> categoryRank -> rank`.
- `stickers.json` parses flat list.
- GIF sticker detection works.
- `diy_data` parses width/height/background/elements.
- `Picture` is not treated as placeholder.
- `Image` derives placeholder.
- Element sorted by `layoutIndex`.
- Asset URL resolver works.
- Background resolver handles hex, URL, file name, blank.
- Placeholder ID is stable.
- Export render mode does not draw placeholder indicator.
- Cache + sync failure uses cache when available.

---

## 24. Important Do / Do Not

Do:

- Keep DTOs nullable/default where remote data can change.
- Store raw JSON for debugging.
- Skip invalid items rather than crashing the whole sync.
- Use Room cache first when available.
- Keep live video wallpaper service out of MVP unless explicitly required.
- Render static wallpaper export on background dispatcher.
- Treat `DIY` as a separate feature, not wallpaper list.

Do not:

- Do not hardcode endpoint URLs in Kotlin repositories.
- Do not treat NDK endpoint hiding as real security.
- Do not resolve `#FFFFFF` as URL.
- Do not map live `content` into `imageUrl`.
- Do not treat `Picture` elements as photo placeholders.
- Do not assume sticker categories exist.
- Do not export placeholder indicator into final image.
- Do not download all assets during metadata sync.
- Do not run bitmap export on Main Thread.

---

## 25. Current MVP Definition

MVP includes:

- Remote sync for 3 endpoints.
- Room cache.
- Category list.
- Static wallpaper list/detail/download/set.
- Live wallpaper items shown by thumbnail, with video URL stored.
- Live wallpaper set action disabled unless later service is implemented.
- DIY template list.
- DIY static render from real `diy_data`.
- User photo insertion into `Image` placeholders.
- Static export.
- Background create list.
- Flat sticker list with GIF preview.
- Error model and cache/sync fallback.

MVP does not require:

- Real video live wallpaper service.
- Animated GIF export.
- Full animation schema for `diy_animation`.
- Sticker categories.
- Premium backend / signed URLs.
- Advanced photo transform inside placeholder.

When implementing or modifying code:
- Always follow this AGENT.md first.
- Do not invent new remote fields unless verified from real JSON.
- If JSON structure is uncertain, add nullable/default DTO fields and mapper fallback.
- Prefer skipping invalid remote items over crashing sync.
- Keep MVP scope; do not implement phase-later features unless explicitly requested.