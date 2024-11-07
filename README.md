# kotlin-convenience-store-precourse

## 기능 요구사항

### 주제 : 편의점 결제 시스템 🏪

## 📂 파일 입출력 프로세스

+ 상품 목록 파일을 불러온다
+ 파일 경로 : src/main/resources/products.md
+ 행사 목록 파일을 불러온다
+ 파일 경로 : src/main/resources/promotions.md
+ 두 파일 모두 내용의 형식을 유지한다면 값은 수정할 수 있다.

## 🧑🏻‍🎤사용자 구매 프로세스

+ 구매할 상품과 수량을 입력 받는다.
+ 상품명, 수량은 하이픈(-)으로 구분한다.
+ 개별 상품은 대괄호([])로 묶어 쉼표(,)로 구분한다.
  입력 예제

```
[콜라-10],[사이다-3]
```

## 📝재고 관리 프로세스

+ 각 상품의 재고 수량을 고려하여 결제 가능 여부를 확인한다.
+ 고객이 상품을 구매할 때마다, 결제된 수량만큼 해당 상품의 재고에서 차감한다.
+ 재고를 차감함으로써 시스템은 최신 재고 상태를 유지한다.
+ 다음 고객이 구매할 때 정확한 재고 정보를 제공한다.

## 🏷️ 프로모션 프로세스

+ 오늘 날짜가 프로모션 기간 내에 포함된 경우에만 할인을 적용한다.
+ 프로모션은 N개 구매 시 1개 무료 증정(ex. 2 + 1)의 형태로 진행된다.
+ 1+1 또는 2+1 프로모션이 각각 지정된 상품에 적용된다.
+ 동일 상품에 여러 프로모션이 적용되지 않는다.
+ 프로모션 혜택은 프로모션 재고 내에서만 적용할 수 있다.
+ 프로모션 기간 중이라면 프로모션 재고를 우선적으로 차감한다.
+ 프로모션 재고가 부족할 경우에는 일반 재고를 사용한다.
+ 프로모션 적용이 가능한 상품에 대해 고객이 해당 수량보다 적게 가져온 경우, 필요한 수량을 추가로 가져오면 혜택을 받을 수 있음을 안내한다
    + 그 수량만큼 추가 여부를 입력받는다.
   ```
   현재 {상품명}은(는) 1개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)
   ```
+ 프로모션 재고가 부족하여 일부 수량을 프로모션 혜택 없이 결제해야 하는 경우, 일부 수량에 대해 정가로 결제하게 됨을 안내한다.
    + 일부 수량에 대해 정가로 결제할지 여부에 대한 안내 메시지를 출력한다.
   ```
    현재 {상품명} {수량}개는 프로모션 할인이 적용되지 않습니다. 그래도 구매하시겠습니까? (Y/N)
   ```

## 🏅 멤버십 할인 프로세스

+ 멤버십 회원은 프로모션 미적용 금액의 30%를 할인받는다.
+ 프로모션 적용 후 남은 금액에 대해 멤버십 할인을 적용한다.
+ 멤버십 할인의 최대 한도는 8,000원이다.
+ 멤버십 할인 적용 여부를 확인하기 위해 안내 문구를 출력한다.
+ Y: 멤버십 할인을 적용한다.
  N: 멤버십 할인을 적용하지 않는다.

  ```
    + 멤버십 할인을 받으시겠습니까? (Y/N)
  ```

## 🧮 계산 프로세스

+ 사용자가 입력한 상품의 가격과 수량을 기반으로 최종 결제 금액을 계산한다.
+ 총구매액은 상품별 가격과 수량을 곱하여 계산한다.
+ 프로모션 및 멤버십 할인 정책을 반영하여 최종 결제 금액을 산출한다.
+ 구매 내역과 산출한 금액 정보를 영수증으로 출력한다.
+ 영수증 출력 후 추가 구매를 진행할지 또는 종료할지를 선택할 수 있다.

## 🧾 영수증 출력 프로세스

