# Hướng Dẫn Chuẩn (SOP): Cấu hình Application, MainActivity & Navigation Graph
Tài liệu này định nghĩa quy trình chuẩn (SOP) để cấu hình các lớp cốt lõi (`Application`, `MainActivity`) và luồng điều hướng (`Navigation Graph`) cho mọi ứng dụng Android sử dụng Jetpack Compose, Hilt và tích hợp đa ngôn ngữ + hệ thống quảng cáo. Việc cấu hình chuẩn từ đầu giúp tránh các lỗi memory leak, crash UI và đứt gãy luồng quảng cáo.
---
## 1. Cấu hình Lớp Application (The App Class)
Lớp `Application` là nơi khởi tạo toàn cục các dịch vụ cốt lõi (Firebase, Ads, Dependency Injection).
### Các bước thiết lập chuẩn:
1. **Dependency Injection**: Luôn đặt annotation `@HiltAndroidApp` trên class để kích hoạt Hilt cho toàn bộ app.
2. **Kế thừa đúng Base Class**: Extend `Application` (hoặc base class của thư viện quảng cáo như `ProxApplication` nếu dự án có quy định).
3. **Thiết lập Đa ngôn ngữ (Localization)**:
    - Ghi đè hàm `attachBaseContext` và bọc context bằng `LocaleManager` (hoặc tiện ích tương đương) để đảm bảo ngôn ngữ được giữ xuyên suốt app.
4. **Khởi tạo dịch vụ trong `onCreate`**:
    - Khởi tạo Firebase: `FirebaseApp.initializeApp(this)`.
    - Áp dụng cấu hình ngôn ngữ đã lưu ngay từ lúc app khởi chạy.
    - Đăng ký và cấu hình thông số SDK Quảng cáo (Ads):
        - Gán khóa Remote Config theo `BuildConfig.VERSION_CODE`.
        - Gọi lệnh `registerOpenAds()` và thiết lập các style native layout (các template giao diện cho Native Ads).
```kotlin
// Ví dụ cấu trúc chuẩn
@HiltAndroidApp
class BaseApp : ProxApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.wrapContext(base))
    }
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // 1. Áp dụng ngôn ngữ
        val lang = LocaleManager.getLocale(this)
        if (lang.isNotBlank()) LocaleManager.applyLocale(this, lang)
        
        // 2. Khởi tạo Ads
        AdsUtils.setKeyRemoteConfig("config_ads_v${BuildConfig.VERSION_CODE}")
        AdsUtils.registerOpenAds()
        // Đăng ký style native (tuỳ thuộc vào từng app)
        // AdsUtils.addStyleNative(101, R.layout.layout_native_bottom)
    }
}
```
---
## 2. Cấu hình MainActivity
`MainActivity` là điểm neo (entry point) cho giao diện Compose. Cần cấu hình chuẩn để hỗ trợ Edge-to-Edge và đa ngôn ngữ.
### Các bước thiết lập chuẩn:
1. **Dependency Injection**: Đặt annotation `@AndroidEntryPoint` để Hilt có thể tiêm các ViewModel.
2. **Đa ngôn ngữ**: Bắt buộc ghi đè `attachBaseContext` giống hệt lớp Application để đảm bảo Activity không bị reset về ngôn ngữ mặc định của máy khi có configuration changes.
3. **Giao diện**:
    - Gọi `enableEdgeToEdge()` trước khi `setContent` để giao diện tràn viền.
    - Bọc NavGraph bên trong `Theme` và `Surface` nền chuẩn của ứng dụng.
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}
```
---
## 3. Cấu hình Navigation Graph (Điều hướng luồng)
Navigation Graph quản lý toàn bộ stack màn hình. Các quy tắc quan trọng nhất xoay quanh việc dọn dẹp Backstack và quản lý trạng thái App Open Ads.
### Các nguyên tắc thiết kế:
1. **Định nghĩa Route rõ ràng**: Luôn sử dụng một `sealed class Screen` tập trung để định nghĩa các route, tránh hardcode chuỗi string rải rác.
2. **Xử lý dọn Backstack (PopUpTo)**:
    - Khi chuyển từ Splash sang Language (hoặc Dashboard), bắt buộc dùng `popUpTo(Screen.Splash.route) { inclusive = true }` để người dùng không thể bấm Back quay lại Splash.
    - Khi chuyển từ Language sang Onboarding (lần đầu), cũng cần pop `Language` khỏi stack.
    - Khi quay lại Language từ Settings, dùng `navController.popBackStack()`.
3. **Điều khiển App Open Ads theo Route**:
    - App Open Ads (quảng cáo khi mở lại app từ background) **tuyệt đối không được bật khi đang ở Splash Screen**, vì nó sẽ đè lên Interstitial Ad của Splash.
    - Sử dụng `LaunchedEffect` quan sát `currentRoute`. Nếu route là `Splash` -> `disableOpenAds()`. Nếu sang các route khác -> `enableOpenAds()`.
```kotlin
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // [QUAN TRỌNG]: Tắt App Open Ads khi ở Splash
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Splash.route) {
            AdsUtils.disableOpenAds()
        } else {
            AdsUtils.enableOpenAds()
        }
    }
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(onFinished = { isFirstTime ->
                val destination = if (isFirstTime) Screen.Language.route else Screen.Dashboard.route
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true } // Chặn back lại Splash
                }
            })
        }
        // 2. Language Screen
        composable(Screen.Language.route) {
            LanguageScreen(onSelected = {
                // Check xem là chọn ngôn ngữ từ Splash hay từ Settings
                if (navController.previousBackStackEntry?.destination?.route == Screen.Dashboard.route) {
                    navController.popBackStack() // Từ Settings -> Back lại
                } else {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Language.route) { inclusive = true }
                    }
                }
            })
        }
        
        // 3. Khai báo các màn hình khác...
    }
}
```
---
## Tổng kết Checklist khi setup dự án mới:
- [ ] Lớp Application đã override `attachBaseContext` cho ngôn ngữ chưa?
- [ ] Application đã có `@HiltAndroidApp` và MainActivity có `@AndroidEntryPoint` chưa?
- [ ] MainActivity đã gọi `enableEdgeToEdge` và ghi đè `attachBaseContext` chưa?
- [ ] Trong NavGraph đã có `LaunchedEffect` khoá Open Ads khi ở Splash chưa?
- [ ] Các logic chuyển khỏi Splash/Language đã kèm theo `popUpTo(inclusive = true)` chưa?
