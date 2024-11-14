package store.domain.model.output

import store.domain.model.product.ProductItem

enum class OutputRules(private val msg: String) {
    WELCOME("안녕하세요. W편의점입니다.\n현재 보유하고 있는 상품입니다.\n"),
    GUIDE("구매하실 상품명과 수량을 입력해 주세요. (예: [사이다-2],[감자칩-1])"),
    OUT_OF_STOCK("재고 없음"),
    STOCK_UNIT("개"),
    PRODUCT("- %s %s원 %s %s"),
    NOT_RECEIVED_PROMOTION("\n현재 %s은(는) 1개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)"),
    STORAGE_STOCK("\n현재 %s %s개는 프로모션 할인이 적용되지 않습니다. 그래도 구매하시겠습니까? (Y/N)"),
    MEMBERSHIP_DISCOUNT("\n멤버십 할인을 받으시겠습니까? (Y/N)"),
    MEMBERSHIP_DISCOUNT_MAX("8000"),
    RECIPE_CATEGORY(
            """
    ==============W 편의점================
    상품명		        수량	    금액
        """.trimIndent()),
    RECIPE_PRODUCT("%s\t\t\t\t\t%d\t%s"),
    RECIPE_PROMOTION("""
        =============증  	정===============
    """.trimIndent()),
    RECIPE_PROMOTION_PRODUCT("%s\t\t\t\t\t%d"),
    DOTTED_LINE("===================================="),
    RECIPE_TOTAL("총구매액\t\t\t\t%d\t\t%s"),
    RECIPE_EVENT_DISCOUNT("행사할인\t\t\t\t\t\t-%s"),
    RECIPE_MEMBERSHIP_DISCOUNT("멤버십할인\t\t\t\t\t\t-%s"),
    RECIPE_TOTAL_PRICE("내실돈\t\t\t\t\t\t%s"),
    END("\n감사합니다. 구매하고 싶은 다른 상품이 있나요? (Y/N)"),
    YES("Y"),
    NO("N");

    override fun toString(): String = msg

    companion object {
        fun productFormat(product: ProductItem): String {
            val promotion = if (product.promotion == null) "" else "${product.promotion}"
            return PRODUCT.msg.format(
                product.name,
                product.price,
                product.quantity,
                promotion
            ).trim()
        }

        fun notReceivedPromotionFormat(name: String): String {
            return NOT_RECEIVED_PROMOTION.toString().format(name)
        }

        fun storageStockFormat(name: String, quantity: Int): String {
            return STORAGE_STOCK.toString().format(name, quantity)
        }

        fun recipeProductFormat(name: String, quantity: Int, price: String): String {
            return RECIPE_PRODUCT.toString().format(name, quantity, price)
        }

        fun recipePromotionFormat(name: String, quantity: Int): String {
            return RECIPE_PROMOTION_PRODUCT.toString().format(name, quantity)
        }

        fun recipeTotalFormat(totalQuantity: Int, totalPrice: String): String {
            return RECIPE_TOTAL.toString().format(totalQuantity, totalPrice)
        }

        fun recipeEventDiscountFormat(eventDiscount: String): String {
            return RECIPE_EVENT_DISCOUNT.toString().format(eventDiscount)
        }

        fun recipeMembershipDiscountFormat(membershipDiscount: String): String {
            return RECIPE_MEMBERSHIP_DISCOUNT.toString().format(membershipDiscount)
        }

        fun recipeTotalPriceFormat(totalPrice: String): String {
            return RECIPE_TOTAL_PRICE.toString().format(totalPrice)
        }

        fun memberShipDiscountMax(): Int = MEMBERSHIP_DISCOUNT_MAX.toString().toInt()
    }
}