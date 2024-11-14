package store.domain.model.product

import store.domain.ext.removeStockUnitSuffix
import store.domain.model.Exception
import store.domain.model.output.OutputRules.STOCK_UNIT
import store.domain.model.output.OutputRules.OUT_OF_STOCK
import store.domain.model.output.OutputRules.GUIDE
import store.domain.model.output.OutputRules.Companion.productFormat
import store.presentation.vm.model.PromotionState.InProgress

data class Products(
    val items: List<ProductItem>
) {
    private fun joinToLineBreak() = items.joinToString("\n") { toUiModel(it) }

    fun makeCurrentStockGuideMessage(): String {
        return "${this.joinToLineBreak()}\n\n$GUIDE"
    }

    fun hasPromotion(name: String): String? {
        return items.find { it.name == name && it.promotion != null }?.promotion
    }

    fun getPromotionStock(name: String) = items.find {
        it.name == name && it.promotion.isNullOrEmpty().not()
    }?.quantity?.removeStockUnitSuffix() ?: 0

    fun isPromotionStockEnough(name: String, quantity: Int, promotion: InProgress): Boolean {
        val promotionApplyStock = ((quantity / promotion.get) * promotion.buy) + quantity
        val promotionStockQuantity = getPromotionStock(name)
        return promotionStockQuantity >= promotionApplyStock
    }

    fun getPrice(name: String): String {
        return items.find { it.name == name }
            ?.price
            ?.replace(",", "")
            ?: "${Exception.NOT_SALES}"
    }

    private fun toUiModel(product: ProductItem) = productFormat(product)
}

data class ProductItem(
    val name: String,
    val price: String,
    val quantity: String,
    val promotion: String?
){
    fun modifyQuantity(quantity: Int): ProductItem {
        return this.copy(
            quantity = if (quantity == 0) "$OUT_OF_STOCK"
            else "$quantity$STOCK_UNIT"
        )
    }
}