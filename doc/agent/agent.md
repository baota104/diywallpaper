# AGENT.md - DIY Wallpaper Maker & Photo Collage

## 1. Project Context

This project is an Android app named **DIY Wallpaper Maker & Photo Collage**.

The product currently supports:

- Browse wallpaper categories from CDN metadata.
- Show static and live wallpaper items in Home and Collection.
- Preview static wallpapers and video live wallpapers.
- Set static wallpapers through Android wallpaper APIs.
- Set video live wallpapers through the app live wallpaper service.
- Browse DIY templates.
- Open DIY templates through the same preview carousel as normal wallpapers.
- Enter the editor from DIY preview by clicking Edit.
- Create wallpapers from scratch directly from Home.
- Edit wallpaper designs with background, imported photos, text, stickers, brush, and text brush.
- Use API backgrounds and API sticker GIFs inside the editor.
- Import local photos and crop by ratio before adding them as photo layers.
- Transform non-background layers by drag, scale, and rotate.
- Export custom static designs as images.
- Export custom animated designs as video live wallpapers when animated sticker/GIF content is present.
- Save user designs only when the user confirms the product flow, such as Next or explicit save from exit dialog.
- Reopen, preview, set, delete, and list saved designs from Collection.
- Favorite static wallpaper, live wallpaper, and DIY template items through local cache state.
- Show Favorites and Designs in Collection.

The app is built with **Kotlin**, **Jetpack Compose**, **Clean Architecture + MVVM**, **Coroutines/Flow**, **Hilt**, **Retrofit/OkHttp**, **Kotlin Serialization**, **Room**, **DataStore**, **Coil**, Android **WallpaperManager**, and Android **WallpaperService**.

Use **C/C++ NDK** only to make endpoint extraction harder. Do not treat CDN endpoints as secrets.

---

## 2. Core Product Rules For Agents

- This is a product implementation, not a prototype plan.
- Do not describe product work as limited prototype scope.
- Do not push completed product behavior back into speculative future work.
- Before changing behavior, inspect the current code path first.
- Prefer extending existing architecture over adding parallel helper stacks.
- Do not create duplicate render pipelines unless there is a clear technical reason.
- When fixing rendering, keep Edit, Preview, pre-set Preview, export image, and export video aligned.
- Keep changes narrow. Do not break existing static wallpaper, live wallpaper, editor, Collection, or favorite behavior.
- Compile after implementation changes unless the user explicitly asks for analysis only.
- For UI tasks, follow `doc/skill/UISkill.md`.

---

## 3. Core CDN Endpoints

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
- `data_stickers_url` contains a flat sticker list.
- Category `DIY` must not be handled as a normal wallpaper category.
- `type = "2d"` means static wallpaper.
- `type = "live"` means video live wallpaper.
- For `type = "live"`, `thumb` is preview image/webp and `content` is the `.mp4` video URL.
- For DIY items, `diy_data` points to template layout JSON and `diy_animation` may point to animation JSON.

---

## 4. Architecture Rules

Use Clean Architecture + MVVM.

```text
Presentation Layer
        -> ViewModel
        -> UseCase
        -> Repository
        -> Remote CDN / Room / DataStore / File Cache / Native Config
```

Rules:

- UI must not call endpoints directly.
- UI calls ViewModel.
- ViewModel calls UseCase.
- UseCase calls Repository.
- Repository uses `EndpointProvider`, not `NativeConfig` directly.
- Data layer returns `AppResult<T>` / `AppError`, not raw exceptions to UI.
- Long-running bitmap, GIF, and video export work must run on `Dispatchers.Default` or `Dispatchers.IO`, never Main.
- UI state should be observable through `StateFlow`.
- Lists should be cache-first where local Room data exists.
- Background sync should preserve user local state such as favorite flags.

---

## 5. Endpoint Provider / NDK Rules

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
- Use backend or signed URLs if true premium asset protection is needed.

---

## 6. Data Contract: `data_url_full`

### Category DTO

```kotlin
@Serializable
data class RemoteCategoryDto(
    val category: String = "",
    val rank: Int = Int.MAX_VALUE,
    val icon: String? = null,
    val items: List<RemoteItemDto> = emptyList()
)
```

### Item DTO

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

## 7. Wallpaper Mapping

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
other/null    -> UNKNOWN
```

Domain model includes favorite state:

```kotlin
data class WallpaperItem(
    val id: String,
    val categoryId: String,
    val type: WallpaperType,
    val rank: Int,
    val thumbUrl: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val isFavorite: Boolean
)
```

Rules:

- `thumbUrl`: preview image for both static and live wallpaper.
- `imageUrl`: static image URL for `type = "2d"`.
- `videoUrl`: `.mp4` URL from `content` for `type = "live"`.
- For `type = "2d"`, use `data` as `imageUrl` if available; otherwise use `thumb`.
- For `type = "live"`, map `content` to `videoUrl`.
- Never map `content` into `imageUrl`.
- If live item has `thumb` but no `content`, show thumbnail but disable video-dependent actions.
- If item has no usable `thumb`, skip it from UI.
- Preserve `isFavorite` across remote sync.

---

## 8. DIY Template Mapping

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

Domain model includes favorite state:

```kotlin
data class DiyTemplate(
    val id: String,
    val type: DiyTemplateType,
    val rank: Int,
    val thumbUrl: String,
    val diyDataUrl: String,
    val diyAnimationUrl: String?,
    val isFavorite: Boolean
)

