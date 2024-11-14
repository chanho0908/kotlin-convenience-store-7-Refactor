package store.data.dto

import store.domain.ext.toKoreanUnit
import store.domain.model.output.OutputRules
import store.domain.model.product.ProductRules.PRODUCT_NAME
import store.domain.model.product.ProductRules.PRODUCT_PRICE
import store.domain.model.product.ProductRules.PRODUCT_QUANTITY
import store.domain.model.product.ProductRules.PRODUCT_PROMOTION
import store.domain.model.product.ProductItem
import store.domain.model.output.OutputRules.STOCK_UNIT

data class ProductResponse(
    val name: String,
    val price: Int,
    val quantity: String,
    val promotion: String?
) {
    fun toDomainModel(): ProductItem {
        val koreaUnitPrice = this.price.toKoreanUnit()
        val item = ProductItem(name, koreaUnitPrice, quantity, promotion)
        return item
    }
}

fun List<String>.toProductResponse(): ProductResponse {
    val name = this[PRODUCT_NAME.getIndex()]
    val price = this[PRODUCT_PRICE.getIndex()].toInt()

    val quantity = if (this[PRODUCT_QUANTITY.getIndex()] == "0") OutputRules.OUT_OF_STOCK.toString()
    else this[PRODUCT_QUANTITY.getIndex()] + STOCK_UNIT.toString()
    val promotion = if (this[PRODUCT_PROMOTION.getIndex()] == "null") null
    else this[PRODUCT_PROMOTION.getIndex()]

    return ProductResponse(name, price, quantity, promotion)
}
