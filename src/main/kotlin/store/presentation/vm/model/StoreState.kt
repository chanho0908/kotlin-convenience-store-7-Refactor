package store.presentation.vm.model

import store.domain.model.output.OutputRules
import store.domain.model.product.Products
import store.domain.model.promotion.Promotions
import store.domain.model.receipt.GiftReceipt
import store.domain.model.receipt.InformationForMakeReceipt
import store.domain.model.receipt.PaymentReceipt
import store.presentation.event.UiEvent

data class StoreState(
    val products: Products,
    val orders: Orders,
    val paymentReceipt : PaymentReceipt,
    val giftReceipt : GiftReceipt,
    val membershipApply: Boolean,
    val uiEvent: UiEvent
) {
    fun clearOrdersAndReceipts(): StoreState = this.copy(
        orders = Orders(emptyList()),
        paymentReceipt = PaymentReceipt(emptyList(), emptyMap()),
        giftReceipt = GiftReceipt(mutableMapOf(), emptyList()),
        membershipApply = false,
        uiEvent = UiEvent.UserAccess(
            "\n${OutputRules.WELCOME}\n${this.products.makeCurrentStockGuideMessage()}"
        )
    )

    fun toReceiptInfo(): InformationForMakeReceipt{
        return InformationForMakeReceipt(
            this.paymentReceipt,
            this.giftReceipt,
            this.membershipApply
        )
    }

    companion object {
        fun create() = StoreState(
            Products(emptyList()),
            Orders(emptyList()),
            PaymentReceipt(emptyList(), emptyMap()),
            GiftReceipt(mutableMapOf(), emptyList()),
            false,
            UiEvent.Loading(OutputRules.WELCOME.toString())
        )
    }
}