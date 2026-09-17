package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.settings.dto.DeleteAccountCommand;

public interface DeleteAccountUseCase {
    /**
     * Apaga definitivamente a conta e todos os seus dados. Irreversível.
     *
     * <p>Exige que o pedido reprove a identidade (senha ou ID token Google no comando), não
     * apenas que traga um bearer token válido — ver {@link DeleteAccountCommand}.
     */
    void execute(DeleteAccountCommand command);
}
