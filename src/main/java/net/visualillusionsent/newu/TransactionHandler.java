/*
 * This file is part of NewU.
 *
 * Copyright © 2014 Visual Illusions Entertainment
 *
 * NewU is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/gpl.html.
 */
package net.visualillusionsent.newu;

import com.greatmancode.craftconomy3.Cause;
import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.craftconomy3.account.AccountManager;
import com.greatmancode.craftconomy3.currency.CurrencyManager;
import net.canarymod.api.entity.living.humanoid.Player;
import net.visualillusionsent.dconomy.accounting.AccountNotFoundException;
import net.visualillusionsent.dconomy.accounting.AccountingException;
import net.visualillusionsent.dconomy.api.InvalidPluginException;

import static net.visualillusionsent.dconomy.api.account.wallet.WalletAPIListener.testWalletDebit;
import static net.visualillusionsent.dconomy.api.account.wallet.WalletAPIListener.walletBalance;
import static net.visualillusionsent.dconomy.api.account.wallet.WalletAPIListener.walletDebit;

/**
 * @author Jason (darkdiplomat)
 */
final class TransactionHandler {

    static boolean hasAmount(Player player) {
        return percentage(player) >= 0.50;
    }

    static double percentage(Player player) {
        if (NewU.indafamily) {
            // dConomy
            try {
                return walletBalance(player.getName(), false) * NewU.cfg.chargePercent();
            }
            catch (AccountingException e) {
                // Ignored
            }
            catch (AccountNotFoundException e) {
                // Ignored
            }
        }
        else {
            // Craftconomy
            AccountManager accountManager = Common.getInstance().getAccountManager();
            CurrencyManager currencyManager = Common.getInstance().getCurrencyManager();

            if (accountManager.exist(player.getName())) {
                Account pAcc = accountManager.getAccount(player.getName());
                return pAcc.getBalance(player.getWorld().getName(), currencyManager.getDefaultCurrency().getName()) * NewU.cfg.chargePercent();
            }
        }
        return 0;
    }

    static double charge(Player player) {
        if (hasAmount(player)) {
            double debit = percentage(player);
            if (NewU.indafamily) {
                // dConomy
                try {
                    testWalletDebit(player.getName(), debit);
                    walletDebit("NewU", player.getName(), debit, false);
                    return debit;
                }
                catch (AccountingException aex) {
                    // Ignored
                }
                catch (AccountNotFoundException anfex) {
                    // Ignored
                }
                catch (InvalidPluginException e) {
                    // Ignored
                }
            }
            else {
                //Craftconomy
                AccountManager accountManager = Common.getInstance().getAccountManager();
                CurrencyManager currencyManager = Common.getInstance().getCurrencyManager();

                if (accountManager.exist(player.getName())) {
                    Account pAcc = accountManager.getAccount(player.getName());
                    pAcc.withdraw(debit, player.getWorld().getFqName(), currencyManager.getDefaultCurrency().getName(), Cause.PLUGIN, "NewU Respawn Fee");
                    return debit;
                }
            }
        }
        return 0;
    }
}
