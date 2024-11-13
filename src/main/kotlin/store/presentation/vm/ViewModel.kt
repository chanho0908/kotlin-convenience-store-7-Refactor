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

class ViewModel(
    private val productRepository: ProductRepository,
    private val checkOrderValidationUseCase: CheckOrderValidationUseCase,
    private val extractOrdersUseCase: ExtractOrdersUseCase,
    private val getInProgressPromotionUseCase: GetInProgressPromotionUseCase,
    private val validateYesNoInputUseCase: ValidateYesNoInputUseCase,
    private val makeOutReceiptUseCase: MakeOutReceiptUseCase
) {
    private var _state = StoreState.create()
    val state get() = _state

    fun initializeStoreState() {
        val products = productRepository.getProduct()
        val guideMsg = products.makeCurrentStockGuideMessage()
        _state = state.copy(
            products = products,
            uiEvent = UiEvent.UserAccess(guideMsg)
        )
    }

    fun processOrder(order: String) {
        val products = _state.products
        onCompleteCheckOrderValidation(checkOrderValidationUseCase(order, products))
        finalizeState()
    }

    private fun onCompleteCheckOrderValidation(order: Map<String, Int>) {
        val newOrders = Orders(
            order.map {
                Order(name = it.key, quantity = it.value, promotion = NoPromotion)
            }
        )
        _state = _state.copy(orders = newOrders)
        applyPromotionsToOrders()
    }

    private fun applyPromotionsToOrders() {
        val updateOrders = _state.orders.items.map { order ->
            val hasPromotion = _state.products.hasPromotion(order.name)
            assignPromotionToOrder(order, hasPromotion)
        }
        _state = _state.copy(orders = Orders(updateOrders))
        finalizeOrderCalculation()
    }

    private fun assignPromotionToOrder(currentOrder: Order, promotionName: String?): Order {
        return promotionName?.let {
            val promotionState = PromotionState.create(getInProgressPromotionUseCase(it))
            currentOrder.copy(promotion = promotionState)
        } ?: currentOrder
    }

    private fun finalizeOrderCalculation() {
        _state.orders.items.forEach { order ->
            val promotion = order.promotion
            handleOrderPromotionStatus(promotion, order)
        }
    }

    private fun handleOrderPromotionStatus(promotion: PromotionState, order: Order) {
        val productPrice = getProductPrice(order.name)

        when (promotion) {
            is InProgress -> handleOrderPromotionStock(order, productPrice, promotion)
            is NotInProgress, NoPromotion ->
                addPaymentReceipt(order.name, order.quantity, productPrice)
        }
    }

    private fun handleOrderPromotionStock(order: Order, price: Int, promotion: InProgress) {
        if (_state.products.isPromotionStockEnough(order.name, order.quantity)) {
            whenPromotionStockEnough(order, price, promotion)
        } else {
            calculateWithShortageStockPromotion(order, promotion)
        }
    }

    private fun whenPromotionStockEnough(order: Order, productPrice: Int, promotion: InProgress) {
        val promotionResult = calculateWithPromotion(order.quantity, promotion.buy, promotion.get)
        updateReceiptForPromotion(order.name, productPrice, promotionResult)
    }

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

    private fun calculatePromotionDetails(
        shortageStock: Int, order: Order, promotion: InProgress
    ): Pair<Int, Int> {
        val maxPromotionStock = (order.quantity - shortageStock) / (promotion.buy + promotion.get)
        val userBuyingAmount = calculateUserBuy(maxPromotionStock, shortageStock, promotion.buy)
        val promotionGift = maxPromotionStock * promotion.get
        return Pair(userBuyingAmount, promotionGift)
    }

    private fun calculateUserBuy(maxInPromotionStock: Int, shortageStock: Int, buy: Int): Int {
        return maxInPromotionStock * buy + shortageStock
    }

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

    private fun calculateWithPromotion(quantity: Int, buy: Int, get: Int): ApplyPromotion {
        val bundles = quantity / (buy + get)
        val remainder = quantity % (buy + get)
        val totalQuantity = bundles * buy + remainder
        val giftQuantity = bundles * get
        val hasReceivedPromotion = remainder == buy
        return ApplyPromotion(totalQuantity, giftQuantity, hasReceivedPromotion)
    }

    private fun updateReceiptForPromotion(name: String, price: Int, result: ApplyPromotion) {
        addPaymentReceipt(name, result.totalQuantity, price)

        val currentGiftQuantity = _state.giftReceipt.items.getOrDefault(name, 0)
        val updatedGiftQuantity = currentGiftQuantity + result.giftQuantity

        updateGiftReceipt(name, updatedGiftQuantity, result.hasReceivedPromotion)
    }

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

    private fun addPaymentReceipt(
        productName: String, productQuantity: Int, productPrice: Int, shortageStock: Int = 0
    ) {
        if (shortageStock > 0) addShortageStock(productName, shortageStock)
        val updatedReceipt = getUpdatePaymentReceipt(productName, productQuantity, productPrice)
        _state = _state.copy(paymentReceipt = updatedReceipt)
    }

    private fun addShortageStock(productName: String, shortageStock: Int) {
        val updatedShortageStock =
            _state.paymentReceipt.shortageStock + (productName to shortageStock)

        _state = _state.copy(
            paymentReceipt = _state.paymentReceipt.copy(
                shortageStock = updatedShortageStock
            )
        )
    }

    private fun getUpdatePaymentReceipt(
        name: String, quantity: Int, price: Int
    ): PaymentReceipt {
        return _state.paymentReceipt.copy(
            items = _state.paymentReceipt.items + PaymentReceiptItem(
                name = name, price = price, originPrice = getProductPrice(name), quantity = quantity
            )
        )
    }

    private fun getReceivedPromotion(
        name: String, hasReceivedPromotion: Boolean
    ): List<String> {
        if (hasReceivedPromotion) {
            return _state.giftReceipt.notReceivedPromotion + name
        }
        return _state.giftReceipt.notReceivedPromotion
    }

    private fun finalizeState() {
        val finalizedState = UiEvent.FinalizeOrder(
            notReceivedPromotionMsg = _state.giftReceipt.createNotReceivedPromotionMsg(),
            shortageStockMsg = _state.paymentReceipt.createShortageStockMsg()
        )
        _state = _state.copy(uiEvent = finalizedState)
    }

    fun whenUserInputYesOrNo(input: String) {
        validateYesNoInputUseCase(input)
    }

    fun addOrRemoveNotReceivedPromotion(idx: Int, input: String) {
        if (input.isNo()) {
            noReceivePromotion(idx)
        }
        if (input.isYes()) {
            addNotReceivedPromotion(idx)
        }
    }

    private fun noReceivePromotion(idx: Int) {
        val notReceivedPromotion = _state.giftReceipt.removeNotReceivedPromotion(idx)
        _state = _state.copy(
            giftReceipt = _state.giftReceipt.copy(notReceivedPromotion = notReceivedPromotion)
        )
    }

    private fun addNotReceivedPromotion(idx: Int) {
        _state.giftReceipt.addNotReceivedPromotion(idx)
    }

    fun noPayShortageStock(productName: String) {
        _state = _state.copy(
            paymentReceipt = _state.paymentReceipt.removeFromShortageStock(productName)
        )
    }

    fun whenUserRequestMembership(input: String) {
        if (input.isYes()) _state = _state.copy(membershipApply = true)
    }

    fun makeOutRecipeState() {
        val receipt = makeOutReceiptUseCase(
            _state.paymentReceipt, _state.giftReceipt, _state.membershipApply
        )
        removeSoldStock()
        val makeOutReceipt = UiEvent.MakeOutReceipt(receipt)
        _state = _state.copy(uiEvent = makeOutReceipt)
    }

    private fun removeSoldStock() {
        _state.paymentReceipt.items.forEach { product ->
            val soldStock = getSumOfProductQuantity(product, _state.giftReceipt)
            val currentStock = _state.products.items.filter { it.name == product.name }
            if (currentStock.size == 1) removeNonPromotionProduct(currentStock, soldStock)
            if (currentStock.size == 2) {
                val isPromotionSoldOut = currentStock[0].quantity == "$OUT_OF_STOCK"
                val nonPromotionStock = currentStock[1].quantity.removeStockUnitSuffix()
                removePromotionStock(nonPromotionStock, isPromotionSoldOut, currentStock, soldStock)
            }
        }
    }

    private fun removeNonPromotionProduct(currentStock: List<ProductItem>, soldStock: Int) {
        val calculatedSoldStock = (currentStock[0].quantity.removeStockUnitSuffix() - soldStock)
        currentStock[0].quantity = if (calculatedSoldStock == 0) OUT_OF_STOCK.toString()
        else calculatedSoldStock.toString() + STOCK_UNIT.toString()
    }

    private fun removePromotionStock(
        nonPromotionStock: Int,
        isPromotionSoldOut: Boolean,
        stock: List<ProductItem>,
        soldStock: Int
    ) {
        if (isPromotionSoldOut) stock[1].quantity = "${nonPromotionStock - soldStock}$STOCK_UNIT"
        val promotionStockQuantity = stock[0].quantity.removeStockUnitSuffix()
        if (promotionStockQuantity < soldStock) {
            whenPromotionStockNotEnough(stock, nonPromotionStock, soldStock, promotionStockQuantity)
        } else {
            stock[0].quantity = "${promotionStockQuantity - soldStock}$STOCK_UNIT"
        }
    }

    private fun whenPromotionStockNotEnough(
        stock: List<ProductItem>,
        nonPromotionStock: Int,
        soldStock: Int,
        promotionStock: Int
    ) {
        stock[0].quantity = "$OUT_OF_STOCK"
        val afterSoldQuantity = nonPromotionStock + promotionStock - soldStock
        stock[1].quantity = if (afterSoldQuantity == 0) "$OUT_OF_STOCK"
        else "$afterSoldQuantity$STOCK_UNIT"
    }

    private fun getSumOfProductQuantity(product: PaymentReceiptItem, receipt: GiftReceipt): Int {
        return receipt.items[product.name]?.plus(product.quantity) ?: product.quantity
    }

    fun whenUserSelectAdditionalPurchase() {
        _state = _state.clearOrdersAndReceipts()
    }

    private fun getProductPrice(name: String): Int = _state.products.getPrice(name).toInt()
}