-- Dev/test-only seed data — same `db/migration-dev` location as
-- V2__seed_default_data.sql, so this never runs against a production
-- database (see that file's header comment for the full rationale).
--
-- Gives the admin/admin demo account (seeded in V2) a realistic, diversified
-- portfolio spanning every asset type the app understands, so the account is
-- immediately useful for exploring Portfolio/Home/Academy without first
-- manually adding holdings. Each row is a single purchase lot — quantity and
-- purchase_price (the average price paid) — mirroring exactly how a real
-- lot is recorded via "Adicionar Lançamento"; live current prices are always
-- fetched from the market data provider at read time
-- (GetPortfolioHoldingsUseCaseImpl.fetchCurrentPrice), never stored here.
--
-- 'Tesouro Selic 2028' isn't a B3-listed ticker, so the external quote
-- lookup will find nothing for it and the app correctly falls back to
-- showing it at its purchase price (no fabricated live pricing for
-- instruments the market data provider doesn't cover).

insert into jf_investments (name, quantity, purchase_price, purchase_date, type, user_id)
select v.name, v.quantity, v.purchase_price, v.purchase_date, v.type, u.user_id
from jf_users u
cross join (
    values
        -- Ações (STOCKS)
        ('BBSE3', 100.0, 35.03, date '2024-11-05', 'STOCKS'),
        ('ITSA3', 224.0, 12.67, date '2024-08-12', 'STOCKS'),
        ('BBAS3', 103.0, 22.40, date '2025-01-20', 'STOCKS'),
        ('EGIE3', 65.0, 33.17, date '2024-06-18', 'STOCKS'),
        ('ABEV3', 100.0, 13.82, date '2024-09-25', 'STOCKS'),
        ('WEGE3', 25.0, 47.48, date '2025-02-10', 'STOCKS'),
        ('PETR4', 19.0, 46.63, date '2024-12-03', 'STOCKS'),
        ('VALE3', 9.0, 66.26, date '2025-03-15', 'STOCKS'),
        -- FIIs (REAL_ESTATE)
        ('HGLG11', 41.0, 154.19, date '2024-07-22', 'REAL_ESTATE'),
        ('KNCR11', 49.0, 106.07, date '2024-10-14', 'REAL_ESTATE'),
        ('XPML11', 40.0, 105.08, date '2025-01-08', 'REAL_ESTATE'),
        ('VGHF11', 529.0, 6.47, date '2024-05-30', 'REAL_ESTATE'),
        ('MXRF11', 236.0, 9.56, date '2024-09-02', 'REAL_ESTATE'),
        ('BTCI11', 220.0, 9.25, date '2025-02-19', 'REAL_ESTATE'),
        -- Criptomoedas (CRYPTO)
        ('BTC', 0.00863505, 362954.18, date '2024-04-10', 'CRYPTO'),
        ('ETH', 0.00442462, 10731.83, date '2024-06-05', 'CRYPTO'),
        -- ETFs (FUNDS)
        ('WRLD11', 83.0, 138.02, date '2024-08-28', 'FUNDS'),
        -- Tesouro Direto (FIXED_INCOME)
        ('Tesouro Selic 2028', 0.15, 14500.00, date '2024-03-01', 'FIXED_INCOME')
) as v(name, quantity, purchase_price, purchase_date, type)
where u.email = 'admin@petinvest.local';
