package com.captain.allowanceos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
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
    var page by rememberSaveable { mutableStateOf(AppPage.OVERVIEW) }
    var chinese by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AppHeader(chinese = chinese, onLanguageToggle = { chinese = !chinese })
            Box(modifier = Modifier.weight(1f)) {
                when (page) {
                    AppPage.OVERVIEW -> OverviewPage(state, viewModel, sender, chinese) { page = it }
                    AppPage.POLICY -> PolicyPage(state, viewModel, sender, chinese)
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
                    Text(t("正在等待钱包…", "Waiting for wallet…", chinese), color = White)
                }
            }
        }
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
            Text("Allowance OS · v0.7.0", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(t("链上周期支出控制", "Onchain recurring spend control", chinese), color = Muted, fontSize = 11.sp)
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
    PageColumn {
        Text(t("你的支出防火墙", "Your spending firewall", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(
            t("一次授权，每笔执行都受策略约束。异常交易不会抵达钱包。", "Approve once. Every execution stays inside policy. Bad requests never reach the wallet.", chinese),
            color = Muted,
            fontSize = 15.sp,
        )

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(if (state.walletAddress.isBlank()) PanelRaised else androidx.compose.ui.graphics.Color(0xFF10251F)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(t("钱包授权", "WALLET AUTHORIZATION", chinese), if (state.walletAddress.isBlank()) t("未连接", "DISCONNECTED", chinese) else t("已授权", "AUTHORIZED", chinese))
            if (state.walletAddress.isBlank()) {
                Text(t("连接 Phantom 或任意 MWA 钱包，在签名前保持策略控制。", "Connect Phantom or any MWA wallet while policy stays in front of every signature.", chinese), color = Muted)
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
            MetricCard(Modifier.weight(1f), "2.0", "USDC", t("单笔上限", "PER CHARGE", chinese), Mint)
            MetricCard(Modifier.weight(1f), "8.0", "USDC", t("周期额度", "CYCLE CAP", chinese), Blue)
            MetricCard(Modifier.weight(1f), "3", "GUARDS", t("策略检查", "POLICY CHECKS", chinese), Amber)
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
            SectionTitle(t("活跃授权", "ACTIVE ALLOWANCE", chinese), t("可撤销", "REVOCABLE", chinese))
            Text("ResearchPulse Agent", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("merchant:researchpulse", color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyTag("USDC", Blue)
                TinyTag(t("证据必需", "EVIDENCE REQUIRED", chinese), Mint)
                TinyTag(t("商户绑定", "MERCHANT BOUND", chinese), Amber)
            }
            Divider(color = Line)
            Text(t("单笔额度占周期上限", "Per-charge share of cycle cap", chinese), color = Muted, fontSize = 12.sp)
            LinearProgressIndicator(
                progress = 0.25f,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                color = Mint,
                backgroundColor = Line,
            )
            ValueRow(t("策略哈希", "Policy hash", chinese), short(viewModel.policyHash))
            TextButton(onClick = { navigate(AppPage.POLICY) }, contentPadding = PaddingValues(0.dp)) {
                Text(t("打开策略中心  →", "Open policy studio  →", chinese), color = Mint, fontWeight = FontWeight.Bold)
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
private fun PolicyPage(state: AllowanceUiState, viewModel: AllowanceViewModel, sender: ActivityResultSender, chinese: Boolean) {
    var amount by rememberSaveable { mutableStateOf(state.requestedAmount.toFloat()) }
    var periodSpent by rememberSaveable { mutableStateOf(state.periodSpent.toFloat()) }
    var trustedMerchant by rememberSaveable { mutableStateOf(state.merchantTrusted) }
    var evidencePresent by rememberSaveable { mutableStateOf(state.evidencePresent) }

    PageColumn {
        Text(t("策略中心", "Policy studio", chinese), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(t("像评委一样改变请求参数，策略结论会在钱包打开之前产生。", "Change the request like a judge. The decision is produced before a wallet opens.", chinese), color = Muted)

        ProductCard {
            SectionTitle(t("请求模拟器", "REQUEST SIMULATOR", chinese), "PRE-FLIGHT")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t("请求金额", "Requested amount", chinese), modifier = Modifier.weight(1f), color = Muted)
                Text("${"%.1f".format(amount)} USDC", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Slider(
                value = amount,
                onValueChange = { amount = it },
                valueRange = 0.5f..12f,
                steps = 22,
                colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Line),
            )
            ValueRow(t("允许上限", "Allowed maximum", chinese), "2.0 USDC")
            Divider(color = Line)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t("本周期已使用", "Spent this period", chinese), modifier = Modifier.weight(1f), color = Muted)
                Text("${"%.1f".format(periodSpent)} / 8.0 USDC", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Slider(
                value = periodSpent,
                onValueChange = { periodSpent = it },
                valueRange = 0f..8f,
                steps = 15,
                colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue, inactiveTrackColor = Line),
            )
            LinearProgressIndicator(
                progress = ((periodSpent + amount) / 8f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                color = if (periodSpent + amount <= 8f) Mint else Rose,
                backgroundColor = Line,
            )
            Text(
                t("执行后预计：", "Projected after request: ", chinese) + "${"%.1f".format(periodSpent + amount)} / 8.0 USDC",
                color = if (periodSpent + amount <= 8f) Muted else Rose,
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

        ProductCard {
            SectionTitle(t("执行边界", "ENFORCEMENT BOUNDARIES", chinese), "4 CHECKS")
            CheckRow(t("商户绑定", "Merchant binding", chinese), trustedMerchant, t("身份变化即冻结", "Freeze on identity drift", chinese))
            CheckRow(t("证据完整性", "Evidence integrity", chinese), evidencePresent, t("缺少证明即冻结", "Freeze when proof is missing", chinese))
            CheckRow(t("单笔预算", "Per-charge budget", chinese), amount <= 2f, t("超额在签名前拦截", "Block overspend before signing", chinese))
            CheckRow(t("周期预算", "Period budget", chinese), periodSpent + amount <= 8f, t("累计支出不会突破周期上限", "Cumulative spend cannot exceed the period cap", chinese))
        }

        ProductCard {
            SectionTitle(t("真实钱包动作", "LIVE WALLET ACTION", chinese), "MWA")
            Text(t("只有策略结果为 VERIFIED 时，才应发布真实 Devnet Memo 授权证明。", "Publish a real Devnet Memo authorization proof only after a VERIFIED decision.", chinese), color = Muted, fontSize = 13.sp)
            PrimaryButton(t("发布真实 Devnet 授权证明", "Publish real Devnet authorization proof", chinese)) { viewModel.publishDevnetProof(sender) }
            if (state.error.isNotBlank()) Notice(state.error, Rose)
            SecondaryButton(Modifier.fillMaxWidth(), t("撤销并取消 MWA 授权", "Revoke and deauthorize MWA", chinese)) { viewModel.revoke(sender) }
        }
        Spacer(Modifier.height(12.dp))
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
            TimelineStep("05", t("SPL 代币结算", "SPL-token settlement", chinese), t("尚未接入 CPI，明确标记为下一阶段", "CPI transfer is not connected and remains the next phase", chinese), false)
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
                t("直接读取五笔公开交易和最终 allowance 账户，验证 Program 是否真的拒绝超额、持久化冻结并完成撤销。", "Read five public transactions and the final allowance account to verify that the Program rejected overspend, persisted the freeze, and recorded revocation.", chinese),
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
            BoundaryRow("SPL TOKEN SETTLEMENT", t("尚未发生代币转账，绝不与状态证明混淆", "No token transfer yet; never conflated with state proof", chinese), Amber)
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
    "WALLET_CONNECTED" -> t("钱包已连接", "Wallet connected", chinese)
    "DEVNET_MEMO_BROADCAST" -> t("Devnet 证明已广播", "Devnet proof broadcast", chinese)
    "MWA_REVOKED" -> t("MWA 授权已撤销", "MWA authorization revoked", chinese)
    "LOCAL_SESSION_CLEARED" -> t("本地会话已清除", "Local session cleared", chinese)
    "WALLET_ERROR" -> t("钱包连接错误", "Wallet connection error", chinese)
    "BALANCE_ERROR" -> t("余额刷新错误", "Balance refresh error", chinese)
    "PROOF_RPC_VERIFIED" -> t("链上证明已独立验证", "Proof independently verified", chinese)
    "PROOF_RPC_ERROR" -> t("链上证明验证失败", "Proof verification error", chinese)
    else -> kind
}
