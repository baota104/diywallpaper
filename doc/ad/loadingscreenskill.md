# Resume Loading Screen Pattern

## Mục tiêu

Đây là pattern tổng quát cho các project cần hiện một màn trung gian khi app quay lại từ background, nhưng không được phá hỏng các flow và điều kiện đang chạy trong app.

Pattern này không phụ thuộc vào logic ads cụ thể.  
Nó chỉ mô tả cách hiện một `loading screen` an toàn ở cấp app lifecycle.

## Khi nào nên dùng

Dùng pattern này khi cần:

- hiện một màn loading tạm thời sau khi app resume
- chèn một flow ngắn trước khi trả user về màn trước đó
- tránh mở đè lên các màn nhạy cảm như splash, onboarding, auth, dialog flow
- tránh loop mở loading screen liên tục

## Nguyên tắc chính

1. Không để từng screen tự quyết định mở loading screen.
2. Bắt lifecycle ở cấp app hoặc process.
3. Dùng một coordinator/manager riêng để quyết định có mở màn hay không.
4. Có blacklist route/screen để không chen vào các flow nhạy cảm.
5. Có cờ chống mở lặp và chống resume-loop.
6. Màn loading chỉ làm đúng 1 việc rồi tự đóng sạch.

## Kiến trúc khuyến nghị

### 1. App-level lifecycle observer

Bắt lifecycle ở cấp app:

- `ProcessLifecycleOwner`
- hoặc `Application.ActivityLifecycleCallbacks`

Mục tiêu:

- biết app vừa xuống background
- biết app vừa resume lại

### 2. Coordinator / Navigation Manager

Tạo một manager riêng, ví dụ:

- `ResumeFlowManager`
- `LoadingScreenCoordinator`

Manager này nên giữ:

- `shouldOpenOnNextResume`
- `isFlowActive`
- `currentRoute`
- danh sách route bị chặn

Manager là nơi duy nhất quyết định:

- có được mở loading screen không
- đang ở route nào
- đã có flow active chưa

### 3. Loading screen riêng

Tạo một screen riêng cho flow trung gian:

- không nhét logic này vào splash
- không nhét vào home
- không nhét vào màn đang hiển thị trước đó

Màn riêng giúp:

- dễ quản lý lifecycle
- dễ thêm timeout
- dễ đóng flow sạch

## Điều kiện mở màn an toàn

Chỉ nên mở loading screen khi đủ tất cả điều kiện:

- app thực sự vừa quay lại từ background
- chưa có flow loading khác đang active
- route hiện tại không nằm trong blacklist
- không đang ở một flow fullscreen nhạy cảm

## Route blacklist nên có

Tùy project, nhưng thường nên chặn các màn như:

- splash
- onboarding
- authentication
- language/apply language
- chính loading screen đó
- fullscreen modal flow

Mục tiêu:

- không chen vào flow khởi động
- không chen vào flow setup ban đầu
- không mở loading screen lồng nhau

## Vòng đời flow chuẩn

### Khi app đi background

Manager ghi nhận:

- app đã đi background
- lần resume tiếp theo có thể cần mở loading screen

### Khi app resume

Manager kiểm tra:

- đang ở route nào
- có bị blacklist không
- flow đã active chưa

Nếu hợp lệ:

- phát event điều hướng tới loading screen

### Khi loading screen mở

Màn loading nên:

1. đánh dấu flow đang active
2. chạy tác vụ trung gian cần thiết
3. có timeout bảo vệ
4. khi xong thì đóng flow
5. clear cờ active

## Timeout pattern

Nên luôn có timeout cho loading screen.

Mục tiêu:

- tránh kẹt màn nếu callback không trả về
- tránh user bị giữ lại vô thời hạn

Nguyên tắc:

- timeout chỉ để tự thoát flow an toàn
- khi timeout xảy ra, luôn clear cờ active trước khi đóng

## Hàm kết thúc flow

Nên gom về một hàm kiểu:

`finishFlow()`

Hàm này nên chịu trách nhiệm:

- chống gọi nhiều lần
- clear cờ active trong manager
- đóng màn loading

Đây là điểm quan trọng để không phá các điều kiện khác.

## Những lỗi phổ biến cần tránh

### 1. Mở loading screen từ từng screen riêng lẻ

Lỗi này làm logic bị phân tán và dễ xung đột.

### 2. Không có blacklist

Dễ làm loading screen chen vào splash, onboarding hoặc auth flow.

### 3. Không có cờ `isFlowActive`

Dễ bị mở lặp nhiều lần khi lifecycle callback chạy liên tiếp.

### 4. Không có timeout

Nếu callback không trả về thì màn bị treo.

### 5. Không có `finishFlow()` tập trung

Dễ quên clear state và làm flow bị kẹt ở những lần resume sau.

## Pseudocode tổng quát

```kotlin
object ResumeFlowManager {
    var shouldOpenOnNextResume = false
    var isFlowActive = false
    var currentRoute: String? = null

    fun onAppWentToBackground() {
        shouldOpenOnNextResume = true
    }

    fun shouldOpen(): Boolean {
        if (!shouldOpenOnNextResume) return false
        if (isFlowActive) return false
        if (currentRoute in blockedRoutes) return false
        return true
    }

    fun markFlowOpened() {
        shouldOpenOnNextResume = false
        isFlowActive = true
    }

    fun markFlowClosed() {
        isFlowActive = false
    }
}
```

```kotlin
fun finishFlow() {
    if (alreadyFinished) return
    alreadyFinished = true
    ResumeFlowManager.markFlowClosed()
    closeScreen()
}
```

## Tóm tắt

Pattern an toàn là:

- bắt lifecycle ở cấp app
- để manager riêng quyết định mở màn
- dùng blacklist route
- có cờ chống mở lặp
- có timeout
- có `finishFlow()` tập trung

Phần tác vụ thực sự chạy trong loading screen có thể thay đổi theo từng project:

- ad flow
- sync dữ liệu
- refresh token
- prefetch nội dung
- kiểm tra state hệ thống

Nhưng khung điều phối hiển thị màn nên giữ như pattern này để không phá các điều kiện khác.