enum class DiyTemplateType {
    DIY_STATIC,
    DIY_LIVE
}
```

Rules:

- DIY item without valid `diy_data` must be skipped.
- Unknown/null DIY type falls back to `DIY_STATIC`.
- Empty `diy_animation` means no animation URL.
- Missing `thumb` is allowed; UI should show a placeholder thumbnail.
- Preserve `isFavorite` across remote sync.
- DIY is included in preview carousel by `PreviewSourceType.DIY`.
- DIY opens editor only after the user clicks Edit from preview.

---

## 9. Data Contract: `bgcreate.json`

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

Rules:

- `data` is used as both `imageUrl` and `thumbnailUrl`.
- If `data` is blank, skip the item.
- Do not assume fields like `category`, `imageUrl`, `thumbnailUrl`, `isPremium`, `width`, or `height`; they are not present in the current schema.

---

## 10. Data Contract: `stickers.json`

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

Rules:

- Sticker list is flat.
- `stickers` maps to `stickerUrl` and `thumbnailUrl`.
- `isAnimated` is true when URL ends with `.gif`, ignoring case.
- If `stickers` is blank, skip the item.
- UI should preview animated GIF stickers where practical.
- Editor canvas may show a static frame for performance.
- Device preview and set wallpaper output must include animated sticker behavior when the design is exported as live wallpaper video.

---

## 11. Data Contract: `diy_data`

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

Do not assume unverified remote fields such as:

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

## 12. DIY Asset / Background Rules

`Picture` elements only have `srcName`, not full URL.

Resolve rule:

```text
assetBaseUrl = parent directory of diyDataUrl
assetUrl = assetBaseUrl + "/" + srcName
```

`background` in `diy_data` may be:

- Hex color, for example `#FFFFFF` or `#FFFFFFFF`.
- Full URL.
- File name relative to the `diy_data` directory.
- Blank.

Rules:

- Never resolve hex colors as URLs.
- `https://...` is remote URL.
- `background.webp` is relative asset file.
- Blank background becomes empty/default fallback.

---

## 13. Editor Product Rules

Supported tools:

- Background from API backgrounds, solid/custom colors, and local image.
- Import photo from picker, then crop by ratio before adding it as `PhotoLayer`.
- Text with font library, text preset library, and color picker.
- Sticker from API sticker list, including animated GIF stickers.
- Brush with solid, dashed, outline, glow, and pattern/icon brush styles.
- Text brush with font, color, and size controls.
- Layer panel with background fixed at bottom and other layers reorderable.
- Non-background layers support selection, remove, drag, scale, and rotate.
- Background is fixed and not directly transformed as a layer.

Save behavior:

- New design should not be persisted just by entering the editor.
- New design is persisted when the user clicks Next or explicitly saves from the exit dialog.
- Existing design opened from Collection can autosave updates.
- Back from unsaved new editor should show a save/exit dialog.
- Collection should not show incomplete white draft cards.

Layer and transform rules:

- Use the shared geometry/render helpers where possible.
- Do not create separate coordinate math for each screen unless necessary.
- Frame/selection handles are UI only; they must not change exported content.
- Selection frame, icons, and stroke width should remain visually stable while content scales.
- Brush/text/text-brush bounds must be computed from actual rendered content, including worst-case long text and irregular drawn paths.

---

## 14. Render / Export Consistency Rules

Edit, editor preview, device preview before set, static export, and live video export must agree on:

- Base design coordinate space.
- Layer order.
- Layer transform pivot.
- Layer scale.
- Layer rotation.
- Center-crop / aspect-fill behavior.
- Font resolution.
- Photo crop spec.
- Brush/text-brush path bounds.
- GIF frame rendering for live output.

Rules:

- Store design data in a stable model coordinate space.
- Map model coordinates to target viewport through shared helpers.
- Do not use one transform implementation in Compose and a different one in export unless both are explicitly proven equivalent.
- Static wallpaper export should use the same render semantics as preview.
- Custom live wallpaper export should render frames through the same layer semantics as preview.
- If preview and set output differ, inspect export/render path before changing editor UI.

---

## 15. Wallpaper Set Rules

Static wallpaper:

```text
Open static item or custom static design
  -> Show preview
  -> Resolve/download image if needed
  -> Set with Android wallpaper APIs
```

Video live wallpaper:

```text
Open live item
  -> Show video/thumbnail preview
  -> Resolve mp4 URL or local mp4
  -> Configure live wallpaper service
  -> Launch Android live wallpaper picker
```

Custom animated design:

```text
Open saved/edited design
  -> Detect animated content such as GIF sticker or live DIY animation
  -> Export design frames to mp4
  -> Configure live wallpaper service with exported mp4
  -> Launch Android live wallpaper picker
```

