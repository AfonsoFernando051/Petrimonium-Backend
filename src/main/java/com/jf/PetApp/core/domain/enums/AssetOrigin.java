package com.jf.PetApp.core.domain.enums;

/**
 * Where an investment lot's data came from. Every write path today
 * (create/update/configure) is manual entry, so {@code MANUAL} is the only
 * value ever produced right now — {@code BROKER_SYNC} exists for the
 * already-scaffolded (but permanently disabled) {@code B3RealPortfolioSyncAdapter}
 * to stamp once real broker sync ships, so synced lots stay distinguishable
 * from user-entered ones without a later migration.
 */
public enum AssetOrigin {
    MANUAL,
    BROKER_SYNC
}
