package store.domain.model.receipt

data class InformationForMakeReceipt (
    val paymentReceipt: PaymentReceipt,
    val giftReceipt: GiftReceipt,
    val isMembershipApply: Boolean
)