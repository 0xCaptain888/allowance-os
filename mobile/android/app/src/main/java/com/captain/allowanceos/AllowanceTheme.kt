package com.captain.allowanceos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF060810)
internal val Panel = Color(0xFF101525)
internal val PanelRaised = Color(0xFF181F34)
internal val Mint = Color(0xFF65F3C5)
internal val Blue = Color(0xFF8CA7FF)
internal val Violet = Color(0xFFB78CFF)
internal val Cyan = Color(0xFF5DDCFF)
internal val Amber = Color(0xFFFFCB72)
internal val Rose = Color(0xFFFF759F)
internal val White = Color(0xFFF7F8FF)
internal val Muted = Color(0xFF9BA6BF)
internal val Line = Color(0xFF29324A)

internal enum class AppPage { HOME, SERVICES, ALLOWANCES, ACTIVITY, EVIDENCE }

@Composable
internal fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
internal fun ProductCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF151C30), Color(0xFF0E1423))),
        )
            .border(1.dp, Line, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        content = content,
    )
}

@Composable
internal fun SectionTitle(title: String, badge: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(
            badge,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Line).padding(horizontal = 8.dp, vertical = 4.dp),
            color = White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
internal fun MetricCard(modifier: Modifier, value: String, unit: String, label: String, accent: Color) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(PanelRaised).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(unit, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 8.sp, maxLines = 2)
    }
}

@Composable
internal fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.width(16.dp))
        Text(value, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Mint, contentColor = Ink),
        onClick = onClick,
    ) { Text(label, fontWeight = FontWeight.Black) }
}

@Composable
internal fun SecondaryButton(modifier: Modifier, label: String, onClick: () -> Unit) {
    OutlinedButton(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
        onClick = onClick,
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun StateAction(modifier: Modifier, label: String, color: Color, onClick: () -> Unit) {
    Button(
        modifier = modifier.height(43.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = Ink),
        contentPadding = PaddingValues(horizontal = 4.dp),
        onClick = onClick,
    ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black) }
}

@Composable
internal fun DecisionCard(state: AllowanceUiState, chinese: Boolean) {
    val accent = stateColor(state.allowanceState)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(accent.copy(alpha = 0.09f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(17.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(state.allowanceState)
            Spacer(Modifier.weight(1f))
            Text("${"%.1f".format(state.requestedAmount)} USDC", color = White, fontWeight = FontWeight.Bold)
        }
        Text(localizedReason(state, chinese), color = White, fontSize = 13.sp)
        if (state.error.isNotBlank()) Notice(state.error, Rose)
    }
}

@Composable
internal fun ToggleRow(label: String, first: String, second: String, firstSelected: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChoice(Modifier.weight(1f), first, firstSelected) { onChange(true) }
            ToggleChoice(Modifier.weight(1f), second, !firstSelected) { onChange(false) }
        }
    }
}

@Composable
private fun ToggleChoice(modifier: Modifier, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Color(0xFF203C34) else PanelRaised)
            .border(1.dp, if (selected) Mint else Line, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Mint else Muted, fontSize = 11.sp, fontWeight = FontWeight.Black) }
}

@Composable
internal fun CheckRow(title: String, passed: Boolean, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(if (passed) Color(0xFF203C34) else Color(0xFF3B232B), CircleShape), contentAlignment = Alignment.Center) {
            Text(if (passed) "✓" else "!", color = if (passed) Mint else Rose, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
        Text(if (passed) "PASS" else "FAIL", color = if (passed) Mint else Rose, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun TimelineStep(number: String, title: String, detail: String, complete: Boolean) {
    Row {
        Box(Modifier.size(34.dp).background(if (complete) Mint else Line, CircleShape), contentAlignment = Alignment.Center) {
            Text(if (complete) "✓" else number, color = if (complete) Ink else Muted, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = 8.dp)) {
            Text(title, color = White, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun BoundaryRow(label: String, detail: String, color: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PanelRaised).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(detail, color = White, fontSize = 12.sp)
    }
}

@Composable
internal fun EmptyProof(chinese: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PanelRaised).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("◇", color = Mint, fontSize = 32.sp)
        Text(t("还没有真实链上签名", "No live onchain signature yet", chinese), color = White, fontWeight = FontWeight.Bold)
        Text(
            t("完成 MWA 授权并发布 Devnet Memo 后，证明会显示在这里。", "Authorize through MWA and publish a Devnet Memo to populate this evidence card.", chinese),
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
        )
    }
}

@Composable
internal fun TinyTag(label: String, color: Color) {
    Text(
        label,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 5.dp),
        color = color,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
    )
}

@Composable
internal fun StatusDot(label: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF17392F)).padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Mint, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun Notice(message: String, color: Color) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)).padding(11.dp),
        color = color,
        fontSize = 12.sp,
    )
}

