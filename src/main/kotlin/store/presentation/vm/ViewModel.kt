package store.presentation.vm

import store.domain.ext.isNo
import store.domain.ext.isYes
import store.domain.ext.removeStockUnitSuffix
import store.domain.model.output.OutputRules
import store.domain.model.output.OutputRules.OUT_OF_STOCK
import store.domain.model.output.OutputRules.STOCK_UNIT
import store.domain.model.product.ProductItem
import store.domain.model.receipt.GiftReceipt
import store.domain.model.receipt.PaymentReceipt
import store.domain.model.receipt.PaymentReceiptItem
import store.domain.repository.ProductRepository
import store.domain.usecase.CheckOrderValidationUseCase
import store.domain.usecase.ExtractOrdersUseCase
import store.domain.usecase.GetInProgressPromotionUseCase
import store.domain.usecase.MakeOutReceiptUseCase
import store.domain.usecase.ValidateYesNoInputUseCase
import store.presentation.event.UiEvent
import store.presentation.vm.model.ApplyPromotion
import store.presentation.vm.model.Order
import store.presentation.vm.model.Orders
import store.presentation.vm.model.PromotionState
import store.presentation.vm.model.PromotionState.InProgress
import store.presentation.vm.model.PromotionState.NoPromotion
import store.presentation.vm.model.PromotionState.NotInProgress
import store.presentation.vm.model.StoreState

/**
 *
 * @property productRepository 상품 데이터 소스를 제공하는 리포지토리
 * @property checkOrderValidationUseCase 주문 유효성을 검사하는 유스케이스
 * @property getInProgressPromotionUseCase 진행 중인 프로모션을 조회하는 유스케이스
 * @property validateYesNoInputUseCase 'Y/N' 입력을 검증하는 유스케이스
 * @property makeOutReceiptUseCase 영수증을 생성하는 유스케이스
 */

