package store.domain.usecase

import store.domain.ext.removeStockUnitSuffix
import store.domain.ext.splitByComma
import store.domain.ext.splitByHyphen
import store.domain.model.Constants.COMMA
import store.domain.model.output.OutputRules
import store.domain.model.product.Products
import store.domain.model.Constants.SQUARE_BRACKETS_LEFT
import store.domain.model.Constants.SQUARE_BRACKETS_RIGHT
import store.domain.model.Exception
import store.domain.model.Exception.NOT_SALES

/**
 * 주문의 유효성을 검사하는 UseCase 클래스.
 *
 * @property extractOrdersUseCase 주문을 파싱하여 제품 이름과 수량을 추출하는 유스케이스
 */
class CheckOrderValidationUseCase(
    private val extractOrdersUseCase: ExtractOrdersUseCase
) {

    /**
     * 주문 문자열의 유효성을 검증하고, 유효한 경우 주문을 처리하여 제품별 수량을 반환.
     *
     * @param order 주문 문자열
     * @param products 유효한 제품 목록
     * @return Map<String, Int> 형태의 제품별 주문 수량
     */
    operator fun invoke(order: String, products: Products): Map<String, Int> {
        checkCommonValidation(order)           // 기본적인 유효성 검사
        checkOrderDelimiterValidation(order)    // 구분자 유효성 검사
        val result = checkOrder(order, products) // 주문 상세 검증 및 처리
        return result
    }

    /**
     * 주문 문자열의 공통 유효성 검증 (빈 문자열 및 포맷 검사).
     *
     * @param order 주문 문자열
     * @throws Exception.INVALID_INPUT 입력이 비어 있는 경우
     * @throws Exception.INVALID_INPUT_FORMAT 입력 형식이 잘못된 경우
     */
    private fun checkCommonValidation(order: String) {
        require(order.isNotEmpty()) { Exception.INVALID_INPUT }
        require(order.matches(REGEX)) { Exception.INVALID_INPUT_FORMAT }
    }

    /**
     * 주문의 제품 이름과 수량을 검증하고 유효한 제품인 경우 집계하여 반환.
     *
     * @param order 주문 문자열
     * @param products 유효한 제품 목록
     * @return Map<String, Int> 형태의 제품별 주문 수량
     */
    private fun checkOrder(order: String, products: Products): Map<String, Int> {
        val extractedOrders = extractOrdersUseCase(order)
        extractedOrders.forEach { (name, quantity) ->
            validQuantity(quantity.toString())   // 수량 유효성 검증
            hasProduct(name, products)           // 제품 존재 여부 검증
            outOfStock(name, products)           // 재고 확인
            notEnoughStock(name, quantity, products) // 충분한 재고 확인
        }
        return extractedOrders
    }

    /**
     * 주문의 구분자 및 대괄호 유효성 검사.
     *
     * @param order 주문 문자열
     */
    private fun checkOrderDelimiterValidation(order: String) {
        if (order.contains("$COMMA")) {
            order.splitByComma().forEach {
                checkSquareBrackets(order)       // 대괄호 체크
                checkDelimiter(it)               // 하이픈 구분자 체크
            }
        } else {
            checkSquareBrackets(order)
            checkDelimiter(order)
        }
    }

    /**
     * 주문 문자열의 대괄호 유효성 검사.
     *
     * @param order 주문 문자열
     * @throws Exception.INVALID_INPUT_FORMAT 대괄호 형식이 올바르지 않을 때
     */
    private fun checkSquareBrackets(order: String) {
        val startsWithSquareBrackets = order.startsWith("$SQUARE_BRACKETS_LEFT")
        val endsWithSquareBrackets = order.endsWith("$SQUARE_BRACKETS_RIGHT")
        require(startsWithSquareBrackets && endsWithSquareBrackets) {
            Exception.INVALID_INPUT_FORMAT
        }
    }

    /**
     * 주문 문자열의 하이픈 구분자 유효성 검사.
     *
     * @param order 주문 항목 문자열
     * @throws Exception.INVALID_INPUT_FORMAT 올바른 하이픈 구분자가 없는 경우
     */
    private fun checkDelimiter(order: String) {
        val separatedOrder = order.splitByHyphen()
        require(separatedOrder.size == SEPARATED_SIZE) {
            Exception.INVALID_INPUT_FORMAT
        }
    }

    /**
     * 제품 목록에 주문한 제품이 존재하는지 확인.
     *
     * @param order 주문 제품 이름
     * @param products 유효한 제품 목록
     * @throws Exception.NOT_SALES 제품이 판매 목록에 없을 경우
     */
    private fun hasProduct(order: String, products: Products) {
        require(products.items.any { it.name == order }) { NOT_SALES }
    }

    /**
     * 주문한 제품 수량의 유효성 확인.
     *
     * @param productQuantity 제품 수량 문자열
     * @throws Exception.INVALID_INPUT 수량이 유효한 숫자가 아닌 경우
     */
    private fun validQuantity(productQuantity: String) {
        require(productQuantity.toIntOrNull() != null) { Exception.INVALID_INPUT }
    }

    /**
     * 주문한 제품이 품절 상태인지 확인.
     *
     * @param name 주문 제품 이름
     * @param products 유효한 제품 목록
     * @throws Exception.NOT_ENOUGH_STOCK 제품이 품절 상태일 경우
     */
    private fun outOfStock(name: String, products: Products) {
        val stockQuantity = getStockQuantity(name, products)
        val notOutOfStock = stockQuantity.any { it != "${OutputRules.OUT_OF_STOCK}" }
        require(notOutOfStock) { Exception.NOT_ENOUGH_STOCK }
    }

    /**
     * 주문한 제품의 재고가 충분한지 확인.
     *
     * @param name 주문 제품 이름
     * @param quantity 주문 수량
     * @param products 유효한 제품 목록
     * @throws Exception.NOT_ENOUGH_STOCK 재고가 충분하지 않은 경우
     */
    private fun notEnoughStock(name: String, quantity: Int, products: Products) {
        val stockQuantity = getNotOutOfStockQuantity(name, products)
        require(stockQuantity >= quantity) { Exception.NOT_ENOUGH_STOCK }
    }

    /**
     * 특정 제품의 전체 재고 수량을 반환.
     *
     * @param name 제품 이름
     * @param products 유효한 제품 목록
     * @return List<String> 형태의 제품별 재고 수량 목록
     */
    private fun getStockQuantity(name: String, products: Products): List<String> {
        return products.items.filter { it.name == name }.map { it.quantity }
    }

    /**
     * 품절이 아닌 특정 제품의 총 재고 수량을 반환.
     *
     * @param name 제품 이름
     * @param products 유효한 제품 목록
     * @return Int 형태의 재고 수량 합계
     */
    private fun getNotOutOfStockQuantity(name: String, products: Products): Int {
        return products.items
            .filter { it.name == name && it.quantity != "${OutputRules.OUT_OF_STOCK}" }
            .map { it.quantity.removeStockUnitSuffix() }
            .sumOf { it }
    }

    companion object {
        // 한글/영문/숫자/대괄호,쉼표의 유효성 검사 정규식
        private val REGEX = Regex("^[가-힣a-zA-Z0-9,\\-\\[\\]]+$")
        private const val SEPARATED_SIZE = 2 // 주문 항목 분리 크기
    }
}
