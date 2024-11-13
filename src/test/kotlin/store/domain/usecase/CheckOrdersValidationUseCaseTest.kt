package store.domain.usecase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import store.data.datasource.ProductDataSource
import store.data.datasource.PromotionDataSource
import store.data.repository.ProductRepositoryImpl
import store.data.repository.PromotionRepositoryImpl
import store.domain.model.Exception
import store.domain.model.product.Products
import store.domain.model.promotion.Promotions

class CheckOrdersValidationUseCaseTest {

    private lateinit var products: Products
    private lateinit var promotions: Promotions
    private lateinit var extractOrdersUseCase: ExtractOrdersUseCase
    private lateinit var checkOrderValidationUseCase: CheckOrderValidationUseCase

    @BeforeEach
    fun setUp() {
        products = ProductRepositoryImpl(ProductDataSource()).getProduct()
        promotions = PromotionRepositoryImpl(PromotionDataSource()).getPromotion()
        extractOrdersUseCase = ExtractOrdersUseCase()
        checkOrderValidationUseCase = CheckOrderValidationUseCase(extractOrdersUseCase)
    }

    @Test
    fun `입력이 비어있을 때 INVALID_INPUT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT}")
    }

    @Test
    fun `입력 형식이 잘못되었을 때 INVALID_INPUT_FORMAT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[소주-1];[맥주-2]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT_FORMAT}")
    }

    @Test
    fun `존재하지 않는 제품명을 입력했을 때 NOT_SALES 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[소주-100],[맥주-2]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.NOT_SALES}")
    }

    @Test
    fun `수량이 정수가 아닐 때 INVALID_INPUT_FORMAT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[콜라-ㅇㄴㅇㅈ],[사이다-;;;]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT_FORMAT}")
    }

    @Test
    fun `재고가 부족할 때 NOT_ENOUGH_STOCK 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[콜라-100]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.NOT_ENOUGH_STOCK}")
    }

    @Test
    fun `중복_상품_입력시 재고가 부족할 때 NOT_ENOUGH_STOCK 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[콜라-5],[콜라-5],[콜라-5],[콜라-5],[콜라-5]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.NOT_ENOUGH_STOCK}")
    }

    @Test
    fun `상품과 수량의구분자가 잘못되었을 때 INVALID_INPUT_FORMAT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[콜라;1,],[사이다-;;;]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT_FORMAT}")
    }

    @Test
    fun `상품들의 구분자가 잘못되었을 때 INVALID_INPUT_FORMAT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("[콜라-1,];[사이다-2]", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT_FORMAT}")
    }

    @Test
    fun `대괄호가 없을 때 INVALID_INPUT_FORMAT 예외가 발생한다`() {
        Assertions.assertThatThrownBy {
            checkOrderValidationUseCase("콜라-1,사이다-2", products)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("${Exception.INVALID_INPUT_FORMAT}")
    }

    @Test
    fun `유효한 입력일 때 Purchase 인스턴스가 정상적으로 생성된다`() {
        assertDoesNotThrow {
            checkOrderValidationUseCase("[콜라-2],[사이다-1]", products)
        }
    }
}