@Composable
internal fun StatusPill(state: AllowanceState) {
    val color = stateColor(state)
    Text(
        state.name,
        modifier = Modifier.background(color, RoundedCornerShape(999.dp)).padding(horizontal = 11.dp, vertical = 6.dp),
        color = Ink,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
    )
}

@Composable
internal fun BottomBar(page: AppPage, chinese: Boolean, onSelect: (AppPage) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Panel).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BottomItem(Modifier.weight(1f), "⌂", t("首页", "Home", chinese), page == AppPage.HOME) { onSelect(AppPage.HOME) }
        BottomItem(Modifier.weight(1f), "◇", t("服务", "Services", chinese), page == AppPage.SERVICES) { onSelect(AppPage.SERVICES) }
        BottomItem(Modifier.weight(1f), "◎", t("授权", "Allowances", chinese), page == AppPage.ALLOWANCES) { onSelect(AppPage.ALLOWANCES) }
        BottomItem(Modifier.weight(1f), "≡", t("活动", "Activity", chinese), page == AppPage.ACTIVITY) { onSelect(AppPage.ACTIVITY) }
        BottomItem(Modifier.weight(1f), "✓", t("证据", "Evidence", chinese), page == AppPage.EVIDENCE) { onSelect(AppPage.EVIDENCE) }
    }
}

@Composable
private fun BottomItem(modifier: Modifier, icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) Color(0xFF203C34) else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = if (selected) Mint else Muted, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Text(label, color = if (selected) Mint else Muted, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, fontSize = 9.sp, maxLines = 1)
    }
}

internal fun stateColor(state: AllowanceState): Color = when (state) {
    AllowanceState.VERIFIED -> Mint
    AllowanceState.BLOCKED -> Amber
    AllowanceState.FROZEN, AllowanceState.REVOKED -> Rose
    AllowanceState.IDLE -> Muted
}

internal fun localizedReason(state: AllowanceUiState, chinese: Boolean): String = when (state.allowanceState) {
    AllowanceState.IDLE -> t("等待策略请求。调整参数后运行预检。", "Waiting for a policy request. Adjust parameters and run pre-flight.", chinese)
    AllowanceState.VERIFIED -> t("商户、证据和预算检查通过。现在可以请求钱包授权。", "Merchant, evidence, and budget checks passed. Wallet authorization may now be requested.", chinese)
    AllowanceState.BLOCKED -> t("请求超过当前授权预算，已在钱包打开前拦截。", "The request exceeds the active allowance budget and was blocked before the wallet opened.", chinese)
    AllowanceState.FROZEN -> if (!state.merchantTrusted) {
        t("商户身份与授权策略不一致，任务已冻结等待恢复审查。", "Merchant identity no longer matches the allowance; the task is frozen for recovery review.", chinese)
    } else {
        t("缺少结果证据，成功的工具调用不足以触发支付。", "Result evidence is missing; a successful tool call alone is not enough for payment.", chinese)
    }
    AllowanceState.REVOKED -> t("MWA 授权已撤销，本地钱包会话已清除。", "MWA authorization was revoked and the local wallet session was cleared.", chinese)
}

internal fun t(zh: String, en: String, chinese: Boolean): String = if (chinese) zh else en

internal fun short(value: String): String = if (value.length <= 18) value else "${value.take(8)}…${value.takeLast(8)}"
