package store.domain.usecase

import store.domain.ext.splitByComma
import store.domain.ext.splitByHyphen
import store.domain.model.Constants.SQUARE_BRACKETS_LEFT
import store.domain.model.Constants.SQUARE_BRACKETS_RIGHT

/**
 * 주문 목록 문자열을 파싱하여 제품 이름과 수량을 집계하는 UseCase 클래스.
 *
 * @sample "[콜라-10],[사이다-10]"과 같은 입력을 받아 제품별 수량을 합산하여 Map으로 반환.
 */
class ExtractOrdersUseCase {

    /**
     * 주문 목록 문자열을 파싱하고 제품별로 수량을 집계하여 반환.
     *
     * @param orderList 콤마(,)로 구분된 제품명과 수량 문자열 목록
     * @return Map<제품명, 수량> 형태의 제품별 총 주문 수량
     */
    operator fun invoke(orderList: String): Map<String, Int> {
        // 콤마로 분리하여 개별 주문 항목을 처리
        val orders = orderList.splitByComma().map {
            // 각 항목의 대괄호를 제거하고
            val extracted = it.removeSurrounding("$SQUARE_BRACKETS_LEFT", "$SQUARE_BRACKETS_RIGHT")

            // 제품명과 수량을 하이픈으로 분리
            val splitOrder = extracted.splitByHyphen()
            Pair(splitOrder[0], splitOrder[1].toInt())
        }

        // 제품 이름별로 수량을 집계
        val groupedOrder = groupByProductName(orders)
        return groupedOrder
    }

    /**
     * 제품명으로 그룹화하여 수량을 합산한 후 Map으로 반환.
     *
     * @param orders 제품명과 수량으로 이루어진 Pair 목록
     * @return Map<String, Int> 형태의 제품별 총 수량
     */
    private fun groupByProductName(orders: List<Pair<String, Int>>): Map<String, Int> {
        return orders.groupBy { it.first }
            .map { (productName, orders) ->
                // 각 제품 이름에 대해 수량을 합산하여 Pair로 반환
                productName to orders.sumOf { it.second }
            }.toMap() // List<Pair<String, Int>> -> Map<String, Int> 변환
    }
}
