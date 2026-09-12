package com.captain.allowanceos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.solana.publickey.SolanaPublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.net.URI

class AllowancePolicyTest {
    private val policy = AllowancePolicy()

    @Test
    fun mwaIdentityIconRemainsRelativeToThePublicIdentityUri() {
        assertTrue(URI(MWA_IDENTITY_URI).isAbsolute)
        assertFalse(URI(MWA_ICON_RELATIVE_URI).isAbsolute)
        assertEquals(
            "https://0xcaptain888.github.io/allowance-os/favicon-ao-v017.svg",
            URI(MWA_IDENTITY_URI).resolve(MWA_ICON_RELATIVE_URI).toString(),
        )
    }

    @Test
    fun commercialMobileSettlementUsesARealPositiveTransferAndDistinctMerchant() {
        assertTrue(AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS > 0L)
        assertEquals(
            AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT,
            SolanaPublicKey.from(AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT).base58(),
        )
        assertNotEquals(
            "D3XJqkeFiPNtuwKkyeJfVG1Gjvi88AV6fiNs29ukjKm6",
            AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT,
        )
        assertEquals(64, AlphaBriefLiveEvidence.ACCEPTED_EVIDENCE_HASH.length)
    }

    @Test
    fun judgeRunKeepsSimulationRpcAndWalletApprovalInExplicitOrder() {
        assertTrue(JudgeRunStage.VERIFIED.ordinal < JudgeRunStage.BLOCKED.ordinal)
        assertTrue(JudgeRunStage.BLOCKED.ordinal < JudgeRunStage.FROZEN.ordinal)
        assertTrue(JudgeRunStage.FROZEN.ordinal < JudgeRunStage.LIVE_PROOF.ordinal)
        assertTrue(JudgeRunStage.LIVE_PROOF.ordinal < JudgeRunStage.PROGRAM_MATRIX.ordinal)
        assertTrue(JudgeRunStage.PROGRAM_MATRIX.ordinal < JudgeRunStage.WALLET_APPROVAL.ordinal)
        assertTrue(JudgeRunStage.WALLET_APPROVAL.ordinal < JudgeRunStage.COMPLETE.ordinal)
    }

    @Test
    fun validChargeIsVerified() {
        val decision = PolicyEngine.evaluate(policy, 1.0, policy.merchant, "evidence")
        assertEquals(AllowanceState.VERIFIED, decision.state)
    }

    @Test
    fun overCapChargeIsBlocked() {
        val decision = PolicyEngine.evaluate(policy, 10.0, policy.merchant, "evidence")
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }

