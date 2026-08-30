package com.starledger.app.core.backup

import com.starledger.app.core.database.dao.AccountDao
import com.starledger.app.core.database.dao.CategoryDao
import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.EnvelopeDao
import com.starledger.app.core.database.dao.PurchaseDao
import com.starledger.app.core.database.dao.RuleDao
import com.starledger.app.core.database.dao.StarDao
import com.starledger.app.core.database.dao.TemplateDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.AccountType
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.CycleCloseReason
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.ForcedSavingType
import com.starledger.app.core.model.IncomeType
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.RuleType
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.model.Transaction
import com.starledger.app.core.model.TxType
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

const val BACKUP_VERSION = 1

@Singleton
class BackupManager @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val cycleDao: CycleDao,
    private val templateDao: TemplateDao,
    private val ruleDao: RuleDao,
    private val envelopeDao: EnvelopeDao,
    private val purchaseDao: PurchaseDao,
    private val starDao: StarDao,
) {

    suspend fun exportJson(): String {
        val root = JSONObject()
            .put("app", "StarLedger")
            .put("version", BACKUP_VERSION)
            .put("exportedAt", System.currentTimeMillis())

        root.put("accounts", JSONArray().apply {
            accountDao.getAll().forEach { put(accountToJson(it)) }
        })
        root.put("categories", JSONArray().apply {
            categoryDao.getAll().forEach { put(categoryToJson(it)) }
        })
        root.put("transactions", JSONArray().apply {
            transactionDao.getAll().forEach { put(transactionToJson(it)) }
        })
        root.put("cycles", JSONArray().apply {
            cycleDao.getAll().forEach { put(cycleToJson(it)) }
        })
        root.put("templates", JSONArray().apply {
            templateDao.getAll().forEach { put(templateToJson(it)) }
        })
        root.put("rules", JSONArray().apply {
            templateDao.getAll().forEach { t ->
                ruleDao.getByTemplate(t.id).forEach { put(ruleToJson(it)) }
            }
        })
        root.put("envelopes", JSONArray().apply {
            cycleDao.getAll().forEach { c ->
                envelopeDao.getByCycle(c.id).forEach { put(envelopeToJson(it)) }
            }
        })
        root.put("purchases", JSONArray().apply {
            purchaseDao.getAll().forEach { put(purchaseToJson(it)) }
        })
        root.put("stars", JSONArray().apply {
            starDao.getAll().forEach { put(starToJson(it)) }
        })
        return root.toString(2)
    }

    /** 导入：清空现有数据后写入备份。返回导入的记录数量。 */
    suspend fun importJson(json: String): Int {
        val root = JSONObject(json)
        if (root.optString("app") != "StarLedger") {
            throw IllegalArgumentException(if (isZh()) "不是星图账本的备份文件" else "Not a StarLedger backup file")
        }
        var count = 0

        // 清空（顺序：先删依赖表）
        val oldTxs = transactionDao.getAll()
        oldTxs.forEach { transactionDao.deleteById(it.id) }
        envelopeDao.getAllCycles().forEach { envelopeDao.deleteByCycle(it) }
        // 清空其他表
        clearTable()

        fun array(key: String): JSONArray = root.optJSONArray(key) ?: JSONArray()

        array("accounts").forEachObj { count++; accountDao.insert(accountFromJson(it)) }
        array("categories").forEachObj { count++; categoryDao.insert(categoryFromJson(it)) }
        array("cycles").forEachObj { count++; cycleDao.insert(cycleFromJson(it)) }
        array("templates").forEachObj { count++; templateDao.insert(templateFromJson(it)) }
        array("rules").forEachObj { count++; ruleDao.insert(ruleFromJson(it)) }
        array("envelopes").forEachObj { count++; envelopeDao.insert(envelopeFromJson(it)) }
        array("transactions").forEachObj { count++; transactionDao.insert(transactionFromJson(it)) }
        array("purchases").forEachObj { count++; purchaseDao.insert(purchaseFromJson(it)) }
        array("stars").forEachObj { count++; starDao.insert(starFromJson(it)) }
        return count
    }

    private suspend fun clearTable() {
        accountDao.getAll().forEach { accountDao.delete(it) }
        categoryDao.getAll().forEach { categoryDao.delete(it) }
        cycleDao.getAll().forEach { cycleDao.deleteById(it.id) }
        // 规则不随模板级联删除，需先收集再清理
        val allTemplates = templateDao.getAll()
        val allRules = allTemplates.flatMap { ruleDao.getByTemplate(it.id) }
        allRules.forEach { ruleDao.delete(it) }
        allTemplates.forEach { templateDao.delete(it) }
        purchaseDao.getAll().forEach { purchaseDao.deleteById(it.id) }
        starDao.getAll().forEach { starDao.deleteByCycle(it.cycleId) }
    }

    // ---------- CSV ----------

    suspend fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append(if (isZh()) "类型,日期,金额(元),账户,转入账户,分类,商户,备注\n" else "Type,Date,Amount,Account,To Account,Category,Merchant,Note\n")
        transactionDao.getAll().sortedBy { it.date }.forEach { tx ->
            val yuan = tx.amount / 100.0
            sb.append(tx.type.name).append(',')
            sb.append(tx.date).append(',')
            sb.append(String.format("%.2f", yuan)).append(',')
            sb.append(tx.accountId).append(',')
            sb.append(tx.toAccountId ?: "").append(',')
            sb.append(tx.categoryId ?: "").append(',')
            sb.append(csvEscape(tx.merchant)).append(',')
            sb.append(csvEscape(tx.note)).append('\n')
        }
        return sb.toString()
    }

    private fun csvEscape(s: String): String {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\""
        }
        return s
    }

    private fun isZh(): Boolean = java.util.Locale.getDefault().language == "zh"

    // ---------- JSON 序列化 ----------

    private fun accountToJson(a: Account) = JSONObject()
        .put("id", a.id).put("name", a.name).put("type", a.type.name)
        .put("initialBalance", a.initialBalance).put("currency", a.currency)
        .put("color", a.color).put("isCredit", a.isCredit).put("isHidden", a.isHidden)
        .put("includeInTotal", a.includeInTotal).put("sortOrder", a.sortOrder)
        .put("createdAt", a.createdAt).put("updatedAt", a.updatedAt)

    private fun accountFromJson(o: JSONObject) = Account(
        id = o.getLong("id"), name = o.getString("name"),
        type = AccountType.valueOf(o.getString("type")),
        initialBalance = o.getLong("initialBalance"), currency = o.optString("currency", "CNY"),
        color = o.getLong("color"), isCredit = o.optBoolean("isCredit"),
        isHidden = o.optBoolean("isHidden"), includeInTotal = o.optBoolean("includeInTotal", true),
        sortOrder = o.optInt("sortOrder"), createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"),
    )

    private fun categoryToJson(c: Category) = JSONObject()
        .put("id", c.id).put("name", c.name).put("icon", c.icon).put("color", c.color)
        .put("isExpense", c.isExpense).put("isMedical", c.isMedical)
        .put("sortOrder", c.sortOrder).put("createdAt", c.createdAt)

    private fun categoryFromJson(o: JSONObject) = Category(
        id = o.getLong("id"), name = o.getString("name"), icon = o.optString("icon", "📦"),
        color = o.optLong("color", 0xFF86A8FF), isExpense = o.optBoolean("isExpense", true),
        isMedical = o.optBoolean("isMedical"),
        sortOrder = o.optInt("sortOrder"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )

    private fun transactionToJson(t: Transaction) = JSONObject()
        .put("id", t.id).put("type", t.type.name).put("amount", t.amount)
        .put("accountId", t.accountId)
        .put("toAccountId", t.toAccountId ?: JSONObject.NULL)
        .put("categoryId", t.categoryId ?: JSONObject.NULL)
        .put("date", t.date).put("merchant", t.merchant).put("note", t.note)
        .put("tags", JSONArray(t.tags))
        .put("relatedPlanId", t.relatedPlanId ?: JSONObject.NULL)
        .put("cycleId", t.cycleId ?: JSONObject.NULL)
        .put("incomeType", t.incomeType?.name ?: JSONObject.NULL)
        .put("createdAt", t.createdAt).put("updatedAt", t.updatedAt)

    private fun transactionFromJson(o: JSONObject) = Transaction(
        id = o.getLong("id"), type = TxType.valueOf(o.getString("type")),
        amount = o.getLong("amount"), accountId = o.getLong("accountId"),
        toAccountId = if (o.isNull("toAccountId")) null else o.getLong("toAccountId"),
        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
        date = o.getLong("date"), merchant = o.optString("merchant"),
        note = o.optString("note"),
        tags = (0 until o.optJSONArray("tags").let { it?.length() ?: 0 })
            .map { i -> o.getJSONArray("tags").getString(i) },
        relatedPlanId = if (o.isNull("relatedPlanId")) null else o.getLong("relatedPlanId"),
        cycleId = if (o.isNull("cycleId")) null else o.getLong("cycleId"),
        incomeType = if (o.isNull("incomeType")) null else IncomeType.valueOf(o.getString("incomeType")),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun cycleToJson(c: BudgetCycle) = JSONObject()
        .put("id", c.id).put("name", c.name).put("year", c.year).put("month", c.month)
        .put("startDate", c.startDate).put("endDate", c.endDate).put("status", c.status.name)
        .put("totalIncome", c.totalIncome).put("totalAllocated", c.totalAllocated)
        .put("observationCompleteness", c.observationCompleteness.toDouble())
        .put("markedUnrecorded", c.markedUnrecorded)
        .put("reviewCompleted", c.reviewCompleted).put("surplusHandled", c.surplusHandled)
        .put("cycleMode", c.cycleMode.name)
        .put("closeReason", c.closeReason?.name ?: JSONObject.NULL)
        .put("maxRunDays", c.maxRunDays)
        .put("effectStartTime", c.effectStartTime ?: JSONObject.NULL)
        .put("forcedSavingType", c.forcedSavingType.name)
        .put("forcedSavingValue", c.forcedSavingValue)
        .put("forcedSavingAmount", c.forcedSavingAmount)
        .put("createdAt", c.createdAt).put("updatedAt", c.updatedAt)

    private fun cycleFromJson(o: JSONObject) = BudgetCycle(
        id = o.getLong("id"), name = o.getString("name"), year = o.getInt("year"),
        month = o.getInt("month"), startDate = o.getLong("startDate"),
        endDate = o.getLong("endDate"), status = CycleStatus.valueOf(o.getString("status")),
        totalIncome = o.getLong("totalIncome"), totalAllocated = o.getLong("totalAllocated"),
        observationCompleteness = o.optDouble("observationCompleteness", 0.0).toFloat(),
        markedUnrecorded = o.optBoolean("markedUnrecorded"),
        reviewCompleted = o.optBoolean("reviewCompleted"),
        surplusHandled = o.optBoolean("surplusHandled"),
        cycleMode = o.optString("cycleMode").takeIf { it.isNotEmpty() }
            ?.let { CycleMode.valueOf(it) } ?: CycleMode.CALENDAR_MONTH,
        closeReason = if (o.isNull("closeReason")) null
        else CycleCloseReason.valueOf(o.getString("closeReason")),
        maxRunDays = o.optInt("maxRunDays", 50),
        effectStartTime = if (o.isNull("effectStartTime")) null else o.getLong("effectStartTime"),
        forcedSavingType = o.optString("forcedSavingType").takeIf { it.isNotEmpty() }
            ?.let { ForcedSavingType.valueOf(it) } ?: ForcedSavingType.NONE,
        forcedSavingValue = o.optLong("forcedSavingValue"),
        forcedSavingAmount = o.optLong("forcedSavingAmount"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun templateToJson(t: AllocationTemplate) = JSONObject()
        .put("id", t.id).put("name", t.name).put("isDefault", t.isDefault).put("createdAt", t.createdAt)

    private fun templateFromJson(o: JSONObject) = AllocationTemplate(
        id = o.getLong("id"), name = o.getString("name"), isDefault = o.optBoolean("isDefault"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )

    private fun ruleToJson(r: AllocationRule) = JSONObject()
        .put("id", r.id).put("templateId", r.templateId).put("name", r.name)
        .put("categoryId", r.categoryId ?: JSONObject.NULL)
        .put("ruleType", r.ruleType.name).put("value", r.value).put("percent", r.percent)
        .put("priority", r.priority)
        .put("minAmount", r.minAmount ?: JSONObject.NULL)
        .put("maxAmount", r.maxAmount ?: JSONObject.NULL)
        .put("carryOver", r.carryOver).put("enabled", r.enabled)
        .put("envelopeType", r.envelopeType.name).put("color", r.color).put("sortOrder", r.sortOrder)

    private fun ruleFromJson(o: JSONObject) = AllocationRule(
        id = o.getLong("id"), templateId = o.getLong("templateId"), name = o.getString("name"),
        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
        ruleType = RuleType.valueOf(o.getString("ruleType")),
        value = o.optLong("value"), percent = o.optInt("percent"), priority = o.optInt("priority"),
        minAmount = if (o.isNull("minAmount")) null else o.getLong("minAmount"),
        maxAmount = if (o.isNull("maxAmount")) null else o.getLong("maxAmount"),
        carryOver = o.optBoolean("carryOver", true), enabled = o.optBoolean("enabled", true),
        envelopeType = EnvelopeType.valueOf(o.optString("envelopeType", "NECESSARY")),
        color = o.optLong("color", 0xFF86A8FF), sortOrder = o.optInt("sortOrder"),
    )

    private fun envelopeToJson(e: BudgetEnvelope) = JSONObject()
        .put("id", e.id).put("cycleId", e.cycleId).put("name", e.name)
        .put("plannedAmount", e.plannedAmount).put("actualAmount", e.actualAmount)
        .put("remainingAmount", e.remainingAmount).put("type", e.type.name)
        .put("categoryId", e.categoryId ?: JSONObject.NULL)
        .put("color", e.color).put("carryOverEnabled", e.carryOverEnabled).put("sortOrder", e.sortOrder)

    private fun envelopeFromJson(o: JSONObject) = BudgetEnvelope(
        id = o.getLong("id"), cycleId = o.getLong("cycleId"), name = o.getString("name"),
        plannedAmount = o.getLong("plannedAmount"), actualAmount = o.getLong("actualAmount"),
        remainingAmount = o.getLong("remainingAmount"),
        type = EnvelopeType.valueOf(o.getString("type")),
        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
        color = o.getLong("color"), carryOverEnabled = o.optBoolean("carryOverEnabled", true),
        sortOrder = o.optInt("sortOrder"),
    )

    private fun purchaseToJson(p: PlannedPurchase) = JSONObject()
        .put("id", p.id).put("name", p.name).put("estimatedAmount", p.estimatedAmount)
        .put("reason", p.reason).put("alternative", p.alternative).put("createdAt", p.createdAt)
        .put("earliestDecisionDate", p.earliestDecisionDate)
        .put("targetDate", p.targetDate ?: JSONObject.NULL)
        .put("coolingDays", p.coolingDays)
        .put("sourceEnvelopeId", p.sourceEnvelopeId ?: JSONObject.NULL)
        .put("status", p.status.name).put("note", p.note)
        .put("purchasedTransactionId", p.purchasedTransactionId ?: JSONObject.NULL)

    private fun purchaseFromJson(o: JSONObject) = PlannedPurchase(
        id = o.getLong("id"), name = o.getString("name"),
        estimatedAmount = o.getLong("estimatedAmount"), reason = o.optString("reason"),
        alternative = o.optString("alternative"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        earliestDecisionDate = o.getLong("earliestDecisionDate"),
        targetDate = if (o.isNull("targetDate")) null else o.getLong("targetDate"),
        coolingDays = o.optInt("coolingDays", 7),
        sourceEnvelopeId = if (o.isNull("sourceEnvelopeId")) null else o.getLong("sourceEnvelopeId"),
        status = PlanStatus.valueOf(o.getString("status")), note = o.optString("note"),
        purchasedTransactionId = if (o.isNull("purchasedTransactionId")) null else o.getLong("purchasedTransactionId"),
    )

    private fun starToJson(s: MonthlyStar) = JSONObject()
        .put("id", s.id).put("cycleId", s.cycleId).put("year", s.year).put("month", s.month)
        .put("brightness", s.brightness.toDouble()).put("colorState", s.colorState.name)
        .put("observationCompleteness", s.observationCompleteness.toDouble())
        .put("allocationCompletion", s.allocationCompletion.toDouble())
        .put("budgetStatus", s.budgetStatus.toDouble())
        .put("reviewCompleted", s.reviewCompleted).put("surplusAmount", s.surplusAmount)
        .put("snapshotData", s.snapshotData)
        .put("createdAt", s.createdAt).put("updatedAt", s.updatedAt)

    private fun starFromJson(o: JSONObject) = MonthlyStar(
        id = o.getLong("id"), cycleId = o.getLong("cycleId"), year = o.getInt("year"),
        month = o.getInt("month"), brightness = o.optDouble("brightness", 0.0).toFloat(),
        colorState = StarColorState.valueOf(o.optString("colorState", "BLUE")),
        observationCompleteness = o.optDouble("observationCompleteness", 0.0).toFloat(),
        allocationCompletion = o.optDouble("allocationCompletion", 0.0).toFloat(),
        budgetStatus = o.optDouble("budgetStatus", 0.0).toFloat(),
        reviewCompleted = o.optBoolean("reviewCompleted"),
        surplusAmount = o.optLong("surplusAmount"),
        snapshotData = o.optString("snapshotData"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
    )

    private inline fun JSONArray.forEachObj(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            block(getJSONObject(i))
        }
    }
}