+ 영수증은 고객의 구매 내역과 할인을 요약하여 출력한다.
+ 영수증 항목은 아래와 같다.
   ```
   구매 상품 내역: 구매한 상품명, 수량, 가격
   증정 상품 내역: 프로모션에 따라 무료로 제공된 증정 상품의 목록
   금액 정보
   총구매액: 구매한 상품의 총 수량과 총 금액
   행사할인: 프로모션에 의해 할인된 금액
   멤버십할인: 멤버십에 의해 추가로 할인된 금액
   내실돈: 최종 결제 금액
   ```
+ 영수증의 구성 요소를 보기 좋게 정렬하여 고객이 쉽게 금액과 수량을 확인할 수 있게 한다.
+ 추가 구매 여부를 입력 받는다.
    + Y: 재고가 업데이트된 상품 목록을 확인 후 추가로 구매를 진행한다.
    + N: 구매를 종료한다.

## ❌ 프로세스별 에러 예측 및 요구사항

+ 사용자가 잘못된 값을 입력할 경우 IllegalArgumentException을 발생시킨다.
+ "[ERROR]"로 시작하는 에러 메시지를 출력 후 그 부분부터 입력을 다시 받는다.
+ Exception이 아닌 IllegalArgumentException, IllegalStateException 등과 같은 명확한 유형을 처리한다.

```
에러 메시지 출력 예제
[ERROR] 로또 번호는 1부터 45 사이의 숫자여야 합니다.
```

### 1. 정수형 공통 에러 처리

| 상황                        | 에러 메시지                               | 타입            |
|---------------------------|--------------------------------------|---------------|
| 공백인 경우                    | ```[ERROR] 잘못된 입력입니다. 다시 입력해 주세요.``` | `INVALID_INPUT` |
| 숫자가 아닌 경우                 | ```[ERROR] 잘못된 입력입니다. 다시 입력해 주세요.``` | `INVALID_INPUT` |
| 정수형으로 표현할 수 있는 범위를 벗어난 경우 | ```[ERROR] 잘못된 입력입니다. 다시 입력해 주세요.``` | `INVALID_INPUT` |

### 2. 사용자 구매 프로세스

#### 구입 금액

| 상황                        | 에러 메시지                                            | 타입                     |
|---------------------------|---------------------------------------------------|------------------------|
| 존재하지 않는 상품을 입력한 경우        | ```[ERROR] 존재하지 않는 상품입니다. 다시 입력해 주세요.```          | `NOT_SALES`            |
| 구매 수량이 재고 수량을 초과한 경우      | ```[ERROR] 재고 수량을 초과하여 구매할 수 없습니다. 다시 입력해 주세요.``` | `OUT_OF_STOCK`         |
| 구매할 상품과 수량 형식이 올바르지 않은 경우 | ```[ERROR] 올바르지 않은 형식으로 입력했습니다. 다시 입력해 주세요.```    | `INVALID_INPUT_FORMAT` |
| 기타 잘못된 입력의 경우 | ```[ERROR] 잘못된 입력입니다. 다시 입력해 주세요.```    | `INVALID_INPUT` |

#### 프로모션/계산/멤버십/계속 구매 공통 에러

| 상황                 | 에러 메시지                               | 타입            |
|--------------------|--------------------------------------|---------------|
| Y,N 이외의 숫자가 입력될 경우 | ```[ERROR] 잘못된 입력입니다. 다시 입력해 주세요.``` | `INVALID_INPUT` |

### 프로그래밍 요구사항 체크 리스트

공백 라인도 한 라인으로 간주한다. 함수의 길이가 10라인 이하인가 ?
함수(또는 메서드)가 한 가지 일만 잘 하도록 구현했는가?
비즈니스 로직과 UI 로직이 분리되었는가 ?
상수는 static final 대신 enum을 활용하였는가 ?
val 키워드를 사용해 값의 변경을 막았는가 ?
객체의 상태 접근을 제한했는가 ?
객체는 객체답게 사용했는가 ? (getter, setter만 있는 경우)