    @Test
    fun zeroNegativeAndNonFiniteChargesAreBlocked() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { amount ->
            assertEquals(
                "Amount $amount must be rejected",
                AllowanceState.BLOCKED,
                PolicyEngine.evaluate(policy, amount, policy.merchant, "evidence").state,
            )
        }
    }

    @Test
    fun invalidPolicyBudgetIsFrozen() {
        assertEquals(
            AllowanceState.FROZEN,
            PolicyEngine.evaluate(policy.copy(periodCap = 1.0), 1.0, policy.merchant, "evidence").state,
        )
    }

    @Test
    fun periodCapIsEnforcedBeforeWalletInvocation() {
        val decision = PolicyEngine.evaluate(policy, 1.5, policy.merchant, "evidence", periodSpent = 7.0)
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }

    @Test
    fun exactRemainingPeriodBudgetIsVerified() {
        val decision = PolicyEngine.evaluate(policy, 1.0, policy.merchant, "evidence", periodSpent = 7.0)
        assertEquals(AllowanceState.VERIFIED, decision.state)
    }

    @Test
    fun merchantMismatchIsFrozen() {
        val decision = PolicyEngine.evaluate(policy, 1.0, "merchant:lookalike", "evidence")
        assertEquals(AllowanceState.FROZEN, decision.state)
    }

    @Test
    fun policyHashChangesWhenCapChanges() {
        assertNotEquals(
            PolicyEngine.policyHash(policy),
            PolicyEngine.policyHash(policy.copy(perChargeCap = 3.0)),
        )
    }

    @Test
    fun commercialCatalogCoversFiveDistinctUseCases() {
        assertEquals(5, CommercialCatalog.templates.size)
        assertEquals(5, CommercialCatalog.templates.map { it.category }.distinct().size)
        assertEquals(5, CommercialCatalog.templates.map { it.merchant }.distinct().size)
    }

    @Test
    fun everyCommercialTemplateProducesAnEnforceablePolicy() {
        CommercialCatalog.templates.forEach { template ->
            val commercialPolicy = template.toPolicy()
            val decision = PolicyEngine.evaluate(
                commercialPolicy,
                template.perCharge,
                template.merchant,
                "service-delivery-evidence",
            )
            assertEquals("${template.name} should pass its own template", AllowanceState.VERIFIED, decision.state)
        }
    }

    @Test
    fun expiredChargeRequestIsBlocked() {
        val envelope = ChargeEnvelope("req_expired_001", 1, 100, 200, AlphaBriefReference.evidenceHash, AlphaBriefReference.EVIDENCE_URI)
        assertEquals(AllowanceState.BLOCKED, PolicyEngine.evaluateRequest(policy, 1.0, policy.merchant, envelope, 201).state)
    }

    @Test
    fun duplicateEvidenceIsBlockedWithoutASecondPayment() {
        val envelope = ChargeEnvelope("req_replay_001", 2, 100, 500, AlphaBriefReference.evidenceHash, AlphaBriefReference.EVIDENCE_URI)
        val decision = PolicyEngine.evaluateRequest(policy, 1.0, policy.merchant, envelope, 200, setOf(AlphaBriefReference.evidenceHash))
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }

    @Test
    fun restoredWalletRequiresBothPublicIdentityAndEncryptedSession() {
        assertEquals(
            WalletSessionState.CONNECTED,
            resolveRestoredWalletSession("wallet-address", "Primary", "encrypted-token").state,
        )
        assertEquals(
            WalletSessionState.REAUTH_REQUIRED,
            resolveRestoredWalletSession("wallet-address", "Primary", null).state,
        )
        assertEquals(
            WalletSessionState.DISCONNECTED,
            resolveRestoredWalletSession("", "", "orphan-token").state,
        )
    }

    @Test
    fun rpcEndpointConfigurationIsTrimmedDeduplicatedAndRejectsEmptyEntries() {
        assertEquals(
            listOf("https://a.example", "https://b.example"),
            parseRpcEndpoints(" https://a.example,https://b.example,https://a.example,, "),
        )
    }

    @Test
    fun delegatedV2StateDecoderValidatesCapsRolesAndFlags() {
        val authority = SolanaPublicKey.from(DevnetRpc.SOURCE_OWNER)
        val merchant = SolanaPublicKey.from(DevnetRpc.MERCHANT_OWNER)
        val executor = SolanaPublicKey.from(DevnetRpc.ALLOWANCE_ACCOUNT)
        val verifier = SolanaPublicKey.from(DevnetRpc.PROGRAM_ID)
        val mint = SolanaPublicKey.from(DevnetRpc.TOKEN_MINT)
        val source = SolanaPublicKey.from(DevnetRpc.SOURCE_TOKEN_ACCOUNT)
        val data = ByteBuffer.allocate(DelegatedAllowanceV2.STATE_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(2)
            listOf(authority, merchant, executor, verifier, mint, source).forEach { put(it.bytes) }
            putLong(2_000_000)
            putLong(8_000_000)
            putLong(24_000_000)
            putLong(1_000_000)
            putLong(3_000_000)
            putLong(1_789_000_000)
            putLong(604_800)
            putLong(1_800_000_000)
            putLong(4)
            put(1)
            put(0)
            put(1)
            put(ByteArray(32) { 3 })
            put(ByteArray(32) { 7 })
        }.array()
        val decoded = DelegatedAllowanceV2.parseState(data)
        assertEquals(authority.base58(), decoded.authority)
        assertEquals(executor.base58(), decoded.executor)
        assertEquals(4uL, decoded.nextNonce)
        assertTrue(decoded.paused)
        assertTrue(decoded.frozen)
        assertEquals("03".repeat(32), decoded.policyHash)
    }

    @Test
    fun delegatedV2ControlInstructionsPreserveSignerBoundaries() {
        val program = DelegatedAllowanceV2.PROGRAM_ID
        val authority = DevnetRpc.SOURCE_OWNER
        val allowance = DevnetRpc.ALLOWANCE_ACCOUNT
        val source = DevnetRpc.SOURCE_TOKEN_ACCOUNT
        val newExecutor = DevnetRpc.MERCHANT_OWNER
        val currentVerifier = DevnetRpc.PROGRAM_ID
        val newVerifier = DevnetRpc.MERCHANT_TOKEN_ACCOUNT

        val pause = DelegatedAllowanceV2.pause(program, authority, allowance)
        assertEquals(5, pause.data.single().toInt())
        assertTrue(pause.accounts[0].isSigner)
        assertTrue(pause.accounts[1].isWritable)

        val revoke = DelegatedAllowanceV2.revoke(program, authority, allowance, source)
        assertEquals(9, revoke.data.single().toInt())
        assertEquals(DevnetRpc.TOKEN_PROGRAM_ID, revoke.accounts[3].publicKey.base58())

        val rotateExecutor = DelegatedAllowanceV2.rotateExecutor(program, authority, allowance, newExecutor)
        assertEquals(10, rotateExecutor.data.first().toInt())
        assertEquals(newExecutor, SolanaPublicKey(rotateExecutor.data.copyOfRange(1, 33)).base58())

        val rotateVerifier = DelegatedAllowanceV2.rotateVerifier(program, authority, currentVerifier, allowance, newVerifier)
        assertEquals(11, rotateVerifier.data.first().toInt())
        assertTrue(rotateVerifier.accounts[0].isSigner)
        assertTrue(rotateVerifier.accounts[1].isSigner)
        assertEquals(newVerifier, SolanaPublicKey(rotateVerifier.data.copyOfRange(1, 33)).base58())
    }
}
