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
            Text("Allowance OS", color = White, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
            DecisionCard(state, chinese)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PolicyPage(state: AllowanceUiState, viewModel: AllowanceViewModel, sender: ActivityResultSender, chinese: Boolean) {
    var amount by rememberSaveable { mutableStateOf(state.requestedAmount.toFloat()) }
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
            ToggleRow(t("商户身份", "Merchant identity", chinese), t("可信", "TRUSTED", chinese), t("不匹配", "MISMATCH", chinese), trustedMerchant) { trustedMerchant = it }
            ToggleRow(t("结果证据", "Result evidence", chinese), t("已提供", "PRESENT", chinese), t("缺失", "MISSING", chinese), evidencePresent) { evidencePresent = it }
            PrimaryButton(t("运行策略预检", "Run policy pre-flight", chinese)) {
                viewModel.evaluateCustom(amount.toDouble(), trustedMerchant, evidencePresent)
            }
        }

        DecisionCard(state, chinese)

        ProductCard {
            SectionTitle(t("执行边界", "ENFORCEMENT BOUNDARIES", chinese), "3 CHECKS")
            CheckRow(t("商户绑定", "Merchant binding", chinese), trustedMerchant, t("身份变化即冻结", "Freeze on identity drift", chinese))
            CheckRow(t("证据完整性", "Evidence integrity", chinese), evidencePresent, t("缺少证明即冻结", "Freeze when proof is missing", chinese))
            CheckRow(t("单笔预算", "Per-charge budget", chinese), amount <= 2f, t("超额在签名前拦截", "Block overspend before signing", chinese))
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
            SectionTitle(t("验证流水线", "VERIFICATION PIPELINE", chinese), "4 LAYERS")
            TimelineStep("01", t("策略预检", "Policy pre-flight", chinese), t("金额、商户和证据在本地确定结果", "Amount, merchant, and evidence determine the local outcome", chinese), true)
            TimelineStep("02", t("MWA 用户授权", "MWA user authorization", chinese), t("钱包密钥始终留在 Phantom", "Wallet keys remain inside Phantom", chinese), state.walletAddress.isNotBlank())
            TimelineStep("03", t("Devnet 广播", "Devnet broadcast", chinese), t("签名后的 Memo 形成公开授权证据", "Signed Memo creates public authorization evidence", chinese), state.signature.isNotBlank())
            TimelineStep("04", t("Program 结算", "Program settlement", chinese), t("尚未部署，明确标记为下一阶段", "Not deployed and explicitly marked as the next phase", chinese), false)
        }

        ProductCard {
            SectionTitle(t("真实性边界", "TRUTH BOUNDARY", chinese), t("透明披露", "HONEST DISCLOSURE", chinese))
            BoundaryRow("SIMULATED", t("策略参数回放与三态矩阵", "Policy replay and three-state matrix", chinese), Blue)
            BoundaryRow("LIVE DEVNET PROOF", t("真实 MWA 钱包授权 + Memo 签名", "Real MWA wallet authorization + Memo signature", chinese), Mint)
            BoundaryRow("PROGRAM SETTLEMENT", t("Rust Program 尚未部署，不能宣称真实扣款", "Rust program is not deployed and is never claimed as a real charge", chinese), Amber)
        }
        Spacer(Modifier.height(12.dp))
    }
}
