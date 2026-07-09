# Global UI Skill — Jetpack Compose Architecture Standard
**Purpose:** This file defines the universal UI design and architectural rules for ALL Android projects using Jetpack Compose.
**Role of AI Agent/Developer:** Always strictly follow these rules when creating, editing, or refactoring UI code. No exceptions.

---

## 1. Nguyên tắc Cốt lõi & Ràng buộc Cứng (Hard Constraints)
Đây là những luật cấm tuyệt đối vi phạm trong quá trình sinh code:
*   **100% Jetpack Compose:** Không sử dụng XML layout.
*   **MANDATORY LOCALIZATION (i18n):** Tuyệt đối không hardcode text (VD: `Text("Đăng nhập")`). Mọi chuỗi ký tự hiển thị trên UI phải sử dụng `stringResource(id = R.string.xxx)`.
*   **MANDATORY RESPONSIVE:** Không fix cứng kích thước (VD: `Modifier.width(300.dp)` hoặc `height(500.dp)`) cho các container chính. Phải sử dụng `Modifier.fillMaxWidth()`, `Modifier.weight()`, và `aspectRatio()` để UI tự thích ứng với các kích thước màn hình khác nhau (Điện thoại, Tablet, Foldable).
*   **NO HARDCODED COLORS:** Tuyệt đối không sử dụng mã màu trực tiếp tại UI (VD: `Color(0xFF000000)` hoặc `Color.Red`).
    *   Luôn gọi màu thông qua Package `theme` (Ví dụ: `MaterialTheme.colorScheme.primary` hoặc custom `AppTheme.colors.background`).
    *   Nếu UI yêu cầu một màu chưa có, **BẮT BUỘC** phải thêm màu đó vào file `Color.kt`, sau đó ánh xạ vào Theme, không được tự ý khai báo cục bộ tại file UI.

## 2. Kiến trúc Màn hình (Screen Architecture)
Mọi màn hình (Screen) phải tuân thủ nghiêm ngặt quy tắc tách biệt **State Wrapper** và **Pure UI Content**.

### 2.1. Screen Wrapper (State & Logic)
Chỉ chịu trách nhiệm gọi ViewModel, thu thập State và điều hướng (Navigation). Không chứa logic giao diện phức tạp.
```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    // 1. Collect State
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 2. Delegate to Content
    LoginContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToHome = onNavigateToHome
    )
}
2.2. Screen Content (Pure UI & Mandatory Scaffold)
LUẬT BẮT BUỘC: Mọi hàm Content của màn hình gốc đều phải có Scaffold làm root layout để dễ dàng quản lý TopAppBar, BottomNavigationBar, FloatingActionButton, và Snackbar.

Không truy cập ViewModel trong hàm này.

Chỉ nhận Data/State và trả ra Event/Action.

Kotlin
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onNavigateToHome: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(id = R.string.title_login))
        },
        snackbarHost = { SnackbarHost(uiState.snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // LUÔN LẤY TỪ THEME
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // UI Components...
        }
    }
}
3. Quản lý Trạng thái (State & Data Flow)
Áp dụng mô hình Unidirectional Data Flow (UDF).

UI State: Mỗi màn hình có một data class duy nhất gom toàn bộ trạng thái.

Kotlin
data class LoginUiState(
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val isLoginSuccess: Boolean = false
)
UI Event: Gom nhóm các hành động của người dùng thành sealed class/interface để gửi về ViewModel.

Kotlin
sealed interface LoginEvent {
    data class OnEmailChanged(val email: String) : LoginEvent
    object OnSubmitClick : LoginEvent
}
4. Hệ thống Design Token & Theming (Package: theme)
Tất cả các thành phần trực quan phải được tham chiếu từ package ui/theme.

Color.kt: Khai báo mã HEX tại đây. Sử dụng Semantic Naming (Primary, Background, Error, Surface...) thay vì Literal Naming (Red, Blue).

Typography.kt: Định nghĩa chuẩn Font chữ. Tại UI, luôn gọi qua MaterialTheme.typography.titleLarge, bodyMedium, labelSmall. Tuyệt đối không dùng fontSize = 16.sp trực tiếp trên UI.

Shapes.kt & Dimensions.kt: Tạo file Dimensions.kt để quản lý khoảng cách (VD: LocalSpacing.current.spaceMedium) nếu dự án yêu cầu quy chuẩn Padding khắt khe.

5. Thư viện Component Cốt lõi (Core UI Components)
Tái sử dụng bằng cách tạo các Custom Wrappers trong ui/common hoặc ui/components.

Thay vì gọi trực tiếp Button của Compose, hãy tạo AppPrimaryButton, AppOutlinedButton với các thông số mặc định của dự án (shape, màu sắc, typography).

Thay vì gọi TextField, hãy tạo AppTextField chứa sẵn cấu hình hiển thị lỗi (Error State) và khoảng cách chuẩn.

6. Xử lý Trạng thái Chung (Standard UI States)
Mọi luồng dữ liệu đều phải tính đến 3 trạng thái:

Loading: Sử dụng CircularProgressIndicator ở giữa màn hình hoặc hiệu ứng Skeleton/Shimmer cho các dạng danh sách.

Error: Luôn có UI để hiển thị lỗi, ưu tiên dùng Snackbar cho lỗi nhẹ và Error Layout (có nút Thử lại) cho lỗi chặn hiển thị (VD: mất mạng khi tải trang đầu).

Empty: Khi danh sách trả về rỗng, luôn hiển thị EmptyState component (chứa 1 Illustration/Icon và text hướng dẫn).