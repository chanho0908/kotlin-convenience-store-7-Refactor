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
        val totalData = calculateTotalQuantityAndPrice(info)
        val (membershipForm, membershipDiscount) =
            calculateMembershipDiscount(info.isMembershipApply, info.getNonPromotionPrice())
        val discount = calculateDiscountFormat(membershipForm, info)

        val finalPrice = totalData.totalPrice - membershipDiscount - discount.second

        return Receipt(
            formatReceiptItems(info),
            formatGiftReceipt(info.giftReceipt),
            recipeTotalFormat(totalData.totalQuantity, totalData.totalPrice.toKoreanUnit()),
            discount.first,
            recipeTotalPriceFormat(finalPrice.toKoreanUnit())
        )
    }

    private fun formatReceiptItems(info : InformationForMakeReceipt): String {
        val productQuantities = info.paymentReceipt.items.map { product ->
            val totalQuantity = info.getSumOfProductQuantity(product)
            val price = (product.originPrice * totalQuantity).toKoreanUnit()
            recipeProductFormat(product.name, totalQuantity, price)
        }
        return productQuantities.joinToString("\n")
    }

    private fun formatGiftReceipt(gift: GiftReceipt): String {
        return gift.items.entries.joinToString("\n") { (name, quantity) ->
            recipePromotionFormat(name, quantity)
        }
    }

    private fun calculateDiscountFormat(membershipForm: String, info: InformationForMakeReceipt)
    : Pair<String, Int> {
        val discount = calculateGiftDiscount(info.paymentReceipt, info.giftReceipt)
        val eventDiscountFormat = recipeEventDiscountFormat(discount.toKoreanUnit())
        return Pair("$eventDiscountFormat\n$membershipForm", discount)
    }

    private fun calculateGiftDiscount(paymentReceipt: PaymentReceipt, giftReceipt: GiftReceipt): Int {
        return giftReceipt.items.entries.sumOf { (giftName, giftQuantity) ->
            paymentReceipt.items.find { it.name == giftName }?.originPrice?.times(giftQuantity) ?: 0
        }
    }

    private fun calculateTotalQuantityAndPrice(info : InformationForMakeReceipt): TotalData {
        val totalQuantity = info.paymentReceipt.items.sumOf { product ->
            info.getSumOfProductQuantity(product)
        }
        val totalPrice = info.paymentReceipt.items.sumOf { product ->
            product.originPrice * info.getSumOfProductQuantity(product)
        }
        return TotalData(totalQuantity, totalPrice)
    }

    private fun calculateMembershipDiscount(membership: Boolean, nonPromotionPrice: Int): Pair<String, Int> {
        return if (membership) {
            applyMembershipDiscount(nonPromotionPrice)
        } else {
            Pair(recipeMembershipDiscountFormat("0"), 0)
        }
    }

    private fun applyMembershipDiscount(nonPromotionPrice: Int): Pair<String, Int> {
        val discount = (nonPromotionPrice * 0.3).toInt().coerceAtMost(memberShipDiscountMax())
        return recipeMembershipDiscountFormat(discount.toKoreanUnit()) to discount
    }

    private data class TotalData(val totalQuantity: Int, val totalPrice: Int)
}
