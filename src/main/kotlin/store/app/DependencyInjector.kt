package store.app

import store.data.datasource.ProductDataSource
import store.data.datasource.PromotionDataSource
import store.data.repository.ProductRepositoryImpl
import store.data.repository.PromotionRepositoryImpl
import store.domain.repository.ProductRepository
import store.domain.repository.PromotionRepository
import store.domain.usecase.CheckOrderValidationUseCase
import store.domain.usecase.ExtractOrdersUseCase
import store.domain.usecase.GetInProgressPromotionUseCase
import store.domain.usecase.MakeOutReceiptUseCase
import store.domain.usecase.ValidateYesNoInputUseCase
import store.presentation.view.InputView
import store.presentation.view.MainView
import store.presentation.view.OutputView
import store.presentation.vm.ViewModel

class DependencyInjector {

    fun injectMainView(): MainView {
        val viewModel = injectViewModel()
        val inputView = injectInputView()
        val outputView = injectOutputView()
        return MainView(viewModel, inputView, outputView)
    }

    private fun injectViewModel(): ViewModel {
        return ViewModel(
            injectProductRepository(),
            CheckOrderValidationUseCase(ExtractOrdersUseCase()),
            GetInProgressPromotionUseCase(injectPromotionRepository()),
            ValidateYesNoInputUseCase(),
            MakeOutReceiptUseCase()
        )
    }

    private fun injectPromotionRepository(): PromotionRepository {
        val injectPromotionDataSource = injectPromotionDataSource()
        return PromotionRepositoryImpl(injectPromotionDataSource)
    }

    private fun injectProductRepository(): ProductRepository {
        val injectProductDataSource = injectProductDataSource()
        return ProductRepositoryImpl(injectProductDataSource)
    }

    private fun injectProductDataSource(): ProductDataSource = ProductDataSource()
    private fun injectPromotionDataSource(): PromotionDataSource = PromotionDataSource()
    private fun injectInputView(): InputView = InputView()
    private fun injectOutputView(): OutputView = OutputView()
}