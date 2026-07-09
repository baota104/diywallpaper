# Applying Language Screen Pattern

## Mục tiêu

Màn `Applying Language` là một màn trung gian ngắn nằm giữa:

- `Language`
- `Onboard 1`

Mục tiêu của màn này:

1. Tạo cảm giác app đang áp dụng ngôn ngữ người dùng vừa chọn.
2. Giữ một khoảng chờ ngắn, có kiểm soát.
3. Tận dụng thời gian này để preload tài nguyên hoặc ad cho màn kế tiếp.
4. Điều hướng tiếp sang `Onboard 1` mà không làm nặng logic của `Language` hoặc `Onboard`.

## Khi nào nên dùng

Dùng pattern này khi cần:

- có một bước chuyển trạng thái giữa hai màn
- muốn tránh nhảy thẳng quá gấp từ chọn ngôn ngữ sang onboarding
- muốn có một khoảng chờ để preload dữ liệu/ad cho màn sau

## Vai trò của màn

`Applying Language` không phải màn nghiệp vụ chính.  
Nó là màn trung gian có thời gian sống ngắn.

Nó chỉ nên làm các việc sau:

- hiển thị UI loading chuyên biệt
- chạy một tác vụ nền ngắn
- chờ timeout cố định
- tự điều hướng tiếp

Không nên nhét business flow dài hoặc điều kiện rẽ nhánh phức tạp vào đây.

## Flow tổng quát

### Bước 1. User hoàn tất ở màn Language

Khi user bấm `Done` ở `Language`:

- app điều hướng sang `Applying Language`

### Bước 2. Màn Applying Language mở ra

Khi màn mở:

- khóa orientation nếu flow yêu cầu
- hiển thị UI loading
- bắt đầu timeout cố định
- đồng thời khởi chạy preload cho màn kế tiếp nếu cần

### Bước 3. Hết timeout

Khi hết thời gian chờ:

- điều hướng sang `Onboard 1`

Nguyên tắc:

- không cần chờ preload hoàn tất mới được đi tiếp
- preload xong sớm thì tốt
- chưa xong thì màn sau tự quyết định tiếp

## Timeout pattern

Timeout của màn nên cố định và ngắn.

Ví dụ pattern đang dùng:

- `3s`

Timeout này phục vụ 2 mục đích:

- tạo cảm giác đang áp dụng cấu hình
- tạo một khoảng đệm để preload

## UI pattern

UI thường gồm:

- background riêng
- card/translucent panel ở giữa
- icon liên quan tới ngôn ngữ hoặc translate
- progress vòng tròn dạng indeterminate
- title + description ngắn

Nguyên tắc:

- UI nên rõ ràng là màn chuyển tiếp
- không thêm CTA hoặc thao tác thừa
- không để user phải quyết định gì thêm ở đây

## Logic preload cho Onboard 1

Đây là phần logic có thể gắn trực tiếp với màn này nếu project cần preload native/ad cho `OB1`.

### Mục tiêu

Trong lúc đang ở `Applying Language`:

- preload ad hoặc resource cho `Onboard 1`

Để khi sang `OB1`:

- ưu tiên dùng dữ liệu/preload đã có
- giảm cảm giác chờ ở màn onboarding

### Pattern xử lý

Nên có một manager riêng cho preload `OB1`, ví dụ:

- `OnboardOb1PreloadManager`

Manager nên giữ các state:

- `IDLE`
- `LOADING`
- `SUCCESS`
- `FAILED`

### Khi vào Applying Language

Màn gọi:

1. reset session preload cũ
2. bắt đầu preload cho `OB1`
3. chạy timeout `3s`

### Khi sang OB1

`OB1` nên xử lý theo thứ tự:

1. nếu preload `SUCCESS`
    - dùng kết quả preload
2. nếu preload vẫn `LOADING`
    - tiếp tục chờ callback
    - không vội rơi sang luồng cũ
3. nếu preload `FAILED`
    - mới rơi sang luồng fallback cũ

Nguyên tắc này giúp:

- không load trùng quá sớm
- không bỏ phí preload đang chạy dở

## Lợi ích của việc tách màn

So với việc để `Language` tự delay rồi sang `Onboard`, màn riêng này tốt hơn vì:

- dễ quản lý timeout
- dễ preload tài nguyên cho `OB1`
- dễ tinh UI chuyển tiếp
- không làm `LanguageScreen` phình logic
- không làm `OnboardScreen` phải biết quá nhiều về bước trước đó

## Những lỗi phổ biến cần tránh

### 1. Để màn này quyết định quá nhiều flow

Màn này nên ngắn, đơn nhiệm.

### 2. Bắt buộc phải chờ preload xong mới cho đi tiếp

Điều này dễ làm user bị kẹt nếu preload chậm.

### 3. Không reset session preload

Dễ làm callback cũ ghi đè flow mới.

### 4. Rơi về fallback quá sớm ở OB1

Nếu preload vẫn đang `LOADING`, nên tiếp tục chờ thay vì load chồng ngay.

## Pseudocode tổng quát

```kotlin
LaunchedEffect(Unit) {
    preloadManager.reset()
    preloadManager.preloadForOb1()

    delay(3000L)
    navigateToOnboard1()
}
```

```kotlin
when (preloadState) {
    SUCCESS -> usePreloadedResult()
    LOADING -> waitForCallback()
    FAILED -> useFallback()
    IDLE -> useFallback()
}
```

## Tóm tắt

Pattern `Applying Language` nên là:

- một màn trung gian ngắn
- có timeout cố định
- UI loading rõ ràng
- tận dụng để preload cho `Onboard 1`
- `Onboard 1` ưu tiên dùng preload, chỉ fallback khi preload fail thật sự

Đây là pattern có thể tái dùng cho các bước chuyển tiếp tương tự, không chỉ riêng ngôn ngữ, miễn là cần một màn đệm ngắn trước màn kế tiếp.
