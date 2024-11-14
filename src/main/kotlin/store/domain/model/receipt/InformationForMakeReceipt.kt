package store.domain.model.receipt

import store.domain.ext.toKoreanUnit
import store.domain.model.output.OutputRules.Companion.memberShipDiscountMax
import store.domain.model.output.OutputRules.Companion.recipeEventDiscountFormat
import store.domain.model.output.OutputRules.Companion.recipeMembershipDiscountFormat
import store.domain.model.output.OutputRules.Companion.recipeProductFormat

data class InformationForMakeReceipt(
    val paymentReceipt: PaymentReceipt,
    val giftReceipt: GiftReceipt,
    val isMembershipApply: Boolean
) {
    fun formatReceiptItems(): String {
        val productQuantities = this.paymentReceipt.items.map { product ->
            val totalQuantity = getSumOfProductQuantity(product)
            val price = (product.originPrice * totalQuantity).toKoreanUnit()
            recipeProductFormat(product.name, totalQuantity, price)
        }
        return productQuantities.joinToString("\n")
    }

    fun calculateDiscountFormat(membershipForm: String): Pair<String, Int> {
        val discount = calculateGiftDiscount()
        val eventDiscountFormat = recipeEventDiscountFormat(discount.toKoreanUnit())
        return Pair("$eventDiscountFormat\n$membershipForm", discount)
    }

    fun calculateTotalQuantityAndPrice(): Pair<Int, Int> {
        val totalQuantity = this.paymentReceipt.items.sumOf { product ->
            getSumOfProductQuantity(product)
        }
        val totalPrice = this.paymentReceipt.items.sumOf { product ->
            product.originPrice * getSumOfProductQuantity(product)
        }
        return Pair(totalQuantity, totalPrice)
    }

    fun calculateMembershipDiscount(): Pair<String, Int> {
        return if (this.isMembershipApply) {
            applyMembershipDiscount(getNonPromotionPrice())
        } else {
            Pair(recipeMembershipDiscountFormat("0"), 0)
        }
    }

    private fun getNonPromotionPrice(): Int {
        val paymentItems = this.paymentReceipt.items
        val gitReceiptItems = this.giftReceipt.items

        val nonPromotionPrice = paymentItems.filterNot { gitReceiptItems.contains(it.name) }
            .sumOf { it.originPrice * it.quantity }
        val shortageStockPrice = this.paymentReceipt.shortageStock.map { (name, quantity) ->
            this.paymentReceipt.getOriginalPrice(name).times(quantity)
        }.sum()
        return nonPromotionPrice + shortageStockPrice
    }

    private fun applyMembershipDiscount(nonPromotionPrice: Int): Pair<String, Int> {
        val discount = (nonPromotionPrice * 0.3).toInt().coerceAtMost(memberShipDiscountMax())
        return recipeMembershipDiscountFormat(discount.toKoreanUnit()) to discount
    }

    private fun calculateGiftDiscount(): Int {
        return this.giftReceipt.items.entries.sumOf { (giftName, giftQuantity) ->
            this.paymentReceipt.items.find { it.name == giftName }
                ?.originPrice
                ?.times(giftQuantity)
                ?: 0
        }
    }

    private fun getSumOfProductQuantity(product: PaymentReceiptItem): Int {
        return this.giftReceipt.items[product.name]?.plus(product.quantity) ?: product.quantity
    }
}