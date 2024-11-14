package store.domain.usecase

import store.domain.ext.toKoreanUnit
import store.domain.model.output.OutputRules.Companion.recipeTotalFormat
import store.domain.model.output.OutputRules.Companion.recipeMembershipDiscountFormat
import store.domain.model.output.OutputRules.Companion.recipeEventDiscountFormat
import store.domain.model.output.OutputRules.Companion.recipeTotalPriceFormat
import store.domain.model.output.OutputRules.Companion.recipeProductFormat
import store.domain.model.output.OutputRules.Companion.recipePromotionFormat
import store.domain.model.output.OutputRules.Companion.memberShipDiscountMax
import store.domain.model.receipt.GiftReceipt
import store.domain.model.receipt.InformationForMakeReceipt
import store.domain.model.receipt.PaymentReceipt
import store.domain.model.receipt.PaymentReceiptItem
import store.domain.model.receipt.Receipt

class MakeOutReceiptUseCase {
    operator fun invoke(info : InformationForMakeReceipt): Receipt {
        val totalData = info.calculateTotalQuantityAndPrice()
        val (membershipForm, membershipDiscount) = info.calculateMembershipDiscount()
        val discount = info.calculateDiscountFormat(membershipForm)
        val finalPrice = totalData.second - membershipDiscount - discount.second

        return Receipt(
            productReceipt = info.formatReceiptItems(),
            promotionRecipe = info.giftReceipt.formatGiftReceipt(),
            totalAmount = recipeTotalFormat(totalData.first, totalData.second.toKoreanUnit()),
            eventDiscount = discount.first,
            payment = recipeTotalPriceFormat(finalPrice.toKoreanUnit())
        )
    }
}