Rules:

- Do not break normal static wallpaper set while changing custom design export.
- Do not break normal video live wallpaper set while changing custom design export.
- If custom live output is white or static, inspect the video exporter and asset resolver first.

---

## 16. Collection Rules

Collection has two filters:

- Favorites.
- Designs.

Favorites:

- Observe local favorite state from wallpaper and DIY cache.
- Use `HomeFeedItem` for favorite list items.
- `HomeFeedItem.WallpaperEntry` renders wallpaper card.
- `HomeFeedItem.DiyEntry` renders DIY card.
- Unfavoriting an item should remove it from Favorites through Flow emission.

Designs:

- Observe saved user designs.
- Design card should show exported/preview/thumbnail content full-card.
- Design card delete uses common confirm dialog.
- Delete action calls `DeleteDesignUseCase` only after confirmation.
- Opening a design goes to editor for that saved design.

---

## 17. UI Rules

Always follow `doc/skill/UISkill.md`.

Rules:

- Compose only. No XML layout.
- Use state wrapper + pure content split.
- Root screen content should use `Scaffold`.
- All user-visible text must use `stringResource`.
- Do not hardcode colors in UI. Add colors to `ui/theme/Color.kt` when needed.
- Prefer shared UI components in `ui/common` or `ui/components`.
- Use `img_bg_app.png` as app background where the current Home/Collection style requires it.
- Keep UI changes separate from domain/render logic unless the feature requires both.
- For common dialogs, use common UI components instead of screen-specific dialog copies.

---

## 18. Cache / Sync Policy

Metadata endpoints:

```text
data_url_full
data_bgcreate_url
data_stickers_url
```

Policy:

- Cache metadata in Room.
- Store `lastSyncedAt` in DataStore.
- If no cache exists, remote sync is required.
- If cache is fresh, use cache.
- If cache is stale, show cache first and sync in background.
- If sync fails but cache exists, keep using cache.
- If sync fails and no cache exists, show error.
- Preserve local favorite flags during metadata refresh.
- Do not download all assets during metadata sync.
- Download/resolve assets on demand.

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

## 20. Invalid Item Rules

Apply these during mapper/sync:

```text
Wallpaper type = 2d:
- If thumb blank and data blank -> skip item.
- If thumb exists but data blank -> imageUrl = thumb.

Wallpaper type = live:
- If thumb blank -> skip item.
- If content blank -> show thumb but disable video-dependent actions.

DIY:
- If diyData blank -> skip item.
- If thumb blank -> keep item and show placeholder thumbnail.

BackgroundCreate:
- If data blank -> skip item.

Sticker:
- If stickers blank -> skip item.
```

---

## 21. Testing Checklist

Required and useful tests:

- Parse `data_url_full` into `RemoteCategoryDto`.
- Missing rank still parses.
- `type = 2d` maps to static wallpaper.
- `type = live` maps `content -> videoUrl`.
- Live item does not map `content` into `imageUrl`.
- Live item without `content` has `videoUrl = null`.
- `type = 2d` without `data` uses `thumb` as `imageUrl`.
- Category `DIY` is separated from normal wallpaper.
- DIY item without `diy_data` is skipped.
- `diy-live` / `diy-static` map correctly.
- Favorite flags survive metadata refresh.
- `bgcreate.json` maps `category_rank -> rank`.
- `stickers.json` parses flat list.
- GIF sticker detection works.
- `diy_data` parses width/height/background/elements.
- `Picture` is not treated as placeholder.
- `Image` derives placeholder.
- Element sorting by `layoutIndex` works.
- Asset URL resolver works.
- Background resolver handles hex, URL, file name, blank.
- Placeholder ID is stable.
- Export render mode does not draw placeholder indicator.
- Collection observes favorite feed items.
- Collection observes user designs.
- Delete design delegates to repository/use case after confirmation.
- Editor new design is not persisted just by opening the editor.
- Preview and export stay aligned for text, photo, sticker, brush, and text brush.

---

## 22. Important Do / Do Not

Do:

- Keep DTOs nullable/default where remote data can change.
- Store raw JSON for debugging where useful.
- Skip invalid items rather than crashing the whole sync.
- Use Room cache first when available.
- Preserve local favorite state during refresh.
- Render static and video export on background dispatchers.
- Treat `DIY` as a separate feature, not a normal wallpaper category.
- Reuse existing render/geometry helpers before adding new ones.
- Compile and run relevant tests after implementation changes.

Do not:

- Do not hardcode endpoint URLs in Kotlin repositories.
- Do not treat NDK endpoint hiding as real security.
- Do not resolve `#FFFFFF` as URL.
- Do not map live `content` into `imageUrl`.
- Do not treat `Picture` elements as photo placeholders.
- Do not assume sticker categories exist.
- Do not export placeholder indicator into final image/video.
- Do not download all assets during metadata sync.
- Do not run bitmap/video export on Main Thread.
- Do not create incomplete saved design cards when the user only opened a new editor.
- Do not add parallel render paths for the same layer behavior without proving equivalence.
