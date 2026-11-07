package com.booking.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Početni administrator. ADMIN uloga ne može da se dodeli samostalnom registracijom, a
 * dodeljuje je samo drugi ADMIN - bez ovog naloga sistem nikada ne bi dobio prvog
 * administratora, pa bi svi ADMIN endpointi ostali nedostupni.
 *
 * <p>Oba polja moraju biti postavljena da bi se nalog kreirao; kada nisu, servis se
 * normalno pokreće bez administratora (vidi {@code AdminAccountInitializer}).
 *
 * @param email    E-mail početnog administratora (env: ADMIN_EMAIL).
 * @param password Lozinka u čistom obliku, čuva se kao bcrypt heš (env: ADMIN_PASSWORD).
 * @param fullName Ime koje se prikazuje.
 */
@ConfigurationProperties(prefix = "security.admin")
public record AdminProperties(String email, String password, String fullName) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
