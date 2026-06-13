package com.epiis.projectcasaketteler.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailHelper {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTemporaryCredentials(String to, String username, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Bienvenido a Casa Ketteler - Credenciales Temporales");
        message.setText(String.format(
                "Hola,\n\n" +
                        "Se ha creado tu cuenta en el sistema de Casa Ketteler.\n\n" +
                        "Tus credenciales temporales son:\n" +
                        "Usuario: %s\n" +
                        "Contraseña temporal: %s\n\n" +
                        "Por favor, ingresa al sistema y cambia tu contraseña en tu primer acceso.\n\n" +
                        "Saludos,\n" +
                        "Equipo Casa Ketteler",
                username, password));

        mailSender.send(message);
    }
}