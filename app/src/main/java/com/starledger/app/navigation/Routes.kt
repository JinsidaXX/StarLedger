package com.starledger.app.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val LANGUAGE = "language"
    const val MAIN = "main"

    const val TRANSACTION_ADD = "transaction_add"
    const val TRANSACTION_EDIT = "transaction_edit/{transactionId}"
    fun transactionEdit(id: Long) = "transaction_edit/$id"

    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"

    const val PLAN_EDIT = "plan_edit?planId={planId}"
    fun planEdit(planId: Long?) = "plan_edit?planId=${planId ?: -1}"

    const val PLAN_DETAIL = "plan_detail/{planId}"
    fun planDetail(id: Long) = "plan_detail/$id"

    const val STAR_DETAIL = "star_detail/{cycleId}"
    fun starDetail(cycleId: Long) = "star_detail/$cycleId"

    const val REVIEW = "review/{cycleId}"
    fun review(cycleId: Long) = "review/$cycleId"

    const val TEMPLATE_EDIT = "template_edit/{templateId}"
    fun templateEdit(templateId: Long) = "template_edit/$templateId"

    const val SETTINGS = "settings"

    const val OWNED_ITEMS = "owned_items"
}
