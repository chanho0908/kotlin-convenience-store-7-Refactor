package store.domain.model.receipt

data class InformationForMakeReceipt (
    val paymentReceipt: PaymentReceipt,
    val giftReceipt: GiftReceipt,
    val isMembershipApply: Boolean
){
    fun getNonPromotionPrice(): Int {
        val paymentItems = this.paymentReceipt.items
        val gitReceiptItems = this.giftReceipt.items

        val nonPromotionPrice = paymentItems.filterNot { gitReceiptItems.contains(it.name) }
            .sumOf { it.originPrice * it.quantity }
        val shortageStockPrice = this.paymentReceipt.shortageStock.map { (name, quantity) ->
            this.paymentReceipt.getOriginalPrice(name).times(quantity)
        }.sum()
        return nonPromotionPrice + shortageStockPrice
    }

    fun getSumOfProductQuantity(product: PaymentReceiptItem): Int {
        return this.giftReceipt.items[product.name]?.plus(product.quantity) ?: product.quantity
    }
}