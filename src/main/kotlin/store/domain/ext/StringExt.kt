package store.domain.ext

import store.domain.model.Constants
import store.domain.model.Constants.SQUARE_BRACKETS_LEFT
import store.domain.model.Constants.SQUARE_BRACKETS_RIGHT

fun String.isNumeric(): Boolean {
    return this.all { it.isDigit() }
}

fun String.splitByComma(): List<String> = this.split(Constants.COMMA.toString())

fun String.splitByHyphen(): List<String> = this.split(Constants.HYPHEN.toString())

fun String.extractProductName(): String {
    val productName = this.splitByHyphen().first()
    return productName.trim().removePrefix("$SQUARE_BRACKETS_LEFT")
}

fun String.extractProductQuantity(): String {
    val productQuantity = this.splitByHyphen().last()
    return productQuantity.trim().removeSuffix("$SQUARE_BRACKETS_RIGHT")
}