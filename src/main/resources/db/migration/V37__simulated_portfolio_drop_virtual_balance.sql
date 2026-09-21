-- The simulated wallet no longer has a fictitious starting balance: it holds only
-- the positions the student registers themselves, valued at real market quotes.
-- Nothing else read these two columns — order placement used to debit/credit
-- virtual_balance and reject a buy larger than it, and that whole cash concept is
-- gone. Positions and orders are untouched; only the invented money goes.
alter table simulated_portfolios drop column virtual_balance;
alter table simulated_portfolios drop column initial_balance;