class ViewModel(
    private val productRepository: ProductRepository,
    private val checkOrderValidationUseCase: CheckOrderValidationUseCase,
    private val getInProgressPromotionUseCase: GetInProgressPromotionUseCase,
    private val validateYesNoInputUseCase: ValidateYesNoInputUseCase,
    private val makeOutReceiptUseCase: MakeOutReceiptUseCase
) {
    private var _state = StoreState.create()
    val state get() = _state

    /**
     * 초기 상태 설정
     * 상품 정보를 가져와 현재 재고 안내 메시지를 표시합니다.
     */
    fun initializeStoreState() {
        val products = productRepository.getProduct()
        val guideMsg = products.makeCurrentStockGuideMessage()
        _state = state.copy(
            products = products,
            uiEvent = UiEvent.UserAccess(guideMsg)
        )
    }

    /**
     * 사용자의 주문을 처리하고 유효성을 확인합니다.
     *
     * @param order 사용자가 입력한 주문 문자열
     */
    fun processOrder(order: String) {
        val products = _state.products
        val validationResult = checkOrderValidationUseCase(order, products)
        onCompleteCheckOrderValidation(validationResult)
        finalizeState()
    }

    /**
     * 주문 유효성 검사 후 새로운 주문 목록을 생성하여 상태에 반영합니다.
     *
     * @param order 주문 정보의 맵 (상품명과 수량)
     */
    private fun onCompleteCheckOrderValidation(order: Map<String, Int>) {
        val newOrders = Orders(
            order.map {
                Order(name = it.key, quantity = it.value, promotion = NoPromotion)
            }
        )
        _state = _state.copy(orders = newOrders)
        applyPromotionsToOrders()
    }

    /**
     * 각 주문 항목에 대해 프로모션을 적용하고 주문 목록을 업데이트합니다.
     */
    private fun applyPromotionsToOrders() {
        val updateOrders = _state.orders.items.map { order ->
            val hasPromotion = _state.products.hasPromotion(order.name)
            assignPromotionToOrder(order, hasPromotion)
        }
        _state = _state.copy(orders = Orders(updateOrders))
        finalizeOrderCalculation()
    }

    /**
     * 특정 주문에 프로모션을 할당
     *
     * @param currentOrder 현재 처리 중인 주문 항목
     * @param promotionName 적용할 프로모션 이름 (없을 경우 null)
     * @return 프로모션이 적용된 주문 항목
     */
    private fun assignPromotionToOrder(currentOrder: Order, promotionName: String?): Order {
        return promotionName?.let {
            val promotionState = PromotionState.create(getInProgressPromotionUseCase(it))
            currentOrder.copy(promotion = promotionState)
        } ?: currentOrder
    }

    /**
     * 주문 계산을 완료하여 상태를 업데이트합니다.
     */
    private fun finalizeOrderCalculation() {
        _state.orders.items.forEach { order ->
            val promotion = order.promotion
            handleOrderPromotionStatus(promotion, order)
        }
    }

    /**
     * 주문 상품의 프로모션 상태를 확인하고 처리
     *
     * @param promotion 프로모션 상태
     * @param order 주문 항목
     */
    private fun handleOrderPromotionStatus(promotion: PromotionState, order: Order) {
        val productPrice = getProductPrice(order.name)

        when (promotion) {
            is InProgress -> handleOrderPromotionStock(order, productPrice, promotion)
            is NotInProgress, NoPromotion ->
                addPaymentReceipt(order.name, order.quantity, productPrice)
        }
    }

    /**
     * 주문에 대한 재고가 충분한 경우 프로모션을 적용
     *
     * @param order 주문 항목
     * @param productPrice 제품 가격
     * @param promotion 진행 중인 프로모션 상태
     */
    private fun handleOrderPromotionStock(order: Order, price: Int, promotion: InProgress) {
        if (_state.products.isPromotionStockEnough(order.name, order.quantity, promotion)) {
            whenPromotionStockEnough(order, price, promotion)
        } else {
            calculateWithShortageStockPromotion(order, promotion)
        }
    }

    /**
     * 주어진 주문에 대해 프로모션이 적용 가능한지 확인하고,
     * 프로모션 결과를 영수증에 반영하는 함수.
     *
     * @param order 주문 정보
     * @param productPrice 제품 가격
     * @param promotion 진행 중인 프로모션 정보
     */
    private fun whenPromotionStockEnough(order: Order, productPrice: Int, promotion: InProgress) {
        val promotionResult = calculateWithPromotion(order.quantity, promotion.buy, promotion.get)
        updateReceiptForPromotion(order.name, productPrice, promotionResult)
    }

    /**
     * 재고 부족을 고려하여 부족한 재고 수량을 계산하는 함수.
     *
     * @param stockQuantity 현재 재고 수량
     * @param orderQuantity 주문 수량
     * @param promotion 진행 중인 프로모션 정보
     * @return 부족한 재고 수량
     */
    private fun calculateShortageStock(
        stockQuantity: Int,
        orderQuantity: Int,
        promotion: InProgress
    ): Int {
        val bundles = stockQuantity % (promotion.buy + promotion.get)
        val remainder = orderQuantity - stockQuantity
        val shortageStock = bundles + remainder
        return shortageStock
    }

    /**
     * 프로모션에 따른 재고 부족을 고려하여 프로모션 적용을 처리하는 함수.
     *
     * @param order 주문 정보
     * @param promotion 진행 중인 프로모션 정보
     */
    private fun calculateWithShortageStockPromotion(order: Order, promotion: InProgress) {
        val stock = _state.products.getPromotionStock(order.name)
        val productPrice = getProductPrice(order.name)
        val shortageStock = calculateShortageStock(stock, order.quantity, promotion)
        val (buyingAmount, promotionGift) =
            calculatePromotionDetails(shortageStock, order, promotion)
        val applyPromotion = ApplyPromotion(buyingAmount, promotionGift, false)
        handleReceiptForNotEnoughPromotion(
            order.name, applyPromotion, buyingAmount * productPrice, shortageStock
        )
    }

    /**
     * 재고 부족을 고려하여 프로모션의 구매 수량과 증정 수량을 계산하는 함수.
     *
     * @param shortageStock 부족한 재고 수량
     * @param order 주문 정보
     * @param promotion 진행 중인 프로모션 정보
     * @return 구매 수량과 증정 수량을 포함한 Pair
     */
    private fun calculatePromotionDetails(
        shortageStock: Int, order: Order, promotion: InProgress
    ): Pair<Int, Int> {
        val maxPromotionStock = (order.quantity - shortageStock) / (promotion.buy + promotion.get)
        val userBuyingAmount = calculateUserBuy(maxPromotionStock, shortageStock, promotion.buy)
        val promotionGift = maxPromotionStock * promotion.get
        return Pair(userBuyingAmount, promotionGift)
    }

    /**
     * 프로모션을 고려하여 사용자가 구매할 수 있는 수량을 계산하는 함수.
     *
     * @param maxInPromotionStock 프로모션 적용 가능한 최대 수량
     * @param shortageStock 부족한 재고 수량
     * @param buy 구매 수량
     * @return 최종 구매 수량
     */
    private fun calculateUserBuy(maxInPromotionStock: Int, shortageStock: Int, buy: Int): Int {
        return maxInPromotionStock * buy + shortageStock
    }

    /**
     * 프로모션 재고가 부족한 경우, 해당 주문에 대해 영수증을 처리하고 선물 수량을 업데이트하는 함수.
     *
     * @param orderName 주문 제품 이름
     * @param result 프로모션 결과
     * @param paymentAmount 지불 금액
     * @param shortageStock 부족한 재고 수량
     */
    private fun handleReceiptForNotEnoughPromotion(
        orderName: String,
        result: ApplyPromotion,
        paymentAmount: Int,
        shortageStock: Int,
    ) {
        addPaymentReceipt(orderName, result.totalQuantity, paymentAmount, shortageStock)
        val currentGiftQuantity = _state.giftReceipt.items.getOrDefault(orderName, 0)
        val updatedGiftQuantity = currentGiftQuantity + result.giftQuantity
        updateGiftReceipt(orderName, updatedGiftQuantity, false)
    }

    /**
     * 프로모션을 계산하여 프로모션 결과를 반환합니다.
     *
     * @param quantity 주문 수량
     * @param buy 프로모션 조건의 구매 수량
     * @param get 프로모션 조건의 증정 수량
     * @return 적용된 프로모션 결과
     */
    private fun calculateWithPromotion(quantity: Int, buy: Int, get: Int): ApplyPromotion {
        val bundles = quantity / (buy + get)
        val remainder = quantity % (buy + get)
        val totalQuantity = bundles * buy + remainder
        val giftQuantity = bundles * get
        val hasReceivedPromotion = remainder == buy
        return ApplyPromotion(totalQuantity, giftQuantity, hasReceivedPromotion)
    }

    /**
     * 프로모션 적용 결과를 사용하여 영수증을 업데이트합니다.
     *
     * @param name 주문한 상품 이름
     * @param price 상품 가격
     * @param result 프로모션 적용 결과, 총 구매 수량과 증정 수량이 포함된 데이터
     */
    private fun updateReceiptForPromotion(name: String, price: Int, result: ApplyPromotion) {
        addPaymentReceipt(name, result.totalQuantity, price)

        val currentGiftQuantity = _state.giftReceipt.items.getOrDefault(name, 0)
        val updatedGiftQuantity = currentGiftQuantity + result.giftQuantity

        updateGiftReceipt(name, updatedGiftQuantity, result.hasReceivedPromotion)
    }

    /**
     * 프로모션에 대한 선물 영수증을 업데이트합니다.
     *
     * @param name 상품 이름
     * @param giftQuantity 증정품 수량
     * @param hasReceivedPromotion 프로모션을 받은 여부
     */
    private fun updateGiftReceipt(
        name: String, giftQuantity: Int, hasReceivedPromotion: Boolean
    ) {
        val updatedNotReceivedPromotion = getReceivedPromotion(name, hasReceivedPromotion)
        _state = _state.copy(
            giftReceipt = _state.giftReceipt.copy(
                items = _state.giftReceipt.items.apply {
                    this[name] = this.getOrDefault(name, 0) + giftQuantity
                }, notReceivedPromotion = updatedNotReceivedPromotion
            )
        )
    }

    /**
     * 결제 영수증을 추가합니다.
     *
     * @param productName 상품 이름
     * @param productQuantity 상품 수량
     * @param productPrice 상품 가격
     * @param shortageStock 부족한 재고 수량
     */
    private fun addPaymentReceipt(
        productName: String, productQuantity: Int, productPrice: Int, shortageStock: Int = 0
    ) {
        if (shortageStock > 0) addShortageStock(productName, shortageStock)
        val updatedReceipt = getUpdatePaymentReceipt(productName, productQuantity, productPrice)
        _state = _state.copy(paymentReceipt = updatedReceipt)
    }

    /**
     * 부족한 재고를 추가합니다.
     *
     * @param productName 상품 이름
     * @param shortageStock 부족한 재고 수량
     */
    private fun addShortageStock(productName: String, shortageStock: Int) {
        val updatedShortageStock = _state.paymentReceipt.shortageStock + (productName to shortageStock)

        _state = _state.copy(
            paymentReceipt = _state.paymentReceipt.copy(
                shortageStock = updatedShortageStock
            )
        )
    }

    /**
     * 결제 영수증을 업데이트하여 반환합니다.
     *
     * @param name 상품 이름
     * @param quantity 상품 수량
     * @param price 상품 가격
     * @return 업데이트된 결제 영수증
     */
    private fun getUpdatePaymentReceipt(
        name: String, quantity: Int, price: Int
    ): PaymentReceipt {
        return _state.paymentReceipt.copy(
            items = _state.paymentReceipt.items + PaymentReceiptItem(
                name, price = price, originPrice = getProductPrice(name), quantity = quantity
            )
        )
    }

    /**
     * 프로모션을 받은 상품 목록을 반환합니다.
     *
     * @param name 상품 이름
     * @param hasReceivedPromotion 프로모션을 받은 여부
     * @return 프로모션을 받은 상품 목록
     */
    private fun getReceivedPromotion(
        name: String, hasReceivedPromotion: Boolean
    ): List<String> {
        if (hasReceivedPromotion) {
            return _state.giftReceipt.notReceivedPromotion + name
        }
        return _state.giftReceipt.notReceivedPromotion
    }

    /**
     * 주문을 최종화하고, 관련된 메시지를 설정합니다.
     */
    private fun finalizeState() {
        val finalizedState = UiEvent.FinalizeOrder(
            notReceivedPromotionMsg = _state.giftReceipt.createNotReceivedPromotionMsg(),
            shortageStockMsg = _state.paymentReceipt.createShortageStockMsg()
        )
        _state = _state.copy(uiEvent = finalizedState)
    }

    /**
     * 사용자가 '예' 또는 '아니오'로 입력한 값을 유효성 검사
     *
     * @param input 사용자의 입력 값
     */
    fun whenUserInputYesOrNo(input: String) {
        validateYesNoInputUseCase(input)
    }

    /**
     * 사용자가 프로모션 수신 여부를 선택한 후 처리합니다.
     *
     * @param idx 프로모션 인덱스
     * @param input 사용자의 입력 값
     */
    fun addOrRemoveNotReceivedPromotion(idx: Int, input: String) {
        if (input.isNo()) {
            noReceivePromotion(idx)
        }
        if (input.isYes()) {
            addNotReceivedPromotion(idx)
        }
    }

    /**
     * 사용자가 프로모션 수신 여부를 선택한 후 처리합니다.
     *
     * @param idx 프로모션 인덱스
     */
    private fun noReceivePromotion(idx: Int) {
        val notReceivedPromotion = _state.giftReceipt.removeNotReceivedPromotion(idx)
        _state = _state.copy(
            giftReceipt = _state.giftReceipt.copy(notReceivedPromotion = notReceivedPromotion)
        )
    }

    /**
     * 프로모션을 수신하도록 추가합니다.
     *
     * @param idx 프로모션 인덱스
     */
    private fun addNotReceivedPromotion(idx: Int) {
        _state.giftReceipt.addNotReceivedPromotion(idx)
    }

    /**
     * 부족한 재고가 결제 장바구니에서 제거되도록 처리합니다.
     *
     * @param productName 상품 이름
     */
    fun noPayShortageStock(productName: String) {
        _state = _state.copy(
            paymentReceipt = _state.paymentReceipt.removeFromShortageStock(productName)
        )
    }

    /**
     * 사용자가 멤버십 신청을 요청했을 때 유효성 검사
     *
     * @param input 사용자의 입력 값
     */
    fun whenUserRequestMembership(input: String) {
        if (input.isYes()) _state = _state.copy(membershipApply = true)
    }

    /**
     * 영수증을 출력 상태로 변환하여 관련 정보를 업데이트합니다.
     */
    fun makeOutRecipeState() {
        val receipt = makeOutReceiptUseCase(_state.toReceiptInfo())
        removeSoldStock()
        val makeOutReceipt = UiEvent.MakeOutReceipt(receipt)
        _state = _state.copy(uiEvent = makeOutReceipt)
    }

    /**
     * 판매된 재고를 제거합니다.
     */
    private fun removeSoldStock() {
        _state.paymentReceipt.items.forEach { product ->
            val soldStock = getSumOfProductQuantity(product, _state.giftReceipt)
            val currentStock = _state.products.items.filter { it.name == product.name }
            if (currentStock.size == 1) removeNonPromotionProduct(currentStock[0], soldStock)
            if (currentStock.size == 2) {
                val isPromotionSoldOut = currentStock[0].quantity == "$OUT_OF_STOCK"
                val nonPromotionStock = currentStock[1].quantity.removeStockUnitSuffix()
                removePromotionStock(nonPromotionStock, isPromotionSoldOut, currentStock, soldStock)
            }
        }
    }

    /**
     * 비프로모션 상품의 재고를 업데이트합니다.
     *
     * @param currentStock 현재 상품 재고
     * @param soldStock 판매된 상품 수량
     */
    private fun removeNonPromotionProduct(currentStock: ProductItem, soldStock: Int) {
        val calculatedSoldStock = (currentStock.quantity.removeStockUnitSuffix() - soldStock)
        val updatedProductItem = currentStock.modifyQuantity(calculatedSoldStock)

        val updatedItems = _state.products.items.map { item ->
            if (item.name == currentStock.name && item.promotion == null) updatedProductItem
            else item
        }
        modifyProduct(updatedItems)
    }

    /**
     * 프로모션 상품의 재고를 업데이트합니다.
     *
     * @param nonPromotionStock 비프로모션 상품의 재고 수량
     * @param isPromotionSoldOut 프로모션 상품이 품절되었는지 여부
     * @param stock 상품 목록
     * @param soldStock 판매된 상품 수량
     */
    private fun removePromotionStock(
        nonPromotionStock: Int,
        isPromotionSoldOut: Boolean,
        stock: List<ProductItem>,
        soldStock: Int
    ) {
        if (isPromotionSoldOut) modifyNonPromotionProductStock(stock[1], nonPromotionStock)

        val promotionStockQuantity = stock[0].quantity.removeStockUnitSuffix()
        if (promotionStockQuantity < soldStock) {
            whenPromotionStockNotEnough(stock, nonPromotionStock, soldStock, promotionStockQuantity)
        } else {
            modifyPromotionProductStock(stock[0], promotionStockQuantity - soldStock)
        }
    }

    /**
     * 프로모션 상품 재고가 부족할 경우 처리합니다.
     *
     * @param stock 상품 목록
     * @param nonPromotionStock 비프로모션 상품 재고
     * @param soldStock 판매된 수량
     * @param promotionStock 프로모션 상품 재고
     */
    private fun whenPromotionStockNotEnough(
        stock: List<ProductItem>,
        nonPromotionStock: Int,
        soldStock: Int,
        promotionStock: Int
    ) {
        modifyPromotionProductStock(stock[0], 0)
        val afterSoldQuantity = nonPromotionStock + promotionStock - soldStock
        modifyNonPromotionProductStock(stock[1], afterSoldQuantity)
    }

    /**
     * 상품 재고를 수정합니다.
     *
     * @param items 수정된 상품 목록
     */
    private fun modifyPromotionProductStock(product : ProductItem, quantity: Int){
        val newStock = product.modifyQuantity(quantity)
        val updatedItems = _state.products.items.map { item ->
            if (item.name == product.name && item.promotion.isNullOrEmpty().not()) newStock
            else item
        }
        modifyProduct(updatedItems)
    }

    /**
     * 비프로모션 상품 재고를 수정합니다.
     *
     * @param item 상품
     * @param updatedQuantity 수정된 수량
     */
    private fun modifyNonPromotionProductStock(product : ProductItem, quantity: Int){
        val newStock = product.modifyQuantity(quantity)
        val updatedItems = _state.products.items.map { item ->
            if (item.name == product.name && item.promotion.isNullOrEmpty()) newStock
            else item
        }
        modifyProduct(updatedItems)
    }

    /**
     * 새로운 상품 목록으로 상태를 업데이트합니다.
     *
     * @param newProduct 새로운 상품 목록
     */
    private fun modifyProduct(newProduct: List<ProductItem>) {
        _state = _state.copy(products = _state.products.copy(items = newProduct))
    }

    /**
     * 사용자가 추가 구매를 선택했을 때, 주문 및 영수증을 초기화합니다.
     */
    fun whenUserSelectAdditionalPurchase() {
        _state = _state.clearOrdersAndReceipts()
    }

    /**
     * 주어진 상품의 수량 합계를 계산합니다.
     * 해당 상품이 영수증에 존재하는 경우, 영수증에 기록된 수량을 더합니다.
     *
     * @param product 영수증에 포함된 상품 항목
     * @param receipt 영수증 객체
     * @return 해당 상품의 총 수량
     */
    private fun getSumOfProductQuantity(product: PaymentReceiptItem, receipt: GiftReceipt): Int {
        return receipt.items[product.name]?.plus(product.quantity) ?: product.quantity
    }

    /**
     * 상품의 가격을 가져옵니다.
     *
     * @param name 상품 이름
     * @return 해당 상품의 가격
     */
    private fun getProductPrice(name: String): Int = _state.products.getPrice(name).toInt()

}