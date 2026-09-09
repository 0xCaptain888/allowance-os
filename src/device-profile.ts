export type DeviceProfile = {
  walletMode: 'MWA' | 'MWA+SEED_VAULT';
  seedVaultAvailable: boolean;
  seekerGenesisVerified: boolean;
  label: string;
};

export const standardAndroidProfile: DeviceProfile = {
  walletMode: 'MWA',
  seedVaultAvailable: false,
  seekerGenesisVerified: false,
  label: 'Standard Android + MWA wallet',
};

export const seekerProfile: DeviceProfile = {
  walletMode: 'MWA+SEED_VAULT',
  seedVaultAvailable: true,
  seekerGenesisVerified: true,
  label: 'Seeker + Seed Vault',
};

export function canUseLiveAllowance(profile: DeviceProfile): boolean {
  return profile.walletMode === 'MWA' || profile.seedVaultAvailable;
}

export function capabilitySummary(profile: DeviceProfile) {
  return {
    walletAuthorization: true,
    seedVault: profile.seedVaultAvailable ? 'AVAILABLE' : 'SEEKER_ONLY',
    seekerGenesis: profile.seekerGenesisVerified ? 'VERIFIED' : 'NOT_AVAILABLE',
    liveAllowance: canUseLiveAllowance(profile) ? 'AVAILABLE' : 'DESIGN',
  } as const;
}
