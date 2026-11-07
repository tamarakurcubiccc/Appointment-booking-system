package com.booking.notification.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultTemplateInitializer implements ApplicationRunner {
    private final NotificationTemplateService templates;

    public DefaultTemplateInitializer(NotificationTemplateService templates) {
        this.templates = templates;
    }

    @Override
    public void run(ApplicationArguments args) {
        templates.createIfAbsent(TemplateCodes.ACCOUNT_ACTIVATION,
                "Aktivacija naloga nakon registracije",
                "Aktivirajte svoj nalog",
                """
                Zdravo {{imeKorisnika}},

                hvala što ste se registrovali. Nalog aktivirajte na sledećem linku:

                {{aktivacioniLink}}

                Ako se niste registrovali, slobodno ignorišite ovaj e-mail.
                """);

        templates.createIfAbsent(TemplateCodes.PASSWORD_RESET,
                "Link za promenu lozinke",
                "Promena lozinke",
                """
                Zdravo {{imeKorisnika}},

                za promenu lozinke otvorite sledeći link:

                {{linkZaPromenuLozinke}}

                Ako niste tražili promenu lozinke, ignorišite ovaj e-mail.
                """);

        templates.createIfAbsent(TemplateCodes.RESERVATION_CONFIRMED_CLIENT,
                "Potvrda rezervacije - klijentu",
                "Potvrda rezervacije - {{nazivUsluge}}",
                """
                Zdravo {{imeKlijenta}},

                vaša rezervacija je potvrđena.

                Usluga: {{nazivUsluge}}
                Pružalac usluge: {{imePruzaocaUsluge}}
                Termin: {{vremeTermina}}
                """);

        templates.createIfAbsent(TemplateCodes.RESERVATION_BOOKED_PROVIDER,
                "Novo zakazivanje - pružaocu usluge",
                "Novo zakazivanje - {{nazivUsluge}}",
                """
                Zdravo {{imePruzaocaUsluge}},

                klijent {{imeKlijenta}} je zakazao termin.

                Usluga: {{nazivUsluge}}
                Termin: {{vremeTermina}}
                """);

        templates.createIfAbsent(TemplateCodes.RESERVATION_CANCELLED_CLIENT,
                "Otkazivanje rezervacije - klijentu",
                "Rezervacija je otkazana - {{nazivUsluge}}",
                """
                Zdravo {{imeKlijenta}},

                rezervacija za uslugu {{nazivUsluge}} u terminu {{vremeTermina}} je otkazana.

                Otkazao: {{otkazao}}
                """);

        templates.createIfAbsent(TemplateCodes.RESERVATION_CANCELLED_PROVIDER,
                "Otkazivanje rezervacije - pružaocu usluge",
                "Otkazan termin - {{nazivUsluge}}",
                """
                Zdravo {{imePruzaocaUsluge}},

                termin {{vremeTermina}} za uslugu {{nazivUsluge}} (klijent {{imeKlijenta}}) je otkazan.

                Otkazao: {{otkazao}}
                """);
    }
}
