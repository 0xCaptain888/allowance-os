package com.captain.allowanceos

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class MainActivity : ComponentActivity() {
    private val viewModel: AllowanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sender = ActivityResultSender(this)
        setContent {
            MaterialTheme(colors = darkColors(background = Ink, surface = Panel, primary = Mint)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    AllowanceApp(viewModel, sender)
                }
            }
        }
    }
}

@Composable
private fun AllowanceApp(viewModel: AllowanceViewModel, sender: ActivityResultSender) {
    val state by viewModel.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(AppPage.HOME) }
    var chinese by rememberSaveable { mutableStateOf(viewModel.preferredChinese) }
    var showOnboarding by rememberSaveable { mutableStateOf(!viewModel.hasCompletedOnboarding) }
    BackHandler(enabled = page != AppPage.HOME && !state.loading) { page = AppPage.HOME }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0D1021), Ink, Color(0xFF070A12))),
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AppHeader(chinese = chinese, onLanguageToggle = {
                chinese = !chinese
                viewModel.setPreferredChinese(chinese)
            })
            Box(modifier = Modifier.weight(1f)) {
                when (page) {
                    AppPage.HOME -> OverviewPage(state, viewModel, sender, chinese) { page = it }
                    AppPage.SERVICES -> ServicesPage(state, viewModel, chinese) { page = it }
                    AppPage.ALLOWANCES -> PolicyPage(state, viewModel, sender, chinese)
                    AppPage.ACTIVITY -> ActivityPage(state, viewModel, chinese)
                    AppPage.EVIDENCE -> EvidencePage(state, viewModel, chinese)
                }
            }
            BottomBar(page = page, chinese = chinese, onSelect = { page = it })
        }

        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xCC080B11)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Mint)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        state.loadingMessage.ifBlank { t("正在处理请求…", "Processing request…", chinese) },
                        color = White,
                    )
                }
            }
        }
    }

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(t("欢迎使用 Allowance OS", "Welcome to Allowance OS", chinese), color = White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t("在开始前，请先确认当前产品边界：", "Before continuing, understand the current product boundary:", chinese), color = White)
                    BoundaryRow("DEVNET", t("所有钱包与链上操作均位于 Solana Devnet。", "All wallet and onchain actions use Solana Devnet.", chinese), Blue)
                    BoundaryRow("NO CUSTODY", t("私钥始终留在钱包中；应用只保存加密的 MWA 重连令牌。", "Private keys remain in the wallet; the app stores only an encrypted MWA reconnect token.", chinese), Mint)
                    BoundaryRow("PRE-PRODUCTION", t("Memo 是授权证明，不是支付；自动周期扣款架构尚未上线。", "A Memo is authorization proof, not payment; autonomous recurring settlement is not yet live.", chinese), Amber)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.completeOnboarding()
                    showOnboarding = false
                }) { Text(t("了解并开始", "Understand and continue", chinese), color = Mint, fontWeight = FontWeight.Black) }
            },
            backgroundColor = Panel,
        )
    }
}

