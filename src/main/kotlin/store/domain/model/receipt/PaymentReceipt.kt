package store.domain.model.receipt

import store.domain.model.output.OutputRules

data class PaymentReceipt(
    val items: List<PaymentReceiptItem>,
    val shortageStock: Map<String, Int>
) {
    fun createShortageStockMsg(): List<String>? {
        if (shortageStock.isNotEmpty()) {
            val shortageStockMsg = mutableListOf<String>()
            shortageStock.forEach { (name, quantity) ->
                shortageStockMsg.add(OutputRules.storageStockFormat(name, quantity))
            }
            return shortageStockMsg
        }
        return null
    }

    fun getOriginalPrice(productName: String): Int {
        return items.find { it.name == productName }?.originPrice ?: 0
    }

    fun removeFromShortageStock(productName: String): PaymentReceipt {
        val shortageStockAmount = this.shortageStock[productName] ?: 0
        val updateItems = this.items.map { item ->
            if (item.name == productName) createNewItem(item, shortageStockAmount)
            else item
        }
        return PaymentReceipt(updateItems, emptyMap())
    }

    private fun createNewItem(item: PaymentReceiptItem, shortageStockAmount: Int): PaymentReceiptItem{
        val shortageStockPrice = item.quantity * shortageStockAmount
        val newQuantity = item.quantity - shortageStockAmount
        val newPrice = item.price - shortageStockPrice
        return PaymentReceiptItem(item.name, newPrice, item.originPrice, newQuantity)
    }
}

data class PaymentReceiptItem(
    val name: String,
    val originPrice: Int,
    val price: Int,
    val quantity: Int
)
