package com.captain.allowanceos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

private val Night = Color(0xFF070B14)
private val Panel = Color(0xFF12192A)
private val Mint = Color(0xFF78E6C1)
private val Amber = Color(0xFFFFC86B)
private val Rose = Color(0xFFFF8CAE)
private val Muted = Color(0xFF9BA8C7)

class MainActivity : ComponentActivity() {
    private val viewModel: AllowanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sender = ActivityResultSender(this)
        setContent {
            MaterialTheme(colors = darkColors(background = Night, surface = Panel, primary = Mint)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Night) {
                    AllowanceScreen(viewModel, sender)
                }
            }
        }
    }
}

@Composable
private fun AllowanceScreen(viewModel: AllowanceViewModel, sender: ActivityResultSender) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("SOLANA MOBILE · DEVNET", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Allowance OS", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text(
                "Approve once. Enforce every charge. Revoke anytime.",
                color = Muted,
                fontSize = 17.sp,
            )

            Card {
                Text("DEVICE CAPABILITIES", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ValueRow("MWA wallet", "AVAILABLE")
                ValueRow("Seed Vault", "SEEKER ONLY")
                ValueRow("Genesis Token", "NOT AVAILABLE")
                ValueRow("Cluster", "SOLANA DEVNET")
            }

            Card {
                Text("WALLET AUTHORIZATION", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (state.walletAddress.isBlank()) {
                    Text("No wallet connected", color = Muted)
                    PrimaryButton("Connect Phantom / MWA Wallet") { viewModel.connect(sender) }
                } else {
                    Text(
                        state.walletLabel.ifBlank { "Authorized account" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.walletAddress,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Balance: ${state.solBalance?.let { "%.4f SOL".format(it) } ?: "refresh on next authorization"}",
                        color = Muted,
                    )
                }
            }

            Card {
                Text("ALLOWANCE POLICY", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ValueRow("Merchant", viewModel.policy.merchant)
                ValueRow("Per charge", "${viewModel.policy.perChargeCap} ${viewModel.policy.token}")
                ValueRow("Period cap", "${viewModel.policy.periodCap} ${viewModel.policy.token}")
                ValueRow("Policy hash", short(viewModel.policyHash))
            }

            Card {
                Text("POLICY REPLAY", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton("VERIFIED", Mint) { viewModel.runVerified() }
                    CompactButton("BLOCKED", Amber) { viewModel.runBlocked() }
                    CompactButton("FROZEN", Rose) { viewModel.runFrozen() }
                }
                Text(
                    "BLOCKED and FROZEN stop before a wallet request is opened.",
                    color = Muted,
                    fontSize = 13.sp,
                )
            }

            Card {
                StatusPill(state.allowanceState)
                Text(state.decisionReason, color = Color.White)
                if (state.error.isNotBlank()) {
                    Text(state.error, color = Rose)
                }
                if (state.signature.isNotBlank()) {
                    Text("REAL DEVNET SIGNATURE", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(state.signature, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            uriHandler.openUri(
                                "https://explorer.solana.com/tx/${state.signature}?cluster=devnet",
                            )
                        },
                    ) {
                        Text("Open in Solana Explorer")
                    }
                }
                PrimaryButton("Publish real Devnet authorization proof") {
                    viewModel.publishDevnetProof(sender)
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.revoke(sender) },
                ) {
                    Text("Revoke & deauthorize")
                }
                Text(
                    "The current live transaction is a Memo authorization proof. It is not represented as a deployed allowance settlement.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99070B14)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Mint)
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        content = content,
    )
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Spacer(Modifier.width(16.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(backgroundColor = Mint, contentColor = Night),
        onClick = onClick,
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RowScope.CompactButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = Night),
        contentPadding = ButtonDefaults.ContentPadding,
        onClick = onClick,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StatusPill(state: AllowanceState) {
    val color = when (state) {
        AllowanceState.VERIFIED -> Mint
        AllowanceState.BLOCKED -> Amber
        AllowanceState.FROZEN, AllowanceState.REVOKED -> Rose
        AllowanceState.IDLE -> Muted
    }
    Text(
        state.name,
        modifier = Modifier.background(color, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        color = Night,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
    )
}

private fun short(value: String): String = "${value.take(8)}…${value.takeLast(8)}"