@Composable
private fun AppHeader(chinese: Boolean, onLanguageToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Mint),
            contentAlignment = Alignment.Center,
        ) { Text("A", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Allowance OS · v0.11.0", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(t("Web3 服务支付与授权层", "Payment authorization for Web3 services", chinese), color = Muted, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(androidx.compose.ui.graphics.Color(0xFF142820)).padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(Mint, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("DEVNET", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Line, RoundedCornerShape(10.dp))
                .clickable(onClick = onLanguageToggle).padding(horizontal = 10.dp, vertical = 7.dp),
        ) { Text(if (chinese) "EN" else "中文", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun OverviewPage(
    state: AllowanceUiState,
    viewModel: AllowanceViewModel,
    sender: ActivityResultSender,
    chinese: Boolean,
    navigate: (AppPage) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val activeTemplate = CommercialCatalog.byId(state.selectedServiceId)
    PageColumn {
        Text(t("所有 Web3 服务，一个授权中心", "One authorization layer for every Web3 service", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(
            t("订阅、报告、信号、交易机器人和 API，都使用可见、可验证、可撤销的支付边界。", "Subscriptions, reports, signals, trading bots, and APIs share visible, verifiable, revocable payment boundaries.", chinese),
            color = Muted,
            fontSize = 15.sp,
        )

        ProductCard {
            SectionTitle(t("当前运行模式", "CURRENT OPERATING MODE", chinese), t("预生产", "PRE-PRODUCTION", chinese))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyTag("SOLANA DEVNET", Blue)
                TinyTag(t("非托管", "NON-CUSTODIAL", chinese), Mint)
                TinyTag(t("不自动扣款", "NO AUTO-DEBIT", chinese), Amber)
            }
            Text(
                t("策略回放、钱包 Memo 证明与历史 Program 结算是三个独立证据层。当前 App 不会自动从钱包扣款。", "Policy replay, wallet Memo proof, and recorded Program settlement are three separate evidence layers. This app does not automatically debit a wallet.", chinese),
                color = Muted,
                fontSize = 12.sp,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(if (state.walletAddress.isBlank()) PanelRaised else androidx.compose.ui.graphics.Color(0xFF10251F)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(t("钱包授权", "WALLET AUTHORIZATION", chinese), if (state.walletAddress.isBlank()) t("未连接", "DISCONNECTED", chinese) else t("已授权", "AUTHORIZED", chinese))
            if (state.walletAddress.isBlank()) {
                Text(t("连接 Phantom 或任意 MWA 钱包，为多个服务创建统一授权。", "Connect Phantom or any MWA wallet to control multiple services from one place.", chinese), color = Muted)
                PrimaryButton(t("连接 Phantom / MWA 钱包", "Connect Phantom / MWA wallet", chinese)) { viewModel.connect(sender) }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t("Devnet 余额", "Devnet balance", chinese), color = Muted, fontSize = 12.sp)
                        Text("${state.solBalance?.let { "%.4f".format(it) } ?: "—"} SOL", color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    StatusDot(t("会话已保存", "SESSION SAVED", chinese))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(short(state.walletAddress), modifier = Modifier.weight(1f), color = Muted, fontSize = 13.sp)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(state.walletAddress)) }) {
                        Text(t("复制地址", "COPY ADDRESS", chinese), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if ((state.solBalance ?: 0.0) <= 0.0) Notice(t("需要少量 Devnet SOL 才能发布授权证明。", "Add a small amount of Devnet SOL before publishing a proof.", chinese), Amber)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(Modifier.weight(1f), t("刷新余额", "Refresh", chinese)) { viewModel.refreshBalance() }
                    SecondaryButton(Modifier.weight(1f), t("重新连接", "Reconnect", chinese)) { viewModel.connect(sender) }
                }
                TextButton(onClick = { viewModel.forgetLocalConnection() }, contentPadding = PaddingValues(0.dp)) {
                    Text(t("清除本地钱包会话", "Forget local wallet session", chinese), color = Rose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(t("清除本地会话不会改变 Phantom 资产、私钥或链上状态。", "Forgetting this local session never changes Phantom funds, keys, or onchain state.", chinese), color = Muted, fontSize = 11.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "5", t("模板", "TEMPLATES", chinese), t("商业场景", "USE CASES", chinese), Mint)
            MetricCard(Modifier.weight(1f), "55", "USDC", t("组合预算", "CONTROLLED", chinese), Blue)
            MetricCard(Modifier.weight(1f), "4", "GUARDS", t("每笔请求", "PER REQUEST", chinese), Amber)
        }

        ProductCard {
            SectionTitle(t("服务目录", "SERVICE CATALOG", chinese), t("可立即试用", "READY", chinese))
            Text(t("从可复用策略开始，而不是每次重新理解授权风险。", "Start from reusable policies instead of rebuilding payment safety for every app.", chinese), color = Muted, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommercialCatalog.templates.forEach { template ->
                    ServiceMiniCard(template, state.selectedServiceId == template.id, chinese) {
                        viewModel.selectCommercialTemplate(template.id)
                    }
                }
            }
            TextButton(onClick = { navigate(AppPage.SERVICES) }, contentPadding = PaddingValues(0.dp)) {
                Text(t("查看全部服务与 Seeker 接入蓝图  →", "Explore services and Seeker blueprints  →", chinese), color = Mint, fontWeight = FontWeight.Bold)
            }
        }

        ProductCard {
            SectionTitle(t("风险概览", "RISK SNAPSHOT", chinese), t("可审计", "AUDITABLE", chinese))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Modifier.weight(1f), "${state.auditEvents.count { it.state == AllowanceState.VERIFIED }}", "PASS", t("已通过", "VERIFIED", chinese), Mint)
                MetricCard(Modifier.weight(1f), "${state.auditEvents.count { it.state == AllowanceState.BLOCKED }}", "STOP", t("已拦截", "BLOCKED", chinese), Amber)
                MetricCard(Modifier.weight(1f), "${state.auditEvents.count { it.state == AllowanceState.FROZEN }}", "HOLD", t("已冻结", "FROZEN", chinese), Rose)
            }
            TextButton(onClick = { navigate(AppPage.ACTIVITY) }, contentPadding = PaddingValues(0.dp)) {
                Text(t("查看完整活动记录  →", "View full activity log  →", chinese), color = Mint, fontWeight = FontWeight.Bold)
            }
        }

        ProductCard {
            SectionTitle(t("当前授权草案", "ACTIVE ALLOWANCE DRAFT", chinese), t("可撤销", "REVOCABLE", chinese))
            Text(if (chinese) activeTemplate.nameZh else activeTemplate.name, color = White, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(activeTemplate.merchant, color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyTag(activeTemplate.token, Blue)
                TinyTag(t("证据必需", "EVIDENCE REQUIRED", chinese), Mint)
                TinyTag(activeTemplate.riskTier, if (activeTemplate.riskTier == "HIGH") Rose else Amber)
            }
            Divider(color = Line)
            Text(t("单笔额度占周期上限", "Per-charge share of cycle cap", chinese), color = Muted, fontSize = 12.sp)
            LinearProgressIndicator(
                progress = (activeTemplate.perCharge / activeTemplate.periodCap).toFloat(),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                color = Mint,
                backgroundColor = Line,
            )
            ValueRow(t("策略哈希", "Policy hash", chinese), short(viewModel.policyHash))
            ValueRow(t("周期", "Billing", chinese), if (chinese) activeTemplate.billingPeriodZh else activeTemplate.billingPeriod)
            TextButton(onClick = { navigate(AppPage.ALLOWANCES) }, contentPadding = PaddingValues(0.dp)) {
                Text(t("审查并测试授权策略  →", "Review and test allowance  →", chinese), color = Mint, fontWeight = FontWeight.Bold)
            }
        }

        ProductCard {
            SectionTitle(t("快速验证矩阵", "QUICK VERIFICATION MATRIX", chinese), "LIVE UI")
            Text(t("点击任一结果，查看策略引擎如何处理请求。", "Replay a request and inspect how the policy engine handles it.", chinese), color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateAction(Modifier.weight(1f), "VERIFIED", Mint) { viewModel.runVerified() }
                StateAction(Modifier.weight(1f), "BLOCKED", Amber) { viewModel.runBlocked() }
                StateAction(Modifier.weight(1f), "FROZEN", Rose) { viewModel.runFrozen() }
            }
            SecondaryButton(Modifier.fillMaxWidth(), t("评委模式：一次生成三种结果", "Judge mode: generate all three outcomes", chinese)) {
                viewModel.runJudgeDemo()
                navigate(AppPage.ACTIVITY)
            }
            DecisionCard(state, chinese)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ServiceMiniCard(
    template: CommercialServiceTemplate,
    selected: Boolean,
    chinese: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(146.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Mint.copy(alpha = 0.14f) else PanelRaised)
            .border(1.dp, if (selected) Mint else Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(
                Brush.linearGradient(listOf(Violet, Blue)),
            ),
            contentAlignment = Alignment.Center,
        ) { Text(template.icon, color = White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
        Text(if (chinese) template.nameZh else template.name, color = White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2)
        Text("${template.perCharge} ${template.token}", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(if (chinese) template.categoryZh else template.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun ServicesPage(
    state: AllowanceUiState,
    viewModel: AllowanceViewModel,
    chinese: Boolean,
    navigate: (AppPage) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var category by rememberSaveable { mutableStateOf("ALL") }
    val visibleTemplates = CommercialCatalog.templates.filter { category == "ALL" || it.category.name == category }

    PageColumn {
        Text(t("商业服务目录", "Commercial service catalog", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(
            t("五类可直接使用的授权模板，证明 Allowance OS 是接入层，而不是单场景 Demo。", "Five ready-to-use policy templates show that Allowance OS is an integration layer, not a single-use demo.", chinese),
            color = Muted,
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("ALL" to t("全部", "All", chinese)) + ServiceCategory.entries.map { it.name to when (it) {
                ServiceCategory.AGENT -> t("Agent", "Agent", chinese)
                ServiceCategory.RESEARCH -> t("报告", "Research", chinese)
                ServiceCategory.SIGNALS -> t("信号", "Signals", chinese)
                ServiceCategory.AUTOMATION -> t("机器人", "Automation", chinese)
                ServiceCategory.API -> "API"
            } }.forEach { (value, label) ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (category == value) White else PanelRaised)
                        .clickable { category = value }.padding(horizontal = 14.dp, vertical = 9.dp),
                ) { Text(label, color = if (category == value) Ink else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }

        visibleTemplates.forEach { template ->
            ProductCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(
                            Brush.linearGradient(listOf(Violet, Blue, Cyan)),
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Text(template.icon, color = White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (chinese) template.nameZh else template.name, color = White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(if (chinese) template.categoryZh else template.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TinyTag(if (state.selectedServiceId == template.id) t("当前", "ACTIVE", chinese) else "READY TEMPLATE", if (state.selectedServiceId == template.id) Mint else Blue)
                }
                Text(if (chinese) template.summaryZh else template.summary, color = Muted, fontSize = 13.sp)
                Divider(color = Line)
                ValueRow(t("单笔 / 周期额度", "Charge / period cap", chinese), "${template.perCharge} / ${template.periodCap} ${template.token}")
                ValueRow(t("计费模式", "Billing", chinese), if (chinese) template.billingPeriodZh else template.billingPeriod)
                ValueRow(t("证据要求", "Evidence", chinese), if (chinese) template.evidenceRequirementZh else template.evidenceRequirement)
                ValueRow(t("允许程序", "Allowed program", chinese), short(template.allowedProgram))
                PrimaryButton(if (state.selectedServiceId == template.id) t("已应用 · 打开授权", "Applied · Open allowance", chinese) else t("应用授权模板", "Apply allowance template", chinese)) {
                    viewModel.selectCommercialTemplate(template.id)
                    navigate(AppPage.ALLOWANCES)
                }
            }
        }

        Text(t("Seeker 接入实验室", "Seeker integration lab", chinese), color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Notice(
            t("以下是基于 Solana Mobile 官方推荐应用设计的非官方接入蓝图，不代表合作或已经上线。", "These are unofficial integration blueprints based on apps featured by Solana Mobile. They do not imply a partnership or live integration.", chinese),
            Amber,
        )
        CommercialCatalog.seekerBlueprints.forEach { blueprint ->
            ProductCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
                        Text(blueprint.icon, color = Cyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(11.dp))
                    Text(blueprint.appName, modifier = Modifier.weight(1f), color = White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    TinyTag("BLUEPRINT · UNOFFICIAL", Amber)
                }
                Text(if (chinese) blueprint.useCaseZh else blueprint.useCase, color = Muted, fontSize = 13.sp)
                BoundaryRow(t("授权模式", "ALLOWANCE PATTERN", chinese), if (chinese) blueprint.allowancePatternZh else blueprint.allowancePattern, Mint)
                BoundaryRow(t("接入要求", "ADAPTER REQUIRED", chinese), if (chinese) blueprint.adapterRequirementZh else blueprint.adapterRequirement, Blue)
                TextButton(onClick = { uriHandler.openUri(blueprint.sourceUrl) }, contentPadding = PaddingValues(0.dp)) {
                    Text(t("查看 Solana Mobile 官方来源  →", "Open Solana Mobile source  →", chinese), color = Mint, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PolicyPage(state: AllowanceUiState, viewModel: AllowanceViewModel, sender: ActivityResultSender, chinese: Boolean) {
    val template = CommercialCatalog.byId(state.selectedServiceId)
    val requestMax = maxOf(template.perCharge * 4.0, 5.0).toFloat()
    var amount by rememberSaveable(state.selectedServiceId) { mutableStateOf(template.perCharge.toFloat()) }
    var periodSpent by rememberSaveable(state.selectedServiceId) { mutableStateOf(0f) }
    var trustedMerchant by rememberSaveable(state.selectedServiceId) { mutableStateOf(true) }
    var evidencePresent by rememberSaveable(state.selectedServiceId) { mutableStateOf(true) }
    var showPublishConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDisconnectConfirmation by rememberSaveable { mutableStateOf(false) }

    PageColumn {
        Text(t("授权中心", "Allowance center", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(t("审查服务预算与证据边界，再决定是否允许钱包签名。", "Review service budgets and evidence boundaries before the wallet may sign.", chinese), color = Muted)

        ProductCard {
            SectionTitle(t("当前服务", "CURRENT SERVICE", chinese), "READY TEMPLATE")
            Text(if (chinese) template.nameZh else template.name, color = White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(if (chinese) template.summaryZh else template.summary, color = Muted, fontSize = 13.sp)
            ValueRow(t("商户", "Merchant", chinese), template.merchant)
            ValueRow(t("计费", "Billing", chinese), if (chinese) template.billingPeriodZh else template.billingPeriod)
            ValueRow(t("交付证明", "Delivery evidence", chinese), if (chinese) template.evidenceRequirementZh else template.evidenceRequirement)
        }

        ProductCard {
            SectionTitle(t("请求模拟器", "REQUEST SIMULATOR", chinese), "PRE-FLIGHT")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t("请求金额", "Requested amount", chinese), modifier = Modifier.weight(1f), color = Muted)
                Text("${"%.1f".format(amount)} USDC", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Slider(
                value = amount,
                onValueChange = { amount = it },
                valueRange = 0.05f..requestMax,
                steps = 39,
                colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Line),
            )
            ValueRow(t("允许上限", "Allowed maximum", chinese), "${template.perCharge} ${template.token}")
            Divider(color = Line)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t("本周期已使用", "Spent this period", chinese), modifier = Modifier.weight(1f), color = Muted)
                Text("${"%.2f".format(periodSpent)} / ${template.periodCap} ${template.token}", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Slider(
                value = periodSpent,
                onValueChange = { periodSpent = it },
                valueRange = 0f..template.periodCap.toFloat(),
                steps = 15,
                colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue, inactiveTrackColor = Line),
            )
            LinearProgressIndicator(
                progress = ((periodSpent + amount) / template.periodCap.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                color = if (periodSpent + amount <= template.periodCap.toFloat()) Mint else Rose,
                backgroundColor = Line,
            )
            Text(
                t("执行后预计：", "Projected after request: ", chinese) + "${"%.2f".format(periodSpent + amount)} / ${template.periodCap} ${template.token}",
                color = if (periodSpent + amount <= template.periodCap.toFloat()) Muted else Rose,
                fontSize = 12.sp,
            )
            Divider(color = Line)
            ToggleRow(t("商户身份", "Merchant identity", chinese), t("可信", "TRUSTED", chinese), t("不匹配", "MISMATCH", chinese), trustedMerchant) { trustedMerchant = it }
            ToggleRow(t("结果证据", "Result evidence", chinese), t("已提供", "PRESENT", chinese), t("缺失", "MISSING", chinese), evidencePresent) { evidencePresent = it }
            PrimaryButton(t("运行策略预检", "Run policy pre-flight", chinese)) {
                viewModel.evaluateCustom(amount.toDouble(), trustedMerchant, evidencePresent, periodSpent.toDouble())
            }
        }

        DecisionCard(state, chinese)

        if (template.id == CommercialCatalog.DEFAULT_ID) {
            ProductCard {
                SectionTitle(t("AlphaBrief 接入样板", "ALPHABRIEF REFERENCE FLOW", chinese), "SDK v2")
                Text(
                    t("报告先生成内容哈希，再经过请求 ID、Nonce、有效期、商户与预算检查；只有 VERIFIED 才解锁。", "The report is content-hashed, then checked for request ID, nonce, expiry, merchant, and budget; it unlocks only after VERIFIED.", chinese),
                    color = Muted,
                    fontSize = 13.sp,
                )
                ValueRow(t("请求 ID", "Request ID", chinese), short(state.requestId))
                ValueRow("Nonce", state.requestNonce.toString())
                ValueRow(t("内容哈希", "Content hash", chinese), short(state.deliveryEvidenceHash.ifBlank { AlphaBriefReference.evidenceHash }))
                if (state.alphaBriefUnlocked) {
                    Notice(t("报告已验证并解锁。", "Report verified and unlocked.", chinese), Mint)
                    Text(AlphaBriefReference.REPORT, color = White, fontSize = 12.sp)
                }
                if (state.replayRejected) {
                    Notice(t("相同证据的新请求已被拒绝，未产生第二次付款。", "A new request reusing the same evidence was rejected; no second payment was created.", chinese), Blue)
                }
                PrimaryButton(t("运行完整报告购买链路", "Run complete report purchase", chinese)) { viewModel.runAlphaBriefDelivery() }
                SecondaryButton(Modifier.fillMaxWidth(), t("尝试重放同一证据", "Attempt evidence replay", chinese)) { viewModel.replayAlphaBriefEvidence() }
            }
        }

        ProductCard {
            SectionTitle(t("执行边界", "ENFORCEMENT BOUNDARIES", chinese), "4 CHECKS")
            CheckRow(t("商户绑定", "Merchant binding", chinese), trustedMerchant, t("身份变化即冻结", "Freeze on identity drift", chinese))
            CheckRow(t("证据完整性", "Evidence integrity", chinese), evidencePresent, t("缺少证明即冻结", "Freeze when proof is missing", chinese))
            CheckRow(t("单笔预算", "Per-charge budget", chinese), amount <= template.perCharge.toFloat(), t("超额在签名前拦截", "Block overspend before signing", chinese))
            CheckRow(t("周期预算", "Period budget", chinese), periodSpent + amount <= template.periodCap.toFloat(), t("累计支出不会突破周期上限", "Cumulative spend cannot exceed the period cap", chinese))
        }

        ProductCard {
            SectionTitle(t("真实钱包动作", "LIVE WALLET ACTION", chinese), "MWA")
            Notice(
                t(
                    "当前动作只发布一笔 Solana Devnet Memo 作为钱包授权证明：不会扣除 USDC，也不会创建可自动扣款的生产授权。只会产生极少量 Devnet 网络费。",
                    "This action publishes only a Solana Devnet Memo as wallet-authorization proof. It does not transfer USDC or create a production recurring-charge allowance; only a tiny Devnet network fee may apply.",
                    chinese,
                ),
                Blue,
            )
            PrimaryButton(t("审查并发布 Devnet Memo", "Review and publish Devnet Memo", chinese)) { showPublishConfirmation = true }
            if (state.error.isNotBlank()) Notice(state.error, Rose)
            SecondaryButton(Modifier.fillMaxWidth(), t("断开钱包并取消 MWA 会话", "Disconnect wallet and deauthorize MWA", chinese)) { showDisconnectConfirmation = true }
            Text(
                t("断开 MWA 会话不会撤销已存在的链上 Allowance；链上撤销必须单独发送 Program 指令。", "Disconnecting MWA does not revoke an existing onchain allowance; onchain revocation requires a separate Program instruction.", chinese),
                color = Muted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showPublishConfirmation) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmation = false },
            title = { Text(t("确认发布测试网证明", "Confirm Devnet proof", chinese), color = White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("网络：Solana Devnet", "Network: Solana Devnet", chinese), color = White)
                    Text(t("资产转移：0 USDC", "Asset transfer: 0 USDC", chinese), color = Mint, fontWeight = FontWeight.Bold)
                    Text(t("内容：策略哈希、商户和请求额度的 Memo 证明", "Payload: Memo proof containing policy hash, merchant, and requested allowance amount", chinese), color = Muted)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPublishConfirmation = false
                    viewModel.publishDevnetProof(sender)
                }) { Text(t("打开钱包确认", "Open wallet", chinese), color = Mint, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirmation = false }) { Text(t("取消", "Cancel", chinese), color = Muted) }
            },
            backgroundColor = Panel,
        )
    }

    if (showDisconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = { Text(t("断开钱包会话？", "Disconnect wallet session?", chinese), color = White, fontWeight = FontWeight.Black) },
            text = { Text(t("这会取消 MWA 重连令牌并清除本地钱包连接，但不会更改链上资产或 Allowance。", "This removes the MWA reconnect token and local wallet connection, but does not change onchain assets or allowances.", chinese), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirmation = false
                    viewModel.revoke(sender)
                }) { Text(t("确认断开", "Disconnect", chinese), color = Rose, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmation = false }) { Text(t("取消", "Cancel", chinese), color = Muted) }
            },
            backgroundColor = Panel,
        )
    }
}

@Composable
private fun EvidencePage(state: AllowanceUiState, viewModel: AllowanceViewModel, chinese: Boolean) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    PageColumn {
        Text(t("证据中心", "Evidence center", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(t("把策略决定、钱包授权与链上证明拆开验证。", "Verify policy decisions, wallet authorization, and onchain proof as separate layers.", chinese), color = Muted)

        ProductCard {
            SectionTitle(t("最新证明", "LATEST PROOF", chinese), if (state.signature.isBlank()) t("待生成", "PENDING", chinese) else "LIVE DEVNET")
            if (state.signature.isBlank()) {
                EmptyProof(chinese)
            } else {
                StatusPill(AllowanceState.VERIFIED)
                Text(t("真实 Devnet Memo 已广播", "Real Devnet Memo broadcast", chinese), color = White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(state.signature, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(Modifier.weight(1f), t("复制签名", "Copy signature", chinese)) { clipboard.setText(AnnotatedString(state.signature)) }
                    SecondaryButton(Modifier.weight(1f), t("浏览器验证", "Verify in Explorer", chinese)) {
                        uriHandler.openUri("https://explorer.solana.com/tx/${state.signature}?cluster=devnet")
                    }
                }
            }
            Divider(color = Line)
            ValueRow(t("策略哈希", "Policy hash", chinese), short(viewModel.policyHash))
            ValueRow(t("证明类型", "Proof type", chinese), "SOLANA MEMO")
            ValueRow(t("网络", "Network", chinese), "SOLANA DEVNET")
        }

        ProductCard {
            SectionTitle(t("验证流水线", "VERIFICATION PIPELINE", chinese), "5 LAYERS")
            TimelineStep("01", t("策略预检", "Policy pre-flight", chinese), t("金额、商户和证据在本地确定结果", "Amount, merchant, and evidence determine the local outcome", chinese), true)
            TimelineStep("02", t("MWA 用户授权", "MWA user authorization", chinese), t("钱包密钥始终留在 Phantom", "Wallet keys remain inside Phantom", chinese), state.walletAddress.isNotBlank())
            TimelineStep("03", t("Devnet 广播", "Devnet broadcast", chinese), t("签名后的 Memo 形成公开授权证据", "Signed Memo creates public authorization evidence", chinese), state.signature.isNotBlank())
            TimelineStep("04", t("Program 强制执行", "Program enforcement", chinese), t("Devnet 已部署，并有真实 VERIFIED / BLOCKED / FROZEN / REVOKED 证明", "Deployed on Devnet with real VERIFIED / BLOCKED / FROZEN / REVOKED proof", chinese), true)
            TimelineStep("05", t("SPL 代币结算", "SPL-token settlement", chinese), t("VERIFIED 已通过 Program CPI 完成真实 Devnet 代币转账", "VERIFIED completed a real Devnet token transfer through Program CPI", chinese), true)
            SecondaryButton(Modifier.fillMaxWidth(), t("打开 Devnet Program", "Open Devnet Program", chinese)) {
                uriHandler.openUri("https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet")
            }
        }

        ProductCard {
            val signature = state.signature.ifBlank { AllowanceViewModel.RECORDED_LIVE_SIGNATURE }
            val usingRecordedProof = state.signature.isBlank()
            SectionTitle(
                t("独立 RPC 验证", "INDEPENDENT RPC VERIFICATION", chinese),
                if (usingRecordedProof) t("已记录证明", "RECORDED PROOF", chinese) else t("本机证明", "DEVICE PROOF", chinese),
            )
            Text(
                if (usingRecordedProof) {
                    t("当前设备尚未广播新证明，因此验证仓库中公开记录的真实 Devnet 交易。", "This device has not broadcast a new proof, so the app verifies the repository's recorded live Devnet transaction.", chinese)
                } else {
                    t("直接通过 Solana RPC 核验当前钱包广播的交易，不依赖区块浏览器页面。", "Verify the wallet-broadcast transaction directly through Solana RPC without trusting an explorer page.", chinese)
                },
                color = Muted,
                fontSize = 13.sp,
            )
            Text(short(signature), color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            when (state.proofCheckPassed) {
                true -> {
                    Notice(t("链上确认成功：交易存在且执行无错误。", "Onchain verification passed: the transaction exists and executed without error.", chinese), Mint)
                    ValueRow(t("确认状态", "Confirmation", chinese), state.proofCheckConfirmation)
                    ValueRow("Slot", state.proofCheckSlot?.toString() ?: "—")
                }
                false -> Notice(state.proofCheckMessage, Rose)
                null -> if (state.proofCheckMessage.isNotBlank()) Notice(state.proofCheckMessage, Blue)
            }
            PrimaryButton(
                if (state.proofCheckLoading) t("正在查询 Solana RPC…", "Querying Solana RPC…", chinese)
                else t("立即独立验证", "Verify independently now", chinese),
            ) { if (!state.proofCheckLoading) viewModel.verifyDevnetProof() }
            SecondaryButton(Modifier.fillMaxWidth(), t("在浏览器打开交易", "Open transaction in explorer", chinese)) {
                uriHandler.openUri("https://explorer.solana.com/tx/$signature?cluster=devnet")
            }
        }

        ProductCard {
            SectionTitle(t("可移植收据", "PORTABLE RECEIPT", chinese), "SHA-256")
            Text(
                t("当前策略决定可导出为结构化收据；哈希可用于把前端结果与链上证明绑定。", "Export the current policy decision as a structured receipt. Its hash binds the UI result to external or onchain evidence.", chinese),
                color = Muted,
                fontSize = 13.sp,
            )
            ValueRow(t("收据指纹", "Receipt fingerprint", chinese), short(viewModel.receiptFingerprint()))
            Text(viewModel.receiptSummary(), color = Muted, fontSize = 10.sp, maxLines = 10, overflow = TextOverflow.Ellipsis)
            SecondaryButton(Modifier.fillMaxWidth(), t("复制完整收据", "Copy full receipt", chinese)) {
                clipboard.setText(AnnotatedString(viewModel.receiptSummary()))
            }
        }

        ProductCard {
            SectionTitle(t("链上三态矩阵", "ONCHAIN STATE MATRIX", chinese), "PROGRAM")
            Text(
                t("直接读取五笔公开交易、最终 allowance 账户和两个 SPL 账户，验证真实结算与失败安全性。", "Read five public transactions, the final allowance account, and both SPL accounts to verify real settlement and failure safety.", chinese),
                color = Muted,
                fontSize = 13.sp,
            )
            when (state.programCheckPassed) {
                true -> Notice(t("Program 三态链路核验成功。", "Program state-transition matrix verified.", chinese), Mint)
                false -> Notice(state.programCheckMessage, Rose)
                null -> if (state.programCheckMessage.isNotBlank()) Notice(state.programCheckMessage, Blue)
            }
            ValueRow("BLOCKED", if (state.programBlockedRejected) t("链上拒绝", "REJECTED ONCHAIN", chinese) else "—")
            ValueRow("FROZEN", if (state.programFrozenPersisted) t("已持久化", "PERSISTED", chinese) else "—")
            ValueRow("REVOKED", if (state.programRevokedPersisted) t("已持久化", "PERSISTED", chinese) else "—")
            ValueRow("SPL CPI", if (state.programSettlementVerified) t("结算已验证", "SETTLEMENT VERIFIED", chinese) else "—")
            ValueRow(t("资金源 raw 余额", "Source raw balance", chinese), state.programSourceTokenRaw.toString())
            ValueRow(t("商户 raw 余额", "Merchant raw balance", chinese), state.programMerchantTokenRaw.toString())
            ValueRow(t("周期已用", "Period spent", chinese), state.programSpentInPeriod.toString())
            PrimaryButton(
                if (state.programCheckLoading) t("正在读取 Devnet…", "Reading Devnet…", chinese)
                else t("验证完整链上矩阵", "Verify full onchain matrix", chinese),
            ) { if (!state.programCheckLoading) viewModel.verifyProgramMatrix() }
            SecondaryButton(Modifier.fillMaxWidth(), t("打开 allowance 账户", "Open allowance account", chinese)) {
                uriHandler.openUri("https://explorer.solana.com/address/${DevnetRpc.ALLOWANCE_ACCOUNT}?cluster=devnet")
            }
        }

        ProductCard {
            SectionTitle(t("真实性边界", "TRUTH BOUNDARY", chinese), t("透明披露", "HONEST DISCLOSURE", chinese))
            BoundaryRow("SIMULATED", t("策略参数回放与三态矩阵", "Policy replay and three-state matrix", chinese), Blue)
            BoundaryRow("LIVE DEVNET PROOF", t("真实 MWA 钱包授权 + Memo 签名", "Real MWA wallet authorization + Memo signature", chinese), Mint)
            BoundaryRow("PROGRAM ENFORCEMENT", t("真实链上状态变更：通过、拒绝、冻结和撤销", "Real onchain state transitions: verify, reject, freeze, and revoke", chinese), Mint)
            BoundaryRow("SPL TOKEN SETTLEMENT", t("VERIFIED 通过 CPI 转移 1,000,000 raw 单位；测试 mint 不是官方 USDC", "VERIFIED transferred 1,000,000 raw units by CPI; the test mint is not canonical USDC", chinese), Mint)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ActivityPage(state: AllowanceUiState, viewModel: AllowanceViewModel, chinese: Boolean) {
    PageColumn {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t("活动记录", "Activity log", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(t("每一次决策、授权和错误都留下本地可审计记录。", "Every decision, authorization, and error leaves a local audit trail.", chinese), color = Muted)
            }
            TextButton(onClick = { viewModel.clearAuditEvents() }) {
                Text(t("清空", "Clear", chinese), color = Rose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.auditEvents.isEmpty()) {
            ProductCard {
                EmptyProof(chinese)
                Text(t("点击策略页的预检按钮后，这里会出现活动记录。", "Run a policy pre-flight to start building the audit trail.", chinese), color = Muted, fontSize = 12.sp)
            }
        } else {
            ProductCard {
                SectionTitle(t("最近事件", "RECENT EVENTS", chinese), "LOCAL AUDIT")
                state.auditEvents.forEachIndexed { index, event ->
                    AuditEventRow(event, chinese)
                    if (index < state.auditEvents.lastIndex) Divider(color = Line)
                }
            }
        }

        ProductCard {
            SectionTitle(t("审计说明", "AUDIT NOTES", chinese), t("设备本地", "DEVICE LOCAL", chinese))
            Text(t("活动记录保存在本机，仅用于演示可审计性；它不会伪装成链上事件。", "Activity entries are stored on-device for auditability and are never presented as onchain events.", chinese), color = Muted, fontSize = 13.sp)
            BoundaryRow("LOCAL AUDIT", t("策略判断、MWA 状态和错误回放", "Policy decisions, MWA state, and error replay", chinese), Blue)
            BoundaryRow("CHAIN EVIDENCE", t("只有真实交易签名才进入公开证据层", "Only a real transaction signature enters the public evidence layer", chinese), Mint)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AuditEventRow(event: AuditEvent, chinese: Boolean) {
    val date = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(event.createdAt))
    Row(verticalAlignment = Alignment.Top) {
        StatusPill(event.state)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(eventTitle(event.kind, chinese), color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(event.message, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (event.amount > 0.0) Text("${"%.1f".format(event.amount)} USDC", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (event.signature.isNotBlank()) Text(short(event.signature), color = Mint, fontSize = 10.sp, maxLines = 1)
        }
        Text(date, color = Muted, fontSize = 10.sp)
    }
}

private fun eventTitle(kind: String, chinese: Boolean): String = when (kind) {
    "POLICY_DECISION" -> t("策略预检", "Policy pre-flight", chinese)
    "TEMPLATE_APPLIED" -> t("商业模板已应用", "Commercial template applied", chinese)
    "WALLET_CONNECTED" -> t("钱包已连接", "Wallet connected", chinese)
    "DEVNET_MEMO_BROADCAST" -> t("Devnet 证明已广播", "Devnet proof broadcast", chinese)
    "MWA_DISCONNECTED" -> t("MWA 会话已断开", "MWA session disconnected", chinese)
    "LOCAL_SESSION_CLEARED" -> t("本地会话已清除", "Local session cleared", chinese)
    "WALLET_ERROR" -> t("钱包连接错误", "Wallet connection error", chinese)
    "BALANCE_ERROR" -> t("余额刷新错误", "Balance refresh error", chinese)
    "PROOF_RPC_VERIFIED" -> t("链上证明已独立验证", "Proof independently verified", chinese)
    "PROOF_RPC_ERROR" -> t("链上证明验证失败", "Proof verification error", chinese)
    "ALPHABRIEF_UNLOCKED" -> t("研究报告已解锁", "Research report unlocked", chinese)
    "REPLAY_REJECTED" -> t("证据重放已拒绝", "Evidence replay rejected", chinese)
    else -> kind
}
