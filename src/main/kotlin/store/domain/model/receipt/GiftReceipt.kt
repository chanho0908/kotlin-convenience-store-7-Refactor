package store.domain.model.receipt

import store.domain.model.output.OutputRules
import store.domain.model.output.OutputRules.Companion.recipePromotionFormat

data class GiftReceipt(
    val items: MutableMap<String, Int>,
    val notReceivedPromotion: List<String>
) {
    fun createNotReceivedPromotionMsg(): List<String>? {
        if (notReceivedPromotion.isNotEmpty()) {
            val notReceivedPromotionMsg = mutableListOf<String>()
            notReceivedPromotion.forEach {
                notReceivedPromotionMsg.add(OutputRules.notReceivedPromotionFormat(it))
            }
            return notReceivedPromotionMsg
        }
        return null
    }

    fun removeNotReceivedPromotion(idx: Int): List<String> {
        return this.notReceivedPromotion.filterNot { it == notReceivedPromotion[idx] }
    }

    fun addNotReceivedPromotion(idx: Int){
        val notReceivedPromotion = this.notReceivedPromotion[idx]
        items[notReceivedPromotion] = items.getOrDefault(notReceivedPromotion, 0) + 1
    }

    fun formatGiftReceipt(): String {
        return this.items.entries.joinToString("\n") { (name, quantity) ->
            recipePromotionFormat(name, quantity)
        }
    }
}